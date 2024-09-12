package com.example.auth_service.controller;


import com.example.auth_service.exception.RefreshTokenException;
import com.example.auth_service.exception.RoleException;
import com.example.auth_service.jwt.JwtUtils;
import com.example.auth_service.model.ERole;
import com.example.auth_service.model.RefreshToken;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.SignUpRequest;
import com.example.auth_service.dto.request.TokenRefreshRequest;
import com.example.auth_service.dto.response.JWTResponse;
import com.example.auth_service.dto.response.MessageResponse;
import com.example.auth_service.dto.response.TokenRefreshResponse;
import com.example.auth_service.security.CustomUserDetails;
import com.example.auth_service.service.RefreshTokenService;
import com.example.auth_service.service.RoleService;
import com.example.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/authenticate")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private final RoleService roleService;

    private final RefreshTokenService refreshTokenService;

    private final AuthenticationManager authenticationManager;

    private final PasswordEncoder encoder;

    private final JwtUtils jwtUtils;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@RequestBody SignUpRequest signUpRequest) {

        String username = signUpRequest.getUsername();
        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if(userService.existsByUsername(username)){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if(userService.existsByEmail(email)){
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already taken!"));
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(encoder.encode(password));

        if (strRoles != null) {
            strRoles.forEach(role -> {
                switch (role) {
                    case "ROLE_ADMIN":
                        Role adminRole = roleService.findByName(ERole.ROLE_ADMIN)
                                .orElseGet(() -> new Role(ERole.ROLE_ADMIN));

                        roles.add(adminRole);
                        break;

                    default:
                        Role userRole = roleService.findByName(ERole.ROLE_USER)
                                .orElseGet(() -> new Role(ERole.ROLE_USER));

                        roles.add(userRole);
                }
            });
        }else{
            roleService.findByName(ERole.ROLE_USER).ifPresentOrElse(roles::add, () -> roles.add(new Role(ERole.ROLE_USER)));
        }

        user.setRoles(roles);
        userService.saveUser(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<JWTResponse> authenticateUser(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username,password);

        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String jwt = jwtUtils.generateTokenFromUsername(userDetails.getUsername());

        List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        JWTResponse jwtResponse = new JWTResponse();
        jwtResponse.setEmail(userDetails.getEmail());
        jwtResponse.setUsername(userDetails.getUsername());
        jwtResponse.setId(userDetails.getId());
        jwtResponse.setToken(jwt);
        jwtResponse.setRefreshToken(refreshToken.getToken());
        jwtResponse.setRoles(roles);

        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<TokenRefreshResponse> refreshtoken(@RequestBody TokenRefreshRequest request) {

        String requestRefreshToken = request.getRefreshToken();

        RefreshToken token = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new RefreshTokenException(requestRefreshToken + "Refresh token is not in database!"));

        RefreshToken deletedToken = refreshTokenService.verifyExpiration(token);

        User userRefreshToken = deletedToken.getUser();

        String newToken = jwtUtils.generateTokenFromUsername(userRefreshToken.getUsername());

        return ResponseEntity.ok(new TokenRefreshResponse(newToken, requestRefreshToken));
    }
}

package com.talkifyx.auth_service.config;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.talkifyx.auth_service.entity.User;
import com.talkifyx.auth_service.jwt.JwtUtil;
import com.talkifyx.auth_service.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .username(email.split("@")[0])
                    .avatarUrl(picture)
                    .provider("GOOGLE")
                    .password(encoder.encode("OAUTH_USER"))
                    .status(User.Status.ONLINE)
                    .build();
            return userRepository.save(newUser); 
        });

        String token = jwtUtil.generateAccessToken(user.getEmail(), user.getId());

        user.setLastSeenAt(LocalDateTime.now());
        userRepository.save(user);

        // response.sendRedirect("http://localhost:3000/oauth2/callback?token=" +
        // token);
        response.setContentType("application/json");

        response.getWriter().write("""
                    {
                        "status": 200,
                        "message": "OAuth login success",
                        "token": "%s",
                        "email": "%s"
                    }
                """.formatted(token, email));
    }
}
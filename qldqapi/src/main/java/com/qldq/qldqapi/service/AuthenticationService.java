package com.qldq.qldqapi.service;

import com.qldq.qldqapi.dto.AuthDtos.AuthUserResponse;
import com.qldq.qldqapi.dto.AuthDtos.LoginRequest;
import com.qldq.qldqapi.exception.UnauthorizedException;
import com.qldq.qldqapi.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthenticationService(AuthenticationManager authenticationManager,
                                 SecurityContextRepository securityContextRepository,
                                 SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    public AuthUserResponse login(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.username().trim(), loginRequest.password()));
            sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            return AuthUserResponse.from((AuthenticatedUser) authentication.getPrincipal());
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            throw new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị vô hiệu hóa");
        }
    }

    public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler("SESSION").logout(request, response, authentication);
    }
}

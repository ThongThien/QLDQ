package com.qldq.qldqapi.controller;

import com.qldq.qldqapi.dto.AuthDtos.AuthUserResponse;
import com.qldq.qldqapi.dto.AuthDtos.CsrfTokenResponse;
import com.qldq.qldqapi.dto.AuthDtos.LoginRequest;
import com.qldq.qldqapi.security.AuthenticatedUser;
import com.qldq.qldqapi.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return new CsrfTokenResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }

    @PostMapping("/login")
    public AuthUserResponse login(@Valid @RequestBody LoginRequest loginRequest,
                                  HttpServletRequest request, HttpServletResponse response) {
        return authenticationService.login(loginRequest, request, response);
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        return AuthUserResponse.from((AuthenticatedUser) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        authenticationService.logout(authentication, request, response);
    }
}

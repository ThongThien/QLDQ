package com.qldq.qldqapi.config;

import tools.jackson.databind.ObjectMapper;
import com.qldq.qldqapi.security.DatabaseUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
public class WebSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            ObjectMapper objectMapper,
            DaoAuthenticationProvider authenticationProvider
    ) throws Exception {
        return http
                .authenticationProvider(authenticationProvider)
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/dev/**", "/api/soldiers/**", "/api/activity-logs/**",
                                "/api/assignment-validation/**").hasRole("COMMANDER")
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/api/guard-shifts/*/acknowledgement",
                                "/api/guard-shifts/*/result").hasRole("SOLDIER")
                        .requestMatchers(HttpMethod.GET, "/api/guard-shifts/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/work-items/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/work-schedules/**",
                                "/api/area-monitoring/**").authenticated()
                        .requestMatchers("/api/guard-shifts/**").hasRole("COMMANDER")
                        .requestMatchers("/api/work-schedules/**", "/api/area-monitoring/**").hasRole("COMMANDER")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(objectMapper, response, request.getRequestURI(), 401,
                                        "Unauthorized", "Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(objectMapper, response, request.getRequestURI(), 403,
                                        "Forbidden", "Tài khoản không có quyền thực hiện thao tác này")))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .build();
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(
            DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static void writeSecurityError(
            ObjectMapper objectMapper,
            jakarta.servlet.http.HttpServletResponse response,
            String path,
            int status,
            String error,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now(), "status", status, "error", error,
                "message", message, "path", path
        ));
    }
}

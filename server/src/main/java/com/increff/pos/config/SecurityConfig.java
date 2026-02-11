package com.increff.pos.config;

// --- Imports for Mappers, User, and Data ---
import com.fasterxml.jackson.databind.ObjectMapper;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.UserDto;
import com.increff.pos.model.data.AuthUserData;
import org.springframework.beans.factory.annotation.Autowired;

// --- Standard Spring Imports ---
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // --- 1. All Injected Dependencies ---
    @Autowired
    private UserDto userDto;
    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // --- Rule 1: Public API Endpoints ---
                        .antMatchers(HttpMethod.POST, "/users/signup").permitAll()
                        .antMatchers(HttpMethod.POST, "/session/login").permitAll()

                        // --- Rule 2: Supervisor-Only API Endpoints ---
                        .antMatchers(HttpMethod.POST, "/products/upload").hasRole("SUPERVISOR")
                        .antMatchers(HttpMethod.POST, "/inventory/upload").hasRole("SUPERVISOR")
                        .antMatchers(HttpMethod.GET, "/report/sales").hasRole("SUPERVISOR")
                        .antMatchers(HttpMethod.GET, "/report/inventory").hasRole("SUPERVISOR")
                        .antMatchers(HttpMethod.GET, "/report/summary").hasAnyRole("OPERATOR", "SUPERVISOR")

                        // --- Rule 3: Secure All Other Endpoints ---
                        .anyRequest().authenticated()
                )

                // --- 2. Login Handler using BOTH mappers ---
                .formLogin(form -> form
                        .loginProcessingUrl("/session/login")
                        .successHandler((req, res, auth) -> {
                            try {
                                AuthUserData authUser = userDto.handleLoginSuccess(auth);

                                res.setStatus(HttpStatus.OK.value());
                                res.setContentType("application/json");
                                objectMapper.writeValue(res.getWriter(), authUser);

                            } catch (ApiException e) {
                                throw new ServletException("Login success logic failed", e);
                            }
                        })
                        .failureHandler((req, res, ex) ->
                                res.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication failed")
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/session/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // --- For Swagger ---
        return (web) -> web.ignoring().antMatchers(
                "/swagger-ui/**",
                "/v2/api-docs",
                "/swagger-resources/**",
                "/webjars/**"
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // --- For Angular Frontend ---
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // --- Required for stateful (session) security ---
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L); // Cache preflight requests for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
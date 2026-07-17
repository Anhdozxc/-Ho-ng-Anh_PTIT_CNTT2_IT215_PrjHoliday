package vn.edu.ptit.holidayplanner.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.beans.factory.annotation.Value;
import vn.edu.ptit.holidayplanner.api.ApiErrorResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomUserDetailsService userDetailsService,
                                                   ObjectMapper objectMapper,
                                                   @Value("${app.security.remember-me-key}") String rememberMeKey) throws Exception {
        AntPathRequestMatcher apiMatcher = new AntPathRequestMatcher("/api/**");
        LoginUrlAuthenticationEntryPoint webAuthenticationEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/login");
        AuthenticationEntryPoint apiAuthenticationEntryPoint = (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiErrorResponse.body(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để sử dụng API"));
        };
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/register",
                        "/api/auth/register", "/error", "/forbidden").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll())
            .httpBasic(basic -> basic.authenticationEntryPoint(apiAuthenticationEntryPoint))
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(7 * 24 * 60 * 60)
                .userDetailsService(userDetailsService))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID"))
            .csrf(csrf -> csrf.ignoringRequestMatchers(apiMatcher))
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler((request, response, exception) -> {
                    String path = request.getRequestURI().substring(request.getContextPath().length());
                    if (path.equals("/api") || path.startsWith("/api/")) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(response.getOutputStream(),
                                ApiErrorResponse.body(HttpStatus.FORBIDDEN,
                                        "Bạn không có quyền thực hiện thao tác này"));
                        return;
                    }
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    request.getRequestDispatcher("/forbidden").forward(request, response);
                })
                .defaultAuthenticationEntryPointFor(apiAuthenticationEntryPoint, apiMatcher)
                .defaultAuthenticationEntryPointFor(webAuthenticationEntryPoint,
                        new NegatedRequestMatcher(apiMatcher)));
        return http.build();
    }

    @Bean
    public CookieSameSiteSupplier rememberMeCookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofLax().whenHasName("remember-me");
    }
}

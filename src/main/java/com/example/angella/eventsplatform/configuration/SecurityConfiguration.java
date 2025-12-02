package com.example.angella.eventsplatform.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.thymeleaf.spring6.expression.ThymeleafEvaluationContext;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager databaseAuthenticationManager(HttpSecurity http,
                                                               UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) throws Exception {
        var authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder.userDetailsService(userDetailsService);

        var authProvider = new DaoAuthenticationProvider(passwordEncoder);
        authProvider.setUserDetailsService(userDetailsService);

        authManagerBuilder.authenticationProvider(authProvider);

        return authManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .authorizeHttpRequests((auth) ->
                        auth
                                // Статические ресурсы - самый высокий приоритет
                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()

                                // Публичные API endpoints
                                .requestMatchers("/api/v1/public/**").permitAll()

                                // Chrome DevTools специфичный путь
                                .requestMatchers("/.well-known/**").permitAll()

                                // WebSocket endpoints
                                .requestMatchers("/ws/**", "/topic/**", "/app/**", "/user/**").permitAll()

                                // API endpoints (должны быть после публичных)
                                .requestMatchers("/api/v1/**").authenticated()

                                .requestMatchers("/chats/**").authenticated()

                                // Публичные страницы
                                .requestMatchers("/", "/home", "/events", "/event/details/**",
                                        "/categories", "/about", "/contact",
                                        "/login", "/register", "/error/**").permitAll()

                                // Страницы создания/редактирования событий
                                .requestMatchers("/event/create", "/event/edit/**").authenticated()

                                // Аутентифицированные страницы
                                .requestMatchers("/profile", "/chats/**").authenticated()
                                .requestMatchers("/profile/**").authenticated()

                                // Admin pages
                                .requestMatchers("/admin/**", "/api/v1/admin/**").hasRole("ADMIN")

                                .anyRequest().authenticated()

                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/")
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(86400) // 24 hours
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Disable CSRF for API
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/error/403")
                )
                .authenticationManager(authenticationManager);

        return http.build();
    }
}
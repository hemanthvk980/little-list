package in.littlelist;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
class SecurityConfig {
    @Bean
    @Profile("local")
    InMemoryUserDetailsManager localUsers() {
        return new InMemoryUserDetailsManager(
            User.withUsername("demo").password("{noop}littlelist-local").roles("HOST").build(),
            User.withUsername("another").password("{noop}littlelist-local").roles("HOST").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectProvider<InMemoryUserDetailsManager> local,
                                ObjectProvider<ClientRegistrationRepository> google, AdminAccess admin, org.springframework.jdbc.core.JdbcTemplate db) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/r/**", "/actuator/health", "/api/v1/session", "/api/v1/shared/**",
                "/api/v1/auth/**", "/api/v1/claims/**", "/oauth2/**", "/login/oauth2/**", "/error").permitAll()
            .requestMatchers("/api/v1/admin/**").access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(admin.allowed(authentication.get())))
            .anyRequest().authenticated())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .logout(logout -> logout.logoutUrl("/api/v1/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));

        if (local.getIfAvailable() != null) {
            http.formLogin(form -> form.loginPage("/sign-in").loginProcessingUrl("/api/v1/local-login")
                .successHandler((request, response, auth) -> response.setStatus(204))
                .failureHandler((request, response, error) -> response.setStatus(401))
                .permitAll());
        }
        if (google.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth.loginPage("/sign-in").defaultSuccessUrl("/dashboard", true)
                .successHandler((request,response,authentication) -> { admin.login(authentication); response.sendRedirect("/dashboard"); })
                .failureUrl("/sign-in?error=google"));
        }
        http.addFilterAfter(new AccountSessionFilter(db), org.springframework.security.web.context.SecurityContextHolderFilter.class);
        return http.build();
    }
}

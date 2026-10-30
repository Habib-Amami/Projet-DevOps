package org.example.achat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The demo API permits anonymous requests and does not derive an authenticated
 * identity from cookies, HTTP Basic, or a login session. The frontend uses a
 * same-origin proxy; Swagger is served from the backend origin.
 *
 * CSRF protection is intentionally disabled because no automatically attached
 * browser credentials authorize these requests. Statelessness alone would not
 * justify this decision. Reassess CSRF protection before introducing cookie-based
 * authentication, HTTP Basic, or any other browser-managed credentials.
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Security review and assumptions: backend/README.md, "CSRF and CORS review".
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.anyRequest().permitAll());

		return http.build();
	}
}

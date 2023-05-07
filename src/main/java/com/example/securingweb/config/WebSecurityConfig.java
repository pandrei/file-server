package com.example.securingweb.config;

import com.example.securingweb.CustomAccessDeniedHandler;
import com.example.securingweb.entity.User;
import com.example.securingweb.service.userStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig  {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAccessDeniedHandler customAccessDeniedHandler) throws Exception {
		return http
				.authorizeRequests(authorizeRequests ->
						authorizeRequests
								.requestMatchers("/resources/**").permitAll()
								.requestMatchers("/login", "/register").anonymous()
								.anyRequest().authenticated()
				)
				.formLogin(formLogin ->
						formLogin
								.loginPage("/login")
								.permitAll()
								.defaultSuccessUrl("/home", true)
								.successHandler(authenticationSuccessHandler())
				)
				.logout(logout ->
						logout
								.logoutUrl("/logout")
								.permitAll()
				)
				.exceptionHandling(exceptionHandling ->
						exceptionHandling.accessDeniedHandler(customAccessDeniedHandler)
				)
				.csrf().disable()
				.headers().cacheControl().disable()
				.and()
				.build();
	}

	@Bean
	public UserDetailsService userDetailsService(userStorageService userRepository) {
		return new UserDetailsService() {
			@Override
			public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
				User user = userRepository.findByUsername(username);
				if (user != null) {
					logger.info("Found user: {}", user);
					List<GrantedAuthority> authorities = new ArrayList<>();
					authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
					return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), authorities);
				} else {
					throw new UsernameNotFoundException("User not found");
				}
			}
		};
	}

	private AuthenticationSuccessHandler authenticationSuccessHandler() {
		return (request, response, authentication) -> {
			if (authentication != null && authentication.isAuthenticated()) {
				response.sendRedirect("/home");
			}
		};
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}
}
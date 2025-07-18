package com.ra.base_spring_boot.security;

import com.ra.base_spring_boot.advice.OAuth2SuccessHandler;
import com.ra.base_spring_boot.model.constants.RoleName;
import com.ra.base_spring_boot.security.exception.AccessDenied;
import com.ra.base_spring_boot.security.exception.JwtEntryPoint;
import com.ra.base_spring_boot.security.jwt.JwtProvider;
import com.ra.base_spring_boot.security.jwt.JwtTokenFilter;
import com.ra.base_spring_boot.security.principle.MyUserDetailsService;
import com.ra.base_spring_boot.services.impl.CustomOAuth2UserService;
import com.ra.base_spring_boot.services.impl.FacebookUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig
{
    private final MyUserDetailsService userDetailsService;
    private final JwtEntryPoint jwtEntryPoint;
    private final AccessDenied accessDenied;
    private final JwtTokenFilter jwtTokenFilter;
    private final JwtProvider jwtProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final FacebookUserService facebookUserService;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println(">>> Security filter chain initialized <<<");

        return http
                .cors(cf -> cf.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173"));
                    config.setAllowedMethods(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("*"));
                    return config;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(url -> url
                       // .requestMatchers("/api/v1/admin/**")
                        //.hasAnyAuthority(RoleName.ROLE_MODERATOR.toString(), RoleName.ROLE_ADMIN.toString())
                        .requestMatchers("/api/v1/user/**").hasAuthority(RoleName.ROLE_USER.toString())
                        .requestMatchers("/api/v1/product-variants")
                        .hasAnyAuthority(RoleName.ROLE_ADMIN.toString(), RoleName.ROLE_USER.toString())
                        .requestMatchers("/", "/auth/**", "/oauth2/**").permitAll()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtEntryPoint)
                        .accessDeniedHandler(accessDenied)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterAfter(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> {
                            userInfo
                                    .oidcUserService(customOAuth2UserService) // Google
                                    .userService(facebookUserService);    // Facebook
                        })
                        .successHandler(oAuth2SuccessHandler())
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider()
    {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration auth) throws Exception
    {
        return auth.getAuthenticationManager();
    }
    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(jwtProvider);
    }

}

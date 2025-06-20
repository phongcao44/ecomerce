package com.ra.base_spring_boot.security.jwt;

import com.ra.base_spring_boot.repository.IBlackListRepository;
import com.ra.base_spring_boot.security.principle.MyUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter
{
    private final MyUserDetailsService userDetailsService;
    private final JwtProvider jwtProvider;
    private final IBlackListRepository blackListTokenRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException
    {
        try
        {
            String token = getTokenFromRequest(request);
            if (token != null) {
                // Kiểm tra nếu token đã bị logout
                if (blackListTokenRepository.existsByToken(token)) {
                    log.warn("Token is blacklisted: " + token);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    return;
                }

                String username = jwtProvider.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtProvider.validateToken(token, userDetails)) {
                    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

        }
        catch (Exception e)
        {
            log.error("Un Authentication {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    public String getTokenFromRequest(HttpServletRequest request)
    {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer "))
        {
            return header.substring(7);
        }
        return null;
    }
}

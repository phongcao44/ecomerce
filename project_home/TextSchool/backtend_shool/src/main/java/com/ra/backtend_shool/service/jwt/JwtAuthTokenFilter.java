package com.ra.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
//onceperrequestfilter đảm bảo chỉ chạy 1l mỗi phương thức
public class JwtAuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    protected JwtProvider jwtProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFormRequest(request);

        try{
            if (token != null && jwtProvider.ValidoToken(token)) {
                //lay ng dung tu token
                String userName = jwtProvider.getUserNameFromToken(token);
                //goi userdetail de lay thong tin ng dung
                UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
                if(userDetails != null) {
                    //tao doi tuong xac thuc userdetail
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    //gan thong tin vao securitycontextholder xem nhu da dang nhap
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
        filterChain.doFilter(request, response);
    }

    // laayy ve token header authorization tu httprequest
    public String getTokenFormRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        // su dung bearer token
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}

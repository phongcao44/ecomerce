package com.ra.security.jwt;



import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
// class bawt looxi
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private Logger loger = LoggerFactory.getLogger(JwtEntryPoint.class);

    @Override
    //in ra man hinh conlso va bao loi ng dung
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
            loger.error("Un Authentication {}", authException.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Un Authentication");
    }
}

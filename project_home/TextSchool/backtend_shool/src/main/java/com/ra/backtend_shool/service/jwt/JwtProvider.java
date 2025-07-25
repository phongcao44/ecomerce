package com.ra.security.jwt;

import com.ra.model.entity.User;
import com.ra.security.UserPrinciple;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.ExpressionException;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtProvider {
    @Value("${secret_key}")
    private String SECRET_KEY;

    @Value("${expired}")
    private Long EXPIRED;

    private Logger logger =  LoggerFactory.getLogger(JwtProvider.class);


    //taoj ra token trar ve client
    public String GenerataToken(UserPrinciple userPrinciple){
        //tao thoi gian song cuar token
        Date dateExpiration = new Date(new Date().getTime() + EXPIRED);
    return Jwts.builder()
            .setSubject(userPrinciple.getUsername())
            //napj vao userid
            .claim("userId", userPrinciple.getUser().getId())
            //mahoa
            .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
            //tg song
            .setExpiration(dateExpiration)
            .compact();
    }

    //kiem tra xem cos hop lej cos ddungs khonog cos ton taij khong
    public Boolean ValidoToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return true;
        } catch (ExpressionException | ExpiredJwtException | MalformedJwtException exception) {
            logger.error(exception.getMessage());
        }
        return false;
    }
    //lay ve thong tin
    public String getUserNameFromToken(String token){
        return Jwts.parser().setSigningKey(SECRET_KEY)
                .parseClaimsJws(token).getBody().getSubject();
    }
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }
    }




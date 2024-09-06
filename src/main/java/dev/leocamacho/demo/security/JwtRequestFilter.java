package dev.leocamacho.demo.security;


import dev.leocamacho.demo.session.Session;
import dev.leocamacho.demo.session.SessionContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


public class JwtRequestFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_PREFIX = "Bearer ";
    @Autowired
    private JwtProvider jwtProvider;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String jwtToken = getToken(request);

        if (jwtProvider.isValidToken(jwtToken)) {
            Session session = Session.newBuilder()
                    .fromMap(jwtProvider.getAllClaimsFromToken(jwtToken))
                    .buildForApiRequest();
            SessionContextHolder.setSession(session);
        } else {
            SessionContextHolder.setSession(Session.newBuilder().buildAnonymous());
        }
        chain.doFilter(request, response);

    }

    private String getToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null) {
            return null;
        } else {
            if (AUTHORIZATION_PREFIX.length() <= authorizationHeader.length()) {
                return authorizationHeader.substring(AUTHORIZATION_PREFIX.length());
            } else {
                return null;
            }
        }
    }

}
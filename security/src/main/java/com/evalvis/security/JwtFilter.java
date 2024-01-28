package com.evalvis.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);
    @Autowired
    private BlacklistedJwtTokenRedisRepository blacklistedJwtTokenRedisRepository;
    @Autowired
    private JwtKey key;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            JwtToken.existing(request, key.value(), blacklistedJwtTokenRedisRepository)
                    .ifPresent(
                            token -> {
                                if (token.tokenIsValid() && csrfDoubleSubmitTokenIsValid(request, token)) {
                                    UserDetails userDetails = new User(token.email(), null);
                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails,
                                                    null,
                                                    userDetails.getAuthorities()
                                            );
                                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                }
                            }
                    );
        } catch (Exception e) {
            logger.error("Cannot set user authentication: ", e);
        }
        filterChain.doFilter(request, response);
    }

    private boolean csrfDoubleSubmitTokenIsValid(HttpServletRequest request, JwtToken token) {
        return request.getHeader("X-CSRF-TOKEN").equals(token.csrfToken());
    }
}
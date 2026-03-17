package com.zym.hd.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_KEY_PREFIX = "auth:session:";
    private static final long SESSION_TTL_MINUTES = 30L;

    private final JwtTokenService jwtTokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.jwtTokenService = jwtTokenService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                LoginUser loginUser = jwtTokenService.parseToken(token);
                String sessionKey = SESSION_KEY_PREFIX + jwtTokenService.tokenDigest(token);
                Boolean exists = redisTemplate.hasKey(sessionKey);
                if (!Boolean.TRUE.equals(exists)) {
                    throw new IllegalArgumentException("session expired");
                }
                redisTemplate.expire(sessionKey, SESSION_TTL_MINUTES, TimeUnit.MINUTES);
                String authority = "ROLE_" + loginUser.getRole();
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                loginUser,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority(authority)));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}


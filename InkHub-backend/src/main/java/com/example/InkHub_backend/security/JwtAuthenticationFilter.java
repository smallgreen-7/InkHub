package com.example.InkHub_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** JWT 过滤器：解析 Header 里的 Token → 放入 SecurityContext */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** request 就是前端发过来的数据包
     * response 是你写给前端的回信，代表后端准备返回给前端的响应
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Long userId = jwtUtil.getUserId(token);
                Integer role = jwtUtil.getRole(token);
                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String authority = (role != null && role == 2) ? "ROLE_ADMIN" : "ROLE_USER";
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority(authority)));
                    /** new UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                     *  第一个参数：principal 主体 → getPrincipal () 取它
                     * 第二个参数：credentials 凭证（密码，JWT 场景填 null）
                     * 第三个参数：权限列表
                     */
                    SecurityContextHolder.getContext().setAuthentication(auth);

                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}

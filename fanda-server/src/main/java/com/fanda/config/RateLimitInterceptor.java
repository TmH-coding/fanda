package com.fanda.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 接口限流拦截器 — 令牌桶算法（内存实现，单实例适用）
 *
 * 规则：
 *  - 每个用户每分钟最多 10 次 AI 请求
 *  - /api/ai/chat 额外限制：每分钟 5 次（SSE 流式较耗资源）
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // key: username, value: 桶状态
    private final ConcurrentHashMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> chatBuckets    = new ConcurrentHashMap<>();

    private static final int GENERAL_LIMIT = 10; // 每分钟
    private static final int CHAT_LIMIT    = 5;  // 每分钟（/chat 专用）
    private static final long WINDOW_MS    = 60_000L;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getRequestURI();
        if (!path.startsWith("/api/ai/")) return true;

        String username = request.getUserPrincipal() != null
                ? request.getUserPrincipal().getName()
                : request.getRemoteAddr();

        boolean isChat = path.equals("/api/ai/chat");

        // 通用 AI 限流
        if (!allow(generalBuckets, username, GENERAL_LIMIT)) {
            reject(response, "AI 请求过于频繁，每分钟最多 " + GENERAL_LIMIT + " 次，请稍后再试");
            log.warn("[RateLimit] user={} 触发 AI 通用限流", username);
            return false;
        }

        // /api/ai/chat 额外限流
        if (isChat && !allow(chatBuckets, username, CHAT_LIMIT)) {
            reject(response, "对话请求过于频繁，每分钟最多 " + CHAT_LIMIT + " 次，请稍后再试");
            log.warn("[RateLimit] user={} 触发 /chat 专项限流", username);
            return false;
        }

        return true;
    }

    private boolean allow(ConcurrentHashMap<String, Bucket> map, String key, int limit) {
        long now = System.currentTimeMillis();
        Bucket bucket = map.computeIfAbsent(key, k -> new Bucket(now, limit));
        return bucket.tryConsume(now);
    }

    private void reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(
                "{\"code\":42900,\"message\":\"" + message + "\",\"data\":null}");
    }

    /** 滑动窗口计数桶 */
    private static class Bucket {
        private final AtomicLong windowStart;
        private final AtomicInteger count;
        private final int limit;

        Bucket(long now, int limit) {
            this.windowStart = new AtomicLong(now);
            this.count = new AtomicInteger(0);
            this.limit = limit;
        }

        synchronized boolean tryConsume(long now) {
            if (now - windowStart.get() >= WINDOW_MS) {
                windowStart.set(now);
                count.set(0);
            }
            if (count.get() >= limit) return false;
            count.incrementAndGet();
            return true;
        }
    }
}

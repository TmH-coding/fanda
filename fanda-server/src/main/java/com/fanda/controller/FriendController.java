package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.Friendship;
import com.fanda.entity.User;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.FriendshipMapper;
import com.fanda.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 好友关系控制器
 * GET  /api/friends              - 好友列表
 * GET  /api/friends/requests     - 收到的好友请求
 * GET  /api/friends/search       - 搜索用户
 * POST /api/friends/request      - 发送好友请求
 * POST /api/friends/accept/{id}  - 接受好友请求
 * POST /api/friends/reject/{id}  - 拒绝好友请求
 * DELETE /api/friends/{id}       - 删除好友
 */
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;

    // 搜索防枚举：每个用户每分钟最多 20 次搜索
    private final ConcurrentHashMap<String, AtomicInteger> searchCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong>   searchWindow = new ConcurrentHashMap<>();
    private static final int  SEARCH_LIMIT  = 20;
    private static final long SEARCH_WINDOW = 60_000L;

    // ── 搜索用户 ────────────────────────────────────────────────────
    @GetMapping("/search")
    public ApiResponse<List<Map<String, Object>>> search(
            @RequestParam String keyword,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        String username = authentication.getName();

        // 频率校验
        if (!allowSearch(username)) {
            return ApiResponse.error(42900, "搜索过于频繁，请稍后再试");
        }

        // 关键词长度校验，防止全库扫描
        if (keyword == null || keyword.trim().length() < 2) {
            return ApiResponse.error(40000, "搜索关键词至少 2 个字符");
        }
        String kw = keyword.trim();

        // 按用户名或昵称模糊搜索（排除自己）
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .ne(User::getId, currentUserId)
                .last("LIMIT 20"));

        // 查询与当前用户的好友关系
        List<Long> otherIds = users.stream().map(User::getId).toList();
        Map<Long, String> relationMap = new HashMap<>();
        if (!otherIds.isEmpty()) {
            List<Friendship> relations = friendshipMapper.selectList(new LambdaQueryWrapper<Friendship>()
                    .and(w -> w
                            .eq(Friendship::getRequesterId, currentUserId)
                            .in(Friendship::getAddresseeId, otherIds))
                    .or(w -> w
                            .in(Friendship::getRequesterId, otherIds)
                            .eq(Friendship::getAddresseeId, currentUserId)));
            for (Friendship f : relations) {
                Long otherId = f.getRequesterId().equals(currentUserId) ? f.getAddresseeId() : f.getRequesterId();
                relationMap.put(otherId, f.getStatus());
            }
        }

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname() != null ? u.getNickname() : u.getUsername());
            m.put("avatar", u.getAvatar());
            m.put("relation", relationMap.getOrDefault(u.getId(), "none")); // none/pending/accepted
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    // ── 好友列表 ────────────────────────────────────────────────────
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listFriends(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return ApiResponse.ok(getFriendList(currentUserId, "accepted"));
    }

    // ── 收到的好友请求 ───────────────────────────────────────────────
    @GetMapping("/requests")
    public ApiResponse<List<Map<String, Object>>> listRequests(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);

        List<Friendship> pending = friendshipMapper.selectList(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getAddresseeId, currentUserId)
                .eq(Friendship::getStatus, "pending"));

        List<Map<String, Object>> result = pending.stream().map(f -> {
            User requester = userMapper.selectById(f.getRequesterId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("friendshipId", f.getId());
            m.put("userId", requester != null ? requester.getId() : null);
            m.put("username", requester != null ? requester.getUsername() : "未知");
            m.put("nickname", requester != null
                    ? (requester.getNickname() != null ? requester.getNickname() : requester.getUsername())
                    : "未知");
            m.put("avatar", requester != null ? requester.getAvatar() : null);
            m.put("createdAt", f.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        return ApiResponse.ok(result);
    }

    // ── 发送好友请求 ─────────────────────────────────────────────────
    @PostMapping("/request")
    public ApiResponse<Void> sendRequest(
            @RequestBody Map<String, Long> body,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Long targetId = body.get("userId");
        if (targetId == null) throw new BusinessException(ErrorCode.BAD_REQUEST);
        if (targetId.equals(currentUserId)) throw new BusinessException(ErrorCode.BAD_REQUEST);

        User target = userMapper.selectById(targetId);
        if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        // 检查是否已存在关系
        long existing = friendshipMapper.selectCount(new LambdaQueryWrapper<Friendship>()
                .and(w -> w.eq(Friendship::getRequesterId, currentUserId).eq(Friendship::getAddresseeId, targetId))
                .or(w -> w.eq(Friendship::getRequesterId, targetId).eq(Friendship::getAddresseeId, currentUserId)));
        if (existing > 0) throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);

        Friendship f = new Friendship();
        f.setRequesterId(currentUserId);
        f.setAddresseeId(targetId);
        f.setStatus("pending");
        friendshipMapper.insert(f);

        return ApiResponse.ok(null);
    }

    // ── 接受好友请求 ─────────────────────────────────────────────────
    @PostMapping("/accept/{id}")
    public ApiResponse<Void> accept(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        Friendship f = friendshipMapper.selectById(id);
        if (f == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (!f.getAddresseeId().equals(currentUserId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        f.setStatus("accepted");
        friendshipMapper.updateById(f);
        return ApiResponse.ok(null);
    }

    // ── 拒绝好友请求 ─────────────────────────────────────────────────
    @PostMapping("/reject/{id}")
    public ApiResponse<Void> reject(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        Friendship f = friendshipMapper.selectById(id);
        if (f == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (!f.getAddresseeId().equals(currentUserId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        friendshipMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ── 删除好友 ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeFriend(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        Friendship f = friendshipMapper.selectById(id);
        if (f == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (!f.getRequesterId().equals(currentUserId) && !f.getAddresseeId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        friendshipMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ── 内部工具 ─────────────────────────────────────────────────────
    private boolean allowSearch(String username) {
        long now = System.currentTimeMillis();
        searchWindow.putIfAbsent(username, new AtomicLong(now));
        searchCount.putIfAbsent(username, new AtomicInteger(0));

        AtomicLong window = searchWindow.get(username);
        AtomicInteger count = searchCount.get(username);

        synchronized (window) {
            if (now - window.get() >= SEARCH_WINDOW) {
                window.set(now);
                count.set(0);
            }
            if (count.get() >= SEARCH_LIMIT) return false;
            count.incrementAndGet();
            return true;
        }
    }

    private List<Map<String, Object>> getFriendList(Long userId, String status) {
        List<Friendship> friendships = friendshipMapper.selectList(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getStatus, status)
                .and(w -> w.eq(Friendship::getRequesterId, userId).or().eq(Friendship::getAddresseeId, userId)));

        return friendships.stream().map(f -> {
            Long friendId = f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId();
            User friend = userMapper.selectById(friendId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("friendshipId", f.getId());
            m.put("userId", friendId);
            m.put("username", friend != null ? friend.getUsername() : "未知");
            m.put("nickname", friend != null
                    ? (friend.getNickname() != null ? friend.getNickname() : friend.getUsername())
                    : "未知");
            m.put("avatar", friend != null ? friend.getAvatar() : null);
            return m;
        }).collect(Collectors.toList());
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}

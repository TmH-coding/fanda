package com.fanda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fanda.dto.request.SocialGroupCreateRequest;
import com.fanda.dto.request.VoteRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.mapper.*;
import com.fanda.websocket.SocialRoomManager;
import com.fanda.websocket.WsMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social/groups")
@RequiredArgsConstructor
public class SocialController {

    private final SocialGroupMapper socialGroupMapper;
    private final SocialMemberMapper socialMemberMapper;
    private final SocialCandidateMapper socialCandidateMapper;
    private final SocialVoteMapper socialVoteMapper;
    private final SocialTagMapper socialTagMapper;
    private final UserMapper userMapper;
    private final SocialRoomManager roomManager;
    private final GroupReviewMapper groupReviewMapper;
    private final GroupMessageMapper groupMessageMapper;

    @GetMapping
    public ApiResponse<List<SocialGroup>> list(
            @RequestParam(required = false) String status,
            Authentication authentication) {

        List<SocialGroup> groups;
        if (status != null && !status.isBlank()) {
            groups = socialGroupMapper.selectList(new LambdaQueryWrapper<SocialGroup>()
                    .eq(SocialGroup::getStatus, status)
                    .orderByDesc(SocialGroup::getCreatedAt));
        } else {
            groups = socialGroupMapper.selectList(new LambdaQueryWrapper<SocialGroup>()
                    .orderByDesc(SocialGroup::getCreatedAt));
        }
        groups.forEach(this::loadChildren);
        return ApiResponse.ok(groups);
    }

    @GetMapping("/{id}")
    public ApiResponse<SocialGroup> getById(@PathVariable Long id) {
        SocialGroup group = socialGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        loadChildren(group);
        return ApiResponse.ok(group);
    }

    @PostMapping
    @Transactional
    public ApiResponse<SocialGroup> create(
            @RequestBody @Valid SocialGroupCreateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        SocialGroup group = new SocialGroup();
        group.setCreatorId(userId);
        group.setCreatorName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        group.setAvatar(user.getAvatar());
        group.setTitle(request.getTitle());
        group.setMealTime(request.getTime());
        group.setLocation(request.getLocation());
        group.setMaxPeople(request.getMaxPeople() != null ? request.getMaxPeople() : 4);
        group.setCurrentPeople(1);
        group.setStatus("open");
        socialGroupMapper.insert(group);

        if (request.getTags() != null) {
            for (String t : request.getTags()) {
                SocialTag tag = new SocialTag();
                tag.setGroupId(group.getId());
                tag.setTag(t);
                socialTagMapper.insert(tag);
            }
        }

        if (request.getCandidates() != null) {
            for (String c : request.getCandidates()) {
                SocialCandidate candidate = new SocialCandidate();
                candidate.setGroupId(group.getId());
                candidate.setName(c);
                candidate.setVotes(0);
                socialCandidateMapper.insert(candidate);
            }
        }

        SocialMember member = new SocialMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        socialMemberMapper.insert(member);

        loadChildren(group);
        return ApiResponse.ok(group);
    }

    @PostMapping("/{id}/join")
    @Transactional
    public ApiResponse<SocialGroup> join(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        SocialGroup group = socialGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        Long memberCount = socialMemberMapper.selectCount(new LambdaQueryWrapper<SocialMember>()
                .eq(SocialMember::getGroupId, id).eq(SocialMember::getUserId, userId));
        if (memberCount > 0) throw new BusinessException(ErrorCode.ALREADY_JOINED);

        if (group.getCurrentPeople() >= group.getMaxPeople()) {
            throw new BusinessException(ErrorCode.GROUP_FULL);
        }

        SocialMember member = new SocialMember();
        member.setGroupId(id);
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        socialMemberMapper.insert(member);

        group.setCurrentPeople(group.getCurrentPeople() + 1);
        if (group.getCurrentPeople() >= group.getMaxPeople()) {
            group.setStatus("full");
        }
        socialGroupMapper.updateById(group);

        // 广播加入事件
        User joinedUser = userMapper.selectById(userId);
        String displayName = joinedUser != null
                ? (joinedUser.getNickname() != null ? joinedUser.getNickname() : joinedUser.getUsername())
                : "新成员";
        roomManager.broadcast(id, WsMessage.join(id, displayName));
        if ("full".equals(group.getStatus())) {
            roomManager.broadcast(id, WsMessage.full(id));
        }

        loadChildren(group);
        return ApiResponse.ok(group);
    }

    @PostMapping("/{id}/vote")
    @Transactional
    public ApiResponse<SocialGroup> vote(
            @PathVariable Long id,
            @RequestBody @Valid VoteRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        SocialGroup group = socialGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        Long voteCount = socialVoteMapper.selectCount(new LambdaQueryWrapper<SocialVote>()
                .eq(SocialVote::getGroupId, id).eq(SocialVote::getUserId, userId));
        if (voteCount > 0) throw new BusinessException(ErrorCode.ALREADY_VOTED);

        SocialCandidate candidate = socialCandidateMapper.selectById(request.getCandidateId());
        if (candidate == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        candidate.setVotes(candidate.getVotes() + 1);
        socialCandidateMapper.updateById(candidate);

        SocialVote vote = new SocialVote();
        vote.setGroupId(id);
        vote.setUserId(userId);
        vote.setCandidateId(request.getCandidateId());
        socialVoteMapper.insert(vote);

        // 广播投票事件（含最新候选列表）
        loadChildren(group);
        User voter = userMapper.selectById(userId);
        String voterName = voter != null
                ? (voter.getNickname() != null ? voter.getNickname() : voter.getUsername())
                : "匿名";
        roomManager.broadcast(id, WsMessage.vote(id, voterName, candidate.getName(), group.getCandidates()));

        return ApiResponse.ok(group);
    }

    @GetMapping("/{id}/bill")
    public ApiResponse<Map<String, Object>> getBill(@PathVariable Long id) {
        SocialGroup group = socialGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        List<SocialMember> members = socialMemberMapper.selectList(
                new LambdaQueryWrapper<SocialMember>().eq(SocialMember::getGroupId, id));
        List<SocialCandidate> candidates = socialCandidateMapper.selectList(
                new LambdaQueryWrapper<SocialCandidate>().eq(SocialCandidate::getGroupId, id)
                        .orderByDesc(SocialCandidate::getVotes));

        int memberCount = members.isEmpty() ? 1 : members.size();

        // 找出得票最多的候选（AA账单基于胜出餐厅，预估人均50元）
        String winner = candidates.isEmpty() ? "待定" : candidates.get(0).getName();
        // 预估人均消费：默认50元，可根据实际扩展
        BigDecimal perPersonEstimate = new BigDecimal("50.00");
        BigDecimal totalEstimate = perPersonEstimate.multiply(BigDecimal.valueOf(memberCount));

        Map<String, Object> bill = new HashMap<>();
        bill.put("groupId", id);
        bill.put("groupTitle", group.getTitle());
        bill.put("winner", winner);
        bill.put("memberCount", memberCount);
        bill.put("perPerson", perPersonEstimate);
        bill.put("totalEstimate", totalEstimate);
        bill.put("members", members);
        bill.put("status", group.getStatus());

        return ApiResponse.ok(bill);
    }

    // ── 获取留言列表 ─────────────────────────────────────────────────
    @GetMapping("/{id}/messages")
    public ApiResponse<List<GroupMessage>> getMessages(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "30") int limit) {
        List<GroupMessage> messages = groupMessageMapper.selectList(
                new LambdaQueryWrapper<GroupMessage>()
                        .eq(GroupMessage::getGroupId, id)
                        .orderByDesc(GroupMessage::getCreatedAt)
                        .last("LIMIT " + Math.min(limit, 50)));
        // 按时间正序返回
        java.util.Collections.reverse(messages);
        return ApiResponse.ok(messages);
    }

    // ── 发送留言 ─────────────────────────────────────────────────────
    @PostMapping("/{id}/messages")
    public ApiResponse<GroupMessage> addMessage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        SocialGroup group = socialGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        String content = body.containsKey("content") ? body.get("content").toString().trim() : "";
        if (content.isEmpty() || content.length() > 300) throw new BusinessException(ErrorCode.BAD_REQUEST);

        User user = userMapper.selectById(userId);
        String displayName = user != null
                ? (user.getNickname() != null ? user.getNickname() : user.getUsername())
                : "匿名";

        GroupMessage msg = new GroupMessage();
        msg.setGroupId(id);
        msg.setUserId(userId);
        msg.setUsername(displayName);
        msg.setContent(content);
        groupMessageMapper.insert(msg);

        return ApiResponse.ok(msg);
    }

    // ── 获取活动评价 ─────────────────────────────────────────────────
    @GetMapping("/{id}/reviews")
    public ApiResponse<List<GroupReview>> getReviews(@PathVariable Long id) {
        List<GroupReview> reviews = groupReviewMapper.selectList(
                new LambdaQueryWrapper<GroupReview>()
                        .eq(GroupReview::getGroupId, id)
                        .orderByDesc(GroupReview::getCreatedAt));
        return ApiResponse.ok(reviews);
    }

    // ── 提交活动评价 ─────────────────────────────────────────────────
    @PostMapping("/{id}/reviews")
    public ApiResponse<GroupReview> addReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        SocialGroup group = socialGroupMapper.selectById(id);
        if (group == null) throw new BusinessException(ErrorCode.NOT_FOUND);

        // 只有成员才能评价
        long isMember = socialMemberMapper.selectCount(new LambdaQueryWrapper<SocialMember>()
                .eq(SocialMember::getGroupId, id).eq(SocialMember::getUserId, userId));
        if (isMember == 0) throw new BusinessException(ErrorCode.FORBIDDEN);

        // 每人只能评价一次
        long hasReviewed = groupReviewMapper.selectCount(new LambdaQueryWrapper<GroupReview>()
                .eq(GroupReview::getGroupId, id).eq(GroupReview::getUserId, userId));
        if (hasReviewed > 0) throw new BusinessException(ErrorCode.ALREADY_REVIEWED);

        int rating = body.containsKey("rating") ? ((Number) body.get("rating")).intValue() : 5;
        String content = body.containsKey("content") ? body.get("content").toString() : "";
        rating = Math.max(1, Math.min(5, rating));

        User user = userMapper.selectById(userId);
        String displayName = user != null
                ? (user.getNickname() != null ? user.getNickname() : user.getUsername())
                : "匿名";

        GroupReview review = new GroupReview();
        review.setGroupId(id);
        review.setUserId(userId);
        review.setRating(rating);
        review.setContent(content);
        review.setUsername(displayName);
        groupReviewMapper.insert(review);

        return ApiResponse.ok(review);
    }

    private void loadChildren(SocialGroup group) {
        Long id = group.getId();
        group.setTags(socialTagMapper.selectList(
                new LambdaQueryWrapper<SocialTag>().eq(SocialTag::getGroupId, id)));
        group.setCandidates(socialCandidateMapper.selectList(
                new LambdaQueryWrapper<SocialCandidate>().eq(SocialCandidate::getGroupId, id)));
        group.setMembers(socialMemberMapper.selectList(
                new LambdaQueryWrapper<SocialMember>().eq(SocialMember::getGroupId, id)));
    }

    private Long getCurrentUserId(Authentication auth) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, auth.getName()));
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        return user.getId();
    }
}

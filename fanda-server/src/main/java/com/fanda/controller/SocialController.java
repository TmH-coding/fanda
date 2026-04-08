package com.fanda.controller;

import com.fanda.dto.request.SocialGroupCreateRequest;
import com.fanda.dto.request.VoteRequest;
import com.fanda.dto.response.ApiResponse;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/social/groups")
@RequiredArgsConstructor
public class SocialController {

    private final SocialGroupRepository socialGroupRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final SocialCandidateRepository socialCandidateRepository;
    private final SocialVoteRepository socialVoteRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<SocialGroup>> list(
            @RequestParam(required = false) String status,
            Authentication authentication) {

        List<SocialGroup> groups;
        if (status != null && !status.isBlank()) {
            groups = socialGroupRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            groups = socialGroupRepository.findAllByOrderByCreatedAtDesc();
        }

        return ApiResponse.ok(groups);
    }

    @GetMapping("/{id}")
    public ApiResponse<SocialGroup> getById(@PathVariable Long id) {
        SocialGroup group = socialGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return ApiResponse.ok(group);
    }

    @PostMapping
    @Transactional
    public ApiResponse<SocialGroup> create(
            @RequestBody @Valid SocialGroupCreateRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

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

        // Add tags
        if (request.getTags() != null) {
            List<SocialTag> tags = new ArrayList<>();
            for (String t : request.getTags()) {
                SocialTag tag = new SocialTag();
                tag.setTag(t);
                tag.setSocialGroup(group);
                tags.add(tag);
            }
            group.setTags(tags);
        }

        // Add candidates
        if (request.getCandidates() != null) {
            List<SocialCandidate> candidates = new ArrayList<>();
            for (String c : request.getCandidates()) {
                SocialCandidate candidate = new SocialCandidate();
                candidate.setName(c);
                candidate.setVotes(0);
                candidate.setSocialGroup(group);
                candidates.add(candidate);
            }
            group.setCandidates(candidates);
        }

        socialGroupRepository.save(group);

        // Creator auto-joins the group
        SocialMember member = new SocialMember();
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        member.setSocialGroup(group);
        socialMemberRepository.save(member);

        return ApiResponse.ok(group);
    }

    @PostMapping("/{id}/join")
    @Transactional
    public ApiResponse<SocialGroup> join(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        SocialGroup group = socialGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (socialMemberRepository.existsByGroupIdAndUserId(id, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        if (group.getCurrentPeople() >= group.getMaxPeople()) {
            throw new BusinessException(ErrorCode.GROUP_FULL);
        }

        SocialMember member = new SocialMember();
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        member.setSocialGroup(group);
        socialMemberRepository.save(member);

        group.setCurrentPeople(group.getCurrentPeople() + 1);
        if (group.getCurrentPeople() >= group.getMaxPeople()) {
            group.setStatus("full");
        }
        socialGroupRepository.save(group);

        return ApiResponse.ok(group);
    }

    @PostMapping("/{id}/vote")
    @Transactional
    public ApiResponse<SocialGroup> vote(
            @PathVariable Long id,
            @RequestBody @Valid VoteRequest request,
            Authentication authentication) {

        Long userId = getCurrentUserId(authentication);

        SocialGroup group = socialGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (socialVoteRepository.existsByGroupIdAndUserId(id, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_VOTED);
        }

        SocialCandidate candidate = socialCandidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        candidate.setVotes(candidate.getVotes() + 1);
        socialCandidateRepository.save(candidate);

        SocialVote vote = new SocialVote();
        vote.setGroupId(id);
        vote.setUserId(userId);
        vote.setCandidateId(request.getCandidateId());
        socialVoteRepository.save(vote);

        // Refresh group data
        SocialGroup updatedGroup = socialGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        return ApiResponse.ok(updatedGroup);
    }

    private Long getCurrentUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .getId();
    }
}

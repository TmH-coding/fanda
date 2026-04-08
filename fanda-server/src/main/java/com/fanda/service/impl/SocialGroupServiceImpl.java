package com.fanda.service.impl;

import com.fanda.dto.request.SocialGroupCreateRequest;
import com.fanda.entity.*;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.*;
import com.fanda.service.SocialGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SocialGroupServiceImpl implements SocialGroupService {

    private final SocialGroupRepository socialGroupRepository;
    private final SocialCandidateRepository socialCandidateRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final SocialVoteRepository socialVoteRepository;
    private final UserRepository userRepository;

    @Override
    public List<SocialGroup> getAll(String status) {
        if (status != null && !status.isEmpty()) {
            return socialGroupRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return socialGroupRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public SocialGroup getById(Long id) {
        return socialGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional
    public SocialGroup create(SocialGroupCreateRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        SocialGroup group = new SocialGroup();
        group.setCreatorId(userId);
        group.setCreatorName(creator.getNickname() != null ? creator.getNickname() : creator.getUsername());
        group.setAvatar(creator.getAvatar());
        group.setTitle(request.getTitle());
        group.setMealTime(request.getTime());
        group.setLocation(request.getLocation());
        group.setMaxPeople(request.getMaxPeople() != null ? request.getMaxPeople() : 4);
        group.setCurrentPeople(1);
        group.setStatus("open");

        // Add tags
        if (request.getTags() != null) {
            for (String tagName : request.getTags()) {
                SocialTag tag = new SocialTag();
                tag.setTag(tagName);
                tag.setSocialGroup(group);
                group.getTags().add(tag);
            }
        }

        // Add candidates
        if (request.getCandidates() != null) {
            for (String candidateName : request.getCandidates()) {
                SocialCandidate candidate = new SocialCandidate();
                candidate.setName(candidateName);
                candidate.setVotes(0);
                candidate.setSocialGroup(group);
                group.getCandidates().add(candidate);
            }
        }

        // Add creator as first member
        SocialMember member = new SocialMember();
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        member.setSocialGroup(group);
        group.getMembers().add(member);

        return socialGroupRepository.save(group);
    }

    @Override
    @Transactional
    public SocialGroup join(Long groupId, Long userId) {
        SocialGroup group = socialGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (socialMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }

        if (group.getCurrentPeople() >= group.getMaxPeople()) {
            throw new BusinessException(ErrorCode.GROUP_FULL);
        }

        SocialMember member = new SocialMember();
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        member.setSocialGroup(group);
        group.getMembers().add(member);

        group.setCurrentPeople(group.getCurrentPeople() + 1);
        if (group.getCurrentPeople().equals(group.getMaxPeople())) {
            group.setStatus("full");
        }

        return socialGroupRepository.save(group);
    }

    @Override
    @Transactional
    public SocialGroup vote(Long groupId, Long userId, Long candidateId) {
        SocialGroup group = socialGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (socialVoteRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new BusinessException(ErrorCode.ALREADY_VOTED);
        }

        SocialCandidate candidate = socialCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        SocialVote vote = new SocialVote();
        vote.setGroupId(groupId);
        vote.setUserId(userId);
        vote.setCandidateId(candidateId);
        socialVoteRepository.save(vote);

        candidate.setVotes(candidate.getVotes() + 1);
        socialCandidateRepository.save(candidate);

        // Refresh the group to return updated data
        return socialGroupRepository.findById(groupId).orElse(group);
    }
}

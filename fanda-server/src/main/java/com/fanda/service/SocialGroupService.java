package com.fanda.service;

import com.fanda.entity.SocialGroup;

import java.util.List;

public interface SocialGroupService {
    List<SocialGroup> getAll(String status);
    SocialGroup getById(Long id);
    SocialGroup create(com.fanda.dto.request.SocialGroupCreateRequest request, Long userId);
    SocialGroup join(Long groupId, Long userId);
    SocialGroup vote(Long groupId, Long userId, Long candidateId);
}

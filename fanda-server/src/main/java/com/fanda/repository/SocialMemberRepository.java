package com.fanda.repository;

import com.fanda.entity.SocialMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialMemberRepository extends JpaRepository<SocialMember, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    long countByUserId(Long userId);
}

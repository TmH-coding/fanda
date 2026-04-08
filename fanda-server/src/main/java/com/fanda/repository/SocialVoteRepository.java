package com.fanda.repository;

import com.fanda.entity.SocialVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialVoteRepository extends JpaRepository<SocialVote, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
}

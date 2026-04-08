package com.fanda.repository;

import com.fanda.entity.SocialCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialCandidateRepository extends JpaRepository<SocialCandidate, Long> {
}

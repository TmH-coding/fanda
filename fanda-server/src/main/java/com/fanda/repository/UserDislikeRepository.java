package com.fanda.repository;

import com.fanda.entity.UserDislike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserDislikeRepository extends JpaRepository<UserDislike, Long> {
    List<UserDislike> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}

package com.fanda.repository;

import com.fanda.entity.UserAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {
    List<UserAllergy> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}

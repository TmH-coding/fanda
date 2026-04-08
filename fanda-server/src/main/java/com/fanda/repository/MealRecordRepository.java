package com.fanda.repository;

import com.fanda.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {
    List<MealRecord> findByUserIdOrderByRecordDateDesc(Long userId);
    List<MealRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);
    List<MealRecord> findByUserIdAndRecordDateBetween(Long userId, LocalDate start, LocalDate end);
    long countByUserId(Long userId);
}

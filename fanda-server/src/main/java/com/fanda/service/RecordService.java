package com.fanda.service;

import com.fanda.dto.request.RecordCreateRequest;
import com.fanda.entity.MealRecord;

import java.time.LocalDate;
import java.util.List;

public interface RecordService {
    List<MealRecord> getAll(Long userId);
    List<MealRecord> getByDate(Long userId, LocalDate date);
    List<MealRecord> getByMonth(Long userId, int year, int month);
    MealRecord create(RecordCreateRequest request, Long userId);
    void delete(Long id, Long userId);
}

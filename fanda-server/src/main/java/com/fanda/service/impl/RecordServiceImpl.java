package com.fanda.service.impl;

import com.fanda.dto.request.RecordCreateRequest;
import com.fanda.entity.MealRecord;
import com.fanda.entity.RecordNutrition;
import com.fanda.exception.BusinessException;
import com.fanda.exception.ErrorCode;
import com.fanda.repository.MealRecordRepository;
import com.fanda.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final MealRecordRepository mealRecordRepository;

    @Override
    public List<MealRecord> getAll(Long userId) {
        return mealRecordRepository.findByUserIdOrderByRecordDateDesc(userId);
    }

    @Override
    public List<MealRecord> getByDate(Long userId, LocalDate date) {
        return mealRecordRepository.findByUserIdAndRecordDate(userId, date);
    }

    @Override
    public List<MealRecord> getByMonth(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        return mealRecordRepository.findByUserIdAndRecordDateBetween(userId, start, end);
    }

    @Override
    @Transactional
    public MealRecord create(RecordCreateRequest request, Long userId) {
        MealRecord record = new MealRecord();
        record.setUserId(userId);
        record.setRecordDate(LocalDate.parse(request.getDate()));
        record.setMealType(request.getMealType());
        record.setFoodName(request.getFoodName());
        record.setFoodId(request.getFoodId());
        record.setCost(request.getCost());

        if (request.getNutrition() != null) {
            for (String nutritionType : request.getNutrition()) {
                RecordNutrition nutrition = new RecordNutrition();
                nutrition.setNutritionType(nutritionType);
                nutrition.setMealRecord(record);
                record.getNutritions().add(nutrition);
            }
        }

        return mealRecordRepository.save(record);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        MealRecord record = mealRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        mealRecordRepository.delete(record);
    }
}

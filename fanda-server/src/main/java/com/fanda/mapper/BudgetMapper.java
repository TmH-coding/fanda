package com.fanda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanda.entity.Budget;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {
}

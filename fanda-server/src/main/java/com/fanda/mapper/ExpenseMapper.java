package com.fanda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanda.entity.Expense;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {
}

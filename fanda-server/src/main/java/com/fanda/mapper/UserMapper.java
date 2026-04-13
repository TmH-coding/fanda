package com.fanda.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fanda.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

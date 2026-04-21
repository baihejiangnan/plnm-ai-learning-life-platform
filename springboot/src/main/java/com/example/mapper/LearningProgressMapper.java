package com.example.mapper;

import com.example.entity.LearningProgress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningProgressMapper {
    @Insert("INSERT INTO learning_progress(user_id, course, percent, note) VALUES(#{userId}, #{course}, #{percent}, #{note})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningProgress p);

    @Update("UPDATE learning_progress SET course=#{course}, percent=#{percent}, note=#{note} WHERE id=#{id} AND user_id=#{userId}")
    int updateById(LearningProgress p);

    @Delete("DELETE FROM learning_progress WHERE id=#{id} AND user_id=#{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM learning_progress WHERE id=#{id} AND user_id=#{userId}")
    LearningProgress selectById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM learning_progress WHERE user_id=#{userId} ORDER BY id DESC")
    List<LearningProgress> selectByUserId(Integer userId);
}
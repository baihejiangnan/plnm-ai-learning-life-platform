package com.example.mapper;

import com.example.entity.LearningCourseCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningCourseCategoryMapper {
    @Insert("INSERT INTO learning_course_category(user_id, name, description, sort_order) VALUES(#{userId}, #{name}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningCourseCategory c);

    @Update("UPDATE learning_course_category SET name=#{name}, description=#{description}, sort_order=#{sortOrder} WHERE id=#{id} AND user_id=#{userId}")
    int updateById(LearningCourseCategory c);

    @Delete("DELETE FROM learning_course_category WHERE id=#{id} AND user_id=#{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM learning_course_category WHERE id=#{id} AND user_id=#{userId}")
    LearningCourseCategory selectById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM learning_course_category WHERE user_id=#{userId} ORDER BY sort_order ASC, id ASC")
    List<LearningCourseCategory> selectByUserId(Integer userId);

    @Select("SELECT * FROM learning_course_category WHERE user_id=#{userId} AND name=#{name}")
    LearningCourseCategory selectByUserIdAndName(@Param("userId") Integer userId, @Param("name") String name);
}
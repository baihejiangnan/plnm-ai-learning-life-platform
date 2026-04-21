package com.example.mapper;

import com.example.entity.LearningResource;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface LearningResourceMapper {
    @Insert("INSERT INTO learning_resource(user_id, title, type, source, link, category_id) VALUES(#{userId}, #{title}, #{type}, #{source}, #{link}, #{categoryId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LearningResource r);

    @Update("UPDATE learning_resource SET title=#{title}, type=#{type}, source=#{source}, link=#{link}, category_id=#{categoryId} WHERE id=#{id} AND user_id=#{userId}")
    int updateById(LearningResource r);

    @Delete("DELETE FROM learning_resource WHERE id=#{id} AND user_id=#{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM learning_resource WHERE id=#{id} AND user_id=#{userId}")
    LearningResource selectById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM learning_resource WHERE user_id=#{userId} ORDER BY id DESC")
    List<LearningResource> selectByUserId(Integer userId);

    @Select("SELECT * FROM learning_resource WHERE user_id=#{userId} AND type=#{type} ORDER BY id DESC")
    List<LearningResource> selectByUserIdAndType(@Param("userId") Integer userId, @Param("type") String type);

    @Select("SELECT * FROM learning_resource WHERE user_id=#{userId} AND category_id=#{categoryId} ORDER BY id DESC")
    List<LearningResource> selectByUserIdAndCategory(@Param("userId") Integer userId, @Param("categoryId") Integer categoryId);
}
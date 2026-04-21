package com.example.mapper;

import com.example.entity.ExpenseCategory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExpenseCategoryMapper {
    @Insert("INSERT INTO expense_category(user_id, name, description, sort_order) VALUES(#{userId}, #{name}, #{description}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExpenseCategory c);

    @Update("UPDATE expense_category SET name=#{name}, description=#{description}, sort_order=#{sortOrder} WHERE id=#{id} AND user_id=#{userId}")
    int updateById(ExpenseCategory c);

    @Delete("DELETE FROM expense_category WHERE id=#{id} AND user_id=#{userId}")
    int deleteById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM expense_category WHERE id=#{id} AND user_id=#{userId}")
    ExpenseCategory selectById(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("SELECT * FROM expense_category WHERE user_id=#{userId} ORDER BY sort_order ASC, id ASC")
    List<ExpenseCategory> selectByUserId(Integer userId);

    @Select("SELECT * FROM expense_category WHERE user_id=#{userId} AND name=#{name}")
    ExpenseCategory selectByUserIdAndName(@Param("userId") Integer userId, @Param("name") String name);
}
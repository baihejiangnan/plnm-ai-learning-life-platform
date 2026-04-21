package com.example.mapper;

import com.example.entity.ExpenseBudget;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ExpenseBudgetMapper {
    @Insert("INSERT INTO expense_budget(user_id, month, category_id, amount) VALUES(#{userId}, #{month}, #{categoryId}, #{amount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExpenseBudget b);

    @Update("UPDATE expense_budget SET amount=#{amount} WHERE id=#{id} AND user_id=#{userId}")
    int updateById(ExpenseBudget b);

    @Select("SELECT * FROM expense_budget WHERE user_id=#{userId} AND month=#{month} AND ((category_id IS NULL AND #{categoryId} IS NULL) OR category_id = #{categoryId})")
    ExpenseBudget selectByKey(@Param("userId") Integer userId, @Param("month") String month, @Param("categoryId") Integer categoryId);

    @Select("SELECT * FROM expense_budget WHERE user_id=#{userId} AND month=#{month} ORDER BY category_id IS NULL DESC, category_id ASC")
    List<ExpenseBudget> selectByUserMonth(@Param("userId") Integer userId, @Param("month") String month);
}
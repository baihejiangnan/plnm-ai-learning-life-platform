package com.example.mapper;

import com.example.entity.Expense;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ExpenseMapper {
    @Insert("INSERT INTO expense(user_id, category_id, amount, note, expense_time) " +
            "VALUES(#{userId}, #{categoryId}, #{amount}, #{note}, #{expenseTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Expense e);

    @Update("UPDATE expense SET category_id=#{categoryId}, amount=#{amount}, note=#{note}, expense_time=#{expenseTime} WHERE id=#{id} AND user_id=#{userId}")
    int updateById(Expense e);

    @Delete("DELETE FROM expense WHERE id=#{id} AND user_id=#{userId}")
    int deleteById(@Param("id") Long id, @Param("userId") Integer userId);

    @Select("SELECT * FROM expense WHERE id=#{id} AND user_id=#{userId}")
    Expense selectById(@Param("id") Long id, @Param("userId") Integer userId);

    @Select("<script>" +
            "SELECT * FROM expense WHERE user_id=#{userId} " +
            "<if test='month != null and month != \"\"'> AND DATE_FORMAT(expense_time, '%Y-%m') = #{month} </if>" +
            "<if test='categoryId != null'> AND category_id = #{categoryId} </if>" +
            "ORDER BY expense_time DESC, id DESC" +
            "</script>")
    List<Expense> selectList(@Param("userId") Integer userId,
                             @Param("month") String month,
                             @Param("categoryId") Integer categoryId);

    @Select("SELECT COALESCE(SUM(amount),0) FROM expense WHERE user_id=#{userId} AND DATE_FORMAT(expense_time, '%Y-%m') = #{month}")
    BigDecimal sumByMonth(@Param("userId") Integer userId, @Param("month") String month);

    @Select("SELECT c.id AS categoryId, c.name AS name, COALESCE(SUM(e.amount),0) AS total " +
            "FROM expense_category c LEFT JOIN expense e ON c.id = e.category_id AND e.user_id = #{userId} AND DATE_FORMAT(e.expense_time, '%Y-%m') = #{month} " +
            "WHERE c.user_id = #{userId} " +
            "GROUP BY c.id, c.name ORDER BY total DESC")
    List<java.util.Map<String, Object>> sumByMonthGroupByCategory(@Param("userId") Integer userId, @Param("month") String month);
}
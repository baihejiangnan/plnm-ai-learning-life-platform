package com.example.service;

import com.example.entity.Expense;
import com.example.mapper.ExpenseMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {
    @Autowired
    private ExpenseMapper expenseMapper;

    public Expense create(Expense e) {
        expenseMapper.insert(e);
        return e;
    }

    public Expense update(Expense e) {
        expenseMapper.updateById(e);
        return e;
    }

    public boolean delete(Long id, Integer userId) {
        return expenseMapper.deleteById(id, userId) > 0;
    }

    public Expense get(Long id, Integer userId) {
        return expenseMapper.selectById(id, userId);
    }

    public PageInfo<Expense> list(Integer userId, String month, Integer categoryId, Integer page, Integer size) {
        PageHelper.startPage(page, size);
        List<Expense> list = expenseMapper.selectList(userId, month, categoryId);
        return new PageInfo<>(list);
    }

    public Map<String, Object> monthStats(Integer userId, String month) {
        Map<String, Object> stats = new HashMap<>();
        BigDecimal total = expenseMapper.sumByMonth(userId, month);
        stats.put("total", total);
        List<Map<String, Object>> byCat = expenseMapper.sumByMonthGroupByCategory(userId, month);
        stats.put("byCategory", byCat);
        return stats;
    }
}
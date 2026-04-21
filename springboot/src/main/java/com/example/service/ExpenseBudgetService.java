package com.example.service;

import com.example.entity.ExpenseBudget;
import com.example.mapper.ExpenseBudgetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseBudgetService {
    @Autowired
    private ExpenseBudgetMapper budgetMapper;

    public ExpenseBudget setBudget(ExpenseBudget b) {
        ExpenseBudget existing = budgetMapper.selectByKey(b.getUserId(), b.getMonth(), b.getCategoryId());
        if (existing == null) {
            budgetMapper.insert(b);
            return b;
        } else {
            b.setId(existing.getId());
            budgetMapper.updateById(b);
            return b;
        }
    }

    public List<ExpenseBudget> getBudgets(Integer userId, String month) {
        return budgetMapper.selectByUserMonth(userId, month);
    }
}
package com.example.service;

import com.example.entity.ExpenseCategory;
import com.example.mapper.ExpenseCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseCategoryService {
    @Autowired
    private ExpenseCategoryMapper categoryMapper;

    public ExpenseCategory create(ExpenseCategory c) {
        categoryMapper.insert(c);
        return c;
    }

    public ExpenseCategory update(ExpenseCategory c) {
        categoryMapper.updateById(c);
        return c;
    }

    public boolean delete(Integer id, Integer userId) {
        return categoryMapper.deleteById(id, userId) > 0;
    }

    public List<ExpenseCategory> list(Integer userId) {
        return categoryMapper.selectByUserId(userId);
    }

    public List<ExpenseCategory> initDefault(Integer userId) {
        String[] defaults = new String[]{"餐饮","交通","购物","娱乐","学习","其他"};
        List<ExpenseCategory> created = new ArrayList<>();
        int order = 1;
        for (String name : defaults) {
            ExpenseCategory exists = categoryMapper.selectByUserIdAndName(userId, name);
            if (exists == null) {
                ExpenseCategory c = new ExpenseCategory();
                c.setUserId(userId);
                c.setName(name);
                c.setDescription(null);
                c.setSortOrder(order++);
                categoryMapper.insert(c);
                created.add(c);
            }
        }
        return created;
    }
}
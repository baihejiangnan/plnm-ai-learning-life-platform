package com.example.service;

import com.example.entity.LearningCourseCategory;
import com.example.mapper.LearningCourseCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LearningCourseCategoryService {
    @Autowired
    private LearningCourseCategoryMapper mapper;

    public LearningCourseCategory create(LearningCourseCategory c) {
        mapper.insert(c);
        return c;
    }

    public LearningCourseCategory update(LearningCourseCategory c) {
        mapper.updateById(c);
        return c;
    }

    public boolean delete(Integer id, Integer userId) {
        return mapper.deleteById(id, userId) > 0;
    }

    public List<LearningCourseCategory> list(Integer userId) {
        return mapper.selectByUserId(userId);
    }

    public List<LearningCourseCategory> initDefault(Integer userId) {
        String[] defaults = new String[]{"编程基础","前端","后端","算法与数据结构","数据库","英语","其他"};
        List<LearningCourseCategory> created = new ArrayList<>();
        int order = 1;
        for (String name : defaults) {
            LearningCourseCategory exists = mapper.selectByUserIdAndName(userId, name);
            if (exists == null) {
                LearningCourseCategory c = new LearningCourseCategory();
                c.setUserId(userId);
                c.setName(name);
                c.setDescription(null);
                c.setSortOrder(order++);
                mapper.insert(c);
                created.add(c);
            }
        }
        return created;
    }
}
package com.example.service;

import com.example.entity.LearningResource;
import com.example.mapper.LearningResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningResourceService {
    @Autowired
    private LearningResourceMapper mapper;

    public LearningResource create(LearningResource r) {
        mapper.insert(r);
        return r;
    }

    public LearningResource update(LearningResource r) {
        mapper.updateById(r);
        return r;
    }

    public boolean delete(Integer id, Integer userId) {
        return mapper.deleteById(id, userId) > 0;
    }

    public List<LearningResource> list(Integer userId) {
        return mapper.selectByUserId(userId);
    }

    public List<LearningResource> listByType(Integer userId, String type) {
        return mapper.selectByUserIdAndType(userId, type);
    }

    public List<LearningResource> listByCategory(Integer userId, Integer categoryId) {
        return mapper.selectByUserIdAndCategory(userId, categoryId);
    }
}
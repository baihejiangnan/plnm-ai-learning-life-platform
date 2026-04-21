package com.example.service;

import com.example.entity.LearningProgress;
import com.example.mapper.LearningProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LearningProgressService {
    @Autowired
    private LearningProgressMapper mapper;

    public LearningProgress create(LearningProgress p) {
        if (p.getPercent() == null) p.setPercent(0);
        mapper.insert(p);
        return p;
    }

    public LearningProgress update(LearningProgress p) {
        mapper.updateById(p);
        return p;
    }

    public boolean delete(Integer id, Integer userId) {
        return mapper.deleteById(id, userId) > 0;
    }

    public List<LearningProgress> list(Integer userId) {
        return mapper.selectByUserId(userId);
    }
}
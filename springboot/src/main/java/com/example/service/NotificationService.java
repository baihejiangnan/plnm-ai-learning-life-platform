package com.example.service;

import com.example.entity.Notification;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private final Map<Integer, List<Notification>> store = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Object>> settings = new ConcurrentHashMap<>();

    private List<Notification> ensureUserData(Integer userId) {
        return store.computeIfAbsent(userId, uid -> {
            List<Notification> list = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (int i = 1; i <= 5; i++) {
                Notification n = new Notification();
                n.setId(i);
                n.setUserId(uid);
                n.setType(i % 2 == 0 ? "system" : "security");
                n.setTitle(i % 2 == 0 ? "系统更新提示" : "安全提醒");
                n.setContent(i % 2 == 0 ? "系统组件已升级至最新版本" : "检测到一次登录尝试，请核对是否本人操作");
                n.setStatus(i <= 3 ? "unread" : "read");
                n.setCreateTime(now - i * 3600_000L);
                n.setLinkUrl("/notes");
                list.add(n);
            }
            return list;
        });
    }

    public Map<String, Object> list(Integer userId, String status, Integer page, Integer size) {
        List<Notification> all = new ArrayList<>(ensureUserData(userId));
        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            all = all.stream().filter(n -> status.equalsIgnoreCase(n.getStatus())).collect(Collectors.toList());
        }
        int total = all.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<Notification> pageList = from < to ? all.subList(from, to) : Collections.emptyList();
        Map<String, Object> res = new HashMap<>();
        res.put("list", pageList);
        res.put("total", total);
        return res;
    }

    public boolean markRead(Integer userId, List<Integer> ids) {
        List<Notification> list = ensureUserData(userId);
        boolean changed = false;
        for (Notification n : list) {
            if (ids.contains(n.getId())) {
                n.setStatus("read");
                changed = true;
            }
        }
        return changed;
    }

    public boolean delete(Integer userId, List<Integer> ids) {
        List<Notification> list = ensureUserData(userId);
        int before = list.size();
        list.removeIf(n -> ids.contains(n.getId()));
        return list.size() < before;
    }

    public Map<String, Object> getSettings(Integer userId) {
        return settings.computeIfAbsent(userId, uid -> {
            Map<String, Object> s = new HashMap<>();
            s.put("enabled", true);
            s.put("categories", Arrays.asList("system", "security", "collaboration"));
            return s;
        });
    }

    public boolean updateSettings(Integer userId, Map<String, Object> payload) {
        Map<String, Object> s = getSettings(userId);
        if (payload.containsKey("enabled")) {
            s.put("enabled", payload.get("enabled"));
        }
        if (payload.containsKey("categories")) {
            Object c = payload.get("categories");
            if (c instanceof List) {
                s.put("categories", c);
            }
        }
        settings.put(userId, s);
        return true;
    }
}
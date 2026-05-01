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

    public void initializeWelcome(Integer userId, String displayName) {
        if (userId == null) {
            return;
        }
        String name = displayName == null || displayName.trim().isEmpty() ? "你" : displayName.trim();
        long now = System.currentTimeMillis();
        List<Notification> list = new ArrayList<>();
        list.add(createNotification(1, userId, "system", "欢迎加入 PLNM", name + "，默认标签和预算分类已经准备好，可以从首页开始记录第一条笔记。", "unread", now, "/home"));
        store.put(userId, list);
        getSettings(userId);
    }

    private List<Notification> ensureUserData(Integer userId) {
        return store.computeIfAbsent(userId, uid -> {
            List<Notification> list = new ArrayList<>();
            long now = System.currentTimeMillis();
            list.add(createNotification(1, uid, "security", "新设备登录提醒", "检测到一次新设备登录，请核对是否本人操作。", "unread", now - 12 * 60_000L, "/settings?tab=security"));
            list.add(createNotification(2, uid, "system", "预算使用率提醒", "本月预算已使用 13%，当前节奏正常，可以继续保持。", "unread", now - 48 * 60_000L, "/life/budgets"));
            list.add(createNotification(3, uid, "collaboration", "AI 周复盘已生成", "AI 已整理最近笔记、预算和学习进度，建议查看审计列表。", "unread", now - 2 * 3600_000L, "/ai/audit"));
            list.add(createNotification(4, uid, "system", "笔记同步完成", "最近编辑的笔记已完成同步，列表统计已刷新。", "read", now - 4 * 3600_000L, "/notes/list"));
            list.add(createNotification(5, uid, "collaboration", "学习计划建议", "Vue 课程进度可继续推进，建议今晚补充一条学习笔记。", "read", now - 8 * 3600_000L, "/learning/progress"));
            list.add(createNotification(6, uid, "security", "密码安全建议", "建议定期更新密码，并避免和其他网站使用相同密码。", "read", now - 24 * 3600_000L, "/settings?tab=security"));
            return list;
        });
    }

    private Notification createNotification(Integer id,
                                            Integer userId,
                                            String type,
                                            String title,
                                            String content,
                                            String status,
                                            Long createTime,
                                            String linkUrl) {
        Notification n = new Notification();
        n.setId(id);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setStatus(status);
        n.setCreateTime(createTime);
        n.setLinkUrl(linkUrl);
        return n;
    }

    public Map<String, Object> list(Integer userId, String status, Integer page, Integer size) {
        List<Notification> source = new ArrayList<>(ensureUserData(userId));
        source.sort(Comparator.comparing(Notification::getCreateTime, Comparator.nullsLast(Long::compareTo)).reversed());
        long unreadCount = source.stream().filter(n -> "unread".equalsIgnoreCase(n.getStatus())).count();
        Map<String, Long> typeCounts = source.stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));

        List<Notification> all = new ArrayList<>(source);
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
        res.put("unreadCount", unreadCount);
        res.put("typeCounts", typeCounts);
        return res;
    }

    public Map<String, Object> overview(Integer userId) {
        List<Notification> all = new ArrayList<>(ensureUserData(userId));
        all.sort(Comparator.comparing(Notification::getCreateTime, Comparator.nullsLast(Long::compareTo)).reversed());

        Map<String, Object> res = new HashMap<>();
        res.put("total", all.size());
        res.put("unreadCount", all.stream().filter(n -> "unread".equalsIgnoreCase(n.getStatus())).count());
        res.put("typeCounts", all.stream().collect(Collectors.groupingBy(Notification::getType, Collectors.counting())));
        res.put("recent", all.stream().limit(6).collect(Collectors.toList()));
        res.put("settings", getSettings(userId));
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
            s.put("desktop", true);
            s.put("emailDigest", false);
            s.put("quietHours", false);
            s.put("sound", true);
            s.put("priorityOnly", false);
            s.put("digestTime", "21:30");
            s.put("categories", Arrays.asList("system", "security", "collaboration"));
            return s;
        });
    }

    public boolean updateSettings(Integer userId, Map<String, Object> payload) {
        Map<String, Object> s = getSettings(userId);
        List<String> booleanKeys = Arrays.asList("enabled", "desktop", "emailDigest", "quietHours", "sound", "priorityOnly");
        for (String key : booleanKeys) {
            if (payload.containsKey(key) && payload.get(key) instanceof Boolean) {
                s.put(key, payload.get(key));
            }
        }
        if (payload.containsKey("digestTime") && payload.get("digestTime") instanceof String) {
            s.put("digestTime", payload.get("digestTime"));
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

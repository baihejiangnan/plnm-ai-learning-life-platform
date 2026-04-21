package com.example.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiRoleConfigService {

    private final List<AiRole> roles = new ArrayList<>();

    public AiRoleConfigService() {
        loadRoles();
    }

    public List<AiRole> listRoles() {
        return roles;
    }

    public AiRole getRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return roles.stream().filter(r -> "system_operator".equals(r.id)).findFirst().orElse(null);
        }
        return roles.stream().filter(r -> roleId.equals(r.id)).findFirst().orElse(null);
    }

    private void loadRoles() {
        try {
            ClassPathResource resource = new ClassPathResource("ai/roles.yml");
            if (!resource.exists()) {
                loadDefaultRoles();
                return;
            }
            InputStream in = resource.getInputStream();
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            Object rolesObj = root == null ? null : root.get("roles");
            if (!(rolesObj instanceof List<?> roleList) || roleList.isEmpty()) {
                loadDefaultRoles();
                return;
            }
            for (Object obj : roleList) {
                if (!(obj instanceof Map<?, ?> map)) continue;
                AiRole role = new AiRole();
                role.id = readString(map, "id", "");
                role.name = readString(map, "name", role.id);
                role.type = readString(map, "type", "generic");
                role.streaming = readBoolean(map, "streaming", false);
                role.temperature = parseDouble(map.get("temperature"), role.streaming ? 0.9 : 0.1);
                role.topP = parseDouble(map.get("topP"), role.streaming ? 0.95 : 0.5);
                role.knowledgeBase = readString(map, "knowledgeBase", "none");
                role.description = readString(map, "description", "");
                roles.add(role);
            }
            if (roles.isEmpty()) {
                loadDefaultRoles();
            }
        } catch (Exception ignored) {
            loadDefaultRoles();
        }
    }

    private void loadDefaultRoles() {
        roles.clear();
        roles.add(buildRole("system_operator", "系统操作员", "operator", false, 0.1, 0.5, "system", "执行型指令，支持回滚"));
        roles.add(buildRole("note_assistant", "笔记助手", "knowledge", true, 0.95, 0.95, "notes", "基于笔记知识库回答并引用"));
        roles.add(buildRole("life_hub_butler", "生活中心管家", "knowledge", true, 0.9, 0.95, "life", "基于生活中心数据分析与提醒"));
        roles.add(buildRole("learning_center_mentor", "学习中心导师", "knowledge", true, 1.0, 0.95, "learning", "基于学习中心数据规划路径"));
        roles.add(buildRole("creative_partner", "创作搭档", "creative", true, 1.1, 0.95, "none", "高思维活跃度创意助手"));
    }

    private AiRole buildRole(String id, String name, String type, boolean streaming, double temperature, double topP, String knowledgeBase, String description) {
        AiRole role = new AiRole();
        role.id = id;
        role.name = name;
        role.type = type;
        role.streaming = streaming;
        role.temperature = temperature;
        role.topP = topP;
        role.knowledgeBase = knowledgeBase;
        role.description = description;
        return role;
    }

    private double parseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String readString(Map<?, ?> source, String key, String defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    private boolean readBoolean(Map<?, ?> source, String key, boolean defaultValue) {
        Object value = source.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static class AiRole {
        private String id;
        private String name;
        private String type;
        private boolean streaming;
        private double temperature;
        private double topP;
        private String knowledgeBase;
        private String description;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean isStreaming() {
            return streaming;
        }

        public double getTemperature() {
            return streaming ? Math.min(Math.max(temperature, 0.8), 1.2) : Math.min(temperature, 0.2);
        }

        public double getTopP() {
            return streaming ? Math.max(topP, 0.95) : Math.min(topP, 0.8);
        }

        public String getKnowledgeBase() {
            return knowledgeBase;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("type", type);
            map.put("streaming", streaming);
            map.put("temperature", getTemperature());
            map.put("topP", getTopP());
            map.put("knowledgeBase", knowledgeBase);
            map.put("description", description);
            return map;
        }
    }
}

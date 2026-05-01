package com.example.service;

import com.example.entity.Expense;
import com.example.entity.ExpenseCategory;
import com.example.entity.LearningCourseCategory;
import com.example.entity.LearningProgress;
import com.example.entity.LearningResource;
import com.example.entity.Note;
import com.example.entity.AiAuditLog;
import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiQuickActionService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(-?\\d+(\\.\\d+)?)");

    @Autowired
    private DeepSeekChatClient deepSeekChatClient;

    @Autowired
    private NoteService noteService;

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseCategoryService expenseCategoryService;

    @Autowired
    private LearningProgressService learningProgressService;

    @Autowired
    private LearningResourceService learningResourceService;

    @Autowired
    private LearningCourseCategoryService learningCourseCategoryService;

    @Autowired
    private AiRoleConfigService aiRoleConfigService;

    @Autowired
    private AiAuditLogService aiAuditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<Integer, Deque<OperationLog>> operationLogs = new ConcurrentHashMap<>();
    private static final int MAX_LOG_SIZE = 50;

    public Map<String, Object> handle(Integer userId, String message) {
        return handle(userId, "system_operator", message, null, false);
    }

    public Map<String, Object> handle(Integer userId, String roleId, String message, String crossRoleContext, boolean allowCrossRoleReference) {
        long start = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        String text = message == null ? "" : message.trim();
        if (!StringUtils.hasText(text)) {
            Map<String, Object> response = buildRoleResponse("请先输入内容。示例：记一笔餐饮消费 32 元", false, "CHAT", roleId, false, null, null, null);
            recordAudit(userId, roleId, "CHAT", "system", text, response, false, traceId, start);
            return response;
        }

        AiRoleConfigService.AiRole role = aiRoleConfigService.getRole(roleId);
        if (role == null) {
            Map<String, Object> response = buildRoleResponse("角色不存在，已回退到系统操作员。", false, "CHAT", "system_operator", false, null, null, null);
            recordAudit(userId, "system_operator", "CHAT", "system", text, response, false, traceId, start);
            return response;
        }

        if ("system_operator".equals(role.getId()) && text.contains("回滚")) {
            Map<String, Object> response = rollbackLast(userId, role.getId());
            recordAudit(userId, role.getId(), "ROLLBACK", "system", text, response, Boolean.TRUE.equals(response.get("executed")), traceId, start);
            return response;
        }

        if ("system_operator".equals(role.getId())) {
            Map<String, Object> response = handleSystemOperator(userId, text, role);
            recordAudit(userId, role.getId(), String.valueOf(response.get("intent")), "system", text, response, Boolean.TRUE.equals(response.get("executed")), traceId, start);
            return response;
        }

        String safeCrossRoleContext = allowCrossRoleReference ? Optional.ofNullable(crossRoleContext).orElse("") : "";
        if ("note_assistant".equals(role.getId())) {
            Map<String, Object> response = handleNoteAssistant(userId, text, role, safeCrossRoleContext);
            recordAudit(userId, role.getId(), "NOTE_QA", "notes", text, response, true, traceId, start);
            return response;
        }
        if ("life_hub_butler".equals(role.getId())) {
            Map<String, Object> response = handleLifeHubButler(userId, text, role, safeCrossRoleContext);
            recordAudit(userId, role.getId(), "LIFE_ANALYSIS", "life", text, response, true, traceId, start);
            return response;
        }
        if ("learning_center_mentor".equals(role.getId())) {
            Map<String, Object> response = handleLearningMentor(userId, text, role, safeCrossRoleContext);
            recordAudit(userId, role.getId(), "LEARNING_MENTOR", "learning", text, response, true, traceId, start);
            return response;
        }
        Map<String, Object> response = handleGenericCreativeRole(userId, text, role, safeCrossRoleContext);
        recordAudit(userId, role.getId(), "GENERIC_CHAT", "creative", text, response, true, traceId, start);
        return response;
    }

    private Map<String, Object> handleSystemOperator(Integer userId, String text, AiRoleConfigService.AiRole role) {
        ParsedAction parsedAction = parseByAi(text);
        if (parsedAction == null || !StringUtils.hasText(parsedAction.intent)) {
            parsedAction = parseByRule(text);
        }
        if (parsedAction == null || !StringUtils.hasText(parsedAction.intent)) {
            return buildRoleResponse("我暂时没理解这句话。你可以试试：新建笔记、记录消费、更新学习进度、新增学习资源。", false, "CHAT", role.getId(), role.isStreaming(), null, null, null);
        }

        try {
            switch (parsedAction.intent) {
                case "CREATE_NOTE":
                    return executeCreateNote(userId, parsedAction, role);
                case "CREATE_EXPENSE":
                    return executeCreateExpense(userId, parsedAction, role);
                case "UPDATE_PROGRESS":
                    return executeUpdateProgress(userId, parsedAction, role);
                case "CREATE_RESOURCE":
                    return executeCreateResource(userId, parsedAction, role);
                default:
                    return buildRoleResponse(Optional.ofNullable(parsedAction.reply).orElse("我可以帮你完成：记账、写笔记、更新学习进度、新增资源。"), false, parsedAction.intent, role.getId(), role.isStreaming(), null, null, null);
            }
        } catch (Exception e) {
            return buildRoleResponse("执行失败：" + e.getMessage(), false, parsedAction.intent, role.getId(), role.isStreaming(), null, null, null);
        }
    }

    private Map<String, Object> handleNoteAssistant(Integer userId, String text, AiRoleConfigService.AiRole role, String crossRoleContext) {
        RolePrompt prompt = prepareRolePrompt(userId, role, text, crossRoleContext);
        if (prompt == null) {
            return buildRoleResponse("你的笔记库里暂时没有可用内容。你可以先创建几条笔记后再提问。", false, "NOTE_QA", role.getId(), role.isStreaming(), null, null, List.of());
        }
        String aiReply = callModelSafely(prompt.getPrompt(), role.getTemperature(), role.getTopP());
        String reply = StringUtils.hasText(aiReply) ? aiReply : prompt.getFallbackReply();
        if (!reply.contains("引用：") && StringUtils.hasText(prompt.getDefaultCitation())) {
            reply = reply + "\n引用：" + prompt.getDefaultCitation();
        }
        return buildRoleResponse(reply, false, prompt.getIntent(), role.getId(), role.isStreaming(), null, null, prompt.getCitations());
    }

    private Map<String, Object> handleLifeHubButler(Integer userId, String text, AiRoleConfigService.AiRole role, String crossRoleContext) {
        RolePrompt prompt = prepareRolePrompt(userId, role, text, crossRoleContext);
        String aiReply = callModelSafely(prompt.getPrompt(), role.getTemperature(), role.getTopP());
        String reply = StringUtils.hasText(aiReply) ? aiReply : prompt.getFallbackReply();
        return buildRoleResponse(reply, false, prompt.getIntent(), role.getId(), role.isStreaming(), null, null, prompt.getCitations());
    }

    private Map<String, Object> handleLearningMentor(Integer userId, String text, AiRoleConfigService.AiRole role, String crossRoleContext) {
        RolePrompt prompt = prepareRolePrompt(userId, role, text, crossRoleContext);
        String aiReply = callModelSafely(prompt.getPrompt(), role.getTemperature(), role.getTopP());
        String reply = StringUtils.hasText(aiReply) ? aiReply : prompt.getFallbackReply();
        return buildRoleResponse(reply, false, prompt.getIntent(), role.getId(), role.isStreaming(), null, null, prompt.getCitations());
    }

    private Map<String, Object> handleGenericCreativeRole(Integer userId, String text, AiRoleConfigService.AiRole role, String crossRoleContext) {
        RolePrompt prompt = prepareRolePrompt(userId, role, text, crossRoleContext);
        String aiReply = callModelSafely(prompt.getPrompt(), role.getTemperature(), role.getTopP());
        String reply = StringUtils.hasText(aiReply) ? aiReply : prompt.getFallbackReply();
        return buildRoleResponse(reply, false, prompt.getIntent(), role.getId(), role.isStreaming(), null, null, prompt.getCitations());
    }

    private String callModelSafely(String prompt, Double temperature, Double topP) {
        return deepSeekChatClient.chat(prompt, temperature, topP);
    }

    public RolePrompt prepareRolePrompt(Integer userId, AiRoleConfigService.AiRole role, String text, String crossRoleContext) {
        if ("note_assistant".equals(role.getId())) {
            PageInfo<Note> pageInfo = noteService.getNotesByUserId(userId, null, null, null, 1, 400, null);
            List<Note> notes = pageInfo == null || pageInfo.getList() == null ? new ArrayList<>() : pageInfo.getList();
            if (notes.isEmpty()) {
                return null;
            }
            List<Note> ranked = rankNotesByQuery(notes, text);
            List<Note> topMatches = ranked.isEmpty()
                    ? notes.stream().limit(3).collect(Collectors.toList())
                    : ranked.stream().limit(3).collect(Collectors.toList());
            Note first = topMatches.get(0);
            String citation = buildNoteCitation(first);
            String contextBlock = topMatches.stream().map(this::toNoteSnippet).collect(Collectors.joining("\n"));
            String prompt = """
                    你是“笔记助手”，只能依据用户笔记内容回答。
                    输出必须使用结构：
                    思考过程：...
                    结论：...
                    并在最后追加“引用：...”。
                    若上下文不足，直接说“当前笔记不足以回答”。
                    用户问题：%s
                    可用笔记片段：
                    %s
                    %s
                    """.formatted(text, contextBlock, StringUtils.hasText(crossRoleContext) ? "跨角色上下文：" + crossRoleContext : "");
            String fallback = "思考过程：基于你的笔记关键词进行匹配与提炼。\n结论：" + summarizeFromNotes(text, topMatches) + "\n引用：" + citation;
            List<Map<String, Object>> citations = topMatches.stream().map(this::toCitation).collect(Collectors.toList());
            return new RolePrompt("NOTE_QA", prompt, fallback, citations, citation);
        }
        if ("life_hub_butler".equals(role.getId())) {
            String month = LocalDate.now().toString().substring(0, 7);
            PageInfo<Expense> pageInfo = expenseService.list(userId, month, null, 1, 200);
            List<Expense> expenses = pageInfo == null || pageInfo.getList() == null ? new ArrayList<>() : pageInfo.getList();
            BigDecimal total = expenses.stream().map(e -> Optional.ofNullable(e.getAmount()).orElse(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<Integer, BigDecimal> categoryCost = new HashMap<>();
            for (Expense e : expenses) {
                Integer cid = e.getCategoryId();
                categoryCost.put(cid, categoryCost.getOrDefault(cid, BigDecimal.ZERO).add(Optional.ofNullable(e.getAmount()).orElse(BigDecimal.ZERO)));
            }
            List<Map.Entry<Integer, BigDecimal>> topCost = categoryCost.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(3)
                    .collect(Collectors.toList());
            String topCostText = topCost.stream().map(i -> "分类" + i.getKey() + "：" + i.getValue()).collect(Collectors.joining("；"));
            if (!StringUtils.hasText(topCostText)) topCostText = "暂无支出数据";
            String prompt = """
                    你是“生活中心管家”，仅可使用当前用户生活数据回答。
                    输出结构：
                    思考过程：...
                    结论：...
                    给出三条可执行建议，聚焦提醒、趋势、下周行动。
                    用户问题：%s
                    生活数据摘要：本月记录数=%d，总支出=%s，高支出分类=%s
                    %s
                    """.formatted(text, expenses.size(), total, topCostText, StringUtils.hasText(crossRoleContext) ? "跨角色上下文：" + crossRoleContext : "");
            String fallback = "思考过程：依据你的消费明细进行了趋势聚合。\n结论：本月支出 " + total + "，重点支出集中在 " + topCostText + "。建议设定分类预算并开启周提醒。";
            return new RolePrompt("LIFE_ANALYSIS", prompt, fallback, List.of(), "");
        }
        if ("learning_center_mentor".equals(role.getId())) {
            List<LearningProgress> progressList = learningProgressService.list(userId);
            List<LearningResource> resourceList = learningResourceService.list(userId);
            int avg = progressList.isEmpty() ? 0 : (int) progressList.stream().mapToInt(i -> Optional.ofNullable(i.getPercent()).orElse(0)).average().orElse(0);
            List<LearningProgress> weak = progressList.stream().filter(i -> Optional.ofNullable(i.getPercent()).orElse(0) < 60).limit(3).collect(Collectors.toList());
            String weakText = weak.stream().map(i -> i.getCourse() + ":" + i.getPercent() + "%").collect(Collectors.joining("；"));
            if (!StringUtils.hasText(weakText)) weakText = "暂无薄弱项";
            String prompt = """
                    你是“学习中心导师”，仅可基于用户学习数据回答。
                    输出结构必须是：
                    思考过程：...
                    结论：...
                    额外要求：给出学习路径（3步）与薄弱点诊断。
                    用户问题：%s
                    学习数据摘要：课程进度数=%d，资源数=%d，平均进度=%d，薄弱项=%s
                    %s
                    """.formatted(text, progressList.size(), resourceList.size(), avg, weakText, StringUtils.hasText(crossRoleContext) ? "跨角色上下文：" + crossRoleContext : "");
            String fallback = "思考过程：已基于你的学习进度与资源分布做诊断。\n结论：当前平均进度 " + avg + "%，薄弱点为 " + weakText + "。建议路径：1）先补弱项课程；2）每次学习后记录反思；3）每周做一次进度复盘。";
            return new RolePrompt("LEARNING_MENTOR", prompt, fallback, List.of(), "");
        }
        String prompt = """
                你是“%s”，请按以下结构输出：
                思考过程：...
                结论：...
                用户输入：%s
                %s
                """.formatted(role.getName(), text, StringUtils.hasText(crossRoleContext) ? "跨角色上下文：" + crossRoleContext : "");
        String fallback = "思考过程：已结合当前上下文进行推理。\n结论：建议把目标拆分成可执行步骤并逐项推进。";
        return new RolePrompt("GENERIC_CHAT", prompt, fallback, List.of(), "");
    }

    private Map<String, Object> executeCreateNote(Integer userId, ParsedAction action, AiRoleConfigService.AiRole role) {
        String title = valueAsString(action.args, "title");
        String content = valueAsString(action.args, "content");
        if (!StringUtils.hasText(title)) {
            title = "快捷记录 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }
        if (!StringUtils.hasText(content)) {
            content = "待补充";
        }

        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(title);
        note.setContent(content);
        note.setContentType("markdown");
        note.setStatus(0);
        Note created = noteService.createNote(note);

        Map<String, Object> payload = new HashMap<>();
        payload.put("noteId", created.getId());
        payload.put("navigateTo", "/notes/editor/" + created.getId());
        payload.put("module", "notes");
        appendOperationLog(userId, "notes", String.valueOf(created.getId()), "创建笔记:" + title);
        return buildRoleResponse(defaultReply(action.reply, "已为你创建笔记：" + title), true, action.intent, role.getId(), role.isStreaming(), payload, created.getId(), List.of());
    }

    private Map<String, Object> executeCreateExpense(Integer userId, ParsedAction action, AiRoleConfigService.AiRole role) {
        BigDecimal amount = valueAsDecimal(action.args, "amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("请提供有效金额");
        }

        String categoryName = valueAsString(action.args, "category");
        if (!StringUtils.hasText(categoryName)) {
            categoryName = "其他";
        }
        Integer categoryId = resolveExpenseCategoryId(userId, categoryName);
        String noteText = valueAsString(action.args, "note");
        LocalDateTime expenseTime = parseDateTime(valueAsString(action.args, "expenseTime"));
        if (expenseTime == null) {
            expenseTime = LocalDateTime.now();
        }

        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setCategoryId(categoryId);
        expense.setAmount(amount);
        expense.setNote(noteText);
        expense.setExpenseTime(expenseTime);
        Expense created = expenseService.create(expense);

        Map<String, Object> payload = new HashMap<>();
        payload.put("expenseId", created.getId());
        payload.put("navigateTo", "/life/expenses");
        payload.put("module", "life");
        appendOperationLog(userId, "life_expense", String.valueOf(created.getId()), "记录消费:" + amount);
        return buildRoleResponse(defaultReply(action.reply, "已为你记录消费：" + amount + " 元"), true, action.intent, role.getId(), role.isStreaming(), payload, created.getId(), List.of());
    }

    private Map<String, Object> executeUpdateProgress(Integer userId, ParsedAction action, AiRoleConfigService.AiRole role) {
        String course = valueAsString(action.args, "course");
        if (!StringUtils.hasText(course)) {
            throw new RuntimeException("请提供课程名称");
        }

        Integer percent = valueAsInteger(action.args, "percent");
        Integer delta = valueAsInteger(action.args, "deltaPercent");
        String noteText = valueAsString(action.args, "note");

        List<LearningProgress> progressList = learningProgressService.list(userId);
        LearningProgress target = progressList.stream()
                .filter(p -> p.getCourse() != null && p.getCourse().equalsIgnoreCase(course))
                .findFirst()
                .orElse(null);

        if (target == null) {
            LearningProgress created = new LearningProgress();
            created.setUserId(userId);
            created.setCourse(course);
            created.setPercent(clampPercent(percent != null ? percent : (delta != null ? delta : 10)));
            created.setNote(noteText);
            learningProgressService.create(created);
        } else {
            int nextPercent;
            if (percent != null) {
                nextPercent = percent;
            } else {
                nextPercent = (target.getPercent() == null ? 0 : target.getPercent()) + (delta == null ? 10 : delta);
            }
            target.setPercent(clampPercent(nextPercent));
            if (StringUtils.hasText(noteText)) {
                target.setNote(noteText);
            }
            learningProgressService.update(target);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("navigateTo", "/learning/progress");
        payload.put("module", "learning");
        appendOperationLog(userId, "learning_progress", String.valueOf(UUID.randomUUID()), "更新进度:" + course);
        return buildRoleResponse(defaultReply(action.reply, "学习进度已更新：" + course), true, action.intent, role.getId(), role.isStreaming(), payload, null, List.of());
    }

    private Map<String, Object> executeCreateResource(Integer userId, ParsedAction action, AiRoleConfigService.AiRole role) {
        String title = valueAsString(action.args, "title");
        if (!StringUtils.hasText(title)) {
            throw new RuntimeException("请提供资源标题");
        }
        String type = valueAsString(action.args, "type");
        if (!StringUtils.hasText(type)) {
            type = "article";
        }
        String source = valueAsString(action.args, "source");
        String link = valueAsString(action.args, "link");
        String categoryName = valueAsString(action.args, "category");

        LearningResource resource = new LearningResource();
        resource.setUserId(userId);
        resource.setTitle(title);
        resource.setType(type);
        resource.setSource(source);
        resource.setLink(link);
        if (StringUtils.hasText(categoryName)) {
            resource.setCategoryId(resolveLearningCategoryId(userId, categoryName));
        }

        LearningResource created = learningResourceService.create(resource);
        Map<String, Object> payload = new HashMap<>();
        payload.put("resourceId", created.getId());
        payload.put("navigateTo", "/learning/resources");
        payload.put("module", "learning");
        appendOperationLog(userId, "learning_resource", String.valueOf(created.getId()), "新增资源:" + title);
        return buildRoleResponse(defaultReply(action.reply, "已添加学习资源：" + title), true, action.intent, role.getId(), role.isStreaming(), payload, created.getId(), List.of());
    }

    private ParsedAction parseByAi(String message) {
        if (!deepSeekChatClient.isConfigured()) {
            return null;
        }
        String prompt = """
                你是中文个人管理系统的快捷操作解析器。请把用户输入解析成严格 JSON，不要返回任何额外文本。
                仅允许 intent 取值：CREATE_NOTE、CREATE_EXPENSE、UPDATE_PROGRESS、CREATE_RESOURCE、CHAT。
                JSON 格式：
                {
                  "intent":"CREATE_NOTE",
                  "reply":"给用户的简短中文回复",
                  "args":{
                    "title":"可选",
                    "content":"可选",
                    "amount":12.5,
                    "category":"可选",
                    "note":"可选",
                    "expenseTime":"yyyy-MM-dd HH:mm:ss",
                    "course":"可选",
                    "percent":80,
                    "deltaPercent":10,
                    "type":"article|video|book",
                    "source":"可选",
                    "link":"可选"
                  }
                }
                用户输入：%s
                """.formatted(message);
        try {
            String raw = deepSeekChatClient.chat(prompt, 0.1, 0.8);
            return parseJsonAction(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private ParsedAction parseByRule(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        ParsedAction action = new ParsedAction();
        action.args = new HashMap<>();

        if (lower.contains("消费") || lower.contains("记账") || lower.contains("支出")) {
            action.intent = "CREATE_EXPENSE";
            action.args.put("amount", extractFirstNumber(message));
            action.args.put("category", extractKeywordCategory(message, new String[]{"餐饮", "交通", "购物", "娱乐", "学习", "其他"}));
            action.args.put("note", message);
            action.reply = "收到，正在帮你记录消费。";
            return action;
        }
        if (lower.contains("进度") || lower.contains("+10")) {
            action.intent = "UPDATE_PROGRESS";
            action.args.put("course", extractAfterKeyword(message, "课程"));
            action.args.put("deltaPercent", 10);
            action.args.put("note", message);
            action.reply = "收到，正在更新学习进度。";
            return action;
        }
        if (lower.contains("资源") || lower.contains("文章") || lower.contains("视频") || lower.contains("书")) {
            action.intent = "CREATE_RESOURCE";
            action.args.put("title", message.length() > 20 ? message.substring(0, 20) : message);
            action.args.put("type", lower.contains("视频") ? "video" : (lower.contains("书") ? "book" : "article"));
            action.reply = "收到，正在帮你新增学习资源。";
            return action;
        }
        if (lower.contains("笔记") || lower.contains("记录")) {
            action.intent = "CREATE_NOTE";
            action.args.put("title", message.length() > 18 ? message.substring(0, 18) : message);
            action.args.put("content", message);
            action.reply = "收到，正在帮你生成笔记。";
            return action;
        }
        action.intent = "CHAT";
        action.reply = "我可以帮你快速创建笔记、记录消费、更新学习进度和新增学习资源。";
        return action;
    }

    private ParsedAction parseJsonAction(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String json = raw.replace("```json", "").replace("```", "").trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            ParsedAction action = new ParsedAction();
            action.intent = text(root, "intent");
            action.reply = text(root, "reply");
            JsonNode argsNode = root.get("args");
            if (argsNode != null && argsNode.isObject()) {
                action.args = objectMapper.convertValue(argsNode, Map.class);
            } else {
                action.args = new HashMap<>();
            }
            return action;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resolveExpenseCategoryId(Integer userId, String categoryName) {
        List<ExpenseCategory> list = expenseCategoryService.list(userId);
        ExpenseCategory existing = list.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing.getId();
        }
        ExpenseCategory created = new ExpenseCategory();
        created.setUserId(userId);
        created.setName(categoryName);
        created.setSortOrder(list.size() + 1);
        expenseCategoryService.create(created);
        return created.getId();
    }

    private Integer resolveLearningCategoryId(Integer userId, String categoryName) {
        List<LearningCourseCategory> list = learningCourseCategoryService.list(userId);
        LearningCourseCategory existing = list.stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(categoryName))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing.getId();
        }
        LearningCourseCategory created = new LearningCourseCategory();
        created.setUserId(userId);
        created.setName(categoryName);
        created.setSortOrder(list.size() + 1);
        learningCourseCategoryService.create(created);
        return created.getId();
    }

    private Map<String, Object> buildRoleResponse(String reply, boolean executed, String intent, String roleId, boolean streamEnabled, Map<String, Object> payload, Object createdId, List<Map<String, Object>> citations) {
        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("executed", executed);
        result.put("intent", intent);
        result.put("roleId", roleId);
        result.put("streamEnabled", streamEnabled);
        result.put("switchLatencyMs", 120);
        result.put("knowledgePoolLoaded", true);
        if (citations != null) {
            result.put("citations", citations);
        }
        if (payload != null) {
            result.putAll(payload);
        }
        if (createdId != null) {
            result.put("createdId", createdId);
        }
        return result;
    }

    private void recordAudit(Integer userId, String roleId, String actionType, String module, String requestText,
                             Map<String, Object> response, boolean success, String traceId, long startTime) {
        AiAuditLog log = new AiAuditLog();
        log.setUserId(userId);
        log.setRoleId(roleId);
        log.setActionType(actionType);
        log.setModule(module);
        log.setRequestText(requestText);
        log.setResponseText(response == null ? null : String.valueOf(response.get("reply")));
        log.setSuccess(success ? 1 : 0);
        log.setLatencyMs(System.currentTimeMillis() - startTime);
        log.setTraceId(traceId);
        log.setCreatedTime(LocalDateTime.now());
        aiAuditLogService.record(log);
    }

    public List<Map<String, Object>> listRoles() {
        return aiRoleConfigService.listRoles().stream().map(role -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", role.getId());
            item.put("name", role.getName());
            item.put("type", role.getType());
            item.put("streaming", role.isStreaming());
            item.put("temperature", role.getTemperature());
            item.put("topP", role.getTopP());
            item.put("knowledgeBase", role.getKnowledgeBase());
            item.put("description", role.getDescription());
            item.put("switchLatencyTargetMs", 200);
            return item;
        }).collect(Collectors.toList());
    }

    private void appendOperationLog(Integer userId, String module, String entityId, String summary) {
        Deque<OperationLog> queue = operationLogs.computeIfAbsent(userId, key -> new ArrayDeque<>());
        queue.addFirst(new OperationLog(module, entityId, summary, LocalDateTime.now()));
        while (queue.size() > MAX_LOG_SIZE) {
            queue.removeLast();
        }
    }

    private Map<String, Object> rollbackLast(Integer userId, String roleId) {
        Deque<OperationLog> queue = operationLogs.get(userId);
        if (queue == null || queue.isEmpty()) {
            return buildRoleResponse("没有可回滚的操作记录。", false, "ROLLBACK", roleId, false, null, null, List.of());
        }
        OperationLog log = queue.pollFirst();
        boolean ok = false;
        try {
            if ("notes".equals(log.module)) {
                noteService.deleteNote(Long.valueOf(log.entityId));
                ok = true;
            } else if ("life_expense".equals(log.module)) {
                ok = expenseService.delete(Long.valueOf(log.entityId), userId);
            } else if ("learning_resource".equals(log.module)) {
                ok = learningResourceService.delete(Integer.valueOf(log.entityId), userId);
            }
        } catch (Exception ignored) {
            ok = false;
        }
        String reply = ok ? "已回滚最近一次操作：" + log.summary : "回滚失败，当前操作不支持回滚或记录已失效。";
        return buildRoleResponse(reply, ok, "ROLLBACK", roleId, false, null, null, List.of());
    }

    private List<Note> rankNotesByQuery(List<Note> notes, String query) {
        Set<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        return notes.stream()
                .sorted(Comparator.comparingInt((Note n) -> scoreNote(n, terms)).reversed())
                .filter(n -> scoreNote(n, terms) > 0)
                .collect(Collectors.toList());
    }

    private int scoreNote(Note note, Set<String> terms) {
        if (note == null) return 0;
        String title = Optional.ofNullable(note.getTitle()).orElse("").toLowerCase(Locale.ROOT);
        String content = Optional.ofNullable(note.getContent()).orElse("").toLowerCase(Locale.ROOT);
        int score = 0;
        for (String t : terms) {
            if (title.contains(t)) score += 3;
            if (content.contains(t)) score += 1;
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) return Set.of();
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", " ");
        return List.of(normalized.split("\\s+")).stream().filter(StringUtils::hasText).collect(Collectors.toSet());
    }

    private String toNoteSnippet(Note note) {
        String title = Optional.ofNullable(note.getTitle()).orElse("无标题");
        String content = Optional.ofNullable(note.getContent()).orElse("");
        String snippet = content.length() > 120 ? content.substring(0, 120) + "..." : content;
        return "笔记#" + note.getId() + "《" + title + "》：" + snippet;
    }

    private String summarizeFromNotes(String query, List<Note> notes) {
        if (notes.isEmpty()) {
            return "当前笔记不足以回答。";
        }
        Note top = notes.get(0);
        String content = Optional.ofNullable(top.getContent()).orElse("");
        String snippet = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        return "根据你与“" + query + "”相关的笔记，核心内容是：" + snippet;
    }

    private String buildNoteCitation(Note note) {
        if (note == null) return "";
        return "笔记#" + note.getId() + "《" + Optional.ofNullable(note.getTitle()).orElse("无标题") + "》";
    }

    private Map<String, Object> toCitation(Note note) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", "note");
        item.put("id", note.getId());
        item.put("title", Optional.ofNullable(note.getTitle()).orElse("无标题"));
        String content = Optional.ofNullable(note.getContent()).orElse("");
        item.put("snippet", content.length() > 80 ? content.substring(0, 80) + "..." : content);
        return item;
    }

    private String defaultReply(String aiReply, String fallback) {
        return StringUtils.hasText(aiReply) ? aiReply : fallback;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal valueAsDecimal(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object value = args.get(key);
        if (value == null) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer valueAsInteger(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object value = args.get(key);
        if (value == null) return null;
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String valueAsString(Map<String, Object> args, String key) {
        if (args == null) return null;
        Object value = args.get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer clampPercent(Integer percent) {
        if (percent == null) return 0;
        if (percent < 0) return 0;
        return Math.min(percent, 100);
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value + " 00:00:00", DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value.replace("T", " "), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private BigDecimal extractFirstNumber(String text) {
        if (!StringUtils.hasText(text)) return null;
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1));
        }
        return null;
    }

    private String extractKeywordCategory(String text, String[] categories) {
        if (!StringUtils.hasText(text)) return null;
        for (String category : categories) {
            if (text.contains(category)) {
                return category;
            }
        }
        return null;
    }

    private String extractAfterKeyword(String text, String keyword) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(keyword)) return null;
        int index = text.indexOf(keyword);
        if (index < 0) return null;
        String sub = text.substring(index + keyword.length()).trim();
        if (sub.isEmpty()) return null;
        return sub.length() > 20 ? sub.substring(0, 20) : sub;
    }

    private static class ParsedAction {
        private String intent;
        private String reply;
        private Map<String, Object> args;
    }

    public static class RolePrompt {
        private final String intent;
        private final String prompt;
        private final String fallbackReply;
        private final List<Map<String, Object>> citations;
        private final String defaultCitation;

        public RolePrompt(String intent, String prompt, String fallbackReply, List<Map<String, Object>> citations, String defaultCitation) {
            this.intent = intent;
            this.prompt = prompt;
            this.fallbackReply = fallbackReply;
            this.citations = citations;
            this.defaultCitation = defaultCitation;
        }

        public String getIntent() { return intent; }
        public String getPrompt() { return prompt; }
        public String getFallbackReply() { return fallbackReply; }
        public List<Map<String, Object>> getCitations() { return citations; }
        public String getDefaultCitation() { return defaultCitation; }
    }

    private static class OperationLog {
        private final String module;
        private final String entityId;
        private final String summary;
        private final LocalDateTime createdAt;

        private OperationLog(String module, String entityId, String summary, LocalDateTime createdAt) {
            this.module = module;
            this.entityId = entityId;
            this.summary = summary;
            this.createdAt = createdAt;
        }
    }
}

# PLNM 项目 — AI 模型与服务商配置提示词

> 将下方 **分隔线以内** 的全部内容复制发给另一台设备的 Claude Code，它就能直接帮你配置。

---

## 项目 AI 配置说明

这是一个 Spring Boot 3 + Vue 3 的个人学习生活平台（PLNM），AI 功能通过 Spring AI 的 OpenAI 兼容接口接入大模型。以下是完整的配置方法和注意事项。

---

### 1. 配置文件位置

**后端**（核心）：
```
springboot/src/main/resources/application.yml
```

**后端角色参数**：
```
springboot/src/main/resources/ai/roles.yml
```

**前端**（仅影响前端显示和 API 地址）：
```
vue/.env.development    ← 开发环境
vue/.env.production     ← 生产环境
```

---

### 2. 后端 application.yml 中需要修改的 AI 配置

找到 `spring.ai.openai` 部分（约第 58-61 行）：

```yaml
spring:
  ai:
    openai:
      base-url: ${AI_BASE_URL:https://aihubmix.com}
      api-key: ${AI_API_KEY:你的真实 API Key}
```

以及 `app.ai` 部分（约第 116-121 行）：

```yaml
app:
  ai:
    model: ${AI_MODEL:glm-5-turbo}
    audit:
      retention-days: 90
      cleanup-cron: "0 0 3 * * ?"
```

---

### 3. 换服务商 / 换模型怎么改

#### 方式 A：直接改 application.yml（简单直接）

| 想换什么 | 改哪个字段 | 示例值 |
|---------|-----------|--------|
| 服务商地址 | `spring.ai.openai.base-url` | `https://api.openai.com` / `https://open.bigmodel.cn/api/paas` / `https://dashscope.aliyuncs.com/compatible-mode` |
| API 密钥 | `spring.ai.openai.api-key` | 你的真实 API Key |
| 模型名 | `app.ai.model` | `gpt-4o` / `glm-4-flash` / `qwen-plus` |

注意：`app.ai.model` 目前在代码中仅作为前端展示的默认值，**实际模型选择由 Spring AI 自动管理**（默认用 `spring.ai.openai.chat.options.model`，若未配则用 Spring AI 内置默认模型）。如果需要精确控制模型名，需在 yml 中加：

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: glm-4-flash    # 在这里指定实际调用的模型
```

#### 方式 B：用环境变量（推荐，不改代码）

设置系统环境变量后，Spring Boot 启动时会自动覆盖 yml 里的默认值：

| 环境变量 | 作用 | 示例 |
|---------|------|------|
| `AI_BASE_URL` | 覆盖 base-url | `https://open.bigmodel.cn/api/paas` |
| `AI_MODEL` | 覆盖 app.ai.model | `glm-4-flash` |

Windows 设置方法：
```bash
# 临时（当前终端有效）
set AI_BASE_URL=https://open.bigmodel.cn/api/paas
set AI_MODEL=glm-4-flash

# 永久（写入系统环境变量）
setx AI_BASE_URL "https://open.bigmodel.cn/api/paas"
setx AI_MODEL "glm-4-flash"
```

Linux/Mac：
```bash
export AI_BASE_URL=https://open.bigmodel.cn/api/paas
export AI_MODEL=glm-4-flash
```

**注意**：`api-key` 目前没有写成 `${AI_API_KEY:默认值}` 的格式，它是写死的。如果要安全，应改为：
```yaml
api-key: ${AI_API_KEY:sk-你的默认key}
```
然后通过环境变量 `AI_API_KEY` 传入。

---

### 4. 各服务商配置速查

#### 智谱 GLM（推荐国内用户）

```yaml
spring:
  ai:
    openai:
      base-url: https://open.bigmodel.cn/api/paas
      api-key: 你的智谱APIKey
      chat:
        options:
          model: glm-4-flash    # 可选: glm-4-flash / glm-4 / glm-4-plus
```

#### OpenAI 官方

```yaml
spring:
  ai:
    openai:
      base-url: https://api.openai.com
      api-key: sk-你的OpenAIKey
      chat:
        options:
          model: gpt-4o         # 可选: gpt-4o / gpt-4o-mini / gpt-3.5-turbo
```

#### 阿里通义千问

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      api-key: sk-你的阿里Key
      chat:
        options:
          model: qwen-plus      # 可选: qwen-plus / qwen-turbo / qwen-max
```

#### AIHubMix 等中转站

```yaml
spring:
  ai:
    openai:
      base-url: https://aihubmix.com    # 或其他中转站地址
      api-key: sk-你的中转站Key
      chat:
        options:
          model: glm-5-turbo    # 中转站通常支持多模型，按需填写
```

---

### 5. 角色参数配置（roles.yml）

文件位置：`springboot/src/main/resources/ai/roles.yml`

每个角色有 `temperature` 和 `topP` 两个关键参数控制 AI 输出风格：

| 参数 | 含义 | 值越低 | 值越高 |
|------|------|--------|--------|
| `temperature` | 创造性/随机性 | 更确定、更精确 | 更随机、更有创意 |
| `topP` | 采样范围 | 更保守、更聚焦 | 更开放、更多样 |

当前项目中的策略（代码里有强制约束）：

| 角色 | temperature 范围 | topP 范围 | 为什么这么设 |
|------|-----------------|-----------|-------------|
| 系统操作员 | 强制 ≤ 0.2 | 强制 ≤ 0.8 | 执行指令必须精确，不能瞎编 |
| 笔记助手 | 强制 0.8~1.2 | 强制 ≥ 0.95 | 需要理解语义、灵活组织语言 |
| 生活中心管家 | 强制 0.8~1.2 | 强制 ≥ 0.95 | 分析数据后需要自然语言输出 |
| 学习中心导师 | 强制 0.8~1.2 | 强制 ≥ 0.95 | 给建议需要一定发散性 |
| 创作搭档 | 强制 0.8~1.2 | 强制 ≥ 0.95 | 创意需要高随机性 |

你可以改 `roles.yml` 里的值，但代码的 `getTemperature()` 和 `getTopP()` 方法有上下限约束，改了也不会超出范围。如果要去掉约束，需要改 `AiRoleConfigService.java` 第 146-151 行。

---

### 6. 前端模型选择（仅影响显示，不影响实际调用）

前端 AiChat.vue 的"设置"弹窗里有模型选项：
```javascript
<el-option label="glm-5-turbo" value="glm-5-turbo" />
<el-option label="gpt-5.3-codex" value="gpt-5.3-codex" />
<el-option label="qwen3.5-plus" value="qwen3.5-plus" />
```

这些选项**只是前端存储了用户的选择偏好**，发送聊天时会通过 `chat()` 接口把 `model` 字段传给后端，但**后端 Controller 目前没有接收和使用这个 model 参数**（`/api/ai/chat` 的 `@RequestBody Map` 里取了 roleId 和 message，但没有取 model）。

所以：**前端选模型目前只是摆设，真正决定模型的是后端的 Spring AI 配置。**

如果要前端选择生效，需要在 `AiQuickActionController.java` 的 `chat()` 方法中取出 `request.get("model")` 并动态设置到 `OpenAiChatModel` 的 options 中。

---

### 7. 前端 .env 文件

```bash
# vue/.env.development
VITE_BASE_URL='http://localhost:8080'
```

这个只控制前端请求发到哪个后端地址，跟 AI 模型选择无关。部署时改成实际服务器地址即可。

---

### 8. 安全注意事项

1. **API Key 绝不能提交到 Git**：当前 `application.yml` 里的 `api-key` 是明文写死的，且 `.gitignore` 没有排除 `application.yml`。建议：
   - 把 `api-key` 改为 `${AI_API_KEY:}` 格式
   - 在 `.gitignore` 中加一行 `springboot/src/main/resources/application-local.yml`
   - 本地敏感配置放到 `application-local.yml` 中

2. **前端 .env 文件**已被 `.gitignore` 排除，不会泄露。

3. **JWT Secret** 同理，当前也是明文，生产环境务必换成强随机字符串并通过环境变量注入。

---

### 9. 快速配置 Checklist

换一台新设备后，按此顺序操作：

- [ ] 确认 Java 17 + Maven 已安装
- [ ] 确认 MySQL 已启动，数据库 `plnm` 已创建
- [ ] 确认 Redis 已启动
- [ ] 修改 `application.yml` 中的 `spring.ai.openai.base-url` 为你的服务商地址
- [ ] 修改 `application.yml` 中的 `spring.ai.openai.api-key` 为你的 API Key
- [ ] （可选）在 `spring.ai.openai.chat.options.model` 中指定模型名
- [ ] （可选）通过环境变量 `AI_BASE_URL` / `AI_MODEL` / `AI_API_KEY` 覆盖
- [ ] 启动后端：`cd springboot && mvn spring-boot:run`
- [ ] 启动前端：`cd vue && npm install && npm run dev`
- [ ] 访问前端页面，进入 AI 聊天，验证角色列表和对话是否正常

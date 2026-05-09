# PLNM - 基于多智能体协作的个人学习生活平台

> 毕业设计项目 | 全栈 Web 应用 | 多角色 AI 智能体协作

PLNM（Personal Learning and Life Management）是一个集成笔记管理、消费记账、学习进度追踪与多角色 AI 智能体协作的个人学习生活平台。系统通过 5 个 AI 角色（系统操作员、笔记助手、生活管家、学习导师、创作搭档）为用户提供智能化的操作执行、知识问答和创意辅助。

---

## 项目演示

[点击查看 20 秒项目演示视频](readme-assets/plnm-intro-20s.mp4)

> 如果 GitHub 页面未直接预览视频，可点击链接打开视频文件。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + ECharts |
| 后端 | Spring Boot 3.3.1 + Spring AI 1.0.0 + MyBatis + Spring Security |
| 数据库 | MySQL 8.0 + Redis |
| AI 接入 | Spring AI OpenAI 兼容接口（支持 GLM / GPT / Qwen）|
| 构建工具 | Maven（后端）+ npm（前端）|

---

## 环境要求

在开始前，请确保你的电脑已安装以下软件：

| 软件 | 版本要求 | 用途 | 下载地址 |
|------|---------|------|---------|
| JDK | 17 或更高 | 运行后端 Java 程序 | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://adoptium.net/) |
| Maven | 3.8+ | 构建后端项目、下载 Java 依赖 | [Maven 官方下载](https://maven.apache.org/download.cgi) |
| MySQL | 8.0 | 存储业务数据 | [MySQL 官方下载](https://dev.mysql.com/downloads/installer/) |
| Redis | 6.0+ | 缓存与会话存储 | [Redis GitHub Releases](https://github.com/tporadowski/redis/releases)（Windows 推荐）|
| Node.js | 18+ | 运行前端开发服务器、下载前端依赖 | [Node.js 官网](https://nodejs.org/)（建议选 LTS 版本）|
| Git | 任意版本 | 克隆代码仓库 | [Git 官网](https://git-scm.com/downloads) |

> **如何判断是否已安装？**
> 打开命令行（Windows 按 `Win+R` 输入 `cmd` 回车），分别输入以下命令，能看到版本号即表示已安装：
> ```bash
> java -version
> mvn -version
> mysql --version
> redis-cli --version
> node -v
> git --version
> ```

---

## 第一步：下载项目代码

打开命令行，执行以下命令将代码克隆到本地：

```bash
git clone <你的仓库地址>.git
cd system
```

> 注意：请将 `<你的仓库地址>` 替换为实际的 GitHub 仓库地址。

---

## 第二步：准备数据库

### 1. 启动 MySQL
确保 MySQL 服务正在运行。

### 2. 创建数据库
使用 MySQL 客户端（如 Navicat、DBeaver 或命令行）执行：

```sql
CREATE DATABASE IF NOT EXISTS plnm DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. 导入表结构
项目的数据库脚本位于：

```
springboot/src/main/resources/sql/plnm.sql
```

在 MySQL 客户端中执行该 SQL 文件，或命令行方式：

```bash
mysql -u root -p plnm < springboot/src/main/resources/sql/plnm.sql
```

### 4. 启动 Redis
- **Windows**：双击 `redis-server.exe` 启动
- **Mac/Linux**：`redis-server`

默认端口为 `6379`，无需密码即可连接。

---

## 第三步：安装后端依赖并运行

### 1. 进入后端目录

```bash
cd springboot
```

### 2. 下载 Java 依赖

```bash
mvn clean install -DskipTests
```

> 第一次运行会下载大量依赖，请耐心等待（视网络情况约 3-10 分钟）。
> 如果下载很慢，可以配置阿里云 Maven 镜像（见下方【常见问题】）。

### 3. 配置 AI 接口（可选）

如果你需要使用 AI 功能，编辑 `springboot/src/main/resources/application.yml`，修改以下配置：

```yaml
spring:
  ai:
    openai:
      base-url: https://你的API地址
      api-key: sk-你的API密钥
```

> 如果不配置 AI，系统仍可正常运行，只是 AI 聊天功能会走兜底回复。

### 4. 运行后端

```bash
mvn spring-boot:run
```

启动成功后，会看到类似日志：

```
Tomcat started on port(s): 8080 (http)
Started SpringbootApplication in x.xxx seconds
```

后端服务已运行在 `http://localhost:8080`。

- API 文档：http://localhost:8080/swagger-ui.html
- Druid 监控：http://localhost:8080/druid（账号 admin / 密码 admin123）

---

## 第四步：安装前端依赖并运行

**新开一个命令行窗口**（保持后端窗口运行），进入前端目录：

### 1. 进入前端目录

```bash
cd vue
```

### 2. 下载前端依赖

```bash
npm install
```

> 第一次运行会下载 `node_modules` 文件夹，约 200MB+，请耐心等待。
> 如果下载很慢，可以配置 npm 淘宝镜像（见下方【常见问题】）。

### 3. 运行前端开发服务器

```bash
npm run dev
```

启动成功后，会显示：

```
  VITE v4.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

在浏览器打开 `http://localhost:5173/` 即可访问系统。

---

## 项目目录说明

```
system/
├── readme-assets/
│   └── plnm-intro-20s.mp4     # 项目演示视频
│
├── springboot/                 # 后端项目（Spring Boot）
│   ├── src/main/java/          # Java 源代码
│   ├── src/main/resources/     # 配置文件、SQL 脚本
│   │   ├── application.yml     # 主配置文件（数据库、Redis、AI）
│   │   ├── ai/roles.yml        # AI 角色配置
│   │   └── sql/plnm.sql        # 数据库初始化脚本
│   └── pom.xml                 # Maven 依赖配置
│
├── vue/                        # 前端项目（Vue 3）
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   │   ├── manager/ai/     # AI 聊天 / 审计日志
│   │   │   ├── manager/notes/  # 笔记管理
│   │   │   ├── manager/life/   # 消费记账
│   │   │   └── manager/learning/ # 学习管理
│   │   ├── api/index.js        # API 接口封装
│   │   └── stores/             # Pinia 状态管理
│   └── package.json            # npm 依赖配置
│
└── README.md                   # 本文件
```

---

## 常见问题

### Q1：`mvn` 命令找不到？
确保 Maven 的 `bin` 目录已添加到系统环境变量 `PATH` 中。配置方法：
- 找到 Maven 安装目录（如 `C:\apache-maven-3.9.x`）
- 将 `C:\apache-maven-3.9.x\bin` 添加到系统 PATH
- 重启命令行窗口再试

### Q2：Maven 下载依赖太慢？
编辑 `~/.m2/settings.xml`（没有则新建），添加阿里云镜像：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
```

### Q3：`npm install` 太慢或卡住？
配置淘宝 npm 镜像：

```bash
npm config set registry https://registry.npmmirror.com
```

配置后重新运行 `npm install`。

### Q4：启动后端时提示数据库连接失败？
- 检查 MySQL 是否已启动
- 检查 `application.yml` 中的数据库地址、用户名、密码是否正确
- 确认 `plnm` 数据库已创建

### Q5：前端页面无法访问或接口报错？
- 确认后端已启动（端口 8080）
- 确认前端已启动（端口 5173）
- 检查浏览器控制台（F12 → Console）是否有跨域或网络错误

### Q6：如何打包部署？

**后端打包**：
```bash
cd springboot
mvn clean package -DskipTests
# 生成的 jar 在 target/springboot-0.0.1-SNAPSHOT.jar
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

**前端打包**：
```bash
cd vue
npm run build
# 生成的静态文件在 dist/ 目录
```

---

## 默认账号

系统支持注册新用户。如需直接使用，可通过注册页面创建账号。

---

## 许可证

本项目为毕业设计作品，仅供学习交流使用。

# 大学生上网痛点资源站 · 项目设计方案

> **面向人群**：计算机大二学生  
> **团队规模**：4人  
> **开发周期**：1周（5个工作日 + 周末缓冲）  
> **技术栈**：HTML/CSS/JS + Java 原生 Servlet + JDBC + MySQL  
> **说明**：无 Spring，无复杂框架，仅用课程已授知识完成前后端结合。

---

## 一、项目定位

一个专门解决大学生上网刚需（Steam/加速器/VPN/种子/GitHub/API 等）的**资源分享 + 教程文章**轻量平台，可理解为"校园版的上网指南资源库"。

核心特征：

- 以**资源下载**和**教程文章**两种内容为核心
- 覆盖 Steam 低价区、网络加速、BT 种子、GitHub 使用、免费 API 对接等痛点
- 拥有用户体系、上传、下载、管理后台的完整闭环
- 极简、可演示，适合课程作业

---

## 二、核心功能（MVP）

### 1. 用户模块
- 注册（用户名、密码）
- 登录（基于 HttpSession）
- 角色：普通用户 / 管理员（可手动修改数据库设定）
- 登录后可上传资源，未登录仅可浏览和下载

### 2. 资源模块
- 首页资源列表，按分类筛选
- 资源详情（标题、描述、分类、上传者、下载量、时间）
- 资源搜索（标题模糊查询）
- 资源上传（需登录）：标题、描述、分类、文件
- 资源下载（记录下载次数）

### 3. 教程文章模块
- 文章列表、文章详情
- 支持 Markdown（前端引入 marked.js 渲染）
- 用于发布"Steam 原理""种子原理"等教学文章

### 4. 管理后台（仅管理员可见）
- 资源管理：删除违规资源
- 用户管理：查看用户列表、删除用户

---

## 三、模块划分（参照课程平台）

| 模块 | 功能点 |
|------|--------|
| 首页/门户 | 资源搜索、分类导航、最新资源、热门下载 |
| 用户中心 | 注册、登录、个人极简信息 |
| 资源市场 | 资源列表、详情、上传、下载 |
| 教程课堂 | 文章列表、文章详情 |
| 后台管理 | 资源删除、用户删除（仅管理员） |

---

## 四、技术选型

- **后端**：原生 Servlet（jakarta.servlet）+ JDBC 直连
- **数据库**：MySQL 8.0
- **前端**：HTML + CSS + JavaScript，不引入 Vue/React
- **通信方式**：前端静态页面通过 AJAX (fetch) 调用后端 API，后端只返回 JSON（**不是真正的前后分离部署，而是前后端代码分离开发**）
- **Markdown 渲染**：marked.js（CDN 引入）
- **文件上传**：前端 FormData，后端用 `@MultipartConfig` 注解处理 multipart/form-data
- **会话管理**：HttpSession（登录后存放 user 对象）
- **样式**：Bootstrap 5（CDN 引入，快速美化）
- **版本管理**：Git + GitHub（**强制使用，每天提交**）

项目运行于 Tomcat 9+，前端静态页面放在 `webapp/` 目录下，后端 Servlet 统一映射在 `/api/*` 路径下。

---

## 五、数据库设计（5张核心表）

### 1. users 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK, AUTO_INCREMENT) | 自增 |
| username | VARCHAR(50) UNIQUE NOT NULL | 唯一 |
| password | VARCHAR(255) NOT NULL | **BCrypt 哈希**（见下方安全说明） |
| role | VARCHAR(20) DEFAULT 'user' | 'user' 或 'admin' |
| created_at | DATETIME DEFAULT NOW() | |

> **密码安全**：不要用 MD5！MD5 已被证明可碰撞破解。使用 BCrypt 哈希，Java 端引入单个依赖 `org.mindrot:jbcrypt:0.4`（仅一个 jar，拷贝到 WEB-INF/lib 即可），用 `BCrypt.hashpw()` 和 `BCrypt.checkpw()` 处理。答辩时这是加分项。

### 2. categories 分类表（可预设）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK, AUTO_INCREMENT) | 自增 |
| name | VARCHAR(50) NOT NULL | 如：Steam相关、VPN工具等 |
| sort_order | INT DEFAULT 0 | 排序 |

**预设分类**：Steam 相关、VPN与加速器、BT/种子与下载、GitHub 与开源、API 对接实战、其他工具

### 3. resources 资源表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK, AUTO_INCREMENT) | |
| title | VARCHAR(100) NOT NULL | |
| description | TEXT | |
| category_id | INT (FK → categories.id) | |
| file_path | VARCHAR(255) | 服务器保存的相对路径 |
| file_size | BIGINT DEFAULT 0 | 字节 |
| original_name | VARCHAR(255) | 原始文件名，下载时恢复 |
| download_count | INT DEFAULT 0 | |
| uploader_id | INT (FK → users.id) | |
| created_at | DATETIME DEFAULT NOW() | |

### 4. articles 文章表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT (PK, AUTO_INCREMENT) | |
| title | VARCHAR(100) NOT NULL | |
| content | TEXT | Markdown 原文 |
| category_id | INT (FK → categories.id) | 可复用分类表 |
| author_id | INT (FK → users.id) | |
| view_count | INT DEFAULT 0 | |
| created_at | DATETIME DEFAULT NOW() | |

### 5. downloads 下载记录表（可选，MVP 可不做）

简单处理：下载时直接对 `resources.download_count` 加 1，可对同 IP 短时间内防刷。

---

## 六、统一 API 返回格式

**所有接口统一返回以下 JSON 结构，前端据此统一处理：**

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

**前端封装一个 `api(url, options)` 工具函数，统一处理 code 判断和错误提示，不要每个页面重复写 fetch 逻辑。**

---

## 七、前后端交互 API 设计

所有 API 返回 JSON，路径统一在 `/api/*`。

### 用户相关
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/register` | 注册 `{username, password}` |
| POST | `/api/login` | 登录 `{username, password}`，返回用户信息，写入 session |
| GET | `/api/logout` | 登出，清除 session |
| GET | `/api/user/current` | 获取当前登录用户信息 |

### 资源相关
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resources?categoryId=&keyword=&page=&size=` | 资源列表（分页） |
| GET | `/api/resources/{id}` | 资源详情 |
| POST | `/api/resources` | 上传资源（需登录，FormData） |
| GET | `/api/resources/{id}/download` | 下载资源（返回文件流，下载量+1） |

### 文章相关
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/articles?categoryId=` | 文章列表 |
| GET | `/api/articles/{id}` | 文章详情 |

### 管理相关（需 session 中 role=admin）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/users` | 用户列表 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |
| DELETE | `/api/admin/resources/{id}` | 删除资源 |

---

## 八、安全规范（答辩必问，必须落实）

### SQL 注入防护
**所有 SQL 查询必须使用 PreparedStatement，禁止字符串拼接 SQL。**

```java
// ✅ 正确
PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
ps.setString(1, username);

// ❌ 禁止
Statement stmt = conn.createStatement();
stmt.executeQuery("SELECT * FROM users WHERE username = '" + username + "'");
```

### XSS 防护
- 前端渲染用户提交的内容（标题、描述）时，使用 `textContent` 而非 `innerHTML`
- Markdown 渲染后的文章内容使用 marked.js 的 sanitize 选项

### 文件上传安全
- 校验文件扩展名白名单（如 `.zip, .rar, .7z, .pdf, .png, .jpg`）
- 文件以 UUID 重命名存储，避免用户上传 `../../etc/passwd` 这类路径遍历攻击
- 限制文件大小（Tomcat 默认 2MB，调整为 50MB）

### 密码安全
- 使用 BCrypt 哈希存储，禁止明文或 MD5

---

## 九、文件存储方案（极简版）

- 在 Tomcat 项目根目录下创建 `uploads/` 文件夹，作为文件存储目录
- 文件以 UUID 重命名，保留原始扩展名
- 数据库同时存储 UUID 文件名和用户上传时的原始文件名（下载时恢复原名）
- Servlet 处理下载：读取文件 → 设置 `Content-Type: application/octet-stream` → 设置 `Content-Disposition: attachment; filename="原始文件名"` → 以流方式写回

此方案无需任何第三方存储依赖。

---

## 十、Tomcat 配置清单

以下配置需在开发前统一确认，避免联调时才发现问题：

1. **文件上传大小**：修改 `web.xml` 中 `max-file-size` 和 `max-request-size` 为 50MB
2. **数据源**：无需配置 JNDI 数据源，直接在代码中使用 JDBC 连接即可（用工具类管理）
3. **编码**：Tomcat `server.xml` 中 Connector 添加 `URIEncoding="UTF-8"`，Servlet 中统一 `request.setCharacterEncoding("UTF-8")`
4. **端口**：默认 8080，全组统一

---

## 十一、MySQL 建库与初始化脚本

项目启动前，由一个人统一执行建库建表 SQL，其他人导入同一份 SQL。

```sql
CREATE DATABASE IF NOT EXISTS campus_resources DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_resources;

-- 建表语句（users, categories, resources, articles）
-- 以及预设分类的 INSERT 语句
-- 统一放在项目根目录的 init.sql 文件中
```

数据库账号建议统一使用 `root` / 自行约定密码（仅开发环境）。

---

## 十二、Git 协作规范（强制）

4 个人必须用 Git 协作，否则代码合并时会是一场灾难。

### 仓库结构
```
项目根目录/
├── src/                # Java 源文件
├── webapp/             # 前端静态页面 + WEB-INF
│   ├── index.html
│   ├── pages/          # 各功能页面
│   ├── css/
│   ├── js/
│   │   └── api.js      # 统一封装的 fetch 工具函数
│   └── WEB-INF/
│       ├── web.xml
│       └── lib/        # 第三方 jar（BCrypt 等）
├── uploads/            # 上传文件存放目录（加入 .gitignore）
└── init.sql            # 建库建表 + 预设数据
```

### 分支策略（简单版）
- `main` 分支为稳定版本，**禁止直接 push**
- 每人一个功能分支（如 `feat/user-module`、`feat/resource-api`）
- 每天收工时合并到 main，解决冲突
- **提交粒度**：完成一个功能点就 commit 一次，不要攒到最后一天

### .gitignore 必须包含
```
uploads/
*.class
/WEB-INF/classes/
.idea/
*.iml
target/
```

---

## 十三、1周开发计划与分工（5个工作日 + 周末缓冲）

### Day 1（周一）：全组铺底——数据库 + 项目骨架 + 接口约定

**上午（4人一起）**
- 确定数据库结构，执行 `init.sql`，每人本地导入
- 确定 API 路径、参数、统一返回格式（按本文档第六、七节）
- 搭建 Git 仓库，各自 clone，确认能 push/pull
- 搭建 Tomcat 环境，跑通一个 HelloServlet 确认环境 OK

**下午（开始分工）**

| 成员 | 任务 |
|------|------|
| **A（前端组长）** | 全局样式框架（Bootstrap 5）、首页 HTML 骨架、导航栏、分类侧栏、搜索栏 |
| **B（前端交互）** | 封装 `api.js` 工具函数、注册/登录页面 HTML + 表单逻辑 |
| **C（后端·用户）** | 用户注册/登录/登出 Servlet、Session 管理、BCrypt 集成 |
| **D（后端·资源）** | 数据库工具类（Druid 连接池）、资源列表/详情 Servlet（先返回假数据） |

> **今日目标**：晚上前，每个 Servlet 至少能返回 JSON（哪怕数据是假的），前端能 fetch 到并 console.log 出来。

---

### Day 2（周二）：核心功能开发

| 成员 | 任务 |
|------|------|
| **A（前端组长）** | 资源列表页（分页、分类筛选）、资源详情页、搜索功能对接 |
| **B（前端交互）** | 资源上传表单页、文章列表页、文章详情页（含 marked.js 渲染） |
| **C（后端·用户+管理）** | 登录校验 Filter、当前用户接口、管理员用户列表/删除接口 |
| **D（后端·资源+文章）** | 资源上传（MultipartConfig + 文件存储）、资源下载、文章列表/详情接口 |

> **今日目标**：用户模块和资源模块各自能跑通独立流程（前后端分别测试，不一定联调）。

---

### Day 3（周三）：管理后台 + 前后端联调

**上午**
| 成员 | 任务 |
|------|------|
| **A** | 管理后台页面（用户表格 + 删除按钮、资源表格 + 删除按钮） |
| **B** | 联调注册登录流程、资源列表/详情/搜索 |
| **C** | 联调管理后台接口（用户删除、资源删除）、权限校验 |
| **D** | 联调资源上传/下载流程 |

**下午（4人一起）**
- 全流程走通：注册 → 登录 → 浏览资源 → 上传资源 → 下载资源 → 管理员登录 → 删除违规资源
- 边界情况处理：未登录提示、文件类型校验、空列表展示、上传失败提示

> **今日目标**：MVP 完整闭环跑通。

---

### Day 4（周四）：内容填充 + 页面打磨

**上午——内容填充**
- **每个人**为自己的模块编写 1~2 条资源和 1 篇文章（先用后台直接插数据，后用网站自己的上传功能录入）
- 确保每个分类下至少有 2 条资源
- 分类覆盖：Steam 低价区教程、Watt Toolkit 指南、qBittorrent 配置、GitHub 学生包申请、免费 API 调用示例

**下午——页面打磨**
| 成员 | 任务 |
|------|------|
| **A** | 首页美化、热门下载排序、响应式适配 |
| **B** | 表单交互优化（loading 状态、错误提示美化、上传进度） |
| **C** | 后端日志补充、错误处理完善、空值判断 |
| **D** | 搜索功能优化（防抖）、分页组件完善 |

> **今日目标**：项目达到可演示水平。

---

### Day 5（周五）：测试 + 文档 + 演示排练

**上午：全组测试**
- 每人用一套完整流程测试（注册新号 → 上传 → 下载 → 切换管理员账号 → 删除）
- 记录发现的 bug，统一修复
- 在另一台电脑上 clone 项目，验证能否跑通（防止环境依赖问题）

**下午：文档 + 演示排练**

| 成员 | 任务 |
|------|------|
| **A** | README 编写（项目简介、技术栈、启动方式、目录结构说明） |
| **B** | 准备演示数据（确保每种分类有足够内容、截图或录屏） |
| **C** | 整理设计文档中的"未来扩展"部分（用于报告加分） |
| **D** | 逐条检查安全规范（SQL 注入、XSS、文件上传校验）并确认 |

> **今日目标**：代码冻结，文档齐全，每人至少演练一遍演示流程。

---

### 周末缓冲
- 如果进度滞后，周末补齐
- 如果进度正常，可选做：首页统计面板（资源总数、用户数、下载量）、文章阅读量统计
- **不做新功能，只打磨和修 bug**

---

## 十四、JDBC 工具类说明（替代单例反模式）

不要手写"单例 Connection"——多个请求共用同一个 Connection 会崩溃。用以下两种方案之一：

**方案一（推荐）：引入 Druid 连接池**
- 仅需 `druid-1.2.20.jar` 放入 `WEB-INF/lib`
- 一个 `DruidDataSource` 实例管理连接池，线程安全
- 每次请求 `getConnection()` → 用完 `close()`（归还到池）

**方案二（最简单）：每次请求新建连接**
- 手写 `DBUtil.getConnection()`，每次都 new 一个 Connection
- 缺点：开销大，但对于 4 人课程项目足够（并发量极小）
- **每个请求 finally 中必须 close**，否则连接数会耗尽

```java
// 方案二示例（课程项目够用）
public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/campus_resources?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL驱动加载失败", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
```

---

## 十五、发散思路（报告加分/后续扩展）

以下内容可在设计文档中作为"未来功能"呈现，不强制在 1 周内实现。

1. **星标/收藏系统**
   用户可为资源打星标，形成个人收藏库，首页展示热门榜单。

2. **评论/讨论区**
   每个资源下允许留言互动，解决具体使用问题。

3. **标签系统**
   一个资源可挂多个标签（如 #土耳其区 #低价区），方便交叉检索。

4. **资源版本管理**
   支持上传新版本，保留历史版本，模仿 GitHub 的 Release。

5. **积分/贡献体系**
   上传资源获积分，下载消耗积分，激励分享，减少伸手党。

6. **API 在线测试小工具**
   在教程页面内嵌一个简单的 API 请求工具（纯前端 fetch），直观展示 API 对接过程。

7. **GitHub OAuth 登录**
   接入 GitHub 第三方登录，既贴合主题又体现技术深度。

---

## 十六、预设内容建议

为让项目演示时内容丰富，建议每个分类下准备：

- **Steam 相关**
  低价区切换教程、Steam 社区加速工具、挂刀比例查询工具
- **VPN/加速器**
  Watt Toolkit 使用指南、Clash 订阅转换原理、校园网免流方案
- **BT/种子**
  qBittorrent 配置教程、Tracker 列表更新、BT 做种与 DHT 网络科普
- **GitHub 使用**
  GitHub 学生包申请、Git 快速入门图解、Actions 自动部署教程
- **API 对接实战**
  免费天气 API 调用示例、一言 API 使用、基于 ChatGPT API 的简易聊天

---

## 十七、避坑指南（1周生存守则）

1. **前后端接口先行**：第一天先约定 JSON 格式，双方各写各的，用假数据开发，不然互相等待。
2. **Git 每天提交**：不要等到最后一天才 push，一个人的代码丢失会拖垮整组。
3. **文件上传大小限制**：Tomcat 默认 2MB，第一天就改 `web.xml` 的 `multipart-config`，否则联调时才发现。
4. **跨域问题**：前端页面和后端 API 部署在同一 Tomcat 下（同源），不存在跨域。如果有人用 Live Server 插件开发前端，需临时配 CORS 或统一用 Tomcat 访问前端页面。
5. **密码安全**：使用 BCrypt，不要用 MD5——答辩时这是高频考点。
6. **SQL 注入**：全部用 PreparedStatement，不要拼接字符串——这也是答辩高频考点。
7. **数据库连接**：finally 块中必须 `conn.close()`，否则 Tomcat 跑一会儿就报 "Too many connections"。
8. **字符编码**：从数据库 URL 参数 → Tomcat Connector → Servlet `setCharacterEncoding` → HTML `<meta charset>`，四层都要设 UTF-8，少一层就乱码。
9. **资源路径安全**：下载时校验文件存在性，文件名包含 `..` 或 `/` 的请求直接拒绝，防止路径遍历。
10. **提前准备演示流程**：教师/助教只看 5 分钟，提前设计好演示路径（注册→登录→浏览→下载→管理后台删除），不要现场乱点。

---

## 十八、鼓励与总结

你们选择的主题**天然具备实用性和趣味性**，比传统的图书管理系统或博客系统更有辨识度。1 周内完成注册登录、文件上传下载、后台管理的"上网指南资源站"，已经是一份完整的全栈项目作业。

核心原则：
- **先跑通，再打磨**——Day 3 之前所有功能要能走通，哪怕界面丑
- **Git 是底线**——4 个人不用 Git 等于在火堆上跳舞
- **安全问题是答辩重点**——SQL 注入、密码哈希、文件上传校验，这三条必须做好

稳住 MVP，后续完全有机会迭代成真正服务同学的校园产品，甚至可以写入简历。

祝一周顺利，肝出精彩！

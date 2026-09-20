# InkHub 🖋️ 知识博客社区

> 一个 Markdown 知识社区：写文章、分类标签、评论互动、站内通知，前后端全栈独立开发。

## ✨ 功能

- **文章**：Markdown 写作（md-editor-v3）、草稿/发布/下架状态机、置顶、封面图、相关文章推荐
- **分类/标签**：多对多，文章流按分类筛选、按标签聚合
- **互动**：二级评论（parent_id 回复模型）、点赞、收藏（唯一索引幂等防并发）
- **通知**：评论回复/点赞/收藏触发站内通知，导航栏铃铛红点 + 通知页
- **浏览量与性能**：浏览量 Redis INCR 计数 + 定时任务批量落库（SCAN 防阻塞、增量写回）
- **管理端**：文章/分类/评论管理 + ECharts 数据统计（横向渐变柱状图、环形饼图）
- **文件上传**：封面/头像/编辑器图片统一接口（UUID 重命名、类型白名单、5MB 限制）
- **安全**：JWT 无状态认证、Spring Security 角色权限（普通用户/管理员）、评论通知防自触发、已读防越权

## 🛠️ 技术栈

| 端 | 技术 |
|---|---|
| 后端 | Java 17 · Spring Boot 4.1 · Spring Security · JWT · MyBatis-Plus · MySQL · Redis |
| 前端 | Vue 3 · Vite · Element Plus · Pinia · Vue Router · Axios · ECharts · md-editor-v3 |

## 📂 项目结构

```
InkHub/
├── InkHub-backend/          # Spring Boot 后端
│   ├── src/main/java/com/example/InkHub_backend/
│   │   ├── controller/      # 接口层（REST）
│   │   ├── service/         # 业务层
│   │   ├── mapper/          # MyBatis-Plus + 手写 SQL（XML）
│   │   ├── entity/          # 实体（9 张表）
│   │   ├── vo/ dto/         # 出入参对象
│   │   ├── security/        # JWT 过滤器 + 认证
│   │   ├── config/          # 配置类（Redis/MyBatis-Plus/Security/CORS）
│   │   └── task/            # 定时任务（浏览量落库）
│   └── src/main/resources/
│       ├── application.yaml
│       └── mapper/          # 手写 SQL XML
└── InkHub-web/              # Vue 3 前端
    └── src/
        ├── views/           # 页面（首页/详情/编辑器/个人中心/管理端）
        ├── api/             # 接口封装
        ├── stores/          # Pinia
        ├── router/          # 路由
        └── styles/          # 设计系统（Ink 墨主题）
```

## 🚀 本地启动

**前置**：MySQL（建库 `inkhub` + 执行建表 SQL）、Redis、JDK 17、Node 18+

**1. 后端**（IDEA 打开 `InkHub-backend`）：

```bash
# 或命令行启动
export MYSQL_PASSWORD=你的MySQL密码
export JWT_SECRET=你的随机密钥
mvn spring-boot:run
```

- 启动后访问 `http://localhost:8080`
- 接口文档（springdoc）：`http://localhost:8080/swagger-ui.html`

**2. 前端**：

```bash
cd InkHub-web
npm install
npm run dev
```

- 访问 `http://localhost:5173`（`/api`、`/uploads` 已代理到后端 8080）

## ⭐ 技术亮点（面试速览）

1. **浏览量不写库**：Redis INCR 计数 → 定时任务每 10 分钟 SCAN 增量写回 MySQL 并清零，SCAN 代替 KEYS 防阻塞，异常兜底下轮重试
2. **并发幂等**：点赞/收藏用唯一索引兜底，并发重复请求不重复计数
3. **二级评论**：一张表 parent_id 模型 + SQL `IF(parent_id IS NULL, id, parent_id)` 排序分组，前端一次查出 filter 渲染，不递归查库
4. **JWT 认证**：无状态登录，过滤器校验 token，role 区分普通用户/管理员权限
5. **上传安全**：UUID 重命名防路径穿越、类型白名单、大小限制、绝对路径存储 + 静态映射

## 📸 截图

**首页 · 深海主题**

![](https://cdn.jsdelivr.net/gh/smallgreen-7/InkHub@main/images/home.png)

**AI 助手 · 站内问答（RAG + 混合检索 + Rerank）**

<img src="https://cdn.jsdelivr.net/gh/smallgreen-7/InkHub@main/images/ai-chat.png" width="360" />

**写文章 · Markdown 编辑器**

![](https://cdn.jsdelivr.net/gh/smallgreen-7/InkHub@main/images/editor.png)

**后台管理 · 文章管理 / 分类标签 / 数据统计**

![](https://cdn.jsdelivr.net/gh/smallgreen-7/InkHub@main/images/admin-article.png)

![](https://cdn.jsdelivr.net/gh/smallgreen-7/InkHub@main/images/admin-category.png)

![](https://cdn.jsdelivr.net/gh/smallgreen-7/InkHub@main/images/admin-stats.png)

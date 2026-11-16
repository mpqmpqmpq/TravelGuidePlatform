# TravelGuidePlatform

TravelGuidePlatform 是一个基于 Java 17 + Spring Boot 3 的旅游平台后端服务，面向旅游内容管理、用户社交、AI 智能问答和多媒体处理等场景，提供一套可扩展的后端能力，适合用于旅游网站、移动端后端、内容管理系统以及智能推荐场景的开发与落地。

## 项目简介

该项目聚焦于旅游行业的典型业务，包含以下核心能力：

- 用户认证与权限控制：注册、登录、JWT 认证、用户信息管理
- 旅游内容管理：目的地、路线、景点、文章、标签、分类
- 社交互动：评论、点赞、收藏、关注、通知
- AI 智能问答：基于 DashScope / Spring AI 接入智能对话与 RAG 能力
- 多媒体处理：图片、音视频上传与对象存储
- 实时通信：WebSocket 推送、在线状态与消息通知
- 异步任务：RabbitMQ、定时任务、后台消费

## 技术栈

- Java 17
- Spring Boot 3.5.3
- Spring Security
- Spring Web / WebSocket
- MyBatis-Plus 3.5.5
- MySQL 8+
- Redis 6+
- RabbitMQ 3+
- MinIO / AWS S3
- JWT
- Knife4j / OpenAPI
- Spring AI / DashScope / MCP
- Lombok
- Maven

## 项目结构

```text
TravelGuidePlatform/
├── pom.xml
├── README.md
├── .gitignore
├── src/
│   ├── main/
│   │   ├── java/com/mpq/
│   │   │   ├── aspect/
│   │   │   ├── config/
│   │   │   ├── consumer/
│   │   │   ├── controller/
│   │   │   ├── handler/
│   │   │   ├── listener/
│   │   │   ├── manager/
│   │   │   ├── mapper/
│   │   │   ├── pojo/
│   │   │   ├── scheduled/
│   │   │   ├── service/
│   │   │   ├── utils/
│   │   │   └── TravelGuidePlatformApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── mapper/
│   └── test/
└── target/
```

## 功能模块

### 1. 用户与认证

- 用户注册 / 登录 / 密码更新
- JWT 令牌生成与校验
- 用户资料、头像、偏好设置
- 关注、收藏、互动行为支持

### 2. 旅游内容管理

- 目的地、路线、景点、攻略文章管理
- 分类、标签、媒体资源关联
- 景点评价与内容审核支持

### 3. AI 与智能问答

- 接入 DashScope 等大模型能力
- 支持 AI 对话与聊天历史管理
- 提供 RAG 与 MCP 接入扩展能力

### 4. 社交与推荐

- 点赞、收藏、评论、消息通知
- 热门内容与推荐逻辑支持
- 用户行为日志采集

### 5. 多媒体与存储

- 图片、视频上传
- MinIO / S3 对象存储
- 支持分块上传与多媒体管理

### 6. 实时消息与异步处理

- WebSocket 连接管理
- RabbitMQ 生产者 / 消费者
- 定时任务与后台调度

## 环境要求

在本地或服务器环境运行前，确保已安装以下依赖：

- JDK 17+
- Maven 3.8+
- MySQL 8+
- Redis 6+
- RabbitMQ 3+
- MinIO
- 可选：DashScope API Key

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/mpqmpqmpq/TravelGuidePlatform.git
cd TravelGuidePlatform
```

### 2. 配置环境变量

项目使用 `application.properties` 中的占位符方式加载配置，常见配置如下：

```properties
spring.application.name=TravelGuidePlatform

spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.password=

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket-name=user-avatars

jwt.secret=your_jwt_secret_key_change_me_in_production
jwt.expiration=86400000

spring.ai.dashscope.api-key=your_dashscope_api_key
```

也可以通过环境变量覆盖默认值，例如：

```bash
export DB_URL="jdbc:mysql://localhost:3306/travel_guide?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export REDIS_HOST="localhost"
export RABBITMQ_HOST="localhost"
export DASHSCOPE_API_KEY="your_api_key"
export JWT_SECRET="your_jwt_secret"
```

### 3. 构建与运行

```bash
mvn clean package
java -jar target/TravelGuidePlatform-0.0.1-SNAPSHOT.jar
```

或者直接在 IDE 中运行 `TravelGuidePlatformApplication`。

## 运行说明

- 本项目是后端服务，适合与前端应用、移动端客户端或网关服务一起使用。
- 多媒体功能依赖 MinIO / S3 及对应存储桶。
- AI 功能依赖 DashScope 或其他兼容模型服务。
- 如果未配置数据库、Redis、RabbitMQ、MinIO 等基础依赖，服务可能无法正确启动。

## API 文档

项目已集成 Knife4j / OpenAPI，服务启动后可访问：

```text
http://localhost:8080/doc.html
```

若你部署在网关、反向代理或自定义端口环境中，请按实际地址进行访问。

## 配置入口

默认配置文件位于：

- `src/main/resources/application.properties`

关键配置包括：

- 数据库连接
- Redis 配置
- RabbitMQ 配置
- JWT 配置
- MinIO 配置
- AI 模型配置
- 监控与指标配置

## 许可证

当前仓库未声明明确许可证信息。如果在生产环境使用，请先确认项目的开源或商业许可要求。

## 贡献与反馈

欢迎提出问题、建议和改进意见。你可以通过以下方式参与：

- 提交 Issue
- 提交 Pull Request
- 改进文档与接口说明

## 备注

这是一个以旅游平台为主题的 Java 后端项目，覆盖了从用户管理到内容管理、AI 智能交互、多媒体处理等较完整的后端能力。若你是第一次阅读该项目，建议优先关注以下目录：

- `pom.xml`：项目依赖与构建配置
- `src/main/resources/application.properties`：服务配置入口
- `src/main/java/com/mpq/controller`：API 控制器入口
- `src/main/java/com/mpq/service`：核心业务逻辑
- `src/main/java/com/mpq/config`：中间件与安全配置

如果你愿意，我还可以继续补充：

- 中文 / English 双语 README
- Docker 部署说明
- API 接口清单
- 数据库表结构说明
- 启动脚本与环境变量模板

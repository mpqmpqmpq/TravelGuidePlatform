# TravelGuidePlatform

这是一个基于 Java 17 与 Spring Boot 3 的后端项目，主要用于演示旅游平台常见的业务模块和后端开发实践。项目包含用户管理、旅游内容管理、社交互动、消息通知以及基础 AI 能力接入等示例代码，适合用于学习和二次开发。

## 项目概览

该项目目前包含以下几类功能：

- 用户认证与权限：注册、登录、JWT、用户信息维护
- 旅游内容管理：目的地、路线、景点、文章、分类与标签
- 社交互动：评论、点赞、收藏、关注、通知
- AI 能力接入：DashScope 集成、聊天记录、RAG 与 MCP 扩展示例
- 多媒体处理：图片上传、视频处理、对象存储集成
- 实时通信：WebSocket、在线状态与消息推送
- 异步任务：RabbitMQ、定时任务与后台处理

这些能力大多按常见后端工程结构组织，便于理解项目如何拆分模块和接入中间件。

## 技术栈

- Java 17
- Spring Boot 3.5.3
- Spring Security
- MyBatis-Plus 3.5.5
- MySQL 8+
- Redis 6+
- RabbitMQ 3+
- MinIO / AWS S3
- JWT
- Knife4j / OpenAPI
- Spring AI / DashScope / MCP
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
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   ├── pojo/
│   │   │   ├── util/
│   │   │   └── TravelGuidePlatformApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── mapper/
│   └── test/
└── target/
```

## 环境要求

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

### 2. 配置应用

在 `src/main/resources/application.properties` 中配置数据库、Redis、RabbitMQ、MinIO 和 AI 相关参数，例如：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=your_password

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin

spring.ai.dashscope.api-key=your_dashscope_api_key
jwt.secret=your_jwt_secret_key_change_me_in_production
```

### 3. 构建并运行

```bash
mvn clean package
java -jar target/TravelGuidePlatform-0.0.1-SNAPSHOT.jar
```

也可以直接在 IDE 中运行 `TravelGuidePlatformApplication`。

## API 文档

项目集成了 Knife4j / OpenAPI，启动后可在浏览器中访问：

```text
http://localhost:8080/doc.html
```

## 运行说明

- 本项目是后端服务，通常需要配合前端、移动端或网关一起使用。
- 多媒体功能依赖 MinIO / S3 等对象存储服务。
- AI 能力依赖 DashScope 或其他兼容模型服务。
- 若未配置基础中间件，服务可能无法正常启动。

## 适用场景

这个项目可作为以下场景的参考：

- 旅游资讯平台后端
- 景点与路线内容管理系统
- 用户社交与互动平台
- AI 驱动的旅游问答示例
- 具备内容管理和基础推荐能力的后端服务

## 备注

这是一个以旅游平台为主题的 Java 后端项目，覆盖了用户管理、内容管理、社交互动、AI 能力与多媒体处理等常见模块，适合用于学习 Spring Boot、数据库集成和中间件接入。

如果你希望进一步扩展该项目，可以从用户权限、内容审核、推荐逻辑、前端联调或部署脚本等方向继续完善。

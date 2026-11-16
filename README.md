# TravelGuidePlatform

TravelGuidePlatform 是一个基于 Java 17 + Spring Boot 3 的旅游平台后端服务，聚焦于旅游内容管理、用户互动、AI 智能问答和多媒体内容处理等能力。该项目采用 MyBatis-Plus + MySQL + Redis + RabbitMQ + MinIO 等多种中间件，支持旅游攻略、景点、评论、收藏、消息通知以及 AI 对话等核心场景。

## 项目简介

该系统包含以下典型业务模块：

- 用户认证与授权：注册、登录、JWT 认证、权限控制
- 旅游内容管理：目的地、路线、景点、文章、标签、分类
- 社交互动：评论、点赞、关注、收藏、通知
- AI 智能能力：对话、聊天记录、RAG、MCP 接入、DashScope 模型调用
- 多媒体处理：图片上传、音视频处理、MinIO/S3 存储
- 实时通信：WebSocket、消息推送、在线状态
- 异步任务：RabbitMQ 消费者、定时任务、推荐效果调度

## 技术栈

- Java 17
- Spring Boot 3.5.3
- Spring Security
- Spring Web / WebSocket
- MyBatis-Plus 3.5.5
- MySQL
- Redis
- RabbitMQ
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
└── README.md
```

## 功能模块概览

### 1. 用户与认证

- 用户注册、登录、密码更新
- JWT 令牌校验
- 用户信息、头像、偏好设置
- 用户关注、收藏与互动

### 2. 旅游内容管理

- 目的地、路线、景点、攻略文章
- 分类、标签、文章媒体资源
- 景点评价与文章审核流

### 3. AI 与智能问答

- DashScope 模型集成
- RAG 相关配置与控制器
- MCP 客户端能力接入
- 聊天历史记录管理

### 4. 社交与推荐

- 点赞、收藏、评论、消息通知
- 热门文章与推荐效果计算
- 用户行为日志采集

### 5. 多媒体与存储

- 图片、视频上传
- MinIO / S3 对象存储
- 分块上传支持（Multipart Upload）

### 6. 实时消息与异步处理

- WebSocket 连接管理
- RabbitMQ 消息生产与消费
- 定时任务和后台处理

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8+
- Redis 6+
- RabbitMQ 3+
- MinIO
- 可选：可用的 DashScope API Key

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/mpqmpqmpq/TravelGuidePlatform.git
cd TravelGuidePlatform
```

### 2. 配置数据库和中间件

在 `src/main/resources/application.properties` 中配置以下参数：

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
```

也可以通过环境变量方式配置，项目中已经内置了大量 `${VAR_NAME:default}` 形式的参数。

### 3. 构建与运行

```bash
mvn clean package
java -jar target/TravelGuidePlatform-0.0.1-SNAPSHOT.jar
```

或者直接在 IDE 中运行 `TravelGuidePlatformApplication`。

## API 文档

项目集成了 Knife4j / OpenAPI 文档，应用启动后可通过以下地址访问文档入口（通常为）：

```text
http://localhost:8080/doc.html
```

如果你的网关或映射地址不同，可根据实际部署环境调整端口和路径。

## 配置说明

项目默认配置文件位于：

- `src/main/resources/application.properties`

关键配置项包括：

- 数据库连接
- Redis 配置
- RabbitMQ 配置
- JWT Secret
- MinIO 配置
- AI 模型配置
- 监控指标配置

## 运行说明

- 本项目是一个后端服务，适合与前端应用、移动端客户端或其他网关服务配合使用。
- 多媒体模块依赖 MinIO / S3 及对应的存储桶资源。
- AI 功能依赖 DashScope 或其他兼容模型服务。
- 若未配置 Redis、RabbitMQ、MySQL、MinIO 等依赖，服务可能无法正常启动。

## 许可证

当前仓库未声明明确许可证信息，若需在生产环境使用，请先确认项目的开源/商业许可要求。

## 贡献说明

欢迎对该项目提出问题、建议和改进意见。你可以：

- 提交 Issue
- 提交 Pull Request
- 改进文档与接口说明

## 备注

这是一个以旅游平台为主题的 Java 后端项目，覆盖了从用户管理到 AI 智能交互的较完整功能。若你是第一次参与该项目，建议优先阅读以下内容：

- `pom.xml`：项目依赖与构建配置
- `src/main/resources/application.properties`：服务配置入口
- `src/main/java/com/mpq/controller`：API 控制器入口
- `src/main/java/com/mpq/service`：核心业务逻辑
- `src/main/java/com/mpq/config`：中间件与安全配置

如果你愿意，我还可以继续为这个项目补充：

- 更正式的中英文 README
- Docker 部署说明
- API 接口清单
- 数据库表结构说明
- 项目启动脚本与环境变量示例

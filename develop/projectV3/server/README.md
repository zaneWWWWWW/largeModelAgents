# 后端（Server）

## 概览
- Spring Boot + MyBatis-Plus 后端服务，提供用户、心理测评（MBTI/SCL90/统一测评等）与文件上传等接口。
- 打包产物名为 `camphor-tree-server-1.0.0.jar`

## 构建与运行
```bash
# 构建（跳过测试）
mvn -q clean package -DskipTests

# 运行
java -jar target/camphor-tree-server-1.0.0.jar
```
- 构建输出目录：`server/target/`
- 同时生成 `camphor-tree-server-1.0.0.jar.original`（Spring Boot 重打包保留的原始 JAR）。
- 产物命名来源：`server/pom.xml:13–15`（`groupId`、`artifactId`、`version`）。

## 配置
- 应用配置位于 `src/main/resources/application.yml`（以及 `.example` / `.local` 示例）。
- 各环境请正确设置数据库连接、端口、日志级别等；`target` 下的 `application.yml` 是打包时复制的结果。

## 目录说明
- 源码：`src/main/java/com/example/bluecat/`
  - `controller/` REST 控制器
  - `service/` 业务服务与实现
  - `mapper/` MyBatis-Plus 映射接口；对应 XML 位于 `src/main/resources/mapper/`
  - `entity/` 数据实体
  - `config/` 安全/上传等配置
  - `security/` JWT 相关
- 资源：`src/main/resources/mapper/*.xml` 与 `application.yml`

## 核心业务功能
- 账号与认证（JWT）
  - 注册、登录、获取信息、更新单字段、修改密码

- 文件上传（头像）
  - `POST /api/upload/avatar`

- MBTI 测评
  - `GET /api/mbti/questions`
  - `GET /api/mbti/types/{typeCode}`
  - `PUT /api/mbti/user/{userId}`

- SCL90 测评
  - `GET /api/scl90/questions`
  - `POST /api/scl90/results`
  - `GET /api/scl90/results/{userId}`
  - `DELETE /api/scl90/results/{userId}`
  - `GET /api/scl90/factors`、`GET /api/scl90/factors/{id}`、`GET /api/scl90/factors/name/{factorName}`

- 统一测评（问卷引擎）
  - `GET /api/tests`
  - `GET /api/tests/{id}`
  - `POST /api/tests/{id}/sessions`（需认证）
  - `POST /api/tests/sessions/{sessionId}/responses`
  - `GET /api/tests/sessions/{sessionId}/result`

- 统一测评后台（管理员）
  - `POST /api/admin/tests`
  - `POST /api/admin/tests/{id}/import`
  - `GET /api/admin/tests/{id}/export`

## 关键类与示例
- 启动类：`BlueCatServerApplication`（见源码）
- 主要模块：`controller`、`service`、`mapper`、`entity`、`security`

## API 概览（示例）
- `GET /` 首页或健康检查
- `POST /upload` 文件上传
- `GET /mbti/...`、`GET /scl90/...` 心理测评相关
- 统一测评接口以 `/api/tests*` 命名空间提供

## 开发常用命令
```bash
# 仅编译（不打包）
mvn compile

# 运行测试
mvn test

# 清理产物
mvn clean
```

## 注意事项
- `target/` 是构建产物目录，每次构建都会再生；不要提交到版本库。
- 如需修改最终 JAR 名称，可在 `server/pom.xml` 中调整：
  - 直接改 `<artifactId>`，或在 `<build>` 内设置 `<finalName>` 固定输出名。

 

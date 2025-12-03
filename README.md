## 项目说明：Code Examner 全栈应用

本项目包含 **后端 Spring Boot 服务** 和 **前端 Vue 3 + Vite 单页应用**，用于实现在线编程评测与考试平台的集成开发。

### 目录结构

- **Code-Examner-master**：后端项目（基于 Spring Boot 3，Java 21，Maven）
  - 主要配置：
    - `src/main/resources/application.yml`：端口 `8081`，上下文路径 `/api`
- **frontend**：前端项目（Vue 3 + Vite + TypeScript）
  - API 客户端默认使用 `/api` 作为前缀，并通过 Vite 代理转发到后端

### 本地开发启动方式

- **启动后端（在项目根目录）**

```bash
cd Code-Examner-master
mvn spring-boot:run
```

- **启动前端（新开一个终端，在项目根目录）**

```bash
cd frontend
npm install        # 首次需要
npm run dev        # 默认 http://localhost:5173
```

前端开发环境已在 `vite.config.ts` 中配置了代理：

- 所有以 `/api` 开头的请求会被转发到 `http://localhost:8081`，与后端的 `server.servlet.context-path=/api` 保持一致。

### 生产环境部署思路（简要）

1. 使用 Maven 打包后端：

```bash
cd Code-Examner-master
mvn clean package
```

2. 使用 Vite 构建前端静态资源：

```bash
cd frontend
npm run build
```

3. 将 `frontend/dist` 下的静态文件部署到 Nginx 或其他静态服务器，并将其反向代理到后端 `http://<your-domain>:8081/api`。

> 如你有需要，我可以进一步帮你配置 Nginx 或整合为 Docker / docker-compose 一键启动方案。





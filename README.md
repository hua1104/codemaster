## 项目说明：Code Examner 全栈应用

本项目包含 **后端 Spring Boot 服务** 和 **前端 Vue 3 + Vite 单页应用**，用于实现在线编程评测与考试平台的集成开发。

### 目录结构（仓库根目录）

- `Code-Examner-master/`：后端项目（Spring Boot 3 + Java 21 + Maven）
  - `src/main/java/com/CodeExamner/`：核心业务代码（控制器 / 服务 / 实体等）
  - `src/main/resources/application.yml`：后端配置（端口 `8081`，上下文路径 `/api`）
  - `pom.xml`：Maven 项目配置
- `frontend/`：前端项目（Vue 3 + Vite + TypeScript + Element Plus）
  - `src/`：前端源码（路由、视图、组件、API 客户端等）
  - `vite.config.ts`：开发代理与构建配置
  - `package.json`：前端依赖与脚本
- `.gitignore`：统一的 Git 忽略规则（排除 `node_modules/`、`target/` 等构建产物）

> 建议直接以当前目录作为 Git 仓库根目录推送到 GitHub，即：`软件工程/` 作为仓库根。

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

### 在 Windows 中用 Docker 启动 Judge0 评测引擎（推荐）

后端已经内置对 Judge0 的调用逻辑，只需要在本机启动 Judge0 即可开始评测。

1. 安装 Docker Desktop for Windows（官网下载安装，启用 WSL2 支持）。
2. 在 PowerShell 中拉取并启动 Judge0：

```powershell
docker pull judge0/api:latest
docker run -d --name judge0 -p 2358:2358 judge0/api:latest
```

3. 确认后端配置与端口一致（`Code-Examner-master/src/main/resources/application.yml`）：

```yaml
app:
  judge0:
    base-url: http://localhost:2358   # 与 docker run 暴露的端口一致
    callback-url: http://localhost:8081/api/judge/callback
```

4. 重新启动后端，即可通过 Judge0 进行代码评测。

### 上传到 GitHub 的推荐步骤

以下命令均在当前目录（`C:\Users\jin\Desktop\软件工程`）执行：

1. 初始化 Git 仓库（如果还没有）：

```bash
git init
```

2. 查看当前状态，确认 `node_modules`、`target` 等未被跟踪（`.gitignore` 已经为你排除这些目录）：

```bash
git status
```

如发现曾经提交过的 `node_modules` 或 `target`，可以用：

```bash
git rm -r --cached frontend/node_modules Code-Examner-master/target
```

3. 添加并提交当前代码：

```bash
git add .
git commit -m "Initial commit: Code Examner fullstack project"
```

4. 在 GitHub 上新建一个空仓库（例如 `code-examner`），然后在本地关联并推送：

```bash
git remote add origin https://github.com/<你的用户名>/code-examner.git
git branch -M main
git push -u origin main
```

这样 GitHub 仓库的结构会非常清晰：

- 根目录只有必要的源代码与配置；
- `node_modules/`、`dist/`、`target/` 等体积较大的构建产物不会被上传；
- README.md 直接说明了如何运行和部署这个项目。

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





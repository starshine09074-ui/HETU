# 河图云端开发与测试

本仓库使用 Spring Boot 4.1.1 / Java 17、Maven 3.9、Vue 3 + Vite 6 / Node.js 20、MySQL 8.4 和 Redis 7.4。后端由 `ruoyi-admin` 聚合启动，河图业务代码在 `hetu-project`；前端目录为 `hetu-ui`。本地配置和 CI 使用的账号仅用于隔离的测试数据库，不能用于生产。

## GitHub Codespaces 一键启动

1. 在 GitHub 仓库页选择 **Code → Codespaces → Create codespace on main**。仓库中的 `.devcontainer/devcontainer.json` 会准备 Java 17、Maven、Node 20 和 MySQL 客户端；Compose 同时启动 MySQL 8.4 和 Redis 7.4。
2. 首次创建时自动安装 MySQL 客户端、执行 `npm ci` 和 `mvn -DskipTests package`；容器启动后会自动运行后端及 Vite 前端。
3. 在 Codespaces 的 **Ports** 面板打开 **5173**，或用自动打开的预览访问河图工作台。仅用户工作台端口会被转发；后端 8080、MySQL 3306、Redis 6379 不转发，数据库和缓存也没有宿主机端口映射。前端 Vite 代理会将 `/api` 转发给后端；启动脚本只把当前 Codespace 的 5173 HTTPS 主机名加入后端 CORS 白名单。

如果 Codespaces 尚未自动打开预览，可在仓库根目录运行：

```bash
bash .devcontainer/start-dev.sh
```

日志位于 `/tmp/hetu-backend.log` 和 `/tmp/hetu-frontend.log`。后端内部地址为 `http://localhost:8080`。登录验证码默认开启，请先在登录页完成计算验证码。

### 本地演示账号

| 账号 | 角色 | 密码 |
| --- | --- | --- |
| `admin` | 系统管理员 | `admin123` |
| `pm` | 产品经理 | `admin123` |
| `director` | 技术总监 | `admin123` |
| `frontend` / `backend` | 前端 / 后端开发 | `admin123` |
| `tester` | 测试人员 | `admin123` |
| `ops` | 运维工程师 | `admin123` |

这些弱口令只由 `.devcontainer` 的隔离 MySQL 卷和 `sql/hetu_demo_users.sql` 提供。不要将该 SQL 导入共享、生产或含真实数据的数据库。

## 手动运行

需要 Java 17+、Maven 3.9+、Node.js 20+、Docker Compose v2。下面演示在本机 Docker 主机运行：

```bash
MYSQL_ROOT_PASSWORD=hetu-local-only docker compose -f compose.yaml -f compose.demo.yaml up -d mysql redis
export HETU_DB_URL='jdbc:mysql://localhost:3306/ry-vue?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8'
export HETU_DB_USERNAME=root
export HETU_DB_PASSWORD=hetu-local-only
export HETU_REDIS_HOST=localhost
export HETU_CORS_ALLOWED_ORIGINS=http://localhost:5173
export HETU_TOKEN_SECRET="$(openssl rand -hex 64)"
mvn -DskipTests package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

另开一个终端运行前端：

```bash
npm --prefix hetu-ui ci
npm --prefix hetu-ui run dev -- --host 0.0.0.0
```

浏览器打开 `http://localhost:5173`。本机 Compose 仅将 MySQL 和 Redis 绑定到本机 loopback，不会暴露到外部网络。

## 测试

GitHub Actions 工作流 `.github/workflows/ci.yml` 会在推送到 `main`、针对 `main` 的 Pull Request 和手动触发时执行：

1. `npm ci` 与 `npm run build`。当前前端 `package.json` 没有单元测试脚本，CI 会执行可用的前端依赖安装和生产构建检查。
2. `mvn test package`，执行 Java 单元测试并打包后端。
3. MySQL 8.4 和 Redis 7.4 上的真实 HTTP 冒烟测试：前端首页、验证码/登录初始化接口、登录 API、认证后的项目 API。
4. `scripts/e2e_workflow.py` 的需求→任务→进度→测试→缺陷扣分→发布→产品验收全流程集成测试。

CI 会在只用于该次运行的一次性 MySQL 中导入演示用户，并在启动应用前临时关闭该库的验证码，以便自动化登录。这个设置不会修改仓库默认 SQL 或开发数据库配置。最小冒烟检查也可对正在运行的隔离测试环境执行：

```bash
python3 scripts/smoke_test.py http://localhost:5173 http://localhost:8080 admin admin123
```

完整业务闭环脚本会写入测试项目记录，只能对一次性测试库运行。该脚本需要测试库预先导入 `sql/ry_20260417.sql`、`sql/hetu.sql`、`sql/hetu_demo_users.sql`，并仅在此隔离库中关闭验证码：

```bash
docker compose -f compose.yaml -f compose.demo.yaml exec -T mysql \
  mysql -uroot -phetu-local-only ry-vue \
  -e "UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'"
python3 scripts/e2e_workflow.py http://localhost:8080
```

## 故障排查与停止资源

- 前端无法连接 API：确认后端已启动；在 Codespaces 中访问前端时使用 `/api` 代理，API 端口不需要公开转发。
- 后端启动失败：查看 `/tmp/hetu-backend.log`，确认 MySQL/Redis 健康、`HETU_TOKEN_SECRET` 至少 64 字节，且 JDBC 主机名在当前环境可解析。Compose 启动的服务名是 `mysql` 和 `redis`；本机终端运行时才使用 `127.0.0.1`。
- 登录失败：开发默认开启验证码；确认输入图片中的算式结果。测试脚本若提示验证码开启，只能在全新、隔离的测试数据库中按上方命令关闭。
- 初始化 SQL 只在 MySQL 数据卷首次创建时自动导入。已有卷不会重复执行；只在本地一次性测试数据可丢弃时才用 `docker compose -f compose.yaml -f compose.demo.yaml down -v` 重建。`-v` 会永久删除这个 Compose 项目的数据库和 Redis 数据。
- 停止 Codespaces：在 Codespaces 页面选择 **Stop codespace**；这会停止整个开发环境，数据库数据卷会保留。重新打开 Codespace 时容器自动恢复。
- 停止本机手动启动的依赖但保留数据：`MYSQL_ROOT_PASSWORD=hetu-local-only docker compose -f compose.yaml -f compose.demo.yaml stop`；重启用相同命令的 `start` 替换 `stop`。
- 删除本机服务和数据卷：仅在确认本地数据可丢弃后运行 `MYSQL_ROOT_PASSWORD=hetu-local-only docker compose -f compose.yaml -f compose.demo.yaml down -v`。

## 本次本地验证结果

2026-10-17 在隔离的容器网络和一次性数据卷上执行：

- `npm --prefix hetu-ui run build`：通过；Vite 报告主 JS chunk 大于 500 kB 的既有优化提示。
- `npm_config_cache=/tmp/hetu-npm-cache npm --prefix hetu-ui ci`：通过；后续 Vite build 也通过。
- `mvn --batch-mode --no-transfer-progress test package`：通过；Hetu 计分单元测试 4 项通过，其他 Java 模块目前没有测试类。单元测试覆盖开发类比例扣分、需求/环境固定扣分、免扣分/未关闭状态，以及开发缺陷必须关联任务。
- `.devcontainer/compose.yaml`：MySQL 8.4.11、Redis 7.4-alpine 均启动并通过 healthcheck；MySQL 自动导入基础表、业务表和本地演示账号，初始验证码开关为 `true`。
- `scripts/smoke_test.py`：4 项检查通过（前端首页、登录初始化接口、登录、认证后的项目 API）。测试期间仅在一次性库里关闭验证码。
- `scripts/e2e_workflow.py`：146 项断言通过，覆盖需求至发布闭环、失败/通过测试、缺陷扣分/免扣分及发布验收。
- Dev Container CLI 0.89.0 成功解析 `.devcontainer/devcontainer.json`；Vite 接受当前 Codespace 的 HTTPS 转发域名并以 HTTP 403 拒绝不受信任 Host；后端 CORS 预检允许该 Codespace 域名并拒绝任意外部域名。

GitHub Actions 工作流文件已做 YAML 语法检查，**尚未在 GitHub Actions 上实际触发**。Dev Container CLI 的完整 `up` 未能在本执行环境完成：Docker 构建容器无法访问 Java Feature 所需的 `get.sdkman.io` 下载站；Codespaces 自身构建需在 GitHub 环境里再验证。未运行或被基础设施阻断的检查不能标记为通过。

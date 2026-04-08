# 饭搭 (FanDa) 项目启动指南

## 环境要求

### 后端环境
- **Java**: JDK 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Git**: 最新版本

### 前端环境
- **Node.js**: 18.0+
- **npm**: 9.0+
- **微信开发者工具**: 最新版（仅小程序开发需要）

---

## 快速启动

### 第一步：启动 MySQL 数据库

#### 方式一：本地 MySQL 服务

```bash
# Windows - 启动 MySQL 服务
net start MySQL80

# macOS - 使用 Homebrew
brew services start mysql

# Linux - 使用 systemctl
sudo systemctl start mysql
```

#### 方式二：Docker 启动 MySQL

```bash
# 拉取 MySQL 镜像
docker pull mysql:8.0

# 启动 MySQL 容器
docker run --name fanda-mysql \
  -e MYSQL_ROOT_PASSWORD=tmh123456 \
  -e MYSQL_DATABASE=fanda \
  -p 3306:3306 \
  -d mysql:8.0

# 验证连接
mysql -h localhost -u root -p
# 输入密码: tmh123456
```

#### 初始化数据库

```bash
# 进入 fanda-server 目录
cd fanda-server

# 执行初始化脚本
mysql -h localhost -u root -p < src/main/resources/db/schema.sql
# 输入密码: tmh123456
```

---

### 第二步：启动后端服务

```bash
# 进入后端项目目录
cd fanda-server

# 方式一：使用 Maven 直接运行
mvn clean spring-boot:run

# 方式二：先编译再运行
mvn clean package
java -jar target/fanda-server-1.0.0.jar

# 方式三：在 IDE 中运行
# 在 IntelliJ IDEA 或 Eclipse 中打开项目
# 右键点击 FandaServerApplication.java → Run
```

**启动成功标志**:
```
Started FandaServerApplication in X.XXX seconds
```

**后端服务地址**: `http://localhost:8080`

**API 文档**: `http://localhost:8080/swagger-ui.html` (如果配置了 Swagger)

---

### 第三步：启动前端服务

#### H5 开发模式（浏览器预览）

```bash
# 进入前端项目目录
cd fanda

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev:h5

# 浏览器自动打开，默认地址: http://localhost:5173
# 建议使用 Chrome 开发者工具 (F12) 切换到手机模式查看
```

#### 微信小程序开发模式

```bash
# 进入前端项目目录
cd fanda

# 编译小程序
npm run dev:mp-weixin

# 打开微信开发者工具
# 1. 启动微信开发者工具
# 2. 点击"导入项目"
# 3. 选择项目路径: fanda-project/fanda
# 4. 编译产物目录: dist/dev/mp-weixin
# 5. 在 manifest.json 中填写你的小程序 appid
```

---

## 完整启动流程

### 一键启动脚本（Windows）

创建 `start.bat` 文件：

```batch
@echo off
echo ========== 饭搭项目启动脚本 ==========
echo.

echo [1/3] 启动 MySQL 数据库...
net start MySQL80
timeout /t 3

echo [2/3] 启动后端服务...
cd fanda-server
start cmd /k "mvn clean spring-boot:run"
timeout /t 10

echo [3/3] 启动前端服务...
cd ..\fanda
start cmd /k "npm install && npm run dev:h5"

echo.
echo ========== 启动完成 ==========
echo 后端服务: http://localhost:8080
echo 前端服务: http://localhost:5173
echo.
pause
```

### 一键启动脚本（macOS/Linux）

创建 `start.sh` 文件：

```bash
#!/bin/bash

echo "========== 饭搭项目启动脚本 =========="
echo ""

echo "[1/3] 启动 MySQL 数据库..."
brew services start mysql
sleep 3

echo "[2/3] 启动后端服务..."
cd fanda-server
mvn clean spring-boot:run &
BACKEND_PID=$!
sleep 10

echo "[3/3] 启动前端服务..."
cd ../fanda
npm install
npm run dev:h5 &
FRONTEND_PID=$!

echo ""
echo "========== 启动完成 =========="
echo "后端服务: http://localhost:8080"
echo "前端服务: http://localhost:5173"
echo ""
echo "按 Ctrl+C 停止所有服务"

wait
```

运行脚本：
```bash
chmod +x start.sh
./start.sh
```

---

## 常见问题排查

### 问题 1：MySQL 连接失败

**错误信息**:
```
java.sql.SQLException: Access denied for user 'root'@'localhost'
```

**解决方案**:
1. 检查 MySQL 是否启动
2. 验证用户名和密码（默认: root / tmh123456）
3. 修改 `application.yml` 中的数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fanda
    username: root
    password: your_password  # 改为你的密码
```

---

### 问题 2：JPA EntityManager 初始化失败

**错误信息**:
```
Error creating bean with name 'jpaSharedEM_entityManagerFactory'
Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'
```

**解决方案**:
1. 确保 MySQL 数据库已启动
2. 确保数据库 `fanda` 已创建
3. 运行初始化脚本: `mysql -u root -p < schema.sql`
4. 检查 `application.yml` 中的 `ddl-auto` 设置

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 自动创建/更新表结构
```

---

### 问题 3：前端无法连接后端

**错误信息**:
```
CORS error: Access to XMLHttpRequest blocked by CORS policy
```

**解决方案**:
1. 确保后端服务已启动 (http://localhost:8080)
2. 检查 `CorsConfig.java` 中的 CORS 配置
3. 前端 API 请求地址应为: `http://localhost:8080/api/...`

---

### 问题 4：npm 依赖安装失败

**错误信息**:
```
npm ERR! code ERESOLVE
npm ERR! ERESOLVE unable to resolve dependency tree
```

**解决方案**:
```bash
# 清除 npm 缓存
npm cache clean --force

# 删除 node_modules 和 package-lock.json
rm -rf node_modules package-lock.json

# 重新安装
npm install --legacy-peer-deps
```

---

## 开发工作流

### 后端开发

```bash
# 1. 进入后端目录
cd fanda-server

# 2. 启动开发服务器（支持热重载）
mvn clean spring-boot:run

# 3. 修改代码后，IDE 会自动重新编译
# 4. 刷新浏览器或重新发送 API 请求测试

# 5. 运行测试
mvn test

# 6. 构建生产包
mvn clean package -DskipTests
```

### 前端开发

```bash
# 1. 进入前端目录
cd fanda

# 2. 启动开发服务器（支持热更新）
npm run dev:h5

# 3. 修改代码后，浏览器会自动刷新
# 4. 使用 Chrome DevTools 调试

# 5. 构建生产版本
npm run build:h5
# 产物在: dist/build/h5
```

---

## 生产部署

### 后端部署

```bash
# 1. 编译生产包
cd fanda-server
mvn clean package -DskipTests

# 2. 上传 JAR 文件到服务器
scp target/fanda-server-1.0.0.jar user@server:/app/

# 3. 在服务器上运行
java -jar fanda-server-1.0.0.jar \
  --server.port=8080 \
  --spring.datasource.url=jdbc:mysql://db-server:3306/fanda \
  --spring.datasource.username=root \
  --spring.datasource.password=your_password \
  --jwt.secret=your-production-secret-key
```

### 前端部署

#### H5 部署到 Web 服务器

```bash
# 1. 构建生产版本
cd fanda
npm run build:h5

# 2. 上传 dist/build/h5 目录到 Web 服务器
scp -r dist/build/h5/* user@server:/var/www/html/fanda/

# 3. 配置 Web 服务器（Nginx 示例）
# 在 /etc/nginx/sites-available/fanda 中添加:
server {
    listen 80;
    server_name fanda.example.com;
    
    root /var/www/html/fanda;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080;
    }
}
```

#### 小程序部署

```bash
# 1. 构建生产版本
npm run build:mp-weixin

# 2. 在微信开发者工具中上传
# 3. 在微信公众平台提交审核
```

---

## 项目配置文件

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fanda
    username: root
    password: tmh123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
  
jwt:
  secret: fanda-secret-key-change-this-in-production
  access-expiration: 7200000      # 2 小时
  refresh-expiration: 604800000   # 7 天

logging:
  level:
    com.fanda: DEBUG
```

### 前端配置 (`vite.config.js`)

```javascript
import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

---

## 监控和日志

### 查看后端日志

```bash
# 实时查看日志
tail -f fanda-server.log

# 查看特定错误
grep ERROR fanda-server.log

# 查看 JWT 相关日志
grep JWT fanda-server.log
```

### 查看前端控制台

```bash
# 在浏览器中打开开发者工具 (F12)
# 查看 Console 标签页的日志和错误
```

---

## 性能优化建议

### 后端优化
- 启用数据库连接池 (HikariCP)
- 添加缓存层 (Redis)
- 使用数据库索引优化查询
- 启用 Gzip 压缩

### 前端优化
- 启用代码分割和懒加载
- 压缩静态资源
- 使用 CDN 加速
- 启用浏览器缓存

---

## 相关文档

- [项目架构文档](ARCHITECTURE.md)
- [前端 README](fanda/README.md)
- [后端 API 文档](fanda-server/docs/API.md)（如果存在）

---

## 获取帮助

- 查看项目 README: `fanda/README.md`
- 检查错误日志
- 查看 Spring Boot 官方文档: https://spring.io/projects/spring-boot
- 查看 uni-app 官方文档: https://uniapp.dcloud.io/

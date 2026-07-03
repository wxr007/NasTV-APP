# API 变更记录 (2026-07-01 -> 2026-07-02)

## 破坏性变更

### 1. 登录接口请求体变更

| 项目 | 旧版 | 新版 |
|------|------|------|
| 请求体 | `{ "password": "admin123" }` | `{ "username": "admin", "password": "admin123" }` |

旧版仅需密码即可登录，新版需要同时提供用户名和密码。**旧客户端将无法登录新服务端。**

### 2. 认证机制升级

| 项目 | 旧版 | 新版 |
|------|------|------|
| 认证方式 | 仅 Cookie | Cookie + Bearer Token |
| 用户角色 | 无 | admin（管理员）/ user（普通用户） |
| `GET /api/auth/me` 响应 | `{ "username": "admin" }` | `{ "id": "admin", "username": "admin", "role": "admin" }` |
| `POST /api/auth/login` 响应 | `{ "success": true, "username": "admin" }` | `{ "success": true, "username": "admin", "role": "admin" }` |

### 3. 管理接口权限升级

| 项目 | 旧版 | 新版 |
|------|------|------|
| 权限要求 | 需要登录 | 需要管理员 role（非管理员返回 403） |
| 默认管理员 | 可通过接口修改/删除 | **不可**通过用户管理接口修改密码或删除 |

---

## 新增

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/auth/token` | 获取 Bearer Token（移动端/客户端使用），响应含 `role` 字段 |
| `GET` | `/api/admin/users` | 获取用户列表（管理员） |
| `POST` | `/api/admin/users` | 创建用户 |
| `PUT` | `/api/admin/users/:username` | 修改用户密码 |
| `DELETE` | `/api/admin/users/:username` | 删除用户（同时清除其所有 session） |
| 参数 | `/api/play/m3u8?raw=1` | 新增 `raw` 参数，设 `1` 时原样返回不重写 URL（用于缓存下载） |

---

## 删除

| 方法 | 路径 | 旧编号 | 说明 |
|------|------|--------|------|
| `GET` | `/api/admin/site-config` | #39 | 已删除，站点名称改为从公开接口 `/api/site-config` 获取 |
| `PUT` | `/api/admin/site-config` | #40 | 已删除 |

---

## 修改

### m3u8 代理增强

`GET /api/play/m3u8` 现在支持解析 share 页面（HTML），自动提取真实 m3u8 URL（含 sign 签名参数）。

### 编号变更

由于新增 `POST /api/auth/token`（#13），以下接口编号后移：

| 接口 | 旧编号 | 新编号 |
|------|--------|--------|
| 验证登录状态 `GET /api/auth/me` | #13 → **#14** |
| 登出 `POST /api/auth/logout` | #14 → **#15** |
| 修改密码 `PUT /api/auth/password` | #15 → **#16** |
| 收藏列表~缓存 m3u8 | #16~38 → **#17~39** |
| 管理后台接口 | #39~44 → **#40~47** |

### EpisodeCacheStatus 类型字段顺序调整

```diff
  interface EpisodeCacheStatus {
    state: CacheState;
+   title: string;          // 从末尾移至第二
    progress: number;
    total: number;
    cached: number;
    totalBytes: number;
    cachedBytes: number;
    updatedAt: string;
-   error?: string;         // 从中间移至末尾
-   title: string;          // 移至上方
    episodeUrl: string;
    picUrl: string;
+   error?: string;         // 移至末尾
  }
```

### 缓存接口 saveSegment 行为变更

`POST /api/cache/upload/segment` 现在会在写入前自动创建缓存目录（之前目录不存在会报错 ENOENT）。

### 启动时缓存任务恢复优化

服务启动时 `restoreInterruptedTasks` 会检查缓存目录是否存在，目录已被手动删除的任务会自动清理 Redis 记录（之前会保留孤儿数据）。

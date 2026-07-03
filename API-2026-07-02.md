# NasTV API 文档

基础路径：`/api`（示例：`GET /api/sources`）

## 响应格式

成功：`{ "code": 0, "data": ..., "message": "ok" }`
错误：`{ "code": -1, "message": "错误描述" }`

认证接口（`/api/auth/*`）使用不同的响应格式，详见认证章节。

---

## 公开接口（无需登录）

### 1. API 健康检查
```
GET /
```
响应：`NasTV API is running`

### 2. 获取站点配置
```
GET /api/site-config
```
响应：
```json
{ "code": 0, "data": { "siteName": "NasTV" } }
```

### 3. 获取视频源列表
```
GET /api/sources
```
响应：
```json
{ "code": 0, "data": [{ "id": "dyttzyapi", "name": "天堂资源", "url": "http://...", "enabled": true }] }
```

### 4. 获取视频源分类
```
GET /api/source/:sourceId/categories
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |

响应：
```json
{ "code": 0, "data": [{ "id": "1", "name": "电影", "children": [{ "id": "6", "name": "动作片" }] }] }
```

### 5. 获取视频列表
```
GET /api/source/:sourceId/videos?page=1&category=6&keyword=蜘蛛侠
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| page | 页码，可选 |
| category | 分类 ID，可选 |
| keyword | 搜索关键词，可选 |

响应：
```json
{ "code": 0, "data": { "videos": [{ "id": "123", "name": "标题", "pic": "...", "year": "2024", "area": "中国", "type": "动作", "remark": "更新至XX集","episodeCount": 279, "sourceId": "dyttzyapi" }], "total": 100 } }
```

### 6. 聚合搜索
```
GET /api/search?keyword=蜘蛛侠
```
| 参数 | 说明 |
|------|------|
| keyword | 搜索关键词 |

响应：
```json
{ "code": 0, "data": { "videos": [{ "id": "123", "name": "标题", "pic": "...", "year": "2024", "area": "中国", "type": "动作", "remark": "更新至XX集","sourceId": "dyttzyapi" }], "total": 50 } }
```
说明：跨所有已启用的视频源搜索，结果已去重。

### 7. 搜索建议
```
GET /api/search/suggest?keyword=蜘蛛
```
| 参数 | 说明 |
|------|------|
| keyword | 搜索关键词 |

响应：
```json
{ "code": 0, "data": ["蜘蛛侠", "蜘蛛侠3"] }
```
说明：返回最多 10 个建议词。

### 8. 获取视频详情
```
GET /api/play/detail?sourceId=dyttzyapi&id=123
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID |
| id | 视频 ID |

响应：
```json
{ "code": 0, "data": { "id": "123", "name": "标题", "pic": "url", "year": "2024","area": "地区", "type": "分类", "actor": "演员", "director": "导演", "content": "简介", "playFrom": ["线路1"], "episodes": [[{ "name": "第1集", "url": "..." }]], "sourceId": "dyttzyapi" } }
```

### 9. 代理 m3u8 播放列表
```
GET /api/play/m3u8?url=https://...
```
| 参数 | 说明 |
|------|------|
| url | 远程 m3u8 地址 |
| raw | 可选，设 `1` 时原样返回不重写 URL（用于缓存下载） |

说明：代理获取并返回 m3u8 内容，`Content-Type: application/vnd.apple.mpegurl`。支持解析 share 页面提取真实 m3u8 URL。

### 10. 代理 TS 分片
```
GET /api/play/ts?url=https://...
```
| 参数 | 说明 |
|------|------|
| url | 远程 TS 地址 |

说明：代理获取并返回 TS 二进制数据，`Content-Type: video/MP2T`

### 11. 代理图片
```
GET /api/play/image?url=https://...&sourceId=xxx&videoId=xxx
```
| 参数 | 说明 |
|------|------|
| url | 远程图片 URL |
| sourceId | 视频源 ID（可选，用于按源分目录缓存） |
| videoId | 视频 ID（可选，用作缓存文件名） |

说明：代理获取并返回图片数据，`Content-Type: image/jpeg`

---

## 认证接口

### 12. 登录
```
POST /api/auth/login
```
请求体：
```json
{ "username": "admin", "password": "admin123" }
```
响应：设置 httpOnly cookie `token`（24小时有效）
```json
{ "success": true, "username": "admin", "role": "admin" }
```

### 13. 获取 Bearer Token（移动端/客户端使用）
```
POST /api/auth/token
```
请求体：
```json
{ "username": "admin", "password": "admin123" }
```
响应：
```json
{
  "code": 0,
  "data": {
    "token": "uuid-token",
    "expiresIn": 86400,
    "username": "admin",
    "role": "admin"
  }
}
```
说明：返回的 token 需要在后续请求的 `Authorization: Bearer <token>` 头部中携带。role 取值 `admin`（环境变量中配置的默认管理员）或 `user`（通过管理后台创建的普通用户）。

### 14. 验证登录状态
```
GET /api/auth/me
```
从 cookie 中读取 token 或从 `Authorization` 头部读取 Bearer token 验证。
响应：
```json
{ "id": "admin", "username": "admin", "role": "admin" }
```
未登录返回 HTTP 401。

### 15. 登出
```
POST /api/auth/logout
```
清除 cookie，使 token 失效。
响应：
```json
{ "success": true }
```

### 16. 修改密码
```
PUT /api/auth/password
```
请求体：
```json
{ "oldPassword": "admin123", "newPassword": "newpass456" }
```
响应：
```json
{ "code": 0, "message": "修改成功" }
```

---

## 需要登录的接口（以下接口均需 cookie 中携带有效 token 或 Authorization: Bearer token）

### 17. 获取收藏列表
```
GET /api/favorites
```
响应：
```json
{
  "code": 0,
  "data": {
    "dyttzyapi:80982": {
      "name": "女神“异世界转生想成为什么”我“勇者的肋骨”",
      "pic": "https://vod.dyttimage.com/upload/vod/20260408-1/0f811249c21dd914c79287cce8e292a8.webp",
      "remark": "已完结"
    }
  }
}
```
说明：返回 `{ key: { name, pic, remark } }` 格式的收藏映射。

### 18. 添加收藏
```
POST /api/favorites
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "name": "标题", "pic": "...", "remark": "备注" }
```
参数说明：sourceId 和 videoId 为必填，name、pic、remark 可选。
响应：
```json
{ "code": 0, "message": "ok" }
```

### 19. 取消收藏
```
DELETE /api/favorites/:sourceId/:videoId
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |

响应：
```json
{ "code": 0, "message": "ok" }
```

### 20. 获取播放历史
```
GET /api/play-history
```
响应：
```json
{
  "code": 0,
  "data": [
    {
      "key": "dyttzyapi:80982",
      "sourceId": "dyttzyapi",
      "videoId": "80982",
      "name": "女神“异世界转生想成为什么”我“勇者的肋骨”",
      "pic": "https://vod.dyttimage.com/upload/vod/20260408-1/0f811249c21dd914c79287cce8e292a8.webp",
      "progress": 26.496523,
      "duration": 1389.2066680000041,
      "timestamp": 1782891828124,
      "episode": "第01集",
      "totalEpisodes": 12,
      "activeSource": 1,
      "activeEpisodeIndex": 0
    }
  ]
}
```

### 21. 更新播放进度
```
PUT /api/play-history
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "progress": 120, "episode": "第1集", "name": "标题", "pic": "...", "duration": 3600, "totalEpisodes": 30, "activeSource": 0, "activeEpisodeIndex": 2 }
```
参数说明：sourceId、videoId、progress、episode、name 为必填，其余可选。
响应：
```json
{ "code": 0, "message": "ok" }
```

### 22. 获取播放进度
```
GET /api/play-progress/:sourceId/:videoId
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |

响应：
```json
{ "code": 0, "data": { "progress": 120, "duration": 3600, "episode": "第1集", "totalEpisodes": 30, "name": "标题", "pic": "...", "activeSource": 0, "activeEpisodeIndex": 2 } }
```

### 23. 删除播放记录
```
DELETE /api/play-history/:sourceId/:videoId
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |

响应：
```json
{ "code": 0, "message": "ok" }
```

### 24. 获取搜索历史
```
GET /api/search-history
```
响应：
```json
{ "code": 0, "data": ["关键词1", "关键词2"] }
```

### 25. 清空搜索历史
```
DELETE /api/search-history
```
响应：
```json
{ "code": 0, "message": "ok" }
```

### 26. 获取跳过配置
```
GET /api/skip-config/:videoId
```
| 参数 | 说明 |
|------|------|
| videoId | 视频 ID（路径参数） |

响应：
```json
{ "code": 0, "data": { "skipStart": 30, "skipEnd": 60 } }
```

### 27. 设置跳过配置
```
PUT /api/skip-config/:videoId
```
| 参数 | 说明 |
|------|------|
| videoId | 视频 ID（路径参数） |

请求体：
```json
{ "skipStart": 30, "skipEnd": 60 }
```
响应：
```json
{ "code": 0, "message": "ok" }
```

---

## 缓存相关接口（需要登录）

### 28. 开始缓存视频
```
POST /api/cache/start
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0, "url": "https://example.com/playlist.m3u8", "title": "电影名", "episodeUrl": "https://example.com/ep1.m3u8", "picUrl": "https://example.com/pic.jpg" }
```
参数说明：sourceId、videoId、episodeIndex、url 为必填，title、episodeUrl、picUrl 可选。
响应：
```json
{ "code": 0, "message": "开始缓存" }
```

### 29. 获取缓存状态
```
GET /api/cache/status/:sourceId/:videoId
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |

响应：
```json
{ "code": 0, "data": { "episodes": { "0": { "state": "completed", "progress": 100, "total": 20, "cached": 20, "totalBytes": 0, "cachedBytes": 1048576, "updatedAt": "2024-01-01T00:00:00.000Z", "title": "电影名", "episodeUrl": "...", "picUrl": "...", "error": null } } } }
```
说明：state 取值 `idle` | `caching` | `completed` | `failed`。

### 30. 获取所有缓存任务（进行中/失败）
```
GET /api/cache/tasks
```
响应：
```json
{ "code": 0, "data": [{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0, "status": { "state": "caching", "progress": 50, ... } }] }
```
说明：只返回 active（caching 或 failed）状态的任务。

### 31. 获取所有已缓存的剧集
```
GET /api/cache/episodes
```
响应：
```json
{ "code": 0, "data": [{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0, "diskSize": 52428800, "status": { "state": "completed", "progress": 100, "total": 20, "cached": 20, "totalBytes": 0, "cachedBytes": 1048576, "updatedAt": "...", "title": "电影名", "episodeUrl": "...", "picUrl": "..." } }] }
```
说明：只返回 state 为 `completed` 的缓存记录，并附带磁盘文件大小（diskSize）。

### 32. 同步缓存剧集信息
```
POST /api/cache/episode/sync
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0, "title": "剧集名称", "episodeUrl": "https://example.com/ep1.m3u8", "picUrl": "https://example.com/pic.jpg" }
```
参数说明：sourceId、videoId、episodeIndex 为必填，title、episodeUrl、picUrl 可选（只更新有值的字段）。
响应：
```json
{ "code": 0, "data": { "state": "completed", ... } }
```

### 33. 删除缓存任务
```
DELETE /api/cache/task/:sourceId/:videoId/:episodeIndex
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |
| episodeIndex | 剧集索引（路径参数） |

说明：同时删除磁盘缓存文件和 Redis 记录。
响应：
```json
{ "code": 0, "message": "删除成功" }
```

### 34. 初始化缓存上传（浏览器下载回传模式）
```
POST /api/cache/upload/init
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0, "total": 20, "title": "电影名", "episodeUrl": "https://example.com/ep1.m3u8", "m3u8Content": "#EXTM3U\n#EXT-X-VERSION:3\n...", "picUrl": "https://example.com/pic.jpg" }
```
参数说明：sourceId、videoId、episodeIndex、total 为必填，其余可选。
响应：
```json
{ "code": 0, "message": "初始化成功" }
```

### 35. 上传 TS 切片
```
POST /api/cache/upload/segment?sourceId=dyttzyapi&videoId=12345&episodeIndex=0&segmentIndex=0
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（Query 参数） |
| videoId | 视频 ID（Query 参数） |
| episodeIndex | 剧集索引（Query 参数） |
| segmentIndex | 切片索引（Query 参数） |

请求体：原始二进制数据（`Content-Type: application/octet-stream`）
响应：
```json
{ "code": 0, "message": "上传成功" }
```

### 36. 完成缓存上传
```
POST /api/cache/upload/complete
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0, "m3u8Content": "#EXTM3U\n#EXT-X-VERSION:3\n..." }
```
响应：
```json
{ "code": 0, "message": "完成上传" }
```

### 37. 继续缓存（从失败/中断恢复）
```
POST /api/cache/upload/continue
```
请求体：
```json
{ "sourceId": "dyttzyapi", "videoId": "12345", "episodeIndex": 0 }
```
说明：将 failed 状态重置为 caching，返回 cached/total/episodeUrl 等信息供前端继续上传。
响应：
```json
{ "code": 0, "data": { "sourceId": "...", "videoId": "...", "episodeIndex": 0, "episodeUrl": "...", "total": 20, "cached": 10, "progress": 50, "title": "电影名" } }
```

### 38. 获取缓存的 TS 分片
```
GET /api/cache/ts/:sourceId/:videoId/:episodeIndex/:filename
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |
| episodeIndex | 剧集索引（路径参数） |
| filename | 文件名，如 `seg_0.ts`（路径参数） |

说明：用于缓存剧集的本地点播播放。`Content-Type: video/MP2T`

### 39. 获取缓存的 m3u8 播放列表
```
GET /api/cache/m3u8/:sourceId/:videoId/:episodeIndex
```
| 参数 | 说明 |
|------|------|
| sourceId | 视频源 ID（路径参数） |
| videoId | 视频 ID（路径参数） |
| episodeIndex | 剧集索引（路径参数） |

说明：用于缓存剧集的本地点播播放。`Content-Type: application/vnd.apple.mpegurl`

---

## 管理后台接口（需要管理员权限）

说明：以下接口需要 cookie 中携带有效 token 或 `Authorization: Bearer token`，且用户 role 必须为 `admin`。默认管理员（环境变量 ADMIN_USERNAME 配置）不可删除和修改。

### 40. 获取用户列表
```
GET /api/admin/users
```
响应：
```json
{ "code": 0, "data": [{ "username": "user1", "createdAt": "2026-07-01T00:00:00.000Z" }] }
```
说明：返回所有通过管理后台创建的普通用户列表。

### 41. 创建用户
```
POST /api/admin/users
```
请求体：
```json
{ "username": "newuser", "password": "password123" }
```
响应：
```json
{ "code": 0, "data": { "username": "newuser" } }
```
错误：400（用户名密码为空或与默认管理员同名）、409（用户已存在）

### 42. 修改用户密码
```
PUT /api/admin/users/:username
```
| 参数 | 说明 |
|------|------|
| username | 用户名（路径参数） |

请求体：
```json
{ "password": "newpassword456" }
```
响应：
```json
{ "code": 0, "message": "更新成功" }
```
错误：403（默认管理员不可修改）、404（用户不存在）

### 43. 删除用户
```
DELETE /api/admin/users/:username
```
| 参数 | 说明 |
|------|------|
| username | 用户名（路径参数） |

说明：同时清除该用户的所有 session。
响应：
```json
{ "code": 0, "message": "删除成功" }
```
错误：403（默认管理员不可删除）、404（用户不存在）

### 44. 添加视频源
```
POST /api/admin/sources
```
请求体：
```json
{ "name": "新资源", "url": "http://example.com/api", "id": "custom_id" }
```
参数说明：name 和 url 为必填，id 可选（不指定则自动生成）。
响应：
```json
{ "code": 0, "data": { "id": "custom_id", "name": "新资源", "url": "http://...", "enabled": false } }
```

### 45. 更新视频源
```
PUT /api/admin/sources/:id
```
| 参数 | 说明 |
|------|------|
| id | 视频源 ID（路径参数） |

请求体：
```json
{ "name": "新名称", "url": "http://new-url.com", "enabled": true }
```
响应：
```json
{ "code": 0, "data": { "id": "dyttzyapi", "name": "新名称", "url": "...", "enabled": true } }
```

### 46. 删除视频源
```
DELETE /api/admin/sources/:id
```
| 参数 | 说明 |
|------|------|
| id | 视频源 ID（路径参数） |

响应：
```json
{ "code": 0, "message": "删除成功" }
```

### 47. 切换视频源启用状态
```
PUT /api/admin/sources/:id/toggle
```
| 参数 | 说明 |
|------|------|
| id | 视频源 ID（路径参数） |

请求体：
```json
{ "enabled": true }
```
响应：
```json
{ "code": 0, "data": { "id": "dyttzyapi", "name": "...", "enabled": true } }
```

---

## 类型定义

```typescript
interface VideoSource {
  id: string;
  name: string;
  url: string;
  enabled: boolean;
}

interface Category {
  id: string;
  name: string;
  children: { id: string; name: string }[];
}

interface Video {
  id: string;
  name: string;
  pic?: string;
  year?: string;
  area?: string;
  type?: string;
  remark?: string;
  episodeCount?: number;
  sourceId: string;
}

interface VideoDetail {
  id: string;
  name: string;
  pic?: string;
  year?: string;
  actor?: string;
  director?: string;
  content?: string;
  playFrom: string[];
  episodes: { name: string; url: string }[][];
  sourceId: string;
}

interface PlayProgress {
  key: string;
  sourceId: string;
  videoId: string;
  name: string;
  pic?: string;
  progress: number;
  duration?: number;
  episode: string;
  totalEpisodes?: number;
  activeSource?: number;
  activeEpisodeIndex?: number;
  timestamp: number;
}

interface SkipConfig {
  skipStart: number;
  skipEnd: number;
}

type CacheState = 'idle' | 'caching' | 'completed' | 'failed';

interface EpisodeCacheStatus {
  state: CacheState;
  title: string;
  progress: number;
  total: number;
  cached: number;
  totalBytes: number;
  cachedBytes: number;
  updatedAt: string;
  episodeUrl: string;
  picUrl: string;
  error?: string;
}

interface VideoCacheStatus {
  episodes: Record<number, EpisodeCacheStatus>;
}

interface CachedEpisode {
  sourceId: string;
  videoId: string;
  episodeIndex: number;
  status: EpisodeCacheStatus;
  diskSize: number;
}
```

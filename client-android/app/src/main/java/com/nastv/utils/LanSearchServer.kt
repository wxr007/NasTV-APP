package com.nastv.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * 极简局域网 HTTP 服务，用于「扫码输入搜索词」：
 * TV 展示二维码 -> 手机扫码打开网页 -> 输入关键词 -> 回传到 TV 触发搜索。
 *
 * 只实现两个路由，够用且不引入任何服务端依赖：
 *   GET  /         手机输入页面
 *   POST /submit   提交关键词（application/x-www-form-urlencoded，字段 keyword）
 */
class LanSearchServer(
    private val basePort: Int = DEFAULT_PORT,
    private val onKeyword: (String) -> Unit
) {

    companion object {
        private const val TAG = "LanSearchServer"
        const val DEFAULT_PORT = 8976
        private const val PORT_TRY_COUNT = 10
        private const val MAX_KEYWORD_LENGTH = 60
    }

    private var serverSocket: ServerSocket? = null
    private var scope: CoroutineScope? = null

    @Volatile
    private var running = false

    @Volatile
    private var port: Int = basePort

    /** 手机应打开的地址；取不到局域网 IP 时为 null */
    val url: String?
        get() = NetworkUtils.localIpv4Address()?.let { "http://$it:$port" }

    /**
     * 启动服务，返回可供手机访问的地址。端口被占用时自动往后试 10 个；
     * 全部失败或没有局域网 IP 时返回 null。
     */
    fun start(): String? {
        if (running) return url

        var bound: ServerSocket? = null
        for (p in basePort until basePort + PORT_TRY_COUNT) {
            try {
                bound = ServerSocket(p, 8).apply { reuseAddress = true }
                port = p
                break
            } catch (e: Exception) {
                Log.w(TAG, "端口 $p 不可用: ${e.message}")
            }
        }
        if (bound == null) {
            Log.e(TAG, "没有可用端口，扫码输入不可用")
            return null
        }

        serverSocket = bound
        running = true
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = serverScope
        serverScope.launch {
            while (running) {
                val client = try {
                    bound.accept()
                } catch (e: Exception) {
                    if (running) Log.w(TAG, "accept 失败: ${e.message}")
                    break
                }
                // 每个连接单独处理，避免慢请求阻塞后续访问
                serverScope.launch {
                    runCatching { handle(client) }
                        .onFailure { Log.w(TAG, "处理请求失败: ${it.message}") }
                }
            }
        }
        Log.d(TAG, "已启动: $url")
        return url
    }

    fun stop() {
        running = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        scope?.cancel()
        scope = null
    }

    private fun handle(client: Socket) {
        client.use { socket ->
            socket.soTimeout = 8000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0].uppercase()
            val path = parts[1].substringBefore('?')

            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.lowercase().startsWith("content-length:")) {
                    contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
                }
            }

            val body = if (method == "POST" && contentLength > 0) {
                val buffer = CharArray(contentLength.coerceAtMost(4096))
                var read = 0
                while (read < buffer.size) {
                    val n = reader.read(buffer, read, buffer.size - read)
                    if (n <= 0) break
                    read += n
                }
                String(buffer, 0, read)
            } else ""

            when {
                method == "POST" && path == "/submit" -> {
                    val keyword = parseForm(body)["keyword"]?.trim().orEmpty()
                    if (keyword.isEmpty()) {
                        respondJson(socket, "400 Bad Request", """{"ok":false,"error":"empty"}""")
                    } else {
                        onKeyword(keyword.take(MAX_KEYWORD_LENGTH))
                        Log.d(TAG, "收到关键词: $keyword")
                        respondJson(socket, "200 OK", """{"ok":true}""")
                    }
                }

                method == "GET" && (path == "/" || path.isEmpty()) ->
                    respondHtml(socket, "200 OK", PAGE_HTML)

                else -> respondHtml(socket, "404 Not Found", NOT_FOUND_HTML)
            }
        }
    }

    private fun parseForm(body: String): Map<String, String> =
        body.split('&')
            .filter { it.isNotBlank() }
            .mapNotNull { pair ->
                val idx = pair.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                key to value
            }
            .toMap()

    private fun respondJson(socket: Socket, status: String, json: String) =
        respond(socket, status, "application/json", json)

    private fun respondHtml(socket: Socket, status: String, html: String) =
        respond(socket, status, "text/html", html)

    private fun respond(socket: Socket, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $contentType; charset=utf-8\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }
        val out: OutputStream = socket.getOutputStream()
        out.write(header.toByteArray(Charsets.US_ASCII))
        out.write(bytes)
        out.flush()
    }

    private val NOT_FOUND_HTML = """
        <!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <title>页面不存在</title></head>
        <body style="font-family:system-ui,sans-serif;padding:24px">
        <h1 style="font-size:20px">页面不存在</h1>
        <p><a href="/">返回输入页</a></p></body></html>
    """.trimIndent()

    private val PAGE_HTML = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1">
<title>NasTV 电视搜索</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body { margin: 0; padding: 24px 20px 40px; background: #0f0f0f; color: #f5f5f5;
         font-family: system-ui, -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; }
  h1 { font-size: 20px; font-weight: 600; margin: 8px 0 6px; }
  .brand { color: #ffd700; font-size: 13px; letter-spacing: 2px; }
  .hint { color: #9ca3af; font-size: 13px; line-height: 1.7; margin: 0 0 22px; }
  .row { display: flex; gap: 10px; }
  input { flex: 1; min-width: 0; padding: 14px 16px; font-size: 16px; color: #f5f5f5;
          background: #1a1a1a; border: 1px solid #333; border-radius: 10px; outline: none; }
  input:focus { border-color: #ffd700; }
  button { flex: 0 0 auto; padding: 14px 20px; font-size: 16px; font-weight: 600;
           color: #111; background: #ffd700; border: 0; border-radius: 10px; }
  button:active { background: #e6c200; }
  .tip { min-height: 22px; margin: 16px 0 0; font-size: 14px; color: #9ca3af; line-height: 1.6; }
  .tip.ok { color: #22c55e; }
  .tip.err { color: #ef4444; }
  .foot { margin-top: 32px; font-size: 12px; color: #555; }
</style>
</head>
<body>
  <div class="brand">NASTV</div>
  <h1>电视搜索输入</h1>
  <p class="hint">手机与电视连接同一个 Wi-Fi 时，在这里输入片名并发送，电视会立刻开始搜索。</p>
  <div class="row">
    <input id="kw" type="search" placeholder="片名 / 演员 / 关键词" autocomplete="off" autocapitalize="off">
    <button id="go" type="button">发送</button>
  </div>
  <p id="tip" class="tip"></p>
  <p class="foot">发送后电视会自动跳转到搜索结果页，可继续在这里输入下一个关键词。</p>
<script>
  var input = document.getElementById('kw');
  var tip = document.getElementById('tip');
  var busy = false;
  function setTip(text, cls) {
    tip.textContent = text;
    tip.className = 'tip' + (cls ? ' ' + cls : '');
  }
  function send() {
    var kw = input.value.trim();
    if (!kw) { setTip('请输入关键词', 'err'); return; }
    if (busy) return;
    busy = true;
    setTip('发送中…');
    fetch('/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'keyword=' + encodeURIComponent(kw)
    }).then(function (res) {
      if (!res.ok) throw new Error('bad status');
      return res.json();
    }).then(function () {
      setTip('已发送「' + kw + '」，可继续输入下一个关键词', 'ok');
      input.value = '';
      input.focus();
    }).catch(function () {
      setTip('发送失败，请确认手机和电视在同一网络', 'err');
    }).then(function () {
      busy = false;
    });
  }
  document.getElementById('go').addEventListener('click', send);
  input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') { e.preventDefault(); send(); }
  });
  input.focus();
</script>
</body>
</html>
    """.trimIndent()
}

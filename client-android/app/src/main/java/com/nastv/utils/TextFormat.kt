package com.nastv.utils

/**
 * 文本格式化工具。
 *
 * 与 web 端 `client/src/utils/text.ts` 的 `formatDescription()` 规则保持一致，
 * 避免同一份简介在两端显示效果不同。
 */

/** 连续空白：半角空格/制表符 2 个及以上，或全角空格/不换行空格 1 个及以上 */
private val MULTI_SPACE_REGEX = Regex("[ \\t]{2,}|[\\u00a0\\u3000]+")

/**
 * 格式化视频简介：把连续空格折叠为一个换行符，便于按段落展示。
 *
 * - 半角空格 / 制表符：连续 2 个及以上才折叠（单个空格是正常的词间分隔）
 * - 全角空格 / 不换行空格：1 个及以上即折叠（中文正文不会用它分隔词语）
 * - 段与段之间只换行、不留空行
 */
fun formatDescription(text: String?): String {
    if (text.isNullOrBlank()) return ""

    return text
        // 统一换行符
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        // 连续空白折叠为一个换行
        .replace(MULTI_SPACE_REGEX, "\n")
        // 清理行首行尾空白，丢弃空行（避免段落间出现空行）
        .split('\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .trim()
}

package com.nastv.ui.theme

import androidx.compose.ui.graphics.Color

// Android Studio 默认主题颜色（基本未使用）
val Purple80 = Color(0xFFD0BCFF)          // 浅紫色，亮色模式主色
val PurpleGrey80 = Color(0xFFCCC2DC)      // 浅紫灰色，亮色模式表面色
val Pink80 = Color(0xFFEFB8C8)            // 浅粉色，亮色模式辅助色

val Purple40 = Color(0xFF6650a4)          // 深紫色，暗色模式主色
val PurpleGrey40 = Color(0xFF625b71)      // 深紫灰色，暗色模式表面色
val Pink40 = Color(0xFF7D5260)            // 深粉色，暗色模式辅助色

// ──────────────────── NasTV 自定义颜色 ────────────────────

// 主色与辅助色（手机端）
val NasTVPrimary = Color(0xFF6366F1)      // 靛蓝色 — 手机端主色，按钮/导航
val NasTVPrimaryDark = Color(0xFF4F46E5)  // 深靛蓝色 — 手机端主色按压态
val NasTVSecondary = Color(0xFF06B6D4)    // 青色 — 手机端辅助色，次要按钮/标记

// 背景与表面
val NasTVBackground = Color(0xFF0F0F0F)   // 深黑色 — 页面全局背景
val NasTVSurface = Color(0xFF1A1A1A)      // 深灰色 — 卡片、弹窗容器背景
val NasTVSurfaceLight = Color(0xFF2A2A2A) // 中灰色 — 较亮表面，词条/Chip 背景

// 语义色
val NasTVError = Color(0xFFEF4444)        // 红色 — 错误提示、删除按钮、选中状态
val NasTVSuccess = Color(0xFF22C55E)      // 绿色 — 成功提示、在线状态
val NasTVWarning = Color(0xFFF59E0B)      // 橙黄色 — 警告提示、过期状态

// 文字色
val NasTVTextPrimary = Color(0xFFF5F5F5)  // 近白色 — 主标题、主要内容文字
val NasTVTextSecondary = Color(0xFF9CA3AF) // 灰色 — 次要文字、边框线

// ──────────────────── TV 端专用颜色 ────────────────────

val NasTVPrimaryGold = Color(0xFFFFD700)   // 金黄色 — TV 端主色，聚焦边框/高亮/按钮
val NasTVSecondaryGold = Color(0xFFFFA500) // 橙色 — TV 端辅助色，次要标记/文案

// TV 端选中态
val NasTVAccent = Color(0xFFEF4444)         // 红色 — 按钮选中背景、分类高亮

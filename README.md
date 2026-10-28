# 笔记本 Notebook

一个只给自己用的 Android 笔记应用。纯 Java + Material Design 3，无第三方服务，不联网，数据完全在本地。

## 功能

- **普通笔记**：Markdown 编辑 + 实时预览，支持插图、导出 TXT（保留标记 / 纯文本）/ PNG。
- **思维笔记**：大纲视图与脑图视图一键切换，脑图可双指缩放、拖拽，自适应屏幕居中对齐，可导出 PNG。
- **回收站**：删除的笔记先进回收站，保留 10 天后自动销毁，显示删除时间与剩余天数，可恢复或彻底删除。
- **图片分享**：笔记（文档 / 思维导图）渲染成图片通过系统分享面板发出，只分享图片。
- **存储位置可选**：内部存储，或用 SAF 选择任意目录（配置文件 `notebook.json`），便于自行备份与同步。
- **背景与外观**：多种背景样式，可选用本地图片；全局跟随系统深浅色（MD3 主题色）。

## 环境

- compileSdk 34 / minSdk 24 / targetSdk 34，Java 17
- Gradle 8.9 + AGP 8.5，依赖 appcompat 1.7.0 / recyclerview 1.3.2 / material 1.12.0

见 [CHANGELOG.md](CHANGELOG.md)。

## 说明

个人使用，不发布到应用商店，不提供 Issues 支持。

每个版本目录包含：`*.apk`（该版正式包）与 `src/`（该版源码）。v1.3 的 `src/` 是完整可编译工程；历史版本的原始工程已被覆盖，其 `src/` 为 APK 反编译代码，仅供查看，详见各目录 `SOURCE.md`。

## Releases

每个版本都在 [Releases](https://github.com/Gardenia-Gentlemen/notebook/releases) 有对应标签，附件：成品包 `notebook-vX.apk`、源代码 `notebook-vX-src.zip`，说明为该版本更新日志。

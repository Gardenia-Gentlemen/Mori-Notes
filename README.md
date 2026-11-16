# Mori Notes

一个AI生成的 Android 笔记应用。纯 Java + Material Design 3，无第三方服务，不联网，数据完全在本地。

## 功能
- **笔记**：Markdown 编辑 + 实时预览、插图；思维笔记可在大纲与脑图间切换，脑图支持缩放拖拽。标题留空自动取正文首句。
- **待办**：按「进行中 / 已完成」分组，勾选完成、左滑删除、一键清除已完成，返回自动保存。
- **倒数日**：卡片显示剩余天数，支持排序、多选批量操作、相册背景与自定义字体，可导出长图分享。
- **组织与检索**：标签（可筛选、重命名、删除）、收藏、置顶；搜索支持正文与标签（`#` 开头只搜标签）；列表可按最近修改 / 创建时间 / 标题排序。
- **隐私笔记**：不出现在列表、搜索与回收站，仅在主页下拉进入；支持数字密码或手机锁屏密码，可重置。
- **分享与导出**：笔记与倒数日可渲染成图片；Markdown / 纯文本导出会带上标签、收藏、置顶与时间；文件统一存到 `Download/mori_notes/`。
- **备份与还原**：整机数据导出为单个 `.mori` 文件，导入时可逐项勾选要还原的内容。
- **外观**：Material Design 3，跟随系统深浅色，支持自定义背景与本地图片。
- **字体库**：导入 / 删除 / 预览（.ttf / .otf / .ttc），支持加密 zip 导入导出。
- **存储与入口**：数据默认存内部存储，可用 SAF 选择任意目录；底部导航「主页 | 待办」，左右滑动与点击双向联动。

## 兼容性说明

- 不支持导入 [TimeTrace（KIPPU_Trace）](https://github.com/KIPPUDESU/KIPPU_Trace) 导出的 `.zip` 文件。
- 本应用导出的文件也不支持导入到 [TimeTrace（KIPPU_Trace）](https://github.com/KIPPUDESU/KIPPU_Trace) 中。

> 倒数日页虽参考了 TimeTrace 的设计与交互，但数据格式与备份包结构相互独立，无法互通。

## 环境

**运行环境**

| 项目 | 值 |
| --- | --- |
| 最低系统 | Android 7.0（API 24） |
| 目标系统 | Android 17（API 37） |
| 支持范围 | Android 7.0 ~ 17 |

**构建环境**

| 项目 | 值 |
| --- | --- |
| compileSdk | 37 |
| minSdk | 24 |
| targetSdk | 37 |
| Java | 17 |
| Gradle | 9.8.2 |
| AGP | 9.5.0 |
| Build Tools | 37.0.0 |

**依赖**：appcompat 1.7.0 / recyclerview 1.3.2 / material 1.12.0 / viewpager2 1.1.0-beta02 / fragment 1.5.4 / zip4j 2.11.5

**构建命令**

```bash
gradle assembleRelease
```

输出：`app/build/outputs/apk/release/app-release.apk`

见 [CHANGELOG.md](CHANGELOG.md)。

## 鸣谢
- 倒数日页最初的设计与交互参考 [TimeTrace（KIPPU_Trace）](https://github.com/KIPPUDESU/KIPPU_Trace)，作者切符；依据 MIT License 使用，许可全文见 [LICENSE-KIPPU_Trace](LICENSE-KIPPU_Trace)。

## 许可

本项目基于 [MIT License](LICENSE) 开源，Copyright © 2026 Gardenia-Gentlemen。

倒数日页参考的 TimeTrace 部分另依其 MIT License 使用，许可全文见 [LICENSE-KIPPU_Trace](LICENSE-KIPPU_Trace)。

## 说明

不发布到应用商店，可以在 Issues 提交新增功能或者bug，不建议在2.0版本前下载。

每个版本目录包含：`*.apk`（该版正式包）与 `src/`（该版源码）

详见各目录 `SOURCE.md`。

## Releases

每个版本都在 [Releases](https://github.com/Gardenia-Gentlemen/Mori-Notes/releases) 有对应标签，附件：成品包 `mori_notes-vX.apk`、源代码 `mori_notes-vX-src.zip`，说明为该版本更新日志。

# Mori Notes

一个AI生成的 Android 笔记应用。纯 Java + Material Design 3，无第三方服务，不联网，数据完全在本地。

## 功能
- **普通笔记**：Markdown 编辑 + 实时预览，插图，导出 TXT / PNG；标题留空自动取正文首句。
- **思维笔记**：大纲与脑图视图一键切换，脑图支持缩放、拖拽、居中，可导出 PNG。
- **待办**：按「进行中 / 已完成」分组，勾选即完成，左滑删除，一键清除已完成，返回自动保存。
- **倒数日**：卡片列表显示剩余天数，点击进全屏详情；支持排序、多选批量操作、相册背景、自定义字体，可导出长图分享。
- **字体库**：导入 / 删除 / 预览字体（.ttf / .otf / .ttc），支持 zip 导入导出（可带密码），按字体本身渲染名称。
- **回收站**：删除的笔记保留 10 天，可恢复或彻底删除。
- **分享与导出**：笔记与倒数日可渲染成图片分享；普通笔记可分享 Markdown 或纯文本。所有导出统一存到 `Download/mori_notes/`。
- **隐私笔记**：设为隐私后不出现在列表、搜索与回收站中，仅在主页下拉手势进入；支持数字密码或锁屏密码验证，可重置；隐私笔记列表按置顶优先排序，并新增「只看收藏」切换。
- **备份与还原**：整机数据导出为单个 `.mori` 文件，导入时可逐项勾选要还原的内容。
- **存储位置**：内部存储，或用 SAF 选择任意目录（配置存于 `notebook.json`）。
- **背景与外观**：多种背景样式，可选用本地图片，全局跟随系统深浅色（MD3）。
- **页面与入口**：底部导航「主页 | 待办」，左右滑动与点击双向联动，设置页独立入口。
- **搜索支持标签**：搜索框输入标签名或带 `#` 前缀可直接命中打了该标签的笔记；`#` 前缀时只匹配标签，不扫正文。
- **导出/分享保留元数据**：导出与分享 Markdown 时在文首写入 frontmatter（tags / starred / pinned / created / updated）；纯文本导出与分享在文末追加 `标签：#a #b · 已收藏 · 已置顶` 一行。
- **标签管理**：标签筛选弹窗新增「管理标签…」，可查看每个标签的笔记数、重命名、删除；重命名影响同步刷新筛选条件。
- **草稿随笔记清理**：删除、批量删除、清空回收站、彻底删除、过期清理以及启动时加载，都会同步清掉对应笔记的编辑器草稿，不再残留孤儿草稿。
- **小组件提示状态**：小组件条目现在会显示 ★（收藏）、#首个标签 和 ↑（置顶）标记。
- **排序说明补充**：列表排序弹窗加了一句说明——置顶笔记始终排在最前，组内按置顶时间倒序，与 1.11 文案一致。
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

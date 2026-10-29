（# Mori Notes

一个AI生成的 Android 笔记应用。纯 Java + Material Design 3，无第三方服务，不联网，数据完全在本地。

## 功能

- **普通笔记**：Markdown 编辑 + 实时预览，插图，导出 TXT / PNG；标题留空自动取正文首句。
- **思维笔记**：大纲与脑图视图一键切换，脑图支持缩放、拖拽、居中，可导出 PNG。
- **待办**：按「进行中 / 已完成」分组，勾选即完成，左滑删除，一键清除已完成，返回自动保存。
- **倒数日**：卡片列表显示剩余天数，点击进全屏详情；支持排序、多选批量操作、相册背景、自定义字体，可导出长图分享。
- **字体库**：导入 / 删除 / 预览字体（.ttf / .otf / .ttc），支持 zip 导入导出（可带密码），按字体本身渲染名称。
- **回收站**：删除的笔记保留 10 天，可恢复或彻底删除。
- **分享与导出**：笔记与倒数日可渲染成图片分享；普通笔记可分享 Markdown 或纯文本。所有导出统一存到 `Download/mori_notes/`。
- **隐私笔记**：设为隐私后不出现在列表、搜索与回收站中，仅在主页下拉手势进入；支持数字密码或锁屏密码验证，可重置。
- **备份与还原**：整机数据导出为单个 `.mori` 文件，导入时可逐项勾选要还原的内容。
- **存储位置**：内部存储，或用 SAF 选择任意目录（配置存于 `notebook.json`）。
- **背景与外观**：多种背景样式，可选用本地图片，全局跟随系统深浅色（MD3）。
- **页面与入口**：底部导航「主页 | 待办」，左右滑动与点击双向联动，设置页独立入口。

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
- 倒数日页后续的新功能非原作者开发，，出现bug请勿在原作者提交Issues

## 说明

不发布到应用商店，不提供 Issues 支持，不建议在2.0版本前下载。

每个版本目录包含：`*.apk`（该版正式包）与 `src/`（该版源码）

详见各目录 `SOURCE.md`。

## Releases

每个版本都在 [Releases](https://github.com/Gardenia-Gentlemen/notebook/releases) 有对应标签，附件：成品包 `mori_notes-vX.apk`、源代码 `mori_notes-vX-src.zip`，说明为该版本更新日志。

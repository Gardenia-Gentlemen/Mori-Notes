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

## 构建

```bash
# local.properties 里指定 SDK 路径
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 签名信息（可选，不放签名也能构建出未签名包）
cat > keystore.properties <<EOF
storeFile=/path/to/your.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
EOF

gradle assembleRelease
```

`keystore.properties`、`local.properties`、`*.jks` 都已在 `.gitignore` 中排除，不会进仓库。

> 如果在 arm64 / aarch64 环境下构建，AAPT2 需要本地替换：在 `~/.gradle/gradle.properties` 里加一行
> `android.aapt2FromMavenOverride=/path/to/android-sdk/build-tools/34.0.0/aapt2`。此项不进仓库。

## 目录

```
app/src/main/java/com/mori/notebook/
├── MainActivity.java        笔记列表、宫格/列表切换、长按菜单（重命名 / 分享图片 / 删除）
├── NoteEditActivity.java    文档笔记编辑、预览、插图、导出
├── MindActivity.java        思维笔记：大纲 + 脑图
├── MindMapView.java         脑图自绘视图（缩放 / 平移 / 绘到 Bitmap）
├── TrashActivity.java       回收站
├── ShareUtils.java          渲染为图片并分享
├── NoteStore.java           笔记存取、回收站、10 天过期清理
└── ...
```


## 安装包

每个版本一个独立文件夹，放在 `releases/` 下：

```
releases/
├── v1.0/        历史版本
├── v1.1/
├── v1.2/
├── v1.2.1/ ~ v1.2.5/
└── v1.3/        当前版本
```

当前版本为 `releases/v1.3/笔记本-Notebook-v1.3.apk`（versionCode 10），签名 CN=Mori_Akira，可直接覆盖安装。

## 版本

见 [CHANGELOG.md](CHANGELOG.md)。

## 说明

个人使用，不发布到应用商店，不提供 Issues 支持。

每个版本目录包含：`*.apk`（该版正式包）与 `src/`（该版源码）。v1.3 的 `src/` 是完整可编译工程；历史版本的原始工程已被覆盖，其 `src/` 为 APK 反编译代码，仅供查看，详见各目录 `SOURCE.md`。

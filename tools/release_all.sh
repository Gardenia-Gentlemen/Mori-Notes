#!/usr/bin/env bash
# 一条龙发版：校验 -> 构建 -> 归档源码 -> 提交推送 tag -> 建 Release -> 本地归档
# 用法:
#   tools/release_all.sh 1.12            # 用已有构建产物
#   tools/release_all.sh 1.12 --build    # 先 gradle assembleRelease
#   tools/release_all.sh 1.12 --dry-run  # 只校验与打印计划

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 1

VER=""; BUILD=0; DRY=0
for a in "$@"; do
  case "$a" in
    --build) BUILD=1 ;;
    --dry-run) DRY=1 ;;
    *) VER="$a" ;;
  esac
done

if [ -z "$VER" ]; then
  echo "用法: tools/release_all.sh <版本号> [--build] [--dry-run]"
  exit 1
fi
if ! echo "$VER" | grep -Eq '^[0-9]+\.[0-9]+(\.[0-9]+)?$'; then
  echo "版本号格式错误: $VER"
  exit 1
fi

APK="app/build/outputs/apk/release/app-release.apk"
ARCHIVE_DIR="/sdcard/Document-Mr.Song/Operit/笔记本-Notebook/v$VER"

step(){ echo; echo "=== $* ==="; }

step "1/6 预检"
if ! grep -q "^## $VER (" CHANGELOG.md; then
  echo "CHANGELOG.md 缺少 '## $VER (code)' 章节，先补再发"
  exit 1
fi
if [ -d "releases/v$VER" ]; then
  echo "releases/v$VER 已存在，重发请先删除该目录"
  exit 1
fi
if [ ! -s "$HOME/.gh_token" ] && [ -z "$GH_TOKEN" ]; then
  echo "缺少 token：~/.gh_token 或 GH_TOKEN"
  exit 1
fi
echo "版本 v$VER OK"

step "2/6 构建"
if [ "$BUILD" = "1" ]; then
  if [ -x ./gradlew ]; then ./gradlew assembleRelease -q; else gradle assembleRelease -q; fi
  if [ $? -ne 0 ]; then echo "构建失败"; exit 1; fi
else
  echo "跳过构建（未传 --build）"
fi
if [ ! -s "$APK" ]; then
  echo "APK 不存在: $APK（先构建或去掉 --build 检查）"
  exit 1
fi
ls -l "$APK"

if [ "$DRY" = "1" ]; then
  step "dry-run 结束"
  echo "将快照 releases/v$VER/src -> python3 tools/pack_src.py 打包 -> commit/push/tag -> 建 Release -> 归档到 $ARCHIVE_DIR"
  exit 0
fi

step "3/6 快照 + 打包 + 推送 + Release（tools/release.py）"
python3 tools/release.py "$VER"
if [ $? -ne 0 ]; then echo "release.py 失败，停止"; exit 1; fi

step "4/6 本地归档"
mkdir -p "$ARCHIVE_DIR"
cp -f "$APK" "$ARCHIVE_DIR/Mori-Notes-v$VER.apk"
FAST_ZIP=$(ls -1t /tmp/mori_notes-v*-src.zip 2>/dev/null | head -1)
if [ -n "$FAST_ZIP" ]; then cp -f "$FAST_ZIP" "$ARCHIVE_DIR/mori_notes-v$VER-src.zip"; fi
rm -rf "$ARCHIVE_DIR/src"
cp -a "releases/v$VER/src" "$ARCHIVE_DIR/src"

step "5/6 校验归档"
ls -l "$ARCHIVE_DIR"
find "$ARCHIVE_DIR/src" -type f | wc -l

step "6/6 完成"
echo "https://github.com/Gardenia-Gentlemen/Mori-Notes/releases/tag/v$VER"
echo "本地: $ARCHIVE_DIR"

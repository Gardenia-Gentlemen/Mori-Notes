# releases 目录规则

每个版本一个子目录，命名固定为 `v<版本号>`（如 `v1.11`），内放三件套：

| 文件/目录 | 命名 | 说明 |
|---|---|---|
| APK | 子目录内 `mori_notes-v<ver>.apk` | release 构建产物，来源 `app/build/outputs/apk/release/app-release.apk` |
| 源码快照包 | 子目录内 `mori_notes-v<ver>-src.zip` | 由 `tools/pack_src.py` 生成，zip 内路径统一 `src/<相对路径>` |
| 源码目录 | 子目录内 `src/` | 该版本源码解压副本 |
| 源码说明 | 子目录内 `src/SOURCE.md` | 由 `tools/release.py` 自动生成，记录版本/提交/时间 |

## 生成与发布

一条龙：

```bash
tools/release_all.sh 1.12 --build   # 构建 + 快照 + 打包推送 tag + 建 Release + 本地归档
tools/release_all.sh 1.12           # 用已有构建产物直接走流程
tools/release_all.sh 1.12 --dry-run # 只构建并打印计划，不提交不发布
```

底层拆分：

- `tools/pack_src.py <ver>` → `/tmp/mori_notes-v<ver>-src.zip`（Ubuntu 侧无 `zip` 命令，故用 Python zipfile 固化）
- `tools/release.py <ver>` → 快照 `releases/v<ver>/`、commit/push、打 tag、上传 APK 与 src.zip 到 GitHub Release，并把更新日志落盘 `/tmp/mori_notes-v<ver>-release.md`
- `tools/gh_release.py <ver>` → 单独建/更新 Release，正文四级回退：`--note` 文件 → `/tmp` 落盘正文 → CHANGELOG 章节 → `git tag -l -n99`

## 约定

- 版本号必须已存在于 `CHANGELOG.md`（`## <ver> (<序号>)` 章节），否则预检失败。
- 同版本号目录已存在时预检失败，避免覆盖历史快照。
- 本地归档路径：`/sdcard/Document-Mr.Song/Operit/笔记本-Notebook/v<ver>/`。
- 远端仓库：`Gardenia-Gentlemen/Mori-Notes`。

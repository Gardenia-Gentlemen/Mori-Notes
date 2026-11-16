# tools 环境与坑位备忘

## aapt2

本机 aapt2 位于各 build-tools 下，实测可执行：

- `/root/android-sdk/build-tools/34.0.0/aapt2`
- `/root/android-sdk/build-tools/35.0.2/aapt2`
- `/root/android-sdk/build-tools/36.0.0/aapt2`
- `/root/android-sdk/build-tools/37.0.0/aapt2`

若构建报 `aapt2` 缺失或不可执行，优先检查：

1. `local.properties` 的 `sdk.dir` 是否指向 `/root/android-sdk`。
2. 上述目录下 `aapt2` 是否具备可执行权限（缺权限时 `chmod +x`）。
3. 不要在 `gradle.properties` 里写任何 `android.aapt2FromMavenOverride` 覆盖，走本地 build-tools 即可。

## gradle.properties

保持只含这四行基础项，不往里加任何本地环境变量：

```
org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
android.enableJetifier=false
```

原因：该文件会随仓库提交，写入 `sdk.dir`、keystore 路径、机器相关 JVM 参数会污染他人环境；机器相关配置放 `local.properties` / `keystore.properties`（均不入库）。

## 打包发布链

无 `zip` 命令，统一走 Python：

- `pack_src.py`：源码快照打包
- `release.py`：快照 + 提交推送 + tag + Release + 日志落盘
- `gh_release.py`：单独建/更新 Release
- `release_all.sh`：一条龙

详见 `releases/README.md`。

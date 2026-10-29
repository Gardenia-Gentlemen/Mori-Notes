#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""一键发版：构建 -> 归档源码 -> 提交推送 -> 建 Release(成品包 + 本版本源码)。

用法:
    export GH_TOKEN=ghp_xxx
    python3 tools/release.py 1.4                # 用已有构建产物
    python3 tools/release.py 1.4 --build        # 先 gradle assembleRelease
    python3 tools/release.py 1.4 --dry-run      # 只打印计划，不动远端

规则:
    * 新版本源码快照进 releases/v<new>/src
    * Release 附件 = mori_notes-v<new>.apk（成品包） + mori_notes-v<new>-src.zip（本版本源码）
    * Release 正文 = CHANGELOG.md 中 ## <new> (code) 章节
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.parse
import urllib.request
import urllib.error
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OWNER_REPO = "Gardenia-Gentlemen/notebook"
API = "https://api.github.com"
EXCLUDE_DIRS = {"build", ".git", ".gradle", ".idea"}
EXCLUDE_FILES = {"local.properties", "keystore.properties", ".DS_Store"}


def sh(cmd, check=True, cwd=ROOT):
    p = subprocess.run(cmd, shell=True, cwd=cwd, text=True,
                       stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if check and p.returncode != 0:
        print(p.stdout)
        sys.exit("命令失败: %s" % cmd)
    return p.stdout


def token():
    t = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not t:
        sys.exit("缺少 GH_TOKEN 环境变量（classic PAT，需 repo 权限）")
    return t


def api(method, path, tok, data=None, raw=None, ctype=None):
    url = API + path
    body = None
    if data is not None:
        body = json.dumps(data).encode()
        ctype = "application/json"
    elif raw is not None:
        body = raw
        ctype = ctype or "application/octet-stream"
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Authorization", "Bearer " + tok)
    req.add_header("Accept", "application/vnd.github+json")
    if ctype:
        req.add_header("Content-Type", ctype)
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        txt = e.read().decode(errors="replace")[:400]
        return e.code, {"message": txt}


def vkey(v):
    return tuple(int(x) for x in v.split("."))


def changelog_body(version):
    path = os.path.join(ROOT, "CHANGELOG.md")
    s = open(path, encoding="utf-8").read()
    m = re.search(r"^## %s \(\d+\)\s*$(.*?)(?=^## |\Z)" % re.escape(version),
                  s, re.S | re.M)
    if not m:
        sys.exit("CHANGELOG.md 里找不到 %s 的章节（格式须为 '## %s (版本号)'）"
                 % (version, version))
    return m.group(1).strip()


def snapshot_src(version):
    dst = os.path.join(ROOT, "releases", "v" + version, "src")
    if os.path.isdir(dst):
        shutil.rmtree(dst)
    os.makedirs(dst)
    for item in ("app", "build.gradle", "settings.gradle", "gradle.properties"):
        src = os.path.join(ROOT, item)
        if not os.path.exists(src):
            continue
        if os.path.isdir(src):
            shutil.copytree(src, os.path.join(dst, item),
                            ignore=shutil.ignore_patterns(*EXCLUDE_DIRS))
        else:
            shutil.copy2(src, os.path.join(dst, item))
    for dirpath, dirnames, filenames in os.walk(dst):
        for f in list(filenames):
            if f in EXCLUDE_FILES:
                os.remove(os.path.join(dirpath, f))
    n = sum(len(f) for _, _, f in os.walk(dst))
    size = sum(os.path.getsize(os.path.join(dp, f))
               for dp, _, fs in os.walk(dst) for f in fs)
    print("源码快照: releases/v%s/src  %d 文件 %.0fK" % (version, n, size / 1024.0))
    return dst


def zip_src(version):
    src = os.path.join(ROOT, "releases", "v" + version, "src")
    if not os.path.isdir(src):
        sys.exit("缺少 releases/v%s/src，无法打包" % version)
    out = "/tmp/mori_notes-v%s-src.zip" % version
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for dp, _, fs in os.walk(src):
            for f in fs:
                p = os.path.join(dp, f)
                z.write(p, os.path.join("src", os.path.relpath(p, src)))
    print("源码包: %s  %d 字节" % (out, os.path.getsize(out)))
    return out


def find_apk(version):
    for pat in ("app/build/outputs/apk/release/app-release.apk",
                "app/build/outputs/apk/release/*.apk"):
        out = sh("ls %s 2>/dev/null" % pat, check=False).strip().splitlines()
        out = [o for o in out if o.endswith(".apk")]
        if out:
            return os.path.join(ROOT, out[0])
    sys.exit("找不到构建产物，先跑一次 gradle assembleRelease 或传 --apk")


def update_readme_features(version, write=True):
    """把该版 CHANGELOG 里 `- **标题**：说明` 条目并入 README 的「## 功能」列表。
    标题同名（或互为子串）则追加说明，新标题直接加到列表末尾。返回 (added, merged, bullets)。"""
    body = changelog_body(version)
    items = []
    for line in body.split("\n"):
        line = line.strip()
        if not line.startswith("- "):
            continue
        m = re.match(r"\*\*(.+?)\*\*\s*[：:]\s*(.+)", line[2:].strip())
        if m:
            items.append((m.group(1).strip(), m.group(2).strip()))
    if not items:
        print("README 功能：该版本 CHANGELOG 无 **标题**：说明 条目，跳过")
        return 0, 0, []

    path = os.path.join(ROOT, "README.md")
    s = open(path, encoding="utf-8").read()
    lines = s.split("\n")
    try:
        i = lines.index("## 功能")
    except ValueError:
        sys.exit("README.md 缺少 '## 功能' 章节")
    j = next((k for k in range(i + 1, len(lines)) if lines[k].startswith("## ")), len(lines))

    entries = []
    for l in lines[i + 1:j]:
        m = re.match(r"- \*\*(.+?)\*\*\s*[：:]\s*(.*)$", l.strip())
        if m:
            entries.append([m.group(1).strip(), m.group(2).strip()])

    added = merged = 0
    for title, text in items:
        hit = None
        for e in entries:
            t = e[0]
            if t == title or t in title or title in t:
                hit = e
                break
        if hit:
            old = hit[1].rstrip("。；; ")
            if text not in old:
                hit[1] = ("%s；%s" % (old, text)) if old else text
                merged += 1
        else:
            entries.append([title, text])
            added += 1

    bullets = ["- **%s**：%s" % (t, x) for t, x in entries]
    new = "\n".join(lines[:i + 1] + bullets + lines[j:])
    if write and new != s:
        open(path, "w", encoding="utf-8").write(new)
    print("README 功能：新增 %d 条、补充 %d 条，共 %d 条" % (added, merged, len(bullets)))
    return added, merged, bullets


def upload_asset(rel, path, name, label, tok):
    """用 curl 上传附件：本机 Python TLS 连 uploads.github.com 会握手失败，
    而 curl 正常，故统一走 curl。"""
    url = rel["upload_url"].split("{")[0] + "?" + urllib.parse.urlencode(
        {"name": name, "label": label})
    ctype = ("application/vnd.android.package-archive"
             if name.endswith(".apk") else "application/zip")
    out = sh("curl -sS -X POST -H 'Authorization: Bearer %s' "
             "-H 'Content-Type: %s' --data-binary @'%s' '%s'"
             % (tok, ctype, path, url), check=False)
    try:
        r = json.loads(out or "{}")
    except Exception:
        return 0, {"message": out[:300]}
    return (200 if r.get("state") == "uploaded" else 0), r


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("version", help="新版本号，例如 1.4 / 1.4.1")
    ap.add_argument("--build", action="store_true", help="先执行 gradle assembleRelease")
    ap.add_argument("--apk", help="指定已构建的 APK 路径")
    ap.add_argument("--readme", action="store_true",
                    help="把本版 CHANGELOG 的 **标题**：说明 条目并入 README「## 功能」"
                         "（默认不动 README，避免旧条目反复堆积）")
    ap.add_argument("--dry-run", action="store_true", help="只打印计划，不提交不推送不建 Release")
    a = ap.parse_args()
    v = a.version

    if not re.fullmatch(r"\d+\.\d+(\.\d+)?", v):
        sys.exit("版本号格式错误: %s" % v)

    rel_root = os.path.join(ROOT, "releases")
    existing = sorted([d[1:] for d in os.listdir(rel_root)
                       if d.startswith("v") and os.path.isdir(os.path.join(rel_root, d))],
                      key=vkey)
    if v in existing:
        sys.exit("releases/v%s 已存在，若是重发请先删除该目录或换版本号" % v)

    prev = existing[-1] if existing else None
    print("新版本 v%s，上一版本 v%s" % (v, prev or "无"))
    body = changelog_body(v)
    print("更新日志: %d 字" % len(body))

    _, _, feat = update_readme_features(v, write=False)
    if a.dry_run and feat:
        print("功能列表预览:")
        for b in feat:
            print("  " + b[:70])

    apk = a.apk or os.path.join(ROOT, "app/build/outputs/apk/release/app-release.apk")
    if a.build:
        print("构建中…")
        gradle = "./gradlew" if os.path.exists(os.path.join(ROOT, "gradlew")) else "gradle"
        sh("%s assembleRelease -q" % gradle, check=True)
        apk = find_apk(v)
    if not os.path.exists(apk):
        sys.exit("APK 不存在: %s" % apk)
    print("成品包: %s  %d 字节" % (apk, os.path.getsize(apk)))

    if a.dry_run:
        print("[dry-run] 将快照 releases/v%s/src、打上一版本源码包、commit、push、建 Release" % v)
        return

    snapshot_src(v)
    if a.readme:
        update_readme_features(v, write=True)
    cur_zip = zip_src(v)

    note = os.path.join(ROOT, "releases", "v" + v, "SOURCE.md")
    with open(note, "w", encoding="utf-8") as f:
        f.write("# v%s 源码\n\n本目录 `src/` 为 v%s 构建时的完整可编译工程"
                "（已剔除 build/、local.properties、keystore.properties）。\n"
                "发版时它被打包成 `mori_notes-v%s-src.zip`，作为本版本"
                " Release 的「源代码」附件。\n" % (v, v, v))

    tok = token()
    sh("git add -A")
    sh("git commit -q -m 'v%s: 归档源码快照' || true" % v)
    # 本机 github.com:443 不通，origin 已配置为 SSH（ssh.github.com:443），走 origin 推送
    print(sh("git push -q origin HEAD:master 2>&1", check=False).strip() or "push OK")
    sh("git tag -a v%s -m 'v%s' 2>/dev/null || true" % (v, v), check=False)
    print(sh("git push -q origin v%s 2>&1" % v, check=False).strip() or "tag push OK")

    st, rel = api("POST", "/repos/%s/releases" % OWNER_REPO, tok, {
        "tag_name": "v" + v,
        "target_commitish": "master",
        "name": "v" + v,
        "body": body,
        "draft": False,
        "prerelease": False,
    })
    if st not in (200, 201):
        sys.exit("建 Release 失败 %s: %s" % (st, rel.get("message")))
    print("release v%s OK" % v)

    assets = [(apk, "mori_notes-v%s.apk" % v, "")]
    if cur_zip:
        assets.append((cur_zip, "mori_notes-v%s-src.zip" % v, "源代码 (v%s)" % v))
    for path, name, label in assets:
        st, r2 = upload_asset(rel, path, name, label, tok)
        print(" asset %s -> %s %s" % (name, st, "" if st in (200, 201) else r2.get("message")))

    print("完成: https://github.com/%s/releases/tag/v%s" % (OWNER_REPO, v))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""源码快照打包器（本机 Ubuntu 侧没有 zip 命令，统一用 python zipfile）。

用法:
    python3 tools/pack_src.py 1.11                 # 打包 releases/v1.11/src
    python3 tools/pack_src.py 1.11 --out /tmp/a.zip
    python3 tools/pack_src.py --dir releases/v1.11/src --out /tmp/a.zip

输出 zip 内路径统一为 src/<相对路径>。
"""
import argparse
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def pack(src_dir, out):
    if not os.path.isdir(src_dir):
        sys.exit("缺少目录: %s" % src_dir)
    n = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for dp, _, fs in os.walk(src_dir):
            for f in fs:
                p = os.path.join(dp, f)
                z.write(p, os.path.join("src", os.path.relpath(p, src_dir)))
                n += 1
    size = os.path.getsize(out)
    print("源码包: %s  %d 文件  %d 字节" % (out, n, size))
    return out, n, size


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("version", nargs="?", help="版本号，例如 1.11")
    ap.add_argument("--dir", help="直接指定源码目录（默认 releases/v<version>/src）")
    ap.add_argument("--out", help="输出 zip 路径（默认 /tmp/mori_notes-v<version>-src.zip）")
    a = ap.parse_args()

    if not a.dir:
        if not a.version:
            sys.exit("需要版本号或 --dir")
        a.dir = os.path.join(ROOT, "releases", "v" + a.version, "src")
    if not a.out:
        tag = a.version or os.path.basename(os.path.dirname(a.dir)) or "src"
        a.out = "/tmp/mori_notes-v%s-src.zip" % tag.lstrip("v")
    pack(a.dir, a.out)


if __name__ == "__main__":
    main()

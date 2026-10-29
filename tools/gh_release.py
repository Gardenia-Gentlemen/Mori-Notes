#!/usr/bin/env python3
# 每次发版自动创建 GitHub Release 页面并上传 APK + 源码包
# 用法: python3 tools/gh_release.py [--version vX.Y.Z] [--apk path] [--note file] [--draft]
import io, json, os, re, subprocess, sys, urllib.parse, urllib.request, urllib.error

REPO = 'Gardenia-Gentlemen/notebook'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def token():
    t = os.environ.get('GITHUB_TOKEN') or os.environ.get('GH_TOKEN')
    if t:
        return t.strip()
    p = os.path.expanduser('~/.gh_token')
    if os.path.exists(p):
        return open(p).read().strip()
    sys.exit('NO_TOKEN')

TOKEN = token()

def api(url, data=None, method=None, raw=None, ctype='application/json'):
    if raw is not None:
        req = urllib.request.Request(url, data=raw, method=method or 'POST')
        req.add_header('Content-Type', ctype)
    elif data is not None:
        req = urllib.request.Request(url, data=json.dumps(data).encode(), method=method or 'POST')
        req.add_header('Content-Type', 'application/json')
    else:
        req = urllib.request.Request(url, method=method or 'GET')
    req.add_header('Authorization', 'token ' + TOKEN)
    req.add_header('Accept', 'application/vnd.github+json')
    req.add_header('User-Agent', 'mori-release')
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

def arg(name, default=None):
    if name in sys.argv:
        return sys.argv[sys.argv.index(name) + 1]
    return default

def gradle_version():
    txt = open(os.path.join(ROOT, 'app/build.gradle'), encoding='utf-8').read()
    m = re.search(r'versionName\s+"([^"]+)"', txt)
    return m.group(1)

ver = arg('--version')
tag = ver if ver else 'v' + gradle_version()
if not tag.startswith('v'):
    tag = 'v' + tag
ver = tag[1:]

apk = arg('--apk', os.path.join(ROOT, 'app/build/outputs/apk/release/app-release.apk'))
if not os.path.exists(apk):
    sys.exit('APK_NOT_FOUND ' + apk)

# 源码包: 直接从 tag 导出
src = '/tmp/notebook-%s-src.zip' % tag
if not os.path.exists(src):
    r = subprocess.run(['git', 'archive', '--format=zip', '-o', src, tag], cwd=ROOT,
                       capture_output=True, text=True)
    if r.returncode != 0:
        sys.exit('SRC_ZIP_FAIL ' + r.stderr[:300])

note_file = arg('--note')
if note_file and os.path.exists(note_file):
    body = open(note_file, encoding='utf-8').read()
else:
    r = subprocess.run(['git', 'tag', '-l', '-n99', tag], cwd=ROOT, capture_output=True, text=True)
    body = r.stdout.strip() or tag

st, txt = api('https://api.github.com/repos/%s/releases/tags/%s' % (REPO, tag))
if st == 200:
    rel = json.loads(txt)
    print('RELEASE_EXISTS', rel['html_url'])
else:
    payload = {'tag_name': tag, 'name': tag, 'body': body,
               'draft': '--draft' in sys.argv, 'prerelease': False}
    st, txt = api('https://api.github.com/repos/%s/releases' % REPO, payload)
    print('CREATE', st)
    if st not in (200, 201):
        print(txt[:800]); sys.exit(1)
    rel = json.loads(txt)
    print('RELEASE_URL', rel['html_url'])

upload = rel['upload_url'].split('{')[0]
existing = {a['name'] for a in rel.get('assets', [])}
for path, name, ctype in [(apk, 'mori_notes-%s.apk' % tag, 'application/vnd.android.package-archive'),
                          (src, 'mori_notes-%s-src.zip' % tag, 'application/zip')]:
    if name in existing:
        print('SKIP_EXISTS', name); continue
    data = open(path, 'rb').read()
    url = upload + '?name=' + urllib.parse.quote(name)
    s2, t2 = api(url, raw=data, method='POST', ctype=ctype)
    print('UPLOAD', name, s2, len(data))
    if s2 not in (200, 201):
        print(t2[:400])

st, txt = api('https://api.github.com/repos/%s/releases/tags/%s' % (REPO, tag))
if st == 200:
    d = json.loads(txt)
    print('FINAL_URL', d['html_url'])
    for a in d['assets']:
        print('ASSET', a['name'], a['size'])

#!/bin/bash

# check_deployment.sh - 通过 Central Portal Publisher API 查询发布状态
# 用法:
#   ./check_deployment.sh <deploymentId>      # 直接查询指定 deployment
#   ./check_deployment.sh <mvn-deploy日志文件> # 从日志中自动提取 deploymentId 后查询
# 退出码: 0=PUBLISHED, 1=其他状态或查询失败

set -e

arg=$1
if [ -z "$arg" ]; then
    echo "用法: $0 <deploymentId 或 mvn deploy 日志文件路径>"
    exit 1
fi

# 参数既可能是 deploymentId，也可能是包含 deploymentId 的日志文件
deployment_id="$arg"
if [ -f "$arg" ]; then
    deployment_id=$(grep -o 'deploymentId: [a-f0-9-]*' "$arg" | tail -1 | cut -d' ' -f2)
    if [ -z "$deployment_id" ]; then
        echo "错误：日志文件中未找到 deploymentId（期望格式 'deploymentId: xxxxxxxx-...'）"
        exit 1
    fi
fi

echo "查询 deployment: ${deployment_id}"

# 用 Python 严格解析 settings.xml（带命名空间，sed 提取易误伤），
# 按官方规范以 base64(username:password) 作为 Bearer token 调用 status 接口
# 注：仅解析本机可信的 ~/.m2/settings.xml，ElementTree 默认不解析外部实体
python3 - "$deployment_id" <<'PYEOF'
import sys, base64, json, urllib.request
import xml.etree.ElementTree as ET
import os

deployment_id = sys.argv[1]
settings = os.path.expanduser('~/.m2/settings.xml')

root = ET.parse(settings).getroot()
user = pwd = None
for s in root.iter():
    if not s.tag.endswith('server'):
        continue
    sid = next((c.text for c in s if c.tag.endswith('id')), None)
    if sid == 'central':
        user = next((c.text for c in s if c.tag.endswith('username')), None)
        pwd = next((c.text for c in s if c.tag.endswith('password')), None)
        break

if not user or not pwd:
    print('错误：settings.xml 中未找到 id=central 的 server 凭据')
    sys.exit(1)

token = base64.b64encode(f'{user}:{pwd}'.encode()).decode()
url = f'https://central.sonatype.com/api/v1/publisher/status?id={deployment_id}'
req = urllib.request.Request(url, method='POST', headers={'Authorization': f'Bearer {token}'})

try:
    with urllib.request.urlopen(req, timeout=30) as r:
        data = json.loads(r.read().decode())
except urllib.error.HTTPError as e:
    print(f'API 请求失败: HTTP {e.code} {e.read()[:200].decode(errors="ignore")}')
    sys.exit(1)

state = data.get('deploymentState', 'UNKNOWN')
purls = data.get('purls') or []
print(f"deploymentState: {state}")
print(f"组件数量: {len(purls)}")
for p in sorted(purls):
    print(f"  - {p.replace('pkg:maven/', '')}")
if data.get('errors'):
    print("errors:", json.dumps(data['errors'], ensure_ascii=False, indent=2))

sys.exit(0 if state == 'PUBLISHED' else 1)
PYEOF

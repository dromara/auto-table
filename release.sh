#!/bin/bash

# release.sh - 自动化发布脚本
# 用法: ./release.sh <version>
# 示例: ./release.sh 2.5.16

set -e

# 参数校验
if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 2.5.16"
    exit 1
fi

version=$1
changelog_file="auto-table-doc/docs/更新日志.md"

echo "================================"
echo "开始发布版本：${version}"
echo "================================"

# 检查变更日志中是否已存在该版本
if grep -q "^## ${version}$" "${changelog_file}"; then
    echo "错误：变更日志中已存在版本 ${version} 的记录"
    exit 1
fi

# 获取上一个 tag
last_tag=$(git describe --tags --abbrev=0 2>/dev/null)
if [ -z "$last_tag" ]; then
    echo "错误：找不到上一个版本的 tag"
    exit 1
fi

echo "上一个版本 tag：${last_tag}"

# 获取自上一个 tag 以来的提交记录（排除合并和无关提交）
commits=$(git log ${last_tag}..HEAD --oneline --no-merges | \
    grep -v "版本升级" | \
    grep -v "更新文档" | \
    grep -vi "^[a-f0-9]* Merge" | \
    grep -vi "^[a-f0-9]* !" || true)

# 格式化提交记录
if [ -n "$commits" ]; then
    changelog_items=$(echo "$commits" | cut -d' ' -f2- | sed 's/^/* /')
else
    changelog_items="* （无新增提交）"
fi

# 将新条目写入临时文件，避免通过 awk -v 传递多行变量
changelog_temp=$(mktemp)
{
    echo ""
    echo "## ${version}"
    echo "$changelog_items"
} > "$changelog_temp"

# 更新变更日志文件
awk '/^# 变更日志$/{print; while((getline line < "'$changelog_temp'") > 0) print line; next} {print}' "${changelog_file}" > temp_changelog.md

rm -f "$changelog_temp"

mv temp_changelog.md "${changelog_file}"

echo "变更日志已更新：${changelog_file}"

# 提交变更日志
git add "${changelog_file}"
git commit -m "docs(changelog): 更新版本 ${version} 的变更日志"

# 执行版本升级（修改版本号、commit、tag、push、mvn deploy）
echo "================================"
echo "开始执行版本升级..."
./upgrade.sh "${version}"

# 发布文档
echo "================================"
echo "开始发布文档..."
./deploy_doc.sh

echo "================================"
echo "版本 ${version} 发布完成！"
echo "================================"

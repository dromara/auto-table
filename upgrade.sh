#!/bin/bash

# 版本升级

# 参数校验
if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 2.5.16"
    exit 1
fi

version=$1

# 以下不动
template=$(cat << EOF
package org.dromara.autotable.core.constants;

public interface Version {
    String VALUE = "${version}";
}
EOF
)

echo "开始替换Version.java的版本号：${version}"
# 替换 org.dromara.autotable.core.constants.Version 的版本号
echo ${template} > ./auto-table-core/src/main/java/org/dromara/autotable/core/constants/Version.java

echo "开始替换pom.xml的版本号：${version}"
mvn versions:set -DnewVersion=${version}

echo "开始替换文档版本号：${version}"
config_file="./auto-table-doc/docs/.vitepress/config.mts"
if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/const LATEST_VERSION = '[0-9.]*'; \/\/ @auto-table-version/const LATEST_VERSION = '$version'; \/\/ @auto-table-version/" "$config_file"
else
    sed -i "s/const LATEST_VERSION = '[0-9.]*'; \/\/ @auto-table-version/const LATEST_VERSION = '$version'; \/\/ @auto-table-version/" "$config_file"
fi

echo "开始commit到本地仓库：${version}"
# 精确 add：仅版本升级涉及的文件，避免捎带工作区其他改动
# 包括：Version.java、文档站 config.mts、更新日志.md（手写或 release.sh 生成）、所有 pom.xml
git add \
    "auto-table-core/src/main/java/org/dromara/autotable/core/constants/Version.java" \
    "auto-table-doc/docs/.vitepress/config.mts" \
    "auto-table-doc/docs/更新日志.md"
# 所有已跟踪的 pom.xml（mvn versions:set 涉及）
git ls-files -z "*pom.xml" | xargs -0 git add
# 仅当存在 staged 改动时才 commit（容错：版本号已提前手动升级的幂等场景）
if ! git diff --cached --quiet; then
    git commit -m "版本升级：${version}"
else
    echo "版本号已为 ${version}，无需新建版本升级 commit（tag 将打在 HEAD）"
fi

tagName=v${version}
echo "开始打tag：${tagName}"
git rev-parse --verify ${tagName} >/dev/null 2>&1
if [ $? -eq 0 ]; then
    git tag -d ${tagName}
    echo "本地标签${tagName}已删除"
fi
if git ls-remote --tags | grep -q "refs/tags/${tagName}"; then
    git push origin --delete ${tagName}
    echo "远程标签${tagName}已删除"
fi
echo "新建标签：${tagName}"
git tag -a ${tagName} -m "版本号：${version}"

echo "开始提交到远程git仓库：${version}"
git push origin main --tags

echo "开始发布新的版本到maven仓库：${version}"
# 全量发布：central-publishing-maven-plugin 接管 deploy，自动跳过配了 skipPublishing=true 的模块
# （auto-table-test、auto-table-adapter 已在各自 pom 配 skipPublishing，不会被上传到 Maven Central）
mvn clean deploy

# 版本升级
version=2.1.4

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

echo "开始commit到本地仓库：${version}"
git commit -am "版本升级：${version}"

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
mvn clean deploy -pl auto-table-annotation,auto-table-core,auto-table-spring-boot-starter,auto-table-solon-plugin -am

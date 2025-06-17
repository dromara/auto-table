#!/bin/bash

# 构建项目
cd auto-table-doc || exit
npm run docs:build

# 使用rsync上传构建后的目录到服务器，提高传输效率
rsync -avz --progress \
  --exclude='.DS_Store' \
  --exclude='.git' \
  --exclude='.svn' \
  docs/.vitepress/dist/ \
  root@tangzc.com:/website/autotable/

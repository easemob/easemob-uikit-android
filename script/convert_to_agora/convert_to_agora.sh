#!/bin/bash

# 使用说明：
# 当需要转换成海外版本时：sh convert_to_agora.sh

unamestr=`uname`

if [[ "$unamestr" = MINGW* || "$unamestr" = "Linux" ]]; then
    	SED="sed"
elif [ "$unamestr" = "Darwin" ]; then
    	SED="gsed"
else
    	echo "only Linux, MingW or Mac OS can be supported" &
    	exit 1
fi
echo "----------------- Start to convert -----------------"

#是否打包成声网UIkit,默认Agora UIkit
is_package_to_shengwang=false

if [[ $1 = "--shengwang" ]];then
		is_package_to_shengwang=true
fi

echo "----------------- is_package_to_shengwang = $is_package_to_shengwang -----------------"

# enter root dir
cd ../../
echo "当前路径：$(pwd)"
# 清空本地改动,切换到agora分支
git checkout -f ; git clean -fd ; git checkout dev; git branch -D dev-agora; git checkout -b dev-agora

echo "current branch :"
git branch

#1、更改包名及文件目录结构
python3 script/convert_to_agora/change_package_name.py ./
#2、对文件中的引用sdk类名进行国内->海外版本的替换
python3 script/convert_to_agora/rename_file_and_update_content.py ./ --replace-content

#3、一些特殊处理
#处理settings.gradle.kts
$SED -i '/.*maven\.aliyun\.com.*/s/^/\/\// ' settings.gradle.kts
#海外和声网版本暂时还没有匹配新uikit的demo，暂时注释掉
$SED -i '/\#\# Product Experience/d' README.md
$SED -i '/In this project*/d' README.md
$SED -i '/If you want to experience*/d' README.md
$SED -i '/.*demo\.png/d' README.md

$SED -i '/\#\# 产品体验/d' README.zh.md
$SED -i '/在这个项目中*/d' README.zh.md
$SED -i '/如果你想体验*/d' README.zh.md
$SED -i '/.*demo\.png/d' README.zh.md


#处理声网UIkit相关，appid替换appkey等
if [[ $is_package_to_shengwang = "true" ]]; then
  #处理Readme.md
  $SED -i 's/io\.hyphenate\/ease-chat-kit/cn\.shengwang\/chat-uikit/g' README.md
  $SED -i 's/io\.hyphenate\:ease-chat-kit/cn\.shengwang\:chat-uikit/g' README.md
  $SED -i 's/io\.hyphenate\/ease-chat-kit/cn\.shengwang\/chat-uikit/g' README.zh.md
  $SED -i 's/io\.hyphenate\:ease-chat-kit/cn\.shengwang\:chat-uikit/g' README.zh.md
  #替换github源码地址
  $SED -i 's/easemob\/chatuikit\-android/Shengwang\-Community\/ShengwangChat\-UIKit\-android/g' README.zh.md
  $SED -i 's/AgoraIO\-Usecase\/AgoraChat\-UIKit\-android\/tree\/dev\-kotlin/Shengwang\-Community\/ShengwangChat\-UIKit\-android/g' README.md

  $SED -i 's/appkey/appId/g' quickstart/src/main/java/com/easemob/quickstart/MainActivity.kt
  $SED -i 's/app_key/app_id/g' quickstart/src/main/java/com/easemob/quickstart/MainActivity.kt
  $SED -i 's/AppKey/AppId/g' quickstart/src/main/java/com/easemob/quickstart/MainActivity.kt
  $SED -i 's/appKey/appId/g' quickstart/src/main/java/com/easemob/quickstart/MainActivity.kt
  $SED -i 's/app_key/app_id/g' quickstart/src/main/res/values/strings.xml

  $SED -i 's/APPKEY/APPID/g' app/src/main/kotlin/com/hyphenate/easeui/demo/DemoApplication.kt
  $SED -i 's/appkey/appId/g' app/src/main/kotlin/com/hyphenate/easeui/demo/DemoApplication.kt
  $SED -i 's/appKey/appId/g' app/src/main/kotlin/com/hyphenate/easeui/demo/DemoApplication.kt
  $SED -i 's/APPKEY/APPID/g' app/build.gradle.kts

  $SED -i 's/appKey/appId/g' README.md
  $SED -i 's/appkey/appId/g' README.md
  $SED -i 's/appKey/appId/g' README.zh.md
  $SED -i 's/appkey/appId/g' README.zh.md

  $SED -i 's/app_key/app_id/g' quickstart/README.md
  $SED -i 's/appkey/appId/g' quickstart/README.md
  $SED -i 's/appKey/appId/g' quickstart/README.md
  $SED -i 's/AppKey/AppId/g' quickstart/README.md
else
  #处理Readme.md
  $SED -i 's/io\.hyphenate\/ease-chat-kit/io\.agora\.rtc\/chat-uikit/g' README.md
  $SED -i 's/io\.hyphenate\:ease-chat-kit/io\.agora\.rtc\:chat-uikit/g' README.md
  $SED -i 's/io\.hyphenate\/ease-chat-kit/io\.agora\.rtc\/chat-uikit/g' README.zh.md
  $SED -i 's/io\.hyphenate\:ease-chat-kit/io\.agora\.rtc\:chat-uikit/g' README.zh.md
fi


git add .
if [[ $is_package_to_shengwang = "true" ]]; then
   git commit -m "convert to shengwang"
   echo "----------------- Finish convert to shengwang -----------------"
else
  git commit -m "convert to agora"
  echo "----------------- Finish convert to agora -----------------"
fi







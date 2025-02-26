#!/bin/bash

# 使用说明：
# 当需要转换成海外版本时：sh convert_to_agora.sh 1.3.2 --shengwang
# 1.3.2:UIkit依赖的sdk版本号
# --shengwang:是否转换成声网UIkit，默认转换成Agora UIkit

unamestr=`uname`

if [[ "$unamestr" = MINGW* || "$unamestr" = "Linux" ]]; then
    	SED="sed"
elif [ "$unamestr" = "Darwin" ]; then
    	SED="gsed"
else
    	echo "only Linux, MingW or Mac OS can be supported" &
    	exit 1
fi

# 检查参数
function check_params() {
  for param in "$@"
  do
    if [[ "$@" =~ "--shengwang" ]]; then
      is_package_to_shengwang=true
    fi
  done
}

echo "----------------- Start to convert -----------------"

#是否打包成声网UIkit,默认Agora UIkit
is_package_to_shengwang=false
check_params $@

SDK_VERSION=$1
# 检查变量 version 是否为空
if [ -z "$SDK_VERSION" ]; then
  SDK_VERSION='latest.release'
fi

echo "----------------- is_package_to_shengwang = $is_package_to_shengwang -----------------"

# enter root dir
cd ../../
echo "当前路径：$(pwd)"
# 清空本地改动,切换到agora分支
git checkout -f ; git clean -fd ; git checkout dev; git branch -D dev-convert; git checkout -b dev-convert

echo "current branch :"
git branch

# 1、替换readme文件内容
if [[ $is_package_to_shengwang = "true" ]]; then
  cp ../emclient-android/resfile/uikit/shengwang/README.md README.md
  cp ../emclient-android/resfile/uikit/shengwang/README.zh.md README.zh.md
  cp ../emclient-android/resfile/uikit/shengwang/quickstart/activity_main.xml quickstart/src/main/res/layout/activity_main.xml
  cp ../emclient-android/resfile/uikit/shengwang/quickstart/MainActivity.kt quickstart/src/main/java/com/easemob/quickstart/MainActivity.kt
  cp ../emclient-android/resfile/uikit/shengwang/quickstart/README.md quickstart/README.md
else
  cp ../emclient-android/resfile/uikit/agora/README.md README.md
  cp ../emclient-android/resfile/uikit/agora/README.zh.md README.zh.md
fi

#2、更改包名及文件目录结构
python3 script/convert_to_agora/change_package_name.py ./ "com.hyphenate.easeui" "io.agora.chat.uikit"
python3 script/convert_to_agora/change_package_name.py ./ "com.easemob.quickstart" "io.agora.quickstart"
#3、对文件中的引用sdk类名进行国内->海外版本的替换
python3 script/convert_to_agora/rename_file_and_update_content.py ./ --replace-content

#4、一些特殊处理
#处理settings.gradle.kts
$SED -i '/.*maven\.aliyun\.com.*/s/^/\/\// ' settings.gradle.kts


#处理声网UIkit相关，appid替换appkey等
if [[ $is_package_to_shengwang = "true" ]]; then

  $SED -i 's/appkey/appId/g' quickstart/src/main/java/io/agora/quickstart/MainActivity.kt
  $SED -i 's/app_key/app_id/g' quickstart/src/main/java/io/agora/quickstart/MainActivity.kt
  $SED -i 's/AppKey/AppId/g' quickstart/src/main/java/io/agora/quickstart/MainActivity.kt
  $SED -i 's/appKey/appId/g' quickstart/src/main/java/io/agora/quickstart/MainActivity.kt
  $SED -i 's/app_key/app_id/g' quickstart/src/main/res/values/strings.xml

  $SED -i 's/APPKEY/APPID/g' app/src/main/kotlin/io/agora/chat/uikit/demo/DemoApplication.kt
  $SED -i 's/appkey/appId/g' app/src/main/kotlin/io/agora/chat/uikit/demo/DemoApplication.kt
  $SED -i 's/appKey/appId/g' app/src/main/kotlin/io/agora/chat/uikit/demo/DemoApplication.kt
  $SED -i 's/APPKEY/APPID/g' app/build.gradle.kts


  $SED -i 's/app_key/app_id/g' quickstart/README.md
  $SED -i 's/appkey/appId/g' quickstart/README.md
  $SED -i 's/appKey/appId/g' quickstart/README.md
  $SED -i 's/AppKey/AppId/g' quickstart/README.md

  # 更改依赖
  $SED -i 's/.*io.hyphenate:hyphenate-chat.*/\tapi("cn.shengwang:chat-sdk:'${SDK_VERSION}'")/g' ease-im-kit/build.gradle.kts
else
  # 更改依赖
  $SED -i 's/.*io.hyphenate:hyphenate-chat.*/\tapi("io.agora.rtc:chat-sdk:'${SDK_VERSION}'")/g' ease-im-kit/build.gradle.kts
fi

git add .
if [[ $is_package_to_shengwang = "true" ]]; then
   git commit -m "convert to shengwang"
   echo "----------------- Finish convert to shengwang -----------------"
else
  git commit -m "convert to agora"
  echo "----------------- Finish convert to agora -----------------"
fi

cd -







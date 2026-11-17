pluginManagement {
    repositories {
        //maven { setUrl("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        //maven { setUrl("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        maven { url = uri("https://download.shengwang.cn/maven/") }
    }
}

rootProject.name = "easeui"
include(":app")
include(":ease-im-kit")
include(":quickstart")

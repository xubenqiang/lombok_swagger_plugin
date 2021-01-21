package com.github.xubenqiang.lombokswaggerplugin.services

import com.github.xubenqiang.lombokswaggerplugin.MyBundle


class MyApplicationService {

    // 项目服务初始化时候
    init {
        println(MyBundle.message("applicationService"))
    }
}

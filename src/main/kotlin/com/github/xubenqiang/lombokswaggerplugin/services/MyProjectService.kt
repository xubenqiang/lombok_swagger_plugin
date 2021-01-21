package com.github.xubenqiang.lombokswaggerplugin.services

import com.github.xubenqiang.lombokswaggerplugin.MyBundle
import com.intellij.openapi.project.Project


class MyProjectService(project: Project){
    init {
        println(MyBundle.message("projectService", project.name))
    }

}

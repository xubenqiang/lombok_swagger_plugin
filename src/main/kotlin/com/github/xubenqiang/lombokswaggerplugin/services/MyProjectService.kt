package com.github.xubenqiang.lombokswaggerplugin.services

import com.intellij.openapi.project.Project
import com.github.xubenqiang.lombokswaggerplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}

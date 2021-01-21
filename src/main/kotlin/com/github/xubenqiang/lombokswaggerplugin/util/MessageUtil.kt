package com.github.xubenqiang.lombokswaggerplugin.util

import com.intellij.openapi.ui.Messages


object MessageUtil {
    fun showInfoMessage(msg : String){
        Messages.showInfoMessage(msg,"INFO")
    }
    fun showErrorMessage(msg : String){
        Messages.showInfoMessage(msg,"ERROR")
    }
}
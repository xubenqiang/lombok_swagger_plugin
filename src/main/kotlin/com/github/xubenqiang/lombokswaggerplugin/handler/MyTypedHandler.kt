package com.github.xubenqiang.lombokswaggerplugin.handler

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler


/**
 * 处理键盘输入事件监听器
 * 比如以下的例子是，当 键盘输入 a 的时候，会在 Document 首行插入 editor_basics 这段文本
 * @author  xubenqiang
 * @date  2020/12/3 10:00
 * @version 1.0
 */
class MyTypeHandler: TypedActionHandler  {
    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        val document = editor.document
        val project = editor.project
        if(charTyped == 'a'){
            val runnable = Runnable { document.insertString(0, "editor_basics\n") }
            WriteCommandAction.runWriteCommandAction(project, runnable)
        }
    }
}
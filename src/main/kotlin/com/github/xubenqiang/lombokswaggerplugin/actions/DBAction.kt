package com.github.xubenqiang.lombokswaggerplugin.actions

import com.github.xubenqiang.lombokswaggerplugin.MyBundle
import com.github.xubenqiang.lombokswaggerplugin.util.MessageUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.annotations.Nullable


/**
 * Action 就相当于是一组动作，这组动作可以显示在菜单中
 * 使用 Action 的基础
 * step1：继承 AnAction
 * step2：注册到  IntelliJ Platform，点击类名 然后 Alt+Enter
 * 文档：https://jetbrains.org/intellij/sdk/docs/tutorials/action_system/working_with_custom_actions.html
 *
 *  键盘监听
 *  val typedAction = TypedAction.getInstance()
    typedAction.setupHandler(MyTypeHandler())

 */
class DBAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println(MyBundle.message("projectService", "This is the DBHelper"))
        // 当前 project 对象
        val project = e.project
        val psiFile: @Nullable PsiFile? = e.getData(CommonDataKeys.PSI_FILE)
        if(psiFile != null){
            val language = PsiUtilBase.getDialect(psiFile)
            if(language.associatedFileType == StdFileTypes.JAVA){
                val psiClass : @Nullable PsiClass? = PsiTreeUtil.getChildOfType(psiFile, PsiClass::class.java)
                if(psiClass != null){
                    val codeStyleManager = CodeStyleManager.getInstance(project!!)
                    val runnable = Runnable { addAnnotationForDB(project, psiClass) }
                    WriteCommandAction.runWriteCommandAction(project, runnable)
                    // 格式化代码
                    WriteCommandAction.runWriteCommandAction(project) { codeStyleManager.reformat(psiClass) }
                }
            }else{
                MessageUtil.showErrorMessage("Cannot handle other Language but JAVA")
            }
        }
    }

    // 快速为 DB 层的方法添加 @QueryByNamed("") @Param @PageIndex @PageSize
    private fun addAnnotationForDB(project: Project,psiClass: PsiClass){
        if(psiClass.isInterface){
            for(method in psiClass.methods){
                if(!method.hasAnnotation(ConfigAction.QueryByNamedWithoutPrefix)){
                    val text = "@QueryByNamed(\"${method.name}\")"
                    val queryByNameAnnotation = createAnnotationFor(project,method,text)
                    method.addBefore(queryByNameAnnotation,method.firstChild)
                }
                for(parameter in method.parameterList.parameters){
                    if(!parameter.hasAnnotation(ConfigAction.ParamWithoutPreFix) && !parameter.hasAnnotation(ConfigAction.PageIndexWithoutPreFix) && !parameter.hasAnnotation(ConfigAction.PageSizeWithoutPreFix)){
                        // pageIndex  和  pageSize  单独处理
                        val annotationText = when (parameter.name) {
                            "pageIndex" -> {
                                "@PageIndex"
                            }
                            "pageSize" -> {
                                "@PageSize"
                            }
                            else -> {
                                "@Param(\"${parameter.name}\")"
                            }
                        }
                        val annotation = createAnnotationFor(project,parameter,annotationText)
                        parameter.addBefore(annotation,parameter.firstChild)
                    }
                }
            }
        }
    }

    // 为指定的 PsiElement 创建 PsiAnnotation，指定 PisAnnotation 插件位置
    private fun createAnnotationFor(project: Project, parentElement: PsiElement, annotationText: String) : PsiAnnotation{
        val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
        return psiElementFactory.createAnnotationFromText(annotationText, parentElement)
    }
}
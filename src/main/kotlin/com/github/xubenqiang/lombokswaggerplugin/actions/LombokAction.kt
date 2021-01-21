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
class LombokAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println(MyBundle.message("projectService", "This is the first LombokAction"))
        // 当前 project 对象
        val project = e.project
        //val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile: @Nullable PsiFile? = e.getData(CommonDataKeys.PSI_FILE)
        if(psiFile != null){
            val language = PsiUtilBase.getDialect(psiFile)
            if(language.associatedFileType == StdFileTypes.JAVA){
                val psiClass : @Nullable PsiClass? = PsiTreeUtil.getChildOfType(psiFile, PsiClass::class.java)
                if(psiClass != null){
                    val codeStyleManager = CodeStyleManager.getInstance(project!!)
                    // lombok 增强
                    val lombokAnnotation = this.getAnnotationsByKey(project, ConfigAction.LOMBOK_CONFIG_KEY, ConfigAction.LOMBOK_DEFAULT_ANNOTATIONS)
                    val lombokRunnable = Runnable { addCustomerAnnotation(project, psiClass, lombokAnnotation) }
                    // 写入到 PsiFile 然后
                    WriteCommandAction.runWriteCommandAction(project, lombokRunnable)
                    // 格式化代码
                    WriteCommandAction.runWriteCommandAction(project) { codeStyleManager.reformat(psiClass) }
                }
            }else{
                MessageUtil.showErrorMessage("Cannot handle other Language but JAVA")
            }
        }
        // 编辑器可见状态
        //e.presentation.isEnabledAndVisible = project != null && editor != null && editor.selectionModel.hasSelection()
        // 获取选中的字符
        // Caret 是被选中的字符串
        // Caret 有逻辑位置和视觉位置
        // 视觉位置就是眼睛看见的位置
        // 逻辑位置是 Editor 中标识该字符串的位置
        // 逻辑位置与视觉位置可能相同，也可能不同
        //val primaryCaret: Caret = editor!!.caretModel.primaryCaret
        // 逻辑位置
        //val logicalPos = primaryCaret.logicalPosition
        // 视觉位置
        //val visualPos = primaryCaret.visualPosition
        //val msg = "logicalPosition is $logicalPos \n visualPosition is $visualPos";
        //Messages.showDialog(msg, "info", arrayOf(Messages.YES_BUTTON), 1, null)
        // 获取 PsiFile
        // priority
        //
    }

    // 为 Class 对象添加自定义注解
    private fun addCustomerAnnotation(project: Project,psiClass: PsiClass,annotationTexts: String){
        // 处理自定义的注解
        if(annotationTexts.contains(ConfigAction.semicolon)){
            for(annotationText in annotationTexts.split(ConfigAction.semicolon)){
                if(!psiClass.hasAnnotation(annotationText.removePrefix(ConfigAction.annotationPrefix))){
                    val annotation = this.createAnnotationFor(project, psiClass, annotationText)
                    psiClass.addAfter(annotation, psiClass.docComment)
                }
            }
        }else{
            if(!psiClass.hasAnnotation(annotationTexts.removePrefix(ConfigAction.annotationPrefix))){
                val annotation = this.createAnnotationFor(project, psiClass, annotationTexts)
                psiClass.addAfter(annotation, psiClass.docComment)
            }
        }
        // 导入
        val lombok = createImport(project, "lombok")
        psiClass.addBefore(lombok,psiClass.firstChild)
    }

    private fun getAnnotationsByKey(project: Project, key: String, default: String) : String {
        var annotations = PropertiesComponent.getInstance(project).getValue(key)
        if(annotations.isNullOrBlank()){
            annotations = default
        }
        return annotations
    }

    // 为指定的 PsiElement 创建 PsiAnnotation，指定 PisAnnotation 插件位置
    private fun createAnnotationFor(project: Project, parentElement: PsiElement, annotationText: String) : PsiAnnotation{
        val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
        return psiElementFactory.createAnnotationFromText(annotationText, parentElement)
    }

    private fun createImport(project: Project,text : String) : PsiImportStatement{
        val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
        return psiElementFactory.createImportStatementOnDemand(text)
    }



}
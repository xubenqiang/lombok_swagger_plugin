package com.github.xubenqiang.lombokswaggerplugin.actions

import com.github.xubenqiang.lombokswaggerplugin.MyBundle
import com.github.xubenqiang.lombokswaggerplugin.util.MessageUtil
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
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
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class Vo2DTOAction : AnAction() {

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
                    val runnable = Runnable {
                        deleteAllNotNull(psiClass)
                        deleteRequired(project,psiClass)
                    }
                    WriteCommandAction.runWriteCommandAction(project, runnable)
                    // 格式化代码
                    WriteCommandAction.runWriteCommandAction(project) { codeStyleManager.reformat(psiClass) }
                }
            }else{
                MessageUtil.showErrorMessage("Cannot handle other Language but JAVA")
            }
        }
    }

    // 删除所有的 @NotNull 注解
    private fun deleteAllNotNull(psiClass: PsiClass){
        for(field in psiClass.fields){
            if(field.hasAnnotation(ConfigAction.NotNullWithoutPrefix)){
                val notNullAnnotation = field.getAnnotation(ConfigAction.NotNullWithoutPrefix)
                notNullAnnotation?.delete()
            }
            if(field.hasAnnotation(ConfigAction.RangeWithoutPrefix)){
                val range = field.getAnnotation(ConfigAction.RangeWithoutPrefix)
                range?.delete()
            }
            if(field.hasAnnotation(ConfigAction.LengthWithoutPrefix)){
                val length = field.getAnnotation(ConfigAction.LengthWithoutPrefix)
                length?.delete()
            }
        }
    }

    // 删除所有的 @ApiModelProperty 中的 required = true 属性
    // 先保存 再添加
    private fun deleteRequired(project: Project,psiClass: PsiClass){
        for(field in psiClass.fields){
            val apiModelProperty = field.getAnnotation(ConfigAction.ApiModelPropertyWithoutPrefix)
            val value = apiModelProperty?.findAttributeValue("value")
            val text = value?.text
            val newAnnotationText = if(isNumber(field.type.presentableText)){
                "@ApiModelProperty(value=$text,example=\"1\")"
            }else{
                "@ApiModelProperty(value=$text)"
            }
            // 删除原来的 @ApiModelProperty 注解
            apiModelProperty!!.delete()
            // 新增一个 @ApiModelProperty 注解
            val newAnnotation = createAnnotationFor(project,field,newAnnotationText)
            field.addBefore(newAnnotation,field.firstChild)
        }
    }

    private fun isNumber(typeName: String) : Boolean{
        if(typeName == "short" || typeName == "Short" || typeName == "int" || typeName=="Integer" || typeName == "long" || typeName=="Long" || typeName == "float" || typeName == "Float" || typeName == "double" || typeName == "Double"){
            return true
        }
        return false
    }

    // 为指定的 PsiElement 创建 PsiAnnotation，指定 PisAnnotation 插件位置
    private fun createAnnotationFor(project: Project, parentElement: PsiElement, annotationText: String) : PsiAnnotation{
        val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
        return psiElementFactory.createAnnotationFromText(annotationText, parentElement)
    }

}
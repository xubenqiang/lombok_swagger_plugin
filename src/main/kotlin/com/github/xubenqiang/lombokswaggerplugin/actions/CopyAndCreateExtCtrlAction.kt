package com.github.xubenqiang.lombokswaggerplugin.actions

import com.github.xubenqiang.lombokswaggerplugin.util.MessageUtil
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.StdFileTypes.JAVA
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.annotations.Nullable
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


class CopyAndCreateExtCtrlAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val psiFile: @Nullable PsiFile? = e.getData(CommonDataKeys.PSI_FILE)
        if(psiFile != null){
            val language = PsiUtilBase.getDialect(psiFile)
            if(language.associatedFileType == JAVA){
                val psiClass : @Nullable PsiClass? = PsiTreeUtil.getChildOfType(psiFile, PsiClass::class.java)
                if(psiClass != null){
                    val codeStyleManager = CodeStyleManager.getInstance(project!!)
                    val runnable = Runnable { createExtClass(project, psiFile, psiClass) }
                    WriteCommandAction.runWriteCommandAction(project, runnable)
                    WriteCommandAction.runWriteCommandAction(project) { codeStyleManager.reformat(psiClass) }
                }
            }else{
                MessageUtil.showErrorMessage("Cannot handle other Language but JAVA")
            }
        }
    }

    private fun createMethods(project: Project, dbClass: PsiClass, extClass: PsiClass){
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val psiElementFactory = javaPsiFacade.elementFactory
        for(method in dbClass.methods){
            var hasThisMethod = false
            for(extMethod in extClass.methods){
                if(method.name == extMethod.name){
                    hasThisMethod = true
                }
            }
            if(!hasThisMethod){
                val returnTypeText = method.returnType?.presentableText
                val sb = StringBuilder()
                val requestParam = StringBuilder()
                for(param in method.parameterList.parameters){
                    if(param.name == "pageIndex" || param.name == "pageSize"){
                        requestParam.append("@RequestParam(value = \"${param.name}\") ${param.type.presentableText} ${param.name}")
                    }else{
                        requestParam.append("@RequestParam(value = \"${param.name}\",required = false) ${param.type.presentableText} ${param.name}")
                    }
                    requestParam.append(",")
                    sb.append(param.name)
                    sb.append(",")
                }
                val requestParamText = requestParam.toString().substring(0, requestParam.length - 2)
                val extDBCallTEXT = sb.toString().substring(0, sb.length - 2)
                val newMethodText = "@GetMapping(\"/${method.name}\") public $returnTypeText ${method.name}($requestParamText){" +
                        "return extDB.${method.name}($extDBCallTEXT);" +
                        "}"
                val newMethod = psiElementFactory.createMethodFromText(newMethodText, extClass)
                extClass.add(newMethod)
            }

        }
    }

    // 将 根据 XXXExtDB 创建 一个 ExtClass 类，然后根据 DB 中的方法，为 ExtClass 创建方法
    // 如果ext类型已经有了，那么只需要为其添加方法
    private fun createExtClass(project: Project, psiFile: PsiFile, dbClass: PsiClass){
        val path = psiFile.virtualFile.path.substringBefore("/db") + "/ctrl"
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(path))
        if(virtualFile != null){
            val directory = PsiManager.getInstance(project).findDirectory(virtualFile)
            // xxxExt
            val extClassName = dbClass.name?.removeSuffix("DB") + "Ctrl"
            if(!Files.exists(Paths.get("$path/${extClassName}.java"))){
                val extClassText = "@APIGroup(\"\")\n" +
                        "@Api(tags = \"\")\n" +
                        "@RestController\n" +
                        "@RequestMapping(\"/\")\n" +
                        "public class $extClassName" +
                        "{" +
                        "    @Resource\n" +
                        "    private ${dbClass.name} extDB;" +
                        "}"
                val psiFileFactory = PsiFileFactory.getInstance(project)
                val extFile  = psiFileFactory.createFileFromText(JavaLanguage.INSTANCE, extClassText)
                val extClass =  PsiTreeUtil.getChildOfType(extFile, PsiClass::class.java)
                createMethods(project, dbClass, extClass!!)
                directory?.add(extClass)
            }else{
                // 已经有 extCtrl 了，直接记载即可
                // 根据限定名加载类
                val javaPsiFacade = JavaPsiFacade.getInstance(project)
                // "com.hdd.pkill.db" --> "com.hdd.pkill." --> "com.hdd.pkill.ctrl.xxxExtCtrl"
                val extClassQualifiedName = "${PsiUtil.getPackageName(dbClass)?.substringBefore("db")}ctrl.$extClassName"
                val extClass = javaPsiFacade.findClass(extClassQualifiedName, GlobalSearchScope.allScope(project))
                createMethods(project, dbClass, extClass!!)
            }
        }
    }
}
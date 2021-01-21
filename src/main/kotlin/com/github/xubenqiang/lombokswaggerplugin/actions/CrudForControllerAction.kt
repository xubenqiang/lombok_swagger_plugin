package com.github.xubenqiang.lombokswaggerplugin.actions

import com.github.xubenqiang.lombokswaggerplugin.MyBundle
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
 * 快速为 Controller 创建 add update delete page detail 接口
 */
class CrudForControllerAction : AnAction() {

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
                    val crudRunnable = Runnable { createCrud(project, psiClass) }
                    // 写入到 PsiFile 然后
                    WriteCommandAction.runWriteCommandAction(project, crudRunnable)
                    // 格式化代码
                    val formatRunnable = Runnable { codeStyleManager.reformat(psiClass) }
                    WriteCommandAction.runWriteCommandAction(project,formatRunnable)
                }
            }
        }
    }

    // 快速创建 crud 方法
    private fun createCrud(project: Project,psiClass: PsiClass){
        val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory

        var hasAdd = false
        var hasUpdate = false
        var hasDelete = false
        var hasPage = false
        var hasDetail = false
        for(method in psiClass.methods){
            if(method.name == "add"){
                hasAdd = true
            }
            if(method.name == "update"){
                hasUpdate = true
            }
            if(method.name == "delete"){
                hasDelete = true
            }
            if(method.name == "page"){
                hasPage = true
            }
            if(method.name == "detail"){
                hasDetail = true
            }
        }

        var voName = ""
        if(psiClass.name!!.contains("Ctrl")){
            voName = psiClass.name!!.replace("Ctrl","")
        }
        if(psiClass.name!!.contains("Controller")){
            voName = psiClass.name!!.replace("Controller","")
        }
        val voClass = voName
        val voNameFirstChar = voName[0]
        val voNameFirstCharUpperCase = voNameFirstChar.toUpperCase()
        val voNameExpectFirstChar = voName.substring(1,voName.length-1)
        val feignName = "${voNameFirstCharUpperCase}${voNameExpectFirstChar}Feign"
        if(!hasAdd){
            val addMethodText = "@ApiOperation(value=\"新增\") @PostMapping(value=\"/add\") public Response<OkDTO> add(@Validated @RequestBody Add${voName}VO vo){" +
                    "        $voClass new$voName = BeanExdUtil.copyProperties(vo, $voName::new);\n" +
                    "        addCreateInfo(new$voName);\n" +
                    "        addUpdateInfo(new$voName);\n" +
                    "        $feignName.add(new$voName);\n" +
                    "        return OkDTO.ok(true);" +
                    "}"
            val addMethod = psiElementFactory.createMethodFromText(addMethodText,psiClass)
            psiClass.add(addMethod)
        }

        if(!hasUpdate){
            val updateMethodText = "@ApiOperation(value=\"编辑\") @PutMapping(value=\"/update\") public Response<OkDTO> update(@Validated @RequestBody Update${voName}VO vo){" +
                    "        $voName update$voName = BeanExdUtil.copyProperties(vo, $voName::new);\n" +
                    "        addUpdateInfo(update$voName);\n" +
                    "        $feignName.update(update$voName);\n" +
                    "        return OkDTO.ok(true);}"
            val updateMethod = psiElementFactory.createMethodFromText(updateMethodText,psiClass)
            psiClass.add(updateMethod)
        }

        if(!hasDelete){
            val deleteMethodText = "@ApiOperation(value=\"删除\") @DeleteMapping(value=\"/delete/{id}\") public Response<OkDTO> delete(@PathVariable(\"id\") Long id){" +
                    "if ($feignName.existsId(id))\n" +
                    "        {\n" +
                    "            $voClass delete$voName  = $feignName.detail(id,false);\n" +
                    "            delete$voName.setIsDelete(true);\n" +
                    "            $feignName.update(delete$voName);\n" +
                    "        }\n" +
                    "        return OkDTO.ok(true);}"
            val deleteMethod = psiElementFactory.createMethodFromText(deleteMethodText,psiClass)
            psiClass.add(deleteMethod)
        }

        if(!hasPage){
            val pageMethodText = "@ApiOperation(value=\"分页\") @PostMapping(value=\"/page/{pageIndex}/{pageSize}\") public Response<Page< >> page(@ApiParam(value = \"当前页\", example = \"1\", required = true) @PathVariable(\"pageIndex\") int pageIndex," +
                    "@ApiParam(value = \"页面容量\", example = \"10\", required = true) @PathVariable(\"pageSize\") int pageSize){}"
            val pageMethod = psiElementFactory.createMethodFromText(pageMethodText,psiClass)
            psiClass.add(pageMethod)
        }

        if(!hasDetail){
            val detailMethodText = "@ApiOperation(value=\"详细信息\") @GetMapping(value=\"/detail/{id}\") public Response< > detail(@PathVariable(\"id\") Long id){" +
                    "if($feignName.existsId(id)){\n" +
                    "            $voName detail$voName = $feignName.detail(id, false);\n" +
                    "            return detail$voName.ok();\n" +
                    "        }else{\n" +
                    "            return AdminErrorCode.E200704.error();\n" +
                    "        }" +
                    "}"
            val detailMethod = psiElementFactory.createMethodFromText(detailMethodText,psiClass)
            psiClass.add(detailMethod)
        }
        val import = psiElementFactory.createImportStatementOnDemand("com.ipsexp.common.entity")
        psiClass.addBefore(import,psiClass.firstChild)
    }
}
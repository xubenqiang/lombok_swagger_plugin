package com.github.xubenqiang.lombokswaggerplugin.actions

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

class SwaggerAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val psiFile: @Nullable PsiFile? = e.getData(CommonDataKeys.PSI_FILE)
        if(psiFile != null){
            val language = PsiUtilBase.getDialect(psiFile)
            if(language.associatedFileType == StdFileTypes.JAVA){
                val psiClass : @Nullable PsiClass? = PsiTreeUtil.getChildOfType(psiFile, PsiClass::class.java)
                if(psiClass != null){
                    val codeStyleManager = CodeStyleManager.getInstance(project!!)
                    val codeFormatRunnable =  Runnable { codeStyleManager.reformat(psiClass) }
                    if((psiClass.name!!.contains("VO") || psiClass.name!!.contains("DTO"))){
                        if(psiClass.name!!.contains("VO")){
                            val runnable1 = Runnable {
                                handleVO(project,psiClass)
                            }
                            WriteCommandAction.runWriteCommandAction(project,runnable1)
                        }
                        if(psiClass.name!!.contains("DTO")){
                            val runnable1 = Runnable {
                                handleDTO(project,psiClass)
                            }
                            WriteCommandAction.runWriteCommandAction(project,runnable1)
                        }
                    }else if(psiClass.name!!.contains("Ext") && psiClass.hasAnnotation(ConfigAction.RestControllerWithoutPrefix)){
                        val runnable1 = Runnable {
                            handleExtCtrl(project,psiClass)
                        }
                        WriteCommandAction.runWriteCommandAction(project,runnable1)
                    }else{
                        // 处理 分页接口 和 restful 接口
                        val runnable1 = Runnable { for(psiMethod in psiClass.methods){
                            checkPage(project,psiMethod)
                            handleRestful(project,psiMethod) } }
                        WriteCommandAction.runWriteCommandAction(project,runnable1)
                        // @ApiOperation
                        // 处理 Page 接口
                        // @PathVariable @RequestParam @RequestBody
                        // @Range @Validated
                        // @ApiParam(value="")
                        // 处理 @ApiOperation，@RequestParam，@RequestBody，@PathVariable
                        val runnable2 = Runnable {
                            handleApiOperationOrRequestParamOrRequestBody(project, psiClass)
                            checkAndAddApiParamAndValidate(project, psiClass)
                        }
                        WriteCommandAction.runWriteCommandAction(project,runnable2)
                    }
                    WriteCommandAction.runWriteCommandAction(project,codeFormatRunnable)
                }
            }
        }
    }

    // Swagger 插件处理核心逻辑
    private fun handleApiOperationOrRequestParamOrRequestBody(project: Project, psiClass: PsiClass){
        if(psiClass.hasAnnotation(ConfigAction.ApiWithoutPrefix)){ // 处理 @Api 也就是 Controller
            val methods = psiClass.methods
            for(psiMethod in methods){
                //handleRestful(project,psiMethod)
                if(psiMethod.modifierList.hasModifierProperty(PsiModifier.PUBLIC)){
                    if(!psiMethod.hasAnnotation(ConfigAction.ApiOperationTextWithoutPrefix)){
                        // @ApiOperation
                        val psiAnnotation = this.createAnnotationFor(project, psiMethod, ConfigAction.ApiOperationAnnotation)
                        psiMethod.addAfter(psiAnnotation, psiMethod.docComment)
                    }
                    for(psiParam in psiMethod.parameterList.parameters){
                        // 检查是否需要添加 @RequestBody 或 @RequestParam 或 @PathVariable
                        handleApiMethodParameter(project,psiParam,psiMethod)
                    }
                }
            }
        }
    }
// ------------------------------------ 处理 @Api -----------------------------------------------------------------------------------

    // 处理 restful 参数
    private fun handleRestful(project: Project,psiMethod: PsiMethod){
        if(psiMethod.modifierList.hasModifierProperty(PsiModifier.PUBLIC) && isRestful(psiMethod)){
            val restfulParameters = getRestfulApiTerm(psiMethod)
            // 如果没有该参数，就需要添加该参数，如果有该参数，则为该参数添加 @PathVariable 注解
            for(parameterName in restfulParameters){
                var hasParameter = false
                var pm : PsiParameter? = null
                for(psiParameter in psiMethod.parameterList.parameters){
                    if(parameterName == psiParameter.name){
                        hasParameter = true
                        pm = psiParameter
                        break
                    }
                }
                if(hasParameter && pm != null && !(isPage(psiMethod.name))){
                    addPathVariable(project,pm)
                }
            }
        }
    }

    // 只处理 restful ，不处理 page 或 RequestParam
    private fun addPathVariable(project: Project,psiParam: PsiParameter){
        if(!psiParam.hasAnnotation(ConfigAction.PathVariableWithoutPrefix) && !psiParam.hasAnnotation(ConfigAction.RequestParamWithoutPrefix)){
            val annotation = this.createAnnotationFor(project,psiParam,"@PathVariable(\"${psiParam.name}\")" )
            psiParam.addBefore(annotation,psiParam.firstChild)
        }
    }

    private fun isRestful(psiMethod: PsiMethod) : Boolean{
        for(annotation in psiMethod.annotations){
            if(annotation.qualifiedName!!.endsWith("Mapping")){
                val value = annotation.findAttributeValue("value")
                return value != null && value.text.contains("{") && value.text.contains("}")
            }
        }
        return false
    }

    private fun getRestfulApiTerm(psiMethod: PsiMethod) : List<String>{
       val list = mutableListOf<String>()
       for(annotation in psiMethod.annotations){
            if(annotation.qualifiedName!!.endsWith("Mapping")){
                val value = annotation.findAttributeValue("value")
                if(value != null){
                    val valueText = value.text
                    if(valueText.contains("{") || valueText.contains("}")){
                        val urlTerms = valueText.removePrefix("\"").removeSuffix("\"").split("/")
                        for(term in urlTerms){
                            if(term.contains("{")){
                                list.add( term.replace("{","").replace("}",""))
                            }
                        }
                    }
                }
            }
       }
       return list
    }

    private fun handleApiMethodParameter(project: Project,psiParam: PsiParameter,psiMethod: PsiMethod){
        if(psiParam.type.presentableText.endsWith("VO")){
            if(!psiParam.hasAnnotation(ConfigAction.RequestBodyWithoutPrefix)){
                // 添加 RequestBody
                val annotation = createAnnotationFor(project,psiParam, ConfigAction.RequestBodyAnnotation)
                psiParam.addBefore(annotation,psiParam.firstChild)
            }
        }else{
            // 添加 @RequestParam 注解
            if(!psiParam.hasAnnotation(ConfigAction.RequestParamWithoutPrefix) && !psiParam.hasAnnotation(ConfigAction.PathVariableWithoutPrefix)){
                // 排除 page 和 restful 接口中 restful 参数
                if(isRestful(psiMethod)){
                    val restfulParameterNames = getRestfulApiTerm(psiMethod)
                    if(restfulParameterNames.contains(psiParam.name)){
                        return
                    }
                }
                val annotationText: String = if(isPage(psiMethod.name)){
                    "@RequestParam(value=\"${psiParam.name}\",required=false)"
                }else{
                    "@RequestParam(value=\"${psiParam.name}\")"
                }
                val annotation = createAnnotationFor(project,psiParam,annotationText)
                psiParam.addBefore(annotation,psiParam.firstChild)
            }
        }
    }

    // 添加 @ApiParam 注解
    private fun checkAndAddApiParamAndValidate(project: Project, psiClass: PsiClass){
        for(method in psiClass.methods){
            if(method.modifierList.hasModifierProperty(PsiModifier.PUBLIC)){
                if(isPage(method.name)){
                   return
                }
                // 只为非 page 接口添加 @ApiParam 因为一般来说 page 接口的参数会通过 VO 封装
                for(psiParam in method.parameterList.parameters){
                    val requestAnnotation = psiParam.getAnnotation(ConfigAction.RequestParamWithoutPrefix)
                    val pathVariableAnnotation = psiParam.getAnnotation(ConfigAction.PathVariableWithoutPrefix)
                    if(pathVariableAnnotation != null){
                        if(!psiParam.hasAnnotation(ConfigAction.ApiParamWithoutPrefix)){
                            var apiParamText = "@ApiParam(value=\"${psiParam.name}\",required=true)"
                            if(isNumber(psiParam.type.presentableText)){
                                apiParamText = "@ApiParam(value=\"${psiParam.name}\",example=\"1\",required=true)"
                            }
                            val apiParamAnnotation = this.createAnnotationFor(project, psiParam, apiParamText)
                            psiParam.addBefore(apiParamAnnotation, psiParam.firstChild)
                        }
                    }else if(requestAnnotation != null){
                        // 为 Number 类型的 参数添加 @Range 注解
                        addRangeForRequestParam(project,psiParam)
                        if(!psiParam.hasAnnotation(ConfigAction.ApiParamWithoutPrefix)){
                            var apiParamText: String
                            val requestParamAnnotationText = requestAnnotation.text
                            // required = true
                            apiParamText = if(this.requiredIsTrue(requestParamAnnotationText)){
                                if(isNumber(psiParam.type.presentableText)){
                                    "@ApiParam(value=\" \",example=\"1\",required=true)"
                                }else{
                                    "@ApiParam(value=\" \",required=true)"
                                }
                            }else{
                                if(isNumber(psiParam.type.presentableText)){
                                    "@ApiParam(value=\" \",example=\"1\")"
                                }else{
                                    "@ApiParam(value=\" \")"
                                }
                            }
                            val apiParamAnnotation = this.createAnnotationFor(project, psiParam, apiParamText)
                            psiParam.addBefore(apiParamAnnotation, psiParam.firstChild)
                        }
                    }
                    checkAndAddValidatedAnnotation(project,psiParam)
                }
            }
        }
    }

    //　如果方法的名字中有　page 或 Page 添加 @PathVariable("pageIndex") int pageIndex 和 @PathVariable("pageSize") int pageSize
    private fun checkPage(project: Project,psiMethod: PsiMethod){
        val methodName = psiMethod.name
        // 是 restful 和
        if( isRestful(psiMethod) && isPage(methodName)){
            // 包含 page 或者 Page 则会被认为是一个 分页接口
            var hasPageIndex = false
            var hasPageSize = false
            for(parameter in psiMethod.parameterList.parameters){
                if(parameter.name == "pageIndex"){
                    hasPageIndex = true
                }
                if(parameter.name == "pageSize"){
                    hasPageSize = true
                }
            }
            val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
            if(!hasPageIndex){
                val pageIndexParam : PsiParameter = psiElementFactory.createParameter("pageIndex", PsiType.INT)
                val pageIndexAnnotation = createAnnotationFor(project,pageIndexParam,"@PathVariable(\"pageIndex\")")
                pageIndexParam.addBefore(pageIndexAnnotation,pageIndexParam.typeElement)
                val apiParamAnnotation1 = createAnnotationFor(project,pageIndexParam,"@ApiParam(value =\"当前页\", example=\"1\",required=true)")
                pageIndexParam.addBefore(apiParamAnnotation1,pageIndexParam.firstChild)
                psiMethod.parameterList.add(pageIndexParam)
            }
            if(!hasPageSize){
                val pageSizeParam = psiElementFactory.createParameter("pageSize", PsiType.INT)
                val pageSizeAnnotation = createAnnotationFor(project,pageSizeParam,"@PathVariable(\"pageSize\")")
                pageSizeParam.addBefore(pageSizeAnnotation,pageSizeParam.typeElement)
                val apiParamAnnotation2 = createAnnotationFor(project,pageSizeParam,"@ApiParam(value = \"页面容量\", example = \"10\",required=true)")
                pageSizeParam.addBefore(apiParamAnnotation2,pageSizeParam.firstChild)
                psiMethod.parameterList.add(pageSizeParam)
            }
        }
    }

    // 添加 @Validated
    private fun checkAndAddValidatedAnnotation(project: Project,psiParam: PsiParameter){
        val requestBodyAnnotation = psiParam.getAnnotation(ConfigAction.RequestBodyWithoutPrefix)
        if(requestBodyAnnotation != null){
            // 查看是否有 @Validated 注解，如果没有，则需要新增一个
            if(!psiParam.hasAnnotation(ConfigAction.ValidatedWithoutPrefix)){
                val apiParamAnnotation = this.createAnnotationFor(project, psiParam, ConfigAction.ValidatedAnnotationText)
                psiParam.addBefore(apiParamAnnotation, psiParam.firstChild)
            }
        }
    }

    // 为 @RequestParam(value="") 添加 @Range Integer,int,Long,long 等类型，前提是 @RequestParam 的 required = true
    private fun addRangeForRequestParam(project: Project,psiParam: PsiParameter){
        if(!psiParam.hasAnnotation(ConfigAction.RangeWithoutPrefix)){
            // 只针对 @RequestParam
            // 必须是数字且要求该参数必传的时，添加 @Range 注解
            if(isNumber(psiParam.type.presentableText) && requiredIsTrue(psiParam.text)){
                val text = "@Range(min=1,message=\"${psiParam.name} 必须大于 0\")"
                val rangeAnnotation = createAnnotationFor(project,psiParam,text)
                psiParam.addBefore(rangeAnnotation,psiParam.firstChild)
            }
        }
    }

    // 判断 required 是否为 true
    private fun requiredIsTrue(text : String) : Boolean{
        return !(text.contains("required") && text.contains("false"))
    }

// ------------------------------------ 处理 @ApiModel ---------------------------------------------------------------------------------

    // 以 VO 结尾的，需要添加 @ApiModelProperty 和 @Range 和 @NotNull 等注解
    // @ApiModelProperty
    // @Range
    private fun handleVO(project: Project, @NotNull psiClass: PsiClass){
        if(psiClass.qualifiedName!!.endsWith("VO")){
            for(psiField in psiClass.fields){
                checkAndAddApiModelPropertyAnnotation(project,psiClass,psiField)
                checkAndRangeOrLengthAnnotation(project,psiClass,psiField)
            }
        }
    }

    // 处理 DTO 类
    private fun handleDTO(project: Project, psiClass: PsiClass){
            val interfaceName = "OK<${psiClass.name}>"
            var isHave = false
            val implementsElements = psiClass.implementsList?.referenceElements
            for(element in implementsElements!!){
                if(element.qualifiedName!! == "com.ipsexp.common.entity.OK" ){
                    isHave = true
                }
            }
            // 添加 OK<xxx> 接口
            if(!isHave){
                val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
                val ref = psiElementFactory.createReferenceFromText(interfaceName,psiClass.lBrace)
                psiClass.implementsList?.add(ref)
            }

            // 将 @ApiModel 添加在注释后
            if(!psiClass.hasAnnotation(ConfigAction.ApiModelWithoutPrefix)){
                val text = "@ApiModel"
                val apiModelAnnotation = createAnnotationFor(project,psiClass,text)
                psiClass.addAfter(apiModelAnnotation, psiClass.docComment)
            }

            // @ApiModelProperty 注解
            for(psiField in psiClass.fields){
                checkAndAddApiModelPropertyAnnotation(project,psiClass,psiField)
            }
    }

    // 添加 @ApiModelProperty 注解
    private fun checkAndAddApiModelPropertyAnnotation(project: Project, psiClass: PsiClass, field: PsiField){
        if(!field.hasAnnotation(ConfigAction.ApiModelPropertyWithoutPrefix)){
            val apiModelPropertyText = if(this.isNumber(field.type.presentableText)){
                // @ApiModelProperty(value="",example="1")
                "@ApiModelProperty(value=\" \",example=\"1\")"
            }else{
                // @ApiModelProperty(value="")
                ConfigAction.ApiModelPropertyAnnotation
            }
            val apiModelPropertyAnnotation = this.createAnnotationFor(project, psiClass, apiModelPropertyText)
            psiClass.addBefore(apiModelPropertyAnnotation, field)
        }
    }

    // 添加 @Range 或 @Length 注解
    private fun checkAndRangeOrLengthAnnotation(project: Project, psiClass: PsiClass, field: PsiField){
        if(this.isNumber(field.type.presentableText)){
            if(!field.hasAnnotation(ConfigAction.RangeWithoutPrefix)){
                val rangeText = "@Range(min=1,message=\"${field.name} 必须大于 0\")"
                val rangeAnnotation = this.createAnnotationFor(project, psiClass, rangeText)
                val notNullAnnotation = this.createAnnotationFor(project, psiClass, "@NotNull(message=\"${field.name} 不能为空\")")
                psiClass.addBefore(rangeAnnotation, field)
                psiClass.addBefore(notNullAnnotation, field)
            }
        }
        if(field.type.presentableText == "String"){
            if(!field.hasAnnotation(ConfigAction.NotBlankWithoutPrefix)){
                val notBlankText = "@NotBlank(message=\"${field.name} 不能为空和空字符\")"
                val notBlankAnnotation = this.createAnnotationFor(project, psiClass, notBlankText)
                val lengthAnnotation = this.createAnnotationFor(project,psiClass,"@Length(min=1,max=255,message=\"${field.name} 长度必须大于 0\")")
                psiClass.addBefore(notBlankAnnotation, field)
                psiClass.addBefore(lengthAnnotation,field)
            }
        }
    }

    // 判断给定的字符串是否为数字类型
    private fun isNumber(typeName: String) : Boolean{
        if(typeName == "short" || typeName == "Short" || typeName == "int" || typeName=="Integer" || typeName == "long" || typeName=="Long" || typeName == "float" || typeName == "Float" || typeName == "double" || typeName == "Double"){
            return true
        }
        return false
    }

    private fun createAnnotationFor(project: Project, parentElement: PsiElement, annotationText: String) : PsiAnnotation{
        val psiElementFactory = JavaPsiFacade.getInstance(project).elementFactory
        return psiElementFactory.createAnnotationFromText(annotationText, parentElement)
    }

    private fun isPage(name:String) : Boolean{
        return name.contains("page") || name.contains("Page")
    }

    // 处理 ExtCtrl
    private fun handleExtCtrl(project: Project,psiClass: PsiClass){
        // 所有方法的参数加上 @RequestParam(value="参数名",required=false)
        for(method in psiClass.allMethods){
            if(method.modifierList.hasModifierProperty(PsiModifier.PUBLIC)){
                for(parameter in method.parameterList.parameters){
                    if(!parameter.hasAnnotation(ConfigAction.RequestParamWithoutPrefix)){
                        val text = "@RequestParam(value=\"${parameter.name}\",required=false)"
                        val requestParamAnnotation = createAnnotationFor(project,parameter,text)
                        parameter.addBefore(requestParamAnnotation,parameter.firstChild)
                    }
                }
            }
        }
    }

    // 包装类，Byte Short Integer Long Float Double
//    private fun isWrapper(typeText : String) : Boolean{
//        return typeText == "Byte" || typeText == "Short" || typeText == "Integer" || typeText == "Long" || typeText == "Float" || typeText == "Double" || typeText == "Boolean" || typeText == "Character"
//    }

    // 判断是否为 字符串类型
//    private fun isString(typeText: String) : Boolean{
//        return typeText == "String"
//    }


}
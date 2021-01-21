package com.github.xubenqiang.lombokswaggerplugin.actions


import com.github.xubenqiang.lombokswaggerplugin.ui.LombokConfigUI
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ConfigAction : AnAction() {

    companion object{
        private const val PREFIX = "com.github.xubenqiang.lombokswaggerplugin"
        const val LOMBOK_CONFIG_KEY = "$PREFIX.lombok_config_key"
        const val SWAGGER_MODEL_CONFIG_KEY = "$PREFIX.swagger_model_config_key"
        const val LOMBOK_DEFAULT_ANNOTATIONS = "@Accessors(chain = true);@Getter;@Setter;@AllArgsConstructor;@NoArgsConstructor;@ToString"
        const val annotationPrefix = "@"
        const val ApiOperationTextWithoutPrefix = "io.swagger.annotations.ApiOperation"
        const val ApiOperationAnnotation = "@ApiOperation(value=\" \")"
        const val ApiModelPropertyWithoutPrefix = "io.swagger.annotations.ApiModelProperty"
        const val ApiModelPropertyAnnotation = "@ApiModelProperty(value=\" \")"
        const val ApiWithoutPrefix = "io.swagger.annotations.Api"
        const val RequestParamWithoutPrefix = "org.springframework.web.bind.annotation.RequestParam"
        const val ValidatedWithoutPrefix = "org.springframework.validation.annotation.Validated"
        const val ValidatedAnnotationText = "@Validated"
        const val ApiParamWithoutPrefix = "io.swagger.annotations.ApiParam"
        const val ApiModelWithoutPrefix = "io.swagger.annotations.ApiModel"
        const val RequestBodyWithoutPrefix = "org.springframework.web.bind.annotation.RequestBody"
        const val PathVariableWithoutPrefix = "org.springframework.web.bind.annotation.PathVariable"
        const val RequestBodyAnnotation = "@RequestBody"
        const val RangeWithoutPrefix = "org.hibernate.validator.constraints.Range"
        const val LengthWithoutPrefix = "org.hibernate.validator.constraints.Length"
        const val RestControllerWithoutPrefix = "org.springframework.web.bind.annotation.RestController"
        const val PageIndexWithoutPreFix = "org.fastquery.page.PageIndex"
        const val PageSizeWithoutPreFix = "org.fastquery.page.PageSize"
        const val ParamWithoutPreFix = "org.fastquery.core.Param"
        const val QueryByNamedWithoutPrefix = "org.fastquery.core.QueryByNamed"
        const val NotBlankWithoutPrefix = "javax.validation.constraints.NotBlank"
        const val NotNullWithoutPrefix = "javax.validation.constraints.NotNull"
        const val DTOText = "DTO"
        const val semicolon = ";"
    }

    override fun actionPerformed(e: AnActionEvent) {
        // 渲染界面
        LombokConfigUI.ui(e.project!!)
    }


}
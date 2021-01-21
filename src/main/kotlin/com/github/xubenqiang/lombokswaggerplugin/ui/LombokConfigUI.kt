package com.github.xubenqiang.lombokswaggerplugin.ui

import com.github.xubenqiang.lombokswaggerplugin.actions.ConfigAction
import com.github.xubenqiang.lombokswaggerplugin.util.MessageUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.panel
import javax.swing.*

object LombokConfigUI {
    private const val LOMBOK_LABEL_NAME = "Lombok Annotations"
    private const val SWAGGER_LABEL_NAME = "Swagger ApiModel Annotations"
    fun ui(project : Project) : JPanel{
        val properties = PropertiesComponent.getInstance(project)
        val frame = JFrame()

        // lombok 输入框
        val lomBokAnnotationTextField = JTextField()
        val lombokConfigText = properties.getValue(ConfigAction.LOMBOK_CONFIG_KEY)
        if(lombokConfigText.isNullOrBlank()){
            lomBokAnnotationTextField.text = ConfigAction.LOMBOK_DEFAULT_ANNOTATIONS
        }else{
            lomBokAnnotationTextField.text = lombokConfigText
        }

        // swagger model 输入框
//        val swaggerAnnotationTextField = JTextField()
//        val swaggerModelConfigText = properties.getValue(ConfigAction.SWAGGER_MODEL_CONFIG_KEY)
//        if(swaggerModelConfigText.isNullOrBlank()){
//            swaggerAnnotationTextField.text = ConfigAction.SWAGGER_MODEL_DEFAULT_ANNOTATIONS
//        }else{
//            // 更新 swagger annotation 文本框的内容
//            swaggerAnnotationTextField.text = swaggerModelConfigText
//        }
        val confirmedButton = JButton("save")
        val cancelButton = JButton("cancel")
        val defaultButton = JButton("restore default")
        val contentPanel =  panel {
            noteRow("Configuration Lombok Annotation list , You can add new Annotation to this list. \n " +
                    "like this : \n" +
                    "\t @Accessors(chain = true) \n " +
                    "\t @Getter \n " +
                    "\t @Setter \n  " +
                    "Use \";\" to separate two Annotations .")
            row("$LOMBOK_LABEL_NAME: ") { lomBokAnnotationTextField() }
            //row("$SWAGGER_LABEL_NAME: ") { swaggerAnnotationTextField() }
            row {
                row(" ") {
                    confirmedButton()
                    defaultButton()
                    //right{  }
                    cancelButton()
                }
            }
        }

        // 确认按钮点击事件
        confirmedButton.addActionListener {
            run {
                val isOk = checkAndSetProperties(project, lomBokAnnotationTextField.text)
                if(isOk){
                    MessageUtil.showInfoMessage("save successful ")
                    frame.isVisible = false
                }

            }
        }

        // 恢复默认 清除内容
        defaultButton.addActionListener {
            run{
                // 恢复默认
                properties.setValue(ConfigAction.LOMBOK_CONFIG_KEY,"")
                properties.setValue(ConfigAction.SWAGGER_MODEL_CONFIG_KEY,"")
                MessageUtil.showInfoMessage("restore default successful")
                frame.isVisible = false
            }
        }

        // 取消按钮点击事件
        cancelButton.addActionListener {
            run{
                frame.isVisible = false
            }
        }

        frame.add(contentPanel)
        // 自适应
        // frame.pack()
        frame.setSize(900,400)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        return contentPanel
    }


    // 验证 和 保存用户输入的配置信息
    private fun checkAndSetProperties(project:Project, lombokConfig:String) : Boolean{
        // 是否为空 或 empty
        // 是否包含中文分号
        // 是否以 @ 作为前缀
        val properties = PropertiesComponent.getInstance(project)
        return if(lombokConfig.isBlank() || lombokConfig.contains("；") || !lombokConfig.startsWith(ConfigAction.annotationPrefix)){
            MessageUtil.showErrorMessage("$LOMBOK_LABEL_NAME or $SWAGGER_LABEL_NAME can not be empty or contains invalid characters")
            false
        }else{
            if(lombokConfig.contains(";")){
                for(lombokAnnotation in lombokConfig.split(";")){
                    if(!lombokAnnotation.startsWith(ConfigAction.annotationPrefix)){
                        return false
                    }
                }
            }
            properties.setValue(ConfigAction.LOMBOK_CONFIG_KEY,lombokConfig)

            true
        }
    }
}
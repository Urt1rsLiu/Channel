package com.royole.plugin

import com.android.build.gradle.api.BaseVariant
import com.android.builder.core.BuilderConstants
import com.royole.plugin.extension.ChannelExtension
import com.royole.plugin.task.ApkChannelTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task

/**
 * @author Hongzhi Liu
 * @date 2018/9/29 14:02 
 */
class ChannelApkPlugin implements Plugin<Project> {
    Project mProject
    ChannelExtension mChannelExtension
    Task mApkChannelTask
    List<String> mChannelInfoList

    @Override
    void apply(Project project) {
        mProject = project
        mChannelExtension = project.extensions.create("channel", ChannelExtension, project)

//        这里不能读取extension
//        getChannelInfoList(mChannelExtension.channelFile)

        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new ProjectConfigurationException("apply 'ChannelApkPlugin' Fail: plugin 'com.android.application' must be apply", null)
        }

        /**
         * build.gradle 配置完成后读取ext,读取渠道信息 并创建task
         */
        project.afterEvaluate {
            def dirName, variantName

            println mChannelExtension.channelFile.absolutePath
            println mChannelExtension.fastMode

            mChannelInfoList = getChannelInfoList()

            /**
             * 前面判断project是否使用com.android.application插件，若使用，则能找到android.applicationVariants
             * 一般variant 分两种：debug 和release
             * 这里进行了优化，只为buildType为release 的variant 创建task
             */

            project.android.applicationVariants.all { BaseVariant variant ->
                // 遍历获得各个variant
//                int variantAmount = variant.outputs.size()
//                for (int i = 0; i < variantAmount; i++) {
//                    BaseVariantOutput variantOutput = variant.outputs.iterator().next()
//                }
//                variant.outputs.each {
//                    //处理manifest后对manifest进行修改
//                    it.processManifest.doFirst {
//                        if (project.hasProperty("single_process") && project.property("single_process").equals("2")) {
//                            //受gradle版本影响，AndroidManifest.xml文件位置有所不同
//                            def manifestFile = new File("${buildDir}/intermediates/bundles/${it.dirname}/AndroidManifest.xml")
//                            if (!manifestFile.exists()) {
//                                def dir = ''
//                                it.baseName.split('-').each {
//                                    dir = dir + it + '/'
//                                }
//                                manifestFile = new File("${buildDir}/intermediates/manifests/full/${dir}AndroidManifest.xml")
//                            }
//
//                            def XmlParser = new XmlParser()
//                            def manifest = XmlParser.parse(manifestFile)
//                            def processTag = "android:process"
//                            def android = new Namespace('http://schemas.android.com/apk/res/android', 'android')
//                            def nodes = manifest.application[0].'*'.findAll {
//                                (it.name() == 'activity' || it.name() == 'service') && it.attribute(android.process) == null
//                                //选择要修改的节点
//                            }
//                            nodes.each {
//                                it.attributes().put(processTag, ":wallet_sdk")
//                            }
//                            PrintWriter pw = new PrintWriter(manifestFile)
//                            pw.write(groovy.xml.XmlUtil.serialize(manifest))
//                            pw.close()
//                        }
//                    }
//                }

                // variantOutput = variant.outputs.first()
                // variantOutput.outputFile  即可获得variant对应的apk的file对象

                dirName = variant.dirName
                variantName = variant.name.capitalize()
                println "----------dirName : ${dirName}-------------"
                println "----------variantName : ${variantName}-------------"
                println "----------buildType : ${variant.buildType.name}-------------"


                if (BuilderConstants.RELEASE.equalsIgnoreCase(variant.buildType.name)){
                    mApkChannelTask = mProject.tasks.create("channel${variantName} ", ApkChannelTask)
                    mApkChannelTask.setGroup("channel")
//                    mApkChannelTask.dependsOn variant.assemble
                    mApkChannelTask.variant = variant
                    mApkChannelTask.channelExtension = mChannelExtension
                    mApkChannelTask.channelList = mChannelInfoList
                }

            }
        }
    }

    /**
     * 读取渠道信息,默认从%{project}/channel.txt 读取
     */
    List<String> getChannelInfoList() {
        File channelFile
//        if (mChannelExtension.channelFile == null) {
//            println '---------channelFile is null---------'
//            channelFile = new File(mProject.getRootDir(), "channel.txt")
//        } else {
        channelFile = mChannelExtension.channelFile
//        }
        List<String> channelListInfo = new ArrayList()
        if (channelFile.exists() && channelFile.isFile()) {
            channelFile.eachLine { line, num ->
                String[] array = line.split('#')
                if (null != array && null != array[0]) {
                    println "channelInfo add ${array[0].trim()}"
                    channelListInfo.add(array[0].trim())
                } else {
                    println "ChannelApkPlugin:  skip line: ${num} / content: ${line}  when reading channel file"
                }
            }
        } else {
            println "----------ChannelApkPlugin:  can't find channel file-------------"
            throw new ProjectConfigurationException("apply 'ChannelApkPlugin' Fail:can not find channel file", null)
        }
        return channelListInfo
    }

}

package com.royole.extension

import com.royole.constant.ChannelConfig
import org.gradle.api.Project

/**
 * @author Hongzhi Liu  2014302580200@whu.edu.cn
 * @date 2018/9/29 14:04 
 */

/**
 * 配置渠道包信息
 */
class ChannelExtension {

    /**
     * 快速模式，默认为非快速模式
     * 快速模式下生成后不会校验签名信息
     */
    boolean fastMode = false


//    /**
//     * 基础debug apk
//     */
//    File baseDebugApkFile
//
//    /**
//     * 基础release apk
//     */
//    File baseReleaseApkFile

    /**
     * 输出渠道包目录 (debug and release)
     */

//    /**
//     * 输出debug 渠道包目录
//     */
//    File outputDebugDir
//
//    /**
//     * 输出release 渠道包目录
//     */
//    File outputReleaseDir

    /**
     * 输出渠道包总目录
     */
    File baseOutputDir

    /**
     * 渠道信息文件
     */
    File channelFile

    /**
     * 渠道包版本
     */
    String channelApkVersion


    /**
     * 构造函数带参数的extension 构建过程
     * 先执行构造函数，再读取gradle 中的extension 并进行赋值
     * 所以可以在该构造函数中确定所有成员的默认值
     * @param project
     */
    ChannelExtension(Project project){
        println "execute constructor"
        fastMode = false
        channelFile = new File(project.getRootDir(), ChannelConfig.DEFAULT_CHANNEL_FILE_NAME)
        baseOutputDir = new File(project.getRootDir(), ChannelConfig.defaultOutputDirName)
    }

}

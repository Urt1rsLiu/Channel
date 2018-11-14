package com.royole.task

import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SigningConfig
import com.royole.constant.ChannelConfig
import com.royole.constant.SignatureMode
import com.royole.extension.ChannelExtension
import com.royole.util.FileUtil
import com.royole.util.V1ChannelUtil
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * @author Hongzhi Liu  2014302580200@whu.edu.cn
 * @date 2018/9/29 14:12
 *
 * 该task 根据 project 的variant 打各类包，分为debug 和release 包
 */
class ApkChannelTask extends DefaultTask {

    /**
     * 签名模式
     */
    @Input
    int signatureMode

    /**
     * BaseVariant
     */
    @Input
    BaseVariant variant

    @Input
    ChannelExtension channelExtension

    /**
     * 基础包路径
     * 基础包可以通过BaseVariant 对象获取
     * 也可直接通过工程相对路径(projectDir/build/outputs/apk/(debug/release)/(debug.apk/release.apk))获取
     * 但相对路径在不同环境下可能不同，还是通过API 提供的对象获取更可靠
     */
    File baseApk

    /**
     * 输出的子目录(debug/release)
     */
    private File outputDir

    /**
     * 各渠道信息
     */
    @Input
    List<String> channelList

    ApkChannelTask() {
        super()
    }

    @TaskAction
    def channel() {
        checkParameter()
        checkSignature()
        println('outputDir:  ' + outputDir.absolutePath)
        channelList.each { String a ->
            println a
        }

        generateChannelApk()


    }

    /**
     * 检查打包的配置参数
     */
    private void checkParameter() {

        //1.确定各渠道的信息
        if (channelList == null || channelList.isEmpty()) {
            throw new GradleException("----------channel plugin: no channel info specified-------------")
        }

        //2.确定variant
        if (variant == null) {
            throw new GradleException("----------channel plugin: read variant is null-------------")
        }

        //3.确定基础的apk包
        baseApk = variant.outputs.first().outputFile
        println "---------channel plugin: base apk path is ${baseApk.absolutePath}-------"
        if (baseApk == null || !baseApk.exists() || !baseApk.isFile()) {
            throw new GradleException("----------channel plugin: couldn't find baseApk-------------")
        }

        //4.确定输出的总目录
        def baseOutputDir = channelExtension.baseOutputDir
        if (baseOutputDir == null) {
            baseOutputDir = new File(project.getRootDir(), ChannelConfig.defaultOutputDirName)
        }

        //5.确定输出的子目录,目录不存在则创建,存在则删除旧的apk包
        outputDir = new File(baseOutputDir, variant.dirName)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        } else {
            outputDir.eachFile { file ->
                file.delete()
            }
        }

    }

    /**
     * 检查project 的gradle 中的签名配置
     */
    private void checkSignature() {
        SigningConfig signingConfig = initSigningConfig()
        if (signingConfig == null) {
            throw new GradleException("SigningConfig is null , please check it")
        }
        //Default configuration of build.gradle is that v2&v1 signature is enabled.
        //This task prefer to write only V2 signature on the apk while v2&v1 signature is enabled
        //But if v2 signature is enabled and v1 signature is disabled, this task will throw an exception
        //apk without V1 signature can't be installed on the device with Android system below 7.0
        if (signingConfig.hasProperty("v2SigningEnabled") && signingConfig.v2SigningEnabled) {
            if (signingConfig.hasProperty("v1SigningEnabled") && !signingConfig.v1SigningEnabled) {
                throw new GradleException("you only assign V2 Mode , but not assign V1 Mode , you can't install Apk below 7.0")
            }
            signatureMode = SignatureMode.V2_MODE
        } else if ((signingConfig.hasProperty("v1SigningEnabled") && signingConfig.v1SigningEnabled) || !signingConfig.hasProperty("v1SigningEnabled")) {
            signatureMode = SignatureMode.V1_MODE
        } else {
            throw new GradleException("you must assign V1 or V2 Mode")
        }
    }

    /**
     * 获取签名设置
     */
    private SigningConfig initSigningConfig() {
        SigningConfig config
        if (variant.buildType.signingConfig == null) {
            config = variant.mergedFlavor.signingConfig
        } else {
            config = variant.buildType.signingConfig
        }
        return config
    }

    private void generateChannelApk() {
        if (SignatureMode.V1_MODE == signatureMode) {
            generateV1ChannelApk()
        } else if (SignatureMode.V2_MODE == signatureMode) {
            generateV2ChannelApk()
        } else {
            throw new Exception("-------channel plugin: read signature fail--------")
        }
    }

    /**
     * generate apk with V1 signature
     */
    private void generateV1ChannelApk() {
        if (!V1ChannelUtil.containV1Signature(baseApk)) {
            throw new GradleException("apk not signed by v1 , please check your signingConfig , if not have v1 signature , you can't install Apk below 7.0")
        }
        for (int i = 0; i < channelList.size(); i++) {
            String channelApkName = initChannelApkName(i)
            File destApk = new File(outputDir, channelApkName)
            FileUtil.copyTo(baseApk, destApk)
            String channel = channelList.get(i)
            V1ChannelUtil.addChannelByV1(destApk, channel)
            if (!channelExtension.fastMode) {
                //校验渠道信息
                if (V1ChannelUtil.verifyChannelByV1(destApk, channel)) {
                    println "------channel plugin: v1 channel verify success--------"
                } else {
                    throw new GradleException("------channel plugin: v1 channel verify fail--------")
                }

                //校验签名信息
                if (V1ChannelUtil.verifySignatureByV1(destApk)) {
                    println "------channel plugin: v1 signature verify success--------"
                } else {
                    throw new GradleException("------channel plugin: v1 signature verify fail--------")
                }
            }
        }

    }

    /**
     * generate apk with V2 signature
     */
    private void generateV2ChannelApk() {
        for (int i = 0; i < channelList.size(); i++) {
            String channelApkName = initChannelApkName()
            File destApk = new File(outputDir, channelApkName)
            FileUtil.copyTo(baseApk, destApk)
            V2ChannelUtil.addChannelByV2()
            if (!channelExtension.fastMode) {
                //校验渠道信息
                if (V2ChannelUtil.verifyChannelByV2(destApk)) {
                    println "------channel plugin: v2 channel verify success--------"
                } else {
                    throw new GradleException("------channel plugin: v2 channel verify fail--------")
                }
                //校验签名信息
                if (V2ChannelUtil.verifySignatureByV2(destApk)) {
                    println "------channel plugin: v2 signature verify success--------"
                } else {
                    throw new GradleException("------channel plugin: v2 signature verify fail--------")
                }
            }
        }
    }

    /**
     * generate apk with both V1/V2 signature
     */
    private void generateV1V2ChannelApk() {

    }

    /**
     * 获取渠道包名称
     * 读取gradle 中的versionCode 和versionName
     * 渠道包名格式默认为 "project + '_' + channel + '_' + debug/release + '_' + version"
     */
    private String initChannelApkName(int index) {
        def extAndroid = project.extensions.getByName("android")
        def propDefaultConfig = extAndroid.properties.get('defaultConfig')
        def propVersionCode = propDefaultConfig.properties.get('versionCode')
        def propVersionName = propDefaultConfig.properties.get('versionName')

        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append(ChannelConfig.APP_NAME)
        stringBuilder.append("_")
        stringBuilder.append("v" + propVersionName.toString())
        stringBuilder.append("_")
        stringBuilder.append(variant.dirName)
        stringBuilder.append("_")
        stringBuilder.append(propVersionCode)
        stringBuilder.append("_")
        stringBuilder.append(index)
        stringBuilder.append("_")
        stringBuilder.append(channelList.get(index))
        stringBuilder.append("_")
        stringBuilder.append("sign")

        println "-------channel plugin:${stringBuilder}---------"
        return stringBuilder.toString()

    }

}

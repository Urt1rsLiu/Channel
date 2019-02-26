package com.royole.plugin

import brut.androlib.ApkDecoder
import com.royole.plugin.util.ApkToolUtil

/**
 * @author Hongzhi Liu
 * @date 2019/2/25
 */
class Main {

    public static void main(String[] args) {
        ApkDecoder apkDecoder = new ApkDecoder()
        File file = new File("D:\\git_repository\\Channel\\apk\\rydrawing_v2.1.6_22_1_xiaomi{}_sign.apk")
        File outputDir = new File("D:\\git_repository\\Channel\\output")
        ApkToolUtil.decodeApk(file, outputDir)
    }
}

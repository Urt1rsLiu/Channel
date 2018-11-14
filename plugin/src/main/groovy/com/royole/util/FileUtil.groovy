package com.royole.util

/**
 * @author Hongzhi Liu Liu13407134075@gmail.com
 * @date 2018/10/8 17:03 
 */
class FileUtil {
    static void copyTo(File src, File dest){
        def input = src.newInputStream()
        def output = dest.newOutputStream()
        output << input
        input.close()
        output.close()
    }
}

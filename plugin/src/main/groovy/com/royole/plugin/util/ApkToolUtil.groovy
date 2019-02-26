package com.royole.plugin.util

import brut.androlib.AndrolibException
import brut.androlib.ApkDecoder
import brut.androlib.err.CantFindFrameworkResException
import brut.androlib.err.InFileNotFoundException
import brut.androlib.err.OutDirExistsException
import brut.directory.DirectoryException


/**
 * @author Hongzhi Liu
 * @date 2019/2/22
 */
class ApkToolUtil {

    public static final String DECODED_APK_DIR = "apk_decoded_temp"

    public static final String TEMP_UNSIGNED_APK_NAME = "temp_unsigned.apk"

    //ApkTool反编译时的缓存目录
    public static final String TEMP_FRAME_PATH = "tempFrame"



    /**
     * decompile apk file to declared directory
     *
     * @param baseApk   source apk file
     * @param outputDir directory where output the decoded apk directory
     * @return the decoded apk directory
     */
    static File decodeApk(File baseApk, File outputDir) throws IOException {
        File decodedApkDir = new File(outputDir, DECODED_APK_DIR)
        File tempFrameWorkDir = new File(outputDir, TEMP_FRAME_PATH)
        //if decoded apk directory already exist, then delete it
//        if (decodedApkDir.exists() && decodedApkDir.isDirectory()) {
//            println "delete decoded apk dir"
//            if (!decodedApkDir.delete()) {
//                throw new IOException("output dir: " + decodedApkDir + "has exist and cant be deleted")
//            }
//        }

        println "decodedApkDir.exists()" + decodedApkDir.exists()


        ApkDecoder apkDecoder = new ApkDecoder()
        try {
            apkDecoder.setApkFile(baseApk)
            apkDecoder.setOutDir(decodedApkDir)
            apkDecoder.setForceDelete(true)
            apkDecoder.setFrameworkDir(tempFrameWorkDir.getAbsolutePath())
            apkDecoder.decode()
        } catch (OutDirExistsException var22) {
            System.out.println("Destination directory (" + decodedApkDir.getAbsolutePath() + ") already exists. Use -f switch if you want to overwrite it.")
            System.exit(1)
        } catch (InFileNotFoundException var23) {
            System.out.println("Input file (" + baseApk.getName() + ") was not found or was not readable.")
            System.exit(1)
        } catch (CantFindFrameworkResException var24) {
            System.out.println("Can't find framework resources for package of id: " + String.valueOf(var24.getPkgId()) + ". You must install proper framework files, see project website for more info.")
            System.exit(1)
        } catch (IOException var25) {
            System.out.println("Could not modify file. Please ensure you have permission.");
            System.exit(1)
        } catch (DirectoryException var26) {
            System.out.println("Could not modify internal dex files. Please ensure you have permission.");
            System.exit(1)
        } catch (AndrolibException e) {
            System.out.println("android lib exception.")
            System.exit(1)
        } finally {
            try {
                apkDecoder.close()
            } catch (IOException var21) {
            }
        }
        return decodedApkDir
    }

    /**
     * @param decodedApkDir source decoded apk directory
     * @param outputDir the directory which result file with ".apk" suffix exists in
     * @return built file with ".apk" suffix.This unsigned apk file needs to sign again.
     */
//    static File buildUnsignedApk(File decodedApkDir, File outputDir) {
//        File outputApk = new File(outputDir, TEMP_UNSIGNED_APK_NAME)
//        ApkOptions apkOptions = new ApkOptions()
//        try {
//            new Androlib(apkOptions).build(decodedApkDir,outputApk)
//        } catch (BrutException e) {
//            System.err.println(e.getMessage())
//            System.err.println("--------channel plugin: cant build " + decodedApkDir.getAbsolutePath() + "to " + outputDir.getAbsolutePath())
//            System.exit(1)
//        }
//        return outputApk
//    }
}

package com.royole.util;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.royole.constant.ZipConstants;
import com.royole.data.Pair;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Hongzhi.Liu 2014302580200@whu.edu.cn
 * @date 2018/10/11
 */
public class V2ChannelUtil {

    /**
     * add channel Information to apk in v2 signature mode
     * V2 signature block locates on the fore of Central Directory block
     */
    public static void addChannelByV2() {

    }

    /**
     * 校验v2 渠道
     *
     * @return
     */
    public static boolean verifyChannelByV2(File apkFile) throws ApkFormatException, NoSuchAlgorithmException, IOException {
        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(apkFile);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        boolean verified = result.isVerified();
        System.out.println("verified : " + verified);
        if (verified) {
            System.out.println("Verified using v1 scheme (jar signature scheme v1): " + result.isVerifiedUsingV1Scheme());
            System.out.println("Verified using v2 scheme (apk Signature scheme v2): " + result.isVerifiedUsingV2Scheme());
            if (result.isVerifiedUsingV2Scheme()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验v2 签名
     *
     * @return
     */
    public static boolean verifySignatureByV2(File apkFile) {
        return false;
    }



    /**
     * get signing block of the apk file with v2 signature
     */
    public static ByteBuffer getApkSigningBlock(File channelFile) throws Exception {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }
        RandomAccessFile apk = null;
        try {
            apk = new RandomAccessFile(channelFile, "r");
            //1.find the EOCD
            Pair<ByteBuffer, Long> eocdAndOffsetInFile = VerifyUtil.getEocd(apk);
            ByteBuffer eocd = eocdAndOffsetInFile.getFirst();
            long eocdOffset = eocdAndOffsetInFile.getSecond();
            if (ZipUtil.isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
                throw new Exception("ZIP64 APK not supported");
            }
            //2.find the offset of central directory block by search EoCD block (Apk Signing block)
            long centralDirOffset = VerifyUtil.getCentralDirOffset(eocd, eocdOffset);
            //3. find the apk V2 signature block
            Pair<ByteBuffer, Long> apkSignatureBlock = VerifyUtil.getApkSigningBlock(apk, centralDirOffset);
            return apkSignatureBlock.getFirst();
        } finally {
            if (apk != null) {
                apk.close();
            }
        }
    }

    /**
     * 根据V2 Signing block获取其中的所有id-value 存储到map中
     *
     * @param apkSigningBlock
     * @return
     * @throws Exception
     */
    public static Map<Integer, ByteBuffer> getAllIdValue(ByteBuffer apkSigningBlock) throws Exception {
        VerifyUtil.checkByteOrderLittleEndian(apkSigningBlock);
        //从第9个字节截取到倒数第25个字节，即id-value block
        ByteBuffer pairs = VerifyUtil.slice(apkSigningBlock, 8, apkSigningBlock.capacity() - 24);
        Map<Integer, ByteBuffer> idValues = new LinkedHashMap<>();
        int entryCount = 0;
        while (pairs.hasRemaining()) {
            entryCount++;
            if (pairs.remaining() < 0) {
                throw new Exception("Insufficient data to read size of APK Signing Block entry #" + entryCount);
            }
            //读取8个字节，内容为当前id-value块的大小
            long len = pairs.getLong();
            if (pairs.remaining() < len) {
                throw new Exception("APK Signing Block entry #" + entryCount + " size out of range: " + len
                        + ", available: " + pairs.remaining());
            }
            int nextEntryPosition = (int) len + pairs.position();
            //读取4个字节的id
            int id = pairs.getInt();
            idValues.put(id, VerifyUtil.getByteBuffer(pairs, (int) len - 4));
            //判断id是否为V2签名信息的id
            if (ZipConstants.V2_SIGN_BLOCK_ID == id){
                System.out.println("find V2 signature block Id : " + ZipConstants.V2_SIGN_BLOCK_ID);
            }
            pairs.position(nextEntryPosition);
        }
        if (idValues.isEmpty()){
            throw new Exception("not have Id-Value Pair in APK Signing Block entry #" + entryCount);
        }
        return idValues;
    }


}

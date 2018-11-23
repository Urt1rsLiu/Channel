package com.royole.util;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.royole.IdValueWriter.IdValueWriter;
import com.royole.constant.ChannelConfig;
import com.royole.constant.ZipConstants;
import com.royole.data.ApkSectionInfo;
import com.royole.data.Pair;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    public static void addChannelByV2(ApkSectionInfo apkSectionInfo, File destApk, String channel) throws Exception {
        if (destApk == null || channel == null || channel.length() <= 0) {
            throw new RuntimeException("addChannelByV2 , param invalid, channel = " + channel + " , destApk = " + destApk);
        }
        if (!destApk.exists() || !destApk.isFile() || destApk.length() <= 0) {
            throw new RuntimeException("addChannelByV2 , destApk invalid");
        }

        byte[] buffer = channel.getBytes(ChannelConfig.CONTENT_CHARSET);
        ByteBuffer channelByteBuffer = ByteBuffer.wrap(buffer);
        channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        IdValueWriter.addIdValue(apkSectionInfo, destApk, ZipConstants.CHANNEL_BLOCK_ID, channelByteBuffer);
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
    public static boolean verifySignatureByV2(File apkFile) throws Exception {
        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(apkFile);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        boolean verified = result.isVerified();
        System.out.println("verified : " + verified);
        if (verified) {
            System.out.println("Verified using v1 scheme (JAR signing): " + result.isVerifiedUsingV1Scheme());
            System.out.println("Verified using v2 scheme (APK Signature Scheme v2): " + result.isVerifiedUsingV2Scheme());
            if (result.isVerifiedUsingV2Scheme()) {
                return true;
            }
        }
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
            idValues.put(id, ByteBufferUtil.getByteBuffer(pairs, (int) len - 4));
            //判断id是否为V2签名信息的id
            if (ZipConstants.V2_SIGN_BLOCK_ID == id) {
                System.out.println("find V2 signature block Id : " + ZipConstants.V2_SIGN_BLOCK_ID);
            }
            pairs.position(nextEntryPosition);
        }
        if (idValues.isEmpty()) {
            throw new Exception("not have Id-Value Pair in APK Signing Block entry #" + entryCount);
        }
        return idValues;
    }

    public static ByteBuffer generateApkSigningBlock(Map<Integer, ByteBuffer> idValueMap) {
        if (idValueMap == null || idValueMap.isEmpty()) {
            throw new RuntimeException("getNewApkV2SchemeBlock , id value pair is empty");
        }
        // FORMAT:
        // uint64:  size (excluding this field)
        // repeated ID-value pairs:
        //     uint64:           size (excluding this field)
        //     uint32:           ID
        //     (size - 4) bytes: value
        // uint64:  size (same as the one above)
        // uint128: magic
        long totalLength = 16 + 8;
        for (Map.Entry<Integer, ByteBuffer> entry : idValueMap.entrySet()) {
            ByteBuffer byteBuffer = entry.getValue();
            totalLength = totalLength + 8 + 4 + byteBuffer.remaining();
        }
        ByteBuffer newV2SignatureBlock = ByteBuffer.allocate((int) (totalLength + 8));
        newV2SignatureBlock.order(ByteOrder.LITTLE_ENDIAN);
        //1.write size (excluding this field)
        newV2SignatureBlock.putLong(totalLength);

        for (Map.Entry<Integer, ByteBuffer> entry : idValueMap.entrySet()) {
            ByteBuffer byteBuffer = entry.getValue();
            //2.1 write length of id-value
            newV2SignatureBlock.putLong(byteBuffer.remaining() + 4);
            //2.2 write id
            newV2SignatureBlock.putInt(entry.getKey());
            //2.3 write value
            newV2SignatureBlock.put(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining());
        }
        //3.write size (same as the one above)
        newV2SignatureBlock.putLong(totalLength);
        //4. write magic
        newV2SignatureBlock.putLong(ZipConstants.V2_MAGIC_LOW);
        newV2SignatureBlock.putLong(ZipConstants.V2_MAGIC_HEIGHT);
        if (newV2SignatureBlock.remaining() > 0) {
            throw new RuntimeException("generateNewApkV2SchemeBlock error");
        }
        newV2SignatureBlock.flip();
        return newV2SignatureBlock;
    }

}

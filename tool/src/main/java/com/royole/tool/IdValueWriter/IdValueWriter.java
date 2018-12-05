package com.royole.tool.IdValueWriter;

import com.android.apksig.internal.zip.ZipUtils;
import com.royole.tool.constant.ZipConstants;
import com.royole.tool.data.ApkSectionInfo;
import com.royole.tool.util.V2ChannelUtil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hongzhi.Liu
 * @date 2018/11/14
 */
public class IdValueWriter {
    /**
     * add id-value to apk
     *
     * @param apkSectionInfo
     * @param destApk
     * @param id
     * @param valueBuffer
     * @throws IOException
     */
    public static void addIdValue(ApkSectionInfo apkSectionInfo, File destApk, int id, ByteBuffer valueBuffer) throws Exception {
        if (id == ZipConstants.V2_SIGN_BLOCK_ID) {
            throw new RuntimeException("addIdValue , id can not is " + String.valueOf(ZipConstants.V2_SIGN_BLOCK_ID) + " , v2 signature block use it");
        }

        Map<Integer, ByteBuffer> idValueMap = new LinkedHashMap<>();
        idValueMap.put(id, valueBuffer);
        addIdValueByteBufferMap(apkSectionInfo, destApk, idValueMap);
    }

    public static void removeIdValue(ApkSectionInfo apkSectionInfo, File destApk, List<Integer> idList) throws Exception {
        if (apkSectionInfo == null || destApk == null || !destApk.isFile() || !destApk.exists() || idList == null || idList.isEmpty()) {
            return;
        }
        Map<Integer, ByteBuffer> existentIdValueMap = V2ChannelUtil.getAllIdValue(apkSectionInfo.apkV2SignBlock.getFirst());
        int existentIdValueSize = existentIdValueMap.size();
        if (!existentIdValueMap.containsKey(ZipConstants.V2_SIGN_BLOCK_ID)) {
            throw new Exception("No APK V2 Signature Scheme block in APK Signing Block");
        }
        System.out.println("removeIdValue , existed IdValueMap = " + existentIdValueMap);

        for (Integer id : idList) {
            if (id.intValue() != ZipConstants.V2_SIGN_BLOCK_ID) {
                existentIdValueMap.remove(id);
            }
        }
        int remainderIdValueSize = existentIdValueMap.size();
        if (existentIdValueSize == remainderIdValueSize) {
            System.out.println("removeIdValue , No idValue was deleted");
        } else {
            System.out.println("removeIdValue , final IdValueMap = " + existentIdValueMap);
            ByteBuffer newApkSigningBlock = V2ChannelUtil.generateApkSigningBlock(existentIdValueMap);
            System.out.println("removeIdValue , oldApkSigningBlock size = " + apkSectionInfo.apkV2SignBlock.getFirst().remaining() + " , newApkSigningBlock size = " + newApkSigningBlock.remaining());

            ByteBuffer centralDir = apkSectionInfo.centralDir.getFirst();
            ByteBuffer eocd = apkSectionInfo.eocd.getFirst();
            long centralDirOffset = apkSectionInfo.centralDir.getSecond();
            int apkChangeSize = newApkSigningBlock.remaining() - apkSectionInfo.apkV2SignBlock.getFirst().remaining();
            //update the offset of centralDir
            ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset + apkChangeSize);//修改了EOCD中保存的中央目录偏移量

            long apkLength = apkSectionInfo.apkSize + apkChangeSize;
            RandomAccessFile fIn = null;
            try {
                fIn = new RandomAccessFile(destApk, "rw");

                fIn.seek(apkSectionInfo.apkV2SignBlock.getSecond());


                //1. write new apk v2 scheme block
                fIn.write(newApkSigningBlock.array(), newApkSigningBlock.arrayOffset() + newApkSigningBlock.position(), newApkSigningBlock.remaining());
                //2. write central dir block
                fIn.write(centralDir.array(), centralDir.arrayOffset() + centralDir.position(), centralDir.remaining());
                //3. write eocd block
                fIn.write(eocd.array(), eocd.arrayOffset() + eocd.position(), eocd.remaining());
                //4. modify the length of apk file
                if (fIn.getFilePointer() != apkLength) {
                    throw new RuntimeException("after removeIdValue , file size wrong , FilePointer : " + fIn.getFilePointer() + ", apkLength : " + apkLength);
                }
                fIn.setLength(apkLength);
                System.out.println("removeIdValue , after remove channel , apk is " + destApk.getAbsolutePath() + " , length = " + destApk.length());
            } finally {
                //恢复EOCD中保存的中央目录偏移量，满足基础包的APK结构
                ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset);
                if (fIn != null) {
                    fIn.close();
                }
            }
        }
    }

    /**
     * add id-value pairs to apk
     *
     * @param apkSectionInfo
     * @param destApk
     * @param idValueMap
     * @throws IOException
     */
    public static void addIdValueByteBufferMap(ApkSectionInfo apkSectionInfo, File destApk, Map<Integer, ByteBuffer> idValueMap) throws Exception {
        if (idValueMap == null || idValueMap.isEmpty()) {
            throw new RuntimeException("addIdValueByteBufferMap , id value pair is empty");
        }
        //不能和系统V2签名块的ID冲突
        if (idValueMap.containsKey(ZipConstants.V2_SIGN_BLOCK_ID)) {
            idValueMap.remove(ZipConstants.V2_SIGN_BLOCK_ID);
        }
        System.out.println("addIdValueByteBufferMap , new IdValueMap = " + idValueMap);

        Map<Integer, ByteBuffer> existentIdValueMap = V2ChannelUtil.getAllIdValue(apkSectionInfo.apkV2SignBlock.getFirst());
        if (!existentIdValueMap.containsKey(ZipConstants.V2_SIGN_BLOCK_ID)) {
            throw new Exception("No APK V2 Signature Scheme block in APK Signing Block");
        }
        System.out.println("addIdValueByteBufferMap , existed IdValueMap = " + existentIdValueMap);

        existentIdValueMap.putAll(idValueMap);
        System.out.println("addIdValueByteBufferMap , final IdValueMap = " + existentIdValueMap);

        ByteBuffer newApkSigningBlock = V2ChannelUtil.generateApkSigningBlock(existentIdValueMap);
        System.out.println("addIdValueByteBufferMap , oldApkSigningBlock size = " + apkSectionInfo.apkV2SignBlock.getFirst().remaining() + " , newApkSigningBlock size = " + newApkSigningBlock.remaining());

        ByteBuffer centralDir = apkSectionInfo.centralDir.getFirst();
        ByteBuffer eocd = apkSectionInfo.eocd.getFirst();
        long centralDirOffset = apkSectionInfo.centralDir.getSecond();
        int apkChangeSize = newApkSigningBlock.remaining() - apkSectionInfo.apkV2SignBlock.getFirst().remaining();
        //update the offset of centralDir
        ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset + apkChangeSize);//修改了EOCD中保存的中央目录偏移量

        long apkLength = apkSectionInfo.apkSize + apkChangeSize;
        RandomAccessFile fIn = null;
        try {
            fIn = new RandomAccessFile(destApk, "rw");
            fIn.seek(apkSectionInfo.apkV2SignBlock.getSecond());
            //2. write new apk v2 scheme block
            fIn.write(newApkSigningBlock.array(), newApkSigningBlock.arrayOffset() + newApkSigningBlock.position(), newApkSigningBlock.remaining());
            //3. write central dir block
            fIn.write(centralDir.array(), centralDir.arrayOffset() + centralDir.position(), centralDir.remaining());
            //4. write eocd block
            fIn.write(eocd.array(), eocd.arrayOffset() + eocd.position(), eocd.remaining());
            //5. modify the length of apk file
            if (fIn.getFilePointer() != apkLength) {
                throw new RuntimeException("after addIdValueByteBufferMap , file size wrong , FilePointer : " + fIn.getFilePointer() + ", apkLength : " + apkLength);
            }
            fIn.setLength(apkLength);
            System.out.println("addIdValueByteBufferMap , after add channel , new apk is " + destApk.getAbsolutePath() + " , length = " + destApk.length());
        } finally {
            //恢复EOCD中保存的中央目录偏移量，满足基础包的APK结构
            ZipUtils.setZipEocdCentralDirectoryOffset(eocd, centralDirOffset);
            if (fIn != null) {
                fIn.close();
            }
        }
    }


    /**
     * add id-value(byte[]) to apk
     *
     * @param destApk
     * @param id
     * @param buffer  please ensure utf-8 charset
     */
    public static void addIdValue(File srcApk, File destApk, int id, byte[] buffer, boolean lowMemory) throws Exception {
        ApkSectionInfo apkSectionInfo = getApkSectionInfo(srcApk, lowMemory);
        ByteBuffer channelByteBuffer = ByteBuffer.wrap(buffer);
        //apk中所有字节都是小端模式
        channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        addIdValue(apkSectionInfo, destApk, id, channelByteBuffer);
    }


    /**
     * add id-value(byte[]) to apk
     *
     * @param apkFile
     * @param id
     * @param buffer
     * @throws IOException
     */
    public static void addIdValue(File apkFile, int id, byte[] buffer, boolean lowMemory) throws Exception {
        addIdValue(apkFile, apkFile, id, buffer, lowMemory);
    }


    /**
     * add id-value(byte[]) pairs to apk
     *
     * @param srcApk
     * @param destApk
     * @param idValueByteMap
     * @throws IOException
     */
    public static void addIdValueByteMap(File srcApk, File destApk, Map<Integer, byte[]> idValueByteMap, boolean lowMemory) throws Exception {
        if (idValueByteMap == null || idValueByteMap.isEmpty()) {
            throw new RuntimeException("addIdValueByteMap , idValueByteMap is empty");
        }
        ApkSectionInfo apkSectionInfo = getApkSectionInfo(srcApk, lowMemory);
        // keep order
        Map<Integer, ByteBuffer> idValues = new LinkedHashMap<Integer, ByteBuffer>();
        for (Integer integer : idValueByteMap.keySet()) {
            ByteBuffer channelByteBuffer = ByteBuffer.wrap(idValueByteMap.get(integer));
            //apk中所有字节都是小端模式
            channelByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            idValues.put(integer, channelByteBuffer);
        }

        addIdValueByteBufferMap(apkSectionInfo, destApk, idValues);
    }

    /**
     * add id-value(byte[]) pairs to apk
     *
     * @param apkFile
     * @param idValueByteMap
     * @throws IOException
     */
    public static void addIdValueByteMap(File apkFile, Map<Integer, byte[]> idValueByteMap, boolean lowMemory) throws Exception {
        addIdValueByteMap(apkFile, apkFile, idValueByteMap, lowMemory);
    }

    public static void removeIdValue(File apk, int id) {

    }

    public static ApkSectionInfo getApkSectionInfo(File baseApk, boolean lowMemory) throws Exception {
        if (baseApk == null || !baseApk.exists() || !baseApk.isFile()) {
            System.out.println("----------error: apksectioninfo is null-------");
            return null;
        }
        return new ApkSectionInfo(baseApk);
    }
}

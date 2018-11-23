package com.royole.data;

import com.royole.util.VerifyUtil;
import com.royole.util.ZipUtil;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * @author Hongzhi.Liu
 * @date 2018/11/14
 * section info in apk with v2 signature
 */

public class ApkSectionInfo {
    /**
     * Low memory mode is only applied in building of v2 signature channel apk.
     * Read eocd, signning block, central directory from apk, while excluding the main block in the low memory mode.
     */
    public long apkSize;
    public Pair<ByteBuffer, Long> contentEntry;
    public Pair<ByteBuffer, Long> apkV2SignBlock;
    public Pair<ByteBuffer, Long> centralDir;
    public Pair<ByteBuffer, Long> eocd;

    private ApkSectionInfo() {
    }

    public ApkSectionInfo(File baseApk) throws Exception {
        RandomAccessFile apk = null;
        try {
            apk = new RandomAccessFile(baseApk, "r");
            //1. find the EoCD and offset
            Pair<ByteBuffer, Long> eocdAndOffset = VerifyUtil.getEocd(apk);
            ByteBuffer eocd = eocdAndOffset.getFirst();
            Long eocdOffset = eocdAndOffset.getSecond();
            if (ZipUtil.isZip64EndOfCentralDirectoryLocatorPresent(apk, eocdOffset)) {
                throw new Exception("Zip 64 not supported");
            }

            //2. find apk V2 signing block,The block immediately precedes the Central Directory.
            long centralDirOffset = VerifyUtil.getCentralDirOffset(eocd, eocdOffset);
            Pair<ByteBuffer, Long> apkV2SignBlock = VerifyUtil.getApkSigningBlock(apk, centralDirOffset);

            //3. find central directory block
            Pair<ByteBuffer, Long> centralDirAndOffset = VerifyUtil.getCentralDir(apk, centralDirOffset, (int) (eocdOffset - centralDirOffset));

            this.eocd = eocdAndOffset;
            this.apkV2SignBlock = apkV2SignBlock;
            this.centralDir = centralDirAndOffset;
            this.apkSize = baseApk.length();

            //4. find the contentEntry, signing block is locate between content entry block and central directory block
            this.contentEntry = VerifyUtil.getContentEntryBlock(apk, (int) apkV2SignBlock.getSecond().longValue());

            this.checkParamters();
            System.out.println("baseApk : " + baseApk.getAbsolutePath() + "\nApkSectionInfo = " + this);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("cant read eocd from apk file");
        } finally {
            if (apk != null) {
                apk.close();
            }
        }


    }


    public void checkParamters() throws Exception {
        boolean isInvalidSectionInfo = apkV2SignBlock == null || centralDir == null || eocd == null;
        if (isInvalidSectionInfo) {
            throw new RuntimeException("ApkSectionInfo paramters is not valid : " + toString());
        }

        boolean result = (apkV2SignBlock.getFirst().remaining() + apkV2SignBlock.getSecond() == centralDir.getSecond())
                && (centralDir.getFirst().remaining() + centralDir.getSecond() == eocd.getSecond())
                && (eocd.getFirst().remaining() + eocd.getSecond() == apkSize);

        if (!result) {
            throw new RuntimeException("ApkSectionInfo paramters is not valid : " + toString());
        }
        checkEocdCentralDirOffset();
    }


    public void rewind() {
        //reset position of reading starts
        if (contentEntry != null) {
            contentEntry.getFirst().rewind();
        }
        if (apkV2SignBlock != null) {
            apkV2SignBlock.getFirst().rewind();
        }
        if (centralDir != null) {
            centralDir.getFirst().rewind();
        }
        if (eocd != null) {
            eocd.getFirst().rewind();
        }
    }

    public void checkEocdCentralDirOffset() throws Exception {
        //通过eocd找到中央目录的偏移量
        long centralDirOffset = VerifyUtil.getCentralDirOffset(eocd.getFirst(), eocd.getSecond());
        if (centralDirOffset != centralDir.getSecond()) {
            throw new RuntimeException("CentralDirOffset mismatch , EocdCentralDirOffset : " + centralDirOffset + ", centralDirOffset : " + centralDir.getSecond());
        }
    }

    @Override
    public String toString() {
        return "apkSize : " + apkSize + "\n contentEntry : " + contentEntry + "\n apkV2SignBlock : " + apkV2SignBlock + "\n centralDir : " + centralDir + "\n eocd : " + eocd;
    }

}

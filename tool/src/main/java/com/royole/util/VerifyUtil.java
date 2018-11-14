package com.royole.util;

import com.royole.constant.ZipConstants;
import com.royole.data.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Hongzhi Liu  2014302580200@whu.edu.cn
 * @date 2018/10/9 10:16
 */

/**
 * utils class for verifying apk file after generating channel apk
 */
class VerifyUtil {

    /**
     * get EOCD record block and its offset
     *
     * @param apk
     * @return
     * @throws IOException
     */
    public static Pair<ByteBuffer, Long> getEocd(RandomAccessFile apk) throws Exception {
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = ZipUtil.findZipEndOfCentralDirectoryRecord(apk);
        if (eocdAndOffsetInFile == null) {
            throw new Exception("Not an APK file: ZIP End of Central Directory record not found");
        }
        return eocdAndOffsetInFile;
    }

    /**
     * get v2 signature block and its offset by parameters
     *
     * @param apk
     * @param centralDirOffset
     * @throws Exception
     */
    public static Pair<ByteBuffer, Long> getApkSigningBlock(RandomAccessFile apk, Long centralDirOffset) throws Exception {
        // FORMAT:
        // OFFSET       BLOCK SIZE              DESCRIPTION
        // * @0         8 bytes                 size in bytes (size excluding this field)
        // * @8         n bytes payload
        // * @8+n       8 bytes                 size in bytes (same value as the one above,but the size include this field)
        // * @16+n      16 bytes                magic

        if (centralDirOffset < ZipConstants.V2_SIGN_BLOCK_MIN_SIZE) {
            throw new Exception("APK too small for APK Signing Block. ZIP Central Directory offset: " + centralDirOffset);
        }
        // Read the magic and offset in file from the footer section of the block:
        // * 8 bytes:   size of block
        // * 16 bytes: magic
        //分配24个字节的ByteBuffer读取magic block和底部的size block
        ByteBuffer footer = ByteBuffer.allocate(24);
        footer.order(ByteOrder.LITTLE_ENDIAN);
        apk.seek(centralDirOffset - footer.capacity());
        apk.readFully(footer.array(), footer.arrayOffset(), footer.capacity());
        //读取magic block 并对比，由于getLong()一次只能读8个字节，而magic block占16个字节，所以分两次读取
        if ((footer.getLong(8) != ZipConstants.V2_MAGIC_LOW) || (footer.getLong(16) != ZipConstants.V2_MAGIC_HEIGHT)) {
            throw new Exception("No APK Signing Block before ZIP Central Directory");
        }

        //从v2签名块中的size block读取该签名块(不包含size block)的大小
        long apkSigningBlockSizeInFooter = footer.getLong(0);

        //由于v2签名块中，magic占16个字节，再加上footer中的size block的8字节，所以至少占24个字节
        //至于最大范围为什么不是Long.MaxValue-8 仍未理解
        if ((apkSigningBlockSizeInFooter < 24) || (apkSigningBlockSizeInFooter) > Integer.MAX_VALUE - 8) {
            throw new Exception("APK Signing Block size out of range: " + apkSigningBlockSizeInFooter);
        }
        int totalSize = (int) apkSigningBlockSizeInFooter + 8;
        //由于V2 Signing block的下面紧跟着的就是central directory
        //所以central directory的offset减去V2 Signing block的大小就是V2 Signing block的offset
        Long apkSigningBlockOffset = centralDirOffset - totalSize;
        if (apkSigningBlockOffset < 0) {
            throw new Exception("APK Signing Block offset out of range: " + apkSigningBlockOffset);
        }
        ByteBuffer apkSigningBlock = ByteBuffer.allocate(totalSize);
        apkSigningBlock.order(ByteOrder.LITTLE_ENDIAN);
        apk.seek(apkSigningBlockOffset);
        apk.readFully(apkSigningBlock.array(), apkSigningBlock.arrayOffset(), apkSigningBlock.capacity());
        //最后检查V2 Signing block的头部和尾部的size block的值(即Signing block的大小)是否相同
        long apkSigningBlockSizeInHeader = apkSigningBlock.getLong(0);
        if (apkSigningBlockSizeInHeader != apkSigningBlockSizeInFooter) {
            throw new Exception("APK Signing Block sizes in header and footer do not match: "
                    + apkSigningBlockSizeInHeader + " vs " + apkSigningBlockSizeInFooter);
        }
        return Pair.create(apkSigningBlock, apkSigningBlockOffset);
    }


    /**
     * check byte order (LittleEndian or BigEndian)
     */
    public static void checkByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    /**
     * 从eocd中获取central directory的offset
     *
     * @param eocd
     * @param eocdOffset
     * @return
     */
    public static long getCentralDirOffset(ByteBuffer eocd, long eocdOffset) throws Exception {
        // Look up the offset of ZIP Central Directory.
        long centralDirOffset = ZipUtil.getZipEocdCentralDirectoryOffset(eocd);
        if (centralDirOffset >= eocdOffset) {
            throw new Exception("ZIP Central Directory offset out of range: " + centralDirOffset
                    + ". ZIP End of Central Directory offset: " + eocdOffset);
        }
        //在eocd中获得central directory的size大小并作校验
        long centralDirSize = ZipUtil.getZipEocdCentralDirectorySizeBytes(eocd);
        if (centralDirOffset + centralDirSize != eocdOffset) {
            throw new Exception("ZIP Central Directory is not immediately followed by End of Central"
                    + " Directory");
        }
        return centralDirOffset;
    }

    /**
     * 读取ByteBuffer指定位置的子ByteBuffer
     */
    public static ByteBuffer slice(ByteBuffer buffer, int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("start: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("end < start: " + end + " < " + start);
        }
        int capacity = buffer.capacity();
        if (end > buffer.capacity()) {
            throw new IllegalArgumentException("end > capacity: " + end + " > " + capacity);
        }
        int originalLimit = buffer.limit();
        int originalPosition = buffer.position();
        try {
            buffer.position(0);
            buffer.limit(end);
            buffer.position(start);
            ByteBuffer result = buffer.slice();
            result.order(buffer.order());
            return result;
        } finally {
            buffer.position(0);
            buffer.limit(originalLimit);
            buffer.position(originalPosition);
        }
    }

    /**
     * 从ByteBuffer的当前Position往后读取指定大小
     * @param source
     * @param size
     * @return
     */
    public static ByteBuffer getByteBuffer(ByteBuffer source,int size) throws Exception{
        if (size < 0){
            throw new IllegalArgumentException("size: " + size);
        }
        int oldLimit = source.limit();
        int position = source.position();
        int limit = position + size;
        if (limit > oldLimit){
            throw new BufferUnderflowException();
        }
        source.limit(limit);
        try {
            ByteBuffer result = source.slice();
            result.order(source.order());
            source.position(limit);
            return result;
        }finally {
            source.limit(oldLimit);
        }

    }
}

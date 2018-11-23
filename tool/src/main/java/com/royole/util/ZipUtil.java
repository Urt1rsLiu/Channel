package com.royole.util;


import com.royole.constant.ZipConstants;
import com.royole.data.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Hongzhi Liu
 * @date 2018/10/9 10:17
 */
public class ZipUtil {

    /**
     * Returns true if the provided zip file contains a ZIP64 End of Central Directory Locator
     *
     * @return
     */
    public static boolean isZip64EndOfCentralDirectoryLocatorPresent(RandomAccessFile zip, long zipEndOfCentralDirectoryPosition) throws Exception {
        //ZIP64 End of Central Directory Locator precedes the ZIP End of Central Directory Record.
        long locatorPosition = zipEndOfCentralDirectoryPosition - ZipConstants.ZIP64_EOCD_LOCATOR_SIZE;
        if (locatorPosition < 0) {
            return false;
        }

        zip.seek(locatorPosition);
        // RandomAccessFile.readInt assumes big-endian byte order, but ZIP format uses little-endian
        return zip.readInt() == ZipConstants.ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER;
    }

    /**
     * Returns contents of the ZIP End of Central Directory record and the record's offset in the
     * file.The record's offset will be null if the file doesn't contain EoCD.
     *
     * @param apk
     * @throws IOException
     */
    public static Pair<ByteBuffer, Long> findZipEndOfCentralDirectoryRecord(RandomAccessFile apk) throws IOException {
        long fileSize = apk.length();
        if (fileSize < ZipConstants.ZIP_EOCD_REC_MIN_SIZE) {
            //no space for EoCD record,it may be a trick!
            return null;
        }

        //according to research, 99.99% of APKs have a zero-length comment field in the EoCD record
        //so try to assume that the EoCD record offset is known and avoid unnecessarily reading more data
        //it will be faster
        Pair<ByteBuffer, Long> result = findZipEndOfCentralDirectoryRecord(apk, 0);
        if (result != null) {
            return result;
        }

        //if the apk has non-empty comment in the EoCD record,then we expand the search
        return findZipEndOfCentralDirectoryRecord(apk, ZipConstants.BIT_16_MAX_VALUE);

    }

    /**
     * @param apk            The apk file need to find EoCD record
     * @param maxCommentSize The max accepted size of EoCD comment field.The permitted value is from 0 to 65535 inclusive.
     *                       The smaller the value, the faster this method locates the record, provided its comment field
     *                       is no longer than this value.
     *                       Why this value is less than 65535(max value in 2 bytes).because the comment field length is
     *                       represent by 2 bytes in comment length field of EoCD record block
     * @return Returns contents of the ZIP End of Central Directory record and the record's offset in the
     * file.The record's offset will be null if the file doesn't contain EoCD.
     * @throws IOException
     */
    private static Pair<ByteBuffer, Long> findZipEndOfCentralDirectoryRecord(RandomAccessFile apk, int maxCommentSize) throws IOException {
        if (maxCommentSize < 0 || maxCommentSize > ZipConstants.BIT_16_MAX_VALUE) {
            throw new IllegalArgumentException("zip maxCommentSize:" + maxCommentSize);
        }

        long fileSize = apk.length();
        if (fileSize < ZipConstants.ZIP_EOCD_REC_MIN_SIZE) {
            //no space for EoCD record,it may be a trick!
            return null;
        }
        // get the theoretical max comment size
        // ${fileSize - ZipConstants.ZIP_EOCD_REC_MIN_SIZE} represents the total bytes of Local file, central directory and comment field
        maxCommentSize = (int) Math.min(maxCommentSize, fileSize - ZipConstants.ZIP_EOCD_REC_MIN_SIZE);

        ByteBuffer buffer = ByteBuffer.allocate(ZipConstants.ZIP_EOCD_REC_MIN_SIZE + maxCommentSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        //begin reading from the most possible position of EoCD record starts
        long bufOffsetInFile = fileSize - buffer.capacity();
        apk.seek(bufOffsetInFile);
        apk.readFully(buffer.array(), buffer.arrayOffset(), buffer.capacity());
        //get the real offset of EoCd record
        int eocdOffsetInBuf = findZipEndOfCentralDirectoryRecord(buffer);
        if (eocdOffsetInBuf == -1) {
            // No EoCD record found in the buffer
            return null;
        }
        // EoCD found
        buffer.position(eocdOffsetInBuf);
        ByteBuffer eocd = buffer.slice();
        eocd.order(ByteOrder.LITTLE_ENDIAN);
        return Pair.create(eocd, bufOffsetInFile + eocdOffsetInBuf);
    }

    /**
     * get the offset of EoCD record according to the byte buffer that most near to EoCD record
     *
     * @return offset of EoCD in apk file,it returns -1 when the EoCD field not found
     * @throws IOException
     */
    private static int findZipEndOfCentralDirectoryRecord(ByteBuffer eocdContents) throws IOException {
        assertByteOrderLittleEndian(eocdContents);
        int eocdSize = eocdContents.capacity();
        if (eocdSize < ZipConstants.ZIP_EOCD_REC_MIN_SIZE) {
            return -1;
        }
        int maxCommentLength = Math.min(eocdSize - ZipConstants.ZIP_EOCD_REC_MIN_SIZE, ZipConstants.BIT_16_MAX_VALUE);
        int eocdWithEmptyCommentStartPosition = eocdSize - ZipConstants.ZIP_EOCD_REC_MIN_SIZE;
        for (int expectedCommentLength = 0; expectedCommentLength < maxCommentLength; expectedCommentLength++) {
            int eocdStartPos = eocdWithEmptyCommentStartPosition - expectedCommentLength;

        }
        return -1;
    }

    /**
     * 根据EoCD的ByteBuffer获取central directory的offset
     *
     * @param eocd
     * @return
     */
    public static Long getZipEocdCentralDirectoryOffset(ByteBuffer eocd) {
        assertByteOrderLittleEndian(eocd);
        return getUnsignedInt32(eocd, eocd.position() + ZipConstants.ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET);
    }

    /**
     * 根据EoCD的ByteBuffer获取central directory的size
     *
     * @param eocd
     * @return
     */
    public static Long getZipEocdCentralDirectorySizeBytes(ByteBuffer eocd) {
        assertByteOrderLittleEndian(eocd);
        return getUnsignedInt32(eocd, eocd.position() + ZipConstants.ZIP_EOCD_CENTRAL_DIR_SIZE_FIELD_OFFSET);
    }

    public static void assertByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    public static long getUnsignedInt32(ByteBuffer buffer, int offset) {
        return buffer.getShort(offset) & 0xffff;
    }
}

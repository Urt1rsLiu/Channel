package com.royole.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Hongzhi.Liu
 * @date 2018/11/14
 */
public class ByteBufferUtil {
    /**
     * 从ByteBuffer的当前Position往后读取指定大小
     *
     * @param source
     * @param size
     * @return
     */
    public static ByteBuffer getByteBuffer(ByteBuffer source, int size) throws Exception {
        if (size < 0) {
            throw new IllegalArgumentException("size: " + size);
        }
        int oldLimit = source.limit();
        int position = source.position();
        int limit = position + size;
        if (limit > oldLimit) {
            throw new BufferUnderflowException();
        }
        source.limit(limit);
        try {
            ByteBuffer result = source.slice();
            result.order(source.order());
            source.position(limit);
            return result;
        } finally {
            source.limit(oldLimit);
        }

    }

    public static ByteBuffer getByteBuffer(RandomAccessFile raf, long offset, int length) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        raf.seek(offset);
        raf.readFully(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.capacity());
        return byteBuffer;
    }
}

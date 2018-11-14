package com.royole.reader;

/**
 * @author Hongzhi.Liu 2014302580200@whu.edu.cn
 * @date 2018/10/11
 */

import com.royole.constant.ChannelConfig;
import com.royole.util.V2ChannelUtil;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

/**
 * 对V2 签名块的id-value通过id读取value
 */
public class IdValueReader {
    public static String getStringValueById(File channelFile, int id) {
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }
        byte[] buffer = getByteValueById(channelFile, id);
        try {
            if (buffer != null && buffer.length > 0) {
                return new String(buffer, ChannelConfig.CONTENT_CHARSET);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getByteValueById(File channelFile,int id){
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }

        ByteBuffer value = getByteBufferValueById(channelFile, id);
        System.out.println("getByteValueById , id = " + id + " , value = " + value);

        if (value != null) {
            return Arrays.copyOfRange(value.array(), value.arrayOffset() + value.position(), value.arrayOffset() + value.limit());
        }
        return null;
    }

    /**
     * get byte buffer which represents value that maps to id
     * @param channelFile
     * @param id
     * @return
     */
    public static ByteBuffer getByteBufferValueById(File channelFile,int id){
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }
        Map<Integer,ByteBuffer>idValueMap = getAllIdValueMap(channelFile);
        if (idValueMap != null){
            return idValueMap.get(id);
        }
        return null;
    }

    /**
     * 获取v2签名块中的所有id-value序列
     */
    public static Map<Integer,ByteBuffer>getAllIdValueMap(File channelFile){
        if (channelFile == null || !channelFile.exists() || !channelFile.isFile()) {
            return null;
        }
        try {
            ByteBuffer apkV2SigningBlock = V2ChannelUtil.getApkSigningBlock(channelFile);
            return V2ChannelUtil.getAllIdValue(apkV2SigningBlock);
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}

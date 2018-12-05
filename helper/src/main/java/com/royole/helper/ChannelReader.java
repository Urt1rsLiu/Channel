package com.royole.helper;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.royole.tool.constant.ZipConstants;
import com.royole.tool.reader.IdValueReader;
import com.royole.tool.util.V1ChannelUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author Hongzhi.Liu
 * @date 2018/10/11
 */
public class ChannelReader {
    private static final String TAG = "ChannelReaderUtil";
    private static String mChannelCache;


    public static String getChannel(Context context) {
        if (mChannelCache == null) {
            String channel = getChannelByV2(context);
            if (channel == null) {
                channel = getChannelByV1(context);
            }
            mChannelCache = channel;
        }
        return mChannelCache;
    }

    /**
     * if apk use v2 signature , please use this method to get channel info
     *
     * @param context
     * @return
     */
    private static String getChannelByV2(Context context) {
        String apkPath = getApkPath(context);
        File channelFile = new File(apkPath);
        String channel = IdValueReader.getStringValueById(channelFile, ZipConstants.CHANNEL_BLOCK_ID);
        return channel;
    }

    /**
     * if apk only use v1 signature , please use this method to get channel info
     *
     * @param context
     * @return
     */
    private static String getChannelByV1(Context context) {
        String channel;
        String apkPath = getApkPath(context);
        File channelFile = new File(apkPath);
        try {
            channel = V1ChannelUtil.readChannel(channelFile);
            Log.i(TAG, "getChannelByV1 , channel = " + channel);
            return channel;
        }catch (Exception e){
            Log.e(TAG,"APK : " + channelFile.getAbsolutePath() + " not have channel info from Zip Comment");
            return null;
        }
    }


    /**
     * get String value from apk by id in the v2 signature mode
     *
     * @param context
     * @param id
     * @return
     */
    public static String getStringValueById(Context context, int id) {
        String apkPath = getApkPath(context);
        String value = IdValueReader.getStringValueById(new File(apkPath), id);
        Log.i(TAG, "id = " + id + " , value = " + value);
        return value;
    }

    /**
     * get byte[] from apk by id in the v2 signature mode
     *
     * @param context
     * @param id
     * @return
     */
    public static byte[] getByteValueById(Context context, int id) {
        String apkPath = getApkPath(context);
        return IdValueReader.getByteValueById(new File(apkPath), id);
    }

    /**
     * find all Id-Value Pair from Apk in the v2 signature mode
     *
     * @param context
     * @return
     */
    public static Map<Integer, ByteBuffer> getAllIdValueMap(Context context) {
        String apkPath = getApkPath(context);
        return IdValueReader.getAllIdValueMap(new File(apkPath));
    }

    /**
     * 获取已安装的APK路径
     *
     * @param context
     * @return
     */
    private static String getApkPath(Context context) {
        String apkPath = null;
        try {
            ApplicationInfo applicationInfo = context.getApplicationInfo();
            if (applicationInfo == null) {
                return null;
            } else {
                apkPath = applicationInfo.sourceDir;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return apkPath;
    }
}

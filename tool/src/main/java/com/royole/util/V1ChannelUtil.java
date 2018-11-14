package com.royole.util;

import com.android.apksig.ApkVerifier;
import com.android.apksig.apk.ApkFormatException;
import com.royole.constant.ChannelConfig;
import com.royole.constant.ZipConstants;
import com.royole.data.Pair;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Hongzhi Liu
 * @date 2018/10/8 17:13
 */
public class V1ChannelUtil {
    /**
     * add channel Information to apk in v1 signature mode
     * write channel information in comment field, containing channel information,magic field and comment length
     */
    public static void addChannelByV1(File apk, String channel) throws Exception {
        if (apk == null || !apk.exists() || !apk.isFile() || channel == null) {
            throw new Exception("------channel plugin: file: ${apk.absolutePath},channel: ${channel}");
        }
        RandomAccessFile raf = null;
        byte[] comment = channel.getBytes(ChannelConfig.CONTENT_CHARSET);
        Pair<ByteBuffer, Long> eocdAndOffsetInFile = getEocd(apk);
        //if the number of Eocd bytes equals the limit size of EOCD file,then there is no comment in Eocd
        //Normally,the initial apk doesn't contain any channel string and has a min size EoCD
        if (eocdAndOffsetInFile.getFirst().remaining() == ZipConstants.ZIP_EOCD_REC_MIN_SIZE) {
            System.out.println("file :  " + apk.getAbsolutePath() + " , has no comment");
            try {
                raf = new RandomAccessFile(apk, "rw");
                //1. locate comment length field
                raf.seek(apk.length() - ZipConstants.ZIP_EOCD_COMMENT_LENGTH_FIELD_SIZE);
                //2. write comment length field(comment content + comment content length + magic field length)
                writeShort(comment.length + ZipConstants.SHORT_LENGTH + ZipConstants.V1_MAGIC.length, raf);
                //3. write comment content
                raf.write(comment);
                //4. write comment content length
                writeShort(comment.length, raf);
                //5. write magic number
                raf.write(ZipConstants.V1_MAGIC);
            } finally {
                if (raf != null) {
                    raf.close();
                }
            }
        } else {
            // otherwise it may has channel information in comment field.
            // If it has channel information,then delete the apk file
            System.out.println("file :  ${apk.getAbsolutePath()} , has comment");
            // If it has customized V1 magic number,
            if (containV1Magic(apk)) {
                try {
                    String existChannel = readChannel(apk);
                    if (existChannel != null) {
                        apk.delete();
                        throw new Exception("file : " + apk.getAbsolutePath() + " has a channel : " + existChannel + ", only ignore");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * read channel information from apk file in comment field at EoCD(end of central directory record)
     *
     * @param apk
     * @return
     * @throws Exception
     */
    public static String readChannel(File apk) throws Exception {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(apk, "r");
            long index = raf.length();
            byte[] buffer = new byte[ZipConstants.V1_MAGIC.length];
            index -= ZipConstants.V1_MAGIC.length;
            raf.seek(index);
            raf.readFully(buffer);
            // whether magic bytes matched
            if (isV1MagicMatch(buffer)) {
                index -= ZipConstants.SHORT_LENGTH;
                raf.seek(index);
                // read channel length field
                int length = readShort(raf);
                if (length > 0) {
                    index -= length;
                    raf.seek(index);
                    // read channel information bytes
                    byte[] bytesComment = new byte[length];
                    raf.readFully(bytesComment);
                    return new String(bytesComment, ChannelConfig.CONTENT_CHARSET);
                } else {
                    throw new Exception("zip channel info not found");
                }
            } else {
                throw new Exception("zip v1 magic not found");
            }
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }


    /**
     * judge whether has v1 signature
     * to judge it,we could judge whether this apk zip directory has META-INF/MANIFEST.MF and .SF files
     */
    public static boolean containV1Signature(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        } else {
            try {
                JarFile jarFile = new JarFile(file);
                JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
                JarEntry sfEntry = null;
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().matches("META-INF/\\w+\\.SF")) {
                        sfEntry = jarFile.getJarEntry(entry.getName());
                        break;
                    }
                }
                if (manifestEntry != null && sfEntry != null) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * judge whether contain v1 magic int the end of file
     */
    public static boolean containV1Magic(File file) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long index = raf.length();
            byte[] buffer = new byte[ZipConstants.V1_MAGIC.length];
            index -= ZipConstants.V1_MAGIC.length;
            raf.seek(index);
            raf.readFully(buffer);
            return isV1MagicMatch(buffer);
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }

    /**
     * verify channel information in V1 signature mode
     */
    public static boolean verifyChannelByV1(File apkFile,String channel){
        if (channel != null){
            String currentChannel = null;
            try {
                currentChannel = V1ChannelUtil.readChannel(apkFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return channel.equals(currentChannel);
        }
        return false;
    }

    /**
     * verify V1 signature by android native method
     */
    public static boolean verifySignatureByV1(File apkFile) throws ApkFormatException, NoSuchAlgorithmException, IOException {
        ApkVerifier.Builder apkVerifierBuilder = new ApkVerifier.Builder(apkFile);
        ApkVerifier apkVerifier = apkVerifierBuilder.build();
        ApkVerifier.Result result = apkVerifier.verify();
        boolean verified = result.isVerified();
        System.out.println("verified: " + verified);
        if (verified){
            System.out.println("Verified using v1 scheme (jar signing scheme V1): " + result.isVerifiedUsingV1Scheme());
            System.out.println("Verified using v1 scheme (apk signing scheme V2): " + result.isVerifiedUsingV2Scheme());
            if (result.isVerifiedUsingV1Scheme()){
                return true;
            }
        }
        return false;
    }


    /**
     * check v1 magic format
     */
    public static boolean isV1MagicMatch(byte[] content) {
        if (content.length != ZipConstants.V1_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < ZipConstants.V1_MAGIC.length; i++) {
            if (content[i] != ZipConstants.V1_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * get EOCD block and offset from APK file
     *
     * @param apk
     * @return
     * @throws IOException
     */
    static Pair<ByteBuffer, Long> getEocd(File apk) throws Exception{
        if (apk == null || !apk.exists() || !apk.isFile()) {
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(apk, "r");
            //find the EOCD
            Pair<ByteBuffer, Long> eocdAndOffsetInFile = VerifyUtil.getEocd(raf);
            if (ZipUtil.isZip64EndOfCentralDirectoryLocatorPresent(raf, eocdAndOffsetInFile.getSecond())) {
                throw new Exception("ZIP64 APK not supported");
            }
            return eocdAndOffsetInFile;
        } finally {
            if (raf != null) {
                raf.close();
            }
        }

    }

    /**
     * read short data in 2 bytes
     */
    private static short readShort(DataInput input) throws IOException {
        byte[] buf = new byte[ZipConstants.SHORT_LENGTH];
        input.readFully(buf);
        ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort(0);
    }

    /**
     * write int data in 2 bytes
     *
     * @param i
     * @param out
     * @throws IOException
     */
    private static void writeShort(int i, DataOutput out) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(ZipConstants.SHORT_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        bb.putShort((short) i);
        out.write(bb.array());
    }
}

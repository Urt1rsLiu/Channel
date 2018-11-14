package com.royole.constant;

/**
 * @author Hongzhi Liu  2014302580200@whu.edu.cn
 * @date 2018/10/9 11:31 
 */
public interface ZipConstants {
    public static final int CHANNEL_BLOCK_ID = 0x881155ff;  //the id data of id-value that represents channel information

    public static final int ZIP64_EOCD_LOCATOR_SIZE = 20;
    public static final int ZIP64_EOCD_LOCATOR_SIG_REVERSE_BYTE_ORDER = 0x504b0607;

    public static final int BIT_16_MAX_VALUE = 0xffff;      //max int value in 2 bytes
    public static final int ZIP_EOCD_REC_MIN_SIZE = 22;     //Eocd min size

    public static final int SHORT_LENGTH = 2;                   //the number of bytes of short type data
    public static final int ZIP_EOCD_COMMENT_LENGTH_FIELD_SIZE = SHORT_LENGTH;            //Eocd comment size

    public static final int ZIP_EOCD_CENTRAL_DIR_SIZE_FIELD_OFFSET = 12;
    public static final int ZIP_EOCD_CENTRAL_DIR_OFFSET_FIELD_OFFSET = 16;

    static final byte[] V1_MAGIC = {0x6c, 0x74, 0x6c, 0x6f, 0x76, 0x65, 0x7a, 0x68};      //v1 magic in LITTLE ENDIAN mode, added to end of EoCD block,it'll be used for reading from behind easily

    static final int V2_SIGN_BLOCK_MIN_SIZE = 32; //min bytes number of v2 signature block
    static final int V2_SIGN_BLOCK_ID = 0x7109871a;  //id of signature value at id-value block in V2 mode

    static final int  V2_MAGIC_SIZE = 16;    //bytes number of v2 magic block
    static final long V2_MAGIC_LOW =  0x20676953204b5041L;
    static final long V2_MAGIC_HEIGHT = 0x3234206b636f6c42L;
    static final long V2_BLOCK_ID = 0x7109871a;    //id of id-value in v2 signature block

}
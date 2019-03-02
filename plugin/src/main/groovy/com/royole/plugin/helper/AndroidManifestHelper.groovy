package com.royole.plugin.helper

import groovy.xml.XmlUtil

class AndroidManifestHelper {

    static final String CHANNEL_META_DATA = "Channel"

    File manifest

    Node root

    AndroidManifestHelper(File manifestFile){
        XmlParser xmlParser = new XmlParser()
        root = xmlParser.parse(manifestFile)
        manifest = manifestFile
    }


    void addOrUpdateChannelMetaData(String channel){
        def applicationNode = manifest.application[0]
        List<Node> metaDatas = applicationNode.'meta-data'

    }


    /**
     * 将内存中对manifest的修改写入文件
     */
    void save(){
        manifest.write(XmlUtil.serialize(root))
    }
}

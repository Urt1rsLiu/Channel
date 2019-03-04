package com.royole.plugin.helper

import groovy.xml.Namespace
import groovy.xml.XmlUtil
import org.gradle.api.GradleException
import org.w3c.dom.NodeList


/**
 * @author Hongzhi Liu
 * @date 2019/3/1
 */
class AndroidManifestHelper {

    static final String META_DATA_CHANNEL = "Channel"

    static final namespace = new Namespace('http://schemas.android.com/apk/res/android','android')

    File manifest

    Node root

    AndroidManifestHelper(File androidManifest) {
        XmlParser xmlParser = new XmlParser()
        root = xmlParser.parse(androidManifest)
        manifest = androidManifest
    }


    /**
     * 在application节点下添加或更新<meta-data android:name="channel" />的节点
     * @param androidName
     * @param androidValue
     */
    void addOrUpdateChannelMetaData(String androidValue) {
        def applicationNode = root.application[0]
        if (null == applicationNode) {
            throw new GradleException("Channel plugin: cant find <application> xml node in AndroidManifest")
        }
        List<Node> metaDataList = applicationNode.'meta-data'
        for(Node node:metaDataList){
            if (META_DATA_CHANNEL.equals(node.attribute(namespace.name))){
                applicationNode.remove(node)
                println "remove node"
            }
        }
        applicationNode.appendNode("meta-data", ["android:name": AndroidManifestHelper.META_DATA_CHANNEL, "android:value": androidValue])
    }

    /**
     * 注意内存中改完后必须写回到外存
     */
    void save() {
        manifest.write(XmlUtil.serialize(root))
    }
}

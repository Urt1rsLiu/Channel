# Channel

## Android 一个自动多渠道打包的gradle plugin（参考tencent VasDolly）

### 实现原理  [参考链接](https://github.com/Tencent/VasDolly/wiki/VasDolly%E5%AE%9E%E7%8E%B0%E5%8E%9F%E7%90%86)

基于Groovy/Java的gradle插件开发，用于项目发布到各应用渠道的多渠道打包。

基于ApkTool的多渠道打包的实现步骤如下:
1. 将Android Studio执行assembleDebug/assembleRelease Task后生成的debug/release基本apk包copy一份
2. 通过ApkTool工具解压apk  (apktool d origin.apk)
3. 删除已有signature
4. 添加渠道信息
5. 通过ApkTool (apktool b newApkDir) 打包成新的apk
6. 重新签名
  
缺点：
1. ApkTool工具不稳定，可能出现解压apk失败的情况
2. 该方式打包效率低，打包时间长

#### 本项目实现多渠道打包的步骤:
1. 根据android studio生成的已签名的apk包copy
2. 读取copy的apk包的签名配置，判断v1、v2签名
3. 根据不同的签名采用不同的方式添加渠道信息

#### 针对v1签名添加渠道信息的方式：
1. 找到EOCD数据块
2. 修改comment length数据块
3. 在comment数据块中添加渠道信息
4. 添加渠道信息长度
5. 添加magic number  （方便从后向前读取渠道信息）

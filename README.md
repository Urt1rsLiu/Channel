# Channel

## Android 一个自动多渠道打包的gradle plugin

### V1.0版本实现原理：直接对源apk的二进制文件作修改，将渠道信息插入到apk二进制结构中（具体参考![Tencent VasDolly的Github地址](https://github.com/Tencent/VasDolly/wiki/VasDolly%E5%AE%9E%E7%8E%B0%E5%8E%9F%E7%90%86)）
该原理实现的优点：效率快，直接便捷，打包时间迅速。
缺点: 后面难以根据项目需求集成apk加固的功能，不能直接修改AndroidManifest.xml等源文件

### V2.0版本实现原理：
1. 利用apktool decode源apk包，输出一个文件夹。
2. 在反编译得出的文件夹中的AndroidManifest.xml中插入渠道包信息
3. 利用apktool工具编译
4. 由于反编译 -> 编译过程破坏了apk文件的结构，需要重新使用ApkSigner签名(位于sdk/build-tools目录下)。
优缺点与V1.0方案相对



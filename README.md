# crosswalk-lite-demo  for Andorid
# 起因 
项目中要用到webview进行上传文件， 由于Android自身的原因，4.4.X的版本，都无法实现上传功能，于是找到了crosswalk. crosswalk基于Chromium. [crosswalk-lite](https://github.com/crosswalk-project/crosswalk-website/wiki/Crosswalk-Project-Lite)相对体积比较小，精简了一些功能，但也基本够用，更适合移动设备,虽然不是官方主推，但是更新频次也挺高 于是就用它了 
crosswalk有两种使用模式. Shared Mode和embedded Mode. 对于crosswalk-lite只支持embedded Mode. 
* Shared Mode: 把包含核心so文件的apk包(20多M)放到指定的http服务器上，crosswalk初始化时自动下载这个apk，然后需要用户手动安装这个apk包(可以称之为框架),非常类似于dotnet的framework.安装完成后crosswalk变成一个系统级的服务。如果你有多个app都用crosswalk，这时shared的好处才会体现出来，因为它只需要安装一次就能给多个app提供服务了。这样生成的apk体积比较小，对于移动端是个不错的方案. 但是由于不支持lite,下载的apk包是crosswalk的大包，体积巨大。
* embedded Mode:把核心so文件(9MB)放到raw文件夹中，打包到apk里。这样的结果就是apk变得很大，难于部署.
## 拆中方案
参考自[crosswalk之"瘦身"秘籍](http://blog.csdn.net/recall2012/article/details/47319653)  使用shared Mode方式，当然你用lite本来也没得选。不要把核心的so文件放到raw中打包，把这文件放到http服务上去，使用时候再下载。基本是结合两种模式的混合体了。
    

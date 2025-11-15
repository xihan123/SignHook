package website.xihan.signhelper.util

object NativeBridge {
    init {
        try {
            System.loadLibrary("nativehook")
        } catch (_: Throwable) {
        }
    }

    /**
     * 配置路径重定向：
     *  enable=true 并且 src/dst 都是非空字符串 => 生效
     *  否则关闭（清空配置）
     */
    @JvmStatic
    external fun enablePathRedirect(srcPath: String?, dstPath: String?, enable: Boolean)

}
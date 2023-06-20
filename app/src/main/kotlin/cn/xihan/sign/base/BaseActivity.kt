package cn.xihan.sign.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.accompanist.themeadapter.material3.Mdc3Theme

/**
 * @项目名 : 签名助手
 * @作者 : MissYang
 * @创建时间 : 2023/6/19 19:43
 * @介绍 :
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        init()
        setContent {
            Mdc3Theme {
                val systemUiController = rememberSystemUiController()
                val darkIcons = isSystemInDarkTheme()
                // We're using the system default here, but you could use any boolean value here

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        darkIcons = !darkIcons
                    )

                    systemUiController.setNavigationBarColor(
                        Color.Transparent,
                        darkIcons = !darkIcons
                    )
                }
                ComposeContent()
            }
        }
    }

    open fun init() {}

    @Composable
    abstract fun ComposeContent()

}
package website.xihan.signhelper.util

import android.content.Context
import website.xihan.signhelper.MyApp


private val sharedPrefs by lazy {
    runCatching {
        MyApp.application.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }.onFailure(ALog::e).getOrThrow()
}

object Settings : ISharedPreferencesOwner by SharedPreferencesOwner(sharedPrefs) {

    var hideHomeAppIcon by boolean()
    var hideAppIcons by boolean()
    var hideSystemApps by boolean(true)

}

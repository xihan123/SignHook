package website.xihan.signhelper

import androidx.multidex.MultiDexApplication
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.lazyModules
import website.xihan.signhelper.di.allModules
import java.util.concurrent.CopyOnWriteArraySet


class MyApp : MultiDexApplication(), XposedServiceHelper.OnServiceListener {

    companion object {
        @Volatile
        var mService: XposedService? = null
            private set

        @Volatile
        lateinit var application: MyApp
            private set

        private val serviceStateListeners = CopyOnWriteArraySet<ServiceStateListener>()

        private fun dispatchServiceState(
            listener: ServiceStateListener, service: XposedService?
        ) {
            if (serviceStateListeners.contains(listener)) {
                listener.onServiceStateChanged(service)
            }
        }

        fun addServiceStateListener(
            listener: ServiceStateListener, notifyImmediately: Boolean
        ) {
            serviceStateListeners.add(listener)
            if (notifyImmediately) {
                dispatchServiceState(listener, mService)
            }
        }

        fun removeServiceStateListener(listener: ServiceStateListener) {
            serviceStateListeners.remove(listener)
        }
    }

    private fun notifyServiceStateChanged(service: XposedService?) {
        for (listener in serviceStateListeners) {
            dispatchServiceState(listener, service)
        }
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        XposedServiceHelper.registerListener(this)
        startKoin {
            androidLogger()
            androidContext(this@MyApp)
            lazyModules(allModules)
        }
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    override fun onServiceBind(service: XposedService) {
        mService = service
        notifyServiceStateChanged(mService)
    }

    override fun onServiceDied(service: XposedService) {
        mService = null
        notifyServiceStateChanged(mService)
    }
}

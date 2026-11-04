package com.argus.agi.app.entry

import android.app.Application
import com.argus.agi.app.bootstrap.ArgusContainer
import com.argus.agi.application.runtime.ArgusRuntime
import com.argus.agi.application.runtime.ArgusRuntimeOwner
import com.argus.agi.platform.android.notification.createArgusNotificationChannels

class ArgusApp : Application(), ArgusRuntimeOwner {

    val container: ArgusContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ArgusContainer(this)
    }

    override val argusRuntime: ArgusRuntime
        get() = container

    override fun onCreate() {
        super.onCreate()
        createArgusNotificationChannels(this)
    }
}

package com.worktime.app

import android.app.Application
import com.worktime.app.data.WorkRepository

class WorkTimeApp : Application() {

    val repository: WorkRepository by lazy { WorkRepository(this) }

    override fun onCreate() {
        super.onCreate()
        repository.start()
    }
}

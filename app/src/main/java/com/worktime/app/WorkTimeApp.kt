package com.worktime.app

import android.app.Application
import com.worktime.app.data.StoreRepository

class WorkTimeApp : Application() {

    val repository: StoreRepository by lazy { StoreRepository(this) }

    override fun onCreate() {
        super.onCreate()
        repository.startLoad()
    }
}

package com.worktime.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.ui.AppRoot

class MainActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Android 6/7 没有浅色导航栏图标，透明导航栏会让白色按键看不见。
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.BLACK
        }
        val app = application as WorkTimeApp
        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.factory(app.repository))
            AppRoot(vm)
        }
    }
}

package com.worktime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.worktime.app.ui.AppRoot

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WorkTimeApp
        setContent {
            val vm: AppViewModel = viewModel(factory = AppViewModel.factory(app.repository))
            AppRoot(vm)
        }
    }
}

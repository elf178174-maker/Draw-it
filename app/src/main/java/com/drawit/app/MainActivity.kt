package com.drawit.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drawit.app.ui.DrawItRoot
import com.drawit.app.ui.theme.DrawItTheme

class MainActivity : ComponentActivity() {

    private var pendingAddRequest by mutableStateOf(false)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DrawItTheme {
                val vm: AppViewModel = viewModel()
                DrawItRoot(
                    viewModel = vm,
                    openAddOnStart = pendingAddRequest,
                    onAddRequestHandled = { pendingAddRequest = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_ADD_DRAWING) pendingAddRequest = true
    }

    companion object {
        const val ACTION_ADD_DRAWING = "com.drawit.app.action.ADD_DRAWING"
    }
}

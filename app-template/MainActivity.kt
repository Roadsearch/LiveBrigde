package com.livebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.livebridge.rtmp.StreamController
import com.livebridge.studio.StudioStore
import com.livebridge.ui.LiveBridgeTheme
import com.livebridge.ui.StudioApp

class MainActivity : ComponentActivity() {

    private var permissionsGranted by mutableStateOf(false)
    private lateinit var controller: StreamController
    private lateinit var store: StudioStore

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionsGranted = hasCapturePermissions()
            if (permissionsGranted) initializeStudio()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsGranted = hasCapturePermissions()
        if (permissionsGranted) {
            initializeStudio()
        } else {
            requestCapturePermissions()
        }

        setContent {
            LiveBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionsGranted && ::controller.isInitialized && ::store.isInitialized) {
                        val ui by controller.ui.collectAsState()
                        StudioApp(controller, store, Prefs(this@MainActivity), ui)
                    } else {
                        PermissionScreen(onRequest = ::requestCapturePermissions)
                    }
                }
            }
        }
    }

    private fun initializeStudio() {
        if (!::controller.isInitialized) {
            val prefs = Prefs(this)
            controller = StreamController.get(this)
            store = StudioStore(this, prefs, controller)
        }
    }

    private fun hasCapturePermissions(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestCapturePermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }
}

@androidx.compose.runtime.Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "LiveBridge a besoin de la caméra et du microphone",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "Autorise ces deux accès pour afficher l’aperçu vidéo et utiliser l’audio.",
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        Button(onClick = onRequest) {
            Text("AUTORISER")
        }
    }
}

package com.livebridge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.livebridge.rtmp.StreamController
import com.livebridge.studio.StudioStore
import com.livebridge.ui.LiveBridgeTheme
import com.livebridge.ui.StudioApp

class MainActivity : ComponentActivity() {
    private lateinit var controller: StreamController
    private lateinit var store: StudioStore
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        controller = StreamController.get(this)
        store = StudioStore(this, prefs, controller)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
        setContent {
            val ui by controller.ui.collectAsState()
            LiveBridgeTheme {
                Surface(Modifier.fillMaxSize()) { StudioApp(controller, store, prefs, ui) }
            }
        }
    }
}

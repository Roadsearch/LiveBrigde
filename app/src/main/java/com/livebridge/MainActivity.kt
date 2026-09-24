package com.livebridge

import android.Manifest
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.livebridge.rtmp.StreamController
import com.livebridge.studio.StudioStore
import com.livebridge.ui.LiveBridgeTheme
import com.livebridge.ui.StudioApp

class MainActivity : ComponentActivity() {

    private var permissionsGranted by mutableStateOf(false)
    private var crashDetected by mutableStateOf(false)
    private lateinit var controller: StreamController
    private lateinit var store: StudioStore
    private lateinit var projectionManager: MediaProjectionManager

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && ::store.isInitialized) {
            runCatching { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } }
                .getOrNull()?.let { store.addImage("Image / logo", it) }
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            controller.setScreenProjection(projectionManager.getMediaProjection(result.resultCode, result.data!!))
        } else {
            CrashReporter.log(this, "SCREEN_CAPTURE_CANCELLED")
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionsGranted = hasCapturePermissions()
            if (permissionsGranted) initializeStudio()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashReporter.install(this)
        CrashReporter.log(this, "MAIN_ACTIVITY_ON_CREATE")

        super.onCreate(savedInstanceState)

        crashDetected = CrashReporter.hasCrash(this)
        if (!crashDetected) continueStartup()

        setContent {
            LiveBridgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        crashDetected -> CrashReportScreen(
                            report = CrashReporter.readCrash(this@MainActivity),
                            onCopy = {
                                if (CrashReporter.copyCrash(this@MainActivity)) {
                                    Toast.makeText(this@MainActivity, "Rapport copié", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRetry = {
                                CrashReporter.clearCrash(this@MainActivity)
                                crashDetected = false
                                continueStartup()
                            }
                        )

                        permissionsGranted && ::controller.isInitialized && ::store.isInitialized -> {
                            val ui by controller.ui.collectAsState()
                            StudioApp(controller, store, Prefs(this@MainActivity), ui, onPickImage = { imagePickerLauncher.launch("image/*") })
                        }

                        else -> PermissionScreen(onRequest = ::requestCapturePermissions)
                    }
                }
            }
        }
    }

    private fun continueStartup() {
        permissionsGranted = hasCapturePermissions()
        CrashReporter.log(this, "STARTUP permissionsGranted=$permissionsGranted")
        if (permissionsGranted) {
            initializeStudio()
        } else {
            requestCapturePermissions()
        }
    }

    private fun initializeStudio() {
        CrashReporter.log(this, "INITIALIZE_STUDIO_BEGIN")
        if (!::controller.isInitialized) {
            val prefs = Prefs(this)
            controller = StreamController.get(this)
            store = StudioStore(this, prefs, controller)
            projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            controller.onRequestScreenCapture = { screenCaptureLauncher.launch(projectionManager.createScreenCaptureIntent()) }
        }
        CrashReporter.log(this, "INITIALIZE_STUDIO_OK")
    }

    private fun hasCapturePermissions(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestCapturePermissions() {
        CrashReporter.log(this, "REQUEST_CAPTURE_PERMISSIONS")
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("LiveBridge a besoin de la caméra et du microphone", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Autorise ces deux accès pour afficher l’aperçu vidéo et utiliser l’audio.",
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        Button(onClick = onRequest) { Text("AUTORISER") }
    }
}

@Composable
private fun CrashReportScreen(
    report: String,
    onCopy: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("LiveBridge a rencontré un problème", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Le rapport est conservé après le crash. Copie-le et envoie-le-moi pour identifier précisément l’erreur.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onCopy) { Text("COPIER LE RAPPORT") }
            OutlinedButton(onClick = onRetry) { Text("RÉESSAYER") }
        }
        Text(
            report,
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

package kr.co.root.legolas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.AndroidEntryPoint
import kr.co.root.legolas.core.designsystem.theme.RootTheme
import kr.co.root.legolas.pairing.ui.PairingScreen
import kr.co.root.legolas.pairing.ui.PairingViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: PairingViewModel by viewModels()

    private val scanner by lazy {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(this, options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            RootTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                PairingScreen(
                    state = state,
                    onScan = ::scan,
                    onForget = {
                        viewModel.forget(getString(R.string.pairing_forget_failed))
                    },
                )
            }
        }
    }

    private fun scan() {
        viewModel.clearError()
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val value = barcode.rawValue
                if (value == null) {
                    viewModel.onScanFailed(getString(R.string.invalid_qr))
                } else {
                    viewModel.onQrScanned(
                        value = value,
                        invalidMessage = getString(R.string.invalid_qr),
                        saveFailedMessage = getString(R.string.pairing_save_failed),
                    )
                }
            }
            .addOnFailureListener {
                viewModel.onScanFailed(getString(R.string.scanner_failed))
            }
    }
}

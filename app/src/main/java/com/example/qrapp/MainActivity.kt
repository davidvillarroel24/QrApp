package com.example.qrapp

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.qrapp.ui.theme.QrAppTheme
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var previewView: PreviewView?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor=Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()
        enableEdgeToEdge()
        setContent {
            QrAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BarcodeScanningScreen(innerPadding)
                }
            }
        }
    }


    @Composable
    fun BarcodeScanningScreen(innerPadding: PaddingValues) {
        var scanResult by remember { mutableStateOf("Escanea el codigo") }
        var context =LocalContext.current
        val requestPermissionLaucher = rememberLauncherForActivityResult(RequestPermission()){
                isGranted ->
            if (isGranted) {
                // Permission granted, proceed with camera setup
                startCamera(context,{result -> scanResult= result})
                scanResult = "Permiso concedido"
            } else {
                // Permission denied, show a message or handle accordingly
                scanResult = "Permiso denegado"
            }
        }
        LaunchedEffect(true) {
            requestPermissionLaucher.launch(android.Manifest.permission.CAMERA)
        }
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            AndroidView(modifier = Modifier.fillMaxSize().weight(1f),
                factory = { context ->
                    PreviewView(context).apply {
                        previewView = this
                        layoutParams=LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT)
                    }
                },
                update = { view ->
                    // Update the PreviewView if needed
                    //view.scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            )
            Text(text = scanResult,
                modifier = Modifier.padding(16.dp),
                style= androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            println(scanResult)
            guardarCsv(scanResult)
        }
    }
    fun guardarCsv(result:String){
        print(result)
    }

    private fun startCamera(context: Context,onBarcodeDetected: (String) -> Unit) {
        // Implement camera setup and barcode detection logic here
        // Use the barcodeScanner to process the camera frames
        // Call onBarcodeDetected with the scanned result when a barcode is detected

        // Example placeholder for camera setup
        // This should be replaced with actual camera initialization and frame processing logic
        val cameraProviderFuture= ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview= androidx.camera.core.Preview.Builder().build()
            previewView?.let{ previewView ->
            preview.setSurfaceProvider(previewView.surfaceProvider)
            val imageAnalyzer = androidx.camera.core.ImageAnalysis.Builder().
            setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor,{
                            imageProxy ->processImageProxy( onBarcodeDetected, imageProxy)
                    })
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            }
            // Set up the camera and bind the lifecycle
            // For example, you can use CameraX to bind the camera to the PreviewView
            // and set up the barcode scanner to process frames

            // Placeholder for barcode detection logic
            // This should be replaced with actual barcode scanning logic
            // For now, we simulate a barcode detection result
        }, ContextCompat.getMainExecutor(context))
        //onBarcodeDetected("Simulated Barcode Result")

    }


    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(onBarcodeDetected: (String) -> Unit, imageProxy: androidx.camera.core.ImageProxy) {
        // Process the image frame and detect barcodes
        // Use the barcodeScanner to scan the image
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image= InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        // Handle the detected barcode
                        handleBarcode(onBarcodeDetected, barcode)
                        /*val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            onBarcodeDetected(rawValue)
                        }*/
                    }
                }
                .addOnFailureListener { e ->
                    // Handle any errors during barcode detection
                    onBarcodeDetected("Error: ${e.message}")
                    Toast.makeText(this, "Error detecting barcode: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    imageProxy.close() // Close the image proxy to avoid memory leaks
                }
        }
    }

    private fun handleBarcode(onBarcodeDetected: (String) -> Unit, barcode: Barcode) {
        // Handle the detected barcode and extract relevant information
        /*val rawValue = barcode.rawValue
        if (rawValue != null) {
            onBarcodeDetected(rawValue)
        } else {
            onBarcodeDetected("No barcode detected")
        }*/
        val barcodeText = barcode.url ?.url ?: barcode.displayValue
        if (barcodeText != null) {
            onBarcodeDetected(barcodeText)
        } else {
            onBarcodeDetected("No barcode detected")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}



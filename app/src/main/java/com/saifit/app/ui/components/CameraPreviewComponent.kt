package com.saifit.app.ui.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraPreviewComponent(
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    onRecordingStarted: () -> Unit = {},
    onRecordingSaved: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()

        val capture = VideoCapture.withOutput(recorder)
        videoCapture = capture

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
        } catch (exc: Exception) {
            Log.e("CameraPreview", "Use case binding failed", exc)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val capture = videoCapture ?: return@LaunchedEffect

            val videoFile = File(
                context.filesDir,
                "saifit_recording_${System.currentTimeMillis()}.mp4"
            )

            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            activeRecording = capture.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            onRecordingStarted()
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (!event.hasError()) {

                                val savedUri = event.outputResults.outputUri
                                val finalUri = if (savedUri != null && savedUri != Uri.EMPTY) {
                                    savedUri
                                } else {
                                    Uri.fromFile(videoFile)
                                }
                                Log.d("CameraPreview", "Video saved to: $finalUri (size: ${videoFile.length()} bytes)")
                                onRecordingSaved(finalUri)
                            } else {
                                Log.e("CameraPreview", "Video capture error: ${event.error}")

                                if (videoFile.exists() && videoFile.length() > 0) {
                                    Log.d("CameraPreview", "Using file fallback: ${videoFile.absolutePath} (size: ${videoFile.length()} bytes)")
                                    onRecordingSaved(Uri.fromFile(videoFile))
                                }
                            }
                        }
                    }
                }
        } else {

            activeRecording?.stop()
            activeRecording = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

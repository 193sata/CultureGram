package com.example.culturegram

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.io.File

class CameraScreen {

    @Composable
    fun Content(s: String, navController: NavController) {
        val context = LocalContext.current
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var hasCameraPermission by remember { mutableStateOf(false) }

        // カメラパーミッションのランチャー
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // カメラパーミッションの確認
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                hasCameraPermission = true
            } else {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }

        if (hasCameraPermission) {
            // パーミッションが許可された場合、カメラプレビューと撮影機能を表示
            Box(modifier = Modifier.fillMaxSize()) {
                // カメラプレビューの表示、sを渡す
                CameraPreview(
                    onImageCaptured = { uri ->
                        imageUri = uri
                        Toast.makeText(context, "Image captured: $uri", Toast.LENGTH_SHORT).show()
                    },
                    onError = { message ->
                        Toast.makeText(context, "Error: $message", Toast.LENGTH_SHORT).show()
                    },
                    fileNameBase = s // sを渡してファイル名に使用する
                )
                // 左上に黒背景の丸ボタン（×ボタン）を配置し、クリックで前の画面に戻る
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)  // 半透明の黒い丸背景
                        .clickable {
                            navController.popBackStack()  // 前の画面に戻る
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = Color.White,
                        fontSize = 24.sp,  // バツのサイズを大きく
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // パーミッションが拒否された場合のメッセージや代替UI
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Camera permission is required to use this feature.")
            }
        }
    }

    @Composable
    fun CameraPreview(
        onImageCaptured: (Uri) -> Unit,
        onError: (String) -> Unit,
        fileNameBase: String // 追加: ファイル名のベース
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

        Box(modifier = Modifier.fillMaxSize()) {  // Box内でalignを使用する
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx)
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder().build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                        onError("Camera initialization failed.")
                    }

                    previewView
                }
            )

            // 画面下部に半透明の黒背景を追加し、ボタンを目立たせる
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.Black.copy(alpha = 0.5f)) // 半透明の黒背景
            ) {
                // 写真撮影ボタンをカメラプレビューの上に重ねる
                Button(
                    onClick = {
                        // ファイル名にfileNameBaseを使用し、ユニークなファイル名を作成
                        val photoFile = createUniqueFile(
                            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!,
                            fileNameBase
                        )
                        val outputOptions =
                            ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        imageCapture?.takePicture(
                            outputOptions, ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    onImageCaptured(Uri.fromFile(photoFile))
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    onError(exc.message ?: "Image capture failed")
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)  // alignをBox内で使用
                        .padding(16.dp)
                        .size(70.dp),  // ボタンのサイズを指定
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)  // ボタンを白くする
                ) {
                }
            }
        }
    }

    // ユニークなファイル名を生成する関数 (常にjpgとして保存)
    private fun createUniqueFile(directory: File, baseName: String): File {
        var counter = 0
        var file = File(directory, "$baseName-$counter.jpg") // 1枚目から s-0.jpg にする
        while (file.exists()) {
            counter++
            file = File(directory, "$baseName-$counter.jpg")
        }
        return file
    }
}
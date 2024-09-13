package com.example.culturegram

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.ExifInterface
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.io.File
import android.media.MediaActionSound
import androidx.compose.ui.viewinterop.AndroidView
import java.io.FileOutputStream

class CameraScreen {

    @Composable
    fun Content(s: String, navController: NavController) {
        val context = LocalContext.current
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        var hasCameraPermission by remember { mutableStateOf(false) }

        // ランダムでキャラクターを選択
        val characterResIds = listOf(
            R.drawable.spanyan1,
            R.drawable.spanyan2,
            R.drawable.spanyan3,
            R.drawable.spanyan4,
            R.drawable.spanyan5,
            R.drawable.spanyan6,
            R.drawable.ryokochan
        )
        val selectedCharacter = remember { characterResIds.random() }

        // キャラクター表示/非表示を管理する状態
        var isCharacterVisible by remember { mutableStateOf(true) }

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
                    fileNameBase = s,  // sを渡してファイル名に使用する
                    selectedCharacter = selectedCharacter, // プレビューに表示するキャラクターを渡す
                    isCharacterVisible = isCharacterVisible // キャラクターの表示状態を渡す
                )

                // 遺産の名前を画面上部中央に表示
//                Box(
//                    modifier = Modifier
//                        .align(Alignment.TopCenter)
//                        .padding(top = 16.dp)
//                ) {
//                    Text(text = s, fontSize = 24.sp, color = Color.White)
//                }

                // キャラクターを表示するかどうかのボタンを追加
                Button(
                    onClick = { isCharacterVisible = !isCharacterVisible },  // ボタンを押すたびに表示/非表示を切り替える
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    Text(text = if (isCharacterVisible) "Hide" else "Show")
                }

                // キャラクターをプレビュー画面に表示
                if (isCharacterVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(100.dp) // キャラクターのサイズを調整 ryokochan: 500を推奨
                    ) {
                        Image(
                            painter = painterResource(id = selectedCharacter),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 左上に黒背景の丸ボタン（×ボタン）を配置し、クリックで前の画面に戻る
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .clickable {
                            navController.popBackStack()  // 前の画面に戻る
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = Color.White,
                        fontSize = 24.sp,
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
        fileNameBase: String,
        selectedCharacter: Int,  // 追加: キャラクターのリソースIDを渡す
        isCharacterVisible: Boolean  // 追加: キャラクター表示のフラグを渡す
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

        // システムのシャッター音を再生するためのMediaActionSound
        val sound = MediaActionSound()

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
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // 写真撮影ボタンをカメラプレビューの上に重ねる
                Button(
                    onClick = {
                        // シャッター音を再生
                        sound.play(MediaActionSound.SHUTTER_CLICK)

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
                                    // 画像保存処理
                                    if (isCharacterVisible) {
                                        addCharacterToImage(
                                            photoFile = photoFile,
                                            context = context,
                                            selectedCharacter = selectedCharacter,  // 保存にも同じキャラクターを使用
                                            xOffset = 0.8f,  // 右下に配置するため、横幅の80%に
                                            yOffset = 0.8f,  // 縦幅の80%に
                                            //scaleFactor = 0.2f  // キャラクターを少し小さく表示
                                        )
                                    }
                                    onImageCaptured(Uri.fromFile(photoFile))
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    onError(exc.message ?: "Image capture failed")
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .size(70.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                }
            }
        }
    }

    // 画像にキャラクターを重ねて保存する処理
    private fun addCharacterToImage(
        photoFile: File,
        context: Context,
        selectedCharacter: Int,
        xOffset: Float,
        yOffset: Float
        //scaleFactor: Float
    ) {
        val backgroundBitmap = loadRotatedBitmap(photoFile)  // 画像をEXIFデータに基づいて正しい向きに回転
        val characterBitmap = BitmapFactory.decodeResource(context.resources, selectedCharacter)
        val combinedBitmap = Bitmap.createBitmap(
            backgroundBitmap.width,
            backgroundBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(combinedBitmap)
        val paint = Paint()

        // 背景画像を描画
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, paint)
        println("characterBitmap.width")
        println(characterBitmap.width)
        //275fはニャンコのbitmap.widthの0.2倍のサイズ感
        var scaleFactor: Float = 275f/characterBitmap.width.toFloat()
        // キャラクター画像を指定した位置とスケールで描画
        val scaledCharacterWidth = characterBitmap.width * scaleFactor
        val scaledCharacterHeight = characterBitmap.height * scaleFactor
        val xPosition = (backgroundBitmap.width - scaledCharacterWidth) * xOffset
        val yPosition = (backgroundBitmap.height - scaledCharacterHeight) * yOffset
        println("scaledsize")
        println(scaledCharacterWidth)
        println(scaledCharacterHeight)
        canvas.drawBitmap(
            characterBitmap,
            null,
            RectF(
                xPosition,
                yPosition,
                xPosition + scaledCharacterWidth,
                yPosition + scaledCharacterHeight
            ),
            paint
        )

        // 結果の画像をファイルに保存
        val outputStream = FileOutputStream(photoFile)
        combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
    }

    // EXIFデータに基づいて画像を回転させる関数
    private fun loadRotatedBitmap(imageFile: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(imageFile.path)
        val exif = ExifInterface(imageFile.path)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    // 画像を指定した角度で回転させる関数
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // ユニークなファイル名を生成する関数 (常にjpgとして保存)
    private fun createUniqueFile(directory: File, baseName: String): File {
        var counter = 0
        var file = File(directory, "$baseName-$counter.jpg")
        while (file.exists()) {
            counter++
            file = File(directory, "$baseName-$counter.jpg")
        }
        return file
    }
}
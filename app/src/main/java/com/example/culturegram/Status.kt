package com.example.culturegram

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import java.io.File
import java.io.IOException

class Status {

    @Composable
    fun Content(navController: NavHostController) {
        val context = LocalContext.current
        AchievementPage(navController, context)
    }

    @Composable
    fun AchievementPage(navController: NavHostController, context: Context) {
        val heritageList = loadOrCreateCsv()
        updateAchievementStatus(heritageList, context)

        val visitedCount = heritageList.count { it.visited }
        val totalCount = heritageList.size
        val achievementRatio = if (totalCount > 0) visitedCount.toFloat() / totalCount else 0f

        var selectedHeritage by remember { mutableStateOf<WorldHeritage?>(null) }

        if (selectedHeritage == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // 達成度のドーナツ型グラフ
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(150.dp)
                ) {
                    DonutChart(achievementRatio, visitedCount, totalCount)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 画像一覧
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(heritageList.size) { index ->
                        val heritage = heritageList[index]

                        val latitudeStr = createLatitudeStr(heritage.latitude)
                        println("latitudeStr")
                        println(latitudeStr)
                        // 経度の処理
                        val longitudeStr = createLongitudeStr(heritage.longitude)

                        val imagePath = getSavedImagePath(context, heritage.name)
                            ?: "/storage/emulated/0/Android/data/com.example.culturegram/files/Pictures/${heritage.name}-0.jpg"
                        val imageFile = File(imagePath)
                        // 緯度経度を文字列に変換し、小数点を除いて結合
                        val latLongStr = "${latitudeStr.replace(".", "")}${longitudeStr.replace(".", "")}"
                        val imageName = "heritage_$latLongStr"
                        println("heritagename")
                        println(heritage.name)
                        println("緯度")
                        println(heritage.latitude)
                        println("経度")
                        println(heritage.longitude)
                        println("imageName")
                        println(imageName)
                        Box(
                            modifier = Modifier
                                .border(0.5.dp, Color.Black)
                                .aspectRatio(1f)
                                .clickable {
                                    selectedHeritage = heritage
                                }
                        ) {
                            if (imageFile.exists()) {
                                val bitmap = loadRotatedBitmap(imageFile)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = heritage.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val context = LocalContext.current
                                val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                                println("resId")
                                println(resId)
                                if (resId != 0) {
                                    // drawableに指定の名前の画像が存在する場合
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = imageName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // 指定の名前の画像が存在しない場合はNo Imageを表示
                                    Image(
                                        painter = painterResource(id = R.drawable.no_image),
                                        contentDescription = "No Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            EnlargedImageScreen(
                heritage = selectedHeritage!!,
                navController = navController,
                onBackClick = { selectedHeritage = null }
            )
        }
    }

    @Composable
    fun DonutChart(percentage: Float, visitedCount: Int, totalCount: Int) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                drawCircle(
                    color = Color.LightGray,
                    style = Stroke(width = 15f)
                )
                drawArc(
                    color = Color.Green,
                    startAngle = -90f,
                    sweepAngle = 360 * percentage,
                    useCenter = false,
                    style = Stroke(width = 15f, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$visitedCount / $totalCount",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EnlargedImageScreen(
        heritage: WorldHeritage,
        navController: NavHostController,
        onBackClick: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = heritage.name, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate("edit/${heritage.name}")
                        }) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = "編集")
                        }
                    }
                )
            },
            content = { padding ->
                val latitudeStr = createLatitudeStr(heritage.latitude)
                val longitudeStr = createLongitudeStr(heritage.longitude)
                // 緯度経度を文字列に変換し、小数点を除いて結合
                val latLongStr = "${latitudeStr.replace(".", "")}${longitudeStr.replace(".", "")}"
                val imageName = "heritage_$latLongStr"

                // 保存された画像パスを取得
                val imagePath = getSavedImagePath(LocalContext.current, heritage.name)
                    ?: "/storage/emulated/0/Android/data/com.example.culturegram/files/Pictures/${heritage.name}-0.jpg"
                val imageFile = File(imagePath)
                println("imagename")
                println(imageName)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (imageFile.exists()) {
                        val bitmap = loadRotatedBitmap(imageFile)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = heritage.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val context = LocalContext.current
                        val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                        println("resId")
                        println(resId)
                        if (resId != 0) {
                            // drawableに指定の名前の画像が存在する場合
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = imageName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // 指定の名前の画像が存在しない場合はNo Imageを表示
                            Image(
                                painter = painterResource(id = R.drawable.no_image),
                                contentDescription = "No Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        )
    }

    // EXIFデータに基づいて画像を回転させる関数
    private fun loadRotatedBitmap(imageFile: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(imageFile.path)
        var rotatedBitmap = bitmap

        try {
            val exif = ExifInterface(imageFile.path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return rotatedBitmap
    }

    // 画像を指定した角度で回転させる関数
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // CSVを読み込むか、なければ作成する関数
    private fun loadOrCreateCsv(): List<WorldHeritage> {
        val filePath = "/storage/emulated/0/Android/data/com.example.culturegram/files/csv/heritages.csv"
        val csvFile = File(filePath)

        // ファイルが存在しない場合、作成する
        if (!csvFile.exists()) {
            createCsv(csvFile)
        }

        val heritageList = mutableListOf<WorldHeritage>()
        if (csvFile.exists()) {
            csvFile.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size == 4) {
                    val name = parts[0]
                    val latitude = parts[1].toDoubleOrNull() ?: 0.0
                    val longitude = parts[2].toDoubleOrNull() ?: 0.0
                    val visited = parts[3].toIntOrNull() == 1
                    heritageList.add(WorldHeritage(name, latitude, longitude, visited))
                }
            }
        }

        return heritageList
    }

    // 達成度を更新する関数
    private fun updateAchievementStatus(heritageList: List<WorldHeritage>, context: Context) {
        heritageList.forEach { heritage ->
            val imagePath = getSavedImagePath(context, heritage.name)
            if (imagePath != null && File(imagePath).exists()) {  // 画像が存在する場合にのみカウント
                heritage.visited = true
            } else {
                heritage.visited = false  // 画像がない場合は未達成にする
            }
        }
    }

    // CSVファイルを作成する関数
    private fun createCsv(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(
            """
        法隆寺地域の仏教建造物,34.6145,135.7356,0
        姫路城,34.8394,134.6939,0
        古都京都の文化財,35.0116,135.7681,0
        白川郷・五箇山の合掌造り集落,36.2605,137.0468,0
        原爆ドーム,34.3955,132.4536,0
        厳島神社,34.2955,132.3197,0
        古都奈良の文化財,34.6851,135.8048,0
        日光の社寺,36.7197,139.6986,0
        琉球王国のグスク及び関連遺産群,26.2173,127.7148,0
        紀伊山地の霊場と参詣道,33.8711,135.7740,0
        石見銀山遺跡とその文化的景観,35.1214,132.4622,0
        平泉‐仏国土（浄土）を表す建築・庭園及び考古学的遺跡群‐,39.0015,141.1082,0
        富士山‐信仰の対象と芸術の源泉‐,35.3606,138.7274,0
        富岡製糸場と絹産業遺産群,36.2586,138.8894,0
        明治日本の産業革命遺産,33.5904,130.4017,0
        ル・コルビュジエの建築作品‐近代建築運動への顕著な貢献‐,35.6586,139.7454,0
        「神宿る島」宗像・沖ノ島と関連遺産群,33.9180,130.5339,0
        長崎と天草地方の潜伏キリシタン関連遺産,32.7503,129.8777,0
        百舌鳥・古市古墳群‐古代日本の墳墓群‐,34.5565,135.4885,0
        北海道・北東北の縄文遺跡群,43.6343,142.5451,0
        佐渡島の金山,37.8034,138.3968,0
        """.trimIndent()
        )
    }

    private fun createLatitudeStr(latitude: Double): String {
        val latitudeStr: String  = latitude.toString()
        if (latitude.toString().contains(".")) {
            val decimalPart = latitude.toString().split(".")[1]
            // 小数点以下の桁数を確認し、3桁の場合にゼロを追加
            if (decimalPart.length == 3) {
                return latitudeStr + "0"
            }
            else {
                return latitudeStr
            }
        }


        return latitudeStr
    }

    private fun createLongitudeStr(longitude: Double): String {
        val longitudeStr: String = longitude.toString()
        if (longitude.toString().contains(".")) {
            val decimalPart = longitude.toString().split(".")[1]
            // 小数点以下の桁数を確認し、3桁の場合にゼロを追加
            if (decimalPart.length == 3) {
                return longitudeStr + "0"
            } else {
                return longitudeStr
            }
        }

        return longitudeStr
    }

}

// WorldHeritageデータクラス
data class WorldHeritage(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var visited: Boolean,
    var imagePath: String = "" // 画像パスを保持する
)
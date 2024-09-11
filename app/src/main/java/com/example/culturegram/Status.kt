package com.example.culturegram

import GetImages
import android.content.Context
import android.os.FileObserver
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


class MyFileObserver(
    private val context: Context,
    private val filename: String,
    private val onFileChanged: () -> Unit
) : FileObserver(File(context.filesDir, filename).absolutePath, CLOSE_WRITE) {
    override fun onEvent(event: Int, path: String?) {
        Log.d("MyFileObserver", "Event: $event, Path: $path, AbsolutePath: ${File(context.filesDir,filename).absolutePath}")
        if (event == 8) {
            onFileChanged()
        }
    }
}

class Status {
    @Composable
    fun Content(){
        val context = LocalContext.current
        var heritageCsv by remember { mutableStateOf(loadCsvFileFromInternalStorage(context,"internalCsvFile")) }
        lateinit var fileObserver: MyFileObserver
        val filename = "internalCsvFile"
        fileObserver = MyFileObserver(context, filename) {
            fileObserver.stopWatching()
            heritageCsv = loadCsvFileFromInternalStorage(context, filename)
            fileObserver.startWatching()
        }

        fileObserver.startWatching()
        LaunchedEffect(Unit) {
            if (heritageCsv[0][0].length <= 5) {
                fileObserver.startWatching()
                saveCsvFileToInternalStorage(
                    context = context,
                    data = readCsvFile(context),
                    fileName = "internalCsvFile"
                )
            }
        }
        //読み込んだheritageCsvの、内部ストレージファイルのinternalCsvFileの初期状態の設定
        //バグあり

//        fun onDestroy() {
//            // 監視を停止
//            fileObserver.stopWatching()
//        }


        // GetImagesクラスのContentメソッドを呼び出して画像のリストを取得
        val getImages = GetImages()
        val images = getImages.Content(context)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painterResource(id = R.drawable.white_00008),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(modifier = Modifier.fillMaxSize()) {
                Row {
                    UserChara()
                    Ratio(sample = heritageCsv)
                }
                HorizontalDivider(thickness = 24.dp)
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    for ((j, i) in heritageCsv.withIndex()) {
                        if (i[3] == "1"){
                            Row(modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color.Blue.copy(alpha = 0.2f))) {
                                ListImage(images,i = i, j = j, context)
                                ListCheck(i = i)
                            }
                        }
                        else if (i[3] == "0"){
                            Row(modifier = Modifier
                                .fillMaxSize()
                                .background(color = Color.LightGray.copy(alpha = 0.2f))) {
                                ListImage(images,i = i, j = j, context)
                                ListCheck(i = i)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(5.dp))
                    }
                }
            }
        }
    }

    //    object Database {
    @Composable
    fun UserChara() {
        Image(
            painter = painterResource(id = R.drawable.android_logo),
            contentDescription = null,
            modifier = Modifier
                .size(180.dp, 180.dp)
                .padding(horizontal = 24.dp, vertical = 5.dp)
        )
    }

    @Composable
    fun Ratio(sample: List<List<String>>) {
        var sum = 0
        for (i in sample) {
            if (i[3] == "1") {
                sum++
            }
        }
        Text(
            text = "$sum/26",
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 50.dp)
        )
    }

    @Composable
    fun ListCheck(i: List<String>) {
        val check: String
        if (i[3] == "1") {
            check = "〇"
        } else {
            check = "ｘ"
        }
        Text(
            text = i[0] + " : " + check,
            fontSize = 24.sp
        )
    }

    //設計途中
    @Composable
    fun ListImage(images:List<String> ,i: List<String>, j: Int, context: Context) {
        val imageInt: Int = R.drawable.white_00008
        var imageStr = ""
        var imageCheck = 0
        println(images)
        for (str in images){
            //if (false){
                if (str[73] == i[0][1]) {
                    imageStr = str
                    imageCheck = 1
                    println("Success.")
                }
        }
        if (imageCheck == 0){
        Image(
            painter = painterResource(id = imageInt),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp, 60.dp)
                .clickable {
                    accessCsvFile(j = j, context)
                }
        )
        }
        else{
            Image(
                painter = rememberImagePainter(data = imageStr),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp,50.dp)
                    .clickable {
                        accessCsvFile(j = j,context)
                    }
            )
        }
    }

    private fun saveCsvFileToInternalStorage(
        context: Context,
        data: List<List<String>>,
        fileName: String
    ) {
        val file = File(context.filesDir, fileName)
//            FileOutputStream(file,false).use{ outputStream ->
//                outputStream.write(ByteArray(0))
//            }

        FileOutputStream(file,false).use { outputStream ->
            val csvContent = data.joinToString("\n") { it.joinToString(",") }
            outputStream.write(csvContent.toByteArray())
        }
    }

    private fun loadCsvFileFromInternalStorage(
        context: Context,
        fileName: String
    ): MutableList<MutableList<String>> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            // ファイルが存在しない場合は空のリストを返すか、新しいファイルを作成するなどの処理を行う
            return mutableListOf()
        }
        return file.readLines()
            .map { it.split(",").map(String::trim).toMutableList() }
            .toMutableList()
    }

    private fun accessCsvFile(j: Int, context: Context) {
        val data = loadCsvFileFromInternalStorage(context, "internalCsvFile")
        val row = data.getOrNull(j)
        if (row != null && row.size > 3) {
            println(row[3])
            if (row[3] == "0") {
                row[3] = "1"
                println("changing...")
            }
            else{
                row[3] = "0"
            }
            println("saved successfully.")
        }
        saveCsvFileToInternalStorage(context, data, "internalCsvFile")
    }


    private fun readCsvFile(context: Context): List<List<String>> {
        val csvLines = mutableListOf<List<String>>()
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("csvData.csv")
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.drop(1).take(21).forEach { line ->
                    // 各行をカンマで分割してList<String>に変換
                    val row = line.split(",").map { it.trim() }
                    csvLines.add(row)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return csvLines
    }
}

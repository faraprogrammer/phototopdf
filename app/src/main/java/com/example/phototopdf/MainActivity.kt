package com.example.phototopdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhotoToPdfApp()
        }
    }
}

@Composable
fun PhotoToPdfApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var fileName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            fileName = "PDF_$timeStamp"
        }
    }

    // طراحی تیره و شیک با گرادیان
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )
    val accentGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = selectedUris.isEmpty(),
                label = "ScreenTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    // صفحه ۱: انتخاب نامحدود عکس
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Photo to PDF",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "تبدیل سریع، آفلاین و محلی",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(2.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🖼️", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "انتخاب عکس‌ها از گالری",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "پشتیبانی از تعداد نامحدود",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    // صفحه ۲: نام‌گذاری فایل و ساخت PDF
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Text(
                                text = "${selectedUris.size} عکس انتخاب شده",
                                color = Color(0xFF818CF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("نام فایل خروجی") },
                            singleLine = true,
                            enabled = !isProcessing,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFA855F7),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isProcessing) {
                            CircularProgressIndicator(color = Color(0xFFA855F7))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = progressText,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Button(
                                onClick = {
                                    isProcessing = true
                                    scope.launch {
                                        generatePdf(
                                            context = context,
                                            uris = selectedUris,
                                            outputName = fileName.ifBlank { "Photos" },
                                            onProgress = { current, total ->
                                                progressText = "پردازش عکس $current از $total..."
                                            },
                                            onComplete = { success ->
                                                isProcessing = false
                                                if (success) {
                                                    Toast.makeText(context, "فایل PDF در پوشه Downloads ذخیره شد!", Toast.LENGTH_LONG).show()
                                                    selectedUris = emptyList()
                                                } else {
                                                    Toast.makeText(context, "خطا در ساخت PDF", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accentGradient),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text("دریافت خروجی PDF", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            TextButton(
                                onClick = { selectedUris = emptyList() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("انتخاب مجدد تصاویر", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// موتور کاملاً آفلاین و استاندارد ساخت PDF بر اساس ابعاد استاندارد A4
suspend fun generatePdf(
    context: Context,
    uris: List<Uri>,
    outputName: String,
    onProgress: (Int, Int) -> Unit,
    onComplete: (Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        uris.forEachIndexed { index, uri ->
            withContext(Dispatchers.Main) {
                onProgress(index + 1, uris.size)
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val scale = minOf(
                    (pageWidth - 40f) / originalBitmap.width,
                    (pageHeight - 40f) / originalBitmap.height
                )
                val targetW = originalBitmap.width * scale
                val targetH = originalBitmap.height * scale
                val left = (pageWidth - targetW) / 2f
                val top = (pageHeight - targetH) / 2f

                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetW.toInt(), targetH.toInt(), true)
                canvas.drawBitmap(scaledBitmap, left, top, null)
                pdfDocument.finishPage(page)
            }
        }

        val finalName = if (outputName.endsWith(".pdf", ignoreCase = true)) outputName else "$outputName.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val outputUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        outputUri?.let { uri ->
            context.contentResolver.openOutputStream(uri)?.use { out ->
                pdfDocument.writeTo(out)
            }
        }
        pdfDocument.close()

        withContext(Dispatchers.Main) { onComplete(true) }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) { onComplete(false) }
    }
}

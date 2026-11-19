package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.GrainIconKey
import com.example.data.model.StockStatus
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.SacredGold
import com.example.ui.theme.StockAvailableBg
import com.example.ui.theme.StockAvailableGreen
import com.example.ui.theme.StockLowAmber
import com.example.ui.theme.StockLowBg
import com.example.ui.theme.StockOutBg
import com.example.ui.theme.StockOutRed
import com.example.ui.theme.WarmOutline
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MahadevLogoBadge(
  modifier: Modifier = Modifier,
  size: Dp = 110.dp,
) {
  Surface(
    modifier = modifier.size(size),
    shape = CircleShape,
    color = ParchmentSurface,
    shadowElevation = 6.dp,
    tonalElevation = 2.dp,
  ) {
    Image(
      painter = painterResource(id = R.drawable.mahadev_logo),
      contentDescription = "મહાદેવ ટ્રેડર્સ લોગો",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape),
    )
  }
}

@Composable
fun GrainThumbnail(
  iconKey: String,
  imageBase64: String,
  grainName: String,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  cornerRadius: Dp = 14.dp,
) {
  val bitmap =
    remember(imageBase64) {
      if (imageBase64.isBlank()) {
        null
      } else {
        try {
          val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
          null
        }
      }
    }

  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(cornerRadius))
        .border(1.dp, WarmOutline, RoundedCornerShape(cornerRadius)),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = grainName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      GrainIllustratedBadge(iconKey = iconKey, grainName = grainName)
    }
  }
}

@Composable
private fun GrainIllustratedBadge(iconKey: String, grainName: String) {
  val key = GrainIconKey.fromKey(iconKey)
  val (bgTop, bgBottom, accentColor, seedColor) =
    when (key) {
      GrainIconKey.WHEAT ->
        listOf(Color(0xFFFFF4DC), Color(0xFFF4D9A0), Color(0xFFC5841A), Color(0xFFE0A43B))
      GrainIconKey.RICE ->
        listOf(Color(0xFFFDFBF7), Color(0xFFEAE3D5), Color(0xFF9E8B70), Color(0xFFFFFFFF))
      GrainIconKey.BAJRI ->
        listOf(Color(0xFFF3EFE8), Color(0xFFD6CEC0), Color(0xFF6B6559), Color(0xFF8C8577))
      GrainIconKey.JOWAR ->
        listOf(Color(0xFFFAF3E6), Color(0xFFE8D4B4), Color(0xFF9C7B4F), Color(0xFFF0DFC2))
      GrainIconKey.CHANA ->
        listOf(Color(0xFFFFF2DB), Color(0xFFF2C97D), Color(0xFFB37414), Color(0xFFD99426))
      GrainIconKey.TOOR_DAL ->
        listOf(Color(0xFFFFF7CC), Color(0xFFF7D547), Color(0xFFB8860B), Color(0xFFFFCA28))
      GrainIconKey.MOONG ->
        listOf(Color(0xFFEAF7E8), Color(0xFFB5DBA8), Color(0xFF2E7D32), Color(0xFF4CAF50))
      GrainIconKey.MAIZE ->
        listOf(Color(0xFFFFF6D1), Color(0xFFFBD14B), Color(0xFFC98A00), Color(0xFFFFB300))
      GrainIconKey.GROCERY ->
        listOf(Color(0xFFFDECE8), Color(0xFFF4C2B8), CrimsonPrimary, SacredGold)
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(colors = listOf(bgTop, bgBottom))),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Decorative grain sack / bowl silhouette
      val bowlPath =
        Path().apply {
          moveTo(w * 0.18f, h * 0.56f)
          quadraticTo(w * 0.50f, h * 0.90f, w * 0.82f, h * 0.56f)
          close()
        }
      drawPath(bowlPath, color = accentColor.copy(alpha = 0.85f))
      drawLine(
        color = accentColor,
        start = Offset(w * 0.16f, h * 0.56f),
        end = Offset(w * 0.84f, h * 0.56f),
        strokeWidth = w * 0.05f,
      )

      // Grain kernels / wheat ear motif
      val kernelOffsets =
        listOf(
          Offset(0.35f, 0.48f),
          Offset(0.50f, 0.45f),
          Offset(0.65f, 0.48f),
          Offset(0.42f, 0.36f),
          Offset(0.58f, 0.36f),
          Offset(0.50f, 0.26f),
        )
      for (pt in kernelOffsets) {
        drawOval(
          color = seedColor,
          topLeft = Offset(w * pt.x - w * 0.06f, h * pt.y - h * 0.045f),
          size = Size(w * 0.12f, h * 0.09f),
        )
        drawOval(
          color = accentColor.copy(alpha = 0.45f),
          topLeft = Offset(w * pt.x - w * 0.06f, h * pt.y - h * 0.045f),
          size = Size(w * 0.12f, h * 0.09f),
          style = Stroke(width = 1.5f),
        )
      }
    }
    if (grainName.isNotBlank()) {
      Text(
        text = grainName.take(1),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
      )
    }
  }
}

@Composable
fun StockStatusBadge(stockLabel: String, modifier: Modifier = Modifier) {
  val status = StockStatus.fromLabel(stockLabel)
  val (bgColor, textColor) =
    when (status) {
      StockStatus.AVAILABLE -> StockAvailableBg to StockAvailableGreen
      StockStatus.LOW_STOCK -> StockLowBg to StockLowAmber
      StockStatus.OUT_OF_STOCK -> StockOutBg to StockOutRed
    }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = bgColor,
  ) {
    Text(
      text = status.labelGu,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = textColor,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

object ImageUtils {
  suspend fun compressUriToBase64(context: Context, uri: Uri): String =
    withContext(Dispatchers.IO) {
      val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext ""
      val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return@withContext ""
      val maxDim = 256
      val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
      val targetW = (original.width * ratio).toInt().coerceAtLeast(1)
      val targetH = (original.height * ratio).toInt().coerceAtLeast(1)
      val scaled =
        if (targetW != original.width || targetH != original.height) {
          Bitmap.createScaledBitmap(original, targetW, targetH, true)
        } else {
          original
        }
      val baos = ByteArrayOutputStream()
      scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos)
      val bytes = baos.toByteArray()
      if (scaled != original) scaled.recycle()
      original.recycle()
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}

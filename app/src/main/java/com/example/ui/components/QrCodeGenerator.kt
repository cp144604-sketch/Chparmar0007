package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.WarmOutline
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
  if (content.isBlank()) return null
  return try {
    val hints =
      mapOf(
        EncodeHintType.MARGIN to 2,
        EncodeHintType.CHARACTER_SET to "UTF-8",
      )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until matrix.width) {
      for (y in 0 until matrix.height) {
        bitmap.setPixel(
          x,
          y,
          if (matrix[x, y]) AndroidColor.rgb(43, 18, 16) else AndroidColor.WHITE,
        )
      }
    }
    bitmap
  } catch (_: Exception) {
    null
  }
}

@Composable
fun QrCodeImage(
  url: String,
  modifier: Modifier = Modifier,
  size: Dp = 220.dp,
) {
  val bitmap = remember(url) { generateQrBitmap(url, 512)?.asImageBitmap() }
  Surface(
    modifier = modifier.size(size),
    shape = RoundedCornerShape(18.dp),
    color = Color.White,
    shadowElevation = 4.dp,
  ) {
    Box(
      modifier =
        Modifier.border(2.dp, WarmOutline, RoundedCornerShape(18.dp)).padding(10.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ QR કોડ",
          modifier = Modifier.size(size - 20.dp),
        )
      }
    }
  }
}

@Composable
fun AppShareQrCard(
  titleGu: String,
  downloadUrl: String,
  onCopiedLink: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = titleGu,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = CrimsonPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "ગ્રાહકો આ QR કોડ સ્કેન કરીને સીધા એપ ખોલી / ડાઉનલોડ કરી શકે છે.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(18.dp))
      QrCodeImage(url = downloadUrl, size = 220.dp)
      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        color = ParchmentSurface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = downloadUrl,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(10.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = {
            clipboard.setText(AnnotatedString(downloadUrl))
            onCopiedLink()
          },
          modifier = Modifier.weight(1f).height(48.dp).testTag("copy_download_link_button"),
          shape = MaterialTheme.shapes.medium,
        ) {
          Icon(
            imageVector = Icons.Filled.ContentCopy,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = CrimsonPrimary,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("કોપી લિંક", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = { shareAppDownloadLink(context, titleGu, downloadUrl) },
          modifier = Modifier.weight(1f).height(48.dp).testTag("share_download_link_button"),
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
        ) {
          Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("શેર કરો", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

fun shareAppDownloadLink(context: Context, titleGu: String, url: String) {
  val shareText =
    "મહાદેવ ટ્રેડર્સ (હળવદ) — અનાજ ભાવ એપ\n$titleGu\n\nલિંક: $url"
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ")
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
  context.startActivity(Intent.createChooser(intent, "એપ લિંક શેર કરો"))
}

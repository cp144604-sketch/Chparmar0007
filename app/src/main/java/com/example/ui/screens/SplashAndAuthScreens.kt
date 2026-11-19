package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.attemptAutoSignIn
import com.example.auth.getAppSigningSha1
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.MahadevLogoBadge
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.ParchmentSurface
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SacredGoldLight
import com.example.ui.theme.WarmOutline

@Composable
fun SplashScreen() {
  var animateIn by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) { animateIn = true }
  val scale by
    animateFloatAsState(
      targetValue = if (animateIn) 1f else 0.84f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "splash_scale",
    )

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(ParchmentSurface, IvoryBackground, SacredGoldLight)
          )
        )
        .safeDrawingPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp).scale(scale),
    ) {
      MahadevLogoBadge(size = 156.dp)
      Spacer(modifier = Modifier.height(24.dp))
      Text(
        text = "મહાદેવ ટ્રેડર્સ",
        style = MaterialTheme.typography.displaySmall,
        color = CrimsonPrimary,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "અનાજ ભાવ એપ • હળવદ",
        style = MaterialTheme.typography.titleMedium,
        color = HarvestAmber,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "અનાજ કરીયાણાના રીટેઈલ તથા હોલસેલ વેપારી",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(32.dp))
      CircularProgressIndicator(
        color = CrimsonPrimary,
        trackColor = SacredGoldLight,
        strokeWidth = 3.dp,
        modifier = Modifier.size(34.dp),
      )
    }
  }
}

@Composable
fun GoogleAuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val clipboard = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var copiedSha1 by remember { mutableStateOf(false) }
  val appSha1 = remember(context) { getAppSigningSha1(context) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(CrimsonPrimary, CrimsonDeep, IvoryBackground, IvoryBackground)
          )
        )
        .safeDrawingPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 520.dp)
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      MahadevLogoBadge(size = 124.dp)
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "મહાદેવ ટ્રેડર્સ",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        textAlign = TextAlign.Center,
      )
      Text(
        text = "અનાજ અને કરીયાણાના લાઈવ ભાવ • હળવદ",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.9f),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(24.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Surface(
            color = SacredGoldLight,
            shape = RoundedCornerShape(50),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = null,
                tint = HarvestAmber,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "સુરક્ષિત લાઈવ સિંક",
                style = MaterialTheme.typography.labelMedium,
                color = HarvestAmber,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "એપમાં પ્રવેશ કરો",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text =
              "રોજના તાજા અનાજ ભાવ, સ્ટોક અપડેટ્સ અને તમારી યાદી તરત મેળવવા Google ખાતા વડે ચાલુ રાખો.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )

          if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = errorMessage!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = CrimsonDeep,
                )
                if (!appSha1.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(6.dp))
                  TextButton(
                    onClick = {
                      clipboard.setText(AnnotatedString(appSha1))
                      copiedSha1 = true
                    },
                    modifier = Modifier.testTag("copy_sha1_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.ContentCopy,
                      contentDescription = null,
                      tint = CrimsonPrimary,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = if (copiedSha1) "SHA-1 કોપી થઈ ગયો ✓" else "SHA-1 કોપી કરો",
                      color = CrimsonPrimary,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(22.dp))
          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
              )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("google_sign_in_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("લોગિન થઈ રહ્યું છે...")
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CustomerMobileLoginScreen(
  initialMobile: String,
  initialName: String,
  isBusy: Boolean,
  onSubmitMobileLogin: (mobile: String, name: String) -> Unit,
  onGoToAdminLogin: () -> Unit,
  onGoogleSignOut: () -> Unit,
) {
  var mobile by remember(initialMobile) { mutableStateOf(initialMobile) }
  var name by remember(initialName) { mutableStateOf(initialName) }
  var localError by remember { mutableStateOf<String?>(null) }

  val submitAction = {
    val digits = mobile.filter { it.isDigit() }
    if (digits.length != 10) {
      localError = "કૃપા કરીને 10 અંકનો સાચો મોબાઈલ નંબર લખો."
    } else {
      localError = null
      onSubmitMobileLogin(digits, name.trim())
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(CrimsonPrimary, CrimsonDeep, IvoryBackground, IvoryBackground)
          )
        )
        .safeDrawingPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .widthIn(max = 520.dp)
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      MahadevLogoBadge(size = 112.dp)
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "મહાદેવ ટ્રેડર્સ",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
      )
      Text(
        text = "ગ્રાહક મોબાઈલ લોગિન",
        style = MaterialTheme.typography.titleMedium,
        color = SacredGoldLight,
      )

      Spacer(modifier = Modifier.height(20.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      ) {
        Column(modifier = Modifier.padding(22.dp)) {
          Text(
            text = "તમારો મોબાઈલ નંબર દાખલ કરો",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "તાજા અનાજ ભાવ અને સ્ટોક જોવા માટે મોબાઈલ નંબર વડે લોગિન કરો.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = mobile,
            onValueChange = { input ->
              val filtered = input.filter { it.isDigit() }.take(10)
              mobile = filtered
              if (localError != null) localError = null
            },
            label = { Text("મોબાઈલ નંબર (10 અંક)") },
            placeholder = { Text("જેમકે 9876543210") },
            leadingIcon = {
              Icon(Icons.Filled.Phone, contentDescription = null, tint = CrimsonPrimary)
            },
            singleLine = true,
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = WarmOutline,
              ),
            modifier = Modifier.fillMaxWidth().testTag("customer_mobile_input"),
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 50) name = it },
            label = { Text("તમારું નામ (વૈકલ્પિક)") },
            placeholder = { Text("જેમકે રમેશભાઈ પટેલ") },
            leadingIcon = {
              Icon(Icons.Filled.Person, contentDescription = null, tint = SacredGold)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submitAction() }),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = WarmOutline,
              ),
            modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
          )

          if (!localError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = localError!!,
              style = MaterialTheme.typography.bodySmall,
              color = CrimsonPrimary,
              fontWeight = FontWeight.Medium,
            )
          }

          Spacer(modifier = Modifier.height(20.dp))

          Button(
            onClick = submitAction,
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("customer_login_button"),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
          ) {
            if (isBusy) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
              )
            } else {
              Text(
                text = "લોગિન કરો",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))
          HorizontalDivider(color = WarmOutline)
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = onGoToAdminLogin,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.testTag("shortcut_admin_login_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.AdminPanelSettings,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = CrimsonPrimary,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("એડમિન લોગિન", color = CrimsonPrimary)
            }

            TextButton(
              onClick = onGoogleSignOut,
              modifier = Modifier.testTag("google_sign_out_button"),
            ) {
              Text("ખાતું બદલો", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    }
  }
}

package com.example.game.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebTestScreen(
  onBackToNative: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler {
    onBackToNative()
  }

  var webViewRef: WebView? = remember { null }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF02182B))
      .statusBarsPadding()
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onBackToNative,
        modifier = Modifier
          .size(36.dp)
          .background(Color(0x5503045E), CircleShape)
          .testTag("back_to_native_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Native Game",
          tint = Color(0xFF90E0EF)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Web Test Version",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFCAF0F8)
        )
        Text(
          text = "HTML5 Canvas • public/index.html",
          fontSize = 11.sp,
          color = Color(0xFF90E0EF)
        )
      }

      IconButton(
        onClick = { webViewRef?.reload() },
        modifier = Modifier
          .size(36.dp)
          .background(Color(0x5503045E), CircleShape)
          .testTag("reload_web_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Reload Web Version",
          tint = Color(0xFF90E0EF)
        )
      }
    }

    // Embedded Web Test View
    Box(
      modifier = Modifier
        .fillMaxSize()
        .weight(1f)
    ) {
      AndroidView(
        factory = { context ->
          WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              allowFileAccess = true
              loadWithOverviewMode = true
              useWideViewPort = true
            }
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/web_test/index.html")
            webViewRef = this
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("web_game_webview")
      )
    }
  }
}

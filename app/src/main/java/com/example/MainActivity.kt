package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.GameViewModel
import com.example.game.ui.FlappyFishScreen
import com.example.game.ui.WebTestScreen
import com.example.ui.theme.DeepOcean
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = DeepOcean
        ) {
          FlappyFishApp()
        }
      }
    }
  }
}

enum class ScreenState {
  NATIVE_GAME,
  WEB_TEST
}

@Composable
fun FlappyFishApp(
  viewModel: GameViewModel = viewModel()
) {
  var currentScreen by remember { mutableStateOf(ScreenState.NATIVE_GAME) }

  when (currentScreen) {
    ScreenState.NATIVE_GAME -> {
      FlappyFishScreen(
        viewModel = viewModel,
        onOpenWebTest = { currentScreen = ScreenState.WEB_TEST }
      )
    }
    ScreenState.WEB_TEST -> {
      WebTestScreen(
        onBackToNative = { currentScreen = ScreenState.NATIVE_GAME }
      )
    }
  }
}


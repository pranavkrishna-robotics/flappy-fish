package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameViewModel
import com.example.game.model.GameDifficulty
import com.example.game.model.GameState
import com.example.game.model.MedalType
import com.example.game.renderer.GameCanvas

@Composable
fun FlappyFishScreen(
  viewModel: GameViewModel,
  onOpenWebTest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val gameState by viewModel.gameState.collectAsState()
  val score by viewModel.score.collectAsState()
  val bestScore by viewModel.bestScore.collectAsState()
  val isNewBest by viewModel.isNewBest.collectAsState()
  val difficulty by viewModel.difficulty.collectAsState()
  val soundEnabled by viewModel.soundEnabled.collectAsState()

  // High-performance 60fps Game Loop
  LaunchedEffect(Unit) {
    var lastFrameTimeNanos = 0L
    while (true) {
      withFrameNanos { frameTimeNanos ->
        if (lastFrameTimeNanos != 0L) {
          val dt = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
          viewModel.tick(dt)
        }
        lastFrameTimeNanos = frameTimeNanos
      }
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    // 1. High-Performance Underwater Game Canvas (isolated recomposition scope)
    GameWorld(
      viewModel = viewModel,
      onTap = { viewModel.onTap() }
    )

    // 2. Top Bar Controls
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 38.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Mode / Web Test button
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x9903045E),
        modifier = Modifier.border(1.dp, Color(0x4490E0EF), RoundedCornerShape(20.dp))
      ) {
        Row(
          modifier = Modifier
            .clickable { onOpenWebTest() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("web_test_button"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Language,
            contentDescription = "Web Test Version",
            tint = Color(0xFF90E0EF),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Web Test",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      // Right: Controls (Sound toggle, Pause)
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { viewModel.toggleSound() },
          modifier = Modifier
            .size(38.dp)
            .background(Color(0x9903045E), CircleShape)
            .border(1.dp, Color(0x4490E0EF), CircleShape)
            .testTag("sound_toggle_button")
        ) {
          Icon(
            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
            contentDescription = "Toggle Sound",
            tint = Color(0xFF90E0EF),
            modifier = Modifier.size(20.dp)
          )
        }

        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = { viewModel.togglePause() },
            modifier = Modifier
              .size(38.dp)
              .background(Color(0x9903045E), CircleShape)
              .border(1.dp, Color(0x4490E0EF), CircleShape)
              .testTag("pause_button")
          ) {
            Icon(
              imageVector = if (gameState == GameState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = "Pause / Resume",
              tint = Color(0xFF90E0EF),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    // 3. In-Game Live Score Counter
    if (gameState == GameState.PLAYING) {
      Text(
        text = "$score",
        fontSize = 54.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 90.dp)
          .shadow(8.dp)
          .testTag("live_score_text")
      )
    }

    // 4. Ready Screen Overlay
    if (gameState == GameState.READY) {
      ReadyOverlay(
        bestScore = bestScore,
        currentDifficulty = difficulty,
        onSelectDifficulty = { viewModel.setDifficulty(it) },
        modifier = Modifier.align(Alignment.Center)
      )
    }

    // 5. Game Over Modal Overlay
    AnimatedVisibility(
      visible = gameState == GameState.GAME_OVER,
      enter = fadeIn() + scaleIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center)
    ) {
      GameOverDialog(
        score = score,
        bestScore = bestScore,
        isNewBest = isNewBest,
        medal = MedalType.forScore(score),
        onPlayAgain = { viewModel.resetGame() }
      )
    }

    // 6. Pause Overlay
    if (gameState == GameState.PAUSED) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0x9903045E)),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Text(
            text = "Game Paused",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { viewModel.togglePause() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
            modifier = Modifier.testTag("resume_button")
          ) {
            Text("Resume Swimming", color = Color(0xFF03045E), fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedButton(
            onClick = { viewModel.resetGame() },
            modifier = Modifier.testTag("restart_button")
          ) {
            Text("Restart", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
private fun GameWorld(
  viewModel: GameViewModel,
  onTap: () -> Unit,
  modifier: Modifier = Modifier
) {
  val fish by viewModel.fish.collectAsState()
  val seaweeds by viewModel.seaweeds.collectAsState()
  val bubbles by viewModel.bubbles.collectAsState()
  val scorePopups by viewModel.scorePopups.collectAsState()
  val gameState by viewModel.gameState.collectAsState()
  val gameTime by viewModel.gameTime.collectAsState()

  GameCanvas(
    fish = fish,
    seaweeds = seaweeds,
    bubbles = bubbles,
    scorePopups = scorePopups,
    gameState = gameState,
    seabedHeight = viewModel.seabedHeight,
    gameTimeSec = gameTime,
    onTap = onTap,
    modifier = modifier
  )
}

@Composable
private fun ReadyOverlay(
  bestScore: Int,
  currentDifficulty: GameDifficulty,
  onSelectDifficulty: (GameDifficulty) -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "tap_bob")
  val tapOffsetY by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -12f,
    animationSpec = infiniteRepeatable(
      animation = tween(600),
      repeatMode = RepeatMode.Reverse
    ),
    label = "tap_offset"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .padding(24.dp)
      .widthIn(max = 380.dp)
  ) {
    // Title
    Text(
      text = "FLAPPY FISH",
      fontSize = 36.sp,
      fontWeight = FontWeight.Black,
      color = Color(0xFFCAF0F8),
      letterSpacing = 2.sp,
      style = MaterialTheme.typography.headlineLarge
    )
    Text(
      text = "Swim through the Seaweed Reef",
      fontSize = 14.sp,
      color = Color(0xFF90E0EF),
      modifier = Modifier.padding(bottom = 32.dp)
    )

    // Tap Prompt
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 16.dp)
    ) {
      Text(
        text = "👆",
        fontSize = 42.sp,
        modifier = Modifier
          .offset(y = tapOffsetY.dp)
          .padding(bottom = 8.dp)
      )
      Text(
        text = "TAP SCREEN TO SWIM",
        fontSize = 18.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFFE3A8),
        letterSpacing = 1.sp
      )
      Text(
        text = "Flap upwards & dodge seaweed obstacles",
        fontSize = 12.sp,
        color = Color(0xCCFFFFFF)
      )
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Best Score Badge
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = Color(0xAA023E8A),
      modifier = Modifier.border(1.dp, Color(0x4490E0EF), RoundedCornerShape(18.dp))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "🏆", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Best Score: $bestScore",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFE9C46A)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Difficulty Selector Chips
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(top = 8.dp)
    ) {
      GameDifficulty.entries.forEach { diff ->
        val selected = diff == currentDifficulty
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (selected) Color(0xFF00B4D8) else Color(0x6603045E),
          modifier = Modifier
            .clickable { onSelectDifficulty(diff) }
            .border(
              width = 1.dp,
              color = if (selected) Color(0xFF90E0EF) else Color(0x33FFFFFF),
              shape = RoundedCornerShape(16.dp)
            )
            .testTag("diff_${diff.name.lowercase()}")
        ) {
          Text(
            text = diff.displayName,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF03045E) else Color(0xDDFFFFFF),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun GameOverDialog(
  score: Int,
  bestScore: Int,
  isNewBest: Boolean,
  medal: MedalType,
  onPlayAgain: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xF2071E3D)),
    modifier = Modifier
      .padding(24.dp)
      .widthIn(max = 340.dp)
      .border(2.dp, Color(0xFF00B4D8), RoundedCornerShape(24.dp))
      .testTag("game_over_card")
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(24.dp)
    ) {
      Text(
        text = "SPLASH! GAME OVER",
        fontSize = 22.sp,
        fontWeight = FontWeight.Black,
        color = Color(0xFFFF7B00)
      )

      if (isNewBest) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFFFB703)
        ) {
          Text(
            text = "✨ NEW HIGH SCORE! ✨",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF03045E),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Stats Box
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0x6603045E), RoundedCornerShape(16.dp))
          .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "SCORE", fontSize = 12.sp, color = Color(0xFF90E0EF), fontWeight = FontWeight.Bold)
          Text(text = "$score", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color(0x4490E0EF))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "BEST", fontSize = 12.sp, color = Color(0xFF90E0EF), fontWeight = FontWeight.Bold)
          Text(text = "$bestScore", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE9C46A))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Medal Tier
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0x44023E8A), RoundedCornerShape(12.dp))
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = medal.emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Medal: ${medal.title}",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFCAF0F8)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Play Again Action Button
      Button(
        onClick = onPlayAgain,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("play_again_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          tint = Color(0xFF03045E),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "PLAY AGAIN",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF03045E)
        )
      }
    }
  }
}

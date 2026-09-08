package com.example.game

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.GameAudio
import com.example.game.model.BubbleParticle
import com.example.game.model.FishState
import com.example.game.model.GameDifficulty
import com.example.game.model.GameState
import com.example.game.model.MedalType
import com.example.game.model.ScorePopup
import com.example.game.model.SeaweedObstacle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("flappy_fish_prefs", Context.MODE_PRIVATE)

  val audio = GameAudio(application)

  val virtualHeight = 640f
  val seabedHeight = 75f

  private val _gameState = MutableStateFlow(GameState.READY)
  val gameState: StateFlow<GameState> = _gameState.asStateFlow()

  private val _difficulty = MutableStateFlow(GameDifficulty.CLASSIC)
  val difficulty: StateFlow<GameDifficulty> = _difficulty.asStateFlow()

  private val _score = MutableStateFlow(0)
  val score: StateFlow<Int> = _score.asStateFlow()

  private val _bestScore = MutableStateFlow(prefs.getInt("high_score", 0))
  val bestScore: StateFlow<Int> = _bestScore.asStateFlow()

  private val _isNewBest = MutableStateFlow(false)
  val isNewBest: StateFlow<Boolean> = _isNewBest.asStateFlow()

  private val _fish = MutableStateFlow(FishState())
  val fish: StateFlow<FishState> = _fish.asStateFlow()

  private val _seaweeds = MutableStateFlow<List<SeaweedObstacle>>(emptyList())
  val seaweeds: StateFlow<List<SeaweedObstacle>> = _seaweeds.asStateFlow()

  private val _bubbles = MutableStateFlow<List<BubbleParticle>>(emptyList())
  val bubbles: StateFlow<List<BubbleParticle>> = _bubbles.asStateFlow()

  private val _scorePopups = MutableStateFlow<List<ScorePopup>>(emptyList())
  val scorePopups: StateFlow<List<ScorePopup>> = _scorePopups.asStateFlow()

  private val _gameTime = MutableStateFlow(0f)
  val gameTime: StateFlow<Float> = _gameTime.asStateFlow()

  private val _soundEnabled = MutableStateFlow(true)
  val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

  private var obstacleIdCounter = 0L
  private var bubbleIdCounter = 0L

  init {
    initBubbles()
    resetGame()
  }

  private fun initBubbles() {
    val initialBubbles = mutableListOf<BubbleParticle>()
    for (i in 0 until 24) {
      initialBubbles.add(
        BubbleParticle(
          id = ++bubbleIdCounter,
          x = Random.nextFloat() * 360f,
          y = Random.nextFloat() * virtualHeight,
          radius = 2.5f + Random.nextFloat() * 5.5f,
          vy = -35f - Random.nextFloat() * 55f,
          alpha = 0.35f + Random.nextFloat() * 0.45f,
          wobblePhase = Random.nextFloat() * (2f * PI.toFloat()),
          wobbleSpeed = 2f + Random.nextFloat() * 2.5f
        )
      )
    }
    _bubbles.value = initialBubbles
  }

  fun resetGame() {
    _score.value = 90
    _isNewBest.value = false
    _fish.value = FishState(
      x = 95f,
      y = 280f,
      radius = 18f,
      vy = 0f,
      rotationDeg = 0f,
      tailPhase = 0f,
      tailAngle = 0f,
      finAngle = 0f,
      swimAnimTimer = 0f,
      squashX = 1f,
      squashY = 1f
    )
    _seaweeds.value = emptyList()
    _scorePopups.value = emptyList()

    val initialList = mutableListOf<SeaweedObstacle>()
    val diff = _difficulty.value
    var currentX = 380f
    for (i in 0 until 3) {
      initialList.add(createObstacle(currentX, diff.gapSize))
      currentX += 210f
    }
    _seaweeds.value = initialList
    _gameState.value = GameState.READY
  }

  fun onTap() {
    when (_gameState.value) {
      GameState.READY -> {
        _gameState.value = GameState.PLAYING
        swim()
      }
      GameState.PLAYING -> {
        swim()
      }
      GameState.GAME_OVER -> {
        resetGame()
      }
      GameState.PAUSED -> {
        _gameState.value = GameState.PLAYING
      }
    }
  }

  private fun swim() {
    val currentFish = _fish.value
    // Instant snappy response: upward swim kick impulse and prompt upward tilt
    _fish.value = currentFish.copy(
      vy = -365f,
      rotationDeg = -24f,
      swimAnimTimer = 1.0f
    )
    audio.playBloop()

    // Spawn wake bubbles trailing directly behind the tail fin
    val wakeBubbles = ArrayList<BubbleParticle>(4)
    for (i in 0 until 4) {
      wakeBubbles.add(
        BubbleParticle(
          id = ++bubbleIdCounter,
          x = currentFish.x - 22f - Random.nextFloat() * 6f,
          y = currentFish.y + (Random.nextFloat() - 0.5f) * 8f,
          radius = 2.2f + Random.nextFloat() * 3.0f,
          vx = -35f - Random.nextFloat() * 30f,
          vy = -20f - Random.nextFloat() * 30f,
          alpha = 0.85f,
          isWake = true
        )
      )
    }
    _bubbles.value = _bubbles.value + wakeBubbles
  }

  fun togglePause() {
    if (_gameState.value == GameState.PLAYING) {
      _gameState.value = GameState.PAUSED
    } else if (_gameState.value == GameState.PAUSED) {
      _gameState.value = GameState.PLAYING
    }
  }

  fun setDifficulty(diff: GameDifficulty) {
    if (_difficulty.value != diff) {
      _difficulty.value = diff
      resetGame()
    }
  }

  fun toggleSound() {
    val newState = !_soundEnabled.value
    _soundEnabled.value = newState
    audio.soundEnabled = newState
  }

  fun tick(deltaSeconds: Float) {
    val dt = deltaSeconds.coerceIn(0.001f, 0.05f)
    val time = _gameTime.value + dt
    _gameTime.value = time

    // Update Bubbles (ambient & wake)
    updateBubbles(dt)

    // Update Score Popups
    updateScorePopups(dt)

    when (_gameState.value) {
      GameState.READY -> {
        val f = _fish.value
        val idleSpeed = 6.5f
        val newPhase = f.tailPhase + idleSpeed * dt
        val tailAngle = sin(newPhase) * 12f
        val finAngle = sin(newPhase) * 14f
        _fish.value = f.copy(
          y = 280f + sin(time * 3.2f) * 10f,
          rotationDeg = sin(time * 3.2f) * 4f,
          tailPhase = newPhase,
          tailAngle = tailAngle,
          finAngle = finAngle,
          squashX = 1f,
          squashY = 1f,
          swimAnimTimer = 0f
        )
      }
      GameState.PLAYING -> {
        updatePlayingPhysics(dt)
      }
      GameState.GAME_OVER -> {
        val f = _fish.value
        val floorY = virtualHeight - seabedHeight - f.radius
        if (f.y < floorY) {
          val newVy = f.vy + 920f * dt
          val newY = (f.y + newVy * dt).coerceAtMost(floorY)
          _fish.value = f.copy(
            vy = newVy,
            y = newY,
            rotationDeg = (f.rotationDeg + 180f * dt).coerceAtMost(90f),
            tailAngle = 0f,
            finAngle = -8f,
            squashX = 1f,
            squashY = 1f
          )
        }
      }
      GameState.PAUSED -> {
        // Paused state does not advance game physics
      }
    }
  }

  private fun updatePlayingPhysics(dt: Float) {
    val f = _fish.value
    // Crisp aquatic gravity & fluid resistance
    val gravity = 820f
    val newVy = (f.vy + gravity * dt).coerceIn(-430f, 660f)
    var newY = f.y + newVy * dt

    // Ceiling clamp
    val ceilY = 16f + f.radius
    if (newY < ceilY) {
      newY = ceilY
    }

    // Swim impulse decay
    val newSwimTimer = (f.swimAnimTimer - dt * 3.5f).coerceAtLeast(0f)

    // Coordinated hydrodynamic pitch angle:
    // When rising, fish holds an active upward angle; when sinking, gracefully pitches down
    val targetRot = if (newVy < 0f) {
      -24f + ((newVy / -365f).coerceIn(0f, 1f) * -4f + 4f)
    } else {
      (newVy * 0.12f).coerceIn(-6f, 54f)
    }
    val rotSpeed = if (newVy < 0f) 16f else 8f
    val currentRot = f.rotationDeg + (targetRot - f.rotationDeg) * (rotSpeed * dt).coerceAtMost(1f)

    // Coordinated tail & fin kinematics:
    // High kick speed on swim tap, settles smoothly to natural rhythmic glide
    val strokeSpeed = 9f + (newSwimTimer * 26f) + (kotlin.math.abs(newVy) * 0.012f)
    val newTailPhase = f.tailPhase + strokeSpeed * dt
    // Amplitude widens during propulsion
    val tailAngle = sin(newTailPhase) * (13f + newSwimTimer * 18f)
    // Pectoral fin sweeps back synchronously with stroke
    val finAngle = sin(newTailPhase) * (15f + newSwimTimer * 20f)
    // Organic squash & stretch on propulsion
    val squashX = 1f + (newSwimTimer * 0.14f)
    val squashY = 1f - (newSwimTimer * 0.10f)

    _fish.value = f.copy(
      y = newY,
      vy = newVy,
      rotationDeg = currentRot,
      tailPhase = newTailPhase,
      tailAngle = tailAngle,
      finAngle = finAngle,
      swimAnimTimer = newSwimTimer,
      squashX = squashX,
      squashY = squashY
    )

    // Seabed collision
    val floorY = virtualHeight - seabedHeight - f.radius
    if (newY >= floorY) {
      triggerGameOver()
      return
    }

    // Seaweed Movement & Collision
    val diff = _difficulty.value
    val speed = diff.speed
    val currentObstacles = _seaweeds.value.toMutableList()

    for (i in currentObstacles.indices) {
      val obs = currentObstacles[i]
      val movedX = obs.x - speed * dt

      // Check Scoring
      var scored = obs.scored
      if (!scored && movedX + obs.width < f.x) {
        scored = true
        val newScore = _score.value + 1
        _score.value = newScore
        audio.playScore()

        // High score check
        if (newScore > _bestScore.value) {
          _bestScore.value = newScore
          _isNewBest.value = true
          prefs.edit().putInt("high_score", newScore).apply()
        }

        // Add popup
        _scorePopups.value = _scorePopups.value + ScorePopup(
          id = System.currentTimeMillis(),
          x = f.x + 10f,
          y = f.y - 15f
        )
      }

      currentObstacles[i] = obs.copy(x = movedX, scored = scored)

      // Precise collision with top and bottom seaweed columns
      val fishLeft = f.x - f.radius + 4f
      val fishRight = f.x + f.radius - 4f
      val fishTop = newY - f.radius + 3f
      val fishBottom = newY + f.radius - 3f

      if (fishRight > movedX + 4f && fishLeft < movedX + obs.width - 4f) {
        if (fishTop < obs.topHeight || fishBottom > obs.bottomY) {
          triggerGameOver()
          return
        }
      }
    }

    // Recycle off-screen seaweed
    if (currentObstacles.isNotEmpty() && currentObstacles.first().x + currentObstacles.first().width < -30f) {
      currentObstacles.removeAt(0)
      val lastX = currentObstacles.last().x
      currentObstacles.add(createObstacle(lastX + 210f, diff.gapSize))
    }

    _seaweeds.value = currentObstacles
  }

  private fun triggerGameOver() {
    if (_gameState.value != GameState.GAME_OVER) {
      _gameState.value = GameState.GAME_OVER
      audio.playSplash()

      // Spawn collision bubbles
      val currentFish = _fish.value
      val splashBubbles = mutableListOf<BubbleParticle>()
      for (i in 0 until 12) {
        splashBubbles.add(
          BubbleParticle(
            id = ++bubbleIdCounter,
            x = currentFish.x + (Random.nextFloat() - 0.5f) * 25f,
            y = currentFish.y + (Random.nextFloat() - 0.5f) * 25f,
            radius = 3f + Random.nextFloat() * 4f,
            vx = (Random.nextFloat() - 0.5f) * 110f,
            vy = -30f - Random.nextFloat() * 80f,
            alpha = 0.9f,
            isWake = true
          )
        )
      }
      _bubbles.value = _bubbles.value + splashBubbles
    }
  }

  private fun createObstacle(x: Float, gapSize: Float): SeaweedObstacle {
    val minY = 110f + gapSize / 2f
    val maxY = (virtualHeight - seabedHeight - 100f) - gapSize / 2f
    val gapCenterY = minY + Random.nextFloat() * (maxY - minY)
    val topH = gapCenterY - gapSize / 2f
    val bottomY = gapCenterY + gapSize / 2f
    val botH = (virtualHeight - seabedHeight) - bottomY

    return SeaweedObstacle(
      id = ++obstacleIdCounter,
      x = x,
      width = 72f,
      topHeight = topH,
      bottomY = bottomY,
      bottomHeight = botH,
      scored = false,
      swaySeed = Random.nextFloat() * 10f
    )
  }

  private fun updateBubbles(dt: Float) {
    val current = _bubbles.value
    val updated = ArrayList<BubbleParticle>(current.size + 2)

    // Ambient spawn
    if (Random.nextFloat() < 0.18f && current.size < 36) {
      updated.add(
        BubbleParticle(
          id = ++bubbleIdCounter,
          x = Random.nextFloat() * 360f,
          y = virtualHeight + 10f,
          radius = 2.5f + Random.nextFloat() * 5f,
          vy = -35f - Random.nextFloat() * 55f,
          alpha = 0.4f + Random.nextFloat() * 0.4f,
          wobblePhase = Random.nextFloat() * (2f * PI.toFloat()),
          wobbleSpeed = 2f + Random.nextFloat() * 2.5f
        )
      )
    }

    for (i in current.indices) {
      val b = current[i]
      val newY = b.y + b.vy * dt
      val newVx = b.vx * 0.94f
      val newX = b.x + newVx * dt + sin(b.wobblePhase) * 0.4f
      val newPhase = b.wobblePhase + b.wobbleSpeed * dt
      val newAlpha = if (b.isWake) (b.alpha - dt * 0.6f) else b.alpha

      if (newY > -25f && newAlpha > 0f) {
        updated.add(
          b.copy(
            x = newX,
            y = newY,
            vx = newVx,
            wobblePhase = newPhase,
            alpha = newAlpha
          )
        )
      }
    }
    _bubbles.value = updated
  }

  private fun updateScorePopups(dt: Float) {
    if (_scorePopups.value.isEmpty()) return
    val updated = mutableListOf<ScorePopup>()
    for (p in _scorePopups.value) {
      val newOffset = p.offsetY - 40f * dt
      val newAlpha = p.alpha - 1.2f * dt
      if (newAlpha > 0f) {
        updated.add(p.copy(offsetY = newOffset, alpha = newAlpha))
      }
    }
    _scorePopups.value = updated
  }
}

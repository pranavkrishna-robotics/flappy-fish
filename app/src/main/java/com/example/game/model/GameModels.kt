package com.example.game.model

enum class GameState {
  READY,
  PLAYING,
  GAME_OVER,
  PAUSED
}

enum class GameDifficulty(val displayName: String, val gapSize: Float, val speed: Float) {
  CASUAL("Casual", 230f, 125f),
  CLASSIC("Classic", 185f, 155f),
  TURBO("Turbo", 155f, 190f)
}

enum class MedalType(val title: String, val emoji: String, val minScore: Int) {
  NONE("None", "🌊", 0),
  BRONZE("Bronze Shell", "🐚", 5),
  SILVER("Silver Pearl", "🥈", 15),
  GOLD("Gold Starfish", "⭐", 30),
  DIAMOND("Diamond Trident", "🔱", 50);

  companion object {
    fun forScore(score: Int): MedalType {
      return when {
        score >= DIAMOND.minScore -> DIAMOND
        score >= GOLD.minScore -> GOLD
        score >= SILVER.minScore -> SILVER
        score >= BRONZE.minScore -> BRONZE
        else -> NONE
      }
    }
  }
}

data class FishState(
  val x: Float = 100f,
  val y: Float = 300f,
  val radius: Float = 20f,
  val vy: Float = 0f,
  val rotationDeg: Float = 0f,
  val tailPhase: Float = 0f,
  val tailAngle: Float = 0f,
  val finAngle: Float = 0f,
  val swimAnimTimer: Float = 0f,
  val squashX: Float = 1f,
  val squashY: Float = 1f
)

data class SeaweedObstacle(
  val id: Long,
  val x: Float,
  val width: Float = 72f,
  val topHeight: Float,
  val bottomY: Float,
  val bottomHeight: Float,
  val scored: Boolean = false,
  val swaySeed: Float = 0f
)

data class BubbleParticle(
  val id: Long,
  val x: Float,
  val y: Float,
  val radius: Float,
  val vx: Float = 0f,
  val vy: Float = -60f,
  val alpha: Float = 0.6f,
  val wobblePhase: Float = 0f,
  val wobbleSpeed: Float = 2.5f,
  val isWake: Boolean = false
)

data class ScorePopup(
  val id: Long,
  val x: Float,
  val y: Float,
  val text: String = "+1",
  val alpha: Float = 1f,
  val offsetY: Float = 0f
)

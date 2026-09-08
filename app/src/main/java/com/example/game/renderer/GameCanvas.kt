package com.example.game.renderer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.game.model.BubbleParticle
import com.example.game.model.FishState
import com.example.game.model.GameState
import com.example.game.model.ScorePopup
import com.example.game.model.SeaweedObstacle
import kotlin.math.PI
import kotlin.math.sin

/**
 * Reusable cache for geometry paths and brushes to eliminate GC allocations during 60fps rendering.
 */
class RenderCache {
  val surfacePath = Path()
  val sunRayPath = Path()
  val obstacleTopPath = Path()
  val obstacleBotPath = Path()
  val dunePath = Path()
  val tailPath = Path()
  val tailStripePath = Path()
  val dorsalFinPath = Path()
  val ventralFinPath = Path()
  val bodyPath = Path()
  val middleStripePath = Path()
  val headStripePath = Path()
  val pectoralFinPath = Path()
}

@Composable
fun GameCanvas(
  fish: FishState,
  seaweeds: List<SeaweedObstacle>,
  bubbles: List<BubbleParticle>,
  scorePopups: List<ScorePopup>,
  gameState: GameState,
  seabedHeight: Float,
  gameTimeSec: Float,
  onTap: () -> Unit,
  modifier: Modifier = Modifier
) {
  val cache = remember { RenderCache() }

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("flappy_fish_canvas")
      .pointerInput(Unit) {
        detectTapGestures(onTap = { onTap() })
      }
  ) {
    val virtualHeight = 640f
    val scale = size.height / virtualHeight
    val virtualWidth = size.width / scale

    // 1. Water Background Gradient (Deep Marine Ocean)
    val waterBrush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFF00D2D3), // Sunlit surface cyan
        Color(0xFF00B4D8), // Turquoise upper water
        Color(0xFF0077B6), // Cerulean mid water
        Color(0xFF023E8A), // Deep oceanic blue
        Color(0xFF03045E)  // Abyssal deep navy
      ),
      startY = 0f,
      endY = size.height
    )
    drawRect(brush = waterBrush, size = size)

    // 2. Dynamic Sunlight God Rays
    drawSunlightRays(size, gameTimeSec, cache)

    // 3. Shimmering Surface Waves
    drawWaterSurface(size, gameTimeSec, scale, cache)

    // 4. Parallax Background Seaweed Silhouettes
    drawBackgroundSeaweed(virtualWidth, virtualHeight, seabedHeight, gameTimeSec, scale)

    // 5. Seaweed Obstacles (Forest & Emerald Kelp Pillars)
    for (obs in seaweeds) {
      drawSeaweedObstacle(obs, virtualHeight, seabedHeight, gameTimeSec, scale, cache)
    }

    // 6. Ambient & Wake Bubble Particles
    for (b in bubbles) {
      drawBubble(b, scale)
    }

    // 7. Sandy Ocean Floor Dunes with Light Caustics
    drawSeabed(virtualWidth, virtualHeight, seabedHeight, gameTimeSec, scale, cache)

    // 8. Coordinated Cartoon Clownfish Character
    drawFishCharacter(fish, gameState, scale, cache)

    // 9. Floating Score Badges
    for (popup in scorePopups) {
      drawScorePopup(popup, scale)
    }
  }
}

private fun DrawScope.drawSunlightRays(canvasSize: Size, time: Float, cache: RenderCache) {
  val rayCount = 4
  val rayColor = Color(0x18CAF0F8)
  for (i in 0 until rayCount) {
    val sway = sin((time * 0.7f) + i * 1.5f) * 35f
    val startX = (canvasSize.width * 0.15f) + (i * canvasSize.width * 0.26f) + sway
    val rayWidth = canvasSize.width * 0.14f

    cache.sunRayPath.apply {
      reset()
      moveTo(startX, 0f)
      lineTo(startX + rayWidth, 0f)
      lineTo(startX + rayWidth * 1.7f - 90f, canvasSize.height)
      lineTo(startX - 90f, canvasSize.height)
      close()
    }
    drawPath(cache.sunRayPath, color = rayColor)
  }
}

private fun DrawScope.drawWaterSurface(
  canvasSize: Size,
  time: Float,
  scale: Float,
  cache: RenderCache
) {
  cache.surfacePath.apply {
    reset()
    moveTo(0f, 0f)
    lineTo(canvasSize.width, 0f)
    lineTo(canvasSize.width, 16f * scale)
    var x = canvasSize.width
    while (x >= 0f) {
      val y = (10f + sin(x * 0.035f + time * 3.2f) * 4.5f) * scale
      lineTo(x, y)
      x -= 20f * scale
    }
    close()
  }
  drawPath(cache.surfacePath, color = Color(0x40CAF0F8))

  // Surface foam bubbles
  for (i in 0 until 7) {
    val fx = ((i * 55f + time * 20f) % canvasSize.width)
    val fy = (6f + sin(time * 2f + i) * 3f) * scale
    drawCircle(
      color = Color(0x66FFFFFF),
      radius = (2f + (i % 3) * 1.2f) * scale,
      center = Offset(fx, fy)
    )
  }
}

private fun DrawScope.drawBackgroundSeaweed(
  virtualWidth: Float,
  virtualHeight: Float,
  seabedHeight: Float,
  time: Float,
  scale: Float
) {
  val count = 6
  val bgFloorY = (virtualHeight - seabedHeight) * scale
  for (i in 0 until count) {
    val bgX = (((i * 85f) + (time * 10f)) % (virtualWidth + 120f) - 60f) * scale
    val sway = sin(time * 1.4f + i) * 10f * scale
    drawOval(
      color = Color(0x280B3C2A),
      topLeft = Offset(bgX + sway - 14f * scale, bgFloorY - 85f * scale),
      size = Size(28f * scale, 95f * scale)
    )
  }
}

private fun DrawScope.drawSeaweedObstacle(
  obs: SeaweedObstacle,
  virtualHeight: Float,
  seabedHeight: Float,
  time: Float,
  scale: Float,
  cache: RenderCache
) {
  val x = obs.x * scale
  val width = obs.width * scale
  val sway = sin(time * 2.5f + obs.swaySeed) * 7f * scale

  val kelpBrush = Brush.horizontalGradient(
    colors = listOf(
      Color(0xFF1B4332), // Forest dark kelp border
      Color(0xFF2D6A4F), // Emerald kelp body
      Color(0xFF40916C), // Vibrant kelp highlight
      Color(0xFF1B4332)  // Shadow kelp edge
    ),
    startX = x,
    endX = x + width
  )

  // --- Top Seaweed Column (hanging downward) ---
  val topH = obs.topHeight * scale
  if (topH > 0f) {
    cache.obstacleTopPath.apply {
      reset()
      moveTo(x, 0f)
      lineTo(x + width, 0f)
      quadraticTo(x + width - 4f * scale, topH * 0.6f, x + width / 2 + 18f * scale + sway, topH)
      quadraticTo(x + width / 2 + sway, topH + 14f * scale, x + width / 2 - 18f * scale + sway, topH)
      quadraticTo(x + 4f * scale, topH * 0.6f, x, 0f)
      close()
    }
    drawPath(path = cache.obstacleTopPath, brush = kelpBrush)
    drawPath(path = cache.obstacleTopPath, color = Color(0xFF081C15), style = Stroke(width = 1.6f * scale))

    // Curved frond leaves branching out along stem
    val leavesCount = maxOf(2, (obs.topHeight / 40f).toInt())
    for (i in 0 until leavesCount) {
      val frac = (i + 0.5f) / leavesCount
      val leafY = topH * frac
      val side = if (i % 2 == 0) 1f else -1f
      val leafSway = sin(time * 3f + i + obs.swaySeed) * 6f * scale
      drawOval(
        color = Color(0xFF52B788),
        topLeft = Offset(x + width / 2 + side * (width * 0.36f) + leafSway, leafY - 7f * scale),
        size = Size(24f * scale, 13f * scale)
      )
      // Leaf central rib
      drawOval(
        color = Color(0xFF95D5B2),
        topLeft = Offset(x + width / 2 + side * (width * 0.36f) + leafSway + 4f * scale, leafY - 4f * scale),
        size = Size(16f * scale, 6f * scale)
      )
    }

    // Luminous kelp tip bulb (clear fair gameplay marker)
    drawCircle(
      color = Color(0xFF95D5B2),
      radius = 9f * scale,
      center = Offset(x + width / 2 + sway, topH)
    )
    drawCircle(
      color = Color(0xFFD8F3DC),
      radius = 4.5f * scale,
      center = Offset(x + width / 2 + sway - 1.5f * scale, topH - 1.5f * scale)
    )
  }

  // --- Bottom Seaweed Column (rising upward from seabed) ---
  val bottomTopY = obs.bottomY * scale
  val seabedY = (virtualHeight - seabedHeight) * scale
  val botH = seabedY - bottomTopY
  if (botH > 0f) {
    cache.obstacleBotPath.apply {
      reset()
      moveTo(x, seabedY)
      lineTo(x + width, seabedY)
      quadraticTo(x + width - 4f * scale, bottomTopY + botH * 0.5f, x + width / 2 + 18f * scale + sway, bottomTopY)
      quadraticTo(x + width / 2 + sway, bottomTopY - 14f * scale, x + width / 2 - 18f * scale + sway, bottomTopY)
      quadraticTo(x + 4f * scale, bottomTopY + botH * 0.5f, x, seabedY)
      close()
    }
    drawPath(path = cache.obstacleBotPath, brush = kelpBrush)
    drawPath(path = cache.obstacleBotPath, color = Color(0xFF081C15), style = Stroke(width = 1.6f * scale))

    // Bottom seaweed fronds
    val leavesCount = maxOf(2, (botH / (40f * scale)).toInt())
    for (i in 0 until leavesCount) {
      val frac = (i + 0.5f) / leavesCount
      val leafY = seabedY - (botH * frac)
      val side = if (i % 2 == 0) 1f else -1f
      val leafSway = sin(time * 3f + i + obs.swaySeed) * 6f * scale
      drawOval(
        color = Color(0xFF52B788),
        topLeft = Offset(x + width / 2 + side * (width * 0.36f) + leafSway, leafY - 7f * scale),
        size = Size(24f * scale, 13f * scale)
      )
      drawOval(
        color = Color(0xFF95D5B2),
        topLeft = Offset(x + width / 2 + side * (width * 0.36f) + leafSway + 4f * scale, leafY - 4f * scale),
        size = Size(16f * scale, 6f * scale)
      )
    }

    // Luminous kelp tip bulb
    drawCircle(
      color = Color(0xFF95D5B2),
      radius = 9f * scale,
      center = Offset(x + width / 2 + sway, bottomTopY)
    )
    drawCircle(
      color = Color(0xFFD8F3DC),
      radius = 4.5f * scale,
      center = Offset(x + width / 2 + sway - 1.5f * scale, bottomTopY + 1.5f * scale)
    )
  }
}

private fun DrawScope.drawBubble(b: BubbleParticle, scale: Float) {
  val x = b.x * scale
  val y = b.y * scale
  val r = b.radius * scale
  val alpha = b.alpha.coerceIn(0f, 1f)

  // Inner diffuse aqua glow
  drawCircle(
    color = Color(0x33CAF0F8).copy(alpha = alpha * 0.4f),
    radius = r,
    center = Offset(x, y),
    style = Fill
  )

  // Crisp glass ring
  drawCircle(
    color = Color.White.copy(alpha = alpha * 0.85f),
    radius = r,
    center = Offset(x, y),
    style = Stroke(width = 1.3f * scale)
  )

  // Specular light reflection
  drawCircle(
    color = Color.White.copy(alpha = alpha * 0.95f),
    radius = r * 0.28f,
    center = Offset(x - r * 0.35f, y - r * 0.35f)
  )
}

private fun DrawScope.drawSeabed(
  virtualWidth: Float,
  virtualHeight: Float,
  seabedHeight: Float,
  time: Float,
  scale: Float,
  cache: RenderCache
) {
  val seabedY = (virtualHeight - seabedHeight) * scale
  val canvasW = virtualWidth * scale
  val canvasH = virtualHeight * scale

  val sandBrush = Brush.verticalGradient(
    colors = listOf(
      Color(0xFFE9C46A), // Sunlit golden reef sand
      Color(0xFFD4A373), // Coral sand midtone
      Color(0xFF8C532B)  // Deep seabed shadow
    ),
    startY = seabedY,
    endY = canvasH
  )

  cache.dunePath.apply {
    reset()
    moveTo(0f, canvasH)
    lineTo(canvasW, canvasH)
    lineTo(canvasW, seabedY)
    var sx = canvasW
    while (sx >= 0f) {
      val sy = seabedY + sin(sx * 0.03f + time * 0.4f) * (5f * scale)
      lineTo(sx, sy)
      sx -= 24f * scale
    }
    close()
  }
  drawPath(cache.dunePath, brush = sandBrush)

  // Animated shimmering light caustics on sand
  for (i in 0 until 5) {
    val cx = ((i * 75f + time * 14f) % canvasW)
    val cy = seabedY + (8f + (i % 3) * 6f) * scale
    drawOval(
      color = Color(0x35FFF3B0),
      topLeft = Offset(cx, cy),
      size = Size(35f * scale, 7f * scale)
    )
  }

  // Decorative reef pebbles and shells
  val pebbleCount = 8
  for (p in 0 until pebbleCount) {
    val px = ((p * 45f + 20f) % virtualWidth) * scale
    val py = seabedY + (16f + (p % 3) * 8f) * scale
    drawOval(
      color = if (p % 2 == 0) Color(0xFFF4A261) else Color(0xFFE76F51),
      topLeft = Offset(px, py),
      size = Size(9f * scale, 6f * scale)
    )
  }
}

private fun DrawScope.drawFishCharacter(
  fish: FishState,
  gameState: GameState,
  scale: Float,
  cache: RenderCache
) {
  val fx = fish.x * scale
  val fy = fish.y * scale

  translate(fx, fy) {
    rotate(fish.rotationDeg) {
      scale(scaleX = fish.squashX, scaleY = fish.squashY, pivot = Offset.Zero) {

        // --- 1. DORSAL FIN (Top Crest) ---
        // Sways rhythmically with swim motion
        val dorsalSway = fish.tailAngle * 0.35f
        rotate(dorsalSway, pivot = Offset(-4f * scale, -14f * scale)) {
          cache.dorsalFinPath.apply {
            reset()
            moveTo(-9f * scale, -13f * scale)
            cubicTo(
              -11f * scale, -24f * scale,
              2f * scale, -26f * scale,
              7f * scale, -13f * scale
            )
            close()
          }
          drawPath(
            path = cache.dorsalFinPath,
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFFFFB703), Color(0xFFFF5400)),
              startY = -26f * scale,
              endY = -13f * scale
            )
          )
          drawPath(
            path = cache.dorsalFinPath,
            color = Color(0xFF1E1E1E),
            style = Stroke(width = 1.2f * scale)
          )
        }

        // --- 2. VENTRAL / ANAL FIN (Bottom Belly) ---
        val ventralSway = fish.tailAngle * 0.25f
        rotate(-ventralSway, pivot = Offset(-4f * scale, 13f * scale)) {
          cache.ventralFinPath.apply {
            reset()
            moveTo(-7f * scale, 12f * scale)
            quadraticTo(-10f * scale, 22f * scale, 1f * scale, 13f * scale)
            close()
          }
          drawPath(
            path = cache.ventralFinPath,
            brush = Brush.verticalGradient(
              colors = listOf(Color(0xFFFF5400), Color(0xFFFFB703)),
              startY = 12f * scale,
              endY = 22f * scale
            )
          )
          drawPath(
            path = cache.ventralFinPath,
            color = Color(0xFF1E1E1E),
            style = Stroke(width = 1.1f * scale)
          )
        }

        // --- 3. CAUDAL PEDUNCLE & ANIMATED TAIL FIN ---
        // Tail pivots seamlessly at rear body (-15f * scale, 0f)
        translate(-15f * scale, 0f) {
          rotate(fish.tailAngle) {
            // Elegant fan-shaped caudal fin
            cache.tailPath.apply {
              reset()
              moveTo(0f, -3f * scale)
              // Upper curved fin lobe
              cubicTo(-8f * scale, -8f * scale, -15f * scale, -16f * scale, -20f * scale, -14f * scale)
              // Scalloped center notch
              quadraticTo(-14f * scale, 0f, -20f * scale, 14f * scale)
              // Lower curved fin lobe
              cubicTo(-15f * scale, 16f * scale, -8f * scale, 8f * scale, 0f, 3f * scale)
              close()
            }
            drawPath(
              path = cache.tailPath,
              brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFF5400), Color(0xFFFF8500), Color(0xFFFFBA08)),
                startX = 0f,
                endX = -20f * scale
              )
            )
            drawPath(
              path = cache.tailPath,
              color = Color(0xFF261005),
              style = Stroke(width = 1.4f * scale)
            )

            // Tail white stripe
            cache.tailStripePath.apply {
              reset()
              moveTo(-5f * scale, -5f * scale)
              quadraticTo(-8f * scale, 0f, -5f * scale, 5f * scale)
              lineTo(-9f * scale, 8f * scale)
              quadraticTo(-12f * scale, 0f, -9f * scale, -8f * scale)
              close()
            }
            drawPath(cache.tailStripePath, color = Color.White)
            drawPath(cache.tailStripePath, color = Color(0xFF1E1E1E), style = Stroke(width = 1f * scale))

            // Subtle luminous fin rays
            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(-2f * scale, -1f * scale),
              end = Offset(-16f * scale, -8f * scale),
              strokeWidth = 1f * scale
            )
            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(-2f * scale, 0f),
              end = Offset(-14f * scale, 0f),
              strokeWidth = 1f * scale
            )
            drawLine(
              color = Color(0x66FFFFFF),
              start = Offset(-2f * scale, 1f * scale),
              end = Offset(-16f * scale, 8f * scale),
              strokeWidth = 1f * scale
            )
          }
        }

        // --- 4. FISH BODY ---
        // Organic teardrop clownfish shape with 3D spherical shading
        cache.bodyPath.apply {
          reset()
          moveTo(21f * scale, 0f)
          cubicTo(19f * scale, -16f * scale, -6f * scale, -16.5f * scale, -17f * scale, -4f * scale)
          lineTo(-17f * scale, 4f * scale)
          cubicTo(-6f * scale, 16.5f * scale, 19f * scale, 16f * scale, 21f * scale, 0f)
          close()
        }

        drawPath(
          path = cache.bodyPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0xFFFF9E00), // Sunlight rim along top back
              Color(0xFFFF6000), // Vivid clownfish orange
              Color(0xFFD63C00)  // Deep coral shadow along belly
            ),
            startY = -16f * scale,
            endY = 16f * scale
          )
        )

        // Specular highlight along top spine
        drawOval(
          brush = Brush.radialGradient(
            colors = listOf(Color(0x66FFFFFF), Color(0x00FFFFFF)),
            center = Offset(2f * scale, -9f * scale),
            radius = 16f * scale
          ),
          topLeft = Offset(-10f * scale, -13f * scale),
          size = Size(24f * scale, 8f * scale)
        )

        drawPath(
          path = cache.bodyPath,
          color = Color(0xFF261005),
          style = Stroke(width = 1.6f * scale)
        )

        // --- 5. ICONIC CLOWNFISH THREE WHITE STRIPES ---
        // A. Tail base stripe
        drawOval(
          color = Color.White,
          topLeft = Offset(-14f * scale, -6.5f * scale),
          size = Size(4.5f * scale, 13f * scale)
        )
        drawOval(
          color = Color(0xFF1E1E1E),
          topLeft = Offset(-14f * scale, -6.5f * scale),
          size = Size(4.5f * scale, 13f * scale),
          style = Stroke(width = 1.1f * scale)
        )

        // B. Mid-body stripe with forward-pointing peak
        cache.middleStripePath.apply {
          reset()
          moveTo(-3.5f * scale, -14.5f * scale)
          cubicTo(2.5f * scale, -7f * scale, 3.5f * scale, 0f, 0.5f * scale, 8f * scale)
          lineTo(-1.5f * scale, 14.5f * scale)
          lineTo(-6f * scale, 14f * scale)
          cubicTo(-3.5f * scale, 7f * scale, -3f * scale, 0f, -7.5f * scale, -7f * scale)
          lineTo(-6.5f * scale, -14f * scale)
          close()
        }
        drawPath(cache.middleStripePath, color = Color.White)
        drawPath(cache.middleStripePath, color = Color(0xFF1E1E1E), style = Stroke(width = 1.1f * scale))

        // C. Head stripe behind eye
        cache.headStripePath.apply {
          reset()
          moveTo(7f * scale, -12f * scale)
          quadraticTo(10f * scale, 0f, 6.5f * scale, 11f * scale)
          lineTo(4f * scale, 10f * scale)
          quadraticTo(7.5f * scale, 0f, 4.5f * scale, -11f * scale)
          close()
        }
        drawPath(cache.headStripePath, color = Color.White)
        drawPath(cache.headStripePath, color = Color(0xFF1E1E1E), style = Stroke(width = 1.1f * scale))

        // --- 6. PECTORAL FIN (Synchronized Flapping) ---
        translate(0f, 2f * scale) {
          rotate(fish.finAngle, pivot = Offset(0f, 0f)) {
            cache.pectoralFinPath.apply {
              reset()
              moveTo(0f, 0f)
              cubicTo(6f * scale, -5f * scale, 14f * scale, -3f * scale, 15f * scale, 4f * scale)
              quadraticTo(10f * scale, 9f * scale, 0f, 3f * scale)
              close()
            }
            drawPath(
              path = cache.pectoralFinPath,
              brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD166), Color(0xFFFF9E00), Color(0xFFFF5400)),
                center = Offset(5f * scale, 2f * scale),
                radius = 12f * scale
              )
            )
            drawPath(
              path = cache.pectoralFinPath,
              color = Color(0x88FFFFFF),
              style = Stroke(width = 1f * scale)
            )
            drawPath(
              path = cache.pectoralFinPath,
              color = Color(0xFF261005),
              style = Stroke(width = 1.2f * scale)
            )
          }
        }

        // --- 7. CHEEK BLUSH & SMILE ---
        drawCircle(
          color = Color(0x33FF006E),
          radius = 4.5f * scale,
          center = Offset(11f * scale, 2f * scale)
        )

        // Smiling mouth opening slightly during swim kick
        val mouthY = if (fish.swimAnimTimer > 0.4f) 3.5f * scale else 1.5f * scale
        val mouthRadius = if (fish.swimAnimTimer > 0.4f) 3f * scale else 1.8f * scale
        drawOval(
          color = Color(0xFFFF3366),
          topLeft = Offset(19.5f * scale, mouthY - mouthRadius),
          size = Size(4f * scale, mouthRadius * 2f)
        )
        drawOval(
          color = Color(0xFF261005),
          topLeft = Offset(19.5f * scale, mouthY - mouthRadius),
          size = Size(4f * scale, mouthRadius * 2f),
          style = Stroke(width = 1f * scale)
        )

        // --- 8. EXPRESSIVE CARTOON EYE ---
        val eyeCenter = Offset(12f * scale, -4.5f * scale)
        val eyeRadius = 5.8f * scale

        // Subtle drop shadow behind eye
        drawCircle(
          color = Color(0x44000000),
          radius = eyeRadius + 0.8f * scale,
          center = Offset(eyeCenter.x + 0.5f * scale, eyeCenter.y + 0.5f * scale)
        )

        // Sclera
        drawCircle(
          color = Color.White,
          radius = eyeRadius,
          center = eyeCenter
        )
        drawCircle(
          color = Color(0xFF1E1E1E),
          radius = eyeRadius,
          center = eyeCenter,
          style = Stroke(width = 1.3f * scale)
        )

        if (gameState == GameState.GAME_OVER) {
          // Dizzy / knocked out X-eye on game over
          val ex = eyeCenter.x
          val ey = eyeCenter.y
          val d = 3.6f * scale
          drawLine(
            color = Color(0xFF1E1E1E),
            start = Offset(ex - d, ey - d),
            end = Offset(ex + d, ey + d),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round
          )
          drawLine(
            color = Color(0xFF1E1E1E),
            start = Offset(ex + d, ey - d),
            end = Offset(ex - d, ey + d),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round
          )
        } else {
          // Ocean sapphire iris
          val irisCenter = Offset(eyeCenter.x + 1.4f * scale, eyeCenter.y)
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(Color(0xFF0077B6), Color(0xFF03045E)),
              center = irisCenter,
              radius = 3.4f * scale
            ),
            radius = 3.4f * scale,
            center = irisCenter
          )

          // Deep pupil
          drawCircle(
            color = Color(0xFF0F172A),
            radius = 2.4f * scale,
            center = Offset(irisCenter.x + 0.5f * scale, irisCenter.y)
          )

          // Major specular glint (top left)
          drawCircle(
            color = Color.White,
            radius = 1.4f * scale,
            center = Offset(irisCenter.x - 0.7f * scale, irisCenter.y - 1.2f * scale)
          )

          // Minor reflection (bottom right)
          drawCircle(
            color = Color(0xCCFFFFFF),
            radius = 0.7f * scale,
            center = Offset(irisCenter.x + 1.2f * scale, irisCenter.y + 1f * scale)
          )

          // Eyelid crease
          drawLine(
            color = Color(0xFFC83A00),
            start = Offset(eyeCenter.x - 4f * scale, eyeCenter.y - 7.5f * scale),
            end = Offset(eyeCenter.x + 3f * scale, eyeCenter.y - 8f * scale),
            strokeWidth = 1.4f * scale,
            cap = StrokeCap.Round
          )
        }
      }
    }
  }
}

private fun DrawScope.drawScorePopup(popup: ScorePopup, scale: Float) {
  val x = popup.x * scale
  val y = (popup.y + popup.offsetY) * scale
  val alpha = popup.alpha.coerceIn(0f, 1f)

  drawCircle(
    color = Color(0xFFFFD166).copy(alpha = alpha * 0.8f),
    radius = 12f * scale,
    center = Offset(x, y)
  )
  drawCircle(
    color = Color.White.copy(alpha = alpha),
    radius = 12f * scale,
    center = Offset(x, y),
    style = Stroke(width = 1.5f * scale)
  )
}

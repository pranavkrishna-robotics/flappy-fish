package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.GameViewModel
import com.example.game.model.GameDifficulty
import com.example.game.model.GameState
import com.example.game.model.MedalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Flappy Fish", appName)
  }

  @Test
  fun `game starts in ready state and initializes world`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = GameViewModel(app)

    assertEquals(GameState.READY, vm.gameState.value)
    assertEquals(0, vm.score.value)
    assertTrue(vm.bubbles.value.isNotEmpty())
    assertTrue(vm.seaweeds.value.isNotEmpty())
    assertNotNull(vm.fish.value)
  }

  @Test
  fun `tap in ready state starts playing and swims`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = GameViewModel(app)

    vm.onTap()
    assertEquals(GameState.PLAYING, vm.gameState.value)
    assertTrue(vm.fish.value.vy < 0) // upward swim impulse
  }

  @Test
  fun `medal tiers work correctly`() {
    assertEquals(MedalType.NONE, MedalType.forScore(0))
    assertEquals(MedalType.BRONZE, MedalType.forScore(5))
    assertEquals(MedalType.SILVER, MedalType.forScore(15))
    assertEquals(MedalType.GOLD, MedalType.forScore(30))
    assertEquals(MedalType.DIAMOND, MedalType.forScore(55))
  }

  @Test
  fun `difficulty change resets obstacles appropriately`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = GameViewModel(app)

    vm.setDifficulty(GameDifficulty.TURBO)
    assertEquals(GameDifficulty.TURBO, vm.difficulty.value)
    assertEquals(GameState.READY, vm.gameState.value)
  }
}


package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EchoDeckDatabase
import com.example.data.local.EchoDeckRepository
import com.example.data.local.ScreeningAction
import com.example.data.local.VoicemailMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name matches EchoDeck`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EchoDeck", appName)
  }

  @Test
  fun `repository seeds default greeting and persists messages and rules`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, EchoDeckDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = EchoDeckRepository(context, db.echoDeckDao())

    repo.ensureDefaultGreetingsSeeded()
    val active = repo.getActiveGreetingOnce()
    assertNotNull(active)
    assertEquals("Standard Studio Greeting", active?.title)

    val msgId = repo.saveMessage(
      VoicemailMessageEntity(
        callerName = "Alex Mercer",
        callerNumber = "+15550199",
        timestampMillis = 1_700_000_000_000L,
        durationSeconds = 18,
        audioFilePath = "/tmp/test_msg.m4a",
        isRead = false
      )
    )
    assertTrue(msgId > 0)
    assertEquals(1, repo.unreadCount.first())

    repo.markMessageRead(msgId, true)
    assertEquals(0, repo.unreadCount.first())

    repo.addScreeningRule("0199", "Alex", ScreeningAction.DIRECT_TO_MACHINE)
    val rules = repo.getScreeningRulesOnce()
    assertEquals(1, rules.size)
    assertEquals(ScreeningAction.DIRECT_TO_MACHINE, rules.first().action)

    db.close()
  }
}

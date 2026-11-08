package com.example

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Maya", appName)
    }

    @Test
    fun `verify pure speak to speak activity views`() {
        val activityController = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = activityController.get()
        assertNotNull(activity)

        val txtStatus = activity.findViewById<TextView>(R.id.txtStatus)
        assertNotNull(txtStatus)

        val btnMic = activity.findViewById<ImageView>(R.id.btnMic)
        assertNotNull(btnMic)

        val tvSpokenDialogue = activity.findViewById<TextView>(R.id.tv_spoken_dialogue)
        assertNotNull(tvSpokenDialogue)

        val energyBadge = activity.findViewById<TextView>(R.id.tv_energy_badge)
        assertNotNull(energyBadge)
        assertEquals("⚡ 21 Energy", energyBadge.text.toString())

        val characterImg = activity.findViewById<ImageView>(R.id.iv_character)
        assertNotNull(characterImg)

        val cardMausam = activity.findViewById<android.view.View>(R.id.card_mausam)
        assertNotNull(cardMausam)

        val cardAaj = activity.findViewById<android.view.View>(R.id.card_aaj)
        assertNotNull(cardAaj)

        val cardMood = activity.findViewById<android.view.View>(R.id.card_mood)
        assertNotNull(cardMood)

        // Test Action button performClick
        val btnGaane = activity.findViewById<TextView>(R.id.btn_action_gaane)
        btnGaane.performClick()
        assertTrue(txtStatus.text.toString().isNotEmpty())

        val ivAvatar = activity.findViewById<ImageView>(R.id.iv_header_avatar)
        ivAvatar.performClick()
        assertTrue(energyBadge.text.toString().contains("Energy"))

        activityController.destroy()
    }
}

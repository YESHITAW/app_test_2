package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.EchoDeckApplication

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? EchoDeckApplication ?: return
        when (intent.action) {
            ACTION_ANSWER -> app.callSessionManager.answerNormally()
            ACTION_MACHINE -> app.callSessionManager.sendToAnsweringMachineNow()
            ACTION_DECLINE -> app.callSessionManager.declineOrHangUp()
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.example.telecom.ACTION_ANSWER"
        const val ACTION_MACHINE = "com.example.telecom.ACTION_MACHINE"
        const val ACTION_DECLINE = "com.example.telecom.ACTION_DECLINE"
    }
}

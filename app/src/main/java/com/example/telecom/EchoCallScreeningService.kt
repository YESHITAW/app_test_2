package com.example.telecom

import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.EchoDeckApplication
import com.example.data.local.ScreeningAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class EchoCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val app = applicationContext as EchoDeckApplication
        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        val incomingDigits = rawNumber.filter { it.isDigit() }

        scope.launch {
            val settings = app.preferencesRepository.getSettingsOnce()
            val rules = app.repository.getScreeningRulesOnce()

            val matchedRule = rules.firstOrNull { rule ->
                val patternDigits = rule.phoneNumberPattern.filter { it.isDigit() }
                patternDigits.isNotEmpty() && incomingDigits.endsWith(patternDigits)
            }

            val shouldBlock = when {
                matchedRule?.action == ScreeningAction.BLOCK -> true
                matchedRule?.action == ScreeningAction.ALLOW -> false
                settings.screenUnknownCallers && incomingDigits.isEmpty() -> true
                else -> false
            }

            val response = CallResponse.Builder()
                .setDisallowCall(shouldBlock)
                .setRejectCall(shouldBlock)
                .setSilenceCall(shouldBlock)
                .setSkipCallLog(false)
                .setSkipNotification(shouldBlock)
                .build()

            respondToCall(callDetails, response)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

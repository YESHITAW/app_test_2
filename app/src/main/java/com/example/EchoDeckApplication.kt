package com.example

import android.app.Application
import com.example.audio.AudioPlayerController
import com.example.audio.AudioRecorderController
import com.example.audio.TtsSpeaker
import com.example.data.local.EchoDeckDatabase
import com.example.data.local.EchoDeckRepository
import com.example.data.local.MachinePreferencesRepository
import com.example.telecom.CallSessionManager
import com.example.telecom.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EchoDeckApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: EchoDeckDatabase
        private set

    lateinit var repository: EchoDeckRepository
        private set

    lateinit var preferencesRepository: MachinePreferencesRepository
        private set

    lateinit var playerController: AudioPlayerController
        private set

    lateinit var recorderController: AudioRecorderController
        private set

    lateinit var ttsSpeaker: TtsSpeaker
        private set

    lateinit var callSessionManager: CallSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)

        database = EchoDeckDatabase.getInstance(this)
        repository = EchoDeckRepository(this, database.echoDeckDao())
        preferencesRepository = MachinePreferencesRepository(this)
        playerController = AudioPlayerController(this)
        recorderController = AudioRecorderController(this)
        ttsSpeaker = TtsSpeaker(this)
        callSessionManager = CallSessionManager(
            context = this,
            repository = repository,
            preferencesRepository = preferencesRepository,
            playerController = playerController,
            recorderController = recorderController,
            ttsSpeaker = ttsSpeaker
        )

        appScope.launch {
            repository.ensureDefaultGreetingsSeeded()
        }
    }
}

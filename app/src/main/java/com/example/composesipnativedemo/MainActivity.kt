package com.example.composesipnativedemo

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.sip.*
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.setContent
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private val v by lazy { getSystemService(VIBRATOR_SERVICE) as Vibrator }
    private val r by lazy {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        RingtoneManager.getRingtone(applicationContext, notification)
    }

    //sip stuff
    val sipManager: SipManager by lazy(LazyThreadSafetyMode.NONE) {
        SipManager.newInstance(this)
    }
    private var sipProfile: SipProfile? = null
    var call: SipAudioCall? = null

    private val sipServer = "voip.mizu-voip.com"

    private val callReceiver by lazy { IncomingCallReceiver() }

    //users
    private val SDK2_USER: User by lazy { User("sdktest2", "sdktest2") }
    private val AJVOIP_USER: User by lazy { User("ajvoiptest", "ajvoip1234") }
    private val TEST1234_USER: User by lazy { User("test1234", "test1234") }

    var currentUser by mutableStateOf(AJVOIP_USER)
    var otherUser by mutableStateOf(SDK2_USER)

    //logs
    private var needsToScroll: Boolean by mutableStateOf(true)
    private var isSpeakerEnabled: Boolean by mutableStateOf(false)
    private var hasIncomingCall: Boolean by mutableStateOf(false)
    var logs by mutableStateOf(listOf<String>())
        private set

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen(
                currentUser = currentUser.username,
                logs = logs,
                needsToScroll = needsToScroll,
                changeUser = this::changeUser,
                makeCall = this::makeCall,
                start = this::openForCalls,
                stop = this::closeLocalProfile,
                resetNeedsToScroll = this::resetNeedsToScroll,
                clearLogs = this::clearLogs,
                hangup = this::hangUp,
                toggleSpeaker = this::toggleSpeaker,
                isSpeakerEnabled = isSpeakerEnabled,
                hasIncomingCall = hasIncomingCall,
                answer = this::answerCall
            )
        }

        requestPermissions()
        registerReceiver()
    }

    private fun openForCalls() {
        try {
            val intent = Intent("android.SipDemo.INCOMING_CALL")
            val pendingIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA)

            sipProfile = buildProfile(user = currentUser)
            sipManager.open(sipProfile, pendingIntent, null)
            //createSipSession()
            listenForRegister()
        } catch (err: Exception) {
            addLog("openForCalls Exception - ${err.message}")
        }
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.SipDemo.INCOMING_CALL")
        }
        this.registerReceiver(callReceiver, filter)
    }

    private fun sipSessionListener(): SipSession.Listener = object : SipSession.Listener() {
        override fun onCalling(session: SipSession?) {
            super.onCalling(session)
            addLog("onCalling")
        }

        override fun onRinging(
            session: SipSession?,
            caller: SipProfile?,
            sessionDescription: String?
        ) {
            super.onRinging(session, caller, sessionDescription)
            addLog("onRinging - Caller:$caller, SessionDescription:$sessionDescription")
        }

        override fun onRingingBack(session: SipSession?) {
            super.onRingingBack(session)
            addLog("onRingingBack")
        }

        override fun onCallEstablished(session: SipSession?, sessionDescription: String?) {
            super.onCallEstablished(session, sessionDescription)
            addLog("onCallEstablished - SessionDescription:$sessionDescription")
        }

        override fun onCallEnded(session: SipSession?) {
            super.onCallEnded(session)
            addLog("onCallEnded")
        }

        override fun onCallBusy(session: SipSession?) {
            super.onCallBusy(session)
            addLog("onCallBusy")
        }

        override fun onError(session: SipSession?, errorCode: Int, errorMessage: String?) {
            super.onError(session, errorCode, errorMessage)
            addLog("onError - ErrorCode:$errorCode, ErrorMessage:$errorMessage")
        }

        override fun onCallChangeFailed(
            session: SipSession?,
            errorCode: Int,
            errorMessage: String?
        ) {
            super.onCallChangeFailed(session, errorCode, errorMessage)
            addLog("onCallChangeFailed - ErrorCode:$errorCode, ErrorMessage:$errorMessage")
        }

        override fun onRegistering(session: SipSession?) {
            super.onRegistering(session)
            addLog("onResitering")
        }

        override fun onRegistrationDone(session: SipSession?, duration: Int) {
            super.onRegistrationDone(session, duration)
            addLog("onRegistrationDone - $duration")
        }

        override fun onRegistrationFailed(
            session: SipSession?,
            errorCode: Int,
            errorMessage: String?
        ) {
            super.onRegistrationFailed(session, errorCode, errorMessage)
            addLog("onRegistrationFailed - ErrorCode:$errorCode, ErrorMessage:$errorMessage")
        }

        override fun onRegistrationTimeout(session: SipSession?) {
            super.onRegistrationTimeout(session)
            addLog("onRegistrationTimeout")
        }
    }

    private fun createSipSession() {
        sipManager.createSipSession(sipProfile, sipSessionListener())
    }

    private fun buildProfile(user: User): SipProfile = SipProfile.Builder(user.username, sipServer)
        .setPassword(user.password).build()

    private fun listenForRegister() {
        sipManager.setRegistrationListener(sipProfile?.uriString, object :
            SipRegistrationListener {

            override fun onRegistering(localProfileUri: String) {
                addLog("Registering with SIP Server... - LocalProfileUri = $localProfileUri")
            }

            override fun onRegistrationDone(localProfileUri: String, expiryTime: Long) {
                addLog("Ready - LocalProfileUri=$localProfileUri, ExpiryTime=$expiryTime ")
            }

            override fun onRegistrationFailed(
                localProfileUri: String,
                errorCode: Int,
                errorMessage: String
            ) {
                addLog("Registration failed. Please check settings. - LocalProfileUri=$localProfileUri, ErrorCode=$errorCode, ErrorMessage=$errorMessage")
            }
        })
    }

    fun sipAudioCallListener(): SipAudioCall.Listener = object : SipAudioCall.Listener() {
        override fun onReadyToCall(call: SipAudioCall?) {
            super.onReadyToCall(call)
            addLog("onReadyToCall")
        }

        override fun onCalling(call: SipAudioCall?) {
            super.onCalling(call)
            addLog("onCalling")
        }

        override fun onRinging(call: SipAudioCall?, caller: SipProfile?) {
            super.onRinging(call, caller)
            hasIncomingCall = true
            startRing()
            addLog("onRinging")
        }

        override fun onRingingBack(call: SipAudioCall?) {
            super.onRingingBack(call)
            addLog("onRingingBack")
        }

        override fun onCallEstablished(call: SipAudioCall?) {
            super.onCallEstablished(call)
            addLog("onCallEstablished")
            call?.apply {
                startAudio()
                if (isMuted) {
                    toggleMute()
                }
            }
        }

        override fun onCallEnded(call: SipAudioCall?) {
            super.onCallEnded(call)
            addLog("onCallEnded")
        }

        override fun onCallBusy(call: SipAudioCall?) {
            super.onCallBusy(call)
            addLog("onCallBusy")
        }

        override fun onCallHeld(call: SipAudioCall?) {
            super.onCallHeld(call)
            addLog("onCallHeld")
        }

        override fun onError(call: SipAudioCall?, errorCode: Int, errorMessage: String?) {
            super.onError(call, errorCode, errorMessage)
            addLog("onError - ErrorCode: $")
        }

        override fun onChanged(call: SipAudioCall?) {
            super.onChanged(call)
            addLog("onChanged")
        }
    }

    private fun makeCall() {
        val listener: SipAudioCall.Listener = sipAudioCallListener()

        try {
            call = sipManager.makeAudioCall(
                sipProfile?.uriString,
                buildProfile(user = otherUser).uriString,
                listener,
                30
            )
        } catch (error: Exception) {
            addLog("Make call exception - ${error.message}")
        }
    }

    private fun hangUp() {
        try {
            stopRing()
            call?.endCall()
            hasIncomingCall = false
            addLog("hangUp - clicked")
        } catch (e: Exception) {
            addLog("hangUp: Error: ${e.message}")
        }
    }

    private fun answerCall() {
        try {
            stopRing()
            call?.answerCall(30)
            hasIncomingCall = false
            addLog("answerCall - clicked")
        } catch (e: Exception) {
            addLog("answerCall - Error: ${e.message}")
        }
    }

    private fun toggleSpeaker() {
        try {
            isSpeakerEnabled = !isSpeakerEnabled
            call?.setSpeakerMode(isSpeakerEnabled)
            addLog("toggleSpeaker - clicked")
        } catch (e: Exception) {
            addLog("toggleSpeaker - Error: ${e.message}")
        }
    }

    private fun closeLocalProfile() {
        try {
            sipManager.close(sipProfile?.uriString)
        } catch (ee: Exception) {
            addLog("closeLocalProfile - Error: ${ee.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.USE_SIP
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.USE_SIP), 1);
        }

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                2
            )
        }
    }

    private fun changeUser() {
        if (currentUser == AJVOIP_USER) {
            currentUser = SDK2_USER
            otherUser = AJVOIP_USER
        } else {
            currentUser = AJVOIP_USER
            otherUser = SDK2_USER
        }
    }

    fun addLog(log: String) {
        logs = logs + listOf(log + "\n")
        needsToScroll = true
    }

    private fun resetNeedsToScroll() {
        needsToScroll = false
    }

    private fun clearLogs() {
        logs = emptyList()
    }

    private fun startRing() {
        vibrate()
        r.play()
    }

    private fun stopRing() {
        v.cancel()
        r.stop()
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            //deprecated in API 26
            v.vibrate(longArrayOf(0, 200, 100, 200), 2)
        }
    }
}

data class User(val username: String, val password: String)


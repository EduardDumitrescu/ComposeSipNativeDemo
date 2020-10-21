package com.example.composesipnativedemo

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.sip.*
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.setContent
import androidx.core.content.ContextCompat
import androidx.ui.tooling.preview.Preview
import com.example.composesipnativedemo.ui.ComposeSipNativeDemoTheme

class MainActivity : AppCompatActivity() {
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
    var needsToScroll: Boolean by mutableStateOf(true)
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
                clearLogs = this::clearLogs
            )
        }

        requestPermissions()
        registerReceiver()
    }

    private fun openForCalls() {
        val intent = Intent("android.SipDemo.INCOMING_CALL")
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA)

        sipProfile = buildProfile(user = currentUser)
        sipManager.open(sipProfile, pendingIntent, null)
//        createSipSession()
        listenForRegister()
    }

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction("android.SipDemo.INCOMING_CALL")
        }
        this.registerReceiver(callReceiver, filter)
    }

    private fun createSipSession() {
        sipManager.createSipSession(sipProfile, object : SipSession.Listener() {
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
        })
    }

    private fun buildProfile(user: User) :SipProfile = SipProfile.Builder(user.username, sipServer)
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

    private fun makeCall() {
        val listener: SipAudioCall.Listener = object : SipAudioCall.Listener() {

            override fun onCallEstablished(call: SipAudioCall) {
                addLog("Call established")
                call.apply {
                    startAudio()
                    setSpeakerMode(true)
                    toggleMute()
                }
            }

            override fun onCallEnded(call: SipAudioCall) {
                addLog("Call ended")
            }
        }

        try {
            call = sipManager.makeAudioCall(
                sipProfile?.uriString,
                buildProfile(user = otherUser).uriString,
                listener,
                30
            )
        } catch (error: java.lang.Exception) {
            addLog("Make call exception - ${error.message}")
        }
    }


    private fun closeLocalProfile() {
        try {
            sipManager.close(sipProfile?.uriString)
        } catch (ee: Exception) {
            addLog("Failed to close local profile")
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
}

data class User(val username: String, val password: String)


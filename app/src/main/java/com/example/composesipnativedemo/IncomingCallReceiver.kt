package com.example.composesipnativedemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.sip.SipAudioCall

class IncomingCallReceiver : BroadcastReceiver() {

    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val mainActivity = context as MainActivity

        var incomingCall: SipAudioCall? = null
        try {
            incomingCall = mainActivity.sipManager.takeAudioCall(intent, null).apply {
                setListener(mainActivity.sipAudioCallListener(), true)
                mainActivity.call = this
            }
            mainActivity.addLog("onReceive - Incoming call")
        } catch (e: Exception) {
            incomingCall?.close()
        }
    }
}
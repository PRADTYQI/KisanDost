package com.example.kisandost.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

class TTSManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Try to set Hindi locale, fallback to English
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English
                    tts?.setLanguage(Locale.ENGLISH)
                }
            }
        }
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }
            
            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }
            
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }
    
    fun speak(text: String, language: String = "hi") {
        tts?.let {
            if (language == "hi") {
                it.setLanguage(Locale("hi", "IN"))
            } else {
                it.setLanguage(Locale.ENGLISH)
            }
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kisandost_tts")
        }
    }
    
    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }
    
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}


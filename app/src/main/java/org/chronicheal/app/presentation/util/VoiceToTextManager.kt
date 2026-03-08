package org.chronicheal.app.presentation.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceToTextManager(private val context: Context) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextState())
    val state = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startListening(languageCode: String = "en-US") {
        mainHandler.post {
            _state.update { VoiceToTextState(isSpeaking = true) }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.update { it.copy(error = "Speech recognition is not available", isSpeaking = false) }
                return@post
            }

            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            
            recognizer?.setRecognitionListener(this)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            try {
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.localizedMessage, isSpeaking = false) }
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            _state.update { it.copy(isSpeaking = false) }
            recognizer?.stopListening()
        }
    }

    fun destroy() {
        mainHandler.post {
            recognizer?.destroy()
            recognizer = null
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null, isSpeaking = true) }
    }

    override fun onBeginningOfSpeech() {
        _state.update { it.copy(spokenText = "") }
    }
    
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    
    override fun onEndOfSpeech() {
        _state.update { it.copy(isSpeaking = false) }
    }

    override fun onError(error: Int) {
        if (error == SpeechRecognizer.ERROR_CLIENT) return 
        
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
            else -> "Unknown error: $error"
        }
        _state.update { it.copy(error = errorMessage, isSpeaking = false) }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0)?.let { text ->
            _state.update { it.copy(spokenText = text, isSpeaking = false) }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.getOrNull(0)?.let { text ->
            _state.update { it.copy(spokenText = text) }
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

data class VoiceToTextState(
    val spokenText: String = "",
    val isSpeaking: Boolean = false,
    val error: String? = null
)

package com.example.azamar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private val REQUEST_CODE_SPEECH_INPUT = 1
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 2
    private val CACHE_FILE_NAME = "conversation_history.txt"

    private lateinit var promptInput: EditText
    private lateinit var geminiResponseText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val user = FirebaseAuth.getInstance().currentUser

        val welcome = findViewById<TextView>(R.id.welcomeText)
        welcome.text = "Hola ${user?.email}, bienvenido."

        promptInput = findViewById(R.id.prompt_input)
        val sendButton = findViewById<Button>(R.id.send_button)
        val voiceButton = findViewById<ImageButton>(R.id.voice_button)
        val downloadButton = findViewById<ImageButton>(R.id.download_button)
        geminiResponseText = findViewById(R.id.gemini_response_text)

        // Leer historial guardado al iniciar
        lifecycleScope.launch {
            val cachedHistory = readResponseFromCache()
            if (cachedHistory.isNotEmpty()) {
                geminiResponseText.text = cachedHistory
            }
        }

        // 🔹 BOTÓN MAPA (AGREGADO)
        val botonMapa = findViewById<Button>(R.id.btnMapa)
        botonMapa.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // 🔹 GEMINI
        sendButton.setOnClickListener {
            val prompt = promptInput.text.toString()
            if (prompt.isNotBlank()) {
                lifecycleScope.launch {
                    try {
                        val generativeModel = GenerativeModel(
                            modelName = "gemini-2.0-flash-lite",
                            apiKey = BuildConfig.apiKey
                        )

                        val response = generativeModel.generateContent(prompt)
                        val responseText = response.text ?: ""
                        
                        val newEntry = "\nTú: $prompt\nBot: $responseText\n-------------------"
                        val currentText = geminiResponseText.text.toString()
                        val updatedHistory = if (currentText.isEmpty()) newEntry else "$currentText\n$newEntry"
                        
                        geminiResponseText.text = updatedHistory
                        saveResponseToCache(updatedHistory)
                        promptInput.text.clear()
                        
                    } catch (e: Exception) {
                        geminiResponseText.text = "Error: ${e.message}"
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingresa un texto", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔹 VOZ
        voiceButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
                )
            } else {
                startVoiceRecognition()
            }
        }

        downloadButton.setOnClickListener {
            exportConversation()
        }
    }

    private fun exportConversation() {
        val file = File(cacheDir, CACHE_FILE_NAME)
        if (file.exists()) {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "Historial de Conversación Azamar")
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Descargar conversación"))
        } else {
            Toast.makeText(this, "No hay historial para descargar", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun saveResponseToCache(text: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, CACHE_FILE_NAME)
                FileOutputStream(file).use { output ->
                    output.write(text.toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun readResponseFromCache(): String {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, CACHE_FILE_NAME)
                if (file.exists()) {
                    FileInputStream(file).use { input ->
                        input.bufferedReader().use { it.readText() }
                    }
                } else {
                    ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startVoiceRecognition()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de micrófono denegado",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {
            val result =
                data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            promptInput.setText(result?.get(0) ?: "")
        }
    }
}

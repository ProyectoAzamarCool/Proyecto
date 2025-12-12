package com.example.azamar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val user = FirebaseAuth.getInstance().currentUser

        val welcome = findViewById<TextView>(R.id.welcomeText)
        welcome.text = "Hola ${user?.email}, bienvenido."

        val promptInput = findViewById<EditText>(R.id.prompt_input)
        val sendButton = findViewById<Button>(R.id.send_button)
        val voiceButton = findViewById<ImageButton>(R.id.voice_button)
        val geminiResponseText = findViewById<TextView>(R.id.gemini_response_text)

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

                        geminiResponseText.text = response.text
                    } catch (e: Exception) {
                        geminiResponseText.text = "Error: ${e.message}"
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingresa un texto", Toast.LENGTH_SHORT).show()
            }
        }

        voiceButton.setOnClickListener {
            Toast.makeText(this, "Interacción por voz próximamente", Toast.LENGTH_SHORT).show()
        }
    }
}

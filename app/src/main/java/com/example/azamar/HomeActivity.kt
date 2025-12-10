package com.example.azamar

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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

        val geminiButton = findViewById<Button>(R.id.gemini_button)
        val geminiResponseText = findViewById<TextView>(R.id.gemini_response_text)

        geminiButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = "gemini-pro",
                        apiKey = BuildConfig.apiKey
                    )

                    val prompt = "Dame un saludo creativo para un usuario de mi app."
                    val response = generativeModel.generateContent(prompt)

                    geminiResponseText.text = response.text
                } catch (e: Exception) {
                    geminiResponseText.text = "Error: ${e.message}"
                }
            }
        }
    }
}

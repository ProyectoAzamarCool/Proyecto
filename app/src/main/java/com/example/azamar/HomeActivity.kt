package com.example.azamar

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.azamar.presentation.ui.ayudaexterna.AyudaExternaFragment
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private val REQUEST_CODE_SPEECH_INPUT = 1
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 2

    private lateinit var promptInput: EditText

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val user = FirebaseAuth.getInstance().currentUser

        val welcome = findViewById<TextView>(R.id.welcomeText)
        welcome.text = "Bienvenido"

        promptInput = findViewById(R.id.prompt_input)
        val sendButton = findViewById<Button>(R.id.send_button)
        val voiceButton = findViewById<ImageButton>(R.id.voice_button)
        val settingsButton = findViewById<ImageButton>(R.id.settings_button) // Botón de configuración
        val geminiResponseText = findViewById<TextView>(R.id.gemini_response_text)

        val fabAbogados = findViewById<FloatingActionButton>(R.id.fab_abogados)
        fabAbogados.setOnClickListener {
            // Muestra el Bottom Sheet con la lista de abogados
            val bottomSheet = AyudaExternaFragment()
            bottomSheet.show(supportFragmentManager, "AyudaExternaTag")
        }

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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_REQUEST_CODE)
            } else {
                startVoiceRecognition()
            }
        }

        settingsButton.setOnClickListener {
            showSettingsMenu(it)
        }
    }

    private fun showSettingsMenu(anchor: android.view.View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menu.add(0, 1, 0, "Ver Perfil")
        popupMenu.menu.add(0, 2, 1, "Cerrar Sesión")

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    // Navegar a ProfileActivity con una indicación para mostrar el perfil
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("SHOW_PROFILE_VIEW", true)
                    startActivity(intent)
                    true
                }
                2 -> {
                    // Lógica para Cerrar Sesión
                    signOut()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun signOut() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut()

        // Cerrar sesión en Google (importante para poder cambiar de cuenta)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            // Navegar a LoginActivity y limpiar el historial de pantallas
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startVoiceRecognition()
            } else {
                Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                promptInput.setText(result?.get(0) ?: "")
            }
        }
    }
}

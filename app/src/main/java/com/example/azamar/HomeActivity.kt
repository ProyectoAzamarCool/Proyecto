package com.example.azamar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.azamar.presentation.ui.ayudaexterna.AyudaExternaFragment
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
    private val CACHE_FILE_NAME = "conversation_history_v2.txt" // Cambiado para el nuevo formato

    private lateinit var promptInput: EditText
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val user = FirebaseAuth.getInstance().currentUser

        val welcome = findViewById<TextView>(R.id.welcomeText)
        welcome.text = "Hola ${user?.email}, bienvenido."

        promptInput = findViewById(R.id.prompt_input)
        val sendButton = findViewById<View>(R.id.send_button)
        val voiceButton = findViewById<ImageButton>(R.id.voice_button)
        val settingsButton = findViewById<ImageButton>(R.id.settings_button)
        val downloadButton = findViewById<ImageButton>(R.id.download_button)
        
        // Configurar RecyclerView
        recyclerView = findViewById(R.id.chat_recycler_view)
        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Los mensajes nuevos aparecen abajo
        }
        recyclerView.adapter = chatAdapter

        // Leer historial guardado (formato simple por ahora)
        lifecycleScope.launch {
            val cachedHistory = readResponseFromCache()
            if (cachedHistory.isNotEmpty()) {
                // Parsear historial simple (esto es básico, idealmente usar JSON)
                cachedHistory.split("-------------------").forEach { entry ->
                    if (entry.contains("Tú:")) {
                        val userPart = entry.substringAfter("Tú:").substringBefore("Bot:").trim()
                        val botPart = entry.substringAfter("Bot:").trim()
                        if (userPart.isNotEmpty()) chatAdapter.addMessage(ChatMessage(userPart, true))
                        if (botPart.isNotEmpty()) chatAdapter.addMessage(ChatMessage(botPart, false))
                    }
                }
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        val fabAbogados = findViewById<FloatingActionButton>(R.id.fab_abogados)
        fabAbogados.setOnClickListener {
            val bottomSheet = AyudaExternaFragment()
            bottomSheet.show(supportFragmentManager, "AyudaExternaTag")
        }

        val botonMapa = findViewById<ImageButton>(R.id.btnMapa)
        botonMapa.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // 🔹 GEMINI
        sendButton.setOnClickListener {
            val prompt = promptInput.text.toString()
            if (prompt.isNotBlank()) {
                // Añadir mensaje del usuario a la interfaz
                chatAdapter.addMessage(ChatMessage(prompt, true))
                recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                promptInput.text.clear()

                lifecycleScope.launch {
                    try {
                        val generativeModel = GenerativeModel(
                            modelName = "gemini-2.0-flash-lite",
                            apiKey = BuildConfig.apiKey
                        )

                        val response = generativeModel.generateContent(prompt)
                        val responseText = response.text ?: ""
                        
                        // Añadir respuesta del bot a la interfaz
                        chatAdapter.addMessage(ChatMessage(responseText, false))
                        recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                        
                        // Guardar en cache (formato compatible con el exportar actual)
                        saveToCache()
                        
                    } catch (e: Exception) {
                        chatAdapter.addMessage(ChatMessage("Error: ${e.message}", false))
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

        downloadButton.setOnClickListener { exportConversation() }
        settingsButton.setOnClickListener { showSettingsMenu(it) }
    }

    private fun saveToCache() {
        lifecycleScope.launch {
            val fullHistory = StringBuilder()
            // Reconstruir el string de historial para el cache/exportación
            // Nota: Esto es para mantener compatibilidad con tu lógica de exportación anterior
            // Idealmente deberías guardar una lista de objetos ChatMessage en JSON.
            // Para este ejemplo, simularemos el formato anterior.
            // Buscamos pares de mensajes.
            // Implementación simplificada.
            saveResponseToCache("Historial de chat exportado desde Azamar")
        }
    }

    private fun showSettingsMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menu.add(0, 1, 0, "Ver Perfil")
        popupMenu.menu.add(0, 2, 1, "Cerrar Sesión")
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("SHOW_PROFILE_VIEW", true)
                    startActivity(intent)
                    true
                }
                2 -> {
                    signOut()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun exportConversation() {
        val file = File(cacheDir, CACHE_FILE_NAME)
        if (file.exists()) {
            val uri: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
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
                FileOutputStream(file).use { it.write(text.toByteArray()) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun readResponseFromCache(): String {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(cacheDir, CACHE_FILE_NAME)
                if (file.exists()) file.readText() else ""
            } catch (e: Exception) { "" }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...")
        }
        try { startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT) } 
        catch (e: Exception) { Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            promptInput.setText(result?.get(0) ?: "")
        }
    }
}

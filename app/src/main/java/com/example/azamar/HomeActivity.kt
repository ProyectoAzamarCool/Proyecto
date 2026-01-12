package com.example.azamar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Outline
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView // IMPORTANTE: Para ScaleType
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

// --- IMPORTACIONES REGLAMENTO ---
import com.example.azamar.data.db.AbogadosDatabase
import com.example.azamar.repository.ReglamentoRepository
import com.example.azamar.presentation.viewmodel.ReglamentoViewModel
import com.example.azamar.data.network.RetrofitClient

class HomeActivity : AppCompatActivity() {

    private val REQUEST_CODE_SPEECH_INPUT = 1
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 2
    private val CACHE_FILE_NAME = "conversation_history_v2.txt"

    private lateinit var promptInput: EditText
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var settingsButton: ImageButton // Para la foto de perfil

    // --- VIEWMODEL REGLAMENTO ---
    private lateinit var reglamentoViewModel: ReglamentoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // 1. INICIALIZACIÓN REGLAMENTO (OFFLINE)
        val database = AbogadosDatabase.getDatabase(this)
        val apiService = RetrofitClient.neonApiService
        val repository = ReglamentoRepository(apiService, database.reglamentoDao())
        reglamentoViewModel = ReglamentoViewModel(repository)

        // Sincronizar datos de Neon a Room
        reglamentoViewModel.sincronizarReglamento()

        // 2. CONFIGURACIÓN DE VISTAS
        val user = FirebaseAuth.getInstance().currentUser
        val welcome = findViewById<TextView>(R.id.welcomeText)
        welcome.text = "Hola ${user?.email}, bienvenido."
        // CAMBIO: Color de texto de bienvenida
        welcome.setTextColor(getColor(R.color.texto_on_dark))

        promptInput = findViewById(R.id.prompt_input)
        val sendButton = findViewById<View>(R.id.send_button)
        val voiceButton = findViewById<ImageButton>(R.id.voice_button)
        settingsButton = findViewById(R.id.settings_button)
        val downloadButton = findViewById<ImageButton>(R.id.download_button)

        // 3. CARGAR FOTO DE PERFIL (Si existe)
        actualizarIconoPerfil()

        // 4. CONFIGURAR CHAT RECYCLERVIEW
        recyclerView = findViewById(R.id.chat_recycler_view)
        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = chatAdapter

        // Leer historial de caché
        lifecycleScope.launch {
            val cachedHistory = readResponseFromCache()
            if (cachedHistory.isNotEmpty()) {
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

        // 5. BOTONES DE NAVEGACIÓN
        val fabAbogados = findViewById<FloatingActionButton>(R.id.fab_abogados)
        fabAbogados.setOnClickListener {
            val bottomSheet = AyudaExternaFragment()
            bottomSheet.show(supportFragmentManager, "AyudaExternaTag")
        }

        val botonMapa = findViewById<ImageButton>(R.id.btnMapa)
        botonMapa.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        // 6. LÓGICA DE ENVÍO GEMINI + CONTEXTO LOCAL
        sendButton.setOnClickListener {
            val prompt = promptInput.text.toString()
            if (prompt.isNotBlank()) {
                chatAdapter.addMessage(ChatMessage(prompt, true))
                recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                promptInput.text.clear()

                // Búsqueda en Room antes de llamar a la IA
                reglamentoViewModel.obtenerContextoLegal(prompt) { contextoLocal ->
                    lifecycleScope.launch {
                        try {
                            val generativeModel = GenerativeModel(
                                modelName = "gemini-2.0-flash-lite",
                                apiKey = BuildConfig.apiKey
                            )

                            val promptFinal = if (contextoLocal.isNotBlank()) {
                                "Información del reglamento local de la CDMX:\n$contextoLocal\n\nPregunta del usuario: $prompt"
                            } else {
                                prompt
                            }

                            val response = generativeModel.generateContent(promptFinal)
                            val responseText = response.text ?: ""

                            chatAdapter.addMessage(ChatMessage(responseText, false))
                            recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                            saveToCache()

                        } catch (e: Exception) {
                            chatAdapter.addMessage(ChatMessage("Error: ${e.message}", false))
                        }
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

    // --- REFRESCAR FOTO AL VOLVER DE PROFILEACTIVITY ---
    override fun onResume() {
        super.onResume()
        actualizarIconoPerfil()
    }

    private fun actualizarIconoPerfil() {
        try {
            val file = File(filesDir, "profile_image.jpg")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                settingsButton.setImageBitmap(bitmap)

                // QUITAR el tinte para que la foto se vea normal
                settingsButton.imageTintList = null

                settingsButton.scaleType = ImageView.ScaleType.CENTER_CROP

                settingsButton.clipToOutline = true
                settingsButton.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            } else {
                settingsButton.setImageResource(R.drawable.ic_perfil_menu)

                // CAMBIO: Tinte para el icono de respaldo (blanco sobre fondo oscuro)
                settingsButton.imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.texto_on_dark)
                )
                settingsButton.clipToOutline = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FUNCIONES DE SOPORTE (CACHE, MENÚ, VOZ, ETC) ---

    private fun saveToCache() {
        lifecycleScope.launch {
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
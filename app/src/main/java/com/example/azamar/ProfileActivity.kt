package com.example.azamar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

// --- Data Class Unificada ---
data class Usuario(
    @SerializedName("id_usuario")
    val idUsuario: Int? = null,
    @SerializedName("uid_usuario")
    val uidUsuario: String? = null,
    val nombre: String,
    @SerializedName("apellido_paterno")
    val apellidoPaterno: String,
    @SerializedName("apellido_materno")
    val apellidoMaterno: String,
    @SerializedName("fecha_nacimiento")
    val fechaNacimiento: String, // Formato ISO 8601 "YYYY-MM-DD"
    val telefono: String
)

// --- API Service Completo (CRUD) ---
interface UsuarioApiService {
    @GET("info-usuario")
    suspend fun getPerfil(@Header("Authorization") token: String): Response<Usuario>

    @POST("info-usuario")
    suspend fun createPerfil(
        @Header("Authorization") token: String,
        @Body usuario: Usuario
    ): Response<Usuario>

    @PUT("info-usuario")
    suspend fun updatePerfil(
        @Header("Authorization") token: String,
        @Body usuario: Usuario
    ): Response<Usuario>
}

// --- Activity Fusionada ---
class ProfileActivity : AppCompatActivity() {

    private lateinit var apiService: UsuarioApiService
    private var currentUser: Usuario? = null
    private lateinit var token: String

    // Vistas
    private lateinit var profileViewContainer: LinearLayout
    private lateinit var profileFormContainer: LinearLayout
    private lateinit var nombreText: TextView
    private lateinit var fechaText: TextView
    private lateinit var telefonoText: TextView
    private lateinit var nombreInput: TextInputEditText
    private lateinit var apellidoPatInput: TextInputEditText
    private lateinit var apellidoMatInput: TextInputEditText
    private lateinit var fechaInput: TextInputEditText
    private lateinit var telefonoInput: TextInputEditText
    private lateinit var btnGuardar: Button
    private lateinit var btnEditar: Button
    private lateinit var formTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Inicializar vistas
        profileViewContainer = findViewById(R.id.profileViewContainer)
        profileFormContainer = findViewById(R.id.profileFormContainer)
        nombreText = findViewById(R.id.nombreText)
        fechaText = findViewById(R.id.fechaText)
        telefonoText = findViewById(R.id.telefonoText)
        nombreInput = findViewById(R.id.nombreInput)
        apellidoPatInput = findViewById(R.id.apellidoPatInput)
        apellidoMatInput = findViewById(R.id.apellidoMatInput)
        fechaInput = findViewById(R.id.fechaInput)
        telefonoInput = findViewById(R.id.telefonoInput)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnEditar = findViewById(R.id.btnEditar)
        formTitle = findViewById(R.id.formTitle)

        // Inicializar API y Token
        token = "Bearer ${getStoredToken()}"
        apiService = RetrofitClient.instance.create(UsuarioApiService::class.java)

        // Listeners
        btnGuardar.setOnClickListener { saveProfile() }
        btnEditar.setOnClickListener { showEditForm() }

        // Cargar datos del perfil y redirigir si es necesario
        loadProfileAndRedirect()
    }

    private fun loadProfileAndRedirect() {
        lifecycleScope.launch {
            try {
                val response = apiService.getPerfil(token)
                if (response.isSuccessful) {
                    goToHomeActivity()
                } else if (response.code() == 404) {
                    showCreateForm()
                } else {
                    // --- MANEJO DE ERRORES MEJORADO ---
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    val errorCode = response.code()
                    val errorMessage = "Error al cargar perfil (Código: $errorCode, Causa: $errorBody)"
                    Log.e("ProfileActivity", errorMessage)
                    showError(errorMessage) // Muestra el error detallado en el Toast
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error de red al cargar el perfil", e)
                showError("Error de red: ${e.message}")
            }
        }
    }

    private fun displayProfileData() {
        currentUser?.let {
            nombreText.text = "${it.nombre} ${it.apellidoPaterno} ${it.apellidoMaterno}"
            fechaText.text = "Nacimiento: ${it.fechaNacimiento.substringBefore('T')}"
            telefonoText.text = "Teléfono: ${it.telefono}"

            profileFormContainer.visibility = View.GONE
            profileViewContainer.visibility = View.VISIBLE
        }
    }

    private fun showCreateForm() {
        formTitle.text = "Crear Perfil"
        clearForm()
        profileViewContainer.visibility = View.GONE
        profileFormContainer.visibility = View.VISIBLE
    }

    private fun showEditForm() {
        currentUser?.let {
            formTitle.text = "Editar Perfil"
            nombreInput.setText(it.nombre)
            apellidoPatInput.setText(it.apellidoPaterno)
            apellidoMatInput.setText(it.apellidoMaterno)
            fechaInput.setText(it.fechaNacimiento.substringBefore('T'))
            telefonoInput.setText(it.telefono)

            profileViewContainer.visibility = View.GONE
            profileFormContainer.visibility = View.VISIBLE
        }
    }

    private fun saveProfile() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            showError("Error: No hay usuario autenticado.")
            return
        }

        val nombre = nombreInput.text.toString().trim()
        val apellidoP = apellidoPatInput.text.toString().trim()
        val apellidoM = apellidoMatInput.text.toString().trim()
        val fecha = fechaInput.text.toString().trim()
        val telefono = telefonoInput.text.toString().trim()

        if (nombre.isEmpty() || apellidoP.isEmpty() || apellidoM.isEmpty() || fecha.isEmpty() || telefono.isEmpty()) {
            showError("Todos los campos son obligatorios")
            return
        }

        val usuario = Usuario(
            idUsuario = currentUser?.idUsuario,
            uidUsuario = firebaseUser.uid,
            nombre = nombre,
            apellidoPaterno = apellidoP,
            apellidoMaterno = apellidoM,
            fechaNacimiento = fecha,
            telefono = telefono
        )

        lifecycleScope.launch {
            try {
                val response = if (currentUser == null) {
                    apiService.createPerfil(token, usuario)
                } else {
                    apiService.updatePerfil(token, usuario)
                }

                if (response.isSuccessful) {
                    val message = if (currentUser == null) "Perfil Creado" else "Perfil Actualizado"
                    Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_SHORT).show()
                    goToHomeActivity()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("ProfileActivity", "Fallo al guardar. Código: ${response.code()}, Mensaje: $errorBody")
                    showError("Fallo al guardar: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error de red al guardar el perfil", e)
                showError("Error de red: ${e.message}")
            }
        }
    }

    private fun clearForm() {
        nombreInput.text?.clear()
        apellidoPatInput.text?.clear()
        apellidoMatInput.text?.clear()
        fechaInput.text?.clear()
        telefonoInput.text?.clear()
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getStoredToken(): String {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return prefs.getString("auth_token", "") ?: ""
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

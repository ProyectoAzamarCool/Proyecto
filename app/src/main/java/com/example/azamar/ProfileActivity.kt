package com.example.azamar

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

// Data class
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
    val fechaNacimiento: String, // ISO 8601
    val telefono: String
)

// API Service
interface UsuarioApiService {
    @GET("/info-usuario")
    suspend fun getPerfil(@Header("Authorization") token: String): Response<Usuario>

    @PUT("/info-usuario")
    suspend fun updatePerfil(
        @Header("Authorization") token: String,
        @Body usuario: Usuario
    ): Response<Usuario>

    @POST("/info-usuario")
    suspend fun createPerfil(
        @Header("Authorization") token: String,
        @Body usuario: Usuario
    ): Response<Usuario>
}

// Activity
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
        btnGuardar.setOnClickListener {
            saveProfile()
        }

        btnEditar.setOnClickListener {
            showFormularioEdicion()
        }

        // Cargar datos
        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val response = apiService.getPerfil(token)
                if (response.isSuccessful) {
                    currentUser = response.body()
                    currentUser?.let { showPerfil(it) }
                } else if (response.code() == 404) {
                    showFormularioCreacion()
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error al cargar el perfil", e)
                Toast.makeText(this@ProfileActivity, "Error al cargar perfil: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPerfil(usuario: Usuario) {
        nombreText.text = "${usuario.nombre} ${usuario.apellidoPaterno} ${usuario.apellidoMaterno}"
        fechaText.text = usuario.fechaNacimiento
        telefonoText.text = usuario.telefono

        profileFormContainer.visibility = View.GONE
        profileViewContainer.visibility = View.VISIBLE
    }

    private fun showFormularioCreacion() {
        formTitle.text = "Crear Perfil"
        clearForm()
        profileViewContainer.visibility = View.GONE
        profileFormContainer.visibility = View.VISIBLE
    }

    private fun showFormularioEdicion() {
        currentUser?.let {
            formTitle.text = "Editar Perfil"
            nombreInput.setText(it.nombre)
            apellidoPatInput.setText(it.apellidoPaterno)
            apellidoMatInput.setText(it.apellidoMaterno)
            fechaInput.setText(it.fechaNacimiento)
            telefonoInput.setText(it.telefono)

            profileViewContainer.visibility = View.GONE
            profileFormContainer.visibility = View.VISIBLE
        }
    }

    private fun saveProfile() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, "Error: No hay usuario autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val usuario = Usuario(
            idUsuario = currentUser?.idUsuario,
            uidUsuario = firebaseUser.uid, // Siempre enviar el UID
            nombre = nombreInput.text.toString(),
            apellidoPaterno = apellidoPatInput.text.toString(),
            apellidoMaterno = apellidoMatInput.text.toString(),
            fechaNacimiento = fechaInput.text.toString(),
            telefono = telefonoInput.text.toString()
        )

        lifecycleScope.launch {
            try {
                val response = if (currentUser == null) {
                    apiService.createPerfil(token, usuario)
                } else {
                    apiService.updatePerfil(token, usuario)
                }

                if (response.isSuccessful) {
                    currentUser = response.body()
                    currentUser?.let { showPerfil(it) }
                    val message = if (currentUser?.idUsuario == usuario.idUsuario) "Perfil Actualizado" else "Perfil Creado"
                    Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("ProfileActivity", "Fallo al guardar. Código: ${response.code()}, Mensaje: $errorBody")
                    Toast.makeText(this@ProfileActivity, "Fallo al guardar: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Error al guardar el perfil", e)
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

    private fun getStoredToken(): String {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return prefs.getString("auth_token", "") ?: ""
    }
}

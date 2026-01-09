package com.example.azamar

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.File
import java.io.FileOutputStream

// --- Data Classes ---
data class Usuario(
    val nombre: String,
    @SerializedName("apellido_paterno") val apellidoPaterno: String,
    @SerializedName("apellido_materno") val apellidoMaterno: String,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String,
    val telefono: String,
    @SerializedName("id_usuario") val idUsuario: Int? = null,
    @SerializedName("uid_usuario") val uidUsuario: String? = null
)

data class VehiculoPerfil(
    @SerializedName("id_vehiculo") val id: Int,
    val modelo: String,
    val placa: String,
    @SerializedName("tipo_nombre") val tipo: String
)

// --- API Service ---
interface UsuarioApiService {
    @GET("info-usuario")
    suspend fun getPerfil(@Header("Authorization") token: String): Response<Usuario>

    @POST("info-usuario")
    suspend fun createPerfil(@Header("Authorization") token: String, @Body usuario: Usuario): Response<Usuario>

    @PUT("info-usuario")
    suspend fun updatePerfil(@Header("Authorization") token: String, @Body usuario: Usuario): Response<Usuario>

    @GET("info-usuario/vehiculo")
    suspend fun getVehiculos(@Header("Authorization") token: String): Response<List<VehiculoPerfil>>

    @DELETE("info-usuario/vehiculo/{id}")
    suspend fun deleteVehiculo(@Header("Authorization") token: String, @Path("id") vehiculoId: Int): Response<Unit>
}

// --- Activity ---
class ProfileActivity : AppCompatActivity() {

    private lateinit var apiService: UsuarioApiService
    private var currentUser: Usuario? = null
    private lateinit var token: String

    // Vistas de UI
    private lateinit var profileViewContainer: LinearLayout
    private lateinit var profileFormContainer: LinearLayout
    private lateinit var vehiclesContainer: LinearLayout
    private lateinit var nombreText: TextView
    private lateinit var fechaText: TextView
    private lateinit var telefonoText: TextView
    private lateinit var btnEditar: Button
    private lateinit var btnAddVehiculo: Button
    private lateinit var profileImage: ImageView
    private lateinit var btnEditPhoto: ImageButton

    // Vistas del formulario de perfil
    private lateinit var nombreInput: TextInputEditText
    private lateinit var apellidoPatInput: TextInputEditText
    private lateinit var apellidoMatInput: TextInputEditText
    private lateinit var fechaInput: TextInputEditText
    private lateinit var telefonoInput: TextInputEditText
    private lateinit var btnGuardar: Button
    private lateinit var formTitle: TextView

    // Launcher para seleccionar imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                saveProfileImageLocally(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        initializeViews()
        token = "Bearer ${getStoredToken()}"
        apiService = RetrofitClient.instance.create(UsuarioApiService::class.java)

        val shouldShowProfile = intent.getBooleanExtra("SHOW_PROFILE_VIEW", false)
        if (shouldShowProfile) {
            loadAndShowProfileAndVehicles()
        } else {
            loadProfileAndRedirect()
        }
    }

    private fun initializeViews() {
        profileViewContainer = findViewById(R.id.profileViewContainer)
        profileFormContainer = findViewById(R.id.profileFormContainer)
        vehiclesContainer = findViewById(R.id.vehiclesContainer)
        nombreText = findViewById(R.id.nombreText)
        fechaText = findViewById(R.id.fechaText)
        telefonoText = findViewById(R.id.telefonoText)
        btnEditar = findViewById(R.id.btnEditar)
        btnAddVehiculo = findViewById(R.id.btnAddVehiculo)
        profileImage = findViewById(R.id.profileImage)
        btnEditPhoto = findViewById(R.id.btnEditPhoto)

        btnAddVehiculo.setOnClickListener {
            val intent = Intent(this, VehiculoActivity::class.java)
            intent.putExtra("FORCE_SHOW_FORM", true)
            startActivity(intent)
        }

        btnEditar.setOnClickListener { showEditForm() }
        btnEditPhoto.setOnClickListener { showImagePickerDialog() }

        loadProfileImage()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Seleccionar de galería", "Tomar foto", "Eliminar foto")
        AlertDialog.Builder(this)
            .setTitle("Foto de perfil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                    2 -> deleteProfileImage()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraCaptureResult.launch(intent)
    }

    private val cameraCaptureResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                saveProfileImageFromBitmap(it)
            }
        }
    }

    private fun saveProfileImageLocally(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            saveProfileImageFromBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error al guardar imagen", e)
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfileImageFromBitmap(bitmap: Bitmap) {
        try {
            val file = File(filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)

            // Comprimir y redimensionar la imagen
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

            outputStream.flush()
            outputStream.close()

            profileImage.setImageBitmap(scaledBitmap)
            Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error al guardar imagen", e)
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProfileImage() {
        try {
            val file = File(filesDir, "profile_image.jpg")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                profileImage.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error al cargar imagen", e)
        }
    }

    private fun deleteProfileImage() {
        try {
            val file = File(filesDir, "profile_image.jpg")
            if (file.exists()) {
                file.delete()
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error al eliminar imagen", e)
            Toast.makeText(this, "Error al eliminar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadAndShowProfileAndVehicles() {
        lifecycleScope.launch {
            try {
                val profileResponseDeferred = async { apiService.getPerfil(token) }
                val vehiclesResponseDeferred = async { apiService.getVehiculos(token) }

                val profileResponse = profileResponseDeferred.await()
                val vehiclesResponse = vehiclesResponseDeferred.await()

                if (profileResponse.isSuccessful) {
                    currentUser = profileResponse.body()
                    displayProfileData()
                } else {
                    handleApiError(profileResponse, "perfil")
                    return@launch
                }

                if (vehiclesResponse.isSuccessful) {
                    val vehicles = vehiclesResponse.body() ?: emptyList()
                    displayVehicles(vehicles)
                } else {
                    handleApiError(vehiclesResponse, "vehículos")
                }

            } catch (e: Exception) {
                handleNetworkError(e, "cargar datos")
            }
        }
    }

    private fun displayProfileData() {
        currentUser?.let {
            nombreText.text = "${it.nombre} ${it.apellidoPaterno} ${it.apellidoMaterno}"
            fechaText.text = it.fechaNacimiento.substringBefore('T')
            telefonoText.text = it.telefono
            profileViewContainer.visibility = View.VISIBLE
        }
    }

    private fun displayVehicles(vehicles: List<VehiculoPerfil>) {
        vehiclesContainer.removeAllViews()
        if (vehicles.isEmpty()) {
            val noVehiclesText = TextView(this).apply {
                text = "No tienes vehículos registrados."
                gravity = Gravity.CENTER
                setTextColor(getColor(R.color.gris_texto_hint))
                setPadding(0, 16, 0, 16)
            }
            vehiclesContainer.addView(noVehiclesText)
        } else {
            vehicles.forEach { vehicle ->
                val vehicleView = createVehicleView(vehicle)
                vehiclesContainer.addView(vehicleView)
            }
        }
    }

    private fun createVehicleView(vehicle: VehiculoPerfil): View {
        val linearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 12, 16, 12)
            setBackgroundResource(R.drawable.vehicle_item_background)
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val infoText = TextView(this).apply {
            text = "${vehicle.modelo} - ${vehicle.placa}"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            setTextColor(getColor(R.color.azul_oscuro))
            textSize = 16f
        }

        val tipoText = TextView(this).apply {
            text = vehicle.tipo
            setTextColor(getColor(R.color.gris_texto_hint))
            textSize = 12f
        }

        val deleteButton = Button(this).apply {
            text = "Eliminar"
            setBackgroundColor(getColor(R.color.rojo_google))
            setTextColor(getColor(R.color.blanco))
            setOnClickListener { showDeleteConfirmationDialog(vehicle) }
        }

        infoLayout.addView(infoText)
        infoLayout.addView(deleteButton)
        linearLayout.addView(infoLayout)
        linearLayout.addView(tipoText)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8
            )
        }
        linearLayout.addView(spacer)

        return linearLayout
    }

    private fun showDeleteConfirmationDialog(vehicle: VehiculoPerfil) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar el vehículo ${vehicle.modelo} con placa ${vehicle.placa}?")
            .setPositiveButton("Eliminar") { _, _ -> deleteVehicle(vehicle.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteVehicle(vehicleId: Int) {
        lifecycleScope.launch {
            try {
                val response = apiService.deleteVehiculo(token, vehicleId)
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Vehículo eliminado", Toast.LENGTH_SHORT).show()
                    loadAndShowProfileAndVehicles()
                } else {
                    handleApiError(response, "eliminar el vehículo")
                }
            } catch (e: Exception) {
                handleNetworkError(e, "eliminar el vehículo")
            }
        }
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
                    handleApiError(response, "perfil")
                }
            } catch (e: Exception) {
                handleNetworkError(e, "cargar el perfil")
            }
        }
    }

    private fun initializeFormViews() {
        nombreInput = findViewById(R.id.nombreInput)
        apellidoPatInput = findViewById(R.id.apellidoPatInput)
        apellidoMatInput = findViewById(R.id.apellidoMatInput)
        fechaInput = findViewById(R.id.fechaInput)
        telefonoInput = findViewById(R.id.telefonoInput)
        btnGuardar = findViewById(R.id.btnGuardar)
        formTitle = findViewById(R.id.formTitle)
    }

    private fun showCreateForm() {
        initializeFormViews()
        formTitle.text = "Crear Perfil"
        clearForm()
        btnGuardar.setOnClickListener { saveProfile() }
        profileViewContainer.visibility = View.GONE
        profileFormContainer.visibility = View.VISIBLE
    }

    private fun showEditForm() {
        initializeFormViews()
        currentUser?.let {
            formTitle.text = "Editar Perfil"
            nombreInput.setText(it.nombre)
            apellidoPatInput.setText(it.apellidoPaterno)
            apellidoMatInput.setText(it.apellidoMaterno)
            fechaInput.setText(it.fechaNacimiento.substringBefore('T'))
            telefonoInput.setText(it.telefono)

            btnGuardar.setOnClickListener { saveProfile() }
            profileViewContainer.visibility = View.GONE
            profileFormContainer.visibility = View.VISIBLE
        }
    }

    private fun saveProfile() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

        val nombre = nombreInput.text.toString().trim()
        val apellidoP = apellidoPatInput.text.toString().trim()
        val apellidoM = apellidoMatInput.text.toString().trim()
        val fecha = fechaInput.text.toString().trim()
        val telefono = telefonoInput.text.toString().trim()

        if (nombre.isEmpty() || apellidoP.isEmpty() || apellidoM.isEmpty() || fecha.isEmpty() || telefono.isEmpty()) {
            showError("Todos los campos son obligatorios")
            return
        }

        val usuario = Usuario(nombre, apellidoP, apellidoM, fecha, telefono, currentUser?.idUsuario, firebaseUser.uid)

        lifecycleScope.launch {
            try {
                val response = if (currentUser == null) apiService.createPerfil(token, usuario) else apiService.updatePerfil(token, usuario)
                if (response.isSuccessful) {
                    val message = if (currentUser == null) "Perfil Creado" else "Perfil Actualizado"
                    Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_SHORT).show()
                    goToHomeActivity()
                } else {
                    handleApiError(response, "guardar el perfil")
                }
            } catch (e: Exception) {
                handleNetworkError(e, "guardar el perfil")
            }
        }
    }

    private fun handleApiError(response: Response<*>, action: String) {
        val errorBody = response.errorBody()?.string() ?: "Sin detalles"
        val errorCode = response.code()
        val errorMessage = "Error al $action (Código: $errorCode): $errorBody"
        Log.e("ProfileActivity", errorMessage)
        showError(errorMessage)
    }

    private fun handleNetworkError(e: Exception, action: String) {
        Log.e("ProfileActivity", "Error de red al $action", e)
        showError("Error de red: ${e.message}")
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
package com.example.azamar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// --- Data Classes ---

// Para crear un vehículo (POST)
data class Vehiculo(
    @SerializedName("modelo") val modelo: String,
    @SerializedName("vin") val vin: String,
    @SerializedName("placa") val placa: String,
    @SerializedName("propio") val propio: Boolean,
    @SerializedName("fk_tipodevehiculo") val fkTipoDeVehiculo: Int,
    @SerializedName("fk_servicio") val fkServicio: Int
)

// Para comprobar si existe un vehículo (GET)
data class VehiculoExistente(
    @SerializedName("id_vehiculo") val id: Int
)

data class TipoVehiculo(
    @SerializedName("id_tipo_vehiculo") val id: Int,
    @SerializedName("nombre") val nombre: String
) {
    override fun toString(): String = nombre
}

data class Servicio(
    @SerializedName("id_servicio") val id: Int,
    @SerializedName("nombre") val nombre: String
) {
    override fun toString(): String = nombre
}

// --- API Service (extendida) ---
interface VehiculoApiService {
    // Lee la lista de vehículos del usuario
    @GET("info-usuario/vehiculo")
    suspend fun getVehiculos(@Header("Authorization") token: String): Response<List<VehiculoExistente>>

    // Crea un nuevo vehículo
    @POST("info-usuario/vehiculo")
    suspend fun createVehiculo(@Header("Authorization") token: String, @Body vehiculo: Vehiculo): Response<Unit>

    // Obtiene los tipos de vehículo para el spinner
    @GET("info-usuario/tipos-vehiculo")
    suspend fun getTiposVehiculo(@Header("Authorization") token: String): Response<List<TipoVehiculo>>

    // Obtiene los servicios para el spinner
    @GET("info-usuario/servicios")
    suspend fun getServicios(@Header("Authorization") token: String): Response<List<Servicio>>
}

// --- Activity ---
class VehiculoActivity : AppCompatActivity() {

    private lateinit var apiService: VehiculoApiService
    private lateinit var token: String

    // Las vistas se inicializan solo si es necesario
    private lateinit var editModelo: EditText
    private lateinit var editVin: EditText
    private lateinit var editPlaca: EditText
    private lateinit var switchVehiculoPropio: Switch
    private lateinit var spinnerTipoVehiculo: Spinner
    private lateinit var spinnerServicio: Spinner
    private lateinit var btnGuardar: Button

    private var tiposVehiculoList: List<TipoVehiculo> = emptyList()
    private var serviciosList: List<Servicio> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehiculo)

        token = "Bearer ${getStoredToken()}"
        apiService = RetrofitClient.instance.create(VehiculoApiService::class.java)

        // Antes de mostrar nada, comprobar si el usuario ya tiene vehículos.
        checkExistingVehicles()
    }

    private fun checkExistingVehicles() {
        lifecycleScope.launch {
            try {
                Log.d("VehiculoActivity", "Verificando vehículos existentes...")
                val response = apiService.getVehiculos(token)

                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    // Si la lista NO está vacía, el usuario tiene vehículos. Ir a Home.
                    Log.d("VehiculoActivity", "Vehículo(s) encontrado(s). Navegando a HomeActivity.")
                    startActivity(Intent(this@VehiculoActivity, HomeActivity::class.java))
                    finish() // Cierra esta actividad para que el usuario no pueda volver.
                } else {
                    // Si la lista está vacía, o hubo un error conocido, mostrar el formulario para añadir uno.
                    Log.d("VehiculoActivity", "No se encontraron vehículos. Mostrando formulario de creación.")
                    setupForm()
                }
            } catch (e: Exception) {
                // En caso de error de red, mostrar el formulario como fallback.
                Log.e("VehiculoActivity", "Error de red al verificar vehículos", e)
                showError("No se pudo verificar tus vehículos.")
                setupForm()
            }
        }
    }

    private fun setupForm() {
        // Inicializar las vistas solo cuando se necesiten
        editModelo = findViewById(R.id.editModelo)
        editVin = findViewById(R.id.editVin)
        editPlaca = findViewById(R.id.editPlaca)
        switchVehiculoPropio = findViewById(R.id.switchVehiculoPropio)
        spinnerTipoVehiculo = findViewById(R.id.spinnerTipoVehiculo)
        spinnerServicio = findViewById(R.id.spinnerServicio)
        btnGuardar = findViewById(R.id.btnGuardar)

        // Listeners
        btnGuardar.setOnClickListener {
            guardarVehiculo()
        }

        // Cargar datos para los spinners
        loadSpinnerData()
    }

    private fun loadSpinnerData() {
        lifecycleScope.launch {
            try {
                // Cargar tipos de vehículo
                val tiposResponse = apiService.getTiposVehiculo(token)
                if (tiposResponse.isSuccessful) {
                    tiposVehiculoList = tiposResponse.body() ?: emptyList()
                    val tipoAdapter = ArrayAdapter(this@VehiculoActivity, android.R.layout.simple_spinner_item, tiposVehiculoList)
                    tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerTipoVehiculo.adapter = tipoAdapter
                } else {
                    showError("Error al cargar tipos de vehículo")
                }

                // Cargar servicios
                val serviciosResponse = apiService.getServicios(token)
                if (serviciosResponse.isSuccessful) {
                    serviciosList = serviciosResponse.body() ?: emptyList()
                    val servicioAdapter = ArrayAdapter(this@VehiculoActivity, android.R.layout.simple_spinner_item, serviciosList)
                    servicioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerServicio.adapter = servicioAdapter
                } else {
                    showError("Error al cargar servicios")
                }

            } catch (e: Exception) {
                Log.e("VehiculoActivity", "Error al cargar datos de spinners", e)
                showError(e.message ?: "Error desconocido")
            }
        }
    }

    private fun guardarVehiculo() {
        val selectedTipo = spinnerTipoVehiculo.selectedItem as? TipoVehiculo
        val selectedServicio = spinnerServicio.selectedItem as? Servicio

        if (selectedTipo == null || selectedServicio == null) {
            showError("Selecciona un tipo de vehículo y un servicio")
            return
        }

        val vehiculo = Vehiculo(
            modelo = editModelo.text.toString().trim(),
            vin = editVin.text.toString().trim(),
            placa = editPlaca.text.toString().trim(),
            propio = switchVehiculoPropio.isChecked,
            fkTipoDeVehiculo = selectedTipo.id,
            fkServicio = selectedServicio.id
        )

        // Validaciones básicas
        if (vehiculo.modelo.isEmpty() || vehiculo.vin.isEmpty() || vehiculo.placa.isEmpty()){
            showError("Modelo, VIN y Placa son obligatorios")
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.createVehiculo(token, vehiculo)
                if (response.isSuccessful) {
                    Toast.makeText(this@VehiculoActivity, "Vehículo guardado con éxito", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@VehiculoActivity, HomeActivity::class.java))
                    finishAffinity()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("VehiculoActivity", "Fallo al guardar. Código: ${response.code()}, Mensaje: $errorBody")
                    showError("Fallo al guardar: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("VehiculoActivity", "Error al guardar vehículo", e)
                showError(e.message ?: "Error desconocido")
            }
        }
    }

    private fun getStoredToken(): String {
        val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        return prefs.getString("auth_token", "") ?: ""
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

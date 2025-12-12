package com.example.azamar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.azamar.databinding.ActivityVehiculoBinding
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// --- Data Classes ---
data class Vehiculo(
    @SerializedName("modelo") val modelo: String,
    @SerializedName("vin") val vin: String,
    @SerializedName("placa") val placa: String,
    @SerializedName("propio") val propio: Boolean,
    @SerializedName("fk_tipodevehiculo") val fkTipoDeVehiculo: Int,
    @SerializedName("fk_servicio") val fkServicio: Int
)

data class TipoVehiculo(
    @SerializedName("id_tipo_vehiculo") val id: Int,
    @SerializedName("nombre") val nombre: String
) {
    // Sobrescribir toString para que el Spinner muestre el nombre
    override fun toString(): String = nombre
}

data class Servicio(
    @SerializedName("id_servicio") val id: Int,
    @SerializedName("nombre") val nombre: String
) {
    // Sobrescribir toString para que el Spinner muestre el nombre
    override fun toString(): String = nombre
}

// --- API Service (extendida) ---
interface VehiculoApiService {
    @POST("/info-usuario/vehiculo")
    suspend fun createVehiculo(@Header("Authorization") token: String, @Body vehiculo: Vehiculo): Response<Unit>

    @GET("/info-usuario/tipos-vehiculo")
    suspend fun getTiposVehiculo(@Header("Authorization") token: String): Response<List<TipoVehiculo>>

    @GET("/info-usuario/servicios")
    suspend fun getServicios(@Header("Authorization") token: String): Response<List<Servicio>>
}

// --- Activity ---
class VehiculoActivity : AppCompatActivity() {

    private lateinit var apiService: VehiculoApiService
    private lateinit var token: String
    private lateinit var binding: ActivityVehiculoBinding

    private var tiposVehiculoList: List<TipoVehiculo> = emptyList()
    private var serviciosList: List<Servicio> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehiculoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar API y Token
        token = "Bearer ${getStoredToken()}"
        apiService = RetrofitClient.instance.create(VehiculoApiService::class.java)

        // Listeners
        binding.btnGuardar.setOnClickListener {
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
                    binding.spinnerTipoVehiculo.adapter = tipoAdapter
                } else {
                    showError("Error al cargar tipos de vehículo")
                }

                // Cargar servicios
                val serviciosResponse = apiService.getServicios(token)
                if (serviciosResponse.isSuccessful) {
                    serviciosList = serviciosResponse.body() ?: emptyList()
                    val servicioAdapter = ArrayAdapter(this@VehiculoActivity, android.R.layout.simple_spinner_item, serviciosList)
                    servicioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.spinnerServicio.adapter = servicioAdapter
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
        // Obtener IDs de los spinners
        val selectedTipo = binding.spinnerTipoVehiculo.selectedItem as? TipoVehiculo
        val selectedServicio = binding.spinnerServicio.selectedItem as? Servicio

        if (selectedTipo == null || selectedServicio == null) {
            showError("Selecciona un tipo de vehículo y un servicio")
            return
        }

        val vehiculo = Vehiculo(
            modelo = binding.editModelo.text.toString(),
            vin = binding.editVin.text.toString(),
            placa = binding.editPlaca.text.toString(),
            propio = binding.switchVehiculoPropio.isChecked,
            fkTipoDeVehiculo = selectedTipo.id,
            fkServicio = selectedServicio.id
        )

        lifecycleScope.launch {
            try {
                val response = apiService.createVehiculo(token, vehiculo)
                if (response.isSuccessful) {
                    Toast.makeText(this@VehiculoActivity, "Vehículo guardado con éxito", Toast.LENGTH_SHORT).show()
                    // Navegar a HomeActivity
                    startActivity(Intent(this@VehiculoActivity, HomeActivity::class.java))
                    finishAffinity() // Cierra esta y las actividades anteriores (Login, Perfil)
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

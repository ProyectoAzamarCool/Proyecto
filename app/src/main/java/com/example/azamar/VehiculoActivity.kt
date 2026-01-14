package com.example.azamar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import android.app.DatePickerDialog
import java.util.Calendar

// --- Data Classes ---

data class Vehiculo(
    @SerializedName("modelo") val modelo: String,
    @SerializedName("vin") val vin: String,
    @SerializedName("placa") val placa: String,
    @SerializedName("propio") val propio: Boolean,
    @SerializedName("fk_tipodevehiculo") val fkTipoDeVehiculo: Int,
    @SerializedName("fk_servicio") val fkServicio: Int,
    // Agregamos fecha si tu API la recibe, o la manejamos como String
    @SerializedName("fecha_registro") val fecha: String
)

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

// --- API Service ---
interface VehiculoApiService {
    @GET("info-usuario/vehiculo")
    suspend fun getVehiculos(@Header("Authorization") token: String): Response<List<VehiculoExistente>>

    @POST("info-usuario/vehiculo")
    suspend fun createVehiculo(@Header("Authorization") token: String, @Body vehiculo: Vehiculo): Response<Unit>

    @GET("info-usuario/tipos-vehiculo")
    suspend fun getTiposVehiculo(@Header("Authorization") token: String): Response<List<TipoVehiculo>>

    @GET("info-usuario/servicios")
    suspend fun getServicios(@Header("Authorization") token: String): Response<List<Servicio>>
}

// --- Activity ---
class VehiculoActivity : AppCompatActivity() {

    private lateinit var apiService: VehiculoApiService
    private lateinit var token: String

    private lateinit var editModelo: EditText
    private lateinit var editVin: EditText
    private lateinit var editPlaca: EditText
    private lateinit var editFechaVehiculo: EditText // Añadido para coincidir con el XML
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

        val forceShowForm = intent.getBooleanExtra("FORCE_SHOW_FORM", false)

        if (forceShowForm) {
            setupForm()
        } else {
            checkExistingVehicles()
        }
    }

    private fun checkExistingVehicles() {
        lifecycleScope.launch {
            try {
                val response = apiService.getVehiculos(token)
                if (response.isSuccessful && response.body()?.isNotEmpty() == true) {
                    navegarAHome()
                } else {
                    setupForm()
                }
            } catch (e: Exception) {
                Log.e("VehiculoActivity", "Error de red", e)
                setupForm() // Si falla la red, permitimos intentar el registro
            }
        }
    }

    private fun setupForm() {
        editModelo = findViewById(R.id.editModelo)
        editVin = findViewById(R.id.editVin)
        editPlaca = findViewById(R.id.editPlaca)
        editFechaVehiculo = findViewById(R.id.editFechaVehiculo)
        switchVehiculoPropio = findViewById(R.id.switchVehiculoPropio)
        spinnerTipoVehiculo = findViewById(R.id.spinnerTipoVehiculo)
        spinnerServicio = findViewById(R.id.spinnerServicio)
        btnGuardar = findViewById(R.id.btnGuardar)

        btnGuardar.setOnClickListener { guardarVehiculo() }

        // Configurar el selector de fecha
        setupDatePicker()

        loadSpinnerData()
    }

    private fun setupDatePicker() {
        editFechaVehiculo.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formatear la fecha como YYYY-MM-DD
                    val fechaFormateada = String.format(
                        "%04d-%02d-%02d",
                        selectedYear,
                        selectedMonth + 1, // Los meses empiezan en 0
                        selectedDay
                    )
                    editFechaVehiculo.setText(fechaFormateada)
                },
                year,
                month,
                day
            )

            // Opcional: establecer fecha máxima (hoy) para evitar fechas futuras
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            datePickerDialog.show()
        }
    }

    private fun loadSpinnerData() {
        lifecycleScope.launch {
            try {
                // Cargar ambos datos en paralelo o secuencia
                val tiposResponse = apiService.getTiposVehiculo(token)
                val serviciosResponse = apiService.getServicios(token)

                if (tiposResponse.isSuccessful) {
                    tiposVehiculoList = tiposResponse.body() ?: emptyList()
                    val adapter = ArrayAdapter(this@VehiculoActivity, android.R.layout.simple_spinner_item, tiposVehiculoList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerTipoVehiculo.adapter = adapter
                }

                if (serviciosResponse.isSuccessful) {
                    serviciosList = serviciosResponse.body() ?: emptyList()
                    val adapter = ArrayAdapter(this@VehiculoActivity, android.R.layout.simple_spinner_item, serviciosList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerServicio.adapter = adapter
                }
            } catch (e: Exception) {
                showError("Error al cargar datos: ${e.message}")
            }
        }
    }

    private fun guardarVehiculo() {
        val selectedTipo = spinnerTipoVehiculo.selectedItem as? TipoVehiculo
        val selectedServicio = spinnerServicio.selectedItem as? Servicio

        if (selectedTipo == null || selectedServicio == null) {
            showError("Selecciona tipo y servicio")
            return
        }

        val fechaTexto = editFechaVehiculo.text.toString().trim()

        if (fechaTexto.isEmpty()) {
            showError("Selecciona una fecha para el vehículo")
            return
        }

        val vehiculo = Vehiculo(
            modelo = editModelo.text.toString().trim(),
            vin = editVin.text.toString().trim(),
            placa = editPlaca.text.toString().trim(),
            propio = switchVehiculoPropio.isChecked,
            fkTipoDeVehiculo = selectedTipo.id,
            fkServicio = selectedServicio.id,
            fecha = fechaTexto
        )

        if (vehiculo.modelo.isEmpty() || vehiculo.vin.isEmpty() || vehiculo.placa.isEmpty()) {
            showError("Completa los campos obligatorios")
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.createVehiculo(token, vehiculo)
                if (response.isSuccessful) {
                    Toast.makeText(this@VehiculoActivity, "Vehículo guardado", Toast.LENGTH_SHORT).show()
                    navegarAHome()
                } else {
                    showError("Error del servidor: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun navegarAHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finishAffinity() // Cierra todas las actividades previas (Login, Registro, etc.)
    }

    private fun getStoredToken(): String {
        return getSharedPreferences("auth_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
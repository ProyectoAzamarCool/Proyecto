package com.example.azamar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar

class DatosPersonalesActivity : AppCompatActivity() {

    private val client = OkHttpClient.Builder()
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_personales)

        val auth = FirebaseAuth.getInstance()

        val nombre = findViewById<EditText>(R.id.editNombre)
        val apellidoP = findViewById<EditText>(R.id.editApellidoP)
        val apellidoM = findViewById<EditText>(R.id.editApellidoM)
        val fecha = findViewById<EditText>(R.id.editFecha)
        val telefono = findViewById<EditText>(R.id.editTelefono)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        fecha.setOnClickListener {
            showDatePickerDialog()
        }

        btnGuardar.setOnClickListener {

            val json = JSONObject()
            json.put("uid", auth.currentUser!!.uid)
            json.put("nombre", nombre.text.toString())
            json.put("apellido_paterno", apellidoP.text.toString())
            json.put("apellido_materno", apellidoM.text.toString())
            json.put("fecha_nacimiento", fecha.text.toString())
            json.put("telefono", telefono.text.toString())

            val body = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url("https://api-bqajzo735a-uc.a.run.app/info-usuario")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("DatosPersonalesActivity", "Error de conexión", e)
                    runOnUiThread {
                        Toast.makeText(this@DatosPersonalesActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if(response.isSuccessful) {
                        runOnUiThread {
                            startActivity(Intent(this@DatosPersonalesActivity, VehiculoActivity::class.java))
                            finish()
                        }
                    } else {
                        val errorBody = response.body?.string()
                        Log.e("DatosPersonalesActivity", "Error en la respuesta del servidor: ${response.code} $errorBody")
                        runOnUiThread {
                            Toast.makeText(this@DatosPersonalesActivity, "Error del servidor: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                findViewById<EditText>(R.id.editFecha).setText(selectedDate)
            }, year, month, day)

        datePickerDialog.show()
    }
}

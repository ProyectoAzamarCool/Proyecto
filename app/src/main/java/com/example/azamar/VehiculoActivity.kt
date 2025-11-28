package com.example.azamar
import okhttp3.MediaType.Companion.toMediaTypeOrNull

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class VehiculoActivity : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehiculo)

        val auth = FirebaseAuth.getInstance()

        val modelo = findViewById<EditText>(R.id.editModelo)
        val vin = findViewById<EditText>(R.id.editVin)
        val placa = findViewById<EditText>(R.id.editPlaca)
        val fechaVehiculo = findViewById<EditText>(R.id.editFechaVehiculo)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        btnGuardar.setOnClickListener {

            val json = JSONObject()
            json.put("uid", auth.currentUser!!.uid)
            json.put("modelo", modelo.text.toString())
            json.put("vin", vin.text.toString())
            json.put("placa", placa.text.toString())
            json.put("fecha_vehiculo", fechaVehiculo.text.toString())

            val body = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url("https://TU_API/usuario/vehiculo")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@VehiculoActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        startActivity(Intent(this@VehiculoActivity, HomeActivity::class.java))
                        finish()
                    }
                }
            })
        }
    }
}

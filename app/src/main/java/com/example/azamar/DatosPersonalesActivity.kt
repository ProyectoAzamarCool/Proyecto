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

class DatosPersonalesActivity : AppCompatActivity() {

    private val client = OkHttpClient()

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
                    runOnUiThread {
                        Toast.makeText(this@DatosPersonalesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        startActivity(Intent(this@DatosPersonalesActivity, VehiculoActivity::class.java))
                        finish()
                    }
                }
            })
        }
    }
}

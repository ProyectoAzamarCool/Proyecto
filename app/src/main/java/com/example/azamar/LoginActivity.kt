package com.example.azamar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoRegister = findViewById<Button>(R.id.btnGoRegister)

        btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {

            auth.signInWithEmailAndPassword(
                email.text.toString(),
                password.text.toString()
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    // Verificar si ya tiene datos en BD externa
                    verificarEstatusUsuario(auth.currentUser!!.uid)
                } else {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun verificarEstatusUsuario(uid: String) {
        val url = "https://TU_API/usuario/estatus?uid=$uid"

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")

                runOnUiThread {
                    when {
                        !json.getBoolean("tieneDatosPersonales") -> {
                            startActivity(Intent(this@LoginActivity, DatosPersonalesActivity::class.java))
                        }
                        !json.getBoolean("tieneVehiculo") -> {
                            startActivity(Intent(this@LoginActivity, VehiculoActivity::class.java))
                        }
                        else -> {
                            startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                        }
                    }
                    finish()
                }
            }
        })
    }
}

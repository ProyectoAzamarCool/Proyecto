package com.example.azamar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val goToRegisterBtn = findViewById<Button>(R.id.goToRegisterBtn)

        loginBtn.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()

            if (emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login exitoso, obtener usuario y token
                        val user = auth.currentUser
                        user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val idToken = tokenTask.result?.token
                                if (idToken != null) {
                                    // Guardar el token
                                    val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                                    prefs.edit().putString("auth_token", idToken).apply()

                                    // Ir a ProfileActivity
                                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, ProfileActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Error: No se pudo obtener el token.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(this, "Error al obtener token: ${tokenTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error de inicio de sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        goToRegisterBtn.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}

package com.example.azamar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val email = findViewById<EditText>(R.id.editEmail)
        val password = findViewById<EditText>(R.id.editPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val checkTerms = findViewById<CheckBox>(R.id.checkTerms)

        val prefs = getSharedPreferences("AzamarPrefs", Context.MODE_PRIVATE)

        btnRegister.setOnClickListener {

            if (!checkTerms.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(
                email.text.toString(),
                password.text.toString()
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    prefs.edit().putBoolean("termsAccepted", true).apply()
                    Toast.makeText(this, "Cuenta creada", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DatosPersonalesActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

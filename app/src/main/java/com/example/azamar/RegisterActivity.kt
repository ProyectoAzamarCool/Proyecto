package com.example.azamar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.azamar.databinding.ActivityRegisterBinding // ¡Importante! Asegúrate de que este import se añada
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    // Se añade la variable para View Binding
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Se infla el layout usando View Binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // El botón de registro empieza deshabilitado (como lo definimos en el XML)
        // binding.btnRegister.isEnabled = false // No es necesario si ya está en el XML, pero es buena práctica

        val prefs = getSharedPreferences("AzamarPrefs", Context.MODE_PRIVATE)

        // --- LÓGICA PARA TÉRMINOS Y CONDICIONES ---

        // 1. Habilitar/deshabilitar el botón de registro al marcar el CheckBox
        binding.checkTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnRegister.isEnabled = isChecked
        }

        // 2. Mostrar/ocultar el texto de los TyC al hacer clic en el TextView
        binding.tvDesplegarTyC.setOnClickListener {
            if (binding.tvContenidoTyC.visibility == View.VISIBLE) {
                binding.tvContenidoTyC.visibility = View.GONE
            } else {
                binding.tvContenidoTyC.visibility = View.VISIBLE
            }
        }

        // 3. (Opcional) Hacer que el texto de los TyC sea "scrollable" por si es muy largo
        binding.tvContenidoTyC.movementMethod = ScrollingMovementMethod.getInstance()

        // 4. Se elimina el texto del CheckBox para que no se duplique con el TextView clicable
        binding.checkTerms.text = ""


        // --- LÓGICA DEL BOTÓN DE REGISTRO ---
        binding.btnRegister.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()

            // Verificaciones básicas
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.edit().putBoolean("termsAccepted", true).apply()
                        Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ProfileActivity::class.java))
                        finish() // Cierra esta actividad para que el usuario no pueda volver atrás
                    } else {
                        // Muestra un error más específico si es posible
                        val errorMessage = task.exception?.message ?: "Error desconocido al registrar"
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

}


package com.example.azamar

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.azamar.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences("AzamarPrefs", Context.MODE_PRIVATE)

        // Configurar el CheckBox con el texto subrayado y clicable
        setupTermsText()

        // Lógica del botón de registro
        binding.btnRegister.setOnClickListener {
            if (!binding.checkTerms.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.edit().putBoolean("termsAccepted", true).apply()
                        Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = task.exception?.message ?: "Error al registrar"
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun setupTermsText() {
        val text = getString(R.string.terms_and_conditions)
        val spannableString = SpannableString(text)
        
        // Creamos un span clicable para todo el texto del CheckBox
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                // Prevenimos que al hacer clic en el texto se cambie el estado del checkbox automáticamente
                // si solo queremos abrir el modal
                showTermsModal()
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true // Subrayado
                ds.color = Color.parseColor("#1F3B5C") // Color azul_medio
            }
        }

        spannableString.setSpan(clickableSpan, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        binding.checkTerms.text = spannableString
        binding.checkTerms.movementMethod = LinkMovementMethod.getInstance() // Necesario para que el clic funcione
    }

    private fun showTermsModal() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Términos y Condiciones")
        builder.setMessage(getString(R.string.full_terms_and_conditions))
        builder.setPositiveButton("He leído y acepto") { dialog, _ ->
            binding.checkTerms.isChecked = true
            dialog.dismiss()
        }
        builder.setNegativeButton("Cerrar") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()

        // Redimensionar el modal para que sea un poco más pequeño (90% del ancho de la pantalla)
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = layoutParams
    }
}

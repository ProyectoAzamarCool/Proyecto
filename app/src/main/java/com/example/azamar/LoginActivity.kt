package com.example.azamar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.azamar.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // --- COMPROBACIÓN DE SESIÓN ACTIVA ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Si hay sesión, refrescar el token antes de continuar para evitar errores 403.
            refreshSessionAndNavigate(currentUser)
        } else {
            // Si no hay sesión, mostrar la pantalla de login.
            setupLoginUI()
        }
    }

    private fun refreshSessionAndNavigate(user: FirebaseUser) {
        user.getIdToken(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken = task.result?.token
                if (idToken != null) {
                    // Guardar el token fresco.
                    val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("auth_token", idToken).apply()
                    // Navegar a la siguiente pantalla.
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                } else {
                    // Caso raro: no se obtuvo token. Forzar login.
                    Toast.makeText(this, "No se pudo verificar tu sesión. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
                    setupLoginUI()
                }
            } else {
                // Fallo al refrescar. Forzar login.
                Toast.makeText(this, "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
                setupLoginUI()
            }
        }
    }

    private fun setupLoginUI() {
        // Inicialización normal de la pantalla de login.
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- GOOGLE SIGN IN ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)

        // --- LISTENERS ---
        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // --- LÍNEA MODIFICADA ---
        // Se comenta esta línea para evitar el error de compilación.
        // DEBES añadir el botón con id 'btnGoogleLogin' a tu XML y luego descomentar esto.
        /*
        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
        }
        */

        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnForgot.setOnClickListener {
            startActivity(Intent(this, RecoverPasswordActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    handleSuccessfulLogin()
                } else {
                    Toast.makeText(this, "Error de inicio de sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_REQUEST_CODE) {
            val result = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (result.isSuccessful) {
                val account = result.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnSuccessListener { handleSuccessfulLogin() }
                    .addOnFailureListener { Toast.makeText(this, "Error de autenticación con Google: ${it.message}", Toast.LENGTH_SHORT).show() }

            } else {
                val exception = result.exception
                Log.w("LoginActivity", "Inicio de sesión con Google fallido", exception)
                Toast.makeText(this, "Inicio de sesión con Google fallido.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleSuccessfulLogin() {
        val user = auth.currentUser
        user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val idToken = tokenTask.result?.token
                if (idToken != null) {
                    val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    prefs.edit().putString("auth_token", idToken).apply()

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
    }
}

package com.example.azamar

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
    private val GOOGLE_SIGN_IN_REQUEST_CODE = 1000 // Define request code as a constant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val prefs = getSharedPreferences("AzamarPrefs", Context.MODE_PRIVATE)

        // INICIO OFFLINE
        if (!isNetworkAvailable() && prefs.getString("userToken", null) != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        // --- GOOGLE SIGN IN ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)

        // --- LISTENERS ---

        // BOTÓN LOGIN NORMAL
        binding.btnLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // BOTÓN LOGIN CON GOOGLE
        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleClient.signInIntent
            startActivityForResult(signInIntent, GOOGLE_SIGN_IN_REQUEST_CODE)
        }

        // IR A REGISTRO
        binding.btnGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // RECUPERAR CONTRASEÑA
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
                    .addOnSuccessListener {
                        handleSuccessfulLogin() // Flujo unificado
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error de autenticación con Google: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Si el inicio de sesión falla o se cancela, ahora lo mostraremos
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
    }

    private fun saveTokenAndGoHome(user: FirebaseUser?) {
        user?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            val prefs = getSharedPreferences("AzamarPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("userToken", token).apply()

            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}

package com.example.azamar.data.network//package com.example.pruebas.data.network
//
//import okhttp3.Interceptor
//import okhttp3.Response
//import com.google.firebase.auth.FirebaseAuth // Asegúrate de tener esta dependencia
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.runBlocking // Usaremos runBlocking para la llamada síncrona
//
//class AuthInterceptor : Interceptor {
//
//    override fun intercept(chain: Interceptor.Chain): Response {
//        // Obtenemos la solicitud original
//        val originalRequest = chain.request()
//
//        // 1. Obtener el Token de Firebase de forma síncrona
//        // Usamos runBlocking porque intercept() es una función síncrona de OkHttp.
//        val idToken = runBlocking {
//            try {
//                // Obtiene el usuario actual de Firebase
//                val user = FirebaseAuth.getInstance().currentUser
//
//                // Si el usuario existe, fuerza la actualización del token
//                // .await() hace que la llamada sea síncrona
//                user?.getIdToken(true)?.await()?.token
//
//            } catch (e: Exception) {
//                // En caso de error (ej. el usuario se desconectó), devolvemos null
//                println("Error obteniendo Firebase ID Token: ${e.message}")
//                null
//            }
//        }
//
//        // 2. Si el token se obtuvo con éxito, lo adjuntamos al encabezado
//        return if (idToken != null) {
//            val authenticatedRequest = originalRequest.newBuilder()
//                .header("Authorization", "Bearer $idToken") // Estándar de autenticación
//                .build()
//
//            chain.proceed(authenticatedRequest)
//        } else {
//            // Si no hay token (el usuario no está logueado), se envía la petición original.
//            // *NOTA*: Tus endpoints de backend deben rechazar peticiones sin token si son rutas protegidas.
//            chain.proceed(originalRequest)
//        }
//    }
//}
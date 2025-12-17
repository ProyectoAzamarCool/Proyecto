package com.example.azamar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private val handler = Handler()
    private var ultimaUbicacion: LatLng? = null
    private var notificacionesIniciadas = false
    private var camaraMovida = false

    private val LOCATION_PERMISSION_CODE = 100
    private val NOTIFICATION_PERMISSION_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        solicitarPermisoUbicacion()
    }

    // ---------------- MAPA ----------------

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
        }

        val cdmxCenter = LatLng(19.432608, -99.133209)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cdmxCenter, 11f))

        dibujarCDMX()
    }

    // ---------------- PERMISOS ----------------

    private fun solicitarPermisoUbicacion() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        } else {
            obtenerUbicacion()
            solicitarPermisoNotificaciones()
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            obtenerUbicacion()
            solicitarPermisoNotificaciones()
        }
    }

    // ---------------- UBICACIÓN ----------------

    private fun obtenerUbicacion() {

        if (!::mMap.isInitialized) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        mMap.isMyLocationEnabled = true

        iniciarActualizacionesUbicacion()

        if (!notificacionesIniciadas) {
            iniciarNotificacionesCada2Minutos()
            notificacionesIniciadas = true
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun iniciarActualizacionesUbicacion() {

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                ultimaUbicacion = LatLng(
                    location.latitude,
                    location.longitude
                )

                if (!::mMap.isInitialized) return

                if (!camaraMovida) {
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(ultimaUbicacion!!, 15f)
                    )
                    camaraMovida = true
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    // ---------------- NOTIFICACIONES ----------------

    private fun iniciarNotificacionesCada2Minutos() {

        val runnable = object : Runnable {
            override fun run() {

                ultimaUbicacion?.let { pos ->

                    if (estaDentroCDMX(pos)) {
                        mostrarNotificacion(
                            "Dentro del área",
                            "Te encuentras dentro de la CDMX"
                        )
                    } else {
                        mostrarNotificacion(
                            "Fuera del área",
                            "No te encuentras dentro de la CDMX"
                        )
                    }
                }

                handler.postDelayed(this, 120_000)
            }
        }

        runnable.run() // 🔥 notificación inmediata
    }

    // ---------------- CDMX ----------------

    private fun dibujarCDMX() {
        val cdmx = listOf(
            LatLng(19.5915, -99.2863),
            LatLng(19.5073, -99.3083),
            LatLng(19.3582, -99.2355),
            LatLng(19.2759, -99.1698),
            LatLng(19.3071, -99.0512),
            LatLng(19.4251, -99.0044),
            LatLng(19.5425, -98.9927),
            LatLng(19.6560, -99.0644),
            LatLng(19.6299, -99.2059),
            LatLng(19.5915, -99.2863)
        )

        mMap.addPolygon(
            PolygonOptions()
                .addAll(cdmx)
                .strokeColor(Color.RED)
                .fillColor(0x30FF0000)
                .strokeWidth(4f)
        )
    }

    private fun estaDentroCDMX(pos: LatLng): Boolean {
        val bounds = LatLngBounds(
            LatLng(19.2300, -99.3650),
            LatLng(19.6090, -99.0600)
        )
        return bounds.contains(pos)
    }

    // ---------------- NOTIFICACIÓN ----------------

    private fun mostrarNotificacion(titulo: String, mensaje: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val channelId = "cdmx_channel"

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ubicación CDMX",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}

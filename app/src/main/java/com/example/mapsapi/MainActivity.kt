package com.example.mapsapi

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener el SupportMapFragment y recibir notificación cuando el mapa esté listo
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Coordenadas de ejemplo - Ciudad de México, Zócalo
        val ubicacion = LatLng(19.2340637, -98.8410597)

        // Agregar un marcador en la ubicación
        mMap.addMarker(
            MarkerOptions()
                .position(ubicacion)
                .title("Ciudad de México")
                .snippet("Zócalo - Centro Histórico")
        )

        // Mover la cámara a la ubicación con zoom
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacion, 18f))

        // Habilitar controles de zoom
        mMap.uiSettings.isZoomControlsEnabled = true
    }
}

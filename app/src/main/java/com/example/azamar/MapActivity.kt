package com.example.azamar

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.*
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import android.content.res.Configuration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val handler = Handler(Looper.getMainLooper())
    private var ultimaUbicacion: LatLng? = null
    private var notificacionesIniciadas = false
    private var camaraMovida = false
    private var modoDemoActivo = false

    private val puntosCDMX = mutableListOf<LatLng>()
    private val infoTramos = HashMap<Polyline, TramoLegal>()

    // --- VISTAS SOS / GRABACIÓN ---
    private lateinit var btnPanico: FloatingActionButton
    private var mediaRecorder: MediaRecorder? = null
    private var grabando = false
    private var tiempoRestante = 60_000L
    private var temporizador: Runnable? = null

    // --- VISTAS PANEL AUDIOS ---
    private lateinit var panelAudios: LinearLayout
    private lateinit var recyclerAudios: RecyclerView
    private lateinit var btnMostrarAudios: FloatingActionButton
    private lateinit var overlayCerrar: View
    private var audioAdapter: AudioAdapter? = null

    // --- VISTAS CONTROL GRABACIÓN ---
    private lateinit var menuGrabacion: CardView
    private lateinit var btnPausar: ImageButton
    private lateinit var btnReanudar: ImageButton
    private lateinit var btnDetener: ImageButton

    // --- VISTAS CARD SEMÁFORO ---
    private lateinit var cardInfoLegal: CardView
    private lateinit var txtNombreCalle: TextView
    private lateinit var txtDetalleCalle: TextView
    private lateinit var txtVelocidadCalle: TextView
    private lateinit var contenedorCard: LinearLayout

    private companion object {
        const val PERMISOS_GENERALES_CODE = 400
        const val AUDIO_PERMISSION_CODE = 300
    }

    enum class ZonaLegal { ROJA, AMARILLA, VERDE }
    data class TramoLegal(
        val nombre: String, val zona: ZonaLegal, val velocidadMax: Int,
        val descripcion: String, val puntos: List<LatLng>
    )

    private val listaTramos = listOf(
        TramoLegal("Viaducto Miguel Alemán", ZonaLegal.ROJA, 50, "Vía de Acceso Controlado. Fotomultas activas.",
            listOf(LatLng(19.4005, -99.1915), LatLng(19.4001, -99.1800), LatLng(19.4004, -99.1650), LatLng(19.4010, -99.1500), LatLng(19.4020, -99.1300), LatLng(19.4035, -99.1150))),
        TramoLegal("Anillo Periférico Sur", ZonaLegal.ROJA, 80, "Carriles centrales. Radares operativos 24/7.",
            listOf(LatLng(19.3660, -99.2000), LatLng(19.3550, -99.1950), LatLng(19.3400, -99.1880), LatLng(19.3250, -99.1850), LatLng(19.3080, -99.1870), LatLng(19.2950, -99.1950))),
        TramoLegal("Calzada de Tlalpan", ZonaLegal.ROJA, 50, "Eje vial principal. Prohibido estacionarse.",
            listOf(LatLng(19.4200, -99.1350), LatLng(19.4000, -99.1370), LatLng(19.3700, -99.1410), LatLng(19.3400, -99.1430), LatLng(19.3100, -99.1450), LatLng(19.2850, -99.1500))),
        TramoLegal("Vasco de Quiroga (Santa Fe)", ZonaLegal.AMARILLA, 40, "Zona comercial. Vigilancia constante.",
            listOf(LatLng(19.3700, -99.2550), LatLng(19.3660, -99.2620), LatLng(19.3630, -99.2680), LatLng(19.3580, -99.2750), LatLng(19.3550, -99.2820))),
        TramoLegal("Calle Colima (Roma Norte)", ZonaLegal.AMARILLA, 40, "Zona EcoParq. Riesgo de inmovilizador.",
            listOf(LatLng(19.4195, -99.1680), LatLng(19.4185, -99.1640), LatLng(19.4170, -99.1580), LatLng(19.4155, -99.1530))),
        TramoLegal("Presidente Masaryk", ZonaLegal.AMARILLA, 40, "Zona de lujo. No se permite doble fila.",
            listOf(LatLng(19.4315, -99.2050), LatLng(19.4312, -99.1950), LatLng(19.4310, -99.1850), LatLng(19.4308, -99.1750))),
        TramoLegal("Francisco Sosa (Coyoacán)", ZonaLegal.VERDE, 20, "Calle Histórica. Prioridad peatón.",
            listOf(LatLng(19.3505, -99.1750), LatLng(19.3495, -99.1680), LatLng(19.3485, -99.1620), LatLng(19.3475, -99.1550))),
        TramoLegal("Paseo del Río", ZonaLegal.VERDE, 20, "Residencial Chimalistac. Tránsito calmado.",
            listOf(LatLng(19.3440, -99.1780), LatLng(19.3425, -99.1810), LatLng(19.3410, -99.1840), LatLng(19.3395, -99.1870))),
        TramoLegal("Plaza San Jacinto", ZonaLegal.VERDE, 20, "Zona Peatonal San Ángel. Respetar cruces.",
            listOf(LatLng(19.3480, -99.1905), LatLng(19.3472, -99.1920), LatLng(19.3465, -99.1935)))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        cargarPuntosDesdeJson()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        vincularVistas()
        configurarPanelAudios()
        configurarBotonesGrabacion()
        solicitarPermisosIniciales()
    }

    private fun cargarPuntosDesdeJson() {
        try {
            val inputStream = resources.openRawResource(R.raw.limite_cdmx)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val features = JSONObject(jsonString).getJSONArray("features")
            val coords = features.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
            puntosCDMX.clear()
            for (i in 0 until coords.length()) {
                val p = coords.getJSONArray(i)
                puntosCDMX.add(LatLng(p.getDouble(1), p.getDouble(0)))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun vincularVistas() {
        btnPanico = findViewById(R.id.btnPanico)
        panelAudios = findViewById(R.id.panelAudios)
        recyclerAudios = findViewById(R.id.recyclerAudios)
        btnMostrarAudios = findViewById(R.id.btnMostrarAudios)
        menuGrabacion = findViewById(R.id.menuGrabacion)
        btnPausar = findViewById(R.id.btnPausar)
        btnReanudar = findViewById(R.id.btnReanudar)
        btnDetener = findViewById(R.id.btnDetener)
        overlayCerrar = findViewById(R.id.overlayCerrar)
        cardInfoLegal = findViewById(R.id.cardInfoLegal)
        txtNombreCalle = findViewById(R.id.txtNombreCalle)
        txtDetalleCalle = findViewById(R.id.txtDetalleCalle)
        txtVelocidadCalle = findViewById(R.id.txtVelocidadCalle)
        contenedorCard = findViewById(R.id.contenedorCard)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            try { googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark))
            } catch (e: Exception) { e.printStackTrace() }
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(19.4326, -99.1332), 11f))
        if (puntosCDMX.isNotEmpty()) dibujarCDMX()
        dibujarSemaforoLegal()

        mMap.setOnPolylineClickListener { polyline ->
            infoTramos[polyline]?.let { mostrarInfoCalle(it) }
        }
        mMap.setOnMapClickListener { cardInfoLegal.visibility = View.GONE }

        mostrarDialogoModoDemo()
    }

    private fun mostrarDialogoModoDemo() {
        val opciones = arrayOf(
            "📍 Usar Ubicación Real (GPS)",
            "🇲🇽 Simular Ubicación en CDMX",
            "🛣️ Simular Punto en Vialidad (Semáforo)"
        )

        AlertDialog.Builder(this)
            .setTitle("Seleccionar modo de funcionamiento")
            .setCancelable(false)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        modoDemoActivo = false
                        obtenerUbicacion()
                    }
                    1 -> {
                        modoDemoActivo = true
                        simularUbicacionCDMX()
                    }
                    2 -> {
                        modoDemoActivo = true
                        simularUbicacionTramo()
                    }
                }
            }
            .show()
    }

    private fun simularUbicacionCDMX() {
        val centroCDMX = LatLng(19.4326, -99.1332)
        val latRandom = centroCDMX.latitude + (Random.nextDouble(-0.05, 0.05))
        val lngRandom = centroCDMX.longitude + (Random.nextDouble(-0.05, 0.05))
        actualizarUbicacionManual(LatLng(latRandom, lngRandom))
    }

    private fun simularUbicacionTramo() {
        val tramoAzar = listaTramos.random()
        val puntoAzar = tramoAzar.puntos.random()
        actualizarUbicacionManual(puntoAzar)
    }

    private fun actualizarUbicacionManual(pos: LatLng) {
        ultimaUbicacion = pos
        mMap.addMarker(MarkerOptions().position(pos).title("Ubicación Simulada"))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))

        if (!notificacionesIniciadas) {
            iniciarNotificacionesCada2Minutos()
            notificacionesIniciadas = true
        }
    }

    private fun dibujarCDMX() {
        mMap.addPolygon(PolygonOptions().addAll(puntosCDMX).strokeColor(Color.RED).fillColor(0x25FF0000).strokeWidth(5f))
    }

    private fun estaDentroCDMX(pos: LatLng): Boolean {
        return if (puntosCDMX.isEmpty()) false else PolyUtil.containsLocation(pos, puntosCDMX, true)
    }

    private fun mostrarInfoCalle(tramo: TramoLegal) {
        cardInfoLegal.visibility = View.VISIBLE
        txtNombreCalle.text = tramo.nombre
        txtDetalleCalle.text = tramo.descripcion
        txtVelocidadCalle.text = "Velocidad Máx: ${tramo.velocidadMax} km/h"

        val (idFondo, idTexto) = when(tramo.zona) {
            ZonaLegal.ROJA -> R.color.bg_zona_roja to R.color.txt_zona_roja
            ZonaLegal.AMARILLA -> R.color.bg_zona_amarilla to R.color.txt_zona_amarilla
            ZonaLegal.VERDE -> R.color.bg_zona_verde to R.color.txt_zona_verde
        }

        contenedorCard.setBackgroundColor(ContextCompat.getColor(this, idFondo))
        txtVelocidadCalle.setTextColor(ContextCompat.getColor(this, idTexto))
    }

    private fun dibujarSemaforoLegal() {
        listaTramos.forEach { tramo ->
            val color = when(tramo.zona) {
                ZonaLegal.ROJA -> Color.parseColor("#D32F2F")
                ZonaLegal.AMARILLA -> Color.parseColor("#FBC02D")
                ZonaLegal.VERDE -> Color.parseColor("#388E3C")
            }
            val prioridad = when(tramo.zona) {
                ZonaLegal.ROJA -> 3f
                ZonaLegal.AMARILLA -> 2f
                ZonaLegal.VERDE -> 1f
            }
            val poly = mMap.addPolyline(PolylineOptions()
                .addAll(tramo.puntos)
                .color(color)
                .width(26f)
                .zIndex(prioridad)
                .clickable(true)
                .jointType(JointType.ROUND)
                .endCap(RoundCap()))
            infoTramos[poly] = tramo
        }
    }

    private fun configurarBotonesGrabacion() {
        btnPanico.setOnClickListener { if (!grabando) iniciarGrabacion() }
        btnPausar.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                btnPausar.visibility = View.GONE
                btnReanudar.visibility = View.VISIBLE
            }
        }
        btnReanudar.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                btnReanudar.visibility = View.GONE
                btnPausar.visibility = View.VISIBLE
            }
        }
        btnDetener.setOnClickListener { detenerGrabacion() }
    }

    private fun iniciarGrabacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_CODE)
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nombreArchivo = "help_$timestamp.mp3"
        val archivo = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), nombreArchivo)

        try {
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(archivo.absolutePath)
                prepare()
                start()
            }
            grabando = true
            menuGrabacion.visibility = View.VISIBLE
            temporizador = Runnable { detenerGrabacion() }
            handler.postDelayed(temporizador!!, tiempoRestante)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun detenerGrabacion() {
        try { mediaRecorder?.stop(); mediaRecorder?.release() } catch (_: Exception) {}
        mediaRecorder = null; grabando = false; menuGrabacion.visibility = View.GONE
        btnReanudar.visibility = View.GONE
        btnPausar.visibility = View.VISIBLE
        temporizador?.let { handler.removeCallbacks(it) }
        cargarAudios()
    }

    private fun configurarPanelAudios() {
        recyclerAudios.layoutManager = LinearLayoutManager(this)
        overlayCerrar.setOnClickListener { cerrarPanelAudios() }
        btnMostrarAudios.setOnClickListener { if (panelAudios.translationX == 0f) cerrarPanelAudios() else abrirPanelAudios() }
    }

    private fun abrirPanelAudios() {
        panelAudios.animate().translationX(0f).setDuration(300).start()
        overlayCerrar.visibility = View.VISIBLE
        cargarAudios()
    }

    private fun cerrarPanelAudios() {
        panelAudios.animate().translationX(-panelAudios.width.toFloat()).setDuration(300).start()
        overlayCerrar.visibility = View.GONE
        audioAdapter?.detenerReproduccionCompleta()
    }

    private fun cargarAudios() {
        val carpeta = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val audios = carpeta?.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        audioAdapter = AudioAdapter(audios)
        recyclerAudios.adapter = audioAdapter
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Se activa el punto azul de ubicación real
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(res: LocationResult) {
                    if (modoDemoActivo) return

                    val loc = res.lastLocation ?: return
                    ultimaUbicacion = LatLng(loc.latitude, loc.longitude)
                    if (!camaraMovida) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ultimaUbicacion!!, 15f))
                        camaraMovida = true
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            if (!notificacionesIniciadas) { iniciarNotificacionesCada2Minutos(); notificacionesIniciadas = true }
        }
    }

    private fun iniciarNotificacionesCada2Minutos() {
        handler.post(object : Runnable {
            override fun run() {
                ultimaUbicacion?.let {
                    val dentro = estaDentroCDMX(it)
                    mostrarNotificacion(
                        if (dentro) "Dentro del Perímetro" else "Fuera de CDMX",
                        if (dentro) "Jurisdicción Ciudad de México" else "Atención: Territorio Estado de México"
                    )
                }
                handler.postDelayed(this, 120000)
            }
        })
    }

    private fun mostrarNotificacion(titulo: String, msj: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("cdmx_channel", "Seguridad CDMX", NotificationManager.IMPORTANCE_HIGH))
        }
        val n = NotificationCompat.Builder(this, "cdmx_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(titulo)
            .setContentText(msj)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), n)
    }

    private fun solicitarPermisosIniciales() {
        val p = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) p.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, p.toTypedArray(), PERMISOS_GENERALES_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAdapter?.detenerReproduccionCompleta()
        handler.removeCallbacksAndMessages(null)
    }
}
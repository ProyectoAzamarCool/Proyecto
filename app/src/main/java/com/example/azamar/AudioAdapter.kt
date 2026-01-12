package com.example.azamar

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
// --- NUEVOS IMPORTS PARA EL FORMATO DE FECHA ---
import java.text.SimpleDateFormat
import java.util.Locale

class AudioAdapter(private val audios: List<File>) :
    RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioActual: File? = null
    private var botonActual: ImageButton? = null

    inner class AudioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.txtNombreAudio)
        val btnPlay: ImageButton = view.findViewById(R.id.btnPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audio, parent, false)
        return AudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val audio = audios[position]

        // --- LÓGICA PARA PONER EL NOMBRE BONITO ---
        holder.txtNombre.text = formatearNombreAudio(audio.name)

        holder.btnPlay.setImageResource(android.R.drawable.ic_media_play)

        holder.btnPlay.setOnClickListener {
            if (audioActual == audio && mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                    holder.btnPlay.setImageResource(android.R.drawable.ic_media_play)
                } else {
                    mediaPlayer!!.start()
                    holder.btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                }
                return@setOnClickListener
            }

            detenerReproduccionCompleta()

            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audio.absolutePath)
                    prepare()
                    start()
                }
                audioActual = audio
                botonActual = holder.btnPlay
                holder.btnPlay.setImageResource(android.R.drawable.ic_media_pause)

                mediaPlayer?.setOnCompletionListener {
                    detenerReproduccionCompleta()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * Esta función toma "sos_20240110_223015.mp3"
     * y lo devuelve como "10 de ene., 10:30 PM"
     */
    private fun formatearNombreAudio(nombreArchivo: String): String {
        return try {
            // 1. Quitamos el "sos_" y el ".mp3" para quedarnos solo con los números
            val soloNumeros = nombreArchivo.replace("help_", "").replace(".mp3", "")

            // 2. Le decimos al código cómo leer esos números (AñoMesDía_HoraMinSeg)
            val formatoEntrada = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fecha = formatoEntrada.parse(soloNumeros)

            // 3. Le decimos cómo queremos que lo muestre al usuario
            // d = día, MMMM = mes completo, hh:mm a = hora:minutos AM/PM
            val formatoSalida = SimpleDateFormat("d 'de' MMMM    hh:mm a", Locale.getDefault())

            fecha?.let { formatoSalida.format(it) } ?: nombreArchivo
        } catch (e: Exception) {
            // Si algo falla o el archivo tiene un nombre viejo, mostramos el nombre original
            nombreArchivo
        }
    }

    override fun getItemCount() = audios.size

    fun detenerReproduccionCompleta() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        botonActual?.setImageResource(android.R.drawable.ic_media_play)
        audioActual = null
        botonActual = null
    }
}
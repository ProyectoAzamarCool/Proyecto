package com.example.azamar.presentation.ui.ayudaexterna

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.azamar.R
import com.example.azamar.data.model.Abogado
// 👇 NUEVA IMPORTACIÓN NECESARIA PARA EL BOTÓN
import com.google.android.material.button.MaterialButton

// 1. EL ADAPTADOR ACEPTA EL LISTENER DE CONTACTO
class AbogadoAdapter(private val contactListener: (String) -> Unit) :
    ListAdapter<Abogado, AbogadoAdapter.AbogadoViewHolder>(AbogadoDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbogadoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_abogado, parent, false)
        // 2. PASAMOS EL LISTENER AL VIEWHOLDER
        return AbogadoViewHolder(view, contactListener)
    }

    override fun onBindViewHolder(holder: AbogadoViewHolder, position: Int) {
        val abogado = getItem(position)
        holder.bind(abogado)
    }

    // 3. EL VIEWHOLDER ACEPTA Y USA EL LISTENER
    class AbogadoViewHolder(itemView: View, private val contactListener: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {

        // Nuevos elementos
        private val fotoImageView: ImageView = itemView.findViewById(R.id.image_abogado_foto)
        private val correoTextView: TextView = itemView.findViewById(R.id.text_correo)

        // Elementos existentes
        private val nombreTextView: TextView = itemView.findViewById(R.id.text_nombre)
        private val especialidadTextView: TextView = itemView.findViewById(R.id.text_especialidad)
        private val distanciaTextView: TextView = itemView.findViewById(R.id.text_distancia)

        // 👇 NUEVO ELEMENTO: El botón de Contactar
        private val contactButton: MaterialButton = itemView.findViewById(R.id.button_contactar)

        fun bind(abogado: Abogado) {
            nombreTextView.text = abogado.nombre
            especialidadTextView.text = abogado.especialidad
            correoTextView.text = abogado.correo

            // Lógica para mostrar la foto (se mantiene)
            fotoImageView.setImageResource(R.drawable.ic_person)

            // Mostrar la distancia (se mantiene)
            distanciaTextView.text = abogado.distanciaKm?.let {
                "Distancia: ${"%.1f".format(it)} km"
            } ?: "Distancia no disponible"

            // 👇 IMPLEMENTACIÓN DEL CLIC DEL BOTÓN
            contactButton.setOnClickListener {
                // Ejecuta la función del Fragmento, pasándole el teléfono del abogado
                contactListener(abogado.telefono)
            }
        }
    }

    class AbogadoDiff : DiffUtil.ItemCallback<Abogado>() {
        override fun areItemsTheSame(oldItem: Abogado, newItem: Abogado): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Abogado, newItem: Abogado): Boolean {
            return oldItem == newItem
        }
    }
}
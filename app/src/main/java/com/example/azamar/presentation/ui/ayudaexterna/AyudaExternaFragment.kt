package com.example.azamar.presentation.ui.ayudaexterna

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// 👇 IMPORTACIONES REQUERIDAS PARA LA LLAMADA
import android.content.Intent
import android.net.Uri
// 👆
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.azamar.R
import com.example.azamar.data.db.AbogadosDatabase
import com.example.azamar.repository.AbogadoRepository
import com.example.azamar.presentation.viewmodel.AbogadoViewModel
import com.example.azamar.presentation.viewmodel.AbogadoViewModelFactory
import androidx.recyclerview.widget.RecyclerView
// Se eliminan las importaciones de Abogado y MaterialButton que solo se usaban para la precarga

class AyudaExternaFragment : BottomSheetDialogFragment() {

    // 1. Inicialización del ViewModel con Factory (Se mantiene igual)
    private val database by lazy { AbogadosDatabase.getDatabase(requireContext()) }
    private val repository by lazy { AbogadoRepository(database.abogadoDao()) }
    private val abogadoViewModel: AbogadoViewModel by viewModels {
        AbogadoViewModelFactory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ayuda_externa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Definimos la acción que se ejecutará al hacer clic en "Contactar Ahora"
        val handleContact: (String) -> Unit = { telefono ->
            // Crea un Intent para abrir la aplicación de teléfono con el número precargado
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$telefono")
            }
            startActivity(dialIntent)
        }

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_abogados)

        // 2. Inicializamos el adaptador, PASANDO la función handleContact
        val adapter = AbogadoAdapter(handleContact)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 3. Observación de LiveData
        // Los datos provienen de Room. El ViewModel ya se encarga de iniciar la sincronización.
        abogadoViewModel.allAbogados.observe(viewLifecycleOwner) { abogados ->
            abogados?.let { adapter.submitList(it) }
        }

        // 🛑 Lógica de precarga eliminada.
    }

    // 🛑 Función private fun precargarDatos() eliminada.
}
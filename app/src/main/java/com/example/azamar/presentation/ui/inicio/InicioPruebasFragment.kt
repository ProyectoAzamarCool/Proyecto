//package com.example.azamar.presentation.ui.inicio
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import com.example.azamar.R
//import com.example.azamar.presentation.ui.ayudaexterna.AyudaExternaFragment
//
//class InicioPruebasFragment : Fragment() {
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_inicio_pruebas, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // 1. Encuentra el botón "ABOGADOS"
//        val abogadosButton = view.findViewById<View>(R.id.button_abogados)
//
//        // 2. Define la navegación para mostrar el Bottom Sheet
//        abogadosButton.setOnClickListener {
//            val abogadosBottomSheet = AyudaExternaFragment()
//
//            // 👇 LÓGICA CORREGIDA: Usamos show() para mostrar el Bottom Sheet
//            abogadosBottomSheet.show(
//                parentFragmentManager,
//                "AyudaExternaTag" // Un tag de identificación
//            )
//        }
//    }
//}
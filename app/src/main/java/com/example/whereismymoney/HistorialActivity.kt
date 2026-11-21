package com.example.whereismymoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import java.util.Locale

data class RegistroHistorial(
    val id: String,
    val tipo: String,
    val monto: Double,
    val categoria: String,
    val nota: String,
    val fecha: String
)

class HistorialActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    private var todosLosMovimientos = listOf<RegistroHistorial>()
    private var filtroActual = "todos" // "todos", "ingreso", "gasto"

    private lateinit var tvTituloResumenHistorial: TextView
    private lateinit var tvTotalFiltrado: TextView
    private lateinit var tvCantidadFiltrada: TextView
    private lateinit var llListaHistorial: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_historial)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvTituloResumenHistorial = findViewById(R.id.tvTituloResumenHistorial)
        tvTotalFiltrado = findViewById(R.id.tvTotalFiltrado)
        tvCantidadFiltrada = findViewById(R.id.tvCantidadFiltrada)
        llListaHistorial = findViewById(R.id.llListaHistorial)

        val btnFiltroTodos = findViewById<Button>(R.id.btnFiltroTodos)
        val btnFiltroIngresos = findViewById<Button>(R.id.btnFiltroIngresos)
        val btnFiltroGastos = findViewById<Button>(R.id.btnFiltroGastos)

        val tabInicio = findViewById<LinearLayout>(R.id.tabInicio)
        val tabRegistrar = findViewById<LinearLayout>(R.id.tabRegistrar)

        fun actualizarEstilosFiltro(activo: String) {
            filtroActual = activo
            val botones = mapOf(
                "todos" to btnFiltroTodos,
                "ingreso" to btnFiltroIngresos,
                "gasto" to btnFiltroGastos
            )
            for ((clave, boton) in botones) {
                if (clave == activo) {
                    boton.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    boton.setTextColor(Color.parseColor("#087F63"))
                } else {
                    boton.setBackgroundColor(Color.TRANSPARENT)
                    boton.setTextColor(Color.parseColor("#74817A"))
                }
            }
            renderizarListaFiltrada()
        }

        btnFiltroTodos.setOnClickListener { actualizarEstilosFiltro("todos") }
        btnFiltroIngresos.setOnClickListener { actualizarEstilosFiltro("ingreso") }
        btnFiltroGastos.setOnClickListener { actualizarEstilosFiltro("gasto") }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.child("usuarios").child(uid).child("movimientos")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val listaTemporal = mutableListOf<RegistroHistorial>()
                        for (movSnap in snapshot.children) {
                            val id = movSnap.key ?: continue
                            val tipo = movSnap.child("tipo").getValue(String::class.java) ?: "gasto"
                            val monto = movSnap.child("monto").getValue(Double::class.java) ?: 0.0
                            val categoria = movSnap.child("categoria").getValue(String::class.java) ?: "Otros"
                            val nota = movSnap.child("nota").getValue(String::class.java) ?: ""
                            val fecha = movSnap.child("fecha").getValue(String::class.java) ?: ""

                            listaTemporal.add(
                                RegistroHistorial(
                                    id = id,
                                    tipo = tipo,
                                    monto = monto,
                                    categoria = categoria,
                                    nota = nota,
                                    fecha = fecha
                                )
                            )
                        }
                        todosLosMovimientos = listaTemporal.asReversed()
                        renderizarListaFiltrada()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@HistorialActivity,
                            "Error al cargar historial: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        tabInicio.setOnClickListener {
            startActivity(Intent(this, InicioActivity::class.java))
            finish()
        }

        tabRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistrarActivity::class.java))
            finish()
        }
    }

    private fun renderizarListaFiltrada() {
        val filtrados = when (filtroActual) {
            "ingreso" -> todosLosMovimientos.filter { it.tipo.equals("ingreso", ignoreCase = true) }
            "gasto" -> todosLosMovimientos.filter { !it.tipo.equals("ingreso", ignoreCase = true) }
            else -> todosLosMovimientos
        }

        var sumaIngresos = 0.0
        var sumaGastos = 0.0
        for (item in filtrados) {
            if (item.tipo.equals("ingreso", ignoreCase = true)) {
                sumaIngresos += item.monto
            } else {
                sumaGastos += item.monto
            }
        }

        tvCantidadFiltrada.text = "${filtrados.size} movs"

        when (filtroActual) {
            "ingreso" -> {
                tvTituloResumenHistorial.text = "Total ingresos filtrados"
                tvTotalFiltrado.text = String.format(Locale("es", "ES"), "+%.2f €", sumaIngresos)
            }
            "gasto" -> {
                tvTituloResumenHistorial.text = "Total gastos filtrados"
                tvTotalFiltrado.text = String.format(Locale("es", "ES"), "-%.2f €", sumaGastos)
            }
            else -> {
                tvTituloResumenHistorial.text = "Balance mostrado"
                tvTotalFiltrado.text = String.format(Locale("es", "ES"), "%.2f €", sumaIngresos - sumaGastos)
            }
        }

        llListaHistorial.removeAllViews()

        if (filtrados.isEmpty()) {
            llListaHistorial.addView(crearTarjetaVacia())
            return
        }

        for (item in filtrados) {
            val esIngreso = item.tipo.equals("ingreso", ignoreCase = true)
            val titulo = if (item.nota.isNotEmpty()) item.nota else item.categoria
            val detalle = if (item.fecha.isNotEmpty()) "${item.categoria} · ${item.fecha}" else item.categoria
            val montoTexto = String.format(
                Locale("es", "ES"),
                if (esIngreso) "+%.2f €" else "-%.2f €",
                item.monto
            )

            val tarjeta = crearTarjetaHistorial(
                icono = if (esIngreso) "💰" else "💸",
                titulo = titulo,
                detalle = detalle,
                montoTexto = montoTexto,
                esIngreso = esIngreso
            )

            tarjeta.setOnLongClickListener {
                confirmarEliminarMovimiento(item)
                true
            }

            llListaHistorial.addView(tarjeta)
        }
    }

    private fun confirmarEliminarMovimiento(item: RegistroHistorial) {
        val uid = auth.currentUser?.uid ?: return
        val etiqueta = if (item.nota.isNotEmpty()) item.nota else item.categoria

        AlertDialog.Builder(this)
            .setTitle("Eliminar movimiento")
            .setMessage("¿Seguro que quieres borrar \"$etiqueta\"?")
            .setPositiveButton("Borrar") { _, _ ->
                db.child("usuarios").child(uid).child("movimientos").child(item.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "No se pudo borrar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun crearTarjetaVacia(): CardView {
        return CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            radius = dpToPx(18).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setCardBackgroundColor(Color.WHITE)

            val tv = TextView(this@HistorialActivity).apply {
                text = "No hay movimientos en este filtro."
                setTextColor(Color.parseColor("#74817A"))
                textSize = 14f
                setPadding(dpToPx(18), dpToPx(20), dpToPx(18), dpToPx(20))
            }
            addView(tv)
        }
    }

    private fun crearTarjetaHistorial(
        icono: String,
        titulo: String,
        detalle: String,
        montoTexto: String,
        esIngreso: Boolean
    ): CardView {
        val card = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            radius = dpToPx(18).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
        }

        val fila = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val tvIcono = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42))
            gravity = Gravity.CENTER
            text = icono
            textSize = 18f
            setBackgroundColor(Color.parseColor(if (esIngreso) "#E5F1EA" else "#F5EAE4"))
        }

        val columnaTexto = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
            }
            orientation = LinearLayout.VERTICAL
        }

        val tvTitulo = TextView(this).apply {
            text = titulo
            setTextColor(Color.parseColor("#1D302D"))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val tvDetalle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
            text = detalle
            setTextColor(Color.parseColor("#74817A"))
            textSize = 12f
        }

        columnaTexto.addView(tvTitulo)
        columnaTexto.addView(tvDetalle)

        val tvMonto = TextView(this).apply {
            text = montoTexto
            setTextColor(Color.parseColor(if (esIngreso) "#087F63" else "#1D302D"))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        fila.addView(tvIcono)
        fila.addView(columnaTexto)
        fila.addView(tvMonto)
        card.addView(fila)

        return card
    }
}
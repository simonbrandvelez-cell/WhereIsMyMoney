package com.example.whereismymoney

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MovimientoItem(
    val tipo: String,
    val monto: Double,
    val categoria: String,
    val nota: String,
    val fecha: String
)

class InicioActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    private var totalIngresosBase = 0.0
    private var totalGastosBase = 0.0
    private var listaMovimientosBase = listOf<MovimientoItem>()
    private var monedaActual = "EUR"

    private lateinit var tvSaldoTotal: TextView
    private lateinit var tvIngresos: TextView
    private lateinit var tvGastos: TextView
    private lateinit var tvDivisaActual: TextView
    private lateinit var tvContadorMovs: TextView
    private lateinit var llUltimosMovimientos: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("wimm_prefs", Context.MODE_PRIVATE)
        monedaActual = prefs.getString("moneda", "EUR") ?: "EUR"

        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        val tvIniciales = findViewById<TextView>(R.id.tvIniciales)
        val btnCerrarSesion = findViewById<CardView>(R.id.btnCerrarSesion)
        tvSaldoTotal = findViewById(R.id.tvSaldoTotal)
        tvIngresos = findViewById(R.id.tvIngresos)
        tvGastos = findViewById(R.id.tvGastos)
        val btnRegistrarMovimiento = findViewById<Button>(R.id.btnRegistrarMovimiento)
        val btnCambiarDivisa = findViewById<CardView>(R.id.btnCambiarDivisa)
        tvDivisaActual = findViewById(R.id.tvDivisaActual)
        tvContadorMovs = findViewById(R.id.tvContadorMovs)
        llUltimosMovimientos = findViewById(R.id.llUltimosMovimientos)

        val tabRegistrar = findViewById<LinearLayout>(R.id.tabRegistrar)
        val tabHistorial = findViewById<LinearLayout>(R.id.tabHistorial)

        actualizarEtiquetaMoneda()

        val fechaActual = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(Date())
        tvFecha.text = fechaActual.replaceFirstChar { it.uppercase() }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.child("usuarios").child(uid).child("nombre").get()
                .addOnSuccessListener { dato ->
                    val nombre = dato.getValue(String::class.java) ?: "Usuario"
                    tvSaludo.text = "Hola, $nombre ☀️"
                    tvIniciales.text = nombre.take(2).uppercase()
                }

            db.child("usuarios").child(uid).child("movimientos")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var ingresos = 0.0
                        var gastos = 0.0
                        val listaTemporal = mutableListOf<MovimientoItem>()

                        for (movSnapshot in snapshot.children) {
                            val tipo = movSnapshot.child("tipo").getValue(String::class.java) ?: "gasto"
                            val monto = movSnapshot.child("monto").getValue(Double::class.java) ?: 0.0
                            val categoria = movSnapshot.child("categoria").getValue(String::class.java) ?: "Otros"
                            val nota = movSnapshot.child("nota").getValue(String::class.java) ?: ""
                            val fecha = movSnapshot.child("fecha").getValue(String::class.java) ?: ""

                            if (tipo.equals("ingreso", ignoreCase = true)) {
                                ingresos += monto
                            } else {
                                gastos += monto
                            }

                            listaTemporal.add(
                                MovimientoItem(
                                    tipo = tipo,
                                    monto = monto,
                                    categoria = categoria,
                                    nota = nota,
                                    fecha = fecha
                                )
                            )
                        }

                        totalIngresosBase = ingresos
                        totalGastosBase = gastos
                        listaMovimientosBase = listaTemporal
                        refrescarPantallaConDivisa()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@InicioActivity,
                            "Error al sincronizar datos: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        btnCambiarDivisa.setOnClickListener {
            val opciones = arrayOf("Euro (EUR €)", "Peso Colombiano (COP $)", "Dólar Estadounidense (USD US$)")
            val codigos = arrayOf("EUR", "COP", "USD")
            val indiceActual = codigos.indexOf(monedaActual).coerceAtLeast(0)

            AlertDialog.Builder(this)
                .setTitle("Selecciona una divisa")
                .setSingleChoiceItems(opciones, indiceActual) { dialog, which ->
                    monedaActual = codigos[which]
                    prefs.edit().putString("moneda", monedaActual).apply()
                    actualizarEtiquetaMoneda()
                    refrescarPantallaConDivisa()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnCerrarSesion.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        btnRegistrarMovimiento.setOnClickListener {
            startActivity(Intent(this, RegistrarActivity::class.java))
        }

        tabRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistrarActivity::class.java))
        }

        tabHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("wimm_prefs", Context.MODE_PRIVATE)
        val guardada = prefs.getString("moneda", "EUR") ?: "EUR"
        if (guardada != monedaActual) {
            monedaActual = guardada
            actualizarEtiquetaMoneda()
            refrescarPantallaConDivisa()
        }
    }

    private fun actualizarEtiquetaMoneda() {
        tvDivisaActual.text = when (monedaActual) {
            "COP" -> "COP ($) ▾"
            "USD" -> "USD (US$) ▾"
            else -> "EUR (€) ▾"
        }
    }

    private fun convertirDesdeEur(montoEur: Double): Double {
        return when (monedaActual) {
            "COP" -> montoEur * 4650.0
            "USD" -> montoEur * 1.08
            else -> montoEur
        }
    }

    private fun formatearMonto(montoEur: Double, prefijoSigno: String = ""): String {
        val convertido = convertirDesdeEur(montoEur)
        return when (monedaActual) {
            "COP" -> "$prefijoSigno${String.format(Locale("es", "CO"), "%,.0f", convertido)} COP"
            "USD" -> "$prefijoSigno${String.format(Locale("en", "US"), "%,.2f", convertido)} US$"
            else -> "$prefijoSigno${String.format(Locale("es", "ES"), "%,.2f", convertido)} €"
        }
    }

    private fun refrescarPantallaConDivisa() {
        val saldoDisponible = totalIngresosBase - totalGastosBase
        tvIngresos.text = formatearMonto(totalIngresosBase, "+")
        tvGastos.text = formatearMonto(totalGastosBase, "-")
        tvSaldoTotal.text = formatearMonto(saldoDisponible)
        tvContadorMovs.text = "${listaMovimientosBase.size} registros"

        llUltimosMovimientos.removeAllViews()

        if (listaMovimientosBase.isEmpty()) {
            llUltimosMovimientos.addView(
                crearTarjetaMovimiento(
                    icono = "💸",
                    titulo = "Sin movimientos aún",
                    detalle = "Registra tu primer gasto o ingreso",
                    montoTexto = formatearMonto(0.0),
                    esIngreso = true
                )
            )
        } else {
            val recientes = listaMovimientosBase.asReversed().take(5)
            for (item in recientes) {
                val esIngreso = item.tipo.equals("ingreso", ignoreCase = true)
                val titulo = if (item.nota.isNotEmpty()) item.nota else item.categoria
                val detalle = if (item.fecha.isNotEmpty()) "${item.categoria} · ${item.fecha}" else item.categoria
                val montoFormateado = formatearMonto(item.monto, if (esIngreso) "+" else "-")

                llUltimosMovimientos.addView(
                    crearTarjetaMovimiento(
                        icono = if (esIngreso) "💰" else "💸",
                        titulo = titulo,
                        detalle = detalle,
                        montoTexto = montoFormateado,
                        esIngreso = esIngreso
                    )
                )
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun crearTarjetaMovimiento(
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
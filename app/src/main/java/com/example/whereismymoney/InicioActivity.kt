package com.example.whereismymoney

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InicioActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        val mainView = findViewById<android.view.View>(R.id.main)
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        val tvIniciales = findViewById<TextView>(R.id.tvIniciales)
        val tvSaldoTotal = findViewById<TextView>(R.id.tvSaldoTotal)
        val tvIngresos = findViewById<TextView>(R.id.tvIngresos)
        val tvGastos = findViewById<TextView>(R.id.tvGastos)

        val tvCatComida = findViewById<TextView>(R.id.tvCatComida)
        val tvCatEstudios = findViewById<TextView>(R.id.tvCatEstudios)
        val tvCatOcio = findViewById<TextView>(R.id.tvCatOcio)
        val tvCatTransporte = findViewById<TextView>(R.id.tvCatTransporte)

        val btnRegistrarMovimiento = findViewById<Button>(R.id.btnRegistrarMovimiento)
        val tabInicio = findViewById<LinearLayout>(R.id.tabInicio)
        val tabRegistrar = findViewById<LinearLayout>(R.id.tabRegistrar)
        val tabHistorial = findViewById<LinearLayout>(R.id.tabHistorial)

        try {
            val fechaActual = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault()).format(Date())
            tvFecha?.text = fechaActual.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            tvFecha?.text = "Hoy"
        }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.child("usuarios").child(uid).child("nombre").get()
                .addOnSuccessListener { dato ->
                    val nombre = dato.getValue(String::class.java) ?: "Usuario"
                    tvSaludo?.text = "Hola, $nombre ☀️"
                    val iniciales = if (nombre.length >= 2) nombre.take(2).uppercase() else "US"
                    tvIniciales?.text = iniciales
                }

            db.child("usuarios").child(uid).child("movimientos")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalIngresos = 0.0
                        var totalGastos = 0.0
                        var gastoComida = 0.0
                        var gastoEstudios = 0.0
                        var gastoOcio = 0.0
                        var gastoTransporte = 0.0

                        for (movSnapshot in snapshot.children) {
                            val tipo = movSnapshot.child("tipo").getValue(String::class.java) ?: "gasto"
                            val categoria = movSnapshot.child("categoria").getValue(String::class.java) ?: "Otros"
                            val monto = movSnapshot.child("monto").getValue(Double::class.java) ?: 0.0

                            if (tipo.equals("ingreso", ignoreCase = true)) {
                                totalIngresos += monto
                            } else {
                                totalGastos += monto
                                when {
                                    categoria.contains("Comida", true) -> gastoComida += monto
                                    categoria.contains("Estudios", true) -> gastoEstudios += monto
                                    categoria.contains("Ocio", true) -> gastoOcio += monto
                                    categoria.contains("Transporte", true) -> gastoTransporte += monto
                                }
                            }
                        }

                        val saldoDisponible = totalIngresos - totalGastos

                        tvIngresos?.text = String.format(Locale.getDefault(), "+%.2f €", totalIngresos)
                        tvGastos?.text = String.format(Locale.getDefault(), "-%.2f €", totalGastos)
                        tvSaldoTotal?.text = String.format(Locale.getDefault(), "%.2f €", saldoDisponible)

                        tvCatComida?.text = String.format(Locale.getDefault(), "🟢 Comida · %.2f €", gastoComida)
                        tvCatEstudios?.text = String.format(Locale.getDefault(), "🟣 Estudios · %.2f €", gastoEstudios)
                        tvCatOcio?.text = String.format(Locale.getDefault(), "🟡 Ocio · %.2f €", gastoOcio)
                        tvCatTransporte?.text = String.format(Locale.getDefault(), "🔵 Transporte · %.2f €", gastoTransporte)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@InicioActivity, "Error al sincronizar datos", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        btnRegistrarMovimiento?.setOnClickListener {
            Toast.makeText(this, "Próximamente: Pantalla de registro", Toast.LENGTH_SHORT).show()
        }

        tabInicio?.setOnClickListener {
            Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
        }

        tabRegistrar?.setOnClickListener {
            Toast.makeText(this, "Ir a Registrar", Toast.LENGTH_SHORT).show()
        }

        tabHistorial?.setOnClickListener {
            Toast.makeText(this, "Ir a Historial", Toast.LENGTH_SHORT).show()
        }
    }
}
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvFecha = findViewById<TextView>(R.id.tvFecha)
        val tvSaludo = findViewById<TextView>(R.id.tvSaludo)
        val tvIniciales = findViewById<TextView>(R.id.tvIniciales)
        val tvSaldoTotal = findViewById<TextView>(R.id.tvSaldoTotal)
        val tvIngresos = findViewById<TextView>(R.id.tvIngresos)
        val tvGastos = findViewById<TextView>(R.id.tvGastos)
        val btnRegistrarMovimiento = findViewById<Button>(R.id.btnRegistrarMovimiento)
        val tabRegistrar = findViewById<LinearLayout>(R.id.tabRegistrar)
        val tabHistorial = findViewById<LinearLayout>(R.id.tabHistorial)

        // 1. Mostrar fecha actual formateada
        val fechaActual = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(Date())
        tvFecha.text = fechaActual.replaceFirstChar { it.uppercase() }

        val uid = auth.currentUser?.uid
        if (uid != null) {
            // 2. Cargar perfil del usuario dinámicamente
            db.child("usuarios").child(uid).child("nombre").get()
                .addOnSuccessListener { dato ->
                    val nombre = dato.getValue(String::class.java) ?: "Usuario"
                    tvSaludo.text = "Hola, $nombre ☀️"
                    tvIniciales.text = nombre.take(2).uppercase()
                }

            // 3. Escuchar los movimientos reales de Firebase (para calcular saldo, ingresos y gastos)
            db.child("usuarios").child(uid).child("movimientos")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalIngresos = 0.0
                        var totalGastos = 0.0

                        for (movSnapshot in snapshot.children) {
                            val tipo = movSnapshot.child("tipo").getValue(String::class.java) ?: "gasto"
                            val monto = movSnapshot.child("monto").getValue(Double::class.java) ?: 0.0

                            if (tipo.equals("ingreso", ignoreCase = true)) {
                                totalIngresos += monto
                            } else {
                                totalGastos += monto
                            }
                        }

                        val saldoDisponible = totalIngresos - totalGastos

                        // Actualizar la interfaz con datos reales de la base de datos
                        tvIngresos.text = String.format(Locale("es", "ES"), "+%.2f €", totalIngresos)
                        tvGastos.text = String.format(Locale("es", "ES"), "-%.2f €", totalGastos)
                        tvSaldoTotal.text = String.format(Locale("es", "ES"), "%.2f €", saldoDisponible)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@InicioActivity, "Error al cargar datos: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // Navegación de botones
        btnRegistrarMovimiento.setOnClickListener {
            Toast.makeText(this, "Próximamente: Pantalla para añadir un gasto o ingreso", Toast.LENGTH_SHORT).show()
        }

        tabRegistrar.setOnClickListener {
            Toast.makeText(this, "Ir a Registrar", Toast.LENGTH_SHORT).show()
        }

        tabHistorial.setOnClickListener {
            Toast.makeText(this, "Ir a Historial", Toast.LENGTH_SHORT).show()
        }
    }
}
package com.example.whereismymoney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistrarActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    private var tipoMovimientoSeleccionado = "gasto"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnTabGasto = findViewById<Button>(R.id.btnTabGasto)
        val btnTabIngreso = findViewById<Button>(R.id.btnTabIngreso)
        val tvPreguntaMonto = findViewById<TextView>(R.id.tvPreguntaMonto)
        val etMonto = findViewById<EditText>(R.id.etMonto)
        val spinnerCategoria = findViewById<Spinner>(R.id.spinnerCategoria)
        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaSeleccionada)
        val etNota = findViewById<EditText>(R.id.etNota)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        val tabInicio = findViewById<LinearLayout>(R.id.tabInicio)
        val tabHistorial = findViewById<LinearLayout>(R.id.tabHistorial)

        fun cargarCategorias(arrayResId: Int) {
            val adapter = ArrayAdapter.createFromResource(
                this,
                arrayResId,
                android.R.layout.simple_spinner_item
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapter
        }

        cargarCategorias(R.array.categorias_gasto_array)

        val fechaFormateada = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date())
        tvFechaSeleccionada.text = fechaFormateada

        btnTabGasto.setOnClickListener {
            tipoMovimientoSeleccionado = "gasto"
            btnTabGasto.setBackgroundColor(Color.parseColor("#FFFFFF"))
            btnTabGasto.setTextColor(Color.parseColor("#087F63"))
            btnTabIngreso.setBackgroundColor(Color.TRANSPARENT)
            btnTabIngreso.setTextColor(Color.parseColor("#74817A"))

            tvPreguntaMonto.text = "¿Cuánto gastaste?"
            btnGuardar.text = "✓   Guardar gasto"
            cargarCategorias(R.array.categorias_gasto_array)
        }

        btnTabIngreso.setOnClickListener {
            tipoMovimientoSeleccionado = "ingreso"
            btnTabIngreso.setBackgroundColor(Color.parseColor("#FFFFFF"))
            btnTabIngreso.setTextColor(Color.parseColor("#087F63"))
            btnTabGasto.setBackgroundColor(Color.TRANSPARENT)
            btnTabGasto.setTextColor(Color.parseColor("#74817A"))

            tvPreguntaMonto.text = "¿Cuánto ingresaste?"
            btnGuardar.text = "✓   Guardar ingreso"
            cargarCategorias(R.array.categorias_ingreso_array)
        }

        btnGuardar.setOnClickListener {
            val montoTexto = etMonto.text.toString().trim().replace(",", ".")
            if (montoTexto.isEmpty()) {
                Toast.makeText(this, "Por favor escribe un monto válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val monto = montoTexto.toDoubleOrNull()
            if (monto == null || monto <= 0.0) {
                Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoria = spinnerCategoria.selectedItem?.toString() ?: "Otros"
            val nota = etNota.text.toString().trim()
            val fecha = tvFechaSeleccionada.text.toString()

            val uid = auth.currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val movimientoMap = mapOf(
                "tipo" to tipoMovimientoSeleccionado,
                "monto" to monto,
                "categoria" to categoria,
                "fecha" to fecha,
                "nota" to nota
            )

            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."

            db.child("usuarios").child(uid).child("movimientos").push()
                .setValue(movimientoMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "¡Movimiento guardado con éxito!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, InicioActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    btnGuardar.isEnabled = true
                    btnGuardar.text = if (tipoMovimientoSeleccionado == "gasto") {
                        "✓   Guardar gasto"
                    } else {
                        "✓   Guardar ingreso"
                    }
                    Toast.makeText(this, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }

        tabInicio.setOnClickListener {
            startActivity(Intent(this, InicioActivity::class.java))
            finish()
        }

        tabHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
            finish()
        }
    }
}
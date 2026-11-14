package com.example.whereismymoney

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference
    private var esModoRegistro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etClave = findViewById<EditText>(R.id.etClave)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvEstado = findViewById<TextView>(R.id.tvEstado)

        btnRegistrar.setOnClickListener {
            esModoRegistro = !esModoRegistro
            if (esModoRegistro) {
                etNombre.visibility = View.VISIBLE
                btnIngresar.text = "Registrarse"
                btnRegistrar.text = "¿Ya tienes cuenta? Inicia sesión"
                tvEstado.text = "Completa tus datos para registrarte."
            } else {
                etNombre.visibility = View.GONE
                btnIngresar.text = "Iniciar sesión"
                btnRegistrar.text = "Crear una cuenta"
                tvEstado.text = ""
            }
        }

        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString().trim()

            if (correo.isEmpty() || clave.isEmpty()) {
                tvEstado.text = "Escribe tu correo y contraseña."
                return@setOnClickListener
            }

            if (esModoRegistro) {
                val nombre = etNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    tvEstado.text = "Escribe tu nombre completo."
                    return@setOnClickListener
                }
                if (clave.length < 6) {
                    tvEstado.text = "La contraseña debe tener al menos 6 caracteres."
                    return@setOnClickListener
                }

                tvEstado.text = "Registrando..."
                auth.createUserWithEmailAndPassword(correo, clave)
                    .addOnSuccessListener { resultado ->
                        val uid = resultado.user?.uid ?: ""
                        val usuarioMap = mapOf("nombre" to nombre, "correo" to correo)
                        db.child("usuarios").child(uid).setValue(usuarioMap)
                            .addOnSuccessListener {
                                val intent = Intent(this, InicioActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                    }
                    .addOnFailureListener { e ->
                        tvEstado.text = "Error: ${e.message}"
                    }
            } else {
                tvEstado.text = "Iniciando sesión..."
                auth.signInWithEmailAndPassword(correo, clave)
                    .addOnSuccessListener {
                        val intent = Intent(this, InicioActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        tvEstado.text = "Correo o contraseña incorrectos."
                    }
            }
        }
    }
}
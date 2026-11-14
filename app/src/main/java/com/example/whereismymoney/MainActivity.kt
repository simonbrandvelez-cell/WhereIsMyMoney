package com.example.whereismymoney

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    // Bandera clara para saber si estamos en modo registro (true) o inicio de sesión (false)
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

        // Enlazar vistas del XML
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etClave = findViewById<EditText>(R.id.etClave)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvEstado = findViewById<TextView>(R.id.tvEstado)

        // -------------------------------------------------------------
        // BOTÓN SECUNDARIO: Alternar entre Iniciar Sesión y Registro
        // -------------------------------------------------------------
        btnRegistrar.setOnClickListener {
            esModoRegistro = !esModoRegistro // Invierte el estado actual

            if (esModoRegistro) {
                // ESTAMOS EN MODO REGISTRO
                etNombre.visibility = View.VISIBLE
                btnIngresar.text = "Registrarse"
                btnRegistrar.text = "¿Ya tienes cuenta? Inicia sesión"
                tvEstado.text = "Completa tus datos para crear una cuenta."
            } else {
                // ESTAMOS EN MODO INICIO DE SESIÓN
                etNombre.visibility = View.GONE
                btnIngresar.text = "Iniciar sesión"
                btnRegistrar.text = "Crear una cuenta"
                tvEstado.text = ""
            }
        }

        // -------------------------------------------------------------
        // BOTÓN PRINCIPAL (DINÁMICO SEGÚN EL MODO)
        // -------------------------------------------------------------
        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString().trim()

            if (correo.isEmpty() || clave.isEmpty()) {
                tvEstado.text = "Escribe tu correo y contraseña."
                return@setOnClickListener
            }

            if (esModoRegistro) {
                // --- LÓGICA DE REGISTRO ---
                val nombre = etNombre.text.toString().trim()

                if (nombre.isEmpty()) {
                    tvEstado.text = "Escribe tu nombre completo."
                    return@setOnClickListener
                }
                if (clave.length < 6) {
                    tvEstado.text = "La contraseña debe tener al menos 6 caracteres."
                    return@setOnClickListener
                }

                tvEstado.text = "Creando cuenta..."

                auth.createUserWithEmailAndPassword(correo, clave)
                    .addOnSuccessListener { resultado ->
                        val uid = resultado.user?.uid ?: ""
                        val usuarioMap = mapOf(
                            "nombre" to nombre,
                            "correo" to correo
                        )

                        db.child("usuarios").child(uid).setValue(usuarioMap)
                            .addOnSuccessListener {
                                tvEstado.text = "¡Registro exitoso! Bienvenido, $nombre"
                            }
                            .addOnFailureListener { e ->
                                tvEstado.text = "Error al guardar en DB: ${e.message}"
                            }
                    }
                    .addOnFailureListener { e ->
                        tvEstado.text = "Error en Auth: ${e.message}"
                    }

            } else {
                // --- LÓGICA DE INICIO DE SESIÓN ---
                tvEstado.text = "Iniciando sesión..."

                auth.signInWithEmailAndPassword(correo, clave)
                    .addOnSuccessListener { resultado ->
                        val uid = resultado.user?.uid ?: ""

                        db.child("usuarios").child(uid).child("nombre").get()
                            .addOnSuccessListener { dato ->
                                val nombre = dato.getValue(String::class.java) ?: "Usuario"
                                tvEstado.text = "¡Bienvenido de nuevo, $nombre!"
                            }
                            .addOnFailureListener {
                                tvEstado.text = "¡Bienvenido! (Sin perfil en DB)"
                            }
                    }
                    .addOnFailureListener { e ->
                        tvEstado.text = "Correo o contraseña incorrectos."
                    }
            }
        }
    }
}
package com.example.whereismymoney

import android.content.Intent
import android.os.Bundle
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

class DMainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etClave = findViewById<EditText>(R.id.etClave)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvEstado = findViewById<TextView>(R.id.tvEstado)

        // -----------------------------------------
        // REGISTRARSE
        // -----------------------------------------
        btnRegistrar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString().trim()

            if (correo.isEmpty() || clave.isEmpty()) {
                tvEstado.text = "Escribe correo y contraseña"
                return@setOnClickListener
            }

            if (clave.length < 6) {
                tvEstado.text = "La contraseña debe tener al menos 6 caracteres"
                return@setOnClickListener
            }

            tvEstado.text = "Registrando..."

            auth.createUserWithEmailAndPassword(correo, clave)
                .addOnSuccessListener { resultado ->
                    val uid = resultado.user!!.uid
                    // Extraemos un nombre por defecto del correo (ej: "hipo123" de "hipo123@gmail.com")
                    val nombrePorDefecto = correo.substringBefore("@")

                    val usuario = mapOf(
                        "nombre" to nombrePorDefecto,
                        "correo" to correo
                    )

                    db.child("usuarios").child(uid).setValue(usuario)
                        .addOnSuccessListener {
                            // ¡Éxito! Saltamos a la pantalla de Inicio
                            val intent = Intent(this, InicioActivity::class.java)
                            startActivity(intent)
                            finish() // Cierra el login para que al volver atrás no regrese aquí
                        }
                        .addOnFailureListener { e ->
                            tvEstado.text = "Error DB: ${e.localizedMessage}"
                        }
                }
                .addOnFailureListener { e ->
                    tvEstado.text = "Error Auth: ${e.localizedMessage}"
                }
        }

        // -----------------------------------------
        // INGRESAR
        // -----------------------------------------
        btnIngresar.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val clave = etClave.text.toString().trim()

            if (correo.isEmpty() || clave.isEmpty()) {
                tvEstado.text = "Escribe correo y contraseña"
                return@setOnClickListener
            }

            tvEstado.text = "Iniciando sesión..."

            auth.signInWithEmailAndPassword(correo, clave)
                .addOnSuccessListener {
                    // ¡Éxito! Saltamos a la pantalla de Inicio
                    val intent = Intent(this, InicioActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    tvEstado.text = "Correo o contraseña incorrectos"
                }
        }
    }
}
package com.example.munch.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

object RepositorioPedidos {
    private val _pedidos = MutableStateFlow<List<Pedido>>(emptyList())
    val pedidos: StateFlow<List<Pedido>> = _pedidos.asStateFlow()

    private var escuchaIniciada = false

    fun inicializar(contexto: Context) {
        try {
            asegurarFirebaseInicializado(contexto)
            if (!escuchaIniciada) {
                escuchaIniciada = true
                iniciarEscuchaTiempoReal(contexto)
            }
        } catch (t: Throwable) {
            Log.e("MUNCH_FIREBASE", "Error al inicializar RepositorioPedidos", t)
        }
    }

    private fun asegurarFirebaseInicializado(contexto: Context?) {
        try {
            if (contexto != null && FirebaseApp.getApps(contexto).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("munchapp-9829d")
                    .setApplicationId("1:720736794797:android:65d5c4424a5affdc51ae24")
                    .setApiKey("AIzaSyAPPDhfsXeX8e0cs_m3V1N6izKWHxf-XTU")
                    .setStorageBucket("munchapp-9829d.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(contexto, options)
                Log.d("MUNCH_FIREBASE", "FirebaseApp inicializado explícitamente")
            }
        } catch (t: Throwable) {
            Log.e("MUNCH_FIREBASE", "Error en asegurarFirebaseInicializado", t)
        }
    }

    private fun obtenerDb(contexto: Context? = null): FirebaseFirestore? {
        return try {
            asegurarFirebaseInicializado(contexto)
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.e("MUNCH_FIREBASE", "Error obteniendo Firestore", t)
            null
        }
    }

    private fun iniciarEscuchaTiempoReal(contexto: Context) {
        try {
            val firestore = obtenerDb(contexto) ?: return
            firestore.collection("pedidos").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MUNCH_FIREBASE", "Error escuchando Firestore: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val lista = snapshot.documents.mapNotNull { doc ->
                    val estadoNombre = doc.getString("estado") ?: EstadoPedido.COMPROBANTE_ENVIADO.name
                    val estadoEnum = try {
                        EstadoPedido.valueOf(estadoNombre)
                    } catch (e: Exception) {
                        EstadoPedido.COMPROBANTE_ENVIADO
                    }

                    Pedido(
                        id = doc.getString("id") ?: doc.id,
                        kitNombre = doc.getString("kitNombre") ?: "",
                        platillo = doc.getString("platillo") ?: "",
                        porciones = (doc.getLong("porciones") ?: 2L).toInt(),
                        preferencias = doc.getString("preferencias") ?: "",
                        direccion = doc.getString("direccion") ?: "",
                        fecha = doc.getString("fecha") ?: "",
                        horario = doc.getString("horario") ?: "",
                        metodoPago = doc.getString("metodoPago") ?: "Transferencia bancaria",
                        comprobanteUri = doc.getString("comprobanteUri") ?: doc.getString("comprobanteBase64") ?: doc.getString("comprobanteUrl"),
                        estado = estadoEnum,
                        fechaCreacion = doc.getLong("fechaCreacion") ?: System.currentTimeMillis()
                    )
                }
                _pedidos.value = lista.sortedByDescending { it.fechaCreacion }
            }
        } catch (e: Throwable) {
            Log.e("MUNCH_FIREBASE", "Excepción escuchando Firestore", e)
        }
    }

    fun guardarPedidoConContexto(
        contexto: Context,
        pedido: Pedido,
        alInicio: () -> Unit = {},
        alExito: () -> Unit = {},
        alError: (String) -> Unit = {}
    ) {
        val uriStr = pedido.comprobanteUri

        if (pedido.metodoPago == "Transferencia bancaria" && uriStr.isNullOrBlank()) {
            mostrarToast(contexto, "Por favor adjunta la captura de tu comprobante de transferencia")
            alError("Sin comprobante")
            return
        }

        alInicio()

        val comprobanteFinal = if (!uriStr.isNullOrBlank() && (uriStr.startsWith("content://") || uriStr.startsWith("file://"))) {
            convertirUriABase64(contexto, Uri.parse(uriStr)) ?: uriStr
        } else {
            uriStr
        }

        val pedidoConImagen = pedido.copy(comprobanteUri = comprobanteFinal)
        guardarEnFirestore(contexto, pedidoConImagen, alExito, alError)
    }

    fun guardarPedido(pedido: Pedido) {
        guardarEnFirestore(null, pedido, {}, {})
    }

    fun convertirUriABase64(contexto: Context, uri: Uri): String? {
        return try {
            val resolver = contexto.contentResolver

            // 1. Obtener dimensiones sin cargar píxeles completos
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxDimension = 800
            var sampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= maxDimension && halfWidth / sampleSize >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // 2. Decodificar la imagen escalada para ahorrar memoria RAM
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmapOriginal = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Ajuste de escala exacto a ancho máximo de 800px
            val width = bitmapOriginal.width
            val height = bitmapOriginal.height
            val ratio = width.toFloat() / height.toFloat()

            val (targetWidth, targetHeight) = if (width > height) {
                maxDimension to (maxDimension / ratio).toInt()
            } else {
                (maxDimension * ratio).toInt() to maxDimension
            }

            val bitmapEscalado = Bitmap.createScaledBitmap(
                bitmapOriginal,
                targetWidth.coerceAtLeast(1),
                targetHeight.coerceAtLeast(1),
                true
            )

            // 4. Compresión JPEG al 50%
            val outputStream = ByteArrayOutputStream()
            bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val bytes = outputStream.toByteArray()

            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d("MUNCH_FIREBASE", "Comprobante procesado en Base64. Peso: ${bytes.size / 1024} KB")
            "data:image/jpeg;base64," + base64String
        } catch (t: Throwable) {
            Log.e("MUNCH_FIREBASE", "Error al convertir imagen a Base64: ${t.message}", t)
            null
        }
    }

    private fun guardarEnFirestore(
        contexto: Context?,
        pedido: Pedido,
        alExito: () -> Unit = {},
        alError: (String) -> Unit = {}
    ) {
        try {
            val firestore = obtenerDb(contexto)
            if (firestore == null) {
                mostrarToast(contexto, "Error: FirebaseApp no está inicializado")
                alError("FirebaseApp no inicializado")
                return
            }

            val mapa = mapOf(
                "id" to pedido.id,
                "kitNombre" to pedido.kitNombre,
                "platillo" to pedido.platillo,
                "porciones" to pedido.porciones,
                "preferencias" to pedido.preferencias,
                "direccion" to pedido.direccion,
                "fecha" to pedido.fecha,
                "horario" to pedido.horario,
                "metodoPago" to pedido.metodoPago,
                "comprobanteUri" to pedido.comprobanteUri,
                "estado" to pedido.estado.name,
                "fechaCreacion" to pedido.fechaCreacion
            )

            firestore.collection("pedidos").document(pedido.id)
                .set(mapa)
                .addOnSuccessListener {
                    Log.d("MUNCH_FIREBASE", "Pedido guardado en Firestore con éxito: ${pedido.id}")
                    mostrarToast(contexto, "¡Pedido y comprobante enviados con éxito! ID: ${pedido.id}")
                    alExito()
                }
                .addOnFailureListener { e ->
                    Log.e("MUNCH_FIREBASE", "Error al guardar en Firestore", e)
                    mostrarToast(contexto, "Error al guardar en Firestore: ${e.localizedMessage}")
                    alError(e.localizedMessage ?: "Error Firestore")
                }
        } catch (t: Throwable) {
            Log.e("MUNCH_FIREBASE", "Excepción al guardar en Firestore", t)
            mostrarToast(contexto, "Excepción Firestore: ${t.localizedMessage}")
            alError(t.localizedMessage ?: "Excepción Firestore")
        }
    }

    private fun mostrarToast(contexto: Context?, mensaje: String) {
        contexto?.let { ctx ->
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(ctx, mensaje, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun obtenerPedido(id: String): Pedido? {
        return _pedidos.value.find { it.id == id }
    }

    fun actualizarEstado(id: String, nuevoEstado: EstadoPedido) {
        try {
            val firestore = obtenerDb() ?: return
            firestore.collection("pedidos").document(id).update("estado", nuevoEstado.name)
                .addOnSuccessListener {
                    Log.d("MUNCH_FIREBASE", "Estado actualizado a ${nuevoEstado.name}")
                }
                .addOnFailureListener { e ->
                    Log.e("MUNCH_FIREBASE", "Error actualizando estado", e)
                }
        } catch (t: Throwable) {
            Log.e("MUNCH_FIREBASE", "Excepción actualizando estado", t)
        }
    }
}

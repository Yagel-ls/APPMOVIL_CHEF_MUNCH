package com.example.munch

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.munch.ui.theme.MunchTheme
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Crema = Color(0xFFFFF8ED)
private val Coral = Color(0xFFD95740)
private val Verde = Color(0xFF486039)
private val Texto = Color(0xFF30271F)

@Composable
fun LogoMunch(modifier: Modifier = Modifier, size: Dp = 80.dp) {
    Image(
        painter = painterResource(id = R.drawable.ic_munch_logo),
        contentDescription = "Logo Munch",
        modifier = modifier.size(size)
    )
}

data class MensajeChat(
    val remitente: String,
    val texto: String,
    val hora: String
)

data class Pedido(
    val id: String,
    val kitNombre: String,
    val platillo: String,
    val porciones: Int,
    val preferencias: String,
    val direccion: String,
    val fecha: String,
    val horario: String,
    val metodoPago: String,
    var estado: String,
    var comprobanteUri: String? = null,
    var pagoAutorizado: Boolean = false,
    var videoDesbloqueadoPorQr: Boolean = false,
    val clienteEmail: String = "cliente@munch.com",
    val chatMensajes: MutableList<MensajeChat> = mutableStateListOf(
        MensajeChat("Cliente", "Hola, por favor avísame cuando vayas en camino.", "12:30")
    )
)

data class RepartidorItem(
    val nombre: String,
    val email: String = "driver@munch.com",
    val vehiculo: String,
    val placa: String,
    val telefono: String,
    val calificacion: Double = 5.0,
    var activo: Boolean = true
)

data class Kit(
    val nombre: String,
    val subtitulo: String,
    val platillo: String,
    val minutos: Int,
    val personas: String,
    val porcionesIniciales: Int,
    val fondo: Color,
    val ingredientes: String,
    val utensilios: String,
    val videoUrl: String,
    var disponible: Boolean = true,
    var precio: Int = 350,
    val iconoEmoji: String = "🍲"
)

fun Pedido.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "kitNombre" to kitNombre,
        "platillo" to platillo,
        "porciones" to porciones,
        "preferencias" to preferencias,
        "direccion" to direccion,
        "fecha" to fecha,
        "horario" to horario,
        "metodoPago" to metodoPago,
        "estado" to estado,
        "comprobanteUri" to comprobanteUri,
        "pagoAutorizado" to pagoAutorizado,
        "videoDesbloqueadoPorQr" to videoDesbloqueadoPorQr,
        "clienteEmail" to clienteEmail,
        "chatMensajes" to chatMensajes.map { mapOf("remitente" to it.remitente, "texto" to it.texto, "hora" to it.hora) }
    )
}

fun mapToPedido(data: Map<String, Any?>): Pedido? {
    val id = data["id"] as? String ?: return null
    val kitNombre = data["kitNombre"] as? String ?: ""
    val platillo = data["platillo"] as? String ?: ""
    val porciones = (data["porciones"] as? Long)?.toInt() ?: 1
    val preferencias = data["preferencias"] as? String ?: ""
    val direccion = data["direccion"] as? String ?: ""
    val fecha = data["fecha"] as? String ?: ""
    val horario = data["horario"] as? String ?: ""
    val metodoPago = data["metodoPago"] as? String ?: ""
    val estado = data["estado"] as? String ?: "Pendiente"
    val comprobanteUri = data["comprobanteUri"] as? String
    val pagoAutorizado = data["pagoAutorizado"] as? Boolean ?: false
    val videoDesbloqueadoPorQr = data["videoDesbloqueadoPorQr"] as? Boolean ?: false
    val clienteEmail = data["clienteEmail"] as? String ?: "cliente@munch.com"

    val chatListRaw = data["chatMensajes"] as? List<Map<String, Any?>> ?: emptyList()
    val chatMensajes = chatListRaw.mapNotNull { m ->
        val rem = m["remitente"] as? String ?: "Cliente"
        val txt = m["texto"] as? String ?: ""
        val hr = m["hora"] as? String ?: ""
        MensajeChat(rem, txt, hr)
    }

    return Pedido(
        id = id,
        kitNombre = kitNombre,
        platillo = platillo,
        porciones = porciones,
        preferencias = preferencias,
        direccion = direccion,
        fecha = fecha,
        horario = horario,
        metodoPago = metodoPago,
        estado = estado,
        comprobanteUri = comprobanteUri,
        pagoAutorizado = pagoAutorizado,
        videoDesbloqueadoPorQr = videoDesbloqueadoPorQr,
        clienteEmail = clienteEmail,
        chatMensajes = mutableStateListOf(*chatMensajes.toTypedArray())
    )
}

fun Kit.toMap(): Map<String, Any?> {
    return mapOf(
        "nombre" to nombre,
        "subtitulo" to subtitulo,
        "platillo" to platillo,
        "minutos" to minutos,
        "personas" to personas,
        "porcionesIniciales" to porcionesIniciales,
        "fondo" to fondo.toArgb(),
        "ingredientes" to ingredientes,
        "utensilios" to utensilios,
        "videoUrl" to videoUrl,
        "disponible" to disponible,
        "precio" to precio,
        "iconoEmoji" to iconoEmoji
    )
}

fun mapToKit(data: Map<String, Any?>): Kit? {
    val nombre = data["nombre"] as? String ?: return null
    val subtitulo = data["subtitulo"] as? String ?: ""
    val platillo = data["platillo"] as? String ?: ""
    val minutos = (data["minutos"] as? Long)?.toInt() ?: 15
    val personas = data["personas"] as? String ?: "2 personas"
    val porcionesIniciales = (data["porcionesIniciales"] as? Long)?.toInt() ?: 2
    val fondoColor = (data["fondo"] as? Long)?.toInt() ?: 0xFFFBE1D5.toInt()
    val ingredientes = data["ingredientes"] as? String ?: ""
    val utensilios = data["utensilios"] as? String ?: ""
    val videoUrl = data["videoUrl"] as? String ?: ""
    val disponible = data["disponible"] as? Boolean ?: true
    val precio = (data["precio"] as? Long)?.toInt() ?: 350
    val iconoEmoji = data["iconoEmoji"] as? String ?: "🍲"

    return Kit(
        nombre = nombre,
        subtitulo = subtitulo,
        platillo = platillo,
        minutos = minutos,
        personas = personas,
        porcionesIniciales = porcionesIniciales,
        fondo = Color(fondoColor),
        ingredientes = ingredientes,
        utensilios = utensilios,
        videoUrl = videoUrl,
        disponible = disponible,
        precio = precio,
        iconoEmoji = iconoEmoji
    )
}

fun guardarPedidoEnFirestore(pedido: Pedido) {
    try {
        FirebaseFirestore.getInstance()
            .collection("pedidos")
            .document(pedido.id)
            .set(pedido.toMap())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun guardarKitEnFirestore(kit: Kit) {
    try {
        FirebaseFirestore.getInstance()
            .collection("kits")
            .document(kit.nombre)
            .set(kit.toMap())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun actualizarPedidoEnListaYFirestore(pedidos: MutableList<Pedido>, pedidoActualizado: Pedido) {
    val index = pedidos.indexOfFirst { it.id == pedidoActualizado.id }
    if (index != -1) {
        pedidos[index] = pedidoActualizado
    }
    guardarPedidoEnFirestore(pedidoActualizado)
}

fun actualizarKitEnListaYFirestore(kits: MutableList<Kit>, kitActualizado: Kit) {
    val index = kits.indexOfFirst { it.nombre.equals(kitActualizado.nombre, ignoreCase = true) }
    if (index != -1) {
        kits[index] = kitActualizado
    }
    guardarKitEnFirestore(kitActualizado)
}

fun eliminarKitEnFirestore(nombreKit: String) {
    try {
        FirebaseFirestore.getInstance()
            .collection("kits")
            .document(nombreKit)
            .delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun RepartidorItem.toMap(): Map<String, Any?> {
    return mapOf(
        "nombre" to nombre,
        "email" to email,
        "vehiculo" to vehiculo,
        "placa" to placa,
        "telefono" to telefono,
        "calificacion" to calificacion,
        "activo" to activo
    )
}

fun mapToRepartidor(data: Map<String, Any?>): RepartidorItem? {
    val nombre = data["nombre"] as? String ?: return null
    val email = data["email"] as? String ?: "driver@munch.com"
    val vehiculo = data["vehiculo"] as? String ?: ""
    val placa = data["placa"] as? String ?: ""
    val telefono = data["telefono"] as? String ?: ""
    val calificacion = (data["calificacion"] as? Double) ?: ((data["calificacion"] as? Long)?.toDouble() ?: 5.0)
    val activo = data["activo"] as? Boolean ?: true

    return RepartidorItem(
        nombre = nombre,
        email = email,
        vehiculo = vehiculo,
        placa = placa,
        telefono = telefono,
        calificacion = calificacion,
        activo = activo
    )
}

fun guardarRepartidorEnFirestore(repartidor: RepartidorItem) {
    try {
        FirebaseFirestore.getInstance()
            .collection("repartidores")
            .document(repartidor.nombre)
            .set(repartidor.toMap())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun eliminarRepartidorEnFirestore(nombreRepartidor: String) {
    try {
        FirebaseFirestore.getInstance()
            .collection("repartidores")
            .document(nombreRepartidor)
            .delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun actualizarRepartidorEnListaYFirestore(repartidores: MutableList<RepartidorItem>, repartidorActualizado: RepartidorItem) {
    val index = repartidores.indexOfFirst { it.nombre.equals(repartidorActualizado.nombre, ignoreCase = true) }
    if (index != -1) {
        repartidores[index] = repartidorActualizado
    }
    guardarRepartidorEnFirestore(repartidorActualizado)
}

private fun textoPorciones(cantidad: Int): String =
    if (cantidad == 1) "1 persona" else "$cantidad personas"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MunchTheme {
                MunchApp()
            }
        }
    }
}

@Composable
fun MunchApp() {
    var rol by rememberSaveable { mutableStateOf("login") }
    var emailUsuarioActivo by rememberSaveable { mutableStateOf("cliente@munch.com") }

    val kitsCompartidos = remember { mutableStateListOf<Kit>() }
    val pedidosCompartidos = remember { mutableStateListOf<Pedido>() }
    val repartidoresCompartidos = remember { mutableStateListOf<RepartidorItem>() }

    DisposableEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        val listenerKits = db.collection("kits").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                if (snapshot.isEmpty) {
                    val iniciales = listOf(
                        Kit(
                            nombre = "Date Night",
                            subtitulo = "Cena romántica para dos",
                            platillo = "Pasta cremosa de champiñones",
                            minutos = 15,
                            personas = "2 personas",
                            porcionesIniciales = 2,
                            fondo = Color(0xFFFBE1D5),
                            ingredientes = "Pasta artesanal, salsa cremosa de trufa, queso parmesano y especias finas.",
                            utensilios = "Olla, sartén, colador y espátula.",
                            videoUrl = "android.resource://com.example.munch/raw/pasta",
                            disponible = true,
                            precio = 350
                        ),
                        Kit(
                            nombre = "Family",
                            subtitulo = "Comida familiar inolvidable",
                            platillo = "Pizza artesanal para armar",
                            minutos = 25,
                            personas = "3-4 personas",
                            porcionesIniciales = 4,
                            fondo = Color(0xFFE5ECD7),
                            ingredientes = "Masa madre, salsa pomodoro italiana, queso mozzarella y albahaca fresca.",
                            utensilios = "Horno y charola para hornear.",
                            videoUrl = "android.resource://com.example.munch/raw/pizza",
                            disponible = true,
                            precio = 420
                        ),
                        Kit(
                            nombre = "Friends (Snacks)",
                            subtitulo = "Ideal para compartir",
                            platillo = "Tacos al pastor",
                            minutos = 20,
                            personas = "4-6 personas",
                            porcionesIniciales = 6,
                            fondo = Color(0xFFFFEDC3),
                            ingredientes = "Tortillas de maíz nixtamalizado, carne al pastor marinada, piña, cilantro y cebolla.",
                            utensilios = "Sartén, tabla y cuchillo.",
                            videoUrl = "android.resource://com.example.munch/raw/tacos",
                            disponible = true,
                            precio = 390
                        )
                    )
                    iniciales.forEach { guardarKitEnFirestore(it) }
                } else {
                    val listaKitsActualizada = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapToKit(it) }
                    }
                    kitsCompartidos.clear()
                    kitsCompartidos.addAll(listaKitsActualizada)
                }
            }
        }

        val listenerPedidos = db.collection("pedidos").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                if (snapshot.isEmpty) {
                    val iniciales = listOf(
                        Pedido(
                            id = "MUNCH-6018",
                            kitNombre = "Date Night",
                            platillo = "Pasta cremosa de champiñones",
                            porciones = 2,
                            preferencias = "Sin cebolla",
                            direccion = "Av. Insurgentes Sur 1234, CDMX",
                            fecha = "Hoy",
                            horario = "14:00-16:00",
                            metodoPago = "Transferencia bancaria",
                            estado = "En camino",
                            comprobanteUri = "content://comprobante_ejemplo.jpg",
                            pagoAutorizado = true,
                            clienteEmail = "cliente@munch.com"
                        ),
                        Pedido(
                            id = "MUNCH-3466",
                            kitNombre = "Family",
                            platillo = "Pizza artesanal para armar",
                            porciones = 4,
                            preferencias = "Extra queso",
                            direccion = "Reforma 222, Col. Juárez, CDMX",
                            fecha = "29/10/2026",
                            horario = "12:00-14:00",
                            metodoPago = "Efectivo contra entrega",
                            estado = "Entregado",
                            pagoAutorizado = false,
                            videoDesbloqueadoPorQr = true,
                            clienteEmail = "cliente@munch.com"
                        )
                    )
                    iniciales.forEach { guardarPedidoEnFirestore(it) }
                } else {
                    val listaActualizada = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapToPedido(it) }
                    }.sortedByDescending { it.id }

                    pedidosCompartidos.clear()
                    pedidosCompartidos.addAll(listaActualizada)
                }
            }
        }

        val listenerRepartidores = db.collection("repartidores").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                if (snapshot.isEmpty) {
                    val iniciales = listOf(
                        RepartidorItem(
                            nombre = "Carlos Rodríguez",
                            email = "driver@munch.com",
                            vehiculo = "Motoneta Itálica 125cc",
                            placa = "45ABC",
                            telefono = "5512345678",
                            calificacion = 5.0,
                            activo = true
                        ),
                        RepartidorItem(
                            nombre = "Luis Morales",
                            email = "luis@munch.com",
                            vehiculo = "Honda Cargo 150",
                            placa = "89XYZ",
                            telefono = "5598765432",
                            calificacion = 4.8,
                            activo = false
                        )
                    )
                    iniciales.forEach { guardarRepartidorEnFirestore(it) }
                } else {
                    val listaActualizada = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapToRepartidor(it) }
                    }
                    repartidoresCompartidos.clear()
                    repartidoresCompartidos.addAll(listaActualizada)
                }
            }
        }

        onDispose {
            listenerKits.remove()
            listenerPedidos.remove()
            listenerRepartidores.remove()
        }
    }

    Surface(modifier = Modifier.fillMaxSize().statusBarsPadding(), color = Crema) {
        Scaffold(containerColor = Crema) { padding ->
            val modificador = Modifier.padding(padding)

            when (rol) {
                "login" -> {
                    PantallaLogin(
                        modifier = modificador,
                        onLoginSuccess = { nuevoRol, emailIngresado ->
                            emailUsuarioActivo = emailIngresado
                            rol = nuevoRol
                        }
                    )
                }
                "cliente" -> {
                    MunchClienteFlow(
                        modifier = modificador,
                        clienteEmail = emailUsuarioActivo,
                        kits = kitsCompartidos,
                        pedidosCompartidos = pedidosCompartidos,
                        alCerrarSesion = { rol = "login" }
                    )
                }
                "chef" -> {
                    PantallaChef(
                        modifier = modificador,
                        pedidos = pedidosCompartidos,
                        repartidores = repartidoresCompartidos,
                        alCerrarSesion = { rol = "login" }
                    )
                }
                "driver" -> {
                    PantallaDriver(
                        modifier = modificador,
                        driverEmail = emailUsuarioActivo,
                        pedidos = pedidosCompartidos,
                        repartidores = repartidoresCompartidos,
                        alCerrarSesion = { rol = "login" }
                    )
                }
                "admin" -> {
                    PantallaAdmin(
                        modifier = modificador,
                        kits = kitsCompartidos,
                        pedidos = pedidosCompartidos,
                        repartidores = repartidoresCompartidos,
                        alCerrarSesion = { rol = "login" }
                    )
                }
            }
        }
    }
}

@Composable
private fun PantallaLogin(
    onLoginSuccess: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var esRegistro by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("cliente@munch.com") }
    var password by rememberSaveable { mutableStateOf("cliente123") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var errorMsg by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoMunch(size = 90.dp)
        Spacer(Modifier.height(8.dp))
        Text(text = "Munch", color = Coral, fontSize = 42.sp, fontWeight = FontWeight.Black)
        Text(
            text = if (esRegistro) "Registro de Nuevo Cliente" else "Plataforma de Kits Culinarios",
            color = Verde,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(24.dp))

        if (esRegistro) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; errorMsg = "" },
                label = { Text("Nombre completo") },
                placeholder = { Text("ej. Ana García") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMsg = "" },
            label = { Text("Correo electrónico") },
            placeholder = { Text("ej. cliente@munch.com") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMsg = "" },
            label = { Text("Contraseña de acceso") },
            placeholder = { Text("••••••") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        if (esRegistro) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMsg = "" },
                label = { Text("Confirmar contraseña") },
                placeholder = { Text("••••••") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        if (errorMsg.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(text = errorMsg, color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val e = email.trim().lowercase()
                val p = password.trim()
                if (esRegistro) {
                    if (nombre.isBlank()) {
                        errorMsg = "Por favor ingresa tu nombre completo."
                    } else if (!e.contains("@") || !e.contains(".")) {
                        errorMsg = "Ingresa un correo electrónico válido."
                    } else if (p.length < 4) {
                        errorMsg = "La contraseña debe tener al menos 4 caracteres."
                    } else if (p != confirmPassword.trim()) {
                        errorMsg = "Las contraseñas no coinciden."
                    } else {
                        try {
                            FirebaseFirestore.getInstance().collection("usuarios").document(e).get()
                                .addOnSuccessListener { doc ->
                                    if (doc != null && doc.exists()) {
                                        errorMsg = "El correo electrónico '$e' ya se encuentra registrado. Por favor inicia sesión."
                                    } else {
                                        val usuarioMap = mapOf(
                                            "nombre" to nombre.trim(),
                                            "email" to e,
                                            "rol" to "cliente",
                                            "fechaRegistro" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                                        )
                                        FirebaseFirestore.getInstance().collection("usuarios").document(e).set(usuarioMap)
                                        onLoginSuccess("cliente", e)
                                    }
                                }
                                .addOnFailureListener {
                                    onLoginSuccess("cliente", e)
                                }
                        } catch (_: Exception) {
                            onLoginSuccess("cliente", e)
                        }
                    }
                } else {
                    when {
                        e == "cliente@munch.com" && p == "cliente123" -> onLoginSuccess("cliente", e)
                        e == "chef@munch.com" && p == "chef123" -> onLoginSuccess("chef", e)
                        e == "driver@munch.com" && p == "driver123" -> onLoginSuccess("driver", e)
                        e == "admin@munch.com" && p == "admin123" -> onLoginSuccess("admin", e)
                        e.isNotBlank() && p.isNotBlank() -> onLoginSuccess("cliente", e)
                        else -> errorMsg = "Credenciales incorrectas. Verifique su correo y contraseña."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) {
            Text(
                text = if (esRegistro) "Crear cuenta de cliente" else "Iniciar sesión",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = {
            esRegistro = !esRegistro
            errorMsg = ""
            if (esRegistro) {
                nombre = ""
                email = ""
                password = ""
                confirmPassword = ""
            } else {
                email = "cliente@munch.com"
                password = "cliente123"
            }
        }) {
            Text(
                text = if (esRegistro) "¿Ya tienes cuenta? Inicia sesión aquí" else "¿No tienes cuenta? Regístrate como cliente",
                color = Verde,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        if (!esRegistro) {
            Spacer(Modifier.height(16.dp))
            Text("Acceso rápido por rol (Demo):", fontSize = 13.sp, color = Verde, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { email = "cliente@munch.com"; password = "cliente123"; errorMsg = "" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cliente", fontSize = 12.sp)
                }
                Button(
                    onClick = { email = "chef@munch.com"; password = "chef123"; errorMsg = "" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Chef", fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { email = "driver@munch.com"; password = "driver123"; errorMsg = "" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Driver", fontSize = 12.sp)
                }
                Button(
                    onClick = { email = "admin@munch.com"; password = "admin123"; errorMsg = "" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Admin", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MunchClienteFlow(
    modifier: Modifier = Modifier,
    clienteEmail: String,
    kits: List<Kit>,
    pedidosCompartidos: MutableList<Pedido>,
    alCerrarSesion: () -> Unit
) {
    var clientTab by rememberSaveable { mutableStateOf("Catálogo") }
    var pantalla by rememberSaveable { mutableStateOf(0) }
    var indiceKit by rememberSaveable { mutableStateOf(-1) }
    var porciones by rememberSaveable { mutableStateOf(2) }
    var preferencias by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf("") }
    var horario by rememberSaveable { mutableStateOf("") }
    var metodoPago by rememberSaveable { mutableStateOf("Transferencia bancaria") }
    var numeroPedido by rememberSaveable { mutableStateOf("") }
    var comprobanteUri by rememberSaveable { mutableStateOf<String?>(null) }
    var videoActivoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var pedidoParaEscanearQr by remember { mutableStateOf<Pedido?>(null) }
    var pedidoChatClienteActivo by remember { mutableStateOf<Pedido?>(null) }

    val kit = kits.getOrNull(indiceKit)
    val contexto = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            comprobanteUri = uri.toString()
        }
    }

    val abrirGoogleMapsSeguimiento: (String) -> Unit = { direccionDestino ->
        try {
            val encodedAddress = Uri.encode(direccionDestino)
            val uri = Uri.parse("geo:0,0?q=$encodedAddress")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
            contexto.startActivity(intent)
        } catch (e: Exception) {
            try {
                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(direccionDestino))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                contexto.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    val volver: () -> Unit = {
        if (videoActivoPath != null) {
            videoActivoPath = null
        } else if (pantalla == 0) {
            alCerrarSesion()
        } else {
            pantalla = if (pantalla == 4) 0 else (pantalla - 1).coerceAtLeast(0)
        }
    }

    BackHandler(enabled = true) { volver() }

    if (videoActivoPath != null) {
        PantallaReproductorVideo(videoPath = videoActivoPath!!, alCerrar = { videoActivoPath = null })
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = Crema, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { clientTab = "Catálogo"; pantalla = 0 }) {
                        Text("Catálogo", color = if (clientTab == "Catálogo") Coral else Texto, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { clientTab = "Mis Pedidos" }) {
                        Text("Mis Pedidos", color = if (clientTab == "Mis Pedidos") Coral else Texto, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = alCerrarSesion) {
                    Text("Cerrar sesión", color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (clientTab == "Mis Pedidos") {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TituloPantalla("Mis Pedidos 🧾")
                    Text("Cliente: $clienteEmail", color = Verde, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    TituloSeccion("Pedidos Activos")
                    val misPedidos = pedidosCompartidos.filter { it.clienteEmail.equals(clienteEmail, ignoreCase = true) }
                    val activos = misPedidos.filter { it.estado != "Entregado" }
                    val anteriores = misPedidos.filter { it.estado == "Entregado" }

                    if (activos.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE1D5))) {
                            Text("No tienes pedidos activos en este momento.", modifier = Modifier.padding(16.dp), color = Texto)
                        }
                    } else {
                        activos.forEach { pedido ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDC3))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "ID: ${pedido.id}", fontWeight = FontWeight.Bold, color = Coral)
                                        Text(text = pedido.estado, fontWeight = FontWeight.Bold, color = Verde)
                                    }
                                    Text(text = "${pedido.kitNombre} - ${pedido.platillo} (${pedido.porciones} pers.)", fontWeight = FontWeight.Medium, color = Texto)
                                    Text(text = "Dirección: ${pedido.direccion}", fontSize = 13.sp, color = Texto.copy(alpha = 0.8f))
                                    Text(text = "Total: $735 MXN | Pago: ${pedido.metodoPago}", fontSize = 13.sp, color = Texto.copy(alpha = 0.8f))

                                    val estatusPagoTxt = if (pedido.metodoPago.contains("Transferencia")) {
                                        if (pedido.pagoAutorizado) "Transferencia autorizada por administración" else "Esperando autorización de comprobante"
                                    } else {
                                        "Efectivo contra entrega"
                                    }
                                    Text(text = estatusPagoTxt, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (pedido.pagoAutorizado) Verde else Coral)

                                    Spacer(Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { abrirGoogleMapsSeguimiento(pedido.direccion) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
                                    ) {
                                        Text("Seguimiento en vivo (Google Maps)")
                                    }

                                    OutlinedButton(
                                        onClick = { pedidoChatClienteActivo = pedido },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Verde)
                                    ) {
                                        Text("Chat con repartidor (${pedido.chatMensajes.size})")
                                    }

                                    OutlinedButton(
                                        onClick = { },
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Video Masterclass (Bloqueado hasta entrega)")
                                    }
                                }
                            }
                        }
                    }

                    TituloSeccion("Pedidos Anteriores")
                    if (anteriores.isEmpty()) {
                        Text("Aún no tienes historial de pedidos entregados.", color = Texto.copy(alpha = 0.7f), fontSize = 13.sp)
                    } else {
                        anteriores.forEach { pedido ->
                            val kitAsociado = kits.find { it.nombre.equals(pedido.kitNombre, ignoreCase = true) } ?: kits[0]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "ID: ${pedido.id}", fontWeight = FontWeight.Bold, color = Coral)
                                        Text(text = "Entregado", color = Verde, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Text(text = "${pedido.kitNombre} - ${pedido.platillo}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Texto)
                                    Text(text = "Fecha: ${pedido.fecha} | Total: $735 MXN", fontSize = 12.sp, color = Texto.copy(alpha = 0.7f))

                                    if (pedido.videoDesbloqueadoPorQr) {
                                        Button(
                                            onClick = { videoActivoPath = kitAsociado.videoUrl },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Verde)
                                        ) {
                                            Text("Ver Video Masterclass (${kitAsociado.nombre})", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { pedidoParaEscanearQr = pedido },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Coral)
                                        ) {
                                            Text("Escanear QR del ticket para ver video", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (pantalla == 0 || kit == null) {
                PantallaInicio(
                    modifier = Modifier.fillMaxSize(),
                    kits = kits,
                    alElegirKit = { seleccionado ->
                        indiceKit = kits.indexOf(seleccionado)
                        porciones = seleccionado.porcionesIniciales
                        preferencias = ""
                        direccion = ""
                        fecha = ""
                        horario = ""
                        metodoPago = "Transferencia bancaria"
                        numeroPedido = ""
                        comprobanteUri = null
                        pantalla = 1
                    }
                )
            } else {
                when (pantalla) {
                    1 -> PantallaDetalle(
                        kit = kit,
                        modifier = Modifier.fillMaxSize(),
                        alVolver = volver,
                        alPersonalizar = { pantalla = 2 }
                    )
                    2 -> PantallaPersonalizar(
                        kit = kit,
                        porciones = porciones,
                        preferencias = preferencias,
                        alCambiarPorciones = { porciones = it },
                        alCambiarPreferencias = { preferencias = it },
                        modifier = Modifier.fillMaxSize(),
                        alVolver = volver,
                        alContinuar = { pantalla = 3 }
                    )
                    3 -> PantallaEntregaPagoConComprobanteRealConHoraPersonalizada(
                        kit = kit,
                        porciones = porciones,
                        preferencias = preferencias,
                        direccion = direccion,
                        fecha = fecha,
                        horario = horario,
                        metodoPago = metodoPago,
                        comprobanteUri = comprobanteUri,
                        alCambiarDireccion = { direccion = it },
                        alCambiarFecha = { fecha = it },
                        alCambiarHorario = { horario = it },
                        alCambiarPago = { metodoPago = it },
                        alElegirArchivo = { filePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxSize(),
                        alVolver = volver,
                        alConfirmar = {
                            val nuevoId = "MUNCH-${System.currentTimeMillis() % 10000}"
                            numeroPedido = nuevoId
                            val nuevoPedidoObj = Pedido(
                                id = nuevoId,
                                kitNombre = kit.nombre,
                                platillo = kit.platillo,
                                porciones = porciones,
                                preferencias = preferencias,
                                direccion = direccion,
                                fecha = fecha.ifBlank { "Hoy" },
                                horario = horario.ifBlank { "14:00" },
                                metodoPago = metodoPago,
                                estado = "Pendiente",
                                comprobanteUri = comprobanteUri,
                                pagoAutorizado = !metodoPago.contains("Transferencia"),
                                clienteEmail = clienteEmail
                            )
                            pedidosCompartidos.add(0, nuevoPedidoObj)
                            guardarPedidoEnFirestore(nuevoPedidoObj)
                            pantalla = 4
                        }
                    )
                    4 -> PantallaPaquete(
                        kit = kit,
                        numeroPedido = numeroPedido,
                        porciones = porciones,
                        preferencias = preferencias,
                        direccion = direccion,
                        fecha = fecha,
                        horario = horario,
                        metodoPago = metodoPago,
                        modifier = Modifier.fillMaxSize(),
                        alVolverInicio = { pantalla = 0 },
                        alCocinar = { pantalla = 5 }
                    )
                    5 -> PantallaCocina(
                        kit = kit,
                        porciones = porciones,
                        modifier = Modifier.fillMaxSize(),
                        alVolver = volver
                    )
                }
            }
        }
    }

    if (pedidoChatClienteActivo != null) {
        val pedidoActual = pedidoChatClienteActivo!!
        VentanaChatDialog(
            titulo = "Chat con repartidor",
            subtitulo = "Pedido #${pedidoActual.id}",
            esEmisorCliente = true,
            mensajes = pedidoActual.chatMensajes,
            mensajesRapidos = listOf("¿Cuánto tardas?", "Aquí estoy atento", "En la puerta del domicilio"),
            alEnviarMensaje = { texto ->
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val nuevosMensajes = pedidoActual.chatMensajes.toMutableList().apply { add(MensajeChat("Cliente", texto, time)) }
                val pedidoActualizado = pedidoActual.copy(chatMensajes = mutableStateListOf(*nuevosMensajes.toTypedArray()))
                actualizarPedidoEnListaYFirestore(pedidosCompartidos, pedidoActualizado)
                pedidoChatClienteActivo = pedidoActualizado
            },
            alCerrar = { pedidoChatClienteActivo = null }
        )
    }

    if (pedidoParaEscanearQr != null) {
        val pedidoActual = pedidoParaEscanearQr!!
        AlertDialog(
            onDismissRequest = { pedidoParaEscanearQr = null },
            containerColor = Crema,
            title = { Text("Escanear QR del ticket Munch", fontWeight = FontWeight.Bold, color = Verde) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Apunta la cámara al código QR impreso en el ticket entregado por el repartidor.", fontSize = 14.sp, color = Texto)

                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.Black, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "[Cámara QR Activa]\nEscaneando...", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Text(text = "Pedido #${pedidoActual.id} (${pedidoActual.kitNombre})", fontWeight = FontWeight.Bold, color = Coral, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pedidoActual.videoDesbloqueadoPorQr = true
                        pedidoParaEscanearQr = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Verde),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simular escaneo exitoso del QR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pedidoParaEscanearQr = null }) {
                    Text("Cancelar", color = Coral)
                }
            }
        )
    }
}

// ==========================================
// REPRODUCTOR DE VIDEOS LOCALES BUNDLEADOS (@TACOS.mp4, @PIZZA.mp4, @PASTA.mp4)
// ==========================================
@Composable
private fun PantallaReproductorVideo(videoPath: String, alCerrar: () -> Unit) {
    val context = LocalContext.current
    var reproduciendo by remember { mutableStateOf(true) }

    val uri = when {
        videoPath.contains("pasta", ignoreCase = true) -> Uri.parse("android.resource://${context.packageName}/raw/pasta")
        videoPath.contains("pizza", ignoreCase = true) -> Uri.parse("android.resource://${context.packageName}/raw/pizza")
        else -> Uri.parse("android.resource://${context.packageName}/raw/tacos")
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Masterclass del Chef Munch",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Verde, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "HD", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        reproduciendo = false
                    } else {
                        exoPlayer.play()
                        reproduciendo = true
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Verde)
            ) {
                Text(if (reproduciendo) "Pausar video" else "Reproducir video", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    exoPlayer.stop()
                    alCerrar()
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                Text("Cerrar reproductor", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PantallaInicio(modifier: Modifier = Modifier, kits: List<Kit>, alElegirKit: (Kit) -> Unit) {
    val kitsDisponibles = kits.filter { it.disponible }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(text = "Munch", color = Coral, fontSize = 46.sp, fontWeight = FontWeight.Black)
        Text(text = "La receta llega, la magia la haces tú", color = Verde, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(32.dp))
        Text(text = "¿Qué plan tienes hoy?", color = Texto, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = "Elige tu experiencia y cocina en casa.", color = Texto.copy(alpha = 0.7f), fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        if (kitsDisponibles.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFBE1D5))) {
                Text("No hay kits disponibles en este momento por mantenimiento del Chef.", modifier = Modifier.padding(16.dp), color = Texto)
            }
        } else {
            kitsDisponibles.forEach { kit ->
                TarjetaKit(kit = kit, alPulsar = { alElegirKit(kit) })
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(text = "Ingredientes listos. Momentos para compartir.", color = Verde, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun TarjetaKit(kit: Kit, alPulsar: () -> Unit) {
    var presionado by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (presionado) 0.97f else 1f, label = "scale")

    val emojiGrande = if (kit.iconoEmoji.isNotBlank()) kit.iconoEmoji else when {
        kit.nombre.contains("Date", ignoreCase = true) -> "🍝"
        kit.nombre.contains("Family", ignoreCase = true) -> "🍕"
        else -> "🌮"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(onClick = {
                presionado = true
                alPulsar()
            }),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = kit.fondo),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = kit.nombre.uppercase(), color = Texto.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text = kit.platillo, color = Texto, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(text = kit.subtitulo, color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${kit.minutos} min", color = Texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(text = kit.personas, color = Texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(text = emojiGrande, fontSize = 54.sp)
        }
    }
}

@Composable
private fun PantallaDetalle(kit: Kit, modifier: Modifier = Modifier, alVolver: () -> Unit, alPersonalizar: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BotonVolver(alVolver)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = kit.nombre.uppercase(), color = Coral, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text = kit.platillo, color = Texto, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(text = kit.subtitulo, color = Verde, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaInformacion(kit.fondo) {
                Text("Tiempo", fontSize = 12.sp, color = Texto.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                Text("${kit.minutos} min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Texto)
            }
            TarjetaInformacion(kit.fondo) {
                Text("Porciones", fontSize = 12.sp, color = Texto.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                Text(kit.personas, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Texto)
            }
        }
        TituloSeccion("Ingredientes incluidos")
        Text(text = kit.ingredientes, color = Texto.copy(alpha = 0.85f), fontSize = 15.sp)
        TituloSeccion("Utensilios necesarios")
        Text(text = kit.utensilios, color = Texto.copy(alpha = 0.85f), fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        BotonPrincipal(texto = "Personalizar kit", alPulsar = alPersonalizar)
    }
}

@Composable
private fun PantallaPersonalizar(
    kit: Kit,
    porciones: Int,
    preferencias: String,
    alCambiarPorciones: (Int) -> Unit,
    alCambiarPreferencias: (String) -> Unit,
    modifier: Modifier = Modifier,
    alVolver: () -> Unit,
    alContinuar: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BotonVolver(alVolver)
        TituloPantalla("Personaliza tu kit")
        Text(text = kit.platillo, color = Verde, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        TituloSeccion("Número de porciones")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(2, 4, 6).forEach { cantidad ->
                val seleccionado = porciones == cantidad
                OutlinedButton(
                    onClick = { alCambiarPorciones(cantidad) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (seleccionado) Coral else Color.Transparent, contentColor = if (seleccionado) Color.White else Texto)
                ) {
                    Text(text = textoPorciones(cantidad), fontWeight = FontWeight.Bold)
                }
            }
        }
        TituloSeccion("Preferencias o alergias (opcional)")
        Text("Notas predeterminadas:", fontSize = 13.sp, color = Verde, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sin cebolla", "Extra picante", "Sin lácteos").forEach { chip ->
                AssistChip(onClick = { val nuevo = if (preferencias.isBlank()) chip else "$preferencias, $chip"; alCambiarPreferencias(nuevo) }, label = { Text(chip, fontSize = 12.sp) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Bien cocido", "Bajo en sal").forEach { chip ->
                AssistChip(onClick = { val nuevo = if (preferencias.isBlank()) chip else "$preferencias, $chip"; alCambiarPreferencias(nuevo) }, label = { Text(chip, fontSize = 12.sp) })
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = preferencias,
            onValueChange = alCambiarPreferencias,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Escribe o dicta tus notas...") },
            trailingIcon = {
                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val spokenText = matches?.getOrNull(0)
                        if (!spokenText.isNullOrBlank()) {
                            val nuevo = if (preferencias.isBlank()) spokenText else "$preferencias, $spokenText"
                            alCambiarPreferencias(nuevo)
                        }
                    }
                }
                IconButton(onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dicta tus preferencias...")
                        }
                        speechLauncher.launch(intent)
                    } catch (_: Exception) {
                        alCambiarPreferencias(if (preferencias.isBlank()) "Dictado por voz" else "$preferencias, Dictado por voz")
                    }
                }) { Text("🎙️ Voz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Verde) }
            },
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(8.dp))
        BotonPrincipal(texto = "Continuar a entrega", alPulsar = alContinuar)
    }
}

// Pantalla de Entrega con selector de hora personalizado (TimePickerDialog) y datos de cuenta específicos
@Composable
private fun PantallaEntregaPagoConComprobanteRealConHoraPersonalizada(
    kit: Kit,
    porciones: Int,
    preferencias: String,
    direccion: String,
    fecha: String,
    horario: String,
    metodoPago: String,
    comprobanteUri: String?,
    alCambiarDireccion: (String) -> Unit,
    alCambiarFecha: (String) -> Unit,
    alCambiarHorario: (String) -> Unit,
    alCambiarPago: (String) -> Unit,
    alElegirArchivo: () -> Unit,
    modifier: Modifier = Modifier,
    alVolver: () -> Unit,
    alConfirmar: () -> Unit
) {
    val contexto = LocalContext.current
    var mostrarResumen by rememberSaveable { mutableStateOf(false) }

    val formularioCompleto = direccion.isNotBlank() && fecha.isNotBlank() && horario.isNotBlank() && (!metodoPago.contains("Transferencia") || comprobanteUri != null)

    val elegirFecha: () -> Unit = {
        val hoy = Calendar.getInstance()
        val selector = DatePickerDialog(contexto, { _, anio, mes, dia ->
            alCambiarFecha(String.format(Locale.getDefault(), "%02d/%02d/%04d", dia, mes + 1, anio))
        }, hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH), hoy.get(Calendar.DAY_OF_MONTH))
        selector.datePicker.minDate = hoy.timeInMillis
        selector.show()
    }

    val elegirHora: () -> Unit = {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            contexto,
            { _, hora, minuto ->
                alCambiarHorario(String.format(Locale.getDefault(), "%02d:%02d hrs", hora, minuto))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    Column(modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BotonVolver(alVolver)
        TituloPantalla("Datos y método de pago")
        Text(text = "${kit.platillo} · ${textoPorciones(porciones)}", color = Verde, fontSize = 16.sp)
        TituloSeccion("Datos de Entrega")
        OutlinedTextField(value = direccion, onValueChange = alCambiarDireccion, modifier = Modifier.fillMaxWidth(), label = { Text("Calle, número y colonia completa") }, minLines = 3, shape = RoundedCornerShape(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = elegirFecha,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = fecha.ifBlank { "Fecha (ej. 29/10)" }, color = Verde, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = elegirHora,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)
            ) {
                Text(text = horario.ifBlank { "Seleccionar hora" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Text("Horarios recomendados:", fontSize = 12.sp, color = Verde, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("12:00 hrs", "14:00 hrs", "16:00 hrs", "18:00 hrs").forEach { horaChip ->
                AssistChip(
                    onClick = { alCambiarHorario(horaChip) },
                    label = { Text(horaChip, fontSize = 11.sp) }
                )
            }
        }

        TituloSeccion("Método de Pago")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Transferencia bancaria", "Efectivo contra entrega").forEach { opcion ->
                val seleccionado = metodoPago == opcion
                OutlinedButton(
                    onClick = { alCambiarPago(opcion) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (seleccionado) Coral else Color.Transparent, contentColor = if (seleccionado) Color.White else Texto)
                ) {
                    Text(opcion, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (metodoPago.contains("Transferencia")) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EAF4))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Datos para Transferencia (BBVA):", fontWeight = FontWeight.Bold, color = Texto)
                    Text("CLABE / Cuenta: 5101250123508028", fontSize = 13.sp, color = Texto, fontWeight = FontWeight.Bold)
                    Text("Titular: Lucia Yunuenn Sanchez Gomez", fontSize = 13.sp, color = Texto, fontWeight = FontWeight.Bold)
                    Text("Total a pagar: Subtotal $700 + Envío $35 = $735 MXN", fontSize = 13.sp, color = Coral)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = alElegirArchivo, colors = ButtonDefaults.buttonColors(containerColor = if (comprobanteUri != null) Verde else Coral), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(if (comprobanteUri != null) "Comprobante adjuntado" else "Abrir galería y adjuntar comprobante")
                    }
                }
            }
        }
        if (!formularioCompleto) {
            Text(text = "Completa la dirección, fecha, horario y selecciona tu comprobante de la galería si elegiste transferencia.", color = Coral, fontSize = 13.sp)
        }
        BotonPrincipal(texto = "Confirmar y enviar pedido ($735 MXN)", habilitado = formularioCompleto, alPulsar = { mostrarResumen = true })
    }

    if (mostrarResumen) {
        AlertDialog(
            onDismissRequest = { mostrarResumen = false },
            containerColor = Crema,
            title = { TituloSeccion("Confirmar Pedido Munch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DatosPedido(kit = kit, porciones = porciones, preferencias = preferencias, direccion = direccion, fecha = fecha, horario = horario, metodoPago = metodoPago)
                    Text("Total Final: $735 MXN", fontWeight = FontWeight.Bold, color = Coral)
                }
            },
            confirmButton = { TextButton(onClick = { mostrarResumen = false; alConfirmar() }) { Text("Confirmar pedido", color = Coral, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { mostrarResumen = false }) { Text("Editar", color = Verde) } }
        )
    }
}

@Composable
private fun PantallaPaquete(kit: Kit, numeroPedido: String, porciones: Int, preferencias: String, direccion: String, fecha: String, horario: String, metodoPago: String, modifier: Modifier = Modifier, alVolverInicio: () -> Unit, alCocinar: () -> Unit) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = kit.fondo)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "¡Pedido confirmado!", color = Texto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = numeroPedido, color = Coral, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Tu kit está en camino al módulo de cocina.", color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
        TituloSeccion("Detalles del pedido")
        DatosPedido(kit = kit, porciones = porciones, preferencias = preferencias, direccion = direccion, fecha = fecha, horario = horario, metodoPago = metodoPago)
        BotonPrincipal(texto = "Ver pasos de cocina", alPulsar = alCocinar)
        OutlinedButton(onClick = alVolverInicio, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Verde)) {
            Text(text = "Volver al inicio", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class PasosCocina(val preparacion: List<String>, val coccion: List<String>, val montaje: List<String>)

private fun pasosParaKit(kit: Kit): PasosCocina {
    return when (kit.nombre) {
        "Date Night" -> PasosCocina(
            preparacion = listOf("Lavar los vegetales con agua fría.", "Picar finamente el ajo y la cebolla."),
            coccion = listOf("Hervir agua con sal y cocinar la pasta durante 9 minutos.", "Saltear el ajo, añadir la salsa cremosa y mezclar con la pasta."),
            montaje = listOf("Servir en platos hondos y espolvorear queso parmesano.", "Disfrutar de la experiencia culinaria.")
        )
        "Family" -> PasosCocina(
            preparacion = listOf("Precalentar el horno a 220°C.", "Extender las masas sobre charolas para hornear."),
            coccion = listOf("Untar la salsa de tomate y distribuir el queso y vegetales.", "Hornear durante 15 minutos hasta que el borde esté dorado."),
            montaje = listOf("Cortar en porciones iguales y servir al centro de la mesa.")
        )
        else -> PasosCocina(
            preparacion = listOf("Picar cilantro y cebolla.", "Calentar el relleno en un sartén a fuego medio."),
            coccion = listOf("Calentar las tortillas en un comal por ambos lados.", "Armar los tacos con el relleno caliente."),
            montaje = listOf("Decorar con cilantro, cebolla y salsa al gusto.")
        )
    }
}

@Composable
private fun PantallaCocina(kit: Kit, porciones: Int, modifier: Modifier = Modifier, alVolver: () -> Unit) {
    val pasos = pasosParaKit(kit)
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BotonVolver(alVolver)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "COCINA PASO A PASO", color = Coral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(text = kit.platillo, color = Texto, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(text = textoPorciones(porciones), color = Verde, fontSize = 14.sp)
            }
        }
        SeccionCocina("1. Preparación", pasos.preparacion)
        SeccionCocina("2. Cocción", pasos.coccion)
        SeccionCocina("3. Montaje y servicio", pasos.montaje)
    }
}

@Composable
private fun SeccionCocina(titulo: String, pasos: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloSeccion(titulo)
        pasos.forEachIndexed { indice, paso ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).background(Coral, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text(text = "${indice + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(text = paso, color = Texto, fontSize = 15.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DatosPedido(kit: Kit, porciones: Int, preferencias: String, direccion: String, fecha: String, horario: String, metodoPago: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "${kit.nombre}: ${kit.platillo}", fontWeight = FontWeight.Bold, color = Texto)
            Text(text = "Porciones: ${textoPorciones(porciones)}", color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
            if (preferencias.isNotBlank()) {
                Text(text = "Preferencias / Notas: $preferencias", color = Coral, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Text(text = "Dirección: $direccion", color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(text = "Fecha: $fecha ($horario)", color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(text = "Pago: $metodoPago", color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun TarjetaInformacion(fondo: Color, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.width(130.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = fondo)) {
        Column(modifier = Modifier.padding(16.dp), content = contenido)
    }
}

@Composable
private fun OpcionSeleccionable(texto: String, seleccionada: Boolean, alSeleccionar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = alSeleccionar),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (seleccionada) Color(0xFFFBE1D5) else Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = texto, color = Texto, fontSize = 15.sp, fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal)
            RadioButton(selected = seleccionada, onClick = alSeleccionar, colors = RadioButtonDefaults.colors(selectedColor = Coral))
        }
    }
}

@Composable
private fun BotonVolver(alPulsar: () -> Unit) {
    TextButton(onClick = alPulsar) {
        Text(text = "‹ Volver", color = Verde, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VentanaChatDialog(
    titulo: String,
    subtitulo: String,
    esEmisorCliente: Boolean,
    mensajes: List<MensajeChat>,
    mensajesRapidos: List<String>,
    alEnviarMensaje: (String) -> Unit,
    alCerrar: () -> Unit
) {
    var textoMensaje by remember { mutableStateOf("") }
    val miRol = if (esEmisorCliente) "Cliente" else "Driver"
    val scrollState = rememberScrollState()

    LaunchedEffect(mensajes.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.getOrNull(0)
            if (!spokenText.isNullOrBlank()) {
                alEnviarMensaje(spokenText)
            }
        }
    }

    AlertDialog(
        onDismissRequest = alCerrar,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 12.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        containerColor = Crema,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = titulo, fontWeight = FontWeight.Bold, color = Verde, fontSize = 18.sp)
                    Text(text = subtitulo, fontSize = 12.sp, color = Texto.copy(alpha = 0.7f))
                }
                IconButton(onClick = alCerrar) {
                    Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Coral)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = Texto.copy(alpha = 0.1f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 260.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mensajes.isEmpty()) {
                        Text(
                            "Inicia la conversación...",
                            fontSize = 13.sp,
                            color = Texto.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        mensajes.forEach { msg ->
                            val esMio = msg.remitente == miRol
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (esMio) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (esMio) 16.dp else 4.dp,
                                        bottomEnd = if (esMio) 4.dp else 16.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (esMio) Coral else Color.White
                                    ),
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = "${msg.remitente} • ${msg.hora}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (esMio) Color.White.copy(alpha = 0.85f) else Verde
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = msg.texto,
                                            fontSize = 14.sp,
                                            color = if (esMio) Color.White else Texto
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Texto.copy(alpha = 0.1f))

                Text("Respuestas rápidas:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Verde)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mensajesRapidos.forEach { autoMsg ->
                        AssistChip(
                            onClick = { alEnviarMensaje(autoMsg) },
                            label = { Text(autoMsg, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color.White,
                                labelColor = Texto
                            )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = textoMensaje,
                        onValueChange = { textoMensaje = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Mensaje...", fontSize = 13.sp) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla tu mensaje...")
                                }
                                speechLauncher.launch(intent)
                            } catch (_: Exception) {
                                alEnviarMensaje("Hola, aquí atento.")
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Text("🎙️", fontSize = 18.sp)
                    }

                    Button(
                        onClick = {
                            if (textoMensaje.isNotBlank()) {
                                alEnviarMensaje(textoMensaje.trim())
                                textoMensaje = ""
                            }
                        },
                        enabled = textoMensaje.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Coral,
                            disabledContainerColor = Coral.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Enviar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun TituloPantalla(texto: String) {
    Text(text = texto, color = Texto, fontSize = 28.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(text = texto, color = Verde, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun BotonPrincipal(texto: String, alPulsar: () -> Unit, habilitado: Boolean = true) {
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral, disabledContainerColor = Coral.copy(alpha = 0.5f))
    ) {
        Text(text = texto, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PantallaChef(
    modifier: Modifier = Modifier,
    pedidos: MutableList<Pedido>,
    repartidores: MutableList<RepartidorItem>,
    alCerrarSesion: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = alCerrarSesion) { Text("Cerrar sesión", color = Coral, fontWeight = FontWeight.Bold) }
            Text(text = "Chef (chef@munch.com)", color = Verde, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        TituloPantalla("Cocina y Comandas")

        // Estatus de Repartidores de Turno
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Estatus de Repartidores de Turno 🛵", fontWeight = FontWeight.Bold, color = Verde, fontSize = 14.sp)
                if (repartidores.isEmpty()) {
                    Text("No hay repartidores registrados.", fontSize = 12.sp, color = Texto.copy(alpha = 0.7f))
                } else {
                    repartidores.forEach { rep ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${rep.nombre} (${rep.vehiculo})", fontSize = 13.sp, color = Texto, fontWeight = FontWeight.Medium)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (rep.activo) Color(0xFFE5ECD7) else Color(0xFFFBE1D5)
                            ) {
                                Text(
                                    text = if (rep.activo) "🟢 Activo hoy" else "🔴 Inactivo hoy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rep.activo) Verde else Coral,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        if (pedidos.isEmpty()) {
            Text("No hay pedidos activos en la cocina.", color = Texto)
        } else {
            pedidos.forEach { pedido ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE5ECD7))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = pedido.id, fontWeight = FontWeight.Bold, color = Coral)
                            Text(text = pedido.estado, fontWeight = FontWeight.SemiBold, color = Verde)
                        }
                        Text(text = "${pedido.platillo} (${pedido.porciones} porciones)", fontWeight = FontWeight.Medium, color = Texto)
                        Text(text = "Kit: ${pedido.kitNombre} | Dir: ${pedido.direccion}", fontSize = 13.sp, color = Texto.copy(alpha = 0.8f))
                        if (pedido.preferencias.isNotBlank()) {
                            Text(text = "Nota: ${pedido.preferencias}", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (pedido.estado == "Pendiente") {
                                Button(onClick = {
                                    actualizarPedidoEnListaYFirestore(pedidos, pedido.copy(estado = "Cocinando"))
                                }, colors = ButtonDefaults.buttonColors(containerColor = Verde)) { Text("Iniciar cocina") }
                            } else if (pedido.estado == "Cocinando") {
                                Button(onClick = {
                                    actualizarPedidoEnListaYFirestore(pedidos, pedido.copy(estado = "Preparado (En cocina)"))
                                }, colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text("Terminar cocina (Avisar Driver)") }
                            } else {
                                Text(text = "Preparado (Esperando al Driver)", color = Verde, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PantallaDriver(
    modifier: Modifier = Modifier,
    driverEmail: String = "driver@munch.com",
    pedidos: MutableList<Pedido>,
    repartidores: MutableList<RepartidorItem>,
    alCerrarSesion: () -> Unit
) {
    val contexto = LocalContext.current
    var pedidoChatActivo by remember { mutableStateOf<Pedido?>(null) }
    var filtroEstado by remember { mutableStateOf("Todos") }

    val miRepartidor = repartidores.find {
        it.email.equals(driverEmail, ignoreCase = true) || it.nombre.contains("Carlos", ignoreCase = true)
    } ?: repartidores.firstOrNull()
      ?: RepartidorItem("Carlos Rodríguez", driverEmail, "Motoneta Itálica 125cc", "45ABC", "5512345678", 5.0, true)

    val abrirGoogleMaps: (String) -> Unit = { direccion ->
        try {
            val encodedAddress = Uri.encode(direccion)
            val uri = Uri.parse("geo:0,0?q=$encodedAddress")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply { setPackage("com.google.android.apps.maps") }
            contexto.startActivity(intent)
        } catch (e: Exception) {
            try {
                val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(direccion))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                contexto.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "Munch Driver", color = Coral, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(text = miRepartidor.nombre, color = Texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = "${miRepartidor.vehiculo} • Placa ${miRepartidor.placa}", color = Verde, fontSize = 12.sp)
            }
            Button(onClick = alCerrarSesion, colors = ButtonDefaults.buttonColors(containerColor = Coral), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Salir", color = Color.White, fontSize = 13.sp)
            }
        }

        // Control de Turno Activo/Inactivo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (miRepartidor.activo) Color(0xFFE5ECD7) else Color(0xFFFBE1D5))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (miRepartidor.activo) "Turno Activo 🟢" else "Turno Inactivo 🔴",
                        fontWeight = FontWeight.Bold,
                        color = if (miRepartidor.activo) Verde else Coral
                    )
                    Text(
                        text = if (miRepartidor.activo) "Disponible para entregas hoy" else "No laborando hoy (Pausado)",
                        fontSize = 12.sp,
                        color = Texto.copy(alpha = 0.8f)
                    )
                }
                Button(
                    onClick = {
                        val repartidorActualizado = miRepartidor.copy(activo = !miRepartidor.activo)
                        actualizarRepartidorEnListaYFirestore(repartidores, repartidorActualizado)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (miRepartidor.activo) Coral else Verde
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (miRepartidor.activo) "Pausar turno" else "Activar turno")
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Listos", "En Ruta").forEach { filtro ->
                val seleccionado = filtroEstado == filtro
                OutlinedButton(onClick = { filtroEstado = filtro }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (seleccionado) Coral else Color.Transparent, contentColor = if (seleccionado) Color.White else Texto)) {
                    Text(filtro, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        val entregasFiltradas = pedidos.filter { pedido ->
            when (filtroEstado) {
                "Listos" -> pedido.estado == "Preparado (En cocina)"
                "En Ruta" -> pedido.estado == "En camino"
                else -> true
            }
        }
        if (entregasFiltradas.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDC3))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No hay pedidos pendientes de reparto en este momento.", color = Texto, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            entregasFiltradas.forEach { pedido ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDC3))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "ID: ${pedido.id}", fontWeight = FontWeight.Bold, color = Coral)
                            Text(text = pedido.estado, fontWeight = FontWeight.SemiBold, color = Verde)
                        }
                        Text(text = "Destino: ${pedido.direccion}", fontWeight = FontWeight.Bold, color = Texto)
                        Text(text = "Platillo: ${pedido.platillo} (${pedido.porciones} pers.)", color = Texto.copy(alpha = 0.8f))

                        val infoPagoDriver = if (pedido.metodoPago.contains("Transferencia")) {
                            if (pedido.pagoAutorizado) "Pago: Transferencia Bancaria (Autorizado)" else "Pago: Transferencia (Pendiente de autorización)"
                        } else {
                            "Pago: Efectivo contra entrega (Cobrar $735 MXN)"
                        }
                        Text(text = infoPagoDriver, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (pedido.pagoAutorizado || !pedido.metodoPago.contains("Transferencia")) Verde else Coral)

                        if (pedido.preferencias.isNotBlank()) {
                            Text(text = "Notas: ${pedido.preferencias}", fontSize = 13.sp, color = Coral, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { abrirGoogleMaps(pedido.direccion) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)) {
                            Text("Abrir ruta en Google Maps (GPS)")
                        }
                        OutlinedButton(onClick = { pedidoChatActivo = pedido }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Verde)) {
                            Text("Chat en vivo con cliente (${pedido.chatMensajes.size})")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (pedido.estado == "Preparado (En cocina)") {
                                Button(onClick = {
                                    actualizarPedidoEnListaYFirestore(pedidos, pedido.copy(estado = "En camino"))
                                }, colors = ButtonDefaults.buttonColors(containerColor = Verde)) { Text("Tomar pedido") }
                            } else if (pedido.estado == "En camino") {
                                Button(onClick = {
                                    actualizarPedidoEnListaYFirestore(pedidos, pedido.copy(estado = "Entregado"))
                                }, colors = ButtonDefaults.buttonColors(containerColor = Coral)) { Text("Marcar entregado") }
                            } else {
                                Text(
                                    text = if (pedido.estado == "Entregado") "Entregado" else "En proceso",
                                    color = Verde,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pedidoChatActivo != null) {
        val pedidoActual = pedidoChatActivo!!
        VentanaChatDialog(
            titulo = "Chat en vivo con cliente",
            subtitulo = "Pedido #${pedidoActual.id}",
            esEmisorCliente = false,
            mensajes = pedidoActual.chatMensajes,
            mensajesRapidos = listOf("Voy en camino", "Llegué al domicilio", "¿Me ayudas con el timbre?", "A 5 minutos de entrega"),
            alEnviarMensaje = { texto ->
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val nuevosMensajes = pedidoActual.chatMensajes.toMutableList().apply { add(MensajeChat("Driver", texto, time)) }
                val pedidoActualizado = pedidoActual.copy(chatMensajes = mutableStateListOf(*nuevosMensajes.toTypedArray()))
                actualizarPedidoEnListaYFirestore(pedidos, pedidoActualizado)
                pedidoChatActivo = pedidoActualizado
            },
            alCerrar = { pedidoChatActivo = null }
        )
    }
}

@Composable
private fun PantallaAdmin(
    modifier: Modifier = Modifier,
    kits: MutableList<Kit>,
    pedidos: MutableList<Pedido>,
    repartidores: MutableList<RepartidorItem>,
    alCerrarSesion: () -> Unit
) {
    var adminTab by rememberSaveable { mutableStateOf("Pedidos") }
    var mostrarDialogoNuevoKit by remember { mutableStateOf(false) }
    var mostrarDialogoNuevoRepartidor by remember { mutableStateOf(false) }
    var kitAEditar by remember { mutableStateOf<Kit?>(null) }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "Munch Admin", color = Coral, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(text = "Gestión integral de pedidos, menú y repartidores", color = Verde, fontSize = 12.sp)
            }
            Button(onClick = alCerrarSesion, colors = ButtonDefaults.buttonColors(containerColor = Coral), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Text("Salir", color = Color.White, fontSize = 13.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Pedidos", "Menú", "Repartidores").forEach { tab ->
                val seleccionado = adminTab == tab
                OutlinedButton(onClick = { adminTab = tab }, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (seleccionado) Coral else Color.Transparent, contentColor = if (seleccionado) Color.White else Texto), modifier = Modifier.weight(1f)) {
                    Text(tab, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        when (adminTab) {
            "Pedidos" -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EAF4))) {
                        Column(modifier = Modifier.padding(16.dp)) { Text("Ventas", fontSize = 12.sp, color = Texto.copy(alpha = 0.7f)); Text("\$3,012 MXN", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Texto) }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE5ECD7))) {
                        Column(modifier = Modifier.padding(16.dp)) { Text("Total", fontSize = 12.sp, color = Texto.copy(alpha = 0.7f)); Text("${pedidos.size} Pedidos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Verde) }
                    }
                }
                TituloSeccion("Listado de Pedidos y Comprobantes")
                pedidos.forEach { pedido ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "ID: ${pedido.id}", fontWeight = FontWeight.Bold, color = Coral)
                                Text(text = pedido.estado, fontWeight = FontWeight.SemiBold, color = Verde)
                            }
                            Text(text = "${pedido.kitNombre} - ${pedido.platillo}", fontWeight = FontWeight.Medium, color = Texto)
                            Text(text = "Método de Pago: ${pedido.metodoPago}", fontSize = 13.sp, color = Texto.copy(alpha = 0.8f))

                            if (pedido.metodoPago.contains("Transferencia")) {
                                val estadoComprobante = if (pedido.comprobanteUri != null) "Comprobante adjuntado" else "Sin comprobante aún"
                                Text(text = estadoComprobante, fontSize = 12.sp, color = Texto)

                                if (pedido.comprobanteUri != null) {
                                    if (pedido.pagoAutorizado) {
                                        Text(text = "Pago autorizado por administración", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Verde)
                                    } else {
                                        Button(onClick = {
                                            actualizarPedidoEnListaYFirestore(pedidos, pedido.copy(pagoAutorizado = true))
                                        }, colors = ButtonDefaults.buttonColors(containerColor = Verde), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                            Text("Autorizar pago de transferencia", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            if (pedido.preferencias.isNotBlank()) { Text(text = "Nota: ${pedido.preferencias}", fontSize = 13.sp, color = Coral) }
                            Text(text = "Dir: ${pedido.direccion}", fontSize = 13.sp, color = Texto.copy(alpha = 0.8f))
                        }
                    }
                }
            }
            "Menú" -> {
                Button(onClick = { mostrarDialogoNuevoKit = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Coral)) {
                    Text("Agregar nuevo kit / platillo", fontWeight = FontWeight.Bold)
                }

                kits.forEachIndexed { index, kit ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = kit.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Texto)
                                Surface(shape = RoundedCornerShape(8.dp), color = if (kit.disponible) Color(0xFFE5ECD7) else Color(0xFFFBE1D5)) {
                                    Text(text = if (kit.disponible) "Disponible" else "Pausado", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (kit.disponible) Verde else Coral)
                                }
                            }
                            Text(text = kit.platillo, color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
                            Text(text = "Precio: \$${kit.precio} MXN | ${kit.porcionesIniciales} personas | ${kit.minutos} min", fontSize = 13.sp, color = Verde)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    actualizarKitEnListaYFirestore(kits, kit.copy(disponible = !kit.disponible))
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = if (kit.disponible) Coral else Verde)) {
                                    Text(if (kit.disponible) "Pausar" else "Activar")
                                }
                                OutlinedButton(onClick = {
                                    kitAEditar = kit
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Verde)) {
                                    Text("Editar")
                                }
                                OutlinedButton(onClick = {
                                    val kitEliminado = kits.removeAt(index)
                                    eliminarKitEnFirestore(kitEliminado.nombre)
                                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
            "Repartidores" -> {
                Button(
                    onClick = { mostrarDialogoNuevoRepartidor = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral)
                ) {
                    Text("＋ Agregar nuevo repartidor", fontWeight = FontWeight.Bold)
                }

                if (repartidores.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EAF4))) {
                        Text("No hay repartidores registrados.", modifier = Modifier.padding(16.dp), color = Texto)
                    }
                } else {
                    repartidores.forEachIndexed { index, rep ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = rep.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Texto)
                                    Surface(shape = RoundedCornerShape(8.dp), color = if (rep.activo) Color(0xFFE5ECD7) else Color(0xFFFBE1D5)) {
                                        Text(text = if (rep.activo) "🟢 Activo" else "🔴 Inactivo", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (rep.activo) Verde else Coral)
                                    }
                                }
                                Text(text = "Vehículo: ${rep.vehiculo} | Placa: ${rep.placa}", color = Texto.copy(alpha = 0.8f), fontSize = 14.sp)
                                Text(text = "Tel: ${rep.telefono} | ⭐ ${rep.calificacion}", fontSize = 13.sp, color = Verde)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    OutlinedButton(onClick = {
                                        val repartidorEliminado = repartidores.removeAt(index)
                                        eliminarRepartidorEnFirestore(repartidorEliminado.nombre)
                                    }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral)) {
                                        Text("Eliminar repartidor")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoNuevoKit) {
        var nombreKit by remember { mutableStateOf("") }
        var platilloKit by remember { mutableStateOf("") }
        var precioKit by remember { mutableStateOf("") }
        var iconoSeleccionado by remember { mutableStateOf("🍲") }
        val iconosOpciones = listOf("🍝", "🍕", "🌮", "🍔", "🍣", "🥗", "🍲", "🍰", "🍳", "🥩")

        AlertDialog(
            onDismissRequest = { mostrarDialogoNuevoKit = false },
            containerColor = Crema,
            title = { Text("Agregar nuevo kit / platillo al menú", fontWeight = FontWeight.Bold, color = Verde) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = nombreKit, onValueChange = { nombreKit = it }, label = { Text("Nombre del kit") }, placeholder = { Text("ej. Gourmet Burger") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = platilloKit, onValueChange = { platilloKit = it }, label = { Text("Platillo") }, placeholder = { Text("ej. Hamburguesa artesanal") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = precioKit, onValueChange = { precioKit = it }, label = { Text("Precio en MXN") }, placeholder = { Text("380") }, shape = RoundedCornerShape(12.dp))

                    Text("Selecciona un ícono para el platillo:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Verde)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconosOpciones.forEach { emoji ->
                            FilterChip(
                                selected = (iconoSeleccionado == emoji),
                                onClick = { iconoSeleccionado = emoji },
                                label = { Text(emoji, fontSize = 20.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Coral,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombreKit.isNotBlank() && platilloKit.isNotBlank()) {
                        val nuevoKit = Kit(
                            nombre = nombreKit.trim(),
                            subtitulo = "Especialidad del Chef",
                            platillo = platilloKit.trim(),
                            minutos = 20,
                            personas = "2 personas",
                            porcionesIniciales = 2,
                            fondo = Color(0xFFFFEDC3),
                            ingredientes = "Ingredientes frescos seleccionados de alta calidad.",
                            utensilios = "Sartén y espátula.",
                            videoUrl = "android.resource://com.example.munch/raw/pasta",
                            disponible = true,
                            precio = precioKit.toIntOrNull() ?: 380,
                            iconoEmoji = iconoSeleccionado
                        )
                        kits.add(nuevoKit)
                        guardarKitEnFirestore(nuevoKit)
                        mostrarDialogoNuevoKit = false
                    }
                }) {
                    Text("Agregar y publicar al catálogo", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNuevoKit = false }) {
                    Text("Cancelar", color = Verde, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (mostrarDialogoNuevoRepartidor) {
        var nombreRep by remember { mutableStateOf("") }
        var emailRep by remember { mutableStateOf("") }
        var passwordRep by remember { mutableStateOf("") }
        var vehiculoRep by remember { mutableStateOf("") }
        var placaRep by remember { mutableStateOf("") }
        var telefonoRep by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoNuevoRepartidor = false },
            containerColor = Crema,
            title = { Text("Agregar nuevo repartidor", fontWeight = FontWeight.Bold, color = Verde) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = nombreRep, onValueChange = { nombreRep = it }, label = { Text("Nombre completo") }, placeholder = { Text("ej. Luis Morales") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = emailRep, onValueChange = { emailRep = it }, label = { Text("Correo de acceso") }, placeholder = { Text("ej. repartidor@munch.com") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = passwordRep, onValueChange = { passwordRep = it }, label = { Text("Contraseña de acceso") }, placeholder = { Text("••••••") }, shape = RoundedCornerShape(12.dp), visualTransformation = PasswordVisualTransformation())
                    OutlinedTextField(value = vehiculoRep, onValueChange = { vehiculoRep = it }, label = { Text("Vehículo") }, placeholder = { Text("ej. Motoneta Itálica 125cc") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = placaRep, onValueChange = { placaRep = it }, label = { Text("Placa o Identificador") }, placeholder = { Text("ej. 89XYZ") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = telefonoRep, onValueChange = { telefonoRep = it }, label = { Text("Teléfono de contacto") }, placeholder = { Text("ej. 5512345678") }, shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val e = emailRep.trim().lowercase()
                    val p = passwordRep.trim()
                    if (nombreRep.isNotBlank() && e.isNotBlank()) {
                        val nuevoRepartidor = RepartidorItem(
                            nombre = nombreRep.trim(),
                            email = e,
                            vehiculo = vehiculoRep.ifBlank { "Motoneta" },
                            placa = placaRep.ifBlank { "000AA" },
                            telefono = telefonoRep.ifBlank { "5500000000" },
                            calificacion = 5.0,
                            activo = true
                        )
                        repartidores.add(nuevoRepartidor)
                        guardarRepartidorEnFirestore(nuevoRepartidor)

                        try {
                            val usuarioDriverMap = mapOf(
                                "nombre" to nombreRep.trim(),
                                "email" to e,
                                "password" to p.ifBlank { "driver123" },
                                "rol" to "driver",
                                "fechaRegistro" to SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                            )
                            FirebaseFirestore.getInstance().collection("usuarios").document(e).set(usuarioDriverMap)
                        } catch (_: Exception) {}

                        mostrarDialogoNuevoRepartidor = false
                    }
                }) {
                    Text("Registrar repartidor", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoNuevoRepartidor = false }) {
                    Text("Cancelar", color = Verde, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (kitAEditar != null) {
        val kitActual = kitAEditar!!
        var nombreKit by remember(kitActual) { mutableStateOf(kitActual.nombre) }
        var subtituloKit by remember(kitActual) { mutableStateOf(kitActual.subtitulo) }
        var platilloKit by remember(kitActual) { mutableStateOf(kitActual.platillo) }
        var precioKit by remember(kitActual) { mutableStateOf(kitActual.precio.toString()) }
        var minutosKit by remember(kitActual) { mutableStateOf(kitActual.minutos.toString()) }
        var ingredientesKit by remember(kitActual) { mutableStateOf(kitActual.ingredientes) }
        var iconoSeleccionado by remember(kitActual) { mutableStateOf(if (kitActual.iconoEmoji.isNotBlank()) kitActual.iconoEmoji else "🍲") }
        val iconosOpciones = listOf("🍝", "🍕", "🌮", "🍔", "🍣", "🥗", "🍲", "🍰", "🍳", "🥩")

        AlertDialog(
            onDismissRequest = { kitAEditar = null },
            containerColor = Crema,
            title = { Text("Editar kit / platillo", fontWeight = FontWeight.Bold, color = Verde) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(value = nombreKit, onValueChange = { nombreKit = it }, label = { Text("Nombre del kit") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = subtituloKit, onValueChange = { subtituloKit = it }, label = { Text("Subtítulo") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = platilloKit, onValueChange = { platilloKit = it }, label = { Text("Platillo") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = precioKit, onValueChange = { precioKit = it }, label = { Text("Precio en MXN") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = minutosKit, onValueChange = { minutosKit = it }, label = { Text("Minutos de preparación") }, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = ingredientesKit, onValueChange = { ingredientesKit = it }, label = { Text("Ingredientes") }, shape = RoundedCornerShape(12.dp), maxLines = 3)

                    Text("Ícono del platillo:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Verde)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconosOpciones.forEach { emoji ->
                            FilterChip(
                                selected = (iconoSeleccionado == emoji),
                                onClick = { iconoSeleccionado = emoji },
                                label = { Text(emoji, fontSize = 20.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Coral,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombreKit.isNotBlank() && platilloKit.isNotBlank()) {
                        val kitModificado = kitActual.copy(
                            nombre = nombreKit.trim(),
                            subtitulo = subtituloKit.trim(),
                            platillo = platilloKit.trim(),
                            precio = precioKit.toIntOrNull() ?: kitActual.precio,
                            minutos = minutosKit.toIntOrNull() ?: kitActual.minutos,
                            ingredientes = ingredientesKit.trim(),
                            iconoEmoji = iconoSeleccionado
                        )
                        actualizarKitEnListaYFirestore(kits, kitModificado)
                        kitAEditar = null
                    }
                }) {
                    Text("Guardar cambios", color = Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { kitAEditar = null }) {
                    Text("Cancelar", color = Verde, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

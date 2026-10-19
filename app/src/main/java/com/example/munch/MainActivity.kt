package com.example.munch

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.munch.data.DatosBanco
import com.example.munch.data.EstadoPedido
import com.example.munch.data.Pedido
import com.example.munch.data.RepositorioPedidos
import com.example.munch.ui.theme.MunchTheme
import com.google.firebase.FirebaseApp
import java.util.Calendar
import java.util.Locale

private val Crema = Color(0xFFFFF8ED)
private val Coral = Color(0xFFD95740)
private val Verde = Color(0xFF486039)
private val Texto = Color(0xFF30271F)

private data class Kit(
    val nombre: String,
    val subtitulo: String,
    val platillo: String,
    val minutos: Int,
    val personas: String,
    val porcionesIniciales: Int,
    val emoji: String,
    val fondo: Color,
    val ingredientes: String,
    val utensilios: String
)

private val kits = listOf(
    Kit(
        nombre = "Date Night",
        subtitulo = "Cena para dos",
        platillo = "Pasta cremosa",
        minutos = 35,
        personas = "2 personas",
        porcionesIniciales = 2,
        emoji = "🍝",
        fondo = Color(0xFFFBE1D5),
        ingredientes = "Pasta, salsa cremosa, queso y especias.",
        utensilios = "Olla, sartén, colador y agua."
    ),
    Kit(
        nombre = "Family",
        subtitulo = "Comida juntos",
        platillo = "Pizzas artesanales",
        minutos = 45,
        personas = "4 personas",
        porcionesIniciales = 4,
        emoji = "🍕",
        fondo = Color(0xFFE5ECD7),
        ingredientes = "Masa, salsa de tomate, queso y vegetales.",
        utensilios = "Horno y charola para hornear."
    ),
    Kit(
        nombre = "Friends",
        subtitulo = "Comparte con amigos",
        platillo = "Tacos",
        minutos = 20,
        personas = "4–6 personas",
        porcionesIniciales = 6,
        emoji = "🌮",
        fondo = Color(0xFFFFEDC3),
        ingredientes = "Tortillas, relleno, salsa, cebolla y cilantro.",
        utensilios = "Sartén, tabla y cuchillo."
    )
)

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
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        RepositorioPedidos.inicializar(contexto)
    }
    // 0: inicio, 1: detalle, 2: personalización,
    // 3: entrega y pago, 4: tu paquete, 5: cocina paso a paso
    var pantalla by rememberSaveable { mutableStateOf(0) }
    var indiceKit by rememberSaveable { mutableStateOf(-1) }
    var porciones by rememberSaveable { mutableStateOf(2) }
    var preferencias by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    var fecha by rememberSaveable { mutableStateOf("") }
    var horario by rememberSaveable { mutableStateOf("") }
    var metodoPago by rememberSaveable {
        mutableStateOf("Transferencia bancaria")
    }
    var comprobanteUri by rememberSaveable { mutableStateOf("") }
    var numeroPedido by rememberSaveable { mutableStateOf("") }

    val kit = kits.getOrNull(indiceKit)

    val volver: () -> Unit = {
        pantalla = if (pantalla == 4) {
            0
        } else {
            (pantalla - 1).coerceAtLeast(0)
        }
    }

    BackHandler(enabled = pantalla > 0) {
        volver()
    }

    Scaffold(containerColor = Crema) { padding ->
        val modificador = Modifier.padding(padding)

        if (pantalla == 0 || kit == null) {
            PantallaInicio(
                modifier = modificador,
                alElegirKit = { seleccionado ->
                    indiceKit = kits.indexOf(seleccionado)
                    porciones = seleccionado.porcionesIniciales
                    preferencias = ""
                    direccion = ""
                    fecha = ""
                    horario = ""
                    metodoPago = "Transferencia bancaria"
                    numeroPedido = ""
                    pantalla = 1
                }
            )
        } else {
            when (pantalla) {
                1 -> PantallaDetalle(
                    kit = kit,
                    modifier = modificador,
                    alVolver = volver,
                    alPersonalizar = { pantalla = 2 }
                )

                2 -> PantallaPersonalizar(
                    kit = kit,
                    porciones = porciones,
                    preferencias = preferencias,
                    alCambiarPorciones = { porciones = it },
                    alCambiarPreferencias = { preferencias = it },
                    modifier = modificador,
                    alVolver = volver,
                    alContinuar = { pantalla = 3 }
                )

                3 -> PantallaEntregaPago(
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
                    alCambiarComprobante = { comprobanteUri = it },
                    modifier = modificador,
                    alVolver = volver,
                    alConfirmar = { alFin ->
                        val idGenerado = "DEMO-${System.currentTimeMillis().toString().takeLast(4)}"
                        val nuevoPedido = Pedido(
                            id = idGenerado,
                            kitNombre = kit.nombre,
                            platillo = kit.platillo,
                            porciones = porciones,
                            preferencias = preferencias,
                            direccion = direccion,
                            fecha = fecha,
                            horario = horario,
                            metodoPago = metodoPago,
                            comprobanteUri = comprobanteUri.ifBlank { null },
                            estado = if (metodoPago == "Transferencia bancaria") EstadoPedido.COMPROBANTE_ENVIADO else EstadoPedido.PAGO_CONFIRMADO
                        )

                        RepositorioPedidos.guardarPedidoConContexto(
                            contexto = contexto,
                            pedido = nuevoPedido,
                            alInicio = {},
                            alExito = {
                                numeroPedido = idGenerado
                                alFin()
                                pantalla = 4
                            },
                            alError = {
                                alFin()
                            }
                        )
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
                    modifier = modificador,
                    alVolverInicio = { pantalla = 0 },
                    alCocinar = { pantalla = 5 }
                )

                5 -> PantallaCocina(
                    kit = kit,
                    porciones = porciones,
                    modifier = modificador,
                    alVolver = volver
                )
            }
        }
    }
}

@Composable
private fun PantallaInicio(
    modifier: Modifier = Modifier,
    alElegirKit: (Kit) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Munch",
            color = Coral,
            fontSize = 46.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "La receta llega, la magia la haces tú",
            color = Verde,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "¿Qué plan tienes hoy?",
            color = Texto,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Elige tu experiencia y cocina en casa.",
            color = Texto.copy(alpha = 0.7f),
            fontSize = 15.sp
        )

        Spacer(Modifier.height(24.dp))

        kits.forEach { kit ->
            TarjetaKit(
                kit = kit,
                alPulsar = { alElegirKit(kit) }
            )
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Ingredientes listos. Momentos para compartir.",
            color = Verde,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun TarjetaKit(
    kit: Kit,
    alPulsar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alPulsar),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = kit.fondo)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = kit.nombre,
                    color = if (kit.nombre == "Family") Verde else Coral,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = kit.subtitulo,
                    color = Texto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = kit.platillo,
                    color = Texto,
                    fontSize = 14.sp
                )

                Text(
                    text = "${kit.minutos} min · ${kit.personas}",
                    color = Texto.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            Text(
                text = kit.emoji,
                fontSize = 48.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Text(
                text = "›",
                color = Texto,
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun PantallaDetalle(
    kit: Kit,
    modifier: Modifier = Modifier,
    alVolver: () -> Unit,
    alPersonalizar: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        BotonVolver(alVolver)

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(kit.fondo, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = kit.emoji, fontSize = 100.sp)
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = kit.nombre,
            color = Coral,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Kit de ${kit.platillo.lowercase()}",
            color = Texto,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "◷ ${kit.minutos} min · ${kit.personas}",
            color = Verde,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(28.dp))

        TituloSeccion("¿Qué incluye?")

        Spacer(Modifier.height(8.dp))

        Text(
            text = kit.ingredientes,
            color = Texto,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(24.dp))

        TarjetaInformacion(fondo = Color(0xFFE5ECD7)) {
            Text(
                text = "Necesitas en casa",
                color = Verde,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = kit.utensilios,
                color = Texto,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        BotonPrincipal(
            texto = "Personalizar kit  ›",
            alPulsar = alPersonalizar
        )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BotonVolver(alVolver)

        TituloPantalla("Personaliza tu kit")

        Text(
            text = "${kit.nombre} · ${kit.platillo}",
            color = Verde,
            fontSize = 16.sp
        )

        TarjetaInformacion {
            TituloSeccion("Porciones")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { alCambiarPorciones(porciones - 1) },
                    enabled = porciones > 1
                ) {
                    Text("−", fontSize = 24.sp)
                }

                Text(
                    text = textoPorciones(porciones),
                    color = Texto,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedButton(
                    onClick = { alCambiarPorciones(porciones + 1) },
                    enabled = porciones < 10
                ) {
                    Text("+", fontSize = 24.sp)
                }
            }
        }

        TituloSeccion("Preferencias")

        OutlinedTextField(
            value = preferencias,
            onValueChange = alCambiarPreferencias,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notas para tu kit") },
            placeholder = { Text("Ejemplo: sin picante") },
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )

        Text(
            text = "Las modificaciones están sujetas a disponibilidad.",
            color = Verde,
            fontSize = 13.sp
        )

        TarjetaInformacion(fondo = kit.fondo) {
            TituloSeccion("Tu selección")

            Text(
                text = "${kit.platillo} · ${textoPorciones(porciones)}",
                color = Texto
            )

            if (preferencias.isNotBlank()) {
                Text(
                    text = "Notas: $preferencias",
                    color = Texto
                )
            }
        }

        BotonPrincipal(
            texto = "Continuar  ›",
            alPulsar = alContinuar
        )
    }
}

@Composable
private fun PantallaEntregaPago(
    kit: Kit,
    porciones: Int,
    preferencias: String,
    direccion: String,
    fecha: String,
    horario: String,
    metodoPago: String,
    comprobanteUri: String,
    alCambiarDireccion: (String) -> Unit,
    alCambiarFecha: (String) -> Unit,
    alCambiarHorario: (String) -> Unit,
    alCambiarPago: (String) -> Unit,
    alCambiarComprobante: (String) -> Unit,
    modifier: Modifier = Modifier,
    alVolver: () -> Unit,
    alConfirmar: (() -> Unit) -> Unit
) {
    val contexto = LocalContext.current

    var mostrarResumen by rememberSaveable { mutableStateOf(false) }
    var cargando by rememberSaveable { mutableStateOf(false) }

    val necesitaComprobante = metodoPago == "Transferencia bancaria" && comprobanteUri.isBlank()
    val formularioCompleto = direccion.isNotBlank() && fecha.isNotBlank() && horario.isNotBlank() && !necesitaComprobante

    val elegirFecha: () -> Unit = {
        val hoy = Calendar.getInstance()

        val selector = DatePickerDialog(
            contexto,
            { _, anio, mes, dia ->
                alCambiarFecha(
                    String.format(
                        Locale.getDefault(),
                        "%02d/%02d/%04d",
                        dia,
                        mes + 1,
                        anio
                    )
                )
            },
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        )

        val inicioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        selector.datePicker.minDate = inicioHoy.timeInMillis
        selector.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BotonVolver(alVolver)

        TituloPantalla("Entrega y pago")

        Text(
            text = "${kit.platillo} · ${textoPorciones(porciones)}",
            color = Verde,
            fontSize = 16.sp
        )

        TituloSeccion("Dirección de entrega")

        OutlinedTextField(
            value = direccion,
            onValueChange = alCambiarDireccion,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dirección completa") },
            placeholder = {
                Text("Calle, número, colonia, ciudad y código postal")
            },
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )

        TituloSeccion("Fecha de entrega")

        OutlinedButton(
            onClick = elegirFecha,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = fecha.ifBlank { "Seleccionar fecha" },
                color = Verde,
                fontSize = 16.sp
            )
        }

        TituloSeccion("Horario de entrega")

        Column {
            listOf(
                "12:00–14:00",
                "14:00–16:00",
                "16:00–18:00"
            ).forEach { opcion ->
                OpcionSeleccionable(
                    texto = opcion,
                    seleccionada = horario == opcion,
                    alSeleccionar = { alCambiarHorario(opcion) }
                )
            }
        }

        TituloSeccion("Método de pago")

        Column {
            listOf(
                "Transferencia bancaria",
                "Pago al recibir"
            ).forEach { opcion ->
                OpcionSeleccionable(
                    texto = opcion,
                    seleccionada = metodoPago == opcion,
                    alSeleccionar = { alCambiarPago(opcion) }
                )
            }
        }

        if (metodoPago == "Transferencia bancaria") {
            val datosBanco = DatosBanco()

            TarjetaInformacion(fondo = Color(0xFFFFEDC3)) {
                Text(
                    text = "Datos para transferencia bancaria",
                    color = Coral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(text = "Banco: ${datosBanco.banco}", color = Texto, fontWeight = FontWeight.SemiBold)
                Text(text = "CLABE: ${datosBanco.clabe}", color = Texto, fontWeight = FontWeight.Bold)
                Text(text = "Titular: ${datosBanco.titular}", color = Texto)
                Text(text = "Concepto: ${datosBanco.concepto}", color = Texto)
            }

            TituloSeccion("Comprobante de pago")

            val selectorImagen = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    alCambiarComprobante(uri.toString())
                }
            }

            if (comprobanteUri.isNotBlank()) {
                TarjetaInformacion(fondo = Color(0xFFE5ECD7)) {
                    Text(
                        text = "✓ Comprobante adjuntado",
                        color = Verde,
                        fontWeight = FontWeight.Bold
                    )

                    ImagenComprobante(
                        modelo = comprobanteUri,
                        contentDescription = "Comprobante",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            selectorImagen.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cambiar imagen de comprobante", color = Verde)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        selectorImagen.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("📷 Adjuntar captura de transferencia", color = Coral, fontSize = 16.sp)
                }
            }
        }

        if (necesitaComprobante) {
            Text(
                text = "⚠️ Debes adjuntar la captura de tu transferencia para poder continuar.",
                color = Coral,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        } else if (!formularioCompleto) {
            Text(
                text = "Completa la dirección, fecha y horario para continuar.",
                color = Verde,
                fontSize = 14.sp
            )
        }

        BotonPrincipal(
            texto = "Revisar pedido  ›",
            habilitado = formularioCompleto && !cargando,
            alPulsar = { mostrarResumen = true }
        )
    }

    if (cargando) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = Crema,
            title = {
                Text(
                    text = "Subiendo comprobante...",
                    color = Coral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(color = Coral)
                    Text(
                        text = "Subiendo imagen a Firebase Storage y registrando pedido...",
                        color = Texto,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {}
        )
    }

    if (mostrarResumen) {
        AlertDialog(
            onDismissRequest = { if (!cargando) mostrarResumen = false },
            containerColor = Crema,
            title = {
                TituloSeccion("Resumen del pedido")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatosPedido(
                        kit = kit,
                        porciones = porciones,
                        preferencias = preferencias,
                        direccion = direccion,
                        fecha = fecha,
                        horario = horario,
                        metodoPago = metodoPago
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !cargando,
                    onClick = {
                        mostrarResumen = false
                        cargando = true
                        alConfirmar {
                            cargando = false
                        }
                    }
                ) {
                    Text(
                        text = if (cargando) "Procesando..." else "Confirmar pedido",
                        color = Coral
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !cargando,
                    onClick = { mostrarResumen = false }
                ) {
                    Text("Editar", color = Verde)
                }
            }
        )
    }
}

@Composable
private fun PantallaPaquete(
    kit: Kit,
    numeroPedido: String,
    porciones: Int,
    preferencias: String,
    direccion: String,
    fecha: String,
    horario: String,
    metodoPago: String,
    modifier: Modifier = Modifier,
    alVolverInicio: () -> Unit,
    alCocinar: () -> Unit
) {
    val listaPedidos by RepositorioPedidos.pedidos.collectAsState()
    val pedidoActual = listaPedidos.find { it.id == numeroPedido }
    val estadoActual = pedidoActual?.estado ?: if (metodoPago == "Transferencia bancaria") EstadoPedido.COMPROBANTE_ENVIADO else EstadoPedido.PAGO_CONFIRMADO

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = alVolverInicio) {
            Text(
                text = "← Inicio",
                color = Verde,
                fontSize = 16.sp
            )
        }

        TituloPantalla("Tu paquete")

        Text(
            text = "Pedido: $numeroPedido",
            color = Coral,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        TarjetaInformacion(
            fondo = when (estadoActual) {
                EstadoPedido.PAGO_CONFIRMADO -> Color(0xFFE5ECD7)
                EstadoPedido.PAGO_RECHAZADO -> Color(0xFFFFDCD8)
                else -> Color(0xFFFFEDC3)
            }
        ) {
            Text(
                text = estadoActual.titulo,
                color = if (estadoActual == EstadoPedido.PAGO_RECHAZADO) Coral else Verde,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = estadoActual.descripcion,
                color = Texto,
                fontSize = 15.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    color = kit.fondo,
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "📦", fontSize = 90.sp)
        }

        TituloSeccion("Seguimiento del pedido")

        TarjetaInformacion {
            val estadosSecuencia = listOf(
                EstadoPedido.COMPROBANTE_ENVIADO to "Comprobante recibido",
                EstadoPedido.PAGO_CONFIRMADO to "Pago verificado",
                EstadoPedido.EN_PREPARACION to "En preparación en cocina",
                EstadoPedido.ENTREGADO_A_REPARTIDOR to "Entregado a repartidor 🛵",
                EstadoPedido.ENTREGADO_A_CLIENTE to "Entregado al cliente 📦"
            )

            val ordenActual = when (estadoActual) {
                EstadoPedido.PENDIENTE_PAGO -> 0
                EstadoPedido.COMPROBANTE_ENVIADO -> 0
                EstadoPedido.PAGO_CONFIRMADO -> 1
                EstadoPedido.EN_PREPARACION -> 2
                EstadoPedido.ENTREGADO_A_REPARTIDOR -> 3
                EstadoPedido.ENTREGADO_A_CLIENTE -> 4
                EstadoPedido.PAGO_RECHAZADO -> -1
            }

            estadosSecuencia.forEachIndexed { indice, (_, etiqueta) ->
                val completado = ordenActual >= indice
                val activo = ordenActual == indice

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (completado) "●" else "○",
                        color = if (activo) Coral else if (completado) Verde else Texto.copy(alpha = 0.4f),
                        fontSize = 22.sp
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = etiqueta,
                        color = if (activo || completado) Texto else Texto.copy(alpha = 0.5f),
                        fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (!pedidoActual?.comprobanteUri.isNullOrBlank()) {
            TituloSeccion("Comprobante adjuntado")
            TarjetaInformacion {
                AsyncImage(
                    model = pedidoActual.comprobanteUri,
                    contentDescription = "Comprobante de pago",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        TituloSeccion("Datos de tu pedido")

        TarjetaInformacion(fondo = kit.fondo) {
            DatosPedido(
                kit = kit,
                porciones = porciones,
                preferencias = preferencias,
                direccion = direccion,
                fecha = fecha,
                horario = horario,
                metodoPago = metodoPago
            )
        }

        BotonPrincipal(
            texto = "Cocina paso a paso",
            alPulsar = alCocinar
        )

        BotonPrincipal(
            texto = "Volver al inicio",
            alPulsar = alVolverInicio
        )
    }
}

private data class PasosCocina(
    val preparacion: List<String>,
    val coccion: List<String>,
    val montaje: List<String>
)

private fun pasosParaKit(kit: Kit): PasosCocina = when (kit.nombre) {
    "Date Night" -> PasosCocina(
        preparacion = listOf(
            "Reúne la olla, la sartén y el colador. Separa la pasta, la salsa, el queso y las especias.",
            "Llena la olla con agua suficiente para la pasta de tus porciones."
        ),
        coccion = listOf(
            "Lleva el agua a ebullición y cocina la pasta según el tiempo indicado en su empaque.",
            "Calienta la salsa a fuego bajo en la sartén, removiendo. Reserva un poco del agua de la pasta antes de escurrirla.",
            "Mezcla la pasta con la salsa. Añade un poco del agua reservada si necesitas aligerarla."
        ),
        montaje = listOf(
            "Reparte la pasta entre las porciones seleccionadas.",
            "Termina con queso y especias al gusto y sirve caliente."
        )
    )
    "Family" -> PasosCocina(
        preparacion = listOf(
            "Reúne el horno y la charola. Lava y corta los vegetales.",
            "Precalienta el horno según las indicaciones del empaque de la masa.",
            "Divide y extiende la masa para las porciones seleccionadas. Colócala en la charola."
        ),
        coccion = listOf(
            "Distribuye la salsa de tomate sobre la masa y agrega el queso y los vegetales.",
            "Hornea a la temperatura y durante el tiempo indicados en el empaque, hasta que la masa esté cocida y el queso fundido."
        ),
        montaje = listOf(
            "Retira la charola con protección para las manos y deja reposar unos minutos.",
            "Corta las pizzas y reparte entre las porciones seleccionadas."
        )
    )
    else -> PasosCocina(
        preparacion = listOf(
            "Reúne la sartén, la tabla y el cuchillo. Separa las tortillas, el relleno y la salsa.",
            "Lava y pica la cebolla y el cilantro. Organiza los ingredientes para las porciones seleccionadas."
        ),
        coccion = listOf(
            "Prepara el relleno en la sartén siguiendo las indicaciones de su empaque.",
            "Calienta las tortillas por ambos lados en una sartén y mantenlas cubiertas hasta servir."
        ),
        montaje = listOf(
            "Distribuye el relleno en las tortillas y reparte los tacos entre las porciones seleccionadas.",
            "Agrega cebolla, cilantro y salsa al gusto. Sirve al momento."
        )
    )
}

@Composable
private fun PantallaCocina(
    kit: Kit,
    porciones: Int,
    modifier: Modifier = Modifier,
    alVolver: () -> Unit
) {
    val pasos = pasosParaKit(kit)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BotonVolver(alVolver)
        TituloPantalla("Cocina paso a paso")

        TarjetaInformacion(fondo = kit.fondo) {
            Text(kit.nombre, color = Coral, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TituloSeccion(kit.platillo)
            Text("Porciones: ${textoPorciones(porciones)}", color = Verde)
            Text("Tiempo estimado: ${kit.minutos} min", color = Texto)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFFE5ECD7), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Video próximamente",
                color = Verde,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        SeccionCocina("Preparación", pasos.preparacion)
        SeccionCocina("Cocción", pasos.coccion)
        SeccionCocina("Montaje", pasos.montaje)
    }
}

@Composable
private fun SeccionCocina(titulo: String, pasos: List<String>) {
    var expandida by rememberSaveable(titulo) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (expandida) "Expandida" else "Contraída"
                }
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expandida) "Contraer $titulo" else "Expandir $titulo",
                    onClick = { expandida = !expandida }
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                titulo,
                modifier = Modifier.weight(1f),
                color = Verde,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(if (expandida) "−" else "+", color = Coral, fontSize = 24.sp)
        }

        if (expandida) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pasos.forEachIndexed { indice, paso ->
                    Text("${indice + 1}. $paso", color = Texto, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun DatosPedido(
    kit: Kit,
    porciones: Int,
    preferencias: String,
    direccion: String,
    fecha: String,
    horario: String,
    metodoPago: String
) {
    Text(
        text = "Kit: ${kit.platillo}",
        color = Texto
    )

    Text(
        text = "Porciones: ${textoPorciones(porciones)}",
        color = Texto
    )

    if (preferencias.isNotBlank()) {
        Text(
            text = "Preferencias: $preferencias",
            color = Texto
        )
    }

    Text(
        text = "Dirección: $direccion",
        color = Texto
    )

    Text(
        text = "Entrega: $fecha · $horario",
        color = Texto
    )

    Text(
        text = "Pago: $metodoPago",
        color = Texto
    )
}

@Composable
private fun TarjetaInformacion(
    fondo: Color = Color.White,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = fondo
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = contenido
        )
    }
}

@Composable
private fun ImagenComprobante(
    modelo: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = remember(modelo) {
        if (!modelo.isNullOrBlank()) {
            try {
                val cleanBase64 = if (modelo.contains("base64,")) {
                    modelo.substringAfter("base64,")
                } else {
                    modelo
                }.replace("\n", "").replace("\r", "").replace(" ", "").trim()

                val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (decodedBitmap != null) {
                    Log.d("MUNCH_FIREBASE", "Bitmap decodificado con éxito: ${decodedBitmap.width}x${decodedBitmap.height}")
                    decodedBitmap.asImageBitmap()
                } else {
                    Log.e("MUNCH_FIREBASE", "BitmapFactory.decodeByteArray devolvió NULL")
                    null
                }
            } catch (t: Throwable) {
                Log.e("MUNCH_FIREBASE", "Error al decodificar Base64: ${t.message}", t)
                null
            }
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (!modelo.isNullOrBlank() && (modelo.startsWith("http://") || modelo.startsWith("https://") || modelo.startsWith("content://"))) {
        AsyncImage(
            model = modelo,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFFE5ECD7)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 32.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (modelo.isNullOrBlank()) "Sin comprobante adjunto" else "No se pudo cargar la imagen",
                    color = Coral,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OpcionSeleccionable(
    texto: String,
    seleccionada: Boolean,
    alSeleccionar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alSeleccionar)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = seleccionada,
            onClick = alSeleccionar,
            colors = RadioButtonDefaults.colors(
                selectedColor = Coral,
                unselectedColor = Verde
            )
        )

        Text(
            text = texto,
            color = Texto,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
private fun BotonVolver(
    alVolver: () -> Unit
) {
    TextButton(onClick = alVolver) {
        Text(
            text = "← Volver",
            color = Verde,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun TituloPantalla(
    texto: String
) {
    Text(
        text = texto,
        color = Texto,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TituloSeccion(
    texto: String
) {
    Text(
        text = texto,
        color = Texto,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun BotonPrincipal(
    texto: String,
    alPulsar: () -> Unit,
    habilitado: Boolean = true
) {
    Button(
        onClick = alPulsar,
        enabled = habilitado,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Coral,
            contentColor = Color.White
        )
    ) {
        Text(
            text = texto,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MunchPreview() {
    MunchTheme {
        MunchApp()
    }
}

package com.example.munch.admin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.munch.data.EstadoPedido
import com.example.munch.data.Pedido
import com.example.munch.data.RepositorioPedidos
import com.google.firebase.FirebaseApp

private val Crema = Color(0xFFFFF8ED)
private val Coral = Color(0xFFD95740)
private val Verde = Color(0xFF486039)
private val Texto = Color(0xFF30271F)

class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()

        setContent {
            AdminMunchTheme {
                AdminMunchApp()
            }
        }
    }
}

@Composable
fun AdminMunchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Coral,
            secondary = Verde,
            background = Crema,
            surface = Crema
        ),
        content = content
    )
}

@Composable
fun AdminMunchApp() {
    val contexto = LocalContext.current

    val lanzadorPermisoNotificacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        RepositorioPedidos.inicializar(contexto)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lanzadorPermisoNotificacion.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val listaPedidos by RepositorioPedidos.pedidos.collectAsState()
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtroSeleccionado by rememberSaveable { mutableIntStateOf(0) } // 0: Todos, 1: Pendientes, 2: En cocina, 3: Reparto/Entregado
    var comprobanteAmpliado by remember { mutableStateOf<Pair<String, Bitmap?>?>(null) }

    val pedidosFiltrados = remember(listaPedidos, filtroSeleccionado, busqueda) {
        val porFiltro = when (filtroSeleccionado) {
            1 -> listaPedidos.filter { it.estado == EstadoPedido.COMPROBANTE_ENVIADO || it.estado == EstadoPedido.PENDIENTE_PAGO }
            2 -> listaPedidos.filter { it.estado == EstadoPedido.PAGO_CONFIRMADO || it.estado == EstadoPedido.EN_PREPARACION }
            3 -> listaPedidos.filter { it.estado == EstadoPedido.ENTREGADO_A_REPARTIDOR || it.estado == EstadoPedido.ENTREGADO_A_CLIENTE }
            else -> listaPedidos
        }

        if (busqueda.isBlank()) {
            porFiltro
        } else {
            porFiltro.filter {
                it.id.contains(busqueda, ignoreCase = true) ||
                        it.kitNombre.contains(busqueda, ignoreCase = true) ||
                        it.platillo.contains(busqueda, ignoreCase = true) ||
                        it.direccion.contains(busqueda, ignoreCase = true) ||
                        it.preferencias.contains(busqueda, ignoreCase = true)
            }
        }
    }

    Scaffold(containerColor = Crema) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Munch Admin",
                        color = Coral,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "Gestión integral de pedidos y entregas",
                        color = Verde,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Resumen de Métricas
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val total = listaPedidos.size
                val pendientes = listaPedidos.count { it.estado == EstadoPedido.COMPROBANTE_ENVIADO || it.estado == EstadoPedido.PENDIENTE_PAGO }
                val cocina = listaPedidos.count { it.estado == EstadoPedido.PAGO_CONFIRMADO || it.estado == EstadoPedido.EN_PREPARACION }
                val repartidor = listaPedidos.count { it.estado == EstadoPedido.ENTREGADO_A_REPARTIDOR || it.estado == EstadoPedido.ENTREGADO_A_CLIENTE }

                item { TarjetaMetrica("Total", "$total", Color(0xFFFBE1D5)) }
                item { TarjetaMetrica("Pendientes", "$pendientes", Color(0xFFFFEDC3)) }
                item { TarjetaMetrica("En Cocina", "$cocina", Color(0xFFE5ECD7)) }
                item { TarjetaMetrica("En Reparto", "$repartidor", Color(0xFFE2E8F0)) }
            }

            // Buscador
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por ID, kit, cliente o dirección...", fontSize = 14.sp) },
                leadingIcon = { Text("🔍", fontSize = 16.sp) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                )
            )

            // Selector de filtro con desplazamiento horizontal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Todos 📦", "Pendientes ⏳", "En cocina 👨‍🍳", "En reparto 🛵").forEachIndexed { indice, etiqueta ->
                    FilterChip(
                        selected = filtroSeleccionado == indice,
                        onClick = { filtroSeleccionado = indice },
                        label = { Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Coral,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Texto
                        )
                    )
                }
            }

            if (pedidosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (busqueda.isNotBlank()) "No se encontraron pedidos con '$busqueda'." else "No hay pedidos en esta categoría.",
                        color = Verde,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(pedidosFiltrados, key = { it.id }) { pedido ->
                        TarjetaPedidoAdmin(
                            pedido = pedido,
                            alAprobar = {
                                RepositorioPedidos.actualizarEstado(pedido.id, EstadoPedido.PAGO_CONFIRMADO)
                            },
                            alRechazar = {
                                RepositorioPedidos.actualizarEstado(pedido.id, EstadoPedido.PAGO_RECHAZADO)
                            },
                            alPreparar = {
                                RepositorioPedidos.actualizarEstado(pedido.id, EstadoPedido.EN_PREPARACION)
                            },
                            alEntregarRepartidor = {
                                RepositorioPedidos.actualizarEstado(pedido.id, EstadoPedido.ENTREGADO_A_REPARTIDOR)
                            },
                            alEntregarCliente = {
                                RepositorioPedidos.actualizarEstado(pedido.id, EstadoPedido.ENTREGADO_A_CLIENTE)
                            },
                            alAmpliarComprobante = { uri, bitmap ->
                                comprobanteAmpliado = uri to bitmap
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal para ver el comprobante a tamaño completo
    comprobanteAmpliado?.let { (uriStr, bitmapCard) ->
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        AlertDialog(
            onDismissRequest = { comprobanteAmpliado = null },
            containerColor = Crema,
            title = {
                Text(
                    text = "Comprobante de transferencia",
                    color = Coral,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val bitmapFinal = remember(uriStr, bitmapCard) {
                    bitmapCard ?: try {
                        val clean = uriStr.substringAfter("base64,").replace("\n", "").replace("\r", "").replace(" ", "").trim()
                        if (clean.isNotEmpty()) {
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF8ED))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmapFinal != null) {
                        Image(
                            bitmap = bitmapFinal.asImageBitmap(),
                            contentDescription = "Comprobante ampliado",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit
                        )
                    } else if (uriStr.isNotBlank() && (uriStr.startsWith("http://") || uriStr.startsWith("https://") || uriStr.startsWith("content://"))) {
                        AsyncImage(
                            model = uriStr,
                            contentDescription = "Comprobante ampliado",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                ),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "Error: comprobanteUri llega como: ${uriStr.take(30)}",
                            color = Coral,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { comprobanteAmpliado = null }) {
                    Text("Cerrar", color = Verde)
                }
            }
        )
    }
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    valor: String,
    fondo: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        modifier = modifier.width(115.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                color = Texto,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = titulo,
                color = Texto.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TarjetaPedidoAdmin(
    pedido: Pedido,
    alAprobar: () -> Unit,
    alRechazar: () -> Unit,
    alPreparar: () -> Unit,
    alEntregarRepartidor: () -> Unit,
    alEntregarCliente: () -> Unit,
    alAmpliarComprobante: (String, Bitmap?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 ID: ${pedido.id}",
                    color = Coral,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                // Badge de estado
                Surface(
                    color = when (pedido.estado) {
                        EstadoPedido.PAGO_CONFIRMADO, EstadoPedido.EN_PREPARACION -> Color(0xFFE5ECD7)
                        EstadoPedido.ENTREGADO_A_REPARTIDOR, EstadoPedido.ENTREGADO_A_CLIENTE -> Color(0xFFE2E8F0)
                        EstadoPedido.PAGO_RECHAZADO -> Color(0xFFFFDCD8)
                        else -> Color(0xFFFFEDC3)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = pedido.estado.titulo,
                        color = if (pedido.estado == EstadoPedido.PAGO_RECHAZADO) Coral else Verde,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            val emojiPlatillo = when {
                pedido.platillo.contains("Pizza", ignoreCase = true) -> "🍕"
                pedido.platillo.contains("Pasta", ignoreCase = true) -> "🍝"
                pedido.platillo.contains("Tacos", ignoreCase = true) -> "🌮"
                else -> "🍱"
            }

            Text(
                text = "$emojiPlatillo ${pedido.kitNombre} · ${pedido.platillo}",
                color = Texto,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "porciones: ${pedido.porciones}  |  Fecha: ${pedido.fecha} (${pedido.horario})",
                color = Texto.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Text(
                text = "📍 Dirección: ${pedido.direccion}",
                color = Texto.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            if (pedido.preferencias.isNotBlank()) {
                Text(
                    text = "📝 Notas: ${pedido.preferencias}",
                    color = Verde,
                    fontSize = 13.sp
                )
            }

            // Sección de comprobante
            if (!pedido.comprobanteUri.isNullOrBlank()) {
                Text(
                    text = "Captura de transferencia:",
                    color = Texto,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                val bitmapCard = remember(pedido.comprobanteUri) {
                    try {
                        val clean = pedido.comprobanteUri.substringAfter("base64,").replace("\n", "").replace("\r", "").replace(" ", "").trim()
                        if (clean.isNotEmpty()) {
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable { alAmpliarComprobante(pedido.comprobanteUri, bitmapCard) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8ED))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (bitmapCard != null) {
                            Image(
                                bitmap = bitmapCard.asImageBitmap(),
                                contentDescription = "Comprobante",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (pedido.comprobanteUri.startsWith("http://") || pedido.comprobanteUri.startsWith("https://")) {
                            AsyncImage(
                                model = pedido.comprobanteUri,
                                contentDescription = "Comprobante",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = "Sin comprobante válido",
                                color = Coral,
                                fontSize = 12.sp
                            )
                        }

                        // Overlay Toca para ampliar
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "🔍 Toca para ampliar",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "⚠️ No se adjuntó imagen de comprobante",
                    color = Coral,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            // Botones de acción del Administrador
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (pedido.estado) {
                    EstadoPedido.COMPROBANTE_ENVIADO, EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGO_RECHAZADO -> {
                        Button(
                            onClick = alAprobar,
                            colors = ButtonDefaults.buttonColors(containerColor = Verde),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Aprobar Pago", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = alRechazar,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Coral),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Rechazar", fontSize = 13.sp)
                        }
                    }
                    EstadoPedido.PAGO_CONFIRMADO -> {
                        Button(
                            onClick = alPreparar,
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Marcar En Preparación 👨‍🍳", fontSize = 14.sp)
                        }
                    }
                    EstadoPedido.EN_PREPARACION -> {
                        Button(
                            onClick = alEntregarRepartidor,
                            colors = ButtonDefaults.buttonColors(containerColor = Verde),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Entregar a Repartidor 🛵", fontSize = 14.sp)
                        }
                    }
                    EstadoPedido.ENTREGADO_A_REPARTIDOR -> {
                        Button(
                            onClick = alEntregarCliente,
                            colors = ButtonDefaults.buttonColors(containerColor = Coral),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Marcar Entregado al Cliente 📦", fontSize = 14.sp)
                        }
                    }
                    EstadoPedido.ENTREGADO_A_CLIENTE -> {
                        Surface(
                            color = Color(0xFFE5ECD7),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Pedido entregado al cliente con éxito",
                                color = Verde,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
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

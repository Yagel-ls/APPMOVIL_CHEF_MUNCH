package com.example.munch.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.munch.data.Pedido

object GestorNotificacionesAdmin {
    private const val CANAL_ID = "canal_pedidos_munch"
    private const val CANAL_NOMBRE = "Notificaciones de Pedidos Munch"

    fun crearCanalNotificacion(contexto: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOMBRE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando un cliente realiza un nuevo pedido"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }

            val manager = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(canal)
        }
    }

    fun notificarNuevoPedido(contexto: Context, pedido: Pedido) {
        crearCanalNotificacion(contexto)

        val intent = Intent(contexto, AdminMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            contexto,
            pedido.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emojiPlatillo = when {
            pedido.platillo.contains("Pizza", ignoreCase = true) -> "🍕"
            pedido.platillo.contains("Pasta", ignoreCase = true) -> "🍝"
            pedido.platillo.contains("Tacos", ignoreCase = true) -> "🌮"
            else -> "🍱"
        }

        val notificacion = NotificationCompat.Builder(contexto, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("🔔 ¡Nuevo Pedido! ${pedido.id}")
            .setContentText("$emojiPlatillo ${pedido.kitNombre} · ${pedido.platillo} (${pedido.porciones} porciones)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$emojiPlatillo ${pedido.kitNombre} · ${pedido.platillo}\n" +
                            "📍 Dirección: ${pedido.direccion}\n" +
                            "🕒 Entrega: ${pedido.fecha} (${pedido.horario})"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(pedido.id.hashCode(), notificacion)
    }
}

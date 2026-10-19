### Resumen de cambios
Se implementó el flujo completo para pagos mediante transferencia bancaria y su validación en tiempo real a través de un nuevo módulo de administración conectado a Firebase.

### Principales características agregadas:
* **App Cliente (`:app`):**
  * Pantalla de datos bancarios para pago por transferencia[cite: 4].
  * Selección y adjunto de comprobante de pago desde la galería del dispositivo[cite: 2, 4].
  * Compresión y codificación de imágenes para envío optimizado a la nube[cite: 6, 8].
* **Backend (Firebase Firestore):**
  * Almacenamiento y sincronización en tiempo real de la colección `pedidos` con estatus de pago, detalles de entrega y comprobante[cite: 2, 10].
* **App Administrador (`:admin` - Munch Admin):**
  * Panel de control con filtros por estado (*Pendientes*, *Confirmados*, *Todos*)[cite: 10].
  * Visualización de miniatura del comprobante de transferencia en cada pedido y modal emergente para inspección detallada[cite: 10, 11].
  * Botones de acción directa para **Aprobar Pago** o **Rechazar** pedidos sincronizados con Firestore[cite: 7, 10, 11].
  * Notificaciones locales al recibir un nuevo pedido[cite: 13].

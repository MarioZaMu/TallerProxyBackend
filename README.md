# Backend: Monitoreo de microservicios con Proxy de logging

API Spring Boot (Java 17, Maven) del taller de **Patrones de Software** sobre el patrón **Proxy**.
Inventario, Pedidos y Pagos son servicios reales en memoria; un `LoggingProxy<T>` genérico los envuelve, mide cada llamada y registra éxitos y errores (con stack trace resumido).

El contrato detallado de cada operación (cuerpos y respuestas) está en la sección de endpoints de más abajo.
2. **Settings -> Networking -> Generate Domain** y copia la URL.
3. En **Variables** agrega `ALLOWED_ORIGINS` con la URL del frontend en Vercel.
4. Verifica `https://<tu-dominio>/actuator/health`.

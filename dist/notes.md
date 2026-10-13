## Beta 5.0.1 · Audio y apariencia

- Normalización adaptativa RMS con nivel editable y limitador de picos.
- Mezcla automática real de 0 a 12 s: superpone canciones y conserva la posición al continuar, también con pantalla apagada.
- DSP PCM flotante y salida compatible de 16 bits; amplitud estéreo ajustable y espacialización de Android cuando el formato y la salida la admiten.
- Frecuencia de actualización según los modos compatibles de la pantalla. Modo de rendimiento y reducción de desenfoque.
- Portada animada: respirar, deslizar o girar; importar GIF/WebP locales desde Opciones de portada. Se detiene al pausar o salir de la app.
- Portada completa o centrada, gradiente de malla, sincronización de letras, difuminado de versos inactivos y volumen visible u oculto.
- Opción para detener al quitar Prisma de recientes; reproducción en segundo plano conservada por defecto.

### Uso
Audio: Ajustes → Audio avanzado. Apariencia: Ajustes → Personalización → Apariencia avanzada. La mezcla está desactivada inicialmente (0 s); la portada animada se activa a elección.

### Calidad e instalación
FLAC estéreo no es Dolby Atmos. Los efectos modifican la señal y no recodifican los archivos. Android, el DAC y Bluetooth determinan la salida final; no se promete bit-perfect. Omitir silencios se usa con salida compatible de 16 bits.

Instala **Prisma-Music-5.0.1-beta.apk** encima de **4.0.1** o **3.0.4** para conservar listas y ajustes; la firma coincide. Versiones 3.0.3 o anteriores usan otra firma y requieren desinstalación previa, que borra ajustes locales, no la música.

Validación: compilación debug/release, pruebas unitarias, lint y 13 pruebas en Android 15, incluyendo mezcla real, pausa, transferencia de posición, cambio de salida, muestras PCM de 24 bits y GIF en reproducción/pausa. La publicación verifica firma, checksum e instalación sobre 4.0.1 antes de crear esta Release.

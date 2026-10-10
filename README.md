# Prisma Music

Reproductor de música local para Android, gratuito y de código abierto. Primera beta: **0.1.0**. Android 8.0 o posterior.

## Incluido

- Reproducción local con Media3, servicio en segundo plano, sesión multimedia, controles de notificación y Bluetooth.
- Biblioteca de canciones, artistas, álbumes, géneros y carpetas; búsqueda, cola, repetición y aleatorio.
- Listas locales: crear, añadir, quitar canciones y eliminar listas sin borrar archivos.
- Exclusión predeterminada de audios/notas de voz de WhatsApp y WhatsApp Business. Carpetas registradas por MediaStore seleccionables.
- Chromatic Flow (acento de la carátula), Liquid Glass (transparencias/degradados), Pure Minimal y Black Night.
- Modo **claro, oscuro y según el sistema**, independiente del tema. Preferencias persistentes.
- Ecualizador DSP original de diez bandas: 31, 63, 125, 250, 500 Hz; 1, 2, 4, 8, 16 kHz. Q global ajustable, preamplificador, graves, medios, agudos, presencia de voces y limitador de picos enlazado entre canales.
- Importación manual de letras TXT/LRC por canción; tiempos múltiples, offset, seguimiento y pulsación para saltar a una línea.
- Metadatos de formato, códec, bitrate, frecuencia de muestreo, canales y profundidad de bits cuando el extractor los informa. Estimaciones de bitrate etiquetadas.

## Instalar

**[⬇ Descargar Prisma Music para Android (APK)](https://github.com/Pablormlopez/prisma-music/releases/download/v0.1.1-beta/Prisma-Music-0.1.1-beta.apk)**

Android 8.0 o posterior. Abre el enlace desde el teléfono, espera a que termine la descarga y abre el archivo `.apk`. Android puede pedir permitir instalaciones desde el navegador o gestor de archivos usado. No necesitas descargar el código ni entrar en Actions.

[Ver versiones y notas de publicación](https://github.com/Pablormlopez/prisma-music/releases).

La versión 0.1.1 es una compilación release sin depuración, firmada con una clave temporal de beta. Todavía no se garantiza actualizar encima de otra beta con una firma diferente. Si ya instalaste una beta anterior y la actualización falla por firma, conserva tus archivos de música y ten presente que desinstalar elimina listas y preferencias locales. Antes de la versión estable se configurará una clave privada duradera.

## Compilar y verificar

JDK 17, SDK Android 35, Gradle 8.11.1. El proyecto incluye Gradle Wrapper. Configura el SDK mediante `ANDROID_HOME` o `local.properties`.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Salida: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions ejecuta pruebas, compilación y lint automáticamente.

## Límites de esta beta

- La biblioteca depende de MediaStore: archivos ocultos con `.nomedia`, carpetas privadas de apps o archivos aún no indexados pueden no aparecer. No hay exploración SAF recursiva en esta versión.
- El procesamiento usa PCM de 16 bits. Reproducir FLAC no implica salida bit-perfect o sin pérdida por Bluetooth. No se promete salida de alta resolución.
- La presencia de voces realza una zona de frecuencias; no separa voces e instrumentos. Q es común a las diez bandas en esta versión. A frecuencias de muestreo bajas se limita la frecuencia de los filtros a menos de Nyquist.
- Liquid Glass es una interpretación visual con transparencias, sin desenfoque real todavía. Las transiciones iniciales son discretas.
- Las letras se importan manualmente y se guardan solo en el teléfono; no hay búsqueda en internet ni extracción automática de letras incrustadas todavía.
- El bitrate estimado usa tamaño/duración y puede incluir metadatos. Profundidad de bits no disponible se omite.
- Géneros de MediaStore requieren Android 11 o posterior; en versiones anteriores se muestra “Sin género”.
- No se restaura la cola tras reiniciar el servicio. Listas y preferencias sí persisten. La app no borra archivos de audio.

## Próximas versiones

Pruebas en teléfonos reales y Bluetooth; importación/exportación de listas; acceso a carpetas mediante SAF; filtros individuales por banda; proveedor opcional de letras con licencias compatibles; desenfoque y animaciones más refinadas; firma de lanzamiento y APKs de actualización.

## Privacidad y licencia

Sin anuncios, analítica, cuentas ni permiso de internet. Solo lee audio con autorización y archivos de letras elegidos por el usuario. Licencia MIT. El diseño y DSP son originales, inspirados en funciones habituales de reproductores; no incluyen código ni recursos de Poweramp, Spotify o Apple.

# Prisma Music

## Instalar

**[⬇ Descargar Prisma Music para Android (APK)](https://github.com/nexlibre/prisma-music/releases/download/v6.0.1-beta/Prisma-Music-6.0.1-beta.apk)**

Android 8.0 o posterior. Abre el enlace desde el teléfono, espera a que termine la descarga y abre el archivo `.apk`. Android puede pedir permitir instalaciones desde el navegador o gestor de archivos usado. No necesitas descargar el código ni entrar en Actions.

[Ver versiones y notas de publicación](https://github.com/nexlibre/prisma-music/releases).

**Actualización 6.0.1:** si ya tienes 5.0.2, 5.0.1, 4.0.1 o 3.0.4, instala encima para conservar listas y ajustes; se mantiene la misma firma.

**Cambio de firma en 3.0.4:** esta beta inicia una nueva identidad de firma. Si tienes 3.0.3 o una versión anterior, debes desinstalarla antes de instalar 3.0.4; esto elimina las listas y ajustes locales, pero conserva los archivos de música. La clave de firma de 3.0.4 cuenta con un respaldo privado para las futuras actualizaciones. Nunca publiques ni compartas la clave privada.


## Beta 6.0.1 · Interrupciones y estabilidad

Interrupciones de audio y pausa al grabar son la prioridad de esta etapa. Nuevas preferencias para pausar o atenuar con notificaciones y reanudar tras interrupciones; la reanudación automática está desactivada inicialmente. La detección de grabación depende de los eventos que Android permita observar, sin acceder al micrófono ni al contenido de mensajes.

APK 6.0.1 firmado y publicado con la misma identidad desde 3.0.4. Compilación debug y release, pruebas unitarias, lint y 14 de 14 pruebas Android 15 satisfactorias. Firma, versión y actualización desde 5.0.2 verificadas en Android 13 por el proceso de publicación.

Esta es la primera etapa del plan. WhatsApp y otras aplicaciones externas en teléfonos físicos siguen pendientes de validación. Recuperación persistente de cola, migración de listas, SAF, ReplayGain y demás mejoras continúan en las siguientes etapas.

[Alcance, validación y tareas pendientes de 6.0.1](docs/BETA-6.0.1.md). [Historial de versiones](CHANGELOG.md).

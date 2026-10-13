# Prisma Music Beta 6.0.1

Primera etapa del plan de consolidación: interrupciones y estabilidad. Android 8 o posterior, versionCode 20. No incorpora todavía todas las etapas del documento original.

## Cambios

Un solo coordinador de foco en el servicio controla los dos decodificadores. Media3 deja de gestionar foco internamente para evitar solicitudes duplicadas. Pérdida temporal pausa ambas pistas; pérdida permanente impide reanudación. La notificación y Bluetooth siguen usando la misma MediaSession.

Pausar al grabar utiliza AudioManager.AudioRecordingCallback y consulta las configuraciones activas; no captura audio, no pide permiso de micrófono ni identifica aplicaciones. La consulta inicial también bloquea reproducción cuando una grabación ya está activa. Android puede limitar los eventos disponibles según dispositivo. No se garantiza detectar absolutamente todas las grabaciones.

Pausar con notificaciones viene activado y se aplica a pérdidas de foco que permiten atenuación. Desactivarlo atenúa ambas pistas al 20 %. No detecta mensajes silenciosos ni lee contenido. No se implementa acceso especial a notificaciones.

Reanudar tras interrupciones viene desactivado. Activado exige reproducción previa, final de grabación y recuperación del foco. Pausa manual, temporizador, retirada de auriculares y pérdida permanente cancelan la intención. La posición y la cola no se borran al pausar; la superposición se cancela.

La duración del crossfade se ajusta a la velocidad de reproducción. Modo de alto rendimiento pasa a llamarse Modo ligero.

## Matriz de validación

| Caso | Android y salida | Estado |
| --- | --- | --- |
| Política de interrupciones simultáneas y cancelación manual | JVM, independiente de salida | Pruebas unitarias añadidas; consultar CI |
| Mezcla real, pausa y cambio de salida PCM | Emulador Android 15 | Prueba existente, pendiente nueva ejecución |
| Foco temporal, permanente y atenuación durante mezcla | Emulador Android 15 | Pruebas de integración por incorporar |
| WhatsApp al grabar y escuchar notas | Infinix Android 13, altavoz y Bluetooth | No verificado, no hay dispositivo disponible |
| Grabadora y cámara externas | Android 8–15, altavoz y auriculares | No verificado en aplicaciones externas |
| Llamada, asistente, notificación sonora y silenciosa | Dispositivo físico | Pendiente |
| Firma e instalación encima de 5.0.2 | Android 13 | Obligatorio antes de publicar APK |

## Investigación del fallo anterior

La ejecución 38478771970 terminó con 13 de 14 pruebas satisfactorias. El informe detallado sitúa AssertionError en AdvancedPlaybackTest.kt:81, comparación de posición tras cambiar de salida. El resultado del comando llega antes que la actualización asíncrona del controlador por Session.setPlayer; la comprobación pasa a esperar la posición requerida manteniendo el umbral original. Esta condición de carrera es una hipótesis que debe validarse con la nueva ejecución, no una regresión demostrada. También se añade pérdida real de foco temporal mientras las dos pistas se superponen.

## Etapas pendientes

1. Recuperación persistente de cola y aleatorio, base de datos con migración sin pérdidas, respaldo e intercambio M3U/M3U8.
2. SAF recursivo, deduplicación, metadatos de disco/pista/artista del álbum y reescaneo seguro.
3. ReplayGain, gapless, ecualizador avanzado y mediciones de DSP.
4. Accesibilidad, gestos, rendimiento y separación gradual de responsabilidades.
5. Historial local y listas inteligentes, widget, desfase LRC, temporizador al terminar canción y balance.

Una compilación no demuestra compatibilidad con WhatsApp ni ausencia de problemas en dispositivos físicos. No publicar un APK con firma distinta ni declarar concluidas estas etapas pendientes.

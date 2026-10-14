# Historial de Prisma Music

## Beta 6.0.1 · Interrupciones y estabilidad

- Pausa ambas pistas al perder foco y ante grabaciones observables por Android.
- Ajustes para pausar o atenuar con notificaciones y reanudación optativa.
- Cancelación de reanudación con pausa manual, temporizador, pérdida permanente o desconexión de auriculares incluso durante una pausa.
- Mezcla ajustada a velocidad, Modo ligero e historial separado del README.
- Pruebas unitarias, lint y 14 pruebas Android 15 satisfactorias. Firma y actualización desde 5.0.2 verificadas en Android 13.
- Primera etapa del plan: la matriz de casos físicos y las demás mejoras pendientes se detallan en docs/BETA-6.0.1.md.

## Beta 5.0.2 · Animaciones en todos los temas

- Ambiente del álbum, portada inmersiva y transiciones compartidas entre los siete temas, con opción para recuperar las composiciones originales.
- Respuesta táctil con muelle en botones y selectores de todos los temas; Liquid Glass conserva su acabado.
- APK 5.0.2 firmado y publicado, con descarga al principio del README. Firma y actualización desde 5.0.1 verificadas antes de publicar.

## Beta 5.0.1 · Audio y apariencia

- Salida PCM flotante: el DSP opera sobre muestras float y conserva muestras PCM de 24 bits antes de la salida. Sigue disponible el modo compatible de 16 bits. Android, el DAC y Bluetooth determinan la salida final; los efectos modifican la señal y no se promete bit-perfect.
- Normalización adaptativa RMS, con ganancia acotada y limitador de picos. No analiza LUFS de la canción completa ni usa un proveedor de ReplayGain.
- Mezcla automática real de 0 a 12 s: dos decodificadores superponen final e inicio y transfieren la posición de la canción entrante a la sesión principal. Conserva cola, controles y notificación; pausa, búsqueda manual y cambios de cola cancelan la mezcla. Repetición de una pista y duración desconocida no se mezclan.
- Audio espacial: permite la espacialización de Android cuando el formato y la salida lo admiten; muestra su disponibilidad real. FLAC estéreo no equivale a Dolby Atmos. Amplitud estéreo ajustable por separado.
- Apariencia: frecuencias de refresco compatibles, modo de rendimiento, menos desenfoque, portada completa o centrada, gradiente de malla, letras sincronizadas y difuminado de versos inactivos.
- Portada animada: respirar, desplazar o girar la carátula. También admite importar GIF/WebP locales desde Opciones de portada; no requiere Spotify Canvas. Se pausa al detener la música, dejar la app o reducir animaciones. Se conserva una imagen estática del álbum para el ambiente.
- Volumen visible u oculto y opción para detener al quitar Prisma de recientes. La reproducción en segundo plano permanece activa por defecto.
- Omitir silencios está disponible con salida compatible de 16 bits; velocidad usa el control nativo de AudioTrack cuando la salida es flotante.

## Beta 4.0.1 · Liquid Glass y portada inmersiva

- Liquid Glass y Chromatic Flow: portada a todo el ancho, fundida en un fondo difuminado de la propia carátula; transición al cambiar de canción y controles blancos con contraste estable.
- Letras sobre el ambiente del álbum, progreso fino, controles de reproducción sin panel grande y accesos a letra, importación y cola.
- Volumen multimedia del teléfono, independiente de los desvanecidos. Los botones físicos se reflejan en el control.
- El contenido se puede desplazar en pantallas cortas o con texto grande. Se conservan gestos de canción, cierre, artista, álbum, letras locales y listas ordenables.
- Sin animaciones también desactiva la transición del fondo. Los otros cinco temas siguen disponibles.

## Beta 3.0.4 · Orden manual de listas

- Cada canción de una lista tiene un tirador ≡: mantenlo pulsado y arrastra hacia arriba o abajo. Los bordes desplazan la lista automáticamente y la posición de destino se resalta.
- El orden se guarda al soltar y se conserva al reabrir la app. La reproducción iniciada desde la lista sigue ese orden; la cola que ya está sonando no cambia.
- Cancelar el gesto conserva el orden anterior. Las canciones ocultas por filtros se mantienen en la lista. TalkBack ofrece acciones para mover hacia arriba o abajo desde el tirador.

## Beta 3.0.3 · Biblioteca y reproductor pulidos

- Selección y total de canciones ocultos en la vista habitual. Mantén presionada una canción o toca sus tres puntos para seleccionar una o varias.
- Ordenar se integra como icono discreto en el buscador y conserva el estilo del tema.
- Liquid Glass y Monocromo: carátula centrada, sombra tenue y degradado suave con los colores del álbum.
- Letras con fondo limpio, contraste mejorado y resaltado estable. Se conservan importación, sincronización y búsqueda por tiempo.

## Beta 3.0.2 · Monocromo y degradado

- Nuevo tema **Monocromo**: blanco puro y tipografía negra en modo claro; negro AMOLED y tipografía blanca en modo oscuro.
- El reproductor de Liquid Glass y Monocromo amplía la carátula y funde sus colores con un degradado hasta la parte inferior.
- La estructura y organización de la biblioteca se mantienen.

## Beta 3.0.1 · Liquid Glass

- Liquid Glass renovado: superficies translúcidas, reflejos finos y fondo visible a través de la navegación y el minirreproductor. Android 12 o posterior añade desenfoque del contenido; Android 8–11 conserva cristal translúcido y fondo de carátula difuminado.
- La cápsula de navegación sigue el dedo y se estira al cambiar de sección; también responde al tocar los iconos. Los botones y el control de posición reaccionan suavemente a la pulsación y al arrastre.
- Se respeta la opción **Sin animaciones**. La organización, las categorías y las vistas de la biblioteca se mantienen, igual que el orden de canciones y los gestos de la beta anterior.
- Actívalo en Ajustes → Personalización → Temas → **Liquid Glass**.

## Beta 2.1.2 · Orden y navegación

- Biblioteca → **Ordenar canciones**: A a Z, Z a A, más nuevas primero y más antiguas primero. El orden se guarda y se aplica a búsquedas, grupos y reproducción desde la biblioteca.
- **Recién añadidas** permite alternar entre nuevas y antiguas; por defecto muestra las nuevas. Usa la fecha de incorporación al teléfono.
- Desliza horizontalmente el contenido para pasar entre Inicio, Biblioteca, Listas, Sonido y Ajustes. Los iconos siguen disponibles. Los gestos cortos y verticales no cambian de menú; los controles horizontales conservan sus gestos.
- Cola simplificada: solo el tirador ≡ para mantener pulsado y arrastrar canciones. Se retiran los botones de subir, bajar y quitar de la cola.

## Beta 2.1.1 · Tu colección, conectada

- Toca el artista del reproductor para ver sus canciones y álbumes locales sin cambiar la reproducción. Los álbumes abren su lista de canciones, con reproducción y añadir a playlist.
- Ajustes → Reproductor permite mostrar u ocultar el nombre del álbum. Cuando está visible, también abre sus canciones.
- Inicio → **Recién añadidas** sustituye «Canciones de tu biblioteca»: usa la fecha de incorporación de MediaStore, con las más nuevas primero. **Ver todas** abre la categoría correspondiente en Biblioteca. El historial de escucha permanece separado.
- **Modo de color** está en Ajustes → Personalización, junto a temas, colores y animaciones.
- Biblioteca → **Seleccionar varias** muestra casillas y un botón Añadir para incorporar canciones, álbumes o artistas en grupo.
- Cada playlist, incluso vacía, tiene **Añadir canciones**: buscar, marcar varias, seleccionar visibles, cancelar o guardar. Las canciones ya incluidas no se repiten y las excluidas de Biblioteca respetan tus filtros.

## Beta 2.1.0 · Sonido y movimiento personales

- Sonido → **Guardar preset** y **Mis presets**: guarda, carga, reemplaza y elimina ajustes propios. Se conservan las diez bandas, Q, preamplificador, graves, medios, agudos, voces y estado del ecualizador. Los cuatro presets incluidos siguen disponibles.
- Ajustes → **Desvanecidos**: entrada y salida independientes de 0 a 12 s, en pasos de 0,5 s. 0 desactiva cada efecto. El servicio aplica el volumen según la posición de la pista, también con pantalla apagada. Pistas cortas ajustan proporcionalmente ambos tiempos; duración desconocida aplica solo entrada. No es crossfade: no superpone pistas. Un salto manual inicia la entrada de la siguiente sin retrasarlo para una salida.
- Ajustes → **Personalización** reúne **Temas**, colores y **Animaciones**. Fundido suave, Deslizamiento, Zoom delicado o Sin animaciones; duración de 250 a 900 ms y vista previa.
- Pantallas, portadas y títulos comparten la transición elegida. La carátula acompaña el dedo al deslizar y vuelve suavemente cuando el gesto es corto. Preferencias anteriores, listas y colores se conservan.

## Beta 2.0.2 · Colección y cola a tu medida

- Cola con tirador ≡: mantener pulsado y arrastrar hacia arriba o abajo, con desplazamiento al acercarse a los bordes, tarjeta elevada y destino resaltado. Se conservan los botones accesibles. Un cambio externo en la cola cancela la aplicación del movimiento.
- Biblioteca → Seleccionar: marcar varias canciones, álbumes o artistas; seleccionar todos los resultados visibles y añadirlos juntos a una playlist. Las canciones repetidas no se duplican en la lista. La selección se conserva al buscar o cambiar de categoría y Atrás la cancela.
- Acceso directo para añadir un álbum, artista o grupo completo a una playlist; barra de selección redondeada y cola con superficies destacadas, inspiradas en las referencias visuales.

## Beta 2.0.1 · Una nueva forma de escuchar

- Inicio independiente con carátulas protagonistas, canciones y álbumes de la colección local e historial reciente.
- Navegación flotante de cinco accesos y reproductor completo con más espacio para la portada.
- Ajustes → **Temas**: Chromatic Flow, Liquid Glass, Pure Minimal y Black Night renovados; nuevos **Studio** y **Sunset**. Modos claro, oscuro y sistema.
- Liquid Glass se inspira en superficies claras, cápsulas flotantes y bordes de cristal; el fondo difuminado se calcula una vez por carátula y funciona desde Android 8.
- Se conservan la personalización manual, cola editable, gestos, letras, metadatos y ecualizador.
- Historial y preferencias se guardan en el teléfono. No requiere cuentas ni servicios de música en línea.

## Beta 1.0.4 · Cola de reproducción

- Botón de cola en el reproductor: muestra la canción actual y cuál sigue, con el orden real incluso en aleatorio.
- Mueve entradas hacia arriba o abajo, o quítalas solo de la cola. Las canciones repetidas se editan individualmente. Mover conserva la posición y estado de reproducción; en aleatorio mantiene la secuencia visible y aplica tu cambio como orden manual.
- La cola se actualiza al cambiar de canción o editarla. Se indica cuando está activada la repetición de una canción.
- Se retira la etiqueta de instrucciones de gestos; deslizar para cambiar canciones o cerrar sigue disponible.

## Beta 1.0.3 · Personalización y acciones de canciones

- Ajustes → Personalización: mini temas de color, código hexadecimal manual, lista/cuadrícula/lista compacta y subtítulo de artista, álbum o archivo. Las elecciones se guardan y se combinan con los cuatro temas. El color elegido se ajusta para mantener contraste.
- Desliza la carátula hacia abajo para volver a la biblioteca sin detener la música. También hay un tirador superior, disponible al leer letras. Los gestos horizontales siguen cambiando de canción; los arrastres cortos vuelven a su posición.
- Mantén pulsada una canción, o usa ⋮, para reproducir ahora, reproducir a continuación, añadir al final de la cola, añadir a listas, compartir, ver detalles o eliminar el archivo. Reproducir a continuación desactiva el orden aleatorio para respetar la posición elegida.
- Eliminar del teléfono exige confirmación; Android 10+ gestiona el consentimiento del archivo. Android 8/9 solicita escritura solo al eliminar. Al completarse, se limpia la cola y las listas. Cancelar conserva el archivo.

# Prisma Music

Reproductor de música local para Android, gratuito y de código abierto. Beta actual: **5.0.1**. Android 8.0 o posterior.

## Beta 1.0.2 — experiencia visual

- Transiciones de páginas de 300 ms y carátulas con fundidos de 340 ms; progreso interpolado entre actualizaciones.
- Liquid Glass: fondo difuminado compatible con Android 8+, reflejos de borde y paneles de cristal translúcido. El desenfoque se calcula fuera del hilo de interfaz al leer la carátula.
- Línea de tiempo por tema: onda cromática, cápsula de cristal, trazo minimalista y segmentos Night; conservan el control táctil y accesible del Slider nativo. La onda es decorativa, no un análisis del audio.
- Botón de información dentro del reproductor para mostrar u ocultar formato, bitrate y frecuencia con animación; comparte la preferencia persistente de Ajustes.

## Ajustes ampliados

- El minirreproductor se oculta al abrir el reproductor completo y vuelve al salir; Atrás regresa a la biblioteca.
- Velocidad de 0.5× a 2×, omisión opcional de silencios y temporizador de 15, 30, 60 o 90 minutos. El servicio pausa la música aunque la interfaz esté cerrada; cerrar el servicio termina la reproducción.
- Información técnica y pantalla encendida configurables (esta última solo en el reproductor completo).
- Duración mínima configurable de 0 a 120 segundos para ocultar audios cortos sin borrarlos. Las preferencias se conservan al cerrar la aplicación.

## Incluido

- Reproducción local con Media3, servicio en segundo plano, sesión multimedia, controles de notificación y Bluetooth.
- Biblioteca de canciones, artistas, álbumes, géneros y carpetas; búsqueda, cola, repetición y aleatorio.
- Listas locales: crear, añadir, quitar canciones y eliminar listas sin borrar archivos.
- Exclusión predeterminada de audios/notas de voz de WhatsApp y WhatsApp Business. Carpetas registradas por MediaStore seleccionables.
- Siete diseños en Ajustes → Temas: Chromatic Flow, Liquid Glass, Pure Minimal, Black Night, Studio, Sunset y Monocromo. Monocromo usa blanco puro en modo claro y negro AMOLED en modo oscuro, con iconos y tipografía en contraste. Liquid Glass y Chromatic Flow funden la portada con el fondo del álbum; Monocromo conserva su carátula centrada y el ambiente discreto. Inicio independiente con canciones, álbumes e historial local.
- Modo **claro, oscuro y según el sistema**, independiente del tema. Preferencias persistentes.
- Ecualizador DSP original de diez bandas: 31, 63, 125, 250, 500 Hz; 1, 2, 4, 8, 16 kHz. Q global ajustable, preamplificador, graves, medios, agudos, presencia de voces y limitador de picos enlazado entre canales.
- Letras incrustadas automáticas: MP3 ID3v2 (USLT/ULT, SYLT/SLT y TXXX de letras), FLAC/Vorbis y Ogg/Opus (LYRICS y variantes), M4A/MP4 (©lyr y campos libres de letras). Prioridad a letras incrustadas; TXT/LRC manual como alternativa. Letras sincronizadas cuando contienen tiempos, seguimiento y salto al pulsar una línea.
- Deslizar a la izquierda/derecha sobre la carátula cambia de canción. Las letras se desplazan verticalmente. No interfiere con la búsqueda de posición ni con el desplazamiento vertical de las letras.
- Metadatos de formato, códec, bitrate, frecuencia de muestreo, canales y profundidad de bits cuando el extractor los informa. Estimaciones de bitrate etiquetadas.

## Compilar y verificar

JDK 17, SDK Android 35, Gradle 8.11.1. El proyecto incluye Gradle Wrapper. Configura el SDK mediante `ANDROID_HOME` o `local.properties`.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Salida: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions ejecuta pruebas, compilación y lint automáticamente. El workflow de distribución genera una APK sin firmar para el mantenedor; no es instalable hasta firmarla. La descarga pública firmada está únicamente en Releases. Nunca se publica la clave privada.

## Límites de esta beta

- La biblioteca depende de MediaStore: archivos ocultos con `.nomedia`, carpetas privadas de apps o archivos aún no indexados pueden no aparecer. No hay exploración SAF recursiva en esta versión.
- La salida ofrece PCM flotante o el modo compatible de 16 bits. Android, el DAC y Bluetooth determinan la salida final; no se promete bit-perfect.
- La presencia de voces realza una zona de frecuencias; no separa voces e instrumentos. Q es común a las diez bandas en esta versión. A frecuencias de muestreo bajas se limita la frecuencia de los filtros a menos de Nyquist.
- Liquid Glass usa una carátula difuminada precalculada, transparencias y reflejos en los bordes. Es una interpretación propia: no reproduce la refracción óptica del sistema de Apple. Los temas cambian también la composición del reproductor y de la biblioteca.
- No se buscan letras en internet. Las etiquetas comprimidas/cifradas, Lyrics3, APE y letras de servicios externos no están cubiertas. SYLT con tiempos expresados en frames se muestra sin sincronización. Metadatos limitados a 16 MB por etiqueta y letra a 1 MB. Un archivo de alta calidad puede no incluir letras ni tiempos.
- El bitrate estimado usa tamaño/duración y puede incluir metadatos. Profundidad de bits no disponible se omite.
- Géneros de MediaStore requieren Android 11 o posterior; en versiones anteriores se muestra “Sin género”.
- La normalización es adaptativa RMS y no un análisis LUFS completo. El tiempo de mezcla sigue la posición del audio; a velocidades distintas de 1× su duración real cambia.
- Omitir silencios está disponible con salida compatible de 16 bits, no en el camino flotante.
- No se restaura la cola tras reiniciar el servicio. Listas y preferencias sí persisten. Quitar de la cola no borra archivos; eliminar del teléfono sí requiere confirmación.

## Próximas versiones

Pruebas en teléfonos reales y Bluetooth; importación/exportación de listas; acceso a carpetas mediante SAF; filtros individuales por banda; proveedor opcional de letras con licencias compatibles; desenfoque y animaciones más refinadas; firma de lanzamiento y APKs de actualización.

## Privacidad y licencia

Sin anuncios, analítica, cuentas ni permiso de internet. Solo lee audio con autorización y archivos de letras elegidos por el usuario. Licencia MIT. El diseño y DSP son originales, inspirados en funciones habituales de reproductores; no incluyen código ni recursos de Poweramp, Spotify o Apple.

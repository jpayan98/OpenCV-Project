# Sistema de Vigilancia con Detección de Movimiento

### Java + OpenCV + Swing

## Descripción General

Este proyecto implementa un sistema de vigilancia en tiempo real
utilizando:

-   Detección de movimiento por diferencia entre frames
-   Reconocimiento mediante Haar Cascade
-   Reproducción de alarma al detectar actividad
-   Interfaz gráfica (Swing) con video en vivo y registro de eventos

## Estructura del Proyecto

    /ProyectoVigilancia
     ├── modelos/
     │     └── haarcascade_frontalface_default.xml
     ├── sounds/
     │     ├── alert_sound.wav
     │     └── alert_sound.mp3
     ├── libs/
     │     └── opencv-460.jar
     ├── native/
     │     └── opencv_java460.dll
     ├── src/
     │     └── InterfazAplicacion.java
     ├── README.md

# Funcionalidades Principales

  -----------------------------------------------------------------------
  Función                     Descripción
  --------------------------- -------------------------------------------
  Captura de video            Utiliza la webcam en tiempo real con
                              OpenCV.

  Detección de movimiento     Diferencia entre frames, aplicación de
                              umbral y contornos.

  Detección de                Mediante Haar Cascade aplicado sobre la
  cuerpos/rostros             zona en movimiento.

  Alarma sonora               Reproduce un sonido si se detecta actividad
                              no autorizada.

  Registro de eventos         Guarda hora, tipo de evento y descripción.

  Interfaz gráfica            Controles para iniciar, detener y limpiar
                              registro.
  -----------------------------------------------------------------------

# Instalación y Dependencias

## Requisitos previos

-   Java JDK 17 o superior\
-   OpenCV 4.x\
-   Webcam\
-   Windows, Linux o macOS

## Instalación de OpenCV (Windows)

1.  Descargar OpenCV desde https://opencv.org/releases/\
2.  Descomprimir en `C:\opencv\`\
3.  Copiar a tu proyecto:
    -   `opencv/build/java/opencv-460.jar` → carpeta `libs/`
    -   `opencv/build/java/x64/opencv_java460.dll` → carpeta `native/`
4.  El proyecto carga automáticamente OpenCV mediante:

``` java
System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
```

# Compilación y Ejecución

## Compilar desde terminal

    javac -cp .;libs/opencv-460.jar src/InterfazAplicacion.java

## Ejecutar

    java -cp .;libs/opencv-460.jar -Djava.library.path=native InterfazAplicacion

# Manual de Usuario

1.  Abrir la aplicación.\
2.  Pulsar "Iniciar vigilancia" para activar la cámara.\
3.  El sistema detecta movimiento, lo marca en pantalla y activa la
    alarma.\
4.  Pulsar "Detener" para finalizar la vigilancia.\
5.  Pulsar "Limpiar registro" para eliminar el historial.

# Problemas Frecuentes

## OpenCV no carga

Verificar que las librerías nativas estén en la ruta especificada y que
el parámetro `java.library.path` sea correcto.

## La alarma no suena

Asegurarse de que existan los archivos `.wav` o `.mp3` dentro del
directorio `sounds/`.

## No detecta movimiento

Puede ser necesario ajustar la sensibilidad modificando el área mínima
del contorno dentro del código.

# Pruebas Realizadas

  Prueba                           Resultado
  -------------------------------- -------------------------------
  Movimiento rápido                Detectado correctamente
  Movimiento lento                 Detectado correctamente
  Sin movimiento                   Sistema estable
  Falta del archivo Haar Cascade   Error controlado
  Alarma repetida                  Suena una vez cada 5 segundos

# Mejoras Futuras

-   Implementación de modelos avanzados como YOLO o redes neuronales.
-   Grabación de video ante detección de movimiento.
-   Implementación de panel web para vigilancia remota.
-   Envío de notificaciones por correo.
-   Selección de sonido desde la interfaz.

# Licencia

Proyecto desarrollado con fines educativos y demostrativos.

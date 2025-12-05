/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

/**
 *
 * @author JAVI
 */
public class Main {

    // Cargar la librería nativa de OpenCV al inicio
    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error al cargar la librería nativa de OpenCV: " + e.getMessage());
            System.err.println("Asegúrate de que OpenCV esté correctamente configurado y las librerías nativas en el java.library.path.");
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        System.out.println("Empezamos a capturar");
        //capturarContornos();
        //capturarCuerpos();
        capturarMovimiento();
        System.out.println("Fin a capturar");
    }

/*    public static void capturarContornos() {
        VideoCapture camera = new VideoCapture(0);
        Mat frame = new Mat();

        while (true) {
            if (camera.read(frame)) {
                // Aplicar detección de color aquí
                // 1. Convertir a escala de grises
                Mat gray = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

                // 2. Binarizar (umbral)
                Mat binary = new Mat();
                Imgproc.threshold(gray, binary, 100, 255, Imgproc.THRESH_BINARY_INV);

                // 3. Buscar contornos
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // 4. Dibujar contornos sobre el frame original
                Imgproc.drawContours(frame, contours, -1, new Scalar(0, 0, 255), 2);

                // 5. Mostrar el resultado
                HighGui.imshow("Contornos en webcam", frame);
            }
            if (HighGui.waitKey(1) == 27) {
                break; // Salir con ESC
            }
        }

        // Liberar la cámara y destruir las ventanas de HighGui
        System.out.println("Liberando la cámara...");
        camera.release();
        frame.release(); // Liberar el Mat del frame también

        System.out.println("Destruyendo ventanas de HighGui...");
        HighGui.destroyAllWindows(); // Cierra todas las ventanas abiertas por HighGui

        System.out.println("Programa finalizado.");
        System.exit(0); // Asegura que la aplicación termine completamente si hay hilos de HighGui
    }

*/

    public static void capturarCuerpos() {
        // Cargar el clasificador Haar Cascade para detección de cuerpos
        //https://github.com/opencv/opencv/tree/master/data/haarcascades
        CascadeClassifier bodyDetector = new CascadeClassifier("modelos/haarcascade_fullbody.xml");


        if (bodyDetector.empty()) {
            System.out.println("No se pudo cargar el clasificador de cuerpos.");
            return;
        }


        VideoCapture camera = new VideoCapture(0); // 0 para la webcam predeterminada
        if (!camera.isOpened()) {
            System.out.println("No se pudo abrir la webcam");
            return;
        }

        Mat frame = new Mat();
        while (true) {
            if (!camera.read(frame) || frame.empty()) {
                break;
            }

            // Convertir a escala de grises
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

            // Detectar cuerpos
            MatOfRect bodys = new MatOfRect();
            bodyDetector.detectMultiScale(gray, bodys, 1.1, 5, 0, new Size(40, 40), new Size());


            // Dibujar rectángulos alrededor de los cuerpos detectados
            for (Rect body : bodys.toArray()) {
                Imgproc.rectangle(frame, body, new Scalar(255, 0, 0), 2);
            }


            // Mostrar el resultado
            HighGui.imshow("Detección de cuerpos en webcam", frame);


            // Salir con la tecla 'ESC'
            if (HighGui.waitKey(1) == 27) {
                break; // Salir con ESC
            }
        }

        // Liberar la cámara y destruir las ventanas de HighGui
        System.out.println("Liberando la cámara...");
        camera.release();
        frame.release(); // Liberar el Mat del frame también

        System.out.println("Destruyendo ventanas de HighGui...");
        HighGui.destroyAllWindows(); // Cierra todas las ventanas abiertas por HighGui

        System.out.println("Programa finalizado.");
        System.exit(0); // Asegura que la aplicación termine completamente si hay hilos de HighGui
    }

    public static void capturarMovimiento() {

        // Cargar clasificador de cuerpos
        CascadeClassifier bodyDetector = new CascadeClassifier("modelos/haarcascade_frontalface_default.xml");

        if (bodyDetector.empty()) {
            System.out.println("No se pudo cargar el clasificador de cuerpos.");
            return;
        }

        VideoCapture camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            System.out.println("No se pudo abrir la webcam");
            return;
        }

        Mat framePrevio = new Mat();
        Mat frameActual = new Mat();

        // Primer frame
        camera.read(framePrevio);
        Imgproc.cvtColor(framePrevio, framePrevio, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(framePrevio, framePrevio, new Size(21, 21), 0);

        while (true) {
            if (!camera.read(frameActual)) break;

            // Convertir a gris + suavizar
            Mat gray = new Mat();
            Imgproc.cvtColor(frameActual, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(9, 9), 0);

            // 1️⃣ DIFERENCIA ENTRE FRAMES
            Mat diff = new Mat();
            Core.absdiff(framePrevio, gray, diff);

            // 2️⃣ UMBRAL PARA RESALTAR MOVIMIENTO
            Mat thresh = new Mat();
            Imgproc.threshold(diff, thresh, 10, 255, Imgproc.THRESH_BINARY);
            Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1, -1), 2);

            // 3️⃣ OBTENER CONTORNOS DE MOVIMIENTO
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(thresh, contours, new Mat(),
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {

                // Ignorar ruido
                if (Imgproc.contourArea(contour) < 150) continue;

                // Rectángulo del área en movimiento
                Rect movimientoRect = Imgproc.boundingRect(contour);

                // ⬇️ EXTRAER SOLO ESA ZONA DEL FRAME
                Mat zonaMovimiento = gray.submat(movimientoRect);

                // 4️⃣ DETECTAR CUERPOS SOLO EN LA ZONA DE MOVIMIENTO
                MatOfRect cuerpos = new MatOfRect();
                bodyDetector.detectMultiScale(
                        zonaMovimiento,
                        cuerpos,
                        1.1,    // escala
                        3,      // vecinos
                        0,
                        new Size(40, 40),
                        new Size()
                );

                // 5️⃣ SI HAY CUERPOS → Mostrar el rectángulo DE MOVIMIENTO
                if (cuerpos.toArray().length > 0) {

                    // Dibujar rectángulo EN FRAME ORIGINAL
                    Imgproc.rectangle(frameActual,
                            movimientoRect,
                            new Scalar(0 , 0, 255),
                            8
                    );

                    Imgproc.putText(frameActual, "Movimiento no autorizado detectado",
                            new Point(movimientoRect.x, movimientoRect.y - 10),
                            Imgproc.FONT_HERSHEY_SIMPLEX,
                            0.6,
                            new Scalar(0, 0, 0),
                            2
                    );
                }
            }

            HighGui.imshow("Movimiento + Cuerpos", frameActual);

            // Actualizar frame previo
            gray.copyTo(framePrevio);

            if (HighGui.waitKey(1) == 27) break; // ESC para salir
        }

        // Limpieza
        camera.release();
        HighGui.destroyAllWindows();
        System.exit(0);
    }


}

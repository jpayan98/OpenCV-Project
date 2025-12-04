/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
        capturarCaras();
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

    public static void capturarCaras() {
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


            // Dibujar rectángulos alrededor de las cuerpos detectadas
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
}

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
        // Cargar el clasificador Haar Cascade para detección de caras
        //https://github.com/opencv/opencv/tree/master/data/haarcascades
        //CascadeClassifier faceDetector = new CascadeClassifier("modelos/haarcascade_frontalface_default.xml");
        CascadeClassifier faceDetector = new CascadeClassifier("modelos/haarcascade_frontalface_default.xml");
        CascadeClassifier eyeDetector = new CascadeClassifier("modelos/haarcascade_eye.xml");

        if (faceDetector.empty()) {
            System.out.println("No se pudo cargar el clasificador de caras.");
            return;
        }
        if (eyeDetector.empty()){
            System.out.println("No se pudo cargar el clasificador de ojos.");
        }

        VideoCapture camera = new VideoCapture(0); // 0 para la webcam predeterminada
        if (!camera.isOpened()) {
            System.out.println("No se pudo abrir la webcam");
            return;
        }

        Mat frame = new Mat();
        while (true) {
            // Variables para suavizado temporal del rectángulo
            int smoothX = 0, smoothY = 0, smoothW = 0, smoothH = 0;
            boolean initialized = false;

            while (true) {

                if (!camera.read(frame) || frame.empty()) {
                    break;
                }

                // Convertir a escala gris
                Mat gray = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);

                // Detectar caras
                MatOfRect faces = new MatOfRect();
                faceDetector.detectMultiScale(gray, faces, 1.1, 5, 0, new Size(40,40), new Size());

                Rect[] detectedFaces = faces.toArray();

                if (detectedFaces.length > 0) {

                    // ======== 1. SUAVIZADO TEMPORAL DEL RECTÁNGULO ========
                    Rect face = detectedFaces[0];  // usamos la primera cara detectada

                    if (!initialized) {
                        smoothX = face.x;
                        smoothY = face.y;
                        smoothW = face.width;
                        smoothH = face.height;
                        initialized = true;
                    }

                    // Filtro exponencial (suaviza el movimiento del rectángulo)
                    double alpha = 0.15;   // cuanto menor, más suave
                    smoothX = (int)(alpha * face.x + (1 - alpha) * smoothX);
                    smoothY = (int)(alpha * face.y + (1 - alpha) * smoothY);
                    smoothW = (int)(alpha * face.width + (1 - alpha) * smoothW);
                    smoothH = (int)(alpha * face.height + (1 - alpha) * smoothH);

                    Rect smoothFace = new Rect(smoothX, smoothY, smoothW, smoothH);

                    // ======== 2. RECORTE DEL ROSTRO ========
                    Mat faceROI = new Mat(gray, smoothFace);

                    // ======== 3. SUAVIZADO ESPACIAL ========
                    Imgproc.GaussianBlur(faceROI, faceROI, new Size(7, 7), 0);

                    // ======== 4. CANNY ESTABLE ========
                    Mat edges = new Mat();
                    Imgproc.Canny(faceROI, edges, 90, 160);

                    // ======== 5. CONTORNOS FILTRADOS ========
                    List<MatOfPoint> contours = new ArrayList<>();
                    Mat hierarchy = new Mat();
                    Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                    for (MatOfPoint c : contours) {

                        // Filtrar contornos pequeños → elimina ruido
                        double area = Imgproc.contourArea(c);
                        if (area < 60) continue; // Ajustable

                        // Convertir puntos a coordenadas reales de la imagen
                        List<org.opencv.core.Point> shifted = new ArrayList<>();
                        for (org.opencv.core.Point p : c.toArray()) {
                            shifted.add(new org.opencv.core.Point(p.x + smoothFace.x, p.y + smoothFace.y));
                        }

                        MatOfPoint shiftedContour = new MatOfPoint();
                        shiftedContour.fromList(shifted);

                        // Dibujar contorno suavizado
                        Imgproc.drawContours(frame, List.of(shiftedContour), -1, new Scalar(255, 0, 0), 2);
                    }
                }

                // ======== OJOS EN CUADRADOS ========
                MatOfRect eyes = new MatOfRect();
                eyeDetector.detectMultiScale(gray, eyes, 1.6, 5, 0, new Size(20,20), new Size());

                for (Rect eye : eyes.toArray()) {
                    Imgproc.rectangle(frame, eye, new Scalar(0, 255, 0), 2);
                }

                // Mostrar resultado
                HighGui.imshow("Detección estable", frame);

                if (HighGui.waitKey(1) == 27){
                    break;
                }
            }
            System.out.println("Liberando la cámara...");
            camera.release();
            frame.release(); // Liberar el Mat del frame también

            System.out.println("Destruyendo ventanas de HighGui...");
            HighGui.destroyAllWindows(); // Cierra todas las ventanas abiertas por HighGui

            System.out.println("Programa finalizado.");
            System.exit(0); // Asegura que la aplicación termine completamente si hay hilos de HighGui

        }

        // Liberar la cámara y destruir las ventanas de HighGui
           }
}

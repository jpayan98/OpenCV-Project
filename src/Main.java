
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import java.util.ArrayList;
import java.util.List;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;


public class Main {
    private static boolean alarmaReproducida = false;
    private static long ultimoMovimiento = 0;

    public static void playAlertSound() {
        if (alarmaReproducida) {
            return; // Ya sonó, no volver a reproducir
        }

        try {
            String path = "sounds/alert_sound.mp3";
            Media sound = new Media(new File(path).toURI().toString());
            MediaPlayer mediaPlayer = new MediaPlayer(sound);
            mediaPlayer.play();

            alarmaReproducida = true; // <-- marcar como reproducida

        } catch (Exception e) {
            System.out.println("Error al reproducir sonido: " + e.getMessage());
        }
    }

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
        new JFXPanel();
        System.out.println("Empezamos a capturar");
        InterfazAplicacion interfaz = new InterfazAplicacion();
        interfaz.setVisible(true);
        //capturarMovimiento();
        System.out.println("Fin a capturar");
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
            Imgproc.GaussianBlur(gray, gray, new Size(25, 25), 0);

            // 1️⃣ DIFERENCIA ENTRE FRAMES
            Mat diff = new Mat();
            Core.absdiff(framePrevio, gray, diff);

            // 2️⃣ UMBRAL PARA RESALTAR MOVIMIENTO
            Mat thresh = new Mat();
            Imgproc.threshold(diff, thresh, 20, 255, Imgproc.THRESH_BINARY);
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

                    ultimoMovimiento = System.currentTimeMillis();

                    if (!alarmaReproducida) {
                        playAlertSound();
                    }
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

            HighGui.imshow("Detector de intrusos", frameActual);

            long ahora = System.currentTimeMillis();
            if (alarmaReproducida && (ahora - ultimoMovimiento > 5000)) {
                alarmaReproducida = false;
                System.out.println("⚠ Alarma reseteada tras 5 segundos sin movimiento.");
            }


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

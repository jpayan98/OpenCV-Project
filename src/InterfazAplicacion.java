import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.sound.sampled.*;
import java.io.IOException;

public class InterfazAplicacion extends JFrame {

    // Componentes de la interfaz
    private JLabel videoLabel;
    private JLabel estadoLabel;
    private JButton iniciarButton;
    private JButton detenerButton;
    private JButton limpiarButton;
    private JTable tablaDetecciones;
    private DefaultTableModel modeloTabla;

    // Variables del sistema de detección
    private VideoCapture camera;
    private Thread capturaThread;
    private volatile boolean sistemaActivo = false;
    private boolean alarmaReproducida = false;
    private long ultimoMovimiento = 0;
    private boolean debeGuardarCaptura = false;

    // OpenCV
    private CascadeClassifier bodyDetector;
    private Mat framePrevio;

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error al cargar OpenCV: " + e.getMessage());
            System.exit(1);
        }
    }

    public InterfazAplicacion() {
        configurarVentana();
        crearComponentes();
        cargarModelo();
    }

    private void configurarVentana() {
        setTitle("Sistema de Vigilancia con Detección de Movimiento");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setSize(1200, 700);
        setLocationRelativeTo(null);

        // Cerrar recursos al salir
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                detenerSistema();
                System.exit(0);
            }
        });
    }

    private void crearComponentes() {
        // Panel superior - Estado y controles
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 10));
        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Estado del sistema
        estadoLabel = new JLabel("⚫ SISTEMA DETENIDO");
        estadoLabel.setFont(new Font("Arial", Font.BOLD, 18));
        estadoLabel.setForeground(new Color(150, 150, 150));
        estadoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        estadoLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // === NUEVOS BOTONES A LA DERECHA DEL ESTADO ===
        JButton btnAbrirCarpeta = new JButton("📁 Ver capturas");
        JButton btnSalir = new JButton("❌ Salir");

        btnAbrirCarpeta.setFocusPainted(false);
        btnSalir.setFocusPainted(false);

        // Abrir carpeta captMov
        btnAbrirCarpeta.addActionListener(e -> {
            try {
                File carpeta = new File("captMov");
                if (!carpeta.exists()) carpeta.mkdirs();
                Desktop.getDesktop().open(carpeta);
            } catch (Exception ex) {
                System.out.println("⚠ No se pudo abrir la carpeta: " + ex.getMessage());
            }
        });

        // Botón salir
        btnSalir.addActionListener(e -> cerrarAplicacion());

        // Panel combinado de estado + botones
        JPanel panelEstado = new JPanel(new BorderLayout());
        panelEstado.add(estadoLabel, BorderLayout.CENTER);

        JPanel panelBotonesEstado = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        panelBotonesEstado.add(btnAbrirCarpeta);
        panelBotonesEstado.add(btnSalir);

        panelEstado.add(panelBotonesEstado, BorderLayout.EAST);

        // === PANEL DE BOTONES PRINCIPALES ===
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        iniciarButton = new JButton("▶ Iniciar Vigilancia");
        iniciarButton.setFont(new Font("Arial", Font.BOLD, 14));
        iniciarButton.setBackground(new Color(46, 204, 113));
        iniciarButton.setForeground(Color.WHITE);
        iniciarButton.setFocusPainted(false);
        iniciarButton.setPreferredSize(new Dimension(180, 40));

        detenerButton = new JButton("⏹ Detener");
        detenerButton.setFont(new Font("Arial", Font.BOLD, 14));
        detenerButton.setBackground(new Color(231, 76, 60));
        detenerButton.setForeground(Color.WHITE);
        detenerButton.setFocusPainted(false);
        detenerButton.setPreferredSize(new Dimension(150, 40));
        detenerButton.setEnabled(false);

        limpiarButton = new JButton("🗑 Limpiar Registro");
        limpiarButton.setFont(new Font("Arial", Font.PLAIN, 12));
        limpiarButton.setPreferredSize(new Dimension(150, 40));

        panelBotones.add(iniciarButton);
        panelBotones.add(detenerButton);
        panelBotones.add(limpiarButton);

        panelSuperior.add(panelEstado, BorderLayout.NORTH);
        panelSuperior.add(panelBotones, BorderLayout.CENTER);

        // Panel central - Video + Tabla
        JPanel panelCentral = new JPanel(new GridLayout(1, 2, 10, 10));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Panel del video
        JPanel panelVideo = new JPanel(new BorderLayout());
        panelVideo.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "Video en Vivo",
                0, 0,
                new Font("Arial", Font.BOLD, 14)
        ));

        videoLabel = new JLabel();
        videoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        videoLabel.setVerticalAlignment(SwingConstants.CENTER);
        videoLabel.setBackground(Color.BLACK);
        videoLabel.setOpaque(true);
        videoLabel.setText("Cámara desactivada");
        videoLabel.setForeground(Color.WHITE);
        videoLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        videoLabel.setPreferredSize(new Dimension(640, 480));

        panelVideo.add(videoLabel, BorderLayout.CENTER);

        // Panel de registro
        JPanel panelRegistro = new JPanel(new BorderLayout());
        panelRegistro.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "Registro de Detecciones",
                0, 0,
                new Font("Arial", Font.BOLD, 14)
        ));

        String[] columnas = {"Hora", "Evento", "Descripción"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaDetecciones = new JTable(modeloTabla);
        tablaDetecciones.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tablaDetecciones.setRowHeight(25);
        tablaDetecciones.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(tablaDetecciones);
        panelRegistro.add(scrollPane, BorderLayout.CENTER);

        panelCentral.add(panelVideo);
        panelCentral.add(panelRegistro);

        // Agregar todo al frame
        add(panelSuperior, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER);

        // Listeners
        iniciarButton.addActionListener(e -> iniciarSistema());
        detenerButton.addActionListener(e -> detenerSistema());
        limpiarButton.addActionListener(e -> limpiarRegistro());
    }


    private void cargarModelo() {
        bodyDetector = new CascadeClassifier("modelos/haarcascade_frontalface_default.xml");
        if (bodyDetector.empty()) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo cargar el modelo de detección.\nVerifica que el archivo exista en la carpeta 'modelos/'",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void iniciarSistema() {
        if (sistemaActivo) return;

        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir la webcam",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        sistemaActivo = true;
        alarmaReproducida = false;

        // Actualizar interfaz
        estadoLabel.setText("🔴 SISTEMA ACTIVO - VIGILANDO");
        estadoLabel.setForeground(new Color(231, 76, 60));
        iniciarButton.setEnabled(false);
        detenerButton.setEnabled(true);

        agregarRegistro("Sistema iniciado", "Vigilancia activa");

        // Iniciar thread de captura
        capturaThread = new Thread(this::capturarMovimiento);
        capturaThread.start();
    }

    private void detenerSistema() {
        System.out.println("Deteniendo sistema...");

        // Marcar como inactivo primero
        sistemaActivo = false;

        // Esperar a que termine el thread con timeout
        if (capturaThread != null && capturaThread.isAlive()) {
            try {
                System.out.println("Esperando a que finalice el thread de captura...");
                capturaThread.interrupt(); // Interrumpir el thread por si está en sleep
                capturaThread.join(3000); // Esperar máximo 3 segundos

                if (capturaThread.isAlive()) {
                    System.out.println("⚠ El thread no finalizó a tiempo, forzando detención...");
                    capturaThread.interrupt();
                    capturaThread.join(2000);
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupción al esperar thread: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        // Liberar Mats de OpenCV
        if (framePrevio != null && !framePrevio.empty()) {
            System.out.println("Liberando framePrevio...");
            framePrevio.release();
            framePrevio = null;
        }

        // Liberar cámara
        if (camera != null && camera.isOpened()) {
            System.out.println("Liberando cámara...");
            camera.release();
            camera = null;
        }

        // Actualizar interfaz
        SwingUtilities.invokeLater(() -> {
            estadoLabel.setText("⚫ SISTEMA DETENIDO");
            estadoLabel.setForeground(new Color(150, 150, 150));
            iniciarButton.setEnabled(true);
            detenerButton.setEnabled(false);
            videoLabel.setIcon(null);
            videoLabel.setText("Cámara desactivada");
            if (modeloTabla.getRowCount() == 0 ||
                    !modeloTabla.getValueAt(0, 1).equals("Sistema detenido")) {
                agregarRegistro("Sistema detenido", "Vigilancia desactivada");
            }
        });

        System.out.println("✓ Sistema detenido correctamente");
    }

    private void cerrarAplicacion() {
        System.out.println("Cerrando aplicación...");
        detenerSistema();
        System.out.println("Finalizando proceso...");
        System.exit(0);
    }

    private void capturarMovimiento() {
        if (bodyDetector.empty()) {
            System.out.println("No se pudo cargar el clasificador de cuerpos.");
            return;
        }

        framePrevio = new Mat();
        Mat frameActual = new Mat();
        Mat gray = null;
        Mat diff = null;
        Mat thresh = null;

        try {
            // Primer frame
            if (!camera.read(framePrevio)) {
                System.out.println("No se pudo leer el primer frame");
                return;
            }
            Imgproc.cvtColor(framePrevio, framePrevio, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(framePrevio, framePrevio, new Size(21, 21), 0);

            while (sistemaActivo && !Thread.currentThread().isInterrupted()) {
                if (!camera.read(frameActual)) {
                    System.out.println("No se pudo leer frame de la cámara");
                    break;
                }

                // Convertir a gris + suavizar
                gray = new Mat();
                Imgproc.cvtColor(frameActual, gray, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(gray, gray, new Size(9, 9), 0);

                // 1️⃣ DIFERENCIA ENTRE FRAMES
                diff = new Mat();
                Core.absdiff(framePrevio, gray, diff);

                // 2️⃣ UMBRAL PARA RESALTAR MOVIMIENTO
                thresh = new Mat();
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
                            1.1,
                            3,
                            0,
                            new Size(40, 40),
                            new Size()
                    );

                    // 5️⃣ SI HAY CUERPOS → Mostrar el rectángulo DE MOVIMIENTO
                    if (cuerpos.toArray().length > 0) {
                        ultimoMovimiento = System.currentTimeMillis();

                        if (debeGuardarCaptura) {
                            guardarCaptura(frameActual);  // NUEVA FUNCIÓN
                            debeGuardarCaptura = false;    // Reseteamos
                        }

                        if (!alarmaReproducida) {
                            playAlertSound();
                            agregarRegistro("⚠ ALERTA", "Movimiento no autorizado detectado");

                        }

                        // Dibujar rectángulo EN FRAME ORIGINAL
                        Imgproc.rectangle(frameActual,
                                movimientoRect,
                                new Scalar(0, 0, 255),
                                8
                        );

                        Imgproc.putText(frameActual, "Movimiento no autorizado detectado",
                                new Point(movimientoRect.x, movimientoRect.y - 10),
                                Imgproc.FONT_HERSHEY_SIMPLEX,
                                0.6,
                                new Scalar(0, 0, 255),
                                2
                        );
                    }
                }

                // Mostrar frame en la interfaz
                if (sistemaActivo) {
                    mostrarFrame(frameActual);
                }

                // Reset de alarma
                long ahora = System.currentTimeMillis();
                if (alarmaReproducida && (ahora - ultimoMovimiento > 5000)) {
                    alarmaReproducida = false;
                    System.out.println("⚠ Alarma reseteada tras 5 segundos sin movimiento.");
                    debeGuardarCaptura=true;
                }

                // Actualizar frame previo
                gray.copyTo(framePrevio);

                // Liberar Mats temporales
                if (diff != null) diff.release();
                if (thresh != null) thresh.release();

                try {
                    Thread.sleep(30); // ~33 FPS
                } catch (InterruptedException e) {
                    System.out.println("Thread de captura interrumpido durante sleep");
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error en captura: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Limpiar todos los recursos de OpenCV
            System.out.println("Thread de captura finalizado - limpiando recursos Mat");
            if (frameActual != null && !frameActual.empty()) frameActual.release();
            if (gray != null && !gray.empty()) gray.release();
            if (diff != null && !diff.empty()) diff.release();
            if (thresh != null && !thresh.empty()) thresh.release();
        }
    }

    private void mostrarFrame(Mat frame) {
        if (frame.empty()) return;

        BufferedImage imagen = matToBufferedImage(frame);

        // Escalar imagen para que se ajuste al label
        int anchoLabel = videoLabel.getWidth();
        int altoLabel = videoLabel.getHeight();

        if (anchoLabel > 0 && altoLabel > 0) {
            Image imagenEscalada = imagen.getScaledInstance(anchoLabel, altoLabel, Image.SCALE_FAST);
            ImageIcon icon = new ImageIcon(imagenEscalada);

            SwingUtilities.invokeLater(() -> {
                videoLabel.setText("");
                videoLabel.setIcon(icon);
            });
        }
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] buffer = new byte[bufferSize];
        mat.get(0, 0, buffer);

        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(buffer, 0, targetPixels, 0, buffer.length);

        return image;
    }

    private void playAlertSound() {
        if (alarmaReproducida) return;

        new Thread(() -> {
            try {
                // Intentar primero con .wav
                String pathWav = "sounds/alert_sound.wav";
                File audioFile = new File(pathWav);

                // Si no existe .wav, intentar con .mp3
                if (!audioFile.exists()) {
                    String pathMp3 = "sounds/alert_sound.mp3";
                    audioFile = new File(pathMp3);
                }

                if (!audioFile.exists()) {
                    System.out.println("⚠ Archivo de audio no encontrado en: " + audioFile.getAbsolutePath());
                    System.out.println("⚠ Asegúrate de tener 'sounds/alert_sound.wav' o 'sounds/alert_sound.mp3'");
                    alarmaReproducida = true;
                    return;
                }

                // Cargar y reproducir el audio
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                System.out.println("✓ Reproduciendo alarma: " + audioFile.getName());
                clip.start();

                // Esperar a que termine de reproducirse
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

            } catch (UnsupportedAudioFileException e) {
                System.out.println("⚠ Formato de audio no soportado. Usa archivos .wav");
                System.out.println("   Para MP3, necesitas convertirlo a WAV primero.");
            } catch (IOException e) {
                System.out.println("⚠ Error al leer el archivo de audio: " + e.getMessage());
            } catch (LineUnavailableException e) {
                System.out.println("⚠ Sistema de audio no disponible: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("⚠ Error inesperado al reproducir audio: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        alarmaReproducida = true;
    }

    private void agregarRegistro(String evento, String descripcion) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String hora = sdf.format(new Date());
            modeloTabla.insertRow(0, new Object[]{hora, evento, descripcion});

            // Limitar a 100 registros
            if (modeloTabla.getRowCount() > 100) {
                modeloTabla.removeRow(100);
            }
        });
    }

    private void limpiarRegistro() {
        modeloTabla.setRowCount(0);
        agregarRegistro("Registro limpiado", "Historial borrado");
    }

    private void guardarCaptura(Mat frame) {
        try {
            // Crear carpeta si no existe
            File carpeta = new File("captMov");
            if (!carpeta.exists()) carpeta.mkdirs();

            // Nombre: captura_2025-12-08_22-14-05.jpg
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String filename = "captMov/captura_" + timestamp + ".jpg";

            // Guardar imagen con OpenCV
            Imgcodecs.imwrite(filename, frame);

            System.out.println("📸 Imagen de movimiento guardada: " + filename);

            // Registrar en tabla
            agregarRegistro("Captura guardada", filename);

        } catch (Exception e) {
            System.out.println("⚠ Error guardando la captura: " + e.getMessage());
        }
    }

}
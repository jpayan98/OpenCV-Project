import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
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
                //detenerSistema();
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
        estadoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        estadoLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        // Panel de botones
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

        panelSuperior.add(estadoLabel, BorderLayout.NORTH);
        panelSuperior.add(panelBotones, BorderLayout.CENTER);

        // Panel central - Video y registro
        JPanel panelCentral = new JPanel(new GridLayout(1, 2, 10, 10));
        panelCentral.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Video de la cámara
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

        // Tabla de detecciones
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

        // Añadir todo al frame
        add(panelSuperior, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER);

        // Listeners de botones
        //iniciarButton.addActionListener(e -> iniciarSistema());
        //detenerButton.addActionListener(e -> detenerSistema());
        //limpiarButton.addActionListener(e -> limpiarRegistro());
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
}

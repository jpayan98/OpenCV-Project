import javafx.embed.swing.JFXPanel;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Empezamos a capturar");
            InterfazAplicacion interfaz = new InterfazAplicacion();
            interfaz.setVisible(true);
        });
    }
}
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package airlinemanagement;

/**
 *
 * @author divya
 */
public class AirlineManagement {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Launch the splash screen first
        try {
            // Set system look and feel
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }
        
        // Launch the application starting with splash screen
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new Splash().setVisible(true);
                } catch (Exception e) {
                    System.err.println("Error launching splash screen: " + e.getMessage());
                    // Fallback: launch login directly
                    new Login().setVisible(true);
                }
            }
        });
    }
}

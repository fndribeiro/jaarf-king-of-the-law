package br.com.ribeiro.fernando;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Main {

    private static String outputFolderPath = "";
    private static List<LawsuitInfo> skippedLawsuits = new ArrayList<>(); 

    public static void main(String[] args) {
    	skippedLawsuits.clear();
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
    	
        JFrame frame = new JFrame("JAARF King of The Law");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 250);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Select a PDF file:");
        JTextField filePathField = new JTextField(30);
        JButton browseButton = new JButton("Browse");
        JButton selectOutputFolderButton = new JButton("Select Output Folder");
        JButton processButton = new JButton("Process");

        JPanel inputPanel = new JPanel();
        inputPanel.add(label);
        inputPanel.add(filePathField);
        inputPanel.add(browseButton);

        JPanel outputPanel = new JPanel();
        outputPanel.add(new JLabel("Output Folder:"));
        outputPanel.add(selectOutputFolderButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(outputPanel, BorderLayout.CENTER);
        panel.add(processButton, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setVisible(true);

        JFileChooser fileChooser = new JFileChooser();
        browseButton.addActionListener(e -> {
            int result = fileChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        selectOutputFolderButton.addActionListener(e -> {
            JFileChooser folderChooser = new JFileChooser();
            folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = folderChooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                outputFolderPath = folderChooser.getSelectedFile().getAbsolutePath();
                JOptionPane.showMessageDialog(frame, "Output folder set to: " + outputFolderPath, "Folder Selected", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        processButton.addActionListener(e -> {
        	
            if (outputFolderPath.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select an output folder!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String filePath = filePathField.getText();
            
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please select a file!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File pdfFile = new File(filePath);
            
            if (pdfFile.getName().endsWith(".pdf")) {
                new PdfExtractor(frame, outputFolderPath, pdfFile).run();
                
            } else if (pdfFile.getName().endsWith(".csv")) {
            	new CsvExtractor(frame, pdfFile, outputFolderPath).run();
            	
            } else {
            	JOptionPane.showMessageDialog(frame, "Please select a pdf or csv file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        });
    }

}

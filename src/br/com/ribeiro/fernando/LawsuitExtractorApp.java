package br.com.ribeiro.fernando;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public class LawsuitExtractorApp {

    private static String outputFolderPath = "";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LawsuitExtractorApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("JAARF King of The Law");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 250);

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
            if (!pdfFile.exists() || !pdfFile.getName().endsWith(".pdf")) {
                JOptionPane.showMessageDialog(frame, "Invalid file selected. Please select a PDF file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Show a loading dialog while processing
            SwingUtilities.invokeLater(() -> {
                JOptionPane optionPane = new JOptionPane("Processing, please wait...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                final JFrame loadingFrame = new JFrame();
                loadingFrame.setUndecorated(true);
                loadingFrame.add(optionPane);
                loadingFrame.pack();
                loadingFrame.setLocationRelativeTo(frame);
                loadingFrame.setVisible(true);

                // Run the processing logic in a separate thread
                new Thread(() -> {
                    try {
                        List<LawsuitInfo> lawsuits = extractLawsuitsFromPDF(pdfFile);
                        Map<String, List<LawsuitInfo>> lawsuitsByLawyer = new HashMap<>();

                        for (LawsuitInfo lawsuit : lawsuits) {
                            String firstBlock = lawsuit.getLawsuitNumber().split("-")[0];

                            // Assign lawsuits to the correct lawyer
                            String lawyer;
                            if (firstBlock.matches(".*[12]$")) {
                                lawyer = "Fernando";
                            } else if (firstBlock.matches(".*[34]$")) {
                                lawyer = "Rafael";
                            } else if (firstBlock.matches(".*[56]$")) {
                                lawyer = "Joao";
                            } else if (firstBlock.matches(".*[78]$")) {
                                lawyer = "Izabella"; // Catch-all for unmatched cases
                            } else {
                            	lawyer = "Nao Encontrado"; // Catch-all for unmatched cases
                            }

                            // Group lawsuits by lawyer
                            lawsuitsByLawyer.computeIfAbsent(lawyer, k -> new ArrayList<>()).add(lawsuit);
                        }

                        // Generate a DOCX file for each lawyer
                        for (Map.Entry<String, List<LawsuitInfo>> entry : lawsuitsByLawyer.entrySet()) {
                            createDocxFileForLawyer(entry.getKey(), entry.getValue());
                        }

                        SwingUtilities.invokeLater(() -> {
                            loadingFrame.dispose();
                            JOptionPane.showMessageDialog(frame, "Processing completed. Files saved to output folder.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        });

                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            loadingFrame.dispose();
                            JOptionPane.showMessageDialog(frame, "Error processing file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            });
        });
    }

    private static List<LawsuitInfo> extractLawsuitsFromPDF(File pdfFile) throws Exception {
        List<LawsuitInfo> lawsuits = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            String[] splitByLawsuit = text.split("(?=SP - (DJE/TJSP|DEJT/TRT2|DOESP))");
            Pattern datePattern = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
            Pattern lawsuitNumberPattern = Pattern.compile("\\d{6,7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");

            for (String lawsuit : splitByLawsuit) {
                Matcher dateMatcher = datePattern.matcher(lawsuit);
                Matcher lawsuitNumberMatcher = lawsuitNumberPattern.matcher(lawsuit);

                String date = dateMatcher.find() ? dateMatcher.group() : "Not Found";
                String lawsuitNumber = lawsuitNumberMatcher.find() ? lawsuitNumberMatcher.group() : "Not Found";

                lawsuits.add(new LawsuitInfo(date, lawsuitNumber, lawsuit.trim()));
            }
        }

        return lawsuits;
    }

    private static void createDocxFileForLawyer(String lawyer, List<LawsuitInfo> lawsuits) {
        try (XWPFDocument doc = new XWPFDocument()) {
            File outputFile = new File(outputFolderPath, lawyer + ".docx");

            for (LawsuitInfo lawsuit : lawsuits) {
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText("Data: " + lawsuit.getDate());
                run.addBreak();
                run.setText("Processo: " + lawsuit.getLawsuitNumber());
                run.addBreak();
                run.setText(extractDescription(lawsuit.getFullText()));
                run.addBreak();
                run.addBreak();
            }

            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                doc.write(out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractDescription(String text) {
        Pattern descriptionPattern = Pattern.compile(
            "(SP - (DJE/TJSP|DEJT/TRT2|DOESP).*?)(?=\\[CodGrifon:)", Pattern.DOTALL);
        Matcher matcher = descriptionPattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Description Not Found";
    }
}

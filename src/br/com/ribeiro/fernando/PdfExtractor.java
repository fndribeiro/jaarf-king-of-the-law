package br.com.ribeiro.fernando;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public class PdfExtractor {
	
	private final List<LawsuitInfo> skippedLawsuits = new ArrayList<>(); 
	
	private final JFrame frame;
	private final String outputFolderPath;
	private final File file;
	
	public PdfExtractor(JFrame frame, String outputFolderPath, File file) {
		this.frame = frame;
		this.outputFolderPath = outputFolderPath;
		this.file = file;
	}

	public void run() {
		
		skippedLawsuits.clear();
		
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
                    List<LawsuitInfo> lawsuits = extractLawsuitsFromPDF(file);
                    Map<String, List<LawsuitInfo>> lawsuitsByLawyer = new HashMap<>();

                    for (LawsuitInfo lawsuit : lawsuits) {
                    	
                    	String firstBlock = lawsuit.getLawsuitNumber().split("-")[0];

                        // Find the effective digit by checking backward until it's not 9 or 0
                        int index = firstBlock.length() - 1;
                        while (index >= 0 && (firstBlock.charAt(index) == '9' || firstBlock.charAt(index) == '0')) {
                            index--; // Move backward
                        }

                        // Determine the effective digit or set to 'n' if none is found
                        char effectiveDigit = (index >= 0) ? firstBlock.charAt(index) : 'n'; // 'n' represents "Nao Encontrado"

                        // Assign lawsuits to the correct lawyer based on the effectiveDigit
                        String lawyer;
                        switch (effectiveDigit) {
                            case '1': case '2':
                                lawyer = "Fernando";
                                break;
                            case '3': case '4':
                                lawyer = "Joao";
                                break;
                            case '5': case '6':
                                lawyer = "Rafael";
                                break;
                            case '7': case '8':
                                lawyer = "Izabella";
                                break;
                            default:
                                lawyer = "Nao Encontrado"; // Catch-all for unmatched or invalid cases
                                
                        }

                        // Group lawsuits by lawyer
                        lawsuitsByLawyer.computeIfAbsent(lawyer, k -> new ArrayList<>()).add(lawsuit);
                    }

                    // Generate a DOCX file for each lawyer
                    for (Map.Entry<String, List<LawsuitInfo>> entry : lawsuitsByLawyer.entrySet()) {
                        createDocxFileForLawyer(entry.getKey(), entry.getValue());
                    }
                    
                    createDocxFileForSkippedLawsuits(skippedLawsuits);
                    
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

    private void createDocxFileForLawyer(String lawyer, List<LawsuitInfo> lawsuits) {
    	
        try (XWPFDocument doc = new XWPFDocument()) {
        	
            File outputFile = new File(outputFolderPath, lawyer + ".docx");

            for (LawsuitInfo lawsuit : lawsuits) {
            	
            	String description = extractDescription(lawsuit.getFullText());
            	
            	// Check for keywords in the description
                if (description.toLowerCase().contains("execução fiscal") ||
                    description.toLowerCase().contains("tribunal de contas") ||
                    description.toLowerCase().contains("tributário")) {
                	
                    skippedLawsuits.add(lawsuit);  // Add to the skipped list

                    continue; // Skip this iteration if any keyword is foundip this iteration if any keyword is found
                    
                }
                
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText("Data: " + lawsuit.getDate());
                run.addBreak();
                run.setText("Processo: " + lawsuit.getLawsuitNumber());
                run.addBreak();
                run.setText(description);
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
    
    private void createDocxFileForSkippedLawsuits(List<LawsuitInfo> lawsuits) {
    	
        try (XWPFDocument doc = new XWPFDocument()) {
        	
            File outputFile = new File(outputFolderPath, "Ignorados" + ".docx");

            for (LawsuitInfo lawsuit : lawsuits) {
            	
            	String description = extractDescription(lawsuit.getFullText());
            	
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText("Data: " + lawsuit.getDate());
                run.addBreak();
                run.setText("Processo: " + lawsuit.getLawsuitNumber());
                run.addBreak();
                run.setText(description);
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

    private String extractDescription(String text) {
        Pattern descriptionPattern = Pattern.compile(
            "(SP - (DJE/TJSP|DEJT/TRT2|DOESP).*?)(?=\\[CodGrifon:)", Pattern.DOTALL);
        Matcher matcher = descriptionPattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "Description Not Found";
    }

}

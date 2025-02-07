package br.com.ribeiro.fernando;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class CsvExtractor {

    private static final Pattern LAWSUIT_PATTERN = Pattern.compile("\\d{6,7}-\\d{2}\\.\\d{4}\\.\\d\\.\\d{2}\\.\\d{4}");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter DATE_FORMATTER_FALLBACK = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final File csvFile;
    private final String outputFolderPath;
    private final JFrame frame;

    public CsvExtractor(JFrame frame, File csvFile, String outputFolderPath) {
        this.frame = frame;
        this.csvFile = csvFile;
        this.outputFolderPath = outputFolderPath;
    }

    public void run() {
        // Show a loading dialog while processing
        SwingUtilities.invokeLater(() -> {
            JOptionPane optionPane = new JOptionPane("Processing CSV, please wait...", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
            final JFrame loadingFrame = new JFrame();
            loadingFrame.setUndecorated(true);
            loadingFrame.add(optionPane);
            loadingFrame.pack();
            loadingFrame.setLocationRelativeTo(frame);
            loadingFrame.setVisible(true);

            // Run the processing logic in a separate thread
            new Thread(() -> {
                try {
                    processCsvFile();
                    SwingUtilities.invokeLater(() -> {
                        loadingFrame.dispose();
                        JOptionPane.showMessageDialog(frame, "CSV processing completed. Files saved to output folder.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        loadingFrame.dispose();
                        JOptionPane.showMessageDialog(frame, "Error processing CSV file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });
    }

    private void processCsvFile() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(csvFile.getAbsolutePath()), StandardCharsets.ISO_8859_1)) {
            List<String> lines = reader
                    .lines()
                    .collect(Collectors.toList());

            if (lines.isEmpty()) return;

            String header = lines.get(0);

            List<String> filteredLines = lines
                    .stream()
                    .skip(1) // Skip header
                    .filter(this::isFromLastDay)
                    .filter(this::hasEffectiveDigit3or4)
                    .collect(Collectors.toList());

            if (!filteredLines.isEmpty()) {
                writeFilteredCsv(header, filteredLines);
            }
        }
    }

    private boolean isFromLastDay(String line) {
        String[] columns = line.split(";");

        if (columns.length < 2) return false;

        try {
            LocalDate date = LocalDate.parse(columns[3], DATE_FORMATTER);
            return date.equals(LocalDate.now().minusDays(1));
        } catch (Exception e) {
        	
        	try {
        		LocalDate date = LocalDate.parse(columns[3], DATE_FORMATTER_FALLBACK);
                return date.equals(LocalDate.now().minusDays(1));
			} catch (Exception e2) {
				e2.printStackTrace();
	            return false;
			}
        	
        }
    }

    private boolean hasEffectiveDigit3or4(String line) {
        String[] columns = line.split(";");
        if (columns.length < 1) return false;

        String lawsuitNumber = columns[0];
        if (!LAWSUIT_PATTERN.matcher(lawsuitNumber).matches()) return false;

        String firstBlock = lawsuitNumber.split("-")[0];
        int index = firstBlock.length() - 1;
        while (index >= 0 && (firstBlock.charAt(index) == '9' || firstBlock.charAt(index) == '0')) {
            index--;
        }

        char effectiveDigit = (index >= 0) ? firstBlock.charAt(index) : 'n';
        return effectiveDigit == '3' || effectiveDigit == '4';
    }

    private void writeFilteredCsv(String header, List<String> filteredLines) {
        File outputFile = new File(outputFolderPath, "filtered_output.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write(header);
            writer.newLine();
            for (String line : filteredLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package com.example.gid2;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.util.ArrayList;
import java.util.List;

public class StickerPrinter implements Printable {

    private final List<String> companyDetails;
    private static final int ROWS = 8;
    private static final int BOXES_PER_ROW = 3;
    private static final int TOP_MARGIN = 10;
    private static final int LEFT_MARGIN = 10;
    private static final int RIGHT_MARGIN = 10;
    private static final int BOTTOM_MARGIN = 10;
    private static final int HORIZONTAL_PADDING = 2;
    private static final int VERTICAL_PADDING = 2;

    public StickerPrinter(List<String> companyDetails) {
        this.companyDetails = companyDetails;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        int stickersPerPage = ROWS * BOXES_PER_ROW;
        int startIndex = pageIndex * stickersPerPage;

        if (startIndex >= companyDetails.size()) {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        double availableWidth = pageFormat.getImageableWidth() - LEFT_MARGIN - RIGHT_MARGIN;
        double availableHeight = pageFormat.getImageableHeight() - TOP_MARGIN - BOTTOM_MARGIN;

        int boxWidth = (int) (availableWidth / BOXES_PER_ROW) - HORIZONTAL_PADDING;
        int boxHeight = (int) (availableHeight / ROWS) - VERTICAL_PADDING;

        int startX = LEFT_MARGIN;
        int startY = TOP_MARGIN;

        int boxNumber = startIndex;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < BOXES_PER_ROW; col++) {
                if (boxNumber < companyDetails.size()) {
                    int x = startX + col * (boxWidth + HORIZONTAL_PADDING);
                    int y = startY + row * (boxHeight + VERTICAL_PADDING);

                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, boxWidth, boxHeight);

                    String boxText = companyDetails.get(boxNumber);
                    int fontSize = calculateFontSize(g2d, boxText, boxWidth, boxHeight);

                    Font font = new Font("Arial", Font.PLAIN, fontSize);
                    g2d.setFont(font);

                    String[] lines = splitTextToFit(boxText, g2d.getFontMetrics(), boxWidth - 10);

                    int yPosition = y + 15;
                    for (String line : lines) {
                        g2d.drawString(line, x + 5, yPosition);
                        yPosition += g2d.getFontMetrics().getHeight();
                    }

                    boxNumber++;
                } else {
                    break; // No more boxes to draw
                }
            }
        }

        return Printable.PAGE_EXISTS;
    }

    private int calculateFontSize(Graphics2D g2d, String text, int boxWidth, int boxHeight) {
        int fontSize = 10;
        Font font = new Font("Arial", Font.PLAIN, fontSize);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        String[] lines = splitTextToFit(text, fm, boxWidth - 10);

        while (!doesTextFit(lines, fm, boxHeight) && fontSize > 6) {
            fontSize--;
            font = new Font("Arial", Font.PLAIN, fontSize);
            g2d.setFont(font);
            fm = g2d.getFontMetrics();
            lines = splitTextToFit(text, fm, boxWidth - 10);
        }

        return fontSize;
    }

    private boolean doesTextFit(String[] lines, FontMetrics fm, int boxHeight) {
        int totalTextHeight = lines.length * fm.getHeight();
        return totalTextHeight <= boxHeight - 10;
    }

    private String[] splitTextToFit(String text, FontMetrics fm, int maxWidth) {
        String[] lines = text.split("\n");
        List<String> wrappedLines = new ArrayList<>();

        for (String line : lines) {
            if (fm.stringWidth(line) <= maxWidth) {
                wrappedLines.add(line);
            } else {
                StringBuilder currentLine = new StringBuilder();
                String[] words = line.split(" ");
                for (String word : words) {
                    if (fm.stringWidth(currentLine + " " + word) < maxWidth) {
                        currentLine.append(" ").append(word);
                    } else {
                        wrappedLines.add(currentLine.toString().trim());
                        currentLine = new StringBuilder(word);
                    }
                }
                wrappedLines.add(currentLine.toString().trim());
            }
        }

        return wrappedLines.toArray(new String[0]);
    }

    public static void printSticker(List<String> companyDetails) {
        SwingUtilities.invokeLater(() -> {
            StickerPrinter printer = new StickerPrinter(companyDetails);

            JFrame frame = new JFrame();
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setResizable(false);

            // Custom title panel
            JPanel titlePanel = new JPanel();
            JLabel titleLabel = new JLabel("Sticker Preview and Print ( Complete PDF data in 2 minutes. )");
            titleLabel.setForeground(Color.RED); // Set the title text color to red
            titlePanel.add(titleLabel);
            frame.add(titlePanel, BorderLayout.NORTH); // Add title panel to the top

            JPanel previewPanel = new JPanel();
            previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));

            JScrollPane scrollPane = new JScrollPane(previewPanel);
            frame.add(scrollPane, BorderLayout.CENTER);

            JButton printButton = new JButton("Print");
            printButton.setBackground(Color.GREEN);
            printButton.setForeground(Color.WHITE);
            printButton.addActionListener(e -> {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintable(printer);

                // Close the preview window after clicking Print
                // Open print dialog in a separate thread to ensure it's in front
                SwingUtilities.invokeLater(() -> {
                    try {
                        if (job.printDialog()) {
                            job.print();
                        }
                    } catch (PrinterAbortException ex) {
                        System.err.println("Print job was aborted by the user or printer: " + ex.getMessage());
                    } catch (PrinterException ex) {
                        System.err.println("Printer exception: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });

                // Optionally close the preview window
                frame.dispose();
            });

            int stickersPerPage = ROWS * BOXES_PER_ROW;
            int totalPages = (int) Math.ceil((double) companyDetails.size() / stickersPerPage);

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                JPanel pagePanel = new JPanel(new GridLayout(ROWS, BOXES_PER_ROW, HORIZONTAL_PADDING, VERTICAL_PADDING));
                pagePanel.setBorder(BorderFactory.createTitledBorder("Page " + (pageIndex + 1)));

                int startIndex = pageIndex * stickersPerPage;
                int endIndex = Math.min(startIndex + stickersPerPage, companyDetails.size());

                for (int i = startIndex; i < endIndex; i++) {
                    String text = companyDetails.get(i);

                    JPanel box = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            g.setColor(Color.BLACK);
                            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                            Font font = new Font("Arial", Font.PLAIN, 10);
                            g.setFont(font);
                            FontMetrics fm = g.getFontMetrics();

                            String[] lines = printer.splitTextToFit(text, fm, getWidth() - 10);
                            int yPosition = 15;

                            for (String line : lines) {
                                g.drawString(line, 5, yPosition);
                                yPosition += fm.getHeight();
                            }
                        }
                    };
                    box.setPreferredSize(new Dimension(200, 100)); // Approximate size
                    pagePanel.add(box);
                }

                previewPanel.add(pagePanel);
            }

            frame.add(printButton, BorderLayout.SOUTH);
            frame.setLocationRelativeTo(null); // Center the frame
            frame.setVisible(true);
        });
    }


    public static void main(String[] args) {
        // Sample data for testing
        List<String> companyDetails = new ArrayList<>();
        companyDetails.add("DESIGNERS' WORKSHOP PARTH\nTHAKKAR 9426080565 A-1109, The Capital, Science City Road, Amdavad-380060 GUJARAT. BHARAT");

        printSticker(companyDetails);
    }
}

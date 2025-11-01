package com.example.esp32detection.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@Service
public class OCRService {

    private final Tesseract tesseract;

    public OCRService() {
    this.tesseract = new Tesseract();
    tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");
    tesseract.setLanguage("eng");
    tesseract.setPageSegMode(6);
    tesseract.setTessVariable("user_defined_dpi", "300");
}


    public String extractTextFromImage(MultipartFile file) throws IOException, TesseractException {
        Path tempFile = Files.createTempFile("ocr-", file.getOriginalFilename());
        
        try {
            file.transferTo(tempFile.toFile());
            String extractedText = tesseract.doOCR(tempFile.toFile());
            return extractedText;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public String extractIDCardData(MultipartFile file) throws IOException, TesseractException {
        Path tempFile = Files.createTempFile("id-card-", file.getOriginalFilename());
        
        try {
            file.transferTo(tempFile.toFile());
            BufferedImage image = ImageIO.read(tempFile.toFile());
            BufferedImage processedImage = preprocessImage(image);
            String result = tesseract.doOCR(processedImage);
            return result;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private BufferedImage preprocessImage(BufferedImage image) {
        BufferedImage gray = new BufferedImage(
            image.getWidth(), 
            image.getHeight(), 
            BufferedImage.TYPE_BYTE_GRAY
        );
        java.awt.Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return gray;
    }
}

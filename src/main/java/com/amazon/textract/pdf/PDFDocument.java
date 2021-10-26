package com.amazon.textract.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

public class PDFDocument {

    //change the font type here
    final PDFont font = PDType1Font.COURIER_BOLD;

    private PDDocument document;

    public PDFDocument(){
        this.document = new PDDocument();
    }

    /*
    Depending on the input document you can adjust the initial font size and the width and height of the extracted text
     */
    private FontInfo calculateFontSize(String text, float bbWidth, float bbHeight) throws IOException {
        int fontSize = 17;
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;

        if(textWidth > bbWidth){
            while(textWidth > bbWidth){
                fontSize -= 1;
                textWidth = font.getStringWidth(text) / 1000 * fontSize;
                textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
            }
        }
        else if(textWidth < bbWidth){
            while(textWidth < bbWidth){
                fontSize += 1;
                textWidth = font.getStringWidth(text) / 1000 * fontSize;
                textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
            }
        }

        //System.out.println("Text height before returning font size: " + textHeight);

        FontInfo fi = new FontInfo();
        fi.fontSize = fontSize;
        fi.textHeight = textHeight;
        fi.textWidth = textWidth;

        return fi;
    }

    public void addPageWithoutFormatting(BufferedImage image, List<TextLine> lines) throws IOException {

        float width = image.getWidth();
        float height = image.getHeight();

        PDRectangle box = new PDRectangle(width, height);
        PDPage page = new PDPage(box);
        page.setMediaBox(box);
        this.document.addPage(page);

        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false);

        contentStream.setRenderingMode(RenderingMode.STROKE);

        for (TextLine cline : lines){
            String clinetext = cline.text;

            FontInfo fontInfo = calculateFontSize(clinetext, (float)cline.width*width, (float)cline.height*height);
            //config for no images
            contentStream.setStrokingColor(Color.BLACK);
            contentStream.setNonStrokingColor(Color.GRAY);
            contentStream.setRenderingMode(RenderingMode.FILL_STROKE);

            contentStream.addRect((float)cline.left*width, (float) (height-height*cline.top-fontInfo.textHeight), (float)cline.width*width, (float)cline.height*height);

            //change the output text color here
            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.beginText();
            contentStream.setFont(this.font, fontInfo.fontSize);
            contentStream.newLineAtOffset((float)cline.left*width, (float)(height-height*cline.top-fontInfo.textHeight));

            contentStream.showText(clinetext);

            contentStream.endText();
        }

        contentStream.close();
    }

    //code for images
    public void addPageWithFormatting(BufferedImage image, ImageType imageType, List<TextLine> lines) throws IOException {

        float width = image.getWidth();
        float height = image.getHeight();

        PDRectangle box = new PDRectangle(width, height);
        PDPage page = new PDPage(box);
        page.setMediaBox(box);
        this.document.addPage(page);

        PDImageXObject pdImage;

        if(imageType == ImageType.JPEG){
            pdImage = JPEGFactory.createFromImage(this.document, image);
        }
        else {
            pdImage = LosslessFactory.createFromImage(this.document, image);
        }

        PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, false);

        contentStream.drawImage(pdImage, 0, 0);

        contentStream.setRenderingMode(RenderingMode.FILL);

        for (TextLine cline : lines){
            String clinetext = cline.text;

            FontInfo fontInfo = calculateFontSize(clinetext, (float)cline.width*width, (float)cline.height*height);

            //configto include original document structure
            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.addRect((float)cline.left*width, (float) (height-height*cline.top-fontInfo.textHeight), (float)cline.width*width, (float)cline.height*height);

            contentStream.fill();
            //change the output text color here
            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.beginText();
            contentStream.setFont(this.font, fontInfo.fontSize);
            contentStream.newLineAtOffset((float)cline.left*width, (float)(height-height*cline.top-fontInfo.textHeight));
            contentStream.showText(clinetext);
            contentStream.endText();
        }
        contentStream.close();
    }

    public void save(OutputStream os) throws IOException {
        this.document.save(os);
    }

    public void close() throws IOException {
        this.document.close();
    }
}

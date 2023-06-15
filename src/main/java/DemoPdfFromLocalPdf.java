import com.amazon.textract.pdf.ImageType;
import com.amazon.textract.pdf.PDFDocument;
import com.amazon.textract.pdf.TextLine;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.TranslateTextRequest;
import software.amazon.awssdk.services.translate.model.TranslateTextResponse;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

public class DemoPdfFromLocalPdf {
    private static final Logger logger = getLogger(DemoPdfFromLocalPdf.class.getName());

    private List<TextLine> extractTextAndTranslate(ByteBuffer imageBytes, String sourceLanguage, String destinationLanguage) {
        logger.info("Extracting text");
        Region region = Region.US_EAST_1;
        TextractClient textractClient = TextractClient.builder()
                .region(region)
                .build();

        // Get the input Document object as bytes
        Document pdfDoc = Document.builder()
                .bytes(SdkBytes.fromByteBuffer(imageBytes))
                .build();

        TranslateClient translateClient = TranslateClient.builder()
                .region(region)
                .build();

        DetectDocumentTextRequest detectDocumentTextRequest = DetectDocumentTextRequest.builder()
                .document(pdfDoc)
                .build();

        // Invoke the Detect operation
        DetectDocumentTextResponse textResponse = textractClient.detectDocumentText(detectDocumentTextRequest);

        List<Block> blocks = textResponse.blocks();
        List<TextLine> lines = new ArrayList<>();
        BoundingBox boundingBox;

        for (Block block : blocks) {
            if ((block.blockType()).equals(BlockType.LINE)) {
                String source = block.text();

                TranslateTextRequest requestTranslate = TranslateTextRequest.builder()
                        .sourceLanguageCode(sourceLanguage)
                        .targetLanguageCode(destinationLanguage)
                        .text(source)
                        .build();

                TranslateTextResponse resultTranslate = translateClient.translateText(requestTranslate);

                boundingBox = block.geometry().boundingBox();
                lines.add(new TextLine(boundingBox.left(),
                        boundingBox.top(),
                        boundingBox.width(),
                        boundingBox.height(),
                        resultTranslate.translatedText(),
                        source));
            }
        }
        return lines;
    }

    public void run(String documentName, String outputDocumentName, String sourceLanguage, String destinationLanguage, boolean retainFormatting) throws IOException {

        logger.info("Generating searchable pdf from: " + documentName);

        PDFDocument pdfDocument = new PDFDocument();

        List<TextLine> lines;
        BufferedImage image;
        ByteArrayOutputStream byteArrayOutputStream;
        ByteBuffer imageBytes;

        //Load pdf document and process each page as image
        PDDocument inputDocument = PDDocument.load(new File(documentName));
        PDFRenderer pdfRenderer = new PDFRenderer(inputDocument);
        for (int page = 0; page < inputDocument.getNumberOfPages(); ++page) {
            int pageNumber = page + 1;
            logger.info("processing page " + pageNumber);
            //Render image
            image = pdfRenderer.renderImage(page, 1, org.apache.pdfbox.rendering.ImageType.RGB);

            //Get image bytes
            byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIOUtil.writeImage(image, "jpeg", byteArrayOutputStream);
            byteArrayOutputStream.flush();
            InputStream sourceStream = new FileInputStream(documentName);
            imageBytes = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());


            //Extract text
            lines = extractTextAndTranslate(imageBytes, sourceLanguage, destinationLanguage);

            //Add page with text layer and image in the pdf document
            if (retainFormatting)
                pdfDocument.addPageWithFormatting(image, ImageType.JPEG, lines);
                //Add page without text layer and image in the pdf document
            else
                pdfDocument.addPageWithoutFormatting(image, ImageType.JPEG, lines);

            logger.info("Processed page " + pageNumber);
        }

        inputDocument.close();

        //Save PDF to local disk
        try (OutputStream outputStream = new FileOutputStream(outputDocumentName)) {
            pdfDocument.save(outputStream);
            pdfDocument.close();
        }

        logger.info("Generated searchable pdf: " + outputDocumentName);
    }

}

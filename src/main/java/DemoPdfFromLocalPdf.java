import com.amazon.textract.pdf.ImageType;
import com.amazon.textract.pdf.PDFDocument;
import com.amazon.textract.pdf.TextLine;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.*;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClientBuilder;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.logging.log4j.Logger;
import static org.apache.logging.log4j.LogManager.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DemoPdfFromLocalPdf {
    private static final Logger logger = getLogger(DemoPdfFromLocalPdf.class.getName());

    private List<TextLine> extractTextAndTranslate(ByteBuffer imageBytes, String sourceLanguage, String destinationLanguage) {
        AmazonTranslate translateClient = AmazonTranslateClientBuilder.defaultClient();

        logger.info("Extracting text");

        AmazonTextract client = AmazonTextractClientBuilder.defaultClient();

        DetectDocumentTextRequest request = new DetectDocumentTextRequest()
                .withDocument(new Document()
                        .withBytes(imageBytes));

        DetectDocumentTextResult result = client.detectDocumentText(request);

        List<Block> blocks = result.getBlocks();
        List<TextLine> lines = new ArrayList<TextLine>();
        BoundingBox boundingBox;

        for (Block block : blocks) {
            if ((block.getBlockType()).equals("LINE")) {
                String source = block.getText();
                TranslateTextRequest requestTranlate = new TranslateTextRequest()
                        .withText(source)
                        .withSourceLanguageCode(sourceLanguage)
                        .withTargetLanguageCode(destinationLanguage);

                TranslateTextResult resultTranslate = translateClient.translateText(requestTranlate);

                boundingBox = block.getGeometry().getBoundingBox();
                lines.add(new TextLine(boundingBox.getLeft(),
                        boundingBox.getTop(),
                        boundingBox.getWidth(),
                        boundingBox.getHeight(),
                        resultTranslate.getTranslatedText(),
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
        ByteArrayOutputStream byteArrayOutputStream = null;
        ByteBuffer imageBytes = null;

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

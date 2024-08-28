import org.apache.logging.log4j.Logger;
import static org.apache.logging.log4j.LogManager.*;


public class Demo {
    private static final Logger log = getLogger(Demo.class.getName());

    public static void main(String[] args) {
        try {
            if (args.length < 4) {
                log.error("arguments sourceLanguage destinationLanguage expected");
                System.exit(0);
            }
            //Generate translated PDF
            log.info("Starting Translation");
            String sourceLanguage = args[1];
            String destinationLanguage = args[3];

            String externalFontPath = null;
            if (args.length == 6) {
            externalFontPath = args[5];
            }

            DemoPdfFromLocalPdf localPdf = new DemoPdfFromLocalPdf();
            //Generate 2 PDFs; one with formatting and one without
            localPdf.run("./documents/SampleInput.pdf", "./documents/SampleOutput-" + destinationLanguage + ".pdf ", sourceLanguage, destinationLanguage, true, externalFontPath);
            localPdf.run("./documents/SampleInput.pdf", "./documents/SampleOutput-min-" + destinationLanguage + ".pdf ", sourceLanguage, destinationLanguage, false, externalFontPath);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

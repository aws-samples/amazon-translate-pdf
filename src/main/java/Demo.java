public class Demo {
    public static void main(String[] args) {
        try {
            if (args.length != 4) {
                System.out.println("arguments sourceLanguage destinationLanguage expected");
                System.exit(0);
            }
            //Generate translated PDF
            String sourceLanguage = args[1];
            String destinationLanguage = args[3];
            DemoPdfFromLocalPdf localPdf = new DemoPdfFromLocalPdf();
            localPdf.run("./documents/SampleInput.pdf", "./documents/SampleOutput-" + destinationLanguage + ".pdf ",
                    "en", "es", true);
            localPdf.run("./documents/SampleInput.pdf",     "./documents/SampleOutput-min-" + destinationLanguage + ".pdf ",
                    sourceLanguage, destinationLanguage, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

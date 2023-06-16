### Translating PDF Documents with Amazon Textract, Amazon Translate and PDFBox while Retaining the Original PDF Formatting

This repository contains a sample library and code examples showing how Amazon Textract, Amazon Translate can be used to extract and translate text from documents and use PDFBox to create a translated pdf while retaining the original formatting.

#### How is the translated PDF generated

To generate a translated PDF, we use Amazon Textract to extract text from documents and then use Amazon Translate to get the translated text. The extracted translated text is added as a layer to the image in the PDF document.

Amazon Textract detects and analyzes text input documents and returns information about detected items such as pages, words, lines, form data (key-value pairs), tables, selection elements etc. It also provides bounding box information which is an axis-aligned coarse representation of the location of the recognized item on the document page. We use detected text and its bounding box information to appropriately place the translated text in the pdf page.

Amazon Translate is a neural machine translation service that delivers fast, high-quality, and affordable language translation. Amazon Translate provides high quality on-demand and batch translation capabilities across more than 2970 language pairs, while decreasing your translation costs.

SampleInput.pdf is an example input document in English. SampleOutput-es.pdf is the translated pdf document in Spanish with all the formatting of the original document retained.

PDFDocument library wraps all the necessary logic to generate the translated PDF document using output from Amazon Textract and Amazon Translate. It also uses open source Java library Apache PDFBox to create the PDF document but there similar pdf processing libraries available in other programming languages.

    ...

    //Load pdf document and process each page as image
            PDDocument inputDocument = PDDocument.load(new File(documentName));
            PDFRenderer pdfRenderer = new PDFRenderer(inputDocument);
            for (int page = 0; page < inputDocument.getNumberOfPages(); ++page) {
                int pageNumber = page + 1;
                System.out.println("processing page " + pageNumber);
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
                    pdfDocument.addPageWithoutFormatting(image, lines);

#### Code examples

Create translated PDF from pdf on local drive

### Run code examples on local machine

Setup AWS Account and AWS CLI using getting started with Amazon Textract.  
Git clone the sample project (need to add to AWS samples) or Download and unzip PDFTranslate-<version>.zip from the GitHub repo(add link to downloadable zip)
Install Apache Maven if it is not already installed.  
In the project directory run "mvn package".  
Run: "java -jar target/translate-pdf-1.0.jar --source en --translated es" to run the Java project.  
(see for https://aws.amazon.com/textract/ the languages Textract currently supports and for the https://docs.aws.amazon.com/translate/latest/dg/what-is.html#what-is-languages language codes

#### Cost

As you run these samples they call different Amazon Textract and Amazon Translate APIs in your AWS account. You will get charged for all the API calls made as part of the analysis.

#### AWS JAVA SDK V1

This branch includes samples for AWS Java SDK V1

Note: AWS Java SDK V1 has the following vulnerability and therefore it is recommend to use the Java SDK V2
    
CVE-2016-10320  suppress

textract before 1.5.0 allows OS Command Injection attacks via a filename in a call to the process function. This may be a remote attack if a web application accepts names of arbitrary uploaded files.
CWE-78 Improper Neutralization of Special Elements used in an OS Command ('OS Command Injection')

CVSSv2:
Base Score: HIGH (9.3)
Vector: /AV:N/AC:M/Au:N/C:C/I:C/A:C
CVSSv3:
Base Score: HIGH (7.8)
Vector: CVSS:3.0/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:H/A:H

References:
MISC - http://seclists.org/oss-sec/2016/q4/442
Vulnerable Software & Versions:

cpe:2.3:a:textract_project:textract:*:*:*:*:*:*:*:* versions up to (including) 1.4.0

#### Other Resources

[1][large scale document processing with amazon textract - reference architecture] (https://github.com/aws-samples/amazon-textract-serverless-large-scale-document-processing)

[2][generating-searchable-pdfs-from-scanned-documents-automatically-with-amazon-textract] (https://aws.amazon.com/blogs/machine-learning/generating-searchable-pdfs-from-scanned-documents-automatically-with-amazon-textract/)

[3]Amazon Textract code samples (https://github.com/aws-samples/amazon-textract-code-samples)

[4]Amazon Translate code samples (https://github.com/aws-samples/amazon-translate-text-extract-sample)

### License

This library is licensed under the MIT-0 License. See the LICENSE file.

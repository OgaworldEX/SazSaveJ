package burp;

import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.HttpRequestResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.io.IOException;

public class SazMaker implements Runnable{

    //private final MontoyaApi api;

    private final static String indexHeader = "<html><head><style>body,thead,td,a,p{font-family:verdana,sans-serif;font-size: 10px;}</style></head><body><table cols=12><thead><tr><th>&nbsp;</th><th>#</th><th>Result</th><th>Protocol</th><th>Host</th><th>URL</th><th>Body</th><th>Caching</th><th>Content-Type</th><th>Process</th><th>Comments</th><th>Custom</th></tr></thead><tbody>";
    private final static String indexBodybase = "<tr><td><a href='raw\\%s_c.txt'>C</a>&nbsp;<a href='raw\\%s_s.txt'>S</a>&nbsp;<a href='raw\\%s_m.xml'>M</a></td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>body</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>";
    private final static String indexFooter = "</tbody></table></body></html>";
    private static String sazMxml;
    private static String sazContentsTypesxml;

    private final ProgressListener listener;
    private final List<HttpRequestResponse> selectedRequestResponsesList;
    private final String fileName;

    private int progresValue;

    static {
        sazMxml = getJarText("/Saz_m_xml.txt");
        sazContentsTypesxml = getJarText("/SazContentsTypesXml.txt");
    }

    public static String generateRawFileNumber(int number, int maxNumber) {
        int digits = String.valueOf(maxNumber).length();
        return String.format("%0" + digits + "d", number);
    }

    public SazMaker(List<HttpRequestResponse> selectedRequestResponsesList,String fileName,ProgressListener listener) {
        this.listener = listener;
        this.selectedRequestResponsesList = selectedRequestResponsesList;
        this.fileName = fileName;
        this.progresValue = 0;
    }

    public void run(){

        //make tmp
        String tmpDirectory = System.getProperty("java.io.tmpdir");
        Path targetDirectoryPath = null;

        try {
            targetDirectoryPath = Files.createTempDirectory(Paths.get(tmpDirectory), "ogasazsave_");
            //OgaSazSave.logging.logToOutput(targetDirectoryPath.toString());
        } catch (IOException ex) {
            OgaSazSave.logging.logToError(ex.getMessage());
        }

        //make raw
        String rawDirectoryPath = targetDirectoryPath.toString() + "/raw";
        File directory = new File(rawDirectoryPath);

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                //SazSave.logging.logToOutput("directory make ok");
            } else {
                //SazSave.logging.logToOutput("directory make ng");
            }
        } else {
            OgaSazSave.logging.logToError("directory exists");
        }

        //make indexhtml+row
        StringBuilder indexhtm = new StringBuilder();
        indexhtm.append(indexHeader);
        int size = selectedRequestResponsesList.size();
        int i = 0;
        for (HttpRequestResponse requestResponse : selectedRequestResponsesList) {

            String targetRawNumber = generateRawFileNumber(i,size);

            //0_c.txt
            String c_txtFileName = targetRawNumber + "_c.txt";
            byte[] addRequestByteArray = changeRequestLine(requestResponse);
            makeFile(addRequestByteArray, rawDirectoryPath + "/" + c_txtFileName);

            //0_m.xml
            String m_xmlFileName = targetRawNumber + "_m.xml";

            //notes to comment
            String notes = requestResponse.annotations().notes();

            String escapeComment = "";
            if(notes != null ){
                escapeComment = notes.replace("\"", "&quot;");
            }else{
                escapeComment = "Notes could not be retrieved because there is a space at the beginning of the Notes field.";
                OgaSazSave.logging.logToError(targetRawNumber + ": "+ escapeComment);
            }

            String newSazXML = String.format(sazMxml,escapeComment);
            makeFile(newSazXML, rawDirectoryPath + "/" + m_xmlFileName);

            //0_s.txt
            String s_txtFileName = targetRawNumber + "_s.txt";
            if(requestResponse.response() == null){
                makeFile("HTTP/0.0 999 No Response (SazSaveJ)\r\n\r\n".getBytes(), rawDirectoryPath + "/" + s_txtFileName);
            }else{
                makeFile(requestResponse.response().toByteArray().getBytes(), rawDirectoryPath + "/" + s_txtFileName);
            }

            //indexhtm
            String number = String.valueOf(i);
            String caching = getResponseHeaderValue(requestResponse, "Cache-Control");
            String result = String.valueOf(requestResponse.statusCode());
            String protocol = requestResponse.httpService().secure() ? "HTTPS" : "HTTP";
            String host = getRequestHeaderValue(requestResponse, "Host");
            String path = requestResponse.request().path();
            String contentType = getResponseHeaderValue(requestResponse, "Content-Type");
            String comments = ""; //requestResponse.annotations().notes();
            String process = "";
            String custom = "";

            //_index.htm
            indexhtm.append(String.format(indexBodybase,
                    targetRawNumber, targetRawNumber, targetRawNumber, number, result, protocol, host, path, caching, contentType, process, comments, custom));

            i++;

            if (listener != null) {
                progresValue = (int)(((i) / (double)(size)) * (50) + 0);
                listener.onProgress(progresValue);
            }
        }
        indexhtm.append(indexFooter);
        makeFile(indexhtm.toString(), targetDirectoryPath + "/_index.htm");

        //[[Content_Types].xml
        makeFile(sazContentsTypesxml, targetDirectoryPath + "/[Content_Types].xml");

        SazZipMaker.exec(targetDirectoryPath.toString(), OgaSazSave.sazSavePath + fileName + ".saz");

        //delete tmpdelectory
        deleteFolder(targetDirectoryPath.toFile());
    }

    private byte[] changeRequestLine(HttpRequestResponse requestResponse) {

        // base byte array
        byte[] originalRequestbyteArray = requestResponse.request().toByteArray().getBytes();

        //protocol + host
        StringBuilder addSb = new StringBuilder();
        addSb.append(requestResponse.httpService().secure() ? "https://" : "http://");
        addSb.append(requestResponse.httpService().host());
        byte[] addProtocolHostByteArray = addSb.toString().getBytes();

        //space position
        byte targetValue = 32;  //0x20
        int insertIndex = findByteIndex(originalRequestbyteArray, targetValue) + 1;

        ///header byte array
        byte[] headArray = new byte[insertIndex];
        System.arraycopy(originalRequestbyteArray, 0, headArray, 0, headArray.length);

        ///hooter byte array
        byte[] footerArray = new byte[originalRequestbyteArray.length - insertIndex];
        System.arraycopy(originalRequestbyteArray, insertIndex, footerArray, 0, footerArray.length);

        //new request byte array
        byte[] newArray = new byte[headArray.length + addProtocolHostByteArray.length + footerArray.length];

        //array copy
        System.arraycopy(headArray, 0, newArray, 0, headArray.length);
        System.arraycopy(addProtocolHostByteArray, 0, newArray, headArray.length, addProtocolHostByteArray.length);
        System.arraycopy(footerArray, 0, newArray, headArray.length + addProtocolHostByteArray.length, footerArray.length);

        return newArray;
    }

    private String getRequestHeaderValue(HttpRequestResponse requestResponse, String name) {
        return getHeaderValue(requestResponse.request().headers(), name);
    }

    private String getResponseHeaderValue(HttpRequestResponse requestResponse, String name) {
        if(requestResponse.response() == null){
            return "";
        }
        return getHeaderValue(requestResponse.response().headers(), name);
    }

    private String getHeaderValue(List<HttpHeader> argHeaders, String name) {
        List<HttpHeader> headers =
                argHeaders
                        .stream()
                        .filter(httpHeader -> httpHeader.name().equals(name)).toList();

        String retValue = "";
        if (!headers.isEmpty()) {
            retValue = headers.get(0).value();
        }
        return retValue;
    }

    private static int findByteIndex(byte[] byteArray, byte targetValue) {
        for (int i = 0; i < byteArray.length; i++) {
            if (byteArray[i] == targetValue) {
                return i;
            }
        }
        return -1;
    }

    private static void makeFile(byte[] byteArray, String distFilePath) {
        try {
            try (FileOutputStream fos = new FileOutputStream(distFilePath)) {
                fos.write(byteArray);
            }
        } catch (IOException e) {
            OgaSazSave.logging.logToError(e.getMessage());
        }
    }

    private static void makeFile(String contents, String distFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(distFilePath))) {
            writer.write(contents);
        } catch (IOException e) {
            OgaSazSave.logging.logToError(e.getMessage());
        }
    }

    private static String getJarText(String fileName) {
        byte[] txtByteArray = getJarResource(fileName);
        return new String(txtByteArray, StandardCharsets.US_ASCII);
    }

    private static byte[] getJarResource(String fileName) {

        byte[] retValue = null;
        try {
            InputStream inputStream = SazMaker.class.getResourceAsStream(fileName);

            if (inputStream != null) {
                retValue = inputStream.readAllBytes();
            }
        } catch (IOException e) {
            OgaSazSave.logging.logToError(e.getMessage());
        }
        return retValue;
    }

    private void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                int i = 0;
                int size = files.length;
                for (File file : files) {
                    if (listener != null) {
                        progresValue = (int)(((i) / (double)(size)) * (100) + 50);
                        listener.onProgress(progresValue);
                    }
                    i++;
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }
}
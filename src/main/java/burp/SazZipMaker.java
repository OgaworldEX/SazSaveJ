package burp;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SazZipMaker {

    public static void exec(String sourceFolderPath, String zipFilePath) {

        //SazSave.logging.logToOutput("sourceFolderPath:" + sourceFolderPath);
        //SazSave.logging.logToOutput("zipFilePath:" + zipFilePath);

        try {
            zipFolder(new File(sourceFolderPath), new File(zipFilePath));
        } catch (IOException e) {
            OgaSazSave.logging.logToError(e.getMessage());
        }
    }
    public static void zipFolder(File sourceFolder, File zipFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFolder(sourceFolder, "", zipOutputStream);
        }
    }

    private static void zipFolder(File sourceFolder, String parentPath, ZipOutputStream zipOutputStream) throws IOException {
        File[] files = sourceFolder.listFiles();
        byte[] buffer = new byte[1024];

        for (File file : files) {
            String entryName = parentPath + file.getName();
            //SazSave.logging.logToOutput("entryName:" + entryName);

            if (file.isDirectory()) {
                zipFolder(file, entryName + "/", zipOutputStream);
            } else {
                ZipEntry zipEntry = new ZipEntry(entryName);
                if (file.length() > 0) {
                    zipOutputStream.putNextEntry(zipEntry);
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            zipOutputStream.write(buffer, 0, length);
                        }
                    }
                    zipOutputStream.closeEntry();
                }
            }
        }
    }
}

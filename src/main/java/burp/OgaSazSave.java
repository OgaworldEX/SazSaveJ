package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;

import java.io.*;
import java.util.Properties;

public class OgaSazSave implements BurpExtension
{
    public final static String extensionName = "SazSaveJ";

    public final static String version = "1.4.1";

    public final static String SAVE_PATH_KEY = "SAVE_PATH_KEY";

    public final static String PROPERTIES_NAME = "OgaSazSaveConfig.properties";

    public final static String DEFAULT_SAVE_PATH = "c:\\tmp\\";

    public static Logging logging ;

    public static String sazSavePath;

    public static Properties properties;

    @Override
    public void initialize(MontoyaApi api)
    {
        api.extension().setName(extensionName);
        logging =  api.logging();

        //context menu
        api.userInterface().registerContextMenuItemsProvider(new SazSaveMenuItemsProvider(api));

        //load date
        properties = new Properties();
        try (InputStream input = new FileInputStream(OgaSazSave.PROPERTIES_NAME)) {
            properties.load(input);
            sazSavePath = properties.getProperty(SAVE_PATH_KEY);
        } catch (IOException ex) {
            logging.logToOutput("No Configuration file: " + ex.getMessage());

            sazSavePath = DEFAULT_SAVE_PATH;

            try (OutputStream output = new FileOutputStream(OgaSazSave.PROPERTIES_NAME)) {
                properties.setProperty(SAVE_PATH_KEY, DEFAULT_SAVE_PATH);
                properties.store(output, null);
                logging.logToOutput("-Created Configuration file-");
            } catch (IOException io) {
                logging.logToError(io.getMessage());
            }
        }

        //OgaSazSave.logging.logToOutput("loadSavePath:" + loadSavePath);
        OgaSazSave.logging.logToOutput("SazSaveJ " + version + " Load ok");
        OgaSazSave.logging.logToOutput("Save Path: " + sazSavePath);
    }

}

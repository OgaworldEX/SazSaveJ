package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SazSaveMenuItemsProvider implements ContextMenuItemsProvider
{
    private final MontoyaApi api;

    public SazSaveMenuItemsProvider(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {

        if (event.isFromTool(ToolType.PROXY, ToolType.REPEATER,ToolType.TARGET, ToolType.LOGGER)) {
            List<Component> menuItemList = new ArrayList<>();

            //saz save
            JMenuItem sazSaveMenuItem = new JMenuItem("Save as Saz");
            sazSaveMenuItem.addActionListener(e -> saveSelected(event.selectedRequestResponses()));
            menuItemList.add(sazSaveMenuItem);

            //config
            JMenuItem configMenuItem = new JMenuItem("Config");
            configMenuItem.addActionListener(e -> showConfig());
            menuItemList.add(configMenuItem);

            return menuItemList;
        }
        return null;
    }

    private void saveSelected(List<HttpRequestResponse> selectedRequestResponsesList) {

        if(selectedRequestResponsesList.size() < 0 ){
            return;
        }

        SazMaker sazMaker = new SazMaker(this.api);
        sazMaker.makeSaz(selectedRequestResponsesList);
    }

    private void showConfig() {
        //OgaSazSave.logging.logToOutput("call: showConfig()");

        String inputValue = null;
        while (true) {

            inputValue = JOptionPane.showInputDialog(
                api.userInterface().swingUtils().suiteFrame(),
                "Enter a new save path for the saz file",
                OgaSazSave.sazSavePath);

            if (inputValue == null){
                //OgaSazSave.logging.logToOutput("cancel");
                return;
            }else {
                //OgaSazSave.logging.logToOutput("InputPath: " + inputValue);

                // input check
                Path path = Paths.get(inputValue);
                if (Files.exists(path) == false) {
                    JOptionPane.showMessageDialog(api.userInterface().swingUtils().suiteFrame(),
                            "The path does not exist",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                }

                if (Files.isRegularFile(path)) {
                    JOptionPane.showMessageDialog(api.userInterface().swingUtils().suiteFrame(),
                            "The path is a file.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                }

                if (Files.isDirectory(path)) {
                    break;
                }
            }
        }

        if (!inputValue.endsWith(File.separator)) {
            inputValue += File.separator;
        }
        OgaSazSave.sazSavePath = inputValue;

        // save
        try (OutputStream output = new FileOutputStream(OgaSazSave.PROPERTIES_NAME)) {
            OgaSazSave.properties.setProperty(OgaSazSave.SAVE_PATH_KEY, OgaSazSave.sazSavePath);
            OgaSazSave.properties.store(output, null);
        } catch (FileNotFoundException e) {
            OgaSazSave.logging.logToError("Configuration file not found." + e.getMessage());
        } catch (IOException e) {
            OgaSazSave.logging.logToError("Configuration IO Exception." + e.getMessage());
        }

        OgaSazSave.logging.logToOutput("New SazSavePath: " + inputValue);
    }
}


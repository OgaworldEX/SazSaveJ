package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SazSaveMenuItemsProvider implements ContextMenuItemsProvider
{
    private final MontoyaApi api;

    public SazSaveMenuItemsProvider(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {

        List<Component> menuItemList = new ArrayList<>();

        //Save MenuItem
        JMenuItem sazSaveInputFileNameMenuItem = new JMenuItem("Save as Saz (Input FileName)");
        JMenuItem sazSaveTimeStampMenuItem = new JMenuItem("Save as Saz (Timestamp FileName)");

        if(! event.selectedRequestResponses().isEmpty()){
            sazSaveTimeStampMenuItem.addActionListener(e -> saveSelected(event.selectedRequestResponses()));
            sazSaveInputFileNameMenuItem.addActionListener(e -> saveSelectedInputFileName(event.selectedRequestResponses()));

        } else if(event.messageEditorRequestResponse().isPresent()){
            List<HttpRequestResponse> reqresList = new ArrayList<>();
            reqresList.add(event.messageEditorRequestResponse().get().requestResponse());

            sazSaveTimeStampMenuItem.addActionListener(e -> saveSelected(reqresList));
            sazSaveInputFileNameMenuItem.addActionListener(e -> saveSelectedInputFileName(reqresList));
        }else {
            return null;
        }

        menuItemList.add(sazSaveInputFileNameMenuItem);
        menuItemList.add(sazSaveTimeStampMenuItem);

        //config MenuItem
        JMenuItem configMenuItem = new JMenuItem("Setting Save Path");
        configMenuItem.addActionListener(e -> showDirectoryChooseDialog());
        menuItemList.add(new JPopupMenu.Separator());
        menuItemList.add(configMenuItem);

        return menuItemList;
    }

    private void saveSelected(List<HttpRequestResponse> selectedRequestResponsesList) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String formattedDateTime = now.format(formatter);

        SazMaker sazMaker = new SazMaker(this.api);
        sazMaker.makeSaz(selectedRequestResponsesList,formattedDateTime);
    }

    private void saveSelectedInputFileName(List<HttpRequestResponse> selectedRequestResponsesList) {

        String saveFileName;

        saveFileName = JOptionPane.showInputDialog(
                api.userInterface().swingUtils().suiteFrame(),
                "Enter a saz file name\n"+OgaSazSave.sazSavePath,
                "");

        //invalid characters in the file name
        String invalidCharsRegex = "[\\\\/:*?\"<>|]";

        Pattern pattern = Pattern.compile(invalidCharsRegex);
        Matcher matcher = pattern.matcher(saveFileName);

        if (matcher.find()) {
            JOptionPane.showMessageDialog(
                    api.userInterface().swingUtils().suiteFrame(),
                    "invalid characters in the file name\n"+matcher.group(),
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Path check
        Path checkSavePath = Paths.get(OgaSazSave.sazSavePath + saveFileName + ".saz");

        if (Files.exists(checkSavePath)) {
            JOptionPane.showMessageDialog(
                    api.userInterface().swingUtils().suiteFrame(),
                    "Sazfile already exists.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Files.isDirectory(checkSavePath)) {
            JOptionPane.showMessageDialog(
                    api.userInterface().swingUtils().suiteFrame(),
                    "The path is a Directory.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SazMaker sazMaker = new SazMaker(this.api);
        sazMaker.makeSaz(selectedRequestResponsesList,saveFileName);
    }

    private void showDirectoryChooseDialog(){
        //OgaSazSave.logging.logToOutput("call showDirectoryChooseDialog()");

        File currentfile = new File(OgaSazSave.sazSavePath);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(currentfile);

        int selected = fileChooser.showOpenDialog(api.userInterface().swingUtils().suiteFrame());

        if (selected == JFileChooser.APPROVE_OPTION){
            OgaSazSave.logging.logToOutput(fileChooser.getSelectedFile().getPath());
        }else if (selected == JFileChooser.CANCEL_OPTION){
            return;
        }else if (selected == JFileChooser.ERROR_OPTION){
            return;
        }else{
            return;
        }
        OgaSazSave.sazSavePath = fileChooser.getSelectedFile().getPath() + FileSystems.getDefault().getSeparator();

        //save
        try (OutputStream output = new FileOutputStream(OgaSazSave.PROPERTIES_NAME)) {
            OgaSazSave.properties.setProperty(OgaSazSave.SAVE_PATH_KEY, OgaSazSave.sazSavePath);
            OgaSazSave.properties.store(output, null);
        } catch (FileNotFoundException e) {
            OgaSazSave.logging.logToError("Configuration file not found." + e.getMessage());
        } catch (IOException e) {
            OgaSazSave.logging.logToError("Configuration IO Exception." + e.getMessage());
        }

        //message
        JOptionPane.showMessageDialog(
                api.userInterface().swingUtils().suiteFrame(),
                "[New Saz Save Path]\n" + OgaSazSave.sazSavePath,
                "SazSaveJ", JOptionPane.INFORMATION_MESSAGE);
    }
}

/*
    Uriel Caracuel Barrera - 2º DAM

 */
package com.vzplayer;

import java.io.*;
import java.util.*;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaRef;

public class FileManager {
    final String VALID_FORMATS = ".mp4,.avi,.mp3,.mkv";

    // Static resources
    static final InputStream icon = Objects.requireNonNull(
            FileManager.class.getResourceAsStream("icon.png"));

    
    MediaControl mc;

    VZVideo video;

    ArrayList<VZMedia> files = new ArrayList<>();
    Stage stage;

    IntegerProperty listIndex = new SimpleIntegerProperty();
    IntegerProperty listSize = new SimpleIntegerProperty(0);
    BooleanProperty listChanged = new SimpleBooleanProperty(false);
    BooleanProperty shuffle = new SimpleBooleanProperty(false);
    
    boolean saveSources = true;
    
    final String SETTINGS_PATH = "settings.json";
    
    public FileManager(Stage stage){
        this.stage = stage;
        makeVideo();
        makePlaylist();
    }
    
    public void makeVideo(){
        video = new VZVideo(this);
        listSize.addListener(cl -> {
            System.out.println("Size changed! Refreshing list view...");
            Platform.runLater(() -> {
                video.setListView();
            });
        });

    }

    public VZVideo getVideo(){
        return video;
    }
    
    public JsonObject loadSettingsFile(){
        File f = new File(SETTINGS_PATH);
        try (FileReader reader = new FileReader(SETTINGS_PATH)){
            if (!checkFile(f)){
                boolean newFile = f.createNewFile();
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement jse = JsonParser.parseReader(reader);
            return gson.fromJson(jse,JsonObject.class);

        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }
    
    public void saveSettingsFile(JsonObject settings){
        File f = new File(SETTINGS_PATH);
        if (!checkFile(f)) {
            try {
                boolean fileCreated = f.createNewFile();
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            }
        } else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = settings.toString();
            JsonElement jse = JsonParser.parseString(jsonString);
            String json = gson.toJson(jse);
            writeToFile(json,SETTINGS_PATH);
        }
    }
    
    public void setMediaControl(MediaControl mc){
        this.mc = mc;

    }

    public void refreshPlaylist(){
        video.setListView();
    }
    
    public void loadSources(){
        Gson gson = new Gson();
        String[] sources = gson.fromJson(mc.settings.get("sources"),String[].class);

        if (sources == null){
            mc.settings.add("sources",new JsonArray());
            saveSettingsFile(mc.settings);
        } else {
            for (String source : sources){
                open(source);
            }
        }
    }
    
    public void saveSource(String sourcePath) {
        ArrayList<String> sources = new ArrayList<>();
        Gson gson = new Gson();
        if (saveSources) {
            boolean exists = false;
            String[] sourceArray = gson.fromJson(mc.settings.get("sources"), String[].class);
            sources.addAll(List.of(sourceArray));

            for (Object source : sources) {
                if (source.equals(sourcePath)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                sources.add(sourcePath);
                saveSettingsFile(mc.settings);
            }
        } else {
            mc.settings.add("sources", gson.toJsonTree(sources,ArrayList.class));
            saveSettingsFile(mc.settings);
        }

    }
    
    public void makePlaylist(){

    }
    
    public int getListSize(){
        return files.size();
    }
    
    public boolean hasMedia(){
        return getListSize() > 0;
    }
   
    public void open(String path){
        mc.stop();
        File source;
        String sourcePath;
        path = path.replace('\\', '/');
        sourcePath = path;
        saveSource(sourcePath);
        source = new File(sourcePath);
        if (checkFile(source)){

            if (source.isDirectory()){
                // La fuente es un directorio
                File[] filesInSource = source.listFiles();
                if (filesInSource != null && addAlltoMediaList(filesInSource) != 0){
                    files.add(new VZMedia(this,source.getName(),source.getAbsolutePath()));
                }
            } else {
                files.add(new VZMedia(this,source.getName(),source.getAbsolutePath()));
            }
        }
        // filter();
        listSize.set(files.size());
        // System.out.println(listSize + " files added to playlist");
    }

    public MediaPlayerFactory getFactory(){
        return mc.getFactory();
    }
    
    public void delete(int index){
        files.remove(index);
        // Update size
        listSize.set(files.size());
    }

    public VZMedia getMedia(int index){
        // System.out.println((String)files.keySet().toArray()[mc.listIndex.get()]);
        return files.get(index);
    }
    
    public void deleteAll(){
        files.clear();
        listSize.set(0);
    }
    
    public void openFile(){
        FileChooser fc = new FileChooser();
        File selectedFile = fc.showOpenDialog(stage);
        open(selectedFile.getAbsolutePath());
    }
    
    public void openFolder(){
        DirectoryChooser cc = new DirectoryChooser();
        File selectedFolder = cc.showDialog(stage);
        open(selectedFolder.getAbsolutePath());
    }

    private int addAlltoMediaList(File[] source){
        // Toma los ficheros de un directorio
        String log = "";
        for (File source1 : source) {
            if (source1.isDirectory()) {
                File[] onSource = source1.listFiles();
                if (onSource != null){
                    addAlltoMediaList(onSource);
                }
            } else {
                VZMedia media = new VZMedia(this,source1.getName(),source1.getAbsolutePath());
                if (VALID_FORMATS.contains(media.getFormat())){
                    files.add(new VZMedia(this,source1.getName(),source1.getAbsolutePath()));
                }

            }

        }
        return 0;
    }

    public ArrayList<VZMedia> getFiles(){
        return files;
    }
    
    private static boolean checkFile(File f){
        // El fichero no existe
        if (!f.exists()){
            System.err.println("Error. La ruta '" + f.getAbsolutePath() +  "' no existe.");
            return false;
        }
        // El fichero no tiene permisos de lectura
        /* 
        if (!f.canRead()){
            System.err.println("Error. La ruta '" + f.getAbsolutePath() +  "' no tiene permisos de lectura.");
            return false;
        }
        */
        return true;
    }

    private void writeToFile(String content, String path){
        File f = new File(path);
        try (FileWriter writer = new FileWriter(f)){
            if (!checkFile(f)){
                boolean newFile = f.createNewFile();
            }
            writer.write(content);
            writer.flush();

        } catch (IOException ex){
            System.err.println(ex.getMessage());
        }
    }
    
    
    
}

/*
    Uriel Caracuel Barrera - 2º DAM

 */
package com.vzplayer;

import java.io.*;
import java.util.*;

import com.google.gson.*;
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
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaRef;

public class FileManager {
    final String VALID_FORMATS = ".mp4,.avi,.mp3,.mkv";

    // Static resources
    static final InputStream icon = Objects.requireNonNull(
            FileManager.class.getResourceAsStream("icon.png"));

    
    MediaControl mc;

    Playlist pl;

    VZVideo video;

    Map<String,MediaRef> filePaths = new LinkedHashMap<>();
    ObservableMap<String, MediaRef> files = FXCollections.observableMap(filePaths);
    Stage stage;

    IntegerProperty listIndex = new SimpleIntegerProperty();
    IntegerProperty listSize = new SimpleIntegerProperty(0);
    BooleanProperty listChanged = new SimpleBooleanProperty(false);
    BooleanProperty shuffle = new SimpleBooleanProperty(false);
    
    boolean saveSources = true;
    
    final String SETTINGS_PATH = "settings.json";
    
    public FileManager(Stage stage){
        this.stage = stage;
    }
    
    public VZVideo makeVideo(){
        video = new VZVideo(this);
        listSize.addListener(cl -> {
            System.out.println("Size changed! Refreshing list view...");
            refreshPlaylist();
        });
        return video;
        
    }
    
    public JsonObject loadSettingsFile(){
        File f = new File(SETTINGS_PATH);
        if (!checkFile(f)){
            try {
                boolean newFile = f.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try (FileReader reader = new FileReader(SETTINGS_PATH)){
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonElement jse = JsonParser.parseReader(reader);
                return gson.fromJson(jse,JsonObject.class);
                
            } catch (IOException e){}
            
        }
        return null;
    }
    
    public void saveSettingsFile(JsonObject settings){
        File f = new File(SETTINGS_PATH);
        if (!checkFile(f)) {
            try {
                boolean fileCreated = f.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = settings.toString();
            JsonElement jse = JsonParser.parseString(jsonString);
            String json = gson.toJson(jse);
            try (FileWriter writer = new FileWriter(SETTINGS_PATH)){
                writer.write(json);
                writer.flush();
                
            } catch (IOException e){
                System.err.println(e.getMessage());
            }
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
        pl = new Playlist(mc,this);
    }

    public MediaRef getRef(){
        return files.get(getFile());
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
                    Media media = mc.getFactory().media().newMedia(source.getAbsolutePath());
                    files.put(source.getName(),media.newMediaRef());
                }
            } else {
                Media media = mc.getFactory().media().newMedia(source.getAbsolutePath());
                files.put(source.getName(),media.newMediaRef());
            }
        }
        filter();
        listSize.set(files.size());
        // System.out.println(listSize + " files added to playlist");
    }
    
    public void delete(int index){
        files.remove(getFileByIndex(index));
    }

    public String getFile(){
        // System.out.println((String)files.keySet().toArray()[mc.listIndex.get()]);
        return (String)files.keySet().toArray()[mc.listIndex.get()];
    }

    public String getFileByIndex(int index){
        System.out.println((String)files.keySet().toArray()[index]);
        return (String)files.keySet().toArray()[index];
    }
    
    public void deleteAll(){
        files.clear();
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
        String log = "";
        for (File source1 : source) {
            if (source1.isDirectory()) {
                File[] onSource = source1.listFiles();
                if (onSource != null){
                    addAlltoMediaList(onSource);
                }
            } else {
                Media media = mc.getFactory().media().newMedia(source1.getAbsolutePath());
                files.put(source1.getName(),media.newMediaRef());

            }

        }
        return 0;
    }

    public ArrayList<String> getKeyList(){
        return new ArrayList<>(files.keySet());
    }
    
    private void filter(){
        ArrayList<String> keys = getKeyList();
        String format;
        String log = "";
        for (String key : keys){
            String path = files.get(key).newMedia().info().mrl();
            format = path.substring(path.length()-4);

            // mp4, avi y mkv, lo demás fuera
            if (!VALID_FORMATS.contains(format)){
                files.remove(key);
            }
        }
        /*
        File f = new File("openlog.txt");
        try (FileWriter writer = new FileWriter(f)){
            if (!checkFile(f)){
                boolean newFile = f.createNewFile();
            }
            writer.write(log);
            writer.flush();


        } catch (IOException ex){}
         */
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
    
    
    
}

/*
    Uriel Caracuel Barrera - 2º DAM

 */
package com.vzplayer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import uk.co.caprica.vlcj.medialist.MediaList;
import uk.co.caprica.vlcj.medialist.MediaListRef;

public class FileManager {
    
    MediaControl mc;
    MediaList mediaList;
    MediaListRef listRef;

    Playlist pl;
    ObservableList<String> fileNames = FXCollections.observableArrayList();
    Stage stage;

    IntegerProperty listSize = new SimpleIntegerProperty(0);
    BooleanProperty listChanged = new SimpleBooleanProperty(false);
    BooleanProperty shuffle = new SimpleBooleanProperty(false);
    
    boolean saveSources = true;
    
    final String SETTINGS_PATH = "settings.json";
    
    public FileManager(Stage stage){
        this.stage = stage;
    }
    
    public VZVideo makeVideo(){
        return new VZVideo(this);
        
    }
    
    public JSONObject loadSettingsFile(){
        File f = new File(SETTINGS_PATH);
        if (!checkFile(f)){
            try {
                boolean newFile = f.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try (FileReader reader = new FileReader(SETTINGS_PATH)){
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(reader);
                return (JSONObject) obj;
                
            } catch (IOException | ParseException e){}
            
        }
        return null;
    }
    
    public void saveSettingsFile(JSONObject settings){
        File f = new File(SETTINGS_PATH);
        if (!checkFile(f)) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonString = settings.toJSONString();
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
    
    public void loadSources(){
        JSONArray sources = (JSONArray) mc.settings.get("sources");
        if (sources == null){
            mc.settings.put("sources", new JSONArray());
            saveSettingsFile(mc.settings);
        } else {
            Iterator<String> iterator = sources.iterator();
            while (iterator.hasNext()) {
                open(iterator.next());
            }
        }
    }
    
    public void saveSource(String sourcePath){
        if (saveSources){
            boolean exists = false;
            JSONArray sources = (JSONArray) mc.settings.get("sources");
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
            mc.settings.put("sources",new JSONArray());
            saveSettingsFile(mc.settings);
        }
        
    }
    
    public void makePlaylist(){
        pl = new Playlist(mc,this);
    }
    
    public MediaListRef getMediaListRef(){
        return listRef;
    }
    
    public void setMediaList(MediaList ml){
        mediaList = ml;
    }
    
    public String getFileName(int index){
        return fileNames.get(index);
    }
    
    public int getListSize(){
        return fileNames.size();
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
                File[] filesInSource = source.listFiles();
                if (addAlltoMediaList(filesInSource) != 0){
                    mediaList.media().add(source.getAbsolutePath());
                    fileNames.add(source.getName());
                }
            } else {
                mediaList.media().add(source.getAbsolutePath());
                fileNames.add(source.getName());
            }
            filter();
            listSize.set(fileNames.size());
            listRef = mediaList.media().newMediaListRef();
        }
        // System.out.println(listSize + " files added to playlist");
    
    }
    
    public void delete(int index){
        fileNames.remove(index);
        mediaList.media().remove(index);
        System.out.println("media list size: " + mediaList.media().mrls().size());
        listRef = mediaList.media().newMediaListRef();
        
    }
    
    public void deleteAll(){
        fileNames.clear();
        mediaList.media().clear();
        listRef = mediaList.media().newMediaListRef();
        
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
        for (File source1 : source) {
            if (source1.isDirectory()) {
                File[] onSource = source1.listFiles();
                addAlltoMediaList(onSource);
            } else {
                mediaList.media().add(source1.getAbsolutePath());
                fileNames.add(source1.getName());
            }
            
        }
        return 0;
    }
    
    private void filter(){
        ArrayList<Integer> toDelete = new ArrayList<>();
        List<String> ml = mediaList.media().mrls();
        String format;
        for (int i = 0; i < ml.size(); i++){
            String path = ml.get(i);
            format = path.substring(path.length()-4);
            // System.out.println("Formato: " + format);
            
            // mp4, avi y mkv, todo lo demás fuera
            if (!mc.VALID_FORMATS.contains(format)){
                toDelete.add((Integer)i);
            } 
        }
        Collections.sort(toDelete);
        Collections.reverse(toDelete);

        for (Integer integer : toDelete) {
            mediaList.media().remove(integer);
            fileNames.remove(integer.intValue());

        }
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

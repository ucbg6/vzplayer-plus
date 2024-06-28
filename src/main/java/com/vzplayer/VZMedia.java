package com.vzplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uk.co.caprica.vlcj.media.MediaRef;

public class VZMedia{
    private final String name;
    private final String path;
    private transient final MediaRef ref;
    private long start = 0;

    public VZMedia(String filePath, MediaRef mediaRef){
        path = filePath;
        name = path.substring(path.lastIndexOf("\\") + 1);
        ref = mediaRef;
    }

    public void setStart(long startAt){
        start = startAt;
    }

    public long getStart() {
        return start;
    }

    public String getName(){
        return name;
    }

    public String getPath(){
        return path;
    }

    public MediaRef getRef(){
        return ref;
    }

    public String getFormat(){
        String path = getPath();
        return path.substring(path.length()-4);
    }

    @Override
    public String toString() {
        return name;
    }

    public String toJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public String tooltip(){
        return name + "\nPath: " + path;
    }


}

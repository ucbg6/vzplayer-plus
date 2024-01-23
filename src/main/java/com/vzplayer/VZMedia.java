package com.vzplayer;

import uk.co.caprica.vlcj.media.MediaRef;

import java.io.File;

public class VZMedia {

    private FileManager fm;
    private MediaControl mc;

    private final String name;
    private final String path;
    private final MediaRef ref;

    public VZMedia(FileManager fm, String fileName, String filePath){
        name = fileName;
        path = filePath;
        ref = fm.getFactory().media().newMediaRef(filePath);
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
        return path.substring(path.length()-4);
    }

    @Override
    public String toString() {
        return name;
    }
}

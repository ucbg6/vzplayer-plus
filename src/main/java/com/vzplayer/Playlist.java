
package com.vzplayer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class Playlist {
    IntegerProperty listSize = new SimpleIntegerProperty(0);
    BooleanProperty shuffle = new SimpleBooleanProperty(false);
    
    MediaControl mc;
    FileManager fm;
    
    IntegerProperty listIndex = new SimpleIntegerProperty(0);
    
    public Playlist(MediaControl mediac, FileManager filem){
        mc = mediac;
        fm = filem;
        listSize.bind(fm.listSize);    
        mc.listIndex.bind(listIndex);
        shuffle.bind(mc.isShuffle);
        initialize();
    }
    
    private void initialize(){
        mc.setPlaylist(this);
    }
    
    public void play(){
        mc.play();
    }
    
    public void shift(boolean isNext){
        if (shuffle.get()){
            nextShuffle();
        } else {
            if (isNext){
                next();   
            } else {
                prev();
            }
        }
    }
    
    public void stop(){
        listIndex.set(0);
        mc.stop();
    }
    
    public void prev(){
        int index = listIndex.get();
        if (index > 0){
            index--;
        } else {
            index = fm.listSize.get()-1;
        }
        listIndex.set(index);
        mc.play();
    }
    
    public void next(){
        int index = listIndex.get();
        if (index < fm.listSize.get()-1){
            index++;
        } else {
            index = 0;
        }
        listIndex.set(index);
        mc.play();
    }
    
    public void nextShuffle(){
        listIndex.set((int) (Math.random() * fm.listSize.get()));
        mc.play();
    }
}

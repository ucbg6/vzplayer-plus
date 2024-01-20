/*
    Uriel Caracuel Barrera - 2º DAM

 */
package com.vzplayer;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import static uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurfaceFactory.videoSurfaceForImageView;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.medialist.MediaList;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.list.MediaListPlayer;

public class MediaControl {
    // Caracteres especiales (botones)
    final String SHUFFLE = "\uD83D\uDD00";
    final String REPEAT = "\uD83D\uDD01";
    final String LOOP = "\uD83D\uDD02";
    final String SETTINGS = "\uD83D\uDEE0";
    final String SOUND0 = "\uD83D\uDD08";
    final String SOUND1 = "\uD83D\uDD09";
    final String SOUND2 = "\uD83D\uDD0A";
    final String AUDIO = "A" + "\u25B6";
    
    // Formatos válidos
    final String VALID_FORMATS = ".mp4,.avi,.mp3,.mkv";

    FileManager fm;
    
    // Interfaz
    VZVideo video;
    Stage stage;
    
    // MediaPlayerFactory and Player
    private final MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory();
    private final EmbeddedMediaPlayer mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
    MediaListPlayer listPlayer;

    // Ruta del fichero y título de la ventana
    StringProperty sourceName = new SimpleStringProperty("VZPlayer");
    
    // Lista de reproducción

    MediaList mediaList;
    Playlist pl;
    
    // Guardar configuración (.json)
    JSONObject settings;
    
    // Propiedades
    IntegerProperty listIndex = new SimpleIntegerProperty(0);           // Índice de lista
    LongProperty currentLength = new SimpleLongProperty(0);             // Duración actual
    BooleanProperty isShuffle = new SimpleBooleanProperty(true);        // Modo aleatorio
    BooleanProperty isSpecialMode = new SimpleBooleanProperty(true);    // Modo especial
    BooleanProperty isSkipNext = new SimpleBooleanProperty(false);      // Modo automático
    BooleanProperty isLoop = new SimpleBooleanProperty(false);          // Bucle
    BooleanProperty isRepeat = new SimpleBooleanProperty(false);        // Repetir lista al finalizar
    BooleanProperty isMute = new SimpleBooleanProperty(false);

    // Gestor de canales y modo canal
    
    // Controles del teclado
    private EventHandler<KeyEvent> keyControls;
 
    // Variables de control
    
    boolean showPauseScreen = true;
    boolean hidden = false;

    // Configuración modo automático
    final ScheduledExecutorService service = Executors.newScheduledThreadPool(5);
    Runnable myTask;
    ScheduledFuture futureTask;
    long minTime = 250;
    long maxTime = 15000;
    long time = 3000;
    
    int subIndex = 0;
    
    // Barra de tiempo
    String timeValue = "";
    
    public MediaControl(){
        initialize();
    }
    
    public MediaControl(VZVideo video, FileManager fm){
        this.video = video;
        this.fm = fm;
        this.fm.setMediaList(mediaPlayerFactory.media().newMediaList());
        video.setOnKeyPressed(keyControls);
        initialize();
    }
    
    private void loadSettings(){
        settings = fm.loadSettingsFile();
        if (settings == null){
            settings = new JSONObject();
            saveSettings();
        } else {
            initProperties();
        }
        
        
        
    }
    
    public void saveSettings(){
        if (settings == null){
            System.out.println("But there were no settings.");
        }
        settings.put("is_shuffle",isShuffle.get());
        settings.put("is_special_mode",isSpecialMode.get());
        settings.put("is_skip_next",isSkipNext.get());
        fm.saveSettingsFile(settings);
    }
    
    private void initialize(){
        // SetImageView
        mediaList = mediaPlayerFactory.media().newMediaList();
        mediaPlayer.videoSurface().set(videoSurfaceForImageView(video.getViewer()));
        
        setKeyboard();

        listIndex.addListener(cl -> {
            play();
        });
        
        videoEvents();
        
        
        mediaPlayerEvents();
        propertyListeners();
        loadSettings();

    }
    
    public void videoEvents(){
        video.titleLabel.textProperty().bind(sourceName);

        video.timeSlider.setOnMouseDragged(me -> {
            video.controlActive = true;
            mediaPlayer.controls().setPosition((float) video.timeSlider.getValue() / 100);
            video.timeBar.setProgress((float) video.timeSlider.getValue() / 100);
            timeValue = timeFormat((long) (mediaPlayer.status().length() * video.timeBar.getProgress() / 1000)) + " / " + timeFormat(mediaPlayer.status().length() / 1000);
            video.timeLabel.setText(timeValue);
        });

        video.timeSlider.setOnMousePressed(me -> {
            video.controlActive = true;
            mediaPlayer.controls().setPosition((float) video.timeSlider.getValue() / 100);
            video.timeBar.setProgress((float) video.timeSlider.getValue() / 100);
            timeValue = timeFormat((long) (mediaPlayer.status().length() * video.timeBar.getProgress() / 1000)) + " / " + timeFormat(mediaPlayer.status().length() / 1000);
            video.timeLabel.setText(timeValue);
        });

        video.timeSlider.setOnMouseReleased(me -> {
            video.requestFocus();
        });

        video.special.setOnMouseClicked(me -> {
            isSpecialMode.set(!isSpecialMode.get());
            video.requestFocus();
        });

        video.skip.setOnMouseClicked(me -> {
            isSkipNext.set(!isSkipNext.get());
            video.requestFocus();
        });
        
        video.nextButton.setOnMouseClicked(me -> {
            pl.shift(true);
        });

        video.shuffle.setText(SHUFFLE);
        video.repeat.setText(REPEAT);
        video.audtrack.setText(AUDIO);
        video.volume.setText(SOUND2);
        video.audtrack.setText(AUDIO);

        video.setOnMouseClicked(e -> {
            video.requestFocus();
            if (e.isStillSincePress() && !hidden) {
                playPause(true);
            }
        });
    }
    
    public void setPlaylist(Playlist playlist){
        pl = playlist;
    }
    
    public void mediaPlayerEvents(){
        this.mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                video.viewScreen.setEffect(null);
                video.pauseScreen.setVisible(false);
                video.showVideo(true);
                if (isSkipNext.get()){
                    video.skipImage.setImage(new Image(getClass().getResourceAsStream("skip_on.png")));
                    video.skipLabel.setTextFill(Color.LIMEGREEN);
                }
                
                
                Platform.runLater(() -> {
                    sourceName.set(fm.getFileName(listIndex.get()));
                    setAudioMode(getFormat(new File(fm.getFileName(listIndex.get()))).equals(".mp3"));
                });

                
            }

            @Override
            public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {
                
                
                
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                if (showPauseScreen){
                    video.viewScreen.setEffect(new GaussianBlur(160.0));
                    video.pauseScreen.setVisible(true);
                }
                if (isSkipNext.get()){
                    video.skipImage.setImage(new Image(getClass().getResourceAsStream("skip_hold.png")));
                    video.skipLabel.setTextFill(Color.YELLOW);
                }
                
            }
            
            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                
                Platform.runLater(() -> {
                    currentLength.set(mediaPlayer.status().length());
                });
                
            }


            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    video.showVideo(false);
                    sourceName.set("VZPlayer");
                    video.timeLabel.setText(timeFormat(0) + " / " + timeFormat(0));
                    video.timeBar.setProgress(1);
                    video.timeSlider.setValue(100);
                    status("Stopped.");
                });
            }
            
            @Override
            public void error(MediaPlayer mediaPlayer){
                video.vzsplash.setImage(new Image(getClass().getResourceAsStream("errorsplash.png")));
                System.out.println("Oops, an error just happened");
                System.out.println(fm.mediaList.media().mrl(listIndex.get()));
                mediaPlayer.submit(() -> {
                    pl.shift(true);
                    // fm.delete(listIndex.get());
                });
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                setTimeBar(newTime);
                
            }
            
            
            @Override
            public void finished(MediaPlayer mediaPlayer){
                mediaPlayer.submit(() -> pl.shift(true));
            }
        });
    }
    
    public void propertyListeners(){
        // Modo aleatorio
        isShuffle.addListener((obv,oval,nval) -> {
            saveSettings();
            if (nval){
                status("Shuffle ON");
            } else {
                status("Shuffle OFF");
            }
        });
        
        // Modo especial
        isSpecialMode.addListener((obv, oval, nval) -> {
            saveSettings();
            if (nval) {
                video.spLabel.setTextFill(Color.CYAN);
                video.starImage.setImage(new Image(getClass().getResourceAsStream("star_on.png")));
                status("Special mode ON");
            } else {
                video.spLabel.setTextFill(Color.DARKGREY);
                video.starImage.setImage(new Image(MediaControl.class.getResourceAsStream("star_off.png")));
                status("Special mode OFF");
            }
        });
        
        // Modo automático
        isSkipNext.addListener((obv,oval,nval) ->{
            saveSettings();
            if (nval) {
                video.skipImage.setImage(new Image(getClass().getResourceAsStream("skip_on.png")));
                video.skipLabel.setTextFill(Color.LIMEGREEN);
                
                status("Auto mode ON - [between " + minTime + "-" + maxTime + "]");
            } else {
                video.skipLabel.setTextFill(Color.DARKGREY);
                video.skipImage.setImage(new Image(getClass().getResourceAsStream("skip_off.png")));
                status("Auto mode OFF");
            }
        });
        
        
    }

    public void initProperties(){
        isShuffle.set((boolean)settings.get("is_shuffle"));
        isSpecialMode.set((boolean)settings.get("is_special_mode"));
        isSkipNext.set((boolean)settings.get("is_skip_next"));
    }
    
    public void setAudioMode(boolean val){
        if (val){
            video.showVideo(false);
            video.vzsplash.setImage(
                    new Image(Objects.requireNonNull(MediaControl.class.getResourceAsStream("audiosplash.png"))));
        } else {
            video.vzsplash.setImage(
                    new Image(Objects.requireNonNull(MediaControl.class.getResourceAsStream("icon.png"))));
        }
    }
    
    public void status(String msg){
        video.sendStatus(msg);
        
    }
    
    public boolean isPlaying(){
        return mediaPlayer.status().isPlaying();
    }
    
    public boolean isStopped(){
        return mediaPlayer.status().state().equals(State.STOPPED);
    }
    
    public void stop(){
        mediaPlayer.controls().stop();
    }
    
    public String timeFormat(long t){
        t /= 1000;
        String f = "%02d:%02d:%02d";
        String st = "";
        int h, m, s;
        s = (int) t % 60;
        m = (int) (t / 60) % 60;
        h = (int) t / 3600;
        
        st = String.format(f,h,m,s);
        
        return st;
    }
    
    public void setKeyboard(){
        keyControls = (KeyEvent t) -> {
            switch (t.getCode()){
                case RIGHT:{
                    // >> 5 sec.
                    if (t.isControlDown() && isSkipNext.get()){
                        maxTime += 50;
                        status("MaxTime: " + maxTime + " ms");
                        
                    } else {
                        if (t.isShiftDown() && isSkipNext.get()){
                            if (minTime < maxTime-50){
                                minTime += 50;  
                            }
                            status("MinTime: " + minTime + " ms");
                        } else {
                            mediaPlayer.controls().skipTime(5000);
                            status(timeFormat(mediaPlayer.status().time()) + " / " + timeFormat(mediaPlayer.status().length()));
                        }
                        
                    }
                    break;
                }
                case LEFT:{
                    // << 5 sec.
                    if (t.isControlDown() && isSkipNext.get()){
                        if (maxTime > minTime-50){
                            maxTime -= 50;
                        }
                        status("MaxTime: " + maxTime + " ms");
                    } else {
                        if (t.isShiftDown() && isSkipNext.get()){
                            if (minTime > 150){
                                minTime -= 50;
                            }
                            status("MinTime: " + minTime + " ms");
                        } else {
                            mediaPlayer.controls().skipTime(-5000);
                            status(timeFormat(mediaPlayer.status().time()) + " / " + timeFormat(mediaPlayer.status().length()));
                        }
                        
                    }
                    
                    break;
                }
                case F:{
                    // Pantalla completa
                    if (!video.isFullscreen) {
                        video.setFullScreen(true);
                        video.setAlwaysOnTop(true);
                    } else {
                        video.setFullScreen(false);
                        video.setAlwaysOnTop(false);
                    }
                        
                    
                    
                    break;
                }
                case N:{
                    // Siguiente
                    pl.shift(true);
                    break;
                }
                case P:{
                    // Anterior
                    pl.shift(false);
                    break;
                }
                case SPACE:{
                    // Pausa/Play
                    if (!hidden){
                        playPause(true);
                    }
                    
                    break;
                }
                case S:{
                    if (t.isControlDown()){
                        stop();
                    } else {
                        isSpecialMode.set(!isSpecialMode.get());
                        
                    }
                    break;
                    
                }
                case D:{
                    if (t.isControlDown()){
                        String filed = fm.getFileName(listIndex.get());
                        stop();
                        fm.delete(listIndex.get());
                        status("Removed from list: " + filed + ". new list size = " + fm.getListSize());
                        if (fm.hasMedia()) {
                            getMedia();
                            shift(true);
                        } else {
                            stop();
                            status("List empty. Stopped.");
                        }
                    }
                    if (t.isAltDown()){
                        stop();
                        status("List empty. Stopped."); 
                        fm.deleteAll();
                    }
                    
                    break;
                }
                case G: {
                    boolean ctrlHidden = !video.timePane.isVisible();
                    if (!ctrlHidden){
                        video.timePane.setVisible(false);
                        status("Recording mode ON");
                    } else {
                        video.timePane.setVisible(true);
                        status("Recording mode OFF");
                    }
                    break;
                }
                /*
                case H: {
                    if (!hidden){
                        video.showVideo(false);
                        if (isPlaying()){
                            playPause(false);
                        }
                        video.pauseScreen.setVisible(false);
                    } else {
                        video.showVideo(false);
                        if (!isPlaying()){
                            playPause(false);
                        }
                    }
                    hidden = !hidden;
                    
                    break;
                } */
                
                case R: {
                    isShuffle.set(!isShuffle.get());
                    break;
                }
                
                case A:{
                    isSkipNext.set(!isSkipNext.get());
                    break;
                }
                case B:{
                    randomStart();
                    break;
                }
                          
                case M:{
                    if (!isMute.get()){
                        muteUnmute(0);
                        status("Muted.");
                    } else {
                        muteUnmute(1);
                        status("Volume: " + mediaPlayer.audio().volume() + " %");
                    }     
                    break;
                }
                case C:{
                    if (t.isControlDown()) {
                        mediaPlayer.subpictures().setTrack(-1);
                        status("Captions OFF");
                        break;
                    }
                    if (t.isAltDown()){
                        setAudioTrack();
                        break;
                    }
                    
                    setSubtitles();
                    break;
                }
                
                
                case L:{
                    /* 
                    video.setFullScreen(false);
                    isMinimized = false;
                    if (!isMini){
                        isMini = true;
                        stage.setHeight(144);
                        
                    } else {
                        isMini = false;
                        stage.setHeight(360);
                    }
                    stage.setWidth(stage.getHeight()*16/9);
                    */
                    
                    break;
                }
                
                case Q:{
                    if (t.isControlDown()){
                        System.exit(0);
                        
                    }
                }
                case O:{
                    if (!video.alwaysOnTop){
                        video.setAlwaysOnTop(true);
                        status("Window set on top");
                    } else {
                        video.setAlwaysOnTop(false);
                        status("Window no longer on top");
                    }
                    break;
                }
                
                case T:{
                    status(timeFormat(mediaPlayer.status().time()) + " / " + timeFormat(mediaPlayer.status().length()));
                    break;
                }
                
                case PLUS:{
                    float rate = mediaPlayer.status().rate();
                    if (rate < 3.95){
                        mediaPlayer.controls().setRate((float)(rate + 0.05));
                        status("Rate set to " + String.format("%.02f",mediaPlayer.status().rate()));
                    }
                    break;
                }
                case MINUS:{
                    if (t.isControlDown()){
                        mediaPlayer.controls().setRate(1);
                    } else {
                        float rate = mediaPlayer.status().rate();
                        if (rate > 0.3){
                            mediaPlayer.controls().setRate((float)(rate-0.05));
                        }
                    }
                    status("Rate set to " + String.format("%.02f",mediaPlayer.status().rate()));
                    break;
                    
                }
                
                case PLAY:
                case PERIOD:{
                    playPause(false);
                    mediaPlayer.controls().nextFrame();
                    break;
                }
                
                case UP:{
                    int volume = mediaPlayer.audio().volume();
                    if (volume < 250){
                        volume += 5;
                    }
                    mediaPlayer.audio().setVolume(volume);
                    status("Volume: " + volume + " %");
                    break;  
                }
                
                case DOWN:{
                    int volume = mediaPlayer.audio().volume();
                    if (t.isControlDown()){
                        volume = 100;
                    } else {
                        volume -= 5;
                    }
                    mediaPlayer.audio().setVolume(volume);
                    status("Volume: " + volume + " %");
                    break;
                }
                
                default:{
                    char c = 'a';
                    try {
                        c = t.getText().charAt(0);
                    } catch (StringIndexOutOfBoundsException e) {
                    }
                    // System.out.println(t.getText());
                    if (Character.isDigit(c)) {
                        mediaPlayer.controls().setPosition((float)(0.1 * Float.parseFloat(Character.toString(c))));
                    }
                    break;
                    
                }
            }
            video.requestFocus();
        };
    }
    
    public EventHandler<KeyEvent> getKeyboard(){
        return keyControls;
    }
    
    public void setAudioTrack(){
        int count = mediaPlayer.audio().trackCount();
        if (count > 0){
            List<TrackDescription> apus = mediaPlayer.audio().trackDescriptions();
            int track = mediaPlayer.audio().track();
            
            int pos = 0;
            for (TrackDescription apu : apus){
                if (apu.id() == track){
                    break;
                }
                pos++;
            }
            if (pos + 1 >= count){
                pos = 0;
            } else {
                pos++;
            }
            
            track = apus.get(pos).id();
            mediaPlayer.audio().setTrack(track);
            status("Audio Track: (" + apus.get(pos).id() + ") " + apus.get(pos).description());
        }
        
    }
    
    public void setSubtitles(){
        int count = mediaPlayer.subpictures().trackCount();
        System.out.println("Track count: " + count);
        if (count > 0){
            List<TrackDescription> spus = mediaPlayer.subpictures().trackDescriptions();
            int track = mediaPlayer.subpictures().track();
            
            int pos = 0;
            for (TrackDescription spu : spus){
                if (spu.id() == track){
                    break;
                }
                pos++;
            }
            if (pos + 1 >= count){
                pos = 0;
            } else {
                pos++;
            }
            
            track = spus.get(pos).id();
            mediaPlayer.subpictures().setTrack(track);
            status("Captions: (" + spus.get(pos).id() + ") " + spus.get(pos).description());
        }
        
        /* 
        
        track++;
        if (track >= spus.size()) {
            track = -1;
            status("Captions: Disabled.");
        } else {
            
        }
        
        
        */
       
    }

    public void getMedia(){
        // Reproductor de lista
        listPlayer = mediaPlayerFactory.mediaPlayers().newMediaListPlayer();
        // Asignación de lista y reproductor
        listPlayer.list().setMediaList(fm.getMediaListRef());
        listPlayer.mediaPlayer().setMediaPlayer(mediaPlayer);

        // Canal por defecto
        pl.play();
        status("Done.");

        myTask = new Runnable() {
            @Override
            public void run() {
                if (isSkipNext.get() && isPlaying()) {
                    pl.shift(true);
                    changeTimeInterval(); 
                }

            }
        };
        if (futureTask == null){
            futureTask = service.scheduleAtFixedRate(myTask, time, time, TimeUnit.MILLISECONDS);
        }
        
        
        
    }
    
    public void changeTimeInterval(){
        if (futureTask != null){
            futureTask.cancel(true);
            time = (long) (minTime + (maxTime * Math.random()));
            System.out.println("new time: " + time);
            futureTask = service.scheduleAtFixedRate(myTask,time,time,TimeUnit.MILLISECONDS);
        }
        
    }
    
    public void setTimeBar(long newTime){
        video.timeBar.setProgress(mediaPlayer.status().position());
        video.timeSlider.setValue(video.timeBar.getProgress() * 100);
        timeValue = timeFormat(mediaPlayer.status().time()) + " / " + timeFormat(mediaPlayer.status().length());
        
        Platform.runLater(() -> {
            video.timeLabel.setText(timeValue);
        });
    }
    
    public void play(){
        if (fm.hasMedia()){
            if (isShuffle.get()){
                pl.listIndex.set((int)(fm.listSize.get() * Math.random()));
            }
            
            listPlayer.controls().play(listIndex.get());
            if (isSpecialMode.get()) randomStart();
            setTimeBar(mediaPlayer.status().time());
            
        }
        
    }
    
    public void randomStart(){
        Random rand = new Random();
        mediaPlayer.controls().setPosition(rand.nextFloat());
    }
    
    public void playPause(boolean show){
        showPauseScreen = show;
        if (mediaPlayer.status().isPlaying()){
            listPlayer.controls().pause();
        } else {
            if (fm.hasMedia()){
                if (isStopped()){
                    getMedia();
                } else {
                    listPlayer.controls().play();
                }
                
            }
            
        }
        if (isSkipNext.get()){
            if (isPlaying()){
                video.skipImage.setImage(new Image(getClass().getResourceAsStream("skip_hold.png")));
                video.skipLabel.setTextFill(Color.YELLOW);
            } else {
                video.skipImage.setImage(new Image(getClass().getResourceAsStream("skip_on.png")));
                video.skipLabel.setTextFill(Color.LIMEGREEN);
            }
        }
        
    }
    
    public void shift(boolean val){
        if (isShuffle.get()){
            nextRandom();
        } else {
            if (val) {
                next();
            } else {
                prev();
            }
        }
        
        
    }
    
    public void next(){
        /*
        if (listIndex.get() < listSize-1){
            listIndex++;
        } else {
            listIndex = 0;
        }
        
        play(listIndex);
*/
    }
    
    public void prev(){
        /*
        if (listIndex > 0){
            listIndex--;
        } else {
            listIndex = fm.getListSize()-1;
        }
        
        play(listIndex);
        */
    }
    
    public void nextRandom(){
        /* 
        Random rand = new Random();
        listIndex = (int)(rand.nextDouble() * fm.getListSize()-1);
        listPlayer.controls().play(listIndex);
        */
    }
    
    public void setStage(Stage st){
        stage = st;
    }
    
    public boolean isPlaylist(){
        if (fm.hasMedia()){
            return false;
        }
        return true;
    }
    
    public void muteUnmute(int m){
        if (m == 0){
            isMute.set(true);
            mediaPlayer.audio().setMute(true);
        } else {
            isMute.set(false);
            mediaPlayer.audio().setMute(false);
        }
    }
    
    public static String getFormat(File f){
        String path = f.getAbsolutePath();
        String format = path.substring(path.length()-4);
        return format;
    }

}

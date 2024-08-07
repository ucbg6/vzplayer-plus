/*
    Uriel Caracuel Barrera - 2º DAM

 */
package com.vzplayer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import com.jfoenix.controls.JFXSlider;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;


public class VZVideo extends StackPane{
    FileManager fm;                     // Gestor de archivos
    MediaControl mc;                    // Control de reproducción
    @FXML ImageView viewer, background, vzsplash, starImage, skipImage, showMenuImg;
    @FXML StackPane pauseScreen, viewScreen;
    @FXML Polygon pauseButton;
    // @FXML ProgressBar timeBar;
    @FXML Label titleLabel, durationLabel, timeLabel, statusLabel, spLabel, skipLabel;
    @FXML BorderPane controlPane, viewerPane, statusPane;
    @FXML HBox upBar, upMenu, listPane;
    @FXML Button showMenu, quit, special, skip, shuffle, repeat, audtrack, captions, settings, volume, 
            showPlaylist, nextButton;
    @FXML
    JFXSlider timeSlider;
    @FXML MenuButton open;
    @FXML MenuItem openFile, openFolder;
    @FXML VBox upPane, timePane;
    @FXML ListView<VZMedia> playListView;
    
    
    EventHandler<KeyEvent> keyboard;
    // StringProperty title = new SimpleStringProperty();
    Stage myStage;
    FadeTransition hideStatus;
    
    boolean isFullscreen = false;
    boolean controlActive = true;
    boolean isMinimized = false;
    boolean isMini = false;
    boolean alwaysOnTop = false;
    boolean showList = false;
    
    double fixedListSize = 300;
    
    // @FXML Label audio;
    
    /*
    TODO: 
    - Playlist at right controlPane
    - Visual settings as another tab in right controlPane
    
    */
    
    public VZVideo(FileManager filem){
        fm = filem;
        initialize();
        screenAdjust(); 
        // initMedia();
    }
    
    public void makeControl(){
        mc = new MediaControl(this, fm); 
        keyboard = mc.getKeyboard();
        fm.setMediaControl(mc);
    }
    
    /* public void rgbBar(){
        Timer timer = new Timer();
        TimerTask tk = new TimerTask(){
            @Override
            public void run() {
                ColorAdjust ca = (ColorAdjust) timeBar.getEffect();
                double hue = ca.getHue();
                if (hue >= 1){
                    hue = -1;
                } else {
                    hue += 0.05;
                }
                ca.setHue(hue);
                timeBar.setEffect(ca);
                // viewerPane.setEffect(ca);
            } 
        };
        timer.scheduleAtFixedRate(tk, 0, 50);
    } */
    
    public EventHandler<KeyEvent> getKeyboard(){
        return keyboard;
    } 
    
    public StringProperty getSourceName(){
        return mc.sourceName;
    }
    
    public void setStage(Stage stage){
        myStage = stage;
        mc.setStage(stage);
    }
    
    public boolean isPlaylist(){
        return mc.isPlaylist();
    }
    
    public void load(){
        sendStatus("Loading...");
        mc.getMedia();
        
    }
    
    public void sendStatus(String msg){
        if (myStage != null){
            hideStatus.stop();
            statusLabel.setOpacity(1);
            statusLabel.setText("> " + msg);

            hideStatus.play();
        }
        
        
    }
   
    private void videoEvents(){
         // Barras de control
        FadeTransition ft = new FadeTransition(Duration.millis(500), controlPane);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setCycleCount(1);

        PauseTransition idle = new PauseTransition(Duration.seconds(2));
        idle.setOnFinished(e -> {
            if (!controlActive) {
                setCursor(Cursor.NONE);
                ft.play();
            }

        });
        /*
        loading.progressProperty().addListener((obv,oval,nval) -> {
        });
        */
        setOnMouseMoved(me -> {
            controlPane.setOpacity(1);
            setCursor(Cursor.DEFAULT);
            ft.stop();
            if (!isMinimized) {
                idle.playFromStart();
            }

        });
        
        playListView.setOnMouseEntered(me -> {
            controlActive = true;
        });
        
        playListView.setOnMouseExited(me -> {
            controlActive = false;
        });
        
        upPane.setOnMouseEntered(me -> {
            controlActive = true;
            FadeTransition ftu = new FadeTransition(Duration.millis(500), upPane);
            ftu.setFromValue(0);
            ftu.setToValue(1);
            ftu.setCycleCount(1);
            // upPane.setOpacity(1);
        });

        upPane.setOnMouseExited(me -> {
            controlActive = false;
            FadeTransition ftu = new FadeTransition(Duration.millis(500), upPane);
            ftu.setFromValue(1);
            ftu.setToValue(0);
            ftu.setCycleCount(1);
        });

        timeSlider.setOnMouseEntered(me -> {
            controlActive = true;
            // timeBar.setPrefHeight(10);
            timeSlider.setVisible(true);
        });

        // timeSlider.setIndicatorPosition(null);

        timeSlider.setValueFactory(new Callback<JFXSlider, StringBinding>() {
            @Override
            public StringBinding call(JFXSlider arg0) {
                return Bindings.createStringBinding(new java.util.concurrent.Callable<String>(){
                    @Override
                    public String call() throws Exception {
                        return mc.timeFormat(mc.getTime());
                    }
                }, timeSlider.valueProperty());
            }
        });

        timeSlider.setOnMouseExited(me -> {
            controlActive = false;
            // timeBar.setPrefHeight(7);
            // timeSlider.setVisible(false);
        });
        
        quit.setOnMouseEntered(e -> {
            quit.getStyleClass().add("quithover");
        });
        
        quit.setOnMouseExited(e -> {
            quit.getStyleClass().remove("quithover");
        });
        
        openFile.setOnAction(e -> {
            fm.openFile();
            requestFocus();
            load();
        });
        
        openFolder.setOnAction(e-> {
            fm.openFolder();
            requestFocus();
            load();
        });
        
        showMenu.setOnMouseClicked(me ->{
            if (upMenu.isVisible()){
                showMenuImg.setRotate(0);
                upMenu.setVisible(false);
            } else {
                showMenuImg.setRotate(180);
                upMenu.setVisible(true);
            }
        });
        
        special.setOnMouseEntered(me -> {
            starImage.setImage(new Image(VZVideo.class.getResourceAsStream("star_hover.png")));
        });
        
        skip.setOnMouseEntered(me -> {
            skipImage.setImage(new Image(VZVideo.class.getResourceAsStream("skip_hover.png")));
        });
        
        skip.setOnMouseExited(me -> {
            if (mc.isSkipNext.get()){
                skipImage.setImage(new Image(VZVideo.class.getResourceAsStream("skip_on.png")));
            } else {
                skipImage.setImage(new Image(VZVideo.class.getResourceAsStream("skip_off.png")));
            }
            
        });
        
        special.setOnMouseExited(me -> {
            if (mc.isSpecialMode.get()){
                starImage.setImage(new Image(VZVideo.class.getResourceAsStream("star_on.png")));
            } else {
                starImage.setImage(new Image(VZVideo.class.getResourceAsStream("star_off.png")));
            }
        });
        
        showPlaylist.setOnMouseClicked(me -> {
            if (showList){
                playListView.setPrefWidth(0);
                playListView.setVisible(false);
                showPlaylist.setText("<");
                showList = false;
            } else {
                playListView.setPrefWidth(fixedListSize);
                playListView.setVisible(true);
                showPlaylist.setText(">");
                showList = true;
            }
        });
        
        hideStatus = new FadeTransition(Duration.millis(500),statusLabel);
        hideStatus.setFromValue(1);
        hideStatus.setToValue(0);
        hideStatus.setCycleCount(1);
        hideStatus.setDelay(Duration.seconds(3));
    }
    
    public void setListView(){
        ObservableList<VZMedia> items = FXCollections.observableArrayList(fm.getFiles());
        playListView.setItems(items);
        
        playListView.setOnMouseClicked(me -> {
            if (me.getButton() == MouseButton.MIDDLE){
                playListView.getSelectionModel().select(mc.listIndex.get());
                playListView.getFocusModel().focus(mc.listIndex.get());
                playListView.scrollTo(Math.max(mc.listIndex.get() - 5, 0));
            } else {
                mc.listIndex.set(playListView.getSelectionModel().getSelectedIndex());
                mc.play();
            }

        });

        playListView.setCellFactory(cell -> new ListCell<>() {
            final Tooltip tooltip = new Tooltip();

            @Override
            protected void updateItem(VZMedia media, boolean b) {
                super.updateItem(media, b);
                if (media == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(media.toString());
                    tooltip.setText(media.tooltip());
                    setTooltip(tooltip);
                }
            }
        });
    }
    
    private void initialize() {
        // Carga de la vista FXML
        FXMLLoader fxmlLoader = new FXMLLoader(
                getClass().getResource("VZVideoView.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        
        makeControl();
        videoEvents();
        // setListView();
        // rgbBar();
    }
    
    private void screenAdjust(){
        // El tamaño de MediaView es el tamaño del componente
        DoubleProperty width = viewer.fitWidthProperty();
        DoubleProperty height = viewer.fitHeightProperty();
        width.bind(prefWidthProperty());
        height.bind(prefHeightProperty());
        
        widthProperty().addListener((observableValue, oldValue, newValue) -> {
            // mc.setPrefWidth((double) newValue);
            // timeBar.setPrefWidth((double)newValue);
            titleLabel.setPrefWidth((double)newValue);
            background.setFitWidth((double)newValue);

            // If you need to know about resizes
        });

        heightProperty().addListener((observableValue, oldValue, newValue) -> {
            background.setFitHeight((double)newValue);
            // If you need to know about resizes
            // mc.setPrefHeight((double) newValue);
        });
        
        // La relación de aspecto se mantiene
        viewer.setPreserveRatio(true);
        
        // viewer.setVisible(true);
    }
    
    public void initMedia(){
        mc.isSpecialMode.set(mc.isSpecialMode.get());
        mc.isSkipNext.set(mc.isSkipNext.get());
        mc.isShuffle.set(mc.isShuffle.get());
    }

    public ImageView getViewer(){
        return viewer;
    }
    
    public void showVideo(boolean val){
        viewScreen.setVisible(val);
    }

    public void setFullScreen(boolean value){
        isFullscreen = value;
        if (myStage != null){
            myStage.setFullScreen(value);
            myStage.setAlwaysOnTop(value);
            // myStage.setMaximized(value);
            

        } else {
            System.out.println("There's nothing here...");
        }        
    }
    
    public void setMinimize(boolean value){
        isMinimized = value;
        
        
        if (isMinimized){
            setFullScreen(false);
            int nheight = 30;
            myStage.setHeight(nheight);
            viewerPane.setVisible(false);
            
        } else {
            myStage.setHeight(myStage.getWidth() / 16 * 9);
            viewerPane.setVisible(true);
        }
        
        
    }
    
    public void setAlwaysOnTop(boolean value){
        if (myStage != null){
            myStage.setAlwaysOnTop(value);
        alwaysOnTop = value;
        } else {
            System.out.println("There's nothing here...");
        } 
    }
    
    public void setStage(){
        myStage = (Stage) getScene().getWindow();
    }
    
    public void setTitle(String title){
        if (myStage != null){
            myStage.setTitle(title);
        }
    }

    public MediaControl getMediaControl() {
        return mc;
    }
}

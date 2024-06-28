package com.vzplayer;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

// Aplicación principal
public class VZPlayer extends Application{
    static String[] cliargs;            // Argumentos en linea de comandos 
    FileManager fm;                     // Gestor de archivos
    VZVideo video;                      // Componente principal VZVideo
    Scene scene;                        // Ventana principal
    EventHandler<KeyEvent> keyboard;    // Gestor de eventos del teclado. Obtenido de MediaControl

    Thread fxThread;
    
    // Desplazamiento de la ventana
    private double xOffset = 0;
    private double yOffset = 0;
    
    // Título de la ventana
    StringProperty windowTitle;

    @Override
    public void start(Stage stage) throws Exception {
        // Carga el gestor de archivos
        fm = new FileManager(stage);
        fm.loadSources();

        /*
            VZPlayer
                FileManager
                    VZVideo
                        MediaControl
                    Playlist


         */

        // Carga el video
        video = fm.getVideo();
        // video.initMedia();

        scene = new Scene(video);
        
        video.setFocusTraversable(true);
        
        keyboard = video.getKeyboard();
        scene.setRoot(video);
        scene.setOnKeyPressed(keyboard);
        video.setStage(stage);
        video.setVisible(true);
        video.getMediaControl().muteUnmute(1);
        
        scene.setFill(Color.TRANSPARENT);
        
        stage.setScene(scene);
        
        // Título de la ventana
        windowTitle = video.getSourceName();
        stage.setTitle(windowTitle.get());

        // Configuraciones
        setScreen();
        dragHandlers(stage);
        windowHandlers(stage);
        
        stage.setFullScreenExitHint("");
        // stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(true);
        // Platform.setImplicitExit(false);

        stage.getIcons().add(new Image(FileManager.icon));
        
        // Muestra la aplicación
        stage.show();
        // video.setFullScreen(true);
        video.sendStatus("Welcome to VZPlayer!");

        fxThread = Thread.currentThread();

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Set<Thread> threads = Thread.getAllStackTraces().keySet();
                System.out.printf("\n\n%-30s \t %-15s \t %-15s \t %s \n", "Name", "State", "Priority", "isDaemon");
                for (Thread t : threads) {
                    System.out.printf("%-30s \t %-15s \t %-15d \t %s\n", t.getName(), t.getState(), t.getPriority(), t.isDaemon());

                }

            }
        };

        timer.scheduleAtFixedRate(task,0,5000);

        if (fm.getListSize() > 0){
            video.load();
        }

        
    }
    
    public void setScreen(){
        // El tamaño del video es el tamaño de la ventana
        DoubleProperty width = video.prefWidthProperty();
        DoubleProperty height = video.prefHeightProperty();
        width.bind(scene.widthProperty());
        height.bind(scene.heightProperty());
    }
    
    public void dragHandlers(Stage stage){
        video.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY){
                if (!video.isFullscreen){
                    xOffset = event.getSceneX();
                    yOffset = event.getSceneY();
                }

            }


        });
        
        video.setOnMouseDragged(event -> {
            if (event.getButton() == MouseButton.SECONDARY){
                if (!video.isFullscreen){
                    stage.setX(event.getScreenX() - xOffset);
                    stage.setY(event.getScreenY() - yOffset);
                }
            }

        });
        
        video.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != video && event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            }
        });
        
        video.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    List<File> files = db.getFiles();
                    for (File f : files){
                        fm.open(f.getAbsolutePath());
                    }
                    if (fm.hasMedia()){
                        video.load();
                    }
                    // dropped.setText(db.getFiles().toString());
                    success = true;
                }
                /* let the source know whether the string was successfully 
                 * transferred and used */
                stage.requestFocus();
                event.setDropCompleted(success);
                event.consume();     
                
            }
        });  
    }

    public void windowHandlers(Stage stage){
        windowTitle.addListener((obv,oval,nval) -> {
            try{
                stage.setTitle(nval);
            } catch (Exception e){
                e.printStackTrace();
            }
            
        });
        
        video.quit.setOnMouseClicked(e -> {
            System.exit(0);
        });
        
        // Al salir del programa
        stage.setOnCloseRequest(event -> {
            // mc.clear();
            fm.getControl().saveSettings();
            System.exit(0);
        });
    }
    
    public static void main(String[] args){
        // Traspasa los argumentos y lanza la aplicación
        cliargs = args;
        launch(cliargs);
    }
}


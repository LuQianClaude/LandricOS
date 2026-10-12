package com.shenjibai.lira;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lira - 桌面端主入口
 * 梦女系统桌面版，与安卓端 Sirius Villa 配套
 * 署名: Lira
 */
public class LiraDesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        LiraRootView root = new LiraRootView(primaryStage);
        Scene scene = new Scene(root, 900, 640);
        scene.getStylesheets().add(getClass().getResource("/css/lira.css").toExternalForm());
        primaryStage.setTitle("Lira");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(560);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

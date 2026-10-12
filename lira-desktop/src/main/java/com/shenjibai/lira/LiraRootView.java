package com.shenjibai.lira;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Lira - 桌面端根视图：左侧导航 + 右侧内容
 * 署名: Lira
 */
public class LiraRootView extends BorderPane {

    private final Stage stage;
    private final LiraStorage storage;
    private final LiraLlmClient llmClient;

    public LiraRootView(Stage stage) {
        this.stage = stage;
        this.storage = new LiraStorage();
        this.llmClient = new LiraLlmClient(storage);
        getStyleClass().add("root");

        // 左侧导航
        VBox nav = new VBox(10);
        nav.getStyleClass().add("sidebar");
        nav.setPrefWidth(180);

        Label brand = new Label("Lira");
        brand.getStyleClass().add("brand");

        Label navChat = new Label("聊天");
        Label navAlbum = new Label("相册");
        Label navDiary = new Label("手札");
        Label navAnniv = new Label("纪念日");
        Label navSettings = new Label("设置");

        navChat.getStyleClass().add("nav-item");
        navAlbum.getStyleClass().add("nav-item");
        navDiary.getStyleClass().add("nav-item");
        navAnniv.getStyleClass().add("nav-item");
        navSettings.getStyleClass().add("nav-item");

        navChat.getStyleClass().add("nav-active");

        nav.getChildren().addAll(brand, navChat, navAlbum, navDiary, navAnniv, navSettings);

        // 右侧内容
        LiraChatView chatView = new LiraChatView(storage, llmClient);
        LiraSettingsView settingsView = new LiraSettingsView(storage);

        setCenter(chatView);
        setLeft(nav);

        // 导航切换
        navChat.setOnMouseClicked(e -> { setCenter(chatView); highlight(navChat, nav); });
        navAlbum.setOnMouseClicked(e -> { setCenter(new Label("相册")); highlight(navAlbum, nav); });
        navDiary.setOnMouseClicked(e -> { setCenter(new Label("手札")); highlight(navDiary, nav); });
        navAnniv.setOnMouseClicked(e -> { setCenter(new Label("纪念日")); highlight(navAnniv, nav); });
        navSettings.setOnMouseClicked(e -> { setCenter(settingsView); highlight(navSettings, nav); });
    }

    private void highlight(Label active, VBox nav) {
        for (javafx.scene.Node n : nav.getChildren()) {
            n.getStyleClass().remove("nav-active");
        }
        active.getStyleClass().add("nav-active");
    }
}

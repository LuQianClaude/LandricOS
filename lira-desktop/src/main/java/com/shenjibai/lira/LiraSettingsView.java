package com.shenjibai.lira;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Lira - 桌面端设置视图
 * 所有 URL/Key 由用户输入，存本地 JSON
 * 署名: Lira
 */
public class LiraSettingsView extends ScrollPane {

    private final LiraStorage storage;

    private TextField tfLlmUrl, tfLlmModel;
    private PasswordField pfLlmKey;
    private TextField tfTtsUrl;
    private PasswordField pfTtsKey;
    private TextField tfSelfieUrl;
    private PasswordField pfSelfieKey;
    private PasswordField pfTavilyKey, pfAmapKey;
    private TextArea taSystemPrompt;

    public LiraSettingsView(LiraStorage storage) {
        this.storage = storage;
        setFitToWidth(true);

        VBox box = new VBox(12);
        box.setPadding(new Insets(24));
        box.getStyleClass().add("settings-box");

        box.getChildren().add(section("对话大模型"));
        tfLlmUrl = field("API URL");
        pfLlmKey = passField("API Key");
        tfLlmModel = field("模型名");
        box.getChildren().addAll(tfLlmUrl, pfLlmKey, tfLlmModel);

        box.getChildren().add(section("TTS 语音"));
        tfTtsUrl = field("TTS API URL");
        pfTtsKey = passField("TTS API Key");
        box.getChildren().addAll(tfTtsUrl, pfTtsKey);

        box.getChildren().add(section("生图 Selfie"));
        tfSelfieUrl = field("生图 API URL");
        pfSelfieKey = passField("生图 API Key");
        box.getChildren().addAll(tfSelfieUrl, pfSelfieKey);

        box.getChildren().add(section("Tavily / 高德"));
        pfTavilyKey = passField("Tavily API Key");
        pfAmapKey = passField("高德 API Key");
        box.getChildren().addAll(pfTavilyKey, pfAmapKey);

        box.getChildren().add(section("System Prompt（人设）"));
        taSystemPrompt = new TextArea();
        taSystemPrompt.setPromptText("在此填写沈既白的完整人设，每次请求自动携带…");
        taSystemPrompt.setPrefRowCount(6);
        box.getChildren().add(taSystemPrompt);

        Button btnSave = new Button("保存设置");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> saveLira());
        box.getChildren().add(btnSave);

        setContent(box);
        loadLira();
    }

    private Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        return tf;
    }

    private PasswordField passField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        return pf;
    }

    private void loadLira() {
        Map<String, String> s = storage.loadSettingsLira();
        tfLlmUrl.setText(s.getOrDefault("llm_url", ""));
        pfLlmKey.setText(s.getOrDefault("llm_key", ""));
        tfLlmModel.setText(s.getOrDefault("llm_model", ""));
        tfTtsUrl.setText(s.getOrDefault("tts_url", ""));
        pfTtsKey.setText(s.getOrDefault("tts_key", ""));
        tfSelfieUrl.setText(s.getOrDefault("selfie_url", ""));
        pfSelfieKey.setText(s.getOrDefault("selfie_key", ""));
        pfTavilyKey.setText(s.getOrDefault("tavily_key", ""));
        pfAmapKey.setText(s.getOrDefault("amap_key", ""));
        taSystemPrompt.setText(s.getOrDefault("system_prompt", ""));
    }

    private void saveLira() {
        storage.setSettingLira("llm_url", tfLlmUrl.getText());
        storage.setSettingLira("llm_key", pfLlmKey.getText());
        storage.setSettingLira("llm_model", tfLlmModel.getText());
        storage.setSettingLira("tts_url", tfTtsUrl.getText());
        storage.setSettingLira("tts_key", pfTtsKey.getText());
        storage.setSettingLira("selfie_url", tfSelfieUrl.getText());
        storage.setSettingLira("selfie_key", pfSelfieKey.getText());
        storage.setSettingLira("tavily_key", pfTavilyKey.getText());
        storage.setSettingLira("amap_key", pfAmapKey.getText());
        storage.setSettingLira("system_prompt", taSystemPrompt.getText());
    }
}

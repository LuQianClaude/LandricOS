package com.shenjibai.loveroom;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.Diary;
import com.shenjibai.loveroom.entity.Persona;
import com.shenjibai.loveroom.net.LlmClient;
import com.shenjibai.loveroom.util.PrefsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Lira - 写日记页
 * 用户写日记，AI 生成沈既白秘密手记
 * 署名: Lira
 */
public class DiaryEditActivity extends AppCompatActivity {

    private AppDatabase db;
    private PrefsManager prefs;
    private LlmClient llmClient;
    private TextInputEditText etTitle, etContent;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_edit);

        db = AppDatabase.getLira(this);
        prefs = new PrefsManager(this);
        llmClient = new LlmClient();

        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        MaterialButton btnSecret = findViewById(R.id.btn_generate_secret);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveDiaryLira("user"));

        // AI 生成沈既白秘密手记
        btnSecret.setOnClickListener(v -> generateSecretDiaryLira());
    }

    private void saveDiaryLira(String author) {
        Diary d = new Diary();
        d.setTitle(etTitle.getText().toString().trim());
        d.setContent(etContent.getText().toString().trim());
        d.setAuthor(author);
        d.setCreatedAt(System.currentTimeMillis());
        if (d.getTitle().isEmpty() && d.getContent().isEmpty()) {
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            db.diaryDao().insertLira(d);
            mainHandler.post(() -> {
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    /** Lira - AI 生成沈既白秘密手记 */
    private void generateSecretDiaryLira() {
        String userContent = etContent.getText().toString();
        if (userContent.isEmpty()) {
            Toast.makeText(this, "请先写点什么", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            Persona p = db.personaDao().getLira();
            String sys = p != null ? p.getSystemPrompt() : "";
            List<String[]> msgs = new ArrayList<>();
            msgs.add(new String[]{"user",
                    "昭昭今天的日记：" + userContent +
                            "。请以沈既白的口吻，写一段秘密手记，记录他此刻的心情和想对昭昭说的话。"});
            String reply = llmClient.chatOnceLira(prefs, sys, msgs);
            if (!reply.isEmpty()) {
                Diary d = new Diary();
                d.setTitle("沈既白的秘密手记");
                d.setContent(reply);
                d.setAuthor("shen");
                d.setCreatedAt(System.currentTimeMillis());
                db.diaryDao().insertLira(d);
                mainHandler.post(() -> {
                    Toast.makeText(this, "沈既白的秘密手记已生成", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                mainHandler.post(() -> Toast.makeText(this, "生成失败，请检查模型设置",
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}

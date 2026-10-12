package com.shenjibai.loveroom;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.Persona;

/**
 * Lira - 世界书/人设编辑页
 * 用户填写完整人设存入 Room，每次 API 请求自动携带 System Prompt
 * 不硬编码任何角色内容
 * 署名: Lira
 */
public class PersonaActivity extends AppCompatActivity {

    private AppDatabase db;
    private TextInputEditText etName, etAppearance, etPersonality, etBackground, etSpeech, etSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_persona);

        db = AppDatabase.getLira(this);

        etName = findViewById(R.id.et_name);
        etAppearance = findViewById(R.id.et_appearance);
        etPersonality = findViewById(R.id.et_personality);
        etBackground = findViewById(R.id.et_background);
        etSpeech = findViewById(R.id.et_speech);
        etSystem = findViewById(R.id.et_system);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> savePersonaLira());

        loadPersonaLira();
    }

    private void loadPersonaLira() {
        new Thread(() -> {
            Persona p = db.personaDao().getLira();
            if (p != null) {
                runOnUiThread(() -> {
                    etName.setText(p.getName());
                    etAppearance.setText(p.getAppearance());
                    etPersonality.setText(p.getPersonality());
                    etBackground.setText(p.getBackground());
                    etSpeech.setText(p.getSpeechStyle());
                    etSystem.setText(p.getSystemPrompt());
                });
            }
        }).start();
    }

    private void savePersonaLira() {
        Persona p = new Persona();
        p.setName(etName.getText().toString());
        p.setAppearance(etAppearance.getText().toString());
        p.setPersonality(etPersonality.getText().toString());
        p.setBackground(etBackground.getText().toString());
        p.setSpeechStyle(etSpeech.getText().toString());
        p.setSystemPrompt(etSystem.getText().toString());
        p.setUpdatedAt(System.currentTimeMillis());

        new Thread(() -> {
            db.personaDao().insertLira(p);
            runOnUiThread(() -> {
                Toast.makeText(this, "人设已保存", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}

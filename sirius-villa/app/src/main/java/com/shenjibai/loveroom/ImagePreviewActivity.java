package com.shenjibai.loveroom;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.AlbumPhoto;

import java.io.InputStream;

/**
 * Lira - 图片大图预览 + 备注编辑（备注存 Room）
 * 署名: Lira
 */
public class ImagePreviewActivity extends AppCompatActivity {

    private AppDatabase db;
    private String assetPath;
    private EditText etNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        db = AppDatabase.getLira(this);
        ImageView iv = findViewById(R.id.iv_preview);
        ImageButton btnClose = findViewById(R.id.btn_close);
        etNote = findViewById(R.id.et_note);

        assetPath = getIntent().getStringExtra("asset_path");
        String imagePath = getIntent().getStringExtra("image_path");

        if (assetPath != null) {
            try {
                InputStream is = getAssets().open("album/" + assetPath);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                iv.setImageBitmap(bmp);
                is.close();
            } catch (Exception ignored) {}
        } else if (imagePath != null) {
            iv.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        }

        btnClose.setOnClickListener(v -> {
            saveNoteLira();
            finish();
        });

        // 加载已有备注
        if (assetPath != null) {
            new Thread(() -> {
                AlbumPhoto photo = db.albumPhotoDao().getByPathLira(assetPath);
                if (photo != null && !TextUtils.isEmpty(photo.getNote())) {
                    new Handler(Looper.getMainLooper()).post(() -> etNote.setText(photo.getNote()));
                }
            }).start();
        }
    }

    private void saveNoteLira() {
        if (assetPath == null) return;
        String note = etNote.getText().toString().trim();
        new Thread(() -> {
            AlbumPhoto photo = new AlbumPhoto();
            photo.setAssetPath(assetPath);
            photo.setNote(note);
            db.albumPhotoDao().insertLira(photo);
        }).start();
    }
}

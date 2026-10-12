package com.shenjibai.loveroom;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.shenjibai.loveroom.fragment.AlbumFragment;
import com.shenjibai.loveroom.fragment.AnniversaryFragment;
import com.shenjibai.loveroom.fragment.ChatFragment;
import com.shenjibai.loveroom.fragment.DiaryFragment;
import com.shenjibai.loveroom.fragment.SettingsFragment;
import com.shenjibai.loveroom.util.NightModeUtil;

/**
 * Lira - 主界面
 * 底部导航：聊天 / 相册 / 手札 / 纪念日 / 设置
 * 23-4 点自动全局暗调
 * 署名: Lira
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 23-4 点自动暗调
        NightModeUtil.applyNightModeLira(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        // 默认显示聊天
        if (savedInstanceState == null) {
            switchFragmentLira(new ChatFragment());
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment f = null;
            int id = item.getItemId();
            if (id == R.id.nav_chat) f = new ChatFragment();
            else if (id == R.id.nav_album) f = new AlbumFragment();
            else if (id == R.id.nav_diary) f = new DiaryFragment();
            else if (id == R.id.nav_anniv) f = new AnniversaryFragment();
            else if (id == R.id.nav_settings) f = new SettingsFragment();
            if (f != null) switchFragmentLira(f);
            return f != null;
        });

        // 唤醒词触发
        if (getIntent() != null && getIntent().getBooleanExtra("wake_up", false)) {
            switchFragmentLira(new ChatFragment());
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }
    }

    private void switchFragmentLira(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra("wake_up", false)) {
            switchFragmentLira(new ChatFragment());
            bottomNav.setSelectedItemId(R.id.nav_chat);
        }
    }
}

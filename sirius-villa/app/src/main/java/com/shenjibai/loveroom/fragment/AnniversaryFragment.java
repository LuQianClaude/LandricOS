package com.shenjibai.loveroom.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.adapter.AnniversaryAdapter;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.Anniversary;
import com.shenjibai.loveroom.worker.AnnivNotificationWorker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lira - 纪念日 Fragment
 * 默认 11-22 沈既白生日；CRUD；自动倒计时；WorkManager 通知
 * 署名: Lira
 */
public class AnniversaryFragment extends Fragment {

    private RecyclerView rvAnniv;
    private AnniversaryAdapter adapter;
    private List<Anniversary> annivList = new ArrayList<>();
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_anniv, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvAnniv = view.findViewById(R.id.rv_anniv);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);

        db = AppDatabase.getLira(requireContext());
        rvAnniv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AnniversaryAdapter(annivList);
        rvAnniv.setAdapter(adapter);

        // 首次启动插入默认生日 11-22
        ensureDefaultLira();

        db.anniversaryDao().getAllLiveLira().observe(getViewLifecycleOwner(), list -> {
            annivList = list;
            adapter.setListLira(list);
        });

        // 启动纪念日通知 Worker（每天检查一次）
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                AnnivNotificationWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "lira_anniv_check", ExistingPeriodicWorkPolicy.KEEP, request);

        fab.setOnClickListener(v -> showAddDialogLira());
    }

    /** Lira - 确保默认生日存在 */
    private void ensureDefaultLira() {
        new Thread(() -> {
            List<Anniversary> all = db.anniversaryDao().getAllLira();
            boolean exists = false;
            for (Anniversary a : all) {
                if (a.getDate() != null && a.getDate().endsWith("-11-22")) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                Anniversary def = new Anniversary();
                def.setName(getString(R.string.anniv_default_birthday));
                // 设为下一个 11-22
                Calendar c = Calendar.getInstance();
                int year = c.get(Calendar.YEAR);
                if (c.get(Calendar.MONTH) > 10
                        || (c.get(Calendar.MONTH) == 10 && c.get(Calendar.DAY_OF_MONTH) > 22)) {
                    year++;
                }
                def.setDate(year + "-11-22");
                db.anniversaryDao().insertLira(def);
            }
        }).start();
    }

    private void showAddDialogLira() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText etName = new EditText(getContext());
        etName.setHint(R.string.anniv_name);
        EditText etDate = new EditText(getContext());
        etDate.setHint("日期 (yyyy-MM-dd)");
        etDate.setInputType(android.text.InputType.TYPE_CLASS_DATETIME);

        layout.addView(etName);
        layout.addView(etDate);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.anniv_add)
                .setView(layout)
                .setPositiveButton("保存", (d, w) -> {
                    Anniversary a = new Anniversary();
                    a.setName(etName.getText().toString().trim());
                    a.setDate(etDate.getText().toString().trim());
                    if (!a.getName().isEmpty() && !a.getDate().isEmpty()) {
                        new Thread(() -> db.anniversaryDao().insertLira(a)).start();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}

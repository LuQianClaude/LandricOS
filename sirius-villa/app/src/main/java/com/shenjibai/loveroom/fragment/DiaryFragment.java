package com.shenjibai.loveroom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shenjibai.loveroom.DiaryEditActivity;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.adapter.DiaryAdapter;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.Diary;

import java.util.ArrayList;
import java.util.List;

/**
 * Lira - 手札 Fragment：时间线浏览
 * 用户写日记，AI 生成沈既白秘密手记
 * 署名: Lira
 */
public class DiaryFragment extends Fragment {

    private RecyclerView rvDiary;
    private DiaryAdapter adapter;
    private List<Diary> diaryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvDiary = view.findViewById(R.id.rv_diary);
        FloatingActionButton fab = view.findViewById(R.id.fab_new);

        rvDiary.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DiaryAdapter(diaryList);
        rvDiary.setAdapter(adapter);

        AppDatabase db = AppDatabase.getLira(requireContext());
        db.diaryDao().getAllLiveLira().observe(getViewLifecycleOwner(), diaries -> {
            diaryList = diaries;
            adapter.setDiariesLira(diaries);
        });

        fab.setOnClickListener(v -> startActivity(new Intent(getContext(), DiaryEditActivity.class)));
    }
}

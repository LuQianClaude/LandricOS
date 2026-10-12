package com.shenjibai.loveroom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shenjibai.loveroom.ImagePreviewActivity;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.adapter.AlbumAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lira - 相册 Fragment
 * 读取 assets/album 下图片，网格展示，点击大图预览
 * 署名: Lira
 */
public class AlbumFragment extends Fragment implements AlbumAdapter.OnPhotoClickListener {

    private RecyclerView rvAlbum;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_album, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvAlbum = view.findViewById(R.id.rv_album);
        rvAlbum.setLayoutManager(new GridLayoutManager(getContext(), 3));

        List<String> photos = listAlbumAssetsLira();
        rvAlbum.setAdapter(new AlbumAdapter(requireContext(), photos, this));
    }

    /** Lira - 列出 assets/album 下所有图片 */
    private List<String> listAlbumAssetsLira() {
        List<String> result = new ArrayList<>();
        try {
            String[] files = requireContext().getAssets().list("album");
            if (files != null) {
                for (String f : files) {
                    if (f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png")
                            || f.endsWith(".webp")) {
                        result.add(f);
                    }
                }
            }
        } catch (IOException ignored) {}
        return result;
    }

    @Override
    public void onPhotoClickLira(String assetPath) {
        Intent intent = new Intent(getContext(), ImagePreviewActivity.class);
        intent.putExtra("asset_path", assetPath);
        startActivity(intent);
    }
}

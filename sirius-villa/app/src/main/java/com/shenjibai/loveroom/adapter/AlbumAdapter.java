package com.shenjibai.loveroom.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.db.AppDatabase;
import com.shenjibai.loveroom.entity.AlbumPhoto;

import java.io.InputStream;
import java.util.List;

/**
 * Lira - 相册网格 Adapter
 * 读取 assets/album 下图片
 * 署名: Lira
 */
public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.VH> {

    private final Context context;
    private final List<String> assetPaths;
    private final OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClickLira(String assetPath);
    }

    public AlbumAdapter(Context context, List<String> assetPaths, OnPhotoClickListener listener) {
        this.context = context;
        this.assetPaths = assetPaths;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_album, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String path = assetPaths.get(position);
        try {
            InputStream is = context.getAssets().open("album/" + path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            holder.ivPhoto.setImageBitmap(bmp);
            is.close();
        } catch (Exception ignored) {}

        // 加载备注
        new Thread(() -> {
            AlbumPhoto photo = AppDatabase.getLira(context).albumPhotoDao().getByPathLira(path);
            if (photo != null && photo.getNote() != null) {
                holder.tvNote.post(() -> holder.tvNote.setText(photo.getNote()));
            }
        }).start();

        holder.itemView.setOnClickListener(v -> listener.onPhotoClickLira(path));
    }

    @Override
    public int getItemCount() {
        return assetPaths == null ? 0 : assetPaths.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvNote;
        VH(View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.iv_photo);
            tvNote = v.findViewById(R.id.tv_note);
        }
    }
}

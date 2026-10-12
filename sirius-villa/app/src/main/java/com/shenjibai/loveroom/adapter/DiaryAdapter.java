package com.shenjibai.loveroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.entity.Diary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lira - 手札时间线 Adapter
 * 署名: Lira
 */
public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.VH> {

    private List<Diary> diaries;

    public DiaryAdapter(List<Diary> diaries) {
        this.diaries = diaries;
    }

    public void setDiariesLira(List<Diary> list) {
        this.diaries = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_diary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Diary d = diaries.get(position);
        holder.tvDate.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date(d.getCreatedAt())));
        holder.tvTitle.setText(d.getTitle());
        holder.tvContent.setText(d.getContent());
        holder.tvAuthor.setText("shen".equals(d.getAuthor()) ? "沈既白 · 秘密手记" : "昭昭");
    }

    @Override
    public int getItemCount() {
        return diaries == null ? 0 : diaries.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvTitle, tvContent, tvAuthor;
        VH(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tv_date);
            tvTitle = v.findViewById(R.id.tv_title);
            tvContent = v.findViewById(R.id.tv_content);
            tvAuthor = v.findViewById(R.id.tv_author);
        }
    }
}

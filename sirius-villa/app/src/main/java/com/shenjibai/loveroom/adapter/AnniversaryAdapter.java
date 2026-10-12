package com.shenjibai.loveroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.entity.Anniversary;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Lira - 纪念日 Adapter（含自动倒计时）
 * 署名: Lira
 */
public class AnniversaryAdapter extends RecyclerView.Adapter<AnniversaryAdapter.VH> {

    private List<Anniversary> list;

    public AnniversaryAdapter(List<Anniversary> list) {
        this.list = list;
    }

    public void setListLira(List<Anniversary> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anniv, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Anniversary a = list.get(position);
        holder.tvName.setText(a.getName());
        holder.tvDate.setText(a.getDate());
        holder.tvCountdown.setText(calcCountdownLira(a.getDate()));
    }

    /** Lira - 计算倒计时天数（下一次到来） */
    private String calcCountdownLira(String dateStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date target = fmt.parse(dateStr);
            if (target == null) return "-";

            Calendar now = Calendar.getInstance();
            Calendar targetCal = Calendar.getInstance();
            targetCal.setTime(target);
            // 设为今年
            targetCal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            // 如果今年已过，设为明年
            if (targetCal.before(now)) {
                targetCal.add(Calendar.YEAR, 1);
            }
            long diff = targetCal.getTimeInMillis() - now.getTimeInMillis();
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + " 天";
        } catch (Exception e) {
            return "-";
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvCountdown;
        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvDate = v.findViewById(R.id.tv_date);
            tvCountdown = v.findViewById(R.id.tv_countdown);
        }
    }
}

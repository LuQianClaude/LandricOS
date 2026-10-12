package com.shenjibai.loveroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.shenjibai.loveroom.R;
import com.shenjibai.loveroom.entity.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Lira - 聊天消息 Adapter
 * 支持文本消息、图片消息卡片、商品卡片
 * 左右气泡：沈既白（左，深墨灰+鎏金）/ 萧渲禾（右，浅米白）
 * 署名: Lira
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SHEN_TEXT = 0;
    private static final int TYPE_XIAO_TEXT = 1;
    private static final int TYPE_SHEN_IMAGE = 2;
    private static final int TYPE_SHEN_PRODUCT = 3;

    private List<ChatMessage> messages;
    private final OnMessageActionListener listener;

    public interface OnMessageActionListener {
        void onMemoryClickLira(ChatMessage msg);
        void onTtsClickLira(ChatMessage msg);
        void onImageClickLira(ChatMessage msg);
    }

    public ChatAdapter(List<ChatMessage> messages, OnMessageActionListener listener) {
        this.messages = messages;
        this.listener = listener;
    }

    public void setMessagesLira(List<ChatMessage> msgs) {
        this.messages = msgs;
        notifyDataSetChanged();
    }

    public void addMessageLira(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = messages.get(position);
        boolean isShen = "shen".equals(m.getSender());
        if (isShen) {
            if ("image".equals(m.getType())) return TYPE_SHEN_IMAGE;
            if ("product".equals(m.getType())) return TYPE_SHEN_PRODUCT;
            return TYPE_SHEN_TEXT;
        }
        return TYPE_XIAO_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_SHEN_IMAGE:
                return new ImageVH(inflater.inflate(R.layout.item_chat_image, parent, false));
            case TYPE_SHEN_PRODUCT:
                return new ProductVH(inflater.inflate(R.layout.item_chat_product, parent, false));
            case TYPE_XIAO_TEXT:
                return new XiaoVH(inflater.inflate(R.layout.item_chat_xiao, parent, false));
            default:
                return new ShenVH(inflater.inflate(R.layout.item_chat_shen, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (holder instanceof ShenVH) {
            ShenVH vh = (ShenVH) holder;
            vh.tvContent.setText(msg.getContent());
            vh.btnMemory.setOnClickListener(v -> listener.onMemoryClickLira(msg));
            vh.btnTts.setOnClickListener(v -> listener.onTtsClickLira(msg));
            // 重要记忆标记
            vh.btnMemory.setAlpha(msg.isImportant() ? 1f : 0.3f);
        } else if (holder instanceof XiaoVH) {
            XiaoVH vh = (XiaoVH) holder;
            vh.tvContent.setText(msg.getContent());
            vh.tvTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(new Date(msg.getTimestamp())));
        } else if (holder instanceof ImageVH) {
            ImageVH vh = (ImageVH) holder;
            if (msg.getImagePath() != null) {
                Glide.with(vh.ivImage.getContext()).load(msg.getImagePath()).into(vh.ivImage);
            }
            vh.tvCaption.setText(msg.getContent());
            vh.itemView.setOnClickListener(v -> listener.onImageClickLira(msg));
        } else if (holder instanceof ProductVH) {
            ProductVH vh = (ProductVH) holder;
            try {
                JsonObject p = new Gson().fromJson(msg.getProductJson(), JsonObject.class);
                vh.tvTitle.setText(p.has("title") ? p.get("title").getAsString() : "");
                vh.tvPrice.setText(p.has("price") ? p.get("price").getAsString() : "");
            } catch (Exception ignored) {}
        }
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    // ---- ViewHolders ----
    static class ShenVH extends RecyclerView.ViewHolder {
        TextView tvContent;
        ImageView btnMemory, btnTts;
        ShenVH(View v) {
            super(v);
            tvContent = v.findViewById(R.id.tv_content);
            btnMemory = v.findViewById(R.id.btn_memory);
            btnTts = v.findViewById(R.id.btn_tts);
        }
    }

    static class XiaoVH extends RecyclerView.ViewHolder {
        TextView tvContent, tvTime;
        XiaoVH(View v) {
            super(v);
            tvContent = v.findViewById(R.id.tv_content);
            tvTime = v.findViewById(R.id.tv_time);
        }
    }

    static class ImageVH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCaption;
        ImageVH(View v) {
            super(v);
            ivImage = v.findViewById(R.id.iv_image);
            tvCaption = v.findViewById(R.id.tv_caption);
        }
    }

    static class ProductVH extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvTitle, tvPrice;
        ProductVH(View v) {
            super(v);
            ivProduct = v.findViewById(R.id.iv_product);
            tvTitle = v.findViewById(R.id.tv_product_title);
            tvPrice = v.findViewById(R.id.tv_product_price);
        }
    }
}

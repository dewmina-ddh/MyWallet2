package com.example.mywallet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * MainCategoryAdapter displays categories horizontally on the Overview / MainActivity screen.
 */
public class MainCategoryAdapter extends RecyclerView.Adapter<MainCategoryAdapter.MainCategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryModel category);
    }

    private Context context;
    private List<CategoryModel> categoryList;
    private OnCategoryClickListener listener;

    public MainCategoryAdapter(Context context, List<CategoryModel> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    public void updateData(List<CategoryModel> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MainCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_main_category, parent, false);
        return new MainCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MainCategoryViewHolder holder, int position) {
        CategoryModel item = categoryList.get(position);

        holder.tvName.setText(item.getName());
        int iconRes = CategoryAdapter.getIconResource(item.getIconName(), item.getName());
        holder.imgIcon.setImageResource(iconRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    static class MainCategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvName;

        public MainCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgMainCategoryIcon);
            tvName = itemView.findViewById(R.id.tvMainCategoryName);
        }
    }
}

package com.example.mywallet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

/**
 * CategoryAdapter displays custom and default categories with live calculated totals from SQLite.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(CategoryModel category);
        void onCategoryEdit(CategoryModel category);
        void onCategoryDelete(CategoryModel category);
    }

    private Context context;
    private List<CategoryModel> categoryList;
    private DatabaseHelper dbHelper;
    private OnCategoryClickListener listener;
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    public CategoryAdapter(Context context, List<CategoryModel> categoryList, DatabaseHelper dbHelper, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.dbHelper = dbHelper;
        this.listener = listener;
    }

    public void updateData(List<CategoryModel> newList) {
        this.categoryList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategoryModel item = categoryList.get(position);

        holder.tvName.setText(item.getName());

        // Calculate live total from SQLite database
        double total = dbHelper != null ? dbHelper.getTotalForCategory(item.getName()) : 0.0;
        boolean isIncome = "INCOME".equalsIgnoreCase(item.getType());

        if (isIncome) {
            holder.tvTotal.setText("Total Earned: " + CurrencyHelper.formatAmount(context, total));
            holder.tvBadge.setText("Income");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.income_green_badge_txt));
            holder.tvBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_income));
        } else {
            holder.tvTotal.setText("Total Spent: " + CurrencyHelper.formatAmount(context, total));
            holder.tvBadge.setText("Expense");
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.expense_red_txt));
            holder.tvBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_expense));
        }

        // Set Category Icon
        int iconRes = getIconResource(item.getIconName(), item.getName());
        holder.imgIcon.setImageResource(iconRes);

        // Click on category card or arrow button to open details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(item);
            }
        });

        if (holder.btnArrow != null) {
            holder.btnArrow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(item);
                }
            });
        }

        // Edit button click
        if (holder.btnEdit != null) {
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryEdit(item);
                }
            });
        }

        // Delete button click
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryDelete(item);
                }
            });
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onCategoryEdit(item);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public static int getIconResource(String iconName, String categoryName) {
        if (iconName != null) {
            switch (iconName) {
                case "ic_category_food": return R.drawable.ic_category_food;
                case "ic_category_shopping": return R.drawable.ic_category_shopping;
                case "ic_category_transport": return R.drawable.ic_category_transport;
                case "ic_category_utilities": return R.drawable.ic_category_utilities;
                case "ic_category_invest": return R.drawable.ic_category_invest;
                case "ic_category_freelance": return R.drawable.ic_category_freelance;
                case "ic_category_medical": return R.drawable.ic_category_medical;
                case "ic_category_education": return R.drawable.ic_category_education;
                case "ic_category_entertainment": return R.drawable.ic_category_entertainment;
                case "ic_field_wallet": return R.drawable.ic_field_wallet;
                case "ic_field_card": return R.drawable.ic_field_card;
            }
        }

        if (categoryName == null) categoryName = "";
        String lower = categoryName.toLowerCase();

        if (lower.contains("food") || lower.contains("dining") || lower.contains("rest") || lower.contains("meal") || lower.contains("cafe") || lower.contains("pizza") || lower.contains("burger")) return R.drawable.ic_category_food;
        if (lower.contains("shop") || lower.contains("cloth") || lower.contains("buy") || lower.contains("store") || lower.contains("mall") || lower.contains("market")) return R.drawable.ic_category_shopping;
        if (lower.contains("travel") || lower.contains("transport") || lower.contains("car") || lower.contains("fuel") || lower.contains("gas") || lower.contains("taxi") || lower.contains("bus") || lower.contains("uber") || lower.contains("flight") || lower.contains("train")) return R.drawable.ic_category_transport;
        if (lower.contains("utilit") || lower.contains("bill") || lower.contains("water") || lower.contains("elec") || lower.contains("power") || lower.contains("wifi") || lower.contains("internet") || lower.contains("phone")) return R.drawable.ic_category_utilities;
        if (lower.contains("invest") || lower.contains("stock") || lower.contains("crypto") || lower.contains("gold") || lower.contains("trade") || lower.contains("share")) return R.drawable.ic_category_invest;
        if (lower.contains("freelance") || lower.contains("salary") || lower.contains("work") || lower.contains("job") || lower.contains("bonus") || lower.contains("wage") || lower.contains("income")) return R.drawable.ic_category_freelance;
        if (lower.contains("health") || lower.contains("med") || lower.contains("doctor") || lower.contains("gym") || lower.contains("fitness") || lower.contains("hospital") || lower.contains("pharma")) return R.drawable.ic_category_medical;
        if (lower.contains("edu") || lower.contains("school") || lower.contains("book") || lower.contains("course") || lower.contains("college") || lower.contains("uni") || lower.contains("study")) return R.drawable.ic_category_education;
        if (lower.contains("entertain") || lower.contains("movie") || lower.contains("game") || lower.contains("party") || lower.contains("music") || lower.contains("fun")) return R.drawable.ic_category_entertainment;
        if (lower.contains("card") || lower.contains("bank") || lower.contains("credit") || lower.contains("atm")) return R.drawable.ic_field_card;

        return R.drawable.ic_field_wallet;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon, btnEdit, btnDelete, btnArrow;
        TextView tvName, tvTotal, tvBadge;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgCategoryIcon);
            btnEdit = itemView.findViewById(R.id.btnEditCategory);
            btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
            btnArrow = itemView.findViewById(R.id.btnCategoryArrow);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvTotal = itemView.findViewById(R.id.tvCategoryTotal);
            tvBadge = itemView.findViewById(R.id.tvCategoryBadge);
        }
    }
}
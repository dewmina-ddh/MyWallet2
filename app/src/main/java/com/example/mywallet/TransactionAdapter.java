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
 * TransactionAdapter displays the list of income and expense transactions in a RecyclerView.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionModel transaction);
        void onTransactionLongClick(TransactionModel transaction);
    }

    private Context context;
    private List<TransactionModel> transactionList;
    private OnTransactionClickListener listener;
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    public TransactionAdapter(Context context, List<TransactionModel> transactionList, OnTransactionClickListener listener) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
    }

    public void updateData(List<TransactionModel> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionModel item = transactionList.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvCategory.setText(item.getCategory());
        holder.tvDate.setText(item.getDate() != null && !item.getDate().isEmpty() ? item.getDate() : "Recent");
        
        if (holder.tvPaymentMethod != null) {
            String method = item.getPaymentMethod();
            if (method != null && !method.isEmpty() && !method.equalsIgnoreCase("General") && !method.equalsIgnoreCase("Income") && !method.equalsIgnoreCase("Expense")) {
                holder.tvPaymentMethod.setText(method);
                holder.tvPaymentMethod.setVisibility(View.VISIBLE);
            } else {
                holder.tvPaymentMethod.setVisibility(View.GONE);
            }
        }

        // Dynamic Icon Resolution
        int iconRes = getIconForCategory(item.getCategory(), item.getType());
        holder.imgIcon.setImageResource(iconRes);

        // Format Amount and Color based on INCOME / EXPENSE
        boolean isIncome = "INCOME".equalsIgnoreCase(item.getType());
        if (isIncome) {
            holder.tvAmount.setText("+" + CurrencyHelper.formatAmount(context, item.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green_txt));
        } else {
            holder.tvAmount.setText("-" + CurrencyHelper.formatAmount(context, item.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red_txt));
        }

        // Click Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransactionClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onTransactionLongClick(item);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return transactionList != null ? transactionList.size() : 0;
    }

    private int getIconForCategory(String category, String type) {
        if (category == null) category = "";
        String lower = category.toLowerCase();

        if (lower.contains("salary") || lower.contains("job") || lower.contains("freelance") || lower.contains("work") || lower.contains("wage")) {
            return R.drawable.ic_category_freelance;
        } else if (lower.contains("travel") || lower.contains("flight") || lower.contains("transport") || lower.contains("car") || lower.contains("fuel") || lower.contains("taxi") || lower.contains("bus")) {
            return R.drawable.ic_category_transport;
        } else if (lower.contains("bill") || lower.contains("utilit") || lower.contains("electric") || lower.contains("water") || lower.contains("power") || lower.contains("wifi")) {
            return R.drawable.ic_category_utilities;
        } else if (lower.contains("food") || lower.contains("dining") || lower.contains("grocer") || lower.contains("meal") || lower.contains("cafe") || lower.contains("pizza")) {
            return R.drawable.ic_category_food;
        } else if (lower.contains("shop") || lower.contains("cloth") || lower.contains("store") || lower.contains("market")) {
            return R.drawable.ic_category_shopping;
        } else if (lower.contains("invest") || lower.contains("stock") || lower.contains("crypto") || lower.contains("gold")) {
            return R.drawable.ic_category_invest;
        } else if (lower.contains("med") || lower.contains("health") || lower.contains("doctor") || lower.contains("hospital") || lower.contains("gym")) {
            return R.drawable.ic_category_medical;
        } else if (lower.contains("edu") || lower.contains("school") || lower.contains("book") || lower.contains("course") || lower.contains("study")) {
            return R.drawable.ic_category_education;
        } else if (lower.contains("entertain") || lower.contains("movie") || lower.contains("game") || lower.contains("music") || lower.contains("party")) {
            return R.drawable.ic_category_entertainment;
        }

        return "INCOME".equalsIgnoreCase(type) ? R.drawable.ic_field_wallet : R.drawable.ic_field_card;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvTitle, tvCategory, tvDate, tvAmount, tvPaymentMethod;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgTransIcon);
            tvTitle = itemView.findViewById(R.id.tvTransTitle);
            tvCategory = itemView.findViewById(R.id.tvTransCategory);
            tvDate = itemView.findViewById(R.id.tvTransDate);
            tvAmount = itemView.findViewById(R.id.tvTransAmount);
            tvPaymentMethod = itemView.findViewById(R.id.tvTransPaymentMethod);
        }
    }
}
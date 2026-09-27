package com.example.mywallet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * AllTransactionsActivity displays the entire history of income and expenses
 * with live search and All / Income / Expense filtering.
 */
public class AllTransactionsActivity extends AppCompatActivity implements RecordTransactionBottomSheet.OnTransactionSavedListener {

    private TextView tabFilterAll, tabFilterIncome, tabFilterExpense, tvEmptyAllTransactions;
    private EditText etSearchTransactions;
    private RecyclerView recyclerViewAllTransactions;
    private TransactionAdapter transactionAdapter;
    private DatabaseHelper dbHelper;

    private List<TransactionModel> fullList = new ArrayList<>();
    private String currentTypeFilter = "ALL"; // "ALL", "INCOME", "EXPENSE"
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_transactions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.allTransactionsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);

        // Bind Views
        tabFilterAll = findViewById(R.id.tabFilterAll);
        tabFilterIncome = findViewById(R.id.tabFilterIncome);
        tabFilterExpense = findViewById(R.id.tabFilterExpense);
        tvEmptyAllTransactions = findViewById(R.id.tvEmptyAllTransactions);
        etSearchTransactions = findViewById(R.id.etSearchTransactions);
        recyclerViewAllTransactions = findViewById(R.id.recyclerViewAllTransactions);

        // Back Button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Add Button
        View btnAddTransaction = findViewById(R.id.btnAddTransaction);
        if (btnAddTransaction != null) {
            btnAddTransaction.setOnClickListener(v -> {
                boolean isIncome = "INCOME".equalsIgnoreCase(currentTypeFilter);
                RecordTransactionBottomSheet sheet = RecordTransactionBottomSheet.newInstance(isIncome);
                sheet.setOnTransactionSavedListener(this);
                sheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
            });
        }

        // Setup Tabs
        tabFilterAll.setOnClickListener(v -> selectFilterTab("ALL"));
        tabFilterIncome.setOnClickListener(v -> selectFilterTab("INCOME"));
        tabFilterExpense.setOnClickListener(v -> selectFilterTab("EXPENSE"));

        // Setup RecyclerView
        recyclerViewAllTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(this, null, new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(TransactionModel transaction) {
                showTransactionOptionsDialog(transaction);
            }

            @Override
            public void onTransactionLongClick(TransactionModel transaction) {
                showTransactionOptionsDialog(transaction);
            }
        });
        recyclerViewAllTransactions.setAdapter(transactionAdapter);

        // Search text watcher
        if (etSearchTransactions != null) {
            etSearchTransactions.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s.toString().trim();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        loadTransactions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    @Override
    public void onTransactionSaved() {
        loadTransactions();
    }

    private void loadTransactions() {
        if (dbHelper == null) return;
        fullList = dbHelper.getAllTransactions();
        applyFilters();
    }

    private void selectFilterTab(String type) {
        currentTypeFilter = type;

        TextView[] tabs = {tabFilterAll, tabFilterIncome, tabFilterExpense};
        String[] types = {"ALL", "INCOME", "EXPENSE"};

        for (int i = 0; i < tabs.length; i++) {
            if (types[i].equals(type)) {
                tabs[i].setBackground(ContextCompat.getDrawable(this, R.drawable.bg_pill_filter_active));
                tabs[i].setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                tabs[i].setBackground(null);
                tabs[i].setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }

        applyFilters();
    }

    private void applyFilters() {
        List<TransactionModel> filtered = new ArrayList<>();

        for (TransactionModel item : fullList) {
            // Type Match
            boolean typeMatches = "ALL".equals(currentTypeFilter) || item.getType().equalsIgnoreCase(currentTypeFilter);

            // Search Match (Title, Category, Payment Method)
            boolean searchMatches = true;
            if (!currentSearchQuery.isEmpty()) {
                String query = currentSearchQuery.toLowerCase();
                searchMatches = (item.getTitle() != null && item.getTitle().toLowerCase().contains(query))
                        || (item.getCategory() != null && item.getCategory().toLowerCase().contains(query))
                        || (item.getPaymentMethod() != null && item.getPaymentMethod().toLowerCase().contains(query));
            }

            if (typeMatches && searchMatches) {
                filtered.add(item);
            }
        }

        if (filtered.isEmpty()) {
            recyclerViewAllTransactions.setVisibility(View.GONE);
            tvEmptyAllTransactions.setVisibility(View.VISIBLE);
        } else {
            recyclerViewAllTransactions.setVisibility(View.VISIBLE);
            tvEmptyAllTransactions.setVisibility(View.GONE);
            transactionAdapter.updateData(filtered);
        }
    }

    private void showTransactionOptionsDialog(TransactionModel transaction) {
        String[] options = {"Edit Transaction", "Delete Transaction"};
        new AlertDialog.Builder(this)
                .setTitle(transaction.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        RecordTransactionBottomSheet sheet = RecordTransactionBottomSheet.newInstanceForEdit(transaction.getId());
                        sheet.setOnTransactionSavedListener(this);
                        sheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
                    } else if (which == 1) {
                        confirmDeleteTransaction(transaction);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteTransaction(TransactionModel transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction?")
                .setMessage("Are you sure you want to delete \"" + transaction.getTitle() + "\"?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteTransaction(transaction.getId());
                    loadTransactions();
                    Toast.makeText(AllTransactionsActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
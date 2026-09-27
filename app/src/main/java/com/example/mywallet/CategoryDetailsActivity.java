package com.example.mywallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
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

import java.text.DecimalFormat;
import java.util.List;

/**
 * CategoryDetailsActivity displays all income or expense records belonging to a specific category.
 */
public class CategoryDetailsActivity extends AppCompatActivity implements RecordTransactionBottomSheet.OnTransactionSavedListener {

    private String categoryName = "Food & Dining";
    private String categoryType = "EXPENSE";

    private TextView tvCategoryName, tvCategoryTypeBadge, tvTotalAmount, tvAmountLabel, tvTransactionCount, tvEmptyCategoryTransactions;
    private ImageView imgCategoryIcon;
    private RecyclerView recyclerViewCategoryTransactions;
    private TransactionAdapter transactionAdapter;
    private DatabaseHelper dbHelper;
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category_details);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoryDetailsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent() != null) {
            if (getIntent().hasExtra("category_name")) {
                categoryName = getIntent().getStringExtra("category_name");
            }
            if (getIntent().hasExtra("category_type")) {
                categoryType = getIntent().getStringExtra("category_type");
            }
        }

        dbHelper = new DatabaseHelper(this);

        // Bind Views
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvCategoryTypeBadge = findViewById(R.id.tvCategoryTypeBadge);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvAmountLabel = findViewById(R.id.tvAmountLabel);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
        tvEmptyCategoryTransactions = findViewById(R.id.tvEmptyCategoryTransactions);
        imgCategoryIcon = findViewById(R.id.imgCategoryIcon);
        recyclerViewCategoryTransactions = findViewById(R.id.recyclerViewCategoryTransactions);

        // Back Button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Add transaction to this specific category
        View btnAddTransaction = findViewById(R.id.btnAddTransaction);
        if (btnAddTransaction != null) {
            btnAddTransaction.setOnClickListener(v -> {
                boolean isIncome = "INCOME".equalsIgnoreCase(categoryType);
                RecordTransactionBottomSheet bottomSheet = RecordTransactionBottomSheet.newInstance(isIncome);
                bottomSheet.setOnTransactionSavedListener(this);
                bottomSheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
            });
        }

        // Edit Category Button
        View btnEditCategory = findViewById(R.id.btnEditCategoryDetail);
        if (btnEditCategory != null) {
            btnEditCategory.setOnClickListener(v -> showEditCategoryDialog());
        }

        // Delete Category Button
        View btnDeleteCategory = findViewById(R.id.btnDeleteCategoryDetail);
        if (btnDeleteCategory != null) {
            btnDeleteCategory.setOnClickListener(v -> showDeleteCategoryDialog());
        }

        // Setup RecyclerView
        recyclerViewCategoryTransactions.setLayoutManager(new LinearLayoutManager(this));
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
        recyclerViewCategoryTransactions.setAdapter(transactionAdapter);

        // Populate details
        loadCategoryData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoryData();
    }

    @Override
    public void onTransactionSaved() {
        loadCategoryData();
    }

    private void loadCategoryData() {
        // Query CategoryModel from DB to ensure freshest info
        CategoryModel cat = dbHelper.getCategoryByName(categoryName);
        String currentIcon = null;
        if (cat != null) {
            categoryName = cat.getName();
            categoryType = cat.getType();
            currentIcon = cat.getIconName();
        }

        tvCategoryName.setText(categoryName);

        boolean isIncome = "INCOME".equalsIgnoreCase(categoryType);
        if (isIncome) {
            tvCategoryTypeBadge.setText("Income");
            tvCategoryTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.income_green_badge_txt));
            tvCategoryTypeBadge.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_income));
            tvAmountLabel.setText("Total Earned");
        } else {
            tvCategoryTypeBadge.setText("Expense");
            tvCategoryTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.expense_red_txt));
            tvCategoryTypeBadge.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_badge_expense));
            tvAmountLabel.setText("Total Spent");
        }

        imgCategoryIcon.setImageResource(CategoryAdapter.getIconResource(currentIcon, categoryName));

        // Load filtered transactions
        double total = dbHelper.getTotalForCategory(categoryName);
        tvTotalAmount.setText(CurrencyHelper.formatAmount(this, total));

        List<TransactionModel> list = dbHelper.getTransactionsByCategory(categoryName);
        tvTransactionCount.setText(String.valueOf(list.size()));

        if (list.isEmpty()) {
            recyclerViewCategoryTransactions.setVisibility(View.GONE);
            tvEmptyCategoryTransactions.setVisibility(View.VISIBLE);
        } else {
            recyclerViewCategoryTransactions.setVisibility(View.VISIBLE);
            tvEmptyCategoryTransactions.setVisibility(View.GONE);
            transactionAdapter.updateData(list);
        }
    }

    private void showEditCategoryDialog() {
        CategoryModel category = dbHelper.getCategoryByName(categoryName);
        if (category == null) {
            category = new CategoryModel(categoryName, categoryType, "ic_category_food");
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.etDialogCategoryName);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbDialogExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbDialogIncome);

        // Prepopulate data
        etName.setText(category.getName());
        if ("INCOME".equalsIgnoreCase(category.getType())) {
            rbIncome.setChecked(true);
        } else {
            rbExpense.setChecked(true);
        }

        final String[] selectedIcon = {category.getIconName() != null ? category.getIconName() : "ic_category_food"};
        ImageView[] iconViews = {
                dialogView.findViewById(R.id.iconFood),
                dialogView.findViewById(R.id.iconShopping),
                dialogView.findViewById(R.id.iconTransport),
                dialogView.findViewById(R.id.iconUtilities),
                dialogView.findViewById(R.id.iconInvest),
                dialogView.findViewById(R.id.iconWork),
                dialogView.findViewById(R.id.iconMedical),
                dialogView.findViewById(R.id.iconEducation),
                dialogView.findViewById(R.id.iconEntertainment),
                dialogView.findViewById(R.id.iconWallet)
        };

        String[] iconNames = {
                "ic_category_food",
                "ic_category_shopping",
                "ic_category_transport",
                "ic_category_utilities",
                "ic_category_invest",
                "ic_category_freelance",
                "ic_category_medical",
                "ic_category_education",
                "ic_category_entertainment",
                "ic_field_wallet"
        };

        // Select initial icon highlight
        for (int i = 0; i < iconViews.length; i++) {
            if (iconViews[i] != null) {
                boolean isSelected = iconNames[i].equalsIgnoreCase(selectedIcon[0]);
                iconViews[i].setBackgroundResource(isSelected ? R.drawable.bg_icon_selected : R.drawable.bg_icon_unselected);
            }
        }

        for (int i = 0; i < iconViews.length; i++) {
            final int index = i;
            if (iconViews[index] != null) {
                iconViews[index].setOnClickListener(v -> {
                    selectedIcon[0] = iconNames[index];
                    for (int j = 0; j < iconViews.length; j++) {
                        if (iconViews[j] != null) {
                            iconViews[j].setBackgroundResource(j == index ? R.drawable.bg_icon_selected : R.drawable.bg_icon_unselected);
                        }
                    }
                });
            }
        }

        final CategoryModel finalCat = category;
        new AlertDialog.Builder(this)
                .setTitle("Edit Category")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(CategoryDetailsActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newType = rbIncome.isChecked() ? "INCOME" : "EXPENSE";
                    String newIcon = selectedIcon[0];
                    String oldName = finalCat.getName();

                    finalCat.setName(newName);
                    finalCat.setType(newType);
                    finalCat.setIconName(newIcon);

                    // Update in SQLite
                    dbHelper.updateCategory(finalCat, oldName);

                    // Update current screen state
                    categoryName = newName;
                    categoryType = newType;
                    loadCategoryData();

                    Toast.makeText(CategoryDetailsActivity.this, "Category updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCategoryDialog() {
        CategoryModel category = dbHelper.getCategoryByName(categoryName);
        new AlertDialog.Builder(this)
                .setTitle("Delete Category?")
                .setMessage("Are you sure you want to delete \"" + categoryName + "\"?\n\nAny existing transactions in this category will be preserved under 'Other " + ("INCOME".equalsIgnoreCase(categoryType) ? "Income" : "Expense") + "'.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (category != null) {
                        dbHelper.deleteCategory(category.getId(), category.getName(), category.getType());
                    }
                    Toast.makeText(CategoryDetailsActivity.this, "Category \"" + categoryName + "\" deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                    loadCategoryData();
                    Toast.makeText(CategoryDetailsActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
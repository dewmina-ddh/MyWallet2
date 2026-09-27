package com.example.mywallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.io.File;

/**
 * MainActivity serves as the primary Overview / Dashboard of MyWallet.
 * It displays real-time Net Balance, Total Income, Total Expenses, and the list of Recent Transactions.
 */
public class MainActivity extends AppCompatActivity implements RecordTransactionBottomSheet.OnTransactionSavedListener {

    public static boolean isSessionAuthenticated = false;

    private TextView tvNetBalance, tvTotalIncome, tvTotalExpenses;
    private RecyclerView recyclerViewTransactions;
    private RecyclerView recyclerViewMainCategories;
    private TextView tvEmptyTransactions;
    private ImageView btnProfileMain, btnNotificationMain;
    private TransactionAdapter transactionAdapter;
    private MainCategoryAdapter mainCategoryAdapter;
    private DatabaseHelper dbHelper;
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            View bottomNav = findViewById(R.id.bottomNavigation);
            if (bottomNav != null) {
                int baseHeight = (int) (80 * getResources().getDisplayMetrics().density);
                android.view.ViewGroup.LayoutParams lp = bottomNav.getLayoutParams();
                if (lp != null) {
                    lp.height = baseHeight + systemBars.bottom;
                    bottomNav.setLayoutParams(lp);
                }
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }
            return insets;
        });

        // Initialize Database Helper
        dbHelper = new DatabaseHelper(this);

        // Check Biometric Security if enabled
        checkBiometricSecurity();

        // Bind Views
        tvNetBalance = findViewById(R.id.tvNetBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions);
        recyclerViewMainCategories = findViewById(R.id.recyclerViewMainCategories);
        tvEmptyTransactions = findViewById(R.id.tvEmptyTransactions);
        btnProfileMain = findViewById(R.id.btnProfileMain);
        btnNotificationMain = findViewById(R.id.btnNotificationMain);

        // Top Header Profile Button
        if (btnProfileMain != null) {
            btnProfileMain.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // Top Header Notification Bell
        if (btnNotificationMain != null) {
            btnNotificationMain.setOnClickListener(v -> showNotificationsDialog());
        }

        // View All Transactions Button
        View btnViewAllTransactions = findViewById(R.id.btnViewAllTransactions);
        if (btnViewAllTransactions != null) {
            btnViewAllTransactions.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AllTransactionsActivity.class);
                startActivity(intent);
            });
        }

        // Setup Transactions RecyclerView
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
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
        recyclerViewTransactions.setAdapter(transactionAdapter);

        // Setup Categories Horizontal RecyclerView
        if (recyclerViewMainCategories != null) {
            recyclerViewMainCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            mainCategoryAdapter = new MainCategoryAdapter(this, null, category -> {
                Intent intent = new Intent(MainActivity.this, CategoryDetailsActivity.class);
                intent.putExtra("category_name", category.getName());
                intent.putExtra("category_type", category.getType());
                intent.putExtra("category_id", category.getId());
                intent.putExtra("category_icon", category.getIconName());
                startActivity(intent);
            });
            recyclerViewMainCategories.setAdapter(mainCategoryAdapter);
        }

        // Load initial data
        loadDashboardData();
        loadSavedAvatar();

        // 1. Navigation to Categories Screen
        View btnViewAllCategories = findViewById(R.id.btnViewAllCategories);
        if (btnViewAllCategories != null) {
            btnViewAllCategories.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                startActivity(intent);
            });
        }

        View navHexagon = findViewById(R.id.navHexagon);
        if (navHexagon != null) {
            navHexagon.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // 2. Navigation to Insights Screen
        View navGraph = findViewById(R.id.navGraph);
        if (navGraph != null) {
            navGraph.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, InsightsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // 3. Navigation to Profile Screen
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // 4. Record Income when clicking Total Income Card
        View cardTotalIncome = findViewById(R.id.cardTotalIncome);
        if (cardTotalIncome != null) {
            cardTotalIncome.setOnClickListener(v -> openRecordTransactionSheet(true));
        }

        // 5. Record Expense when clicking Total Expenses Card
        View cardTotalExpenses = findViewById(R.id.cardTotalExpenses);
        if (cardTotalExpenses != null) {
            cardTotalExpenses.setOnClickListener(v -> openRecordTransactionSheet(false));
        }

        // 6. Floating Action Button '+'
        View btnNavPlus = findViewById(R.id.btnNavPlus);
        if (btnNavPlus != null) {
            btnNavPlus.setOnClickListener(v -> openRecordTransactionSheet(false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh balances, recent transactions, and avatar whenever returning to this screen
        loadDashboardData();
        loadSavedAvatar();
    }

    @Override
    public void onTransactionSaved() {
        // Callback from BottomSheet after successful Add / Update
        loadDashboardData();
    }

    /**
     * Query SQLite database for updated Income, Expense, Net Balance and Recent Transactions.
     */
    private void loadDashboardData() {
        if (dbHelper == null) return;

        double income = dbHelper.getTotalIncome();
        double expense = dbHelper.getTotalExpense();
        double netBalance = income - expense;

        tvTotalIncome.setText(CurrencyHelper.formatAmount(this, income));
        tvTotalExpenses.setText(CurrencyHelper.formatAmount(this, expense));
        tvNetBalance.setText(CurrencyHelper.formatAmount(this, netBalance));

        // Load recent 5 transactions for dashboard
        List<TransactionModel> recentList = dbHelper.getRecentTransactions(5);
        if (recentList.isEmpty()) {
            recyclerViewTransactions.setVisibility(View.GONE);
            tvEmptyTransactions.setVisibility(View.VISIBLE);
        } else {
            recyclerViewTransactions.setVisibility(View.VISIBLE);
            tvEmptyTransactions.setVisibility(View.GONE);
            transactionAdapter.updateData(recentList);
        }

        // Load dynamic categories for overview
        if (mainCategoryAdapter != null) {
            List<CategoryModel> categoryList = dbHelper.getAllCategories();
            mainCategoryAdapter.updateData(categoryList);
        }
    }

    private void checkBiometricSecurity() {
        SharedPreferences prefs = getSharedPreferences("my_wallet_prefs", MODE_PRIVATE);
        boolean isAppLocked = prefs.getBoolean("is_app_locked", false);

        if (isAppLocked && !isSessionAuthenticated) {
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock MyWallet")
                    .setSubtitle("Use your fingerprint or biometric credentials to continue")
                    .setNegativeButtonText("Exit")
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this,
                    ContextCompat.getMainExecutor(MainActivity.this),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Toast.makeText(MainActivity.this, "Authentication required to open MyWallet", Toast.LENGTH_SHORT).show();
                            finishAffinity();
                        }

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            isSessionAuthenticated = true;
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                        }
                    });

            biometricPrompt.authenticate(promptInfo);
        }
    }

    private void loadSavedAvatar() {
        if (btnProfileMain == null) return;
        try {
            File file = new File(getFilesDir(), "profile_avatar.jpg");
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bitmap != null) {
                    Bitmap circularBitmap = ImageUtils.getCircularBitmap(bitmap);
                    btnProfileMain.setImageBitmap(circularBitmap);
                    return;
                }
            }
            btnProfileMain.setImageResource(R.drawable.ic_user_profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotificationsDialog() {
        double income = dbHelper != null ? dbHelper.getTotalIncome() : 0.0;
        double expense = dbHelper != null ? dbHelper.getTotalExpense() : 0.0;
        int count = dbHelper != null ? dbHelper.getTransactionsCount() : 0;

        String message = "✨ Welcome back!\n\n" +
                "• Recorded Transactions: " + count + "\n" +
                "• Total Income: " + CurrencyHelper.formatAmount(this, income) + "\n" +
                "• Total Expenses: " + CurrencyHelper.formatAmount(this, expense) + "\n\n" +
                "💡 Tip: Review your spending habits in the Insights tab regularly to achieve your savings goals!";

        new AlertDialog.Builder(this)
                .setTitle("Notifications & Alerts")
                .setMessage(message)
                .setIcon(R.drawable.ic_bell)
                .setPositiveButton("Got it", null)
                .show();
    }

    private void openRecordTransactionSheet(boolean isIncome) {
        RecordTransactionBottomSheet sheet = RecordTransactionBottomSheet.newInstance(isIncome);
        sheet.setOnTransactionSavedListener(this);
        sheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
    }

    /**
     * Options dialog for selecting Edit, Delete, or viewing details of a transaction.
     */
    private void showTransactionOptionsDialog(TransactionModel transaction) {
        String[] options = {"Edit Transaction", "Delete Transaction"};
        new AlertDialog.Builder(this)
                .setTitle(transaction.getTitle())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Edit Transaction
                        RecordTransactionBottomSheet sheet = RecordTransactionBottomSheet.newInstanceForEdit(transaction.getId());
                        sheet.setOnTransactionSavedListener(this);
                        sheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
                    } else if (which == 1) {
                        // Delete Transaction
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
                    loadDashboardData();
                    Toast.makeText(MainActivity.this, "Transaction deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
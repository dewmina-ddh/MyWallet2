package com.example.mywallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * InsightsActivity displays spending analytics, charts, and top spending breakdowns from SQLite.
 */
public class InsightsActivity extends AppCompatActivity {

    private TextView tabDay, tabWeek, tabMonth, tabYear;
    private TextView tvFoodTotal, tvTravelTotal, tvUtilitiesTotal, tvShoppingTotal;
    private TextView tvFoodPercent, tvTravelPercent, tvUtilitiesPercent, tvShoppingPercent;
    private ProgressBar progressFood, progressTravel, progressUtilities, progressShopping;
    private TextView tvSummaryTitle, tvSummaryDescription;
    private TextView tvChartPeriodLabel, tvChartTotalAmount;
    private BarChartView barChartView;
    private DatabaseHelper dbHelper;
    private String currentPeriod = "MONTH"; // "DAY", "WEEK", "MONTH", "YEAR"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_insights);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.insightsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            View bottomNav = findViewById(R.id.bottomNavigationInsights);
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

        dbHelper = new DatabaseHelper(this);

        // Setup Time Filter Tabs
        tabDay = findViewById(R.id.tabDay);
        tabWeek = findViewById(R.id.tabWeek);
        tabMonth = findViewById(R.id.tabMonth);
        tabYear = findViewById(R.id.tabYear);

        // Chart views
        barChartView = findViewById(R.id.barChartView);
        tvChartPeriodLabel = findViewById(R.id.tvChartPeriodLabel);
        tvChartTotalAmount = findViewById(R.id.tvChartTotalAmount);

        tabDay.setOnClickListener(v -> {
            currentPeriod = "DAY";
            selectTab(tabDay);
            loadChartData();
        });
        tabWeek.setOnClickListener(v -> {
            currentPeriod = "WEEK";
            selectTab(tabWeek);
            loadChartData();
        });
        tabMonth.setOnClickListener(v -> {
            currentPeriod = "MONTH";
            selectTab(tabMonth);
            loadChartData();
        });
        tabYear.setOnClickListener(v -> {
            currentPeriod = "YEAR";
            selectTab(tabYear);
            loadChartData();
        });

        // Spending Area TextViews & ProgressBars
        tvFoodTotal = findViewById(R.id.tvFoodTotal);
        tvTravelTotal = findViewById(R.id.tvTravelTotal);
        tvUtilitiesTotal = findViewById(R.id.tvUtilitiesTotal);
        tvShoppingTotal = findViewById(R.id.tvShoppingTotal);

        tvFoodPercent = findViewById(R.id.tvFoodPercent);
        tvTravelPercent = findViewById(R.id.tvTravelPercent);
        tvUtilitiesPercent = findViewById(R.id.tvUtilitiesPercent);
        tvShoppingPercent = findViewById(R.id.tvShoppingPercent);

        progressFood = findViewById(R.id.progressFood);
        progressTravel = findViewById(R.id.progressTravel);
        progressUtilities = findViewById(R.id.progressUtilities);
        progressShopping = findViewById(R.id.progressShopping);

        tvSummaryTitle = findViewById(R.id.tvSummaryTitle);
        tvSummaryDescription = findViewById(R.id.tvSummaryDescription);

        loadInsightsData();
        loadChartData();

        // Navigate to Home / Overview
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(InsightsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        // Navigate to Categories
        View navHexagon = findViewById(R.id.navHexagon);
        if (navHexagon != null) {
            navHexagon.setOnClickListener(v -> {
                Intent intent = new Intent(InsightsActivity.this, CategoriesActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        // Open Record Transaction Bottom Sheet
        View btnNavPlus = findViewById(R.id.btnNavPlus);
        if (btnNavPlus != null) {
            btnNavPlus.setOnClickListener(v -> {
                RecordTransactionBottomSheet bottomSheet = RecordTransactionBottomSheet.newInstance(false);
                bottomSheet.setOnTransactionSavedListener(() -> {
                    loadInsightsData();
                    loadChartData();
                });
                bottomSheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
            });
        }

        // Open Profile Screen
        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(InsightsActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        // More 3-Dots Button
        View btnMore = findViewById(R.id.btnMore);
        if (btnMore != null) {
            btnMore.setOnClickListener(v -> Toast.makeText(this, "Analytics options", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInsightsData();
        loadChartData();
    }

    /**
     * Compute live totals from SQLite for top spending categories and auto-calculate percentages.
     */
    private void loadInsightsData() {
        if (dbHelper == null) return;

        double totalExpense = dbHelper.getTotalExpense();
        double food = dbHelper.getTotalForCategory("Food & Dining");
        double travel = dbHelper.getTotalForCategory("Travel & Transport");
        double utilities = dbHelper.getTotalForCategory("Utilities & Bills");
        double shopping = dbHelper.getTotalForCategory("Shopping");

        if (tvFoodTotal != null) tvFoodTotal.setText(CurrencyHelper.formatAmount(this, food));
        if (tvTravelTotal != null) tvTravelTotal.setText(CurrencyHelper.formatAmount(this, travel));
        if (tvUtilitiesTotal != null) tvUtilitiesTotal.setText(CurrencyHelper.formatAmount(this, utilities));
        if (tvShoppingTotal != null) tvShoppingTotal.setText(CurrencyHelper.formatAmount(this, shopping));

        if (totalExpense <= 0) {
            if (progressFood != null) progressFood.setProgress(0);
            if (progressTravel != null) progressTravel.setProgress(0);
            if (progressUtilities != null) progressUtilities.setProgress(0);
            if (progressShopping != null) progressShopping.setProgress(0);

            if (tvFoodPercent != null) tvFoodPercent.setText("0%");
            if (tvTravelPercent != null) tvTravelPercent.setText("0%");
            if (tvUtilitiesPercent != null) tvUtilitiesPercent.setText("0%");
            if (tvShoppingPercent != null) tvShoppingPercent.setText("0%");

            if (tvSummaryTitle != null) tvSummaryTitle.setText("No Expense Activity");
            if (tvSummaryDescription != null) {
                tvSummaryDescription.setText("You have no expenses recorded yet. Once you add transactions, your highest spending categories and breakdowns will appear here automatically.");
            }
        } else {
            int foodP = (int) Math.round((food / totalExpense) * 100);
            int travelP = (int) Math.round((travel / totalExpense) * 100);
            int utilitiesP = (int) Math.round((utilities / totalExpense) * 100);
            int shoppingP = (int) Math.round((shopping / totalExpense) * 100);

            if (progressFood != null) progressFood.setProgress(foodP);
            if (progressTravel != null) progressTravel.setProgress(travelP);
            if (progressUtilities != null) progressUtilities.setProgress(utilitiesP);
            if (progressShopping != null) progressShopping.setProgress(shoppingP);

            if (tvFoodPercent != null) tvFoodPercent.setText(foodP + "%");
            if (tvTravelPercent != null) tvTravelPercent.setText(travelP + "%");
            if (tvUtilitiesPercent != null) tvUtilitiesPercent.setText(utilitiesP + "%");
            if (tvShoppingPercent != null) tvShoppingPercent.setText(shoppingP + "%");

            // Determine highest spending category dynamically across all expense categories
            String topCat = "Food & Dining";
            double topAmount = food;
            int topPercent = foodP;

            java.util.List<CategoryModel> expenseCategories = dbHelper.getCategoriesByType("EXPENSE");
            for (CategoryModel cat : expenseCategories) {
                double amount = dbHelper.getTotalForCategory(cat.getName());
                if (amount > topAmount) {
                    topCat = cat.getName();
                    topAmount = amount;
                    topPercent = (int) Math.round((topAmount / totalExpense) * 100);
                }
            }

            if (tvSummaryTitle != null) tvSummaryTitle.setText("Top Expense Category: " + topCat);
            if (tvSummaryDescription != null) {
                tvSummaryDescription.setText("Your largest spending area is " + topCat + " (" + CurrencyHelper.formatAmount(this, topAmount) + "), making up " + topPercent + "% of your total expenses.");
            }
        }
    }

    /**
     * Fetch real-time spending data from SQLite based on selected period and render on BarChartView.
     */
    private void loadChartData() {
        if (dbHelper == null || barChartView == null) return;

        java.util.List<BarChartView.BarItem> barItems;
        String periodTitle;

        switch (currentPeriod) {
            case "DAY":
                barItems = dbHelper.getDailySpendingForWeek();
                periodTitle = "This Week's Daily Spending";
                break;
            case "WEEK":
                barItems = dbHelper.getWeeklySpendingForMonth();
                periodTitle = "This Month's Weekly Spending";
                break;
            case "YEAR":
                barItems = dbHelper.getYearlySpending();
                periodTitle = "Annual Spending Trends";
                break;
            case "MONTH":
            default:
                barItems = dbHelper.getMonthlySpendingForYear();
                periodTitle = "This Year's Monthly Spending";
                break;
        }

        double totalPeriodSpending = 0.0;
        for (BarChartView.BarItem item : barItems) {
            totalPeriodSpending += item.value;
        }

        if (tvChartPeriodLabel != null) tvChartPeriodLabel.setText(periodTitle);
        if (tvChartTotalAmount != null) tvChartTotalAmount.setText(CurrencyHelper.formatAmount(this, totalPeriodSpending));

        String symbol = CurrencyHelper.getCurrencySymbol(this);
        barChartView.setItems(barItems, symbol);
    }

    private void selectTab(TextView selected) {
        TextView[] tabs = {tabDay, tabWeek, tabMonth, tabYear};
        for (TextView tab : tabs) {
            if (tab == selected) {
                tab.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_pill_filter_active));
                tab.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                tab.setBackground(null);
                tab.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        }
    }
}
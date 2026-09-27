package com.example.mywallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

import java.util.ArrayList;
import java.util.List;

/**
 * CategoriesActivity displays all expense and income categories,
 * calculates live total spending/earnings per category, and allows adding/searching/deleting categories.
 */
public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCategories;
    private TextView tvEmptyCategories;
    private EditText etSearch;
    private CategoryAdapter categoryAdapter;
    private DatabaseHelper dbHelper;
    private List<CategoryModel> allCategoriesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_categories);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.categoriesMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            View bottomNav = findViewById(R.id.bottomNavigationCategories);
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

        // Bind Views
        recyclerViewCategories = findViewById(R.id.recyclerViewCategories);
        tvEmptyCategories = findViewById(R.id.tvEmptyCategories);
        etSearch = findViewById(R.id.etSearch);

        // Setup RecyclerView & Adapter
        recyclerViewCategories.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(this, null, dbHelper, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(CategoryModel category) {
                Intent intent = new Intent(CategoriesActivity.this, CategoryDetailsActivity.class);
                intent.putExtra("category_name", category.getName());
                intent.putExtra("category_type", category.getType());
                intent.putExtra("category_id", category.getId());
                intent.putExtra("category_icon", category.getIconName());
                startActivity(intent);
            }

            @Override
            public void onCategoryEdit(CategoryModel category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onCategoryDelete(CategoryModel category) {
                showDeleteCategoryDialog(category);
            }
        });
        recyclerViewCategories.setAdapter(categoryAdapter);

        // Load Categories from Database
        loadCategories();

        // Real-Time Search Filter
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCategories(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Add Category Button Handler
        View btnAddCategory = findViewById(R.id.btnAddCategory);
        if (btnAddCategory != null) {
            btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
        }

        // Bottom Navigation Listeners
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(CategoriesActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        View navGraph = findViewById(R.id.navGraph);
        if (navGraph != null) {
            navGraph.setOnClickListener(v -> {
                Intent intent = new Intent(CategoriesActivity.this, InsightsActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        View navProfile = findViewById(R.id.navProfile);
        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                Intent intent = new Intent(CategoriesActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        View btnNavPlus = findViewById(R.id.btnNavPlus);
        if (btnNavPlus != null) {
            btnNavPlus.setOnClickListener(v -> {
                RecordTransactionBottomSheet bottomSheet = RecordTransactionBottomSheet.newInstance(true);
                bottomSheet.setOnTransactionSavedListener(() -> loadCategories());
                bottomSheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        if (dbHelper == null) return;
        allCategoriesList = dbHelper.getAllCategories();
        if (allCategoriesList.isEmpty()) {
            recyclerViewCategories.setVisibility(View.GONE);
            tvEmptyCategories.setVisibility(View.VISIBLE);
        } else {
            recyclerViewCategories.setVisibility(View.VISIBLE);
            tvEmptyCategories.setVisibility(View.GONE);
            categoryAdapter.updateData(allCategoriesList);
        }
    }

    private void filterCategories(String query) {
        if (query == null || query.trim().isEmpty()) {
            categoryAdapter.updateData(allCategoriesList);
            tvEmptyCategories.setVisibility(allCategoriesList.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }

        List<CategoryModel> filtered = new ArrayList<>();
        for (CategoryModel cat : allCategoriesList) {
            if (cat.getName().toLowerCase().contains(query.toLowerCase().trim())) {
                filtered.add(cat);
            }
        }

        if (filtered.isEmpty()) {
            recyclerViewCategories.setVisibility(View.GONE);
            tvEmptyCategories.setVisibility(View.VISIBLE);
        } else {
            recyclerViewCategories.setVisibility(View.VISIBLE);
            tvEmptyCategories.setVisibility(View.GONE);
            categoryAdapter.updateData(filtered);
        }
    }

    /**
     * Dialog to create a new custom category (Expense or Income) with custom icon selection.
     */
    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText etName = dialogView.findViewById(R.id.etDialogCategoryName);
        RadioButton rbExpense = dialogView.findViewById(R.id.rbDialogExpense);
        RadioButton rbIncome = dialogView.findViewById(R.id.rbDialogIncome);

        final String[] selectedIcon = {"ic_category_food"};
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

        new AlertDialog.Builder(this)
                .setTitle("Add New Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(CategoriesActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";
                    String icon = selectedIcon[0];

                    // Insert into SQLite
                    dbHelper.addCategory(new CategoryModel(name, type, icon));

                    // Clear search bar and immediately reload the live list
                    if (etSearch != null) {
                        etSearch.setText("");
                    }
                    loadCategories();

                    Toast.makeText(CategoriesActivity.this, "Category \"" + name + "\" added successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Dialog to update/edit an existing category with preloaded values and custom icon selection.
     */
    private void showEditCategoryDialog(CategoryModel category) {
        if (category == null) return;

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

        new AlertDialog.Builder(this)
                .setTitle("Edit Category")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(CategoriesActivity.this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String newType = rbIncome.isChecked() ? "INCOME" : "EXPENSE";
                    String newIcon = selectedIcon[0];
                    String oldName = category.getName();

                    category.setName(newName);
                    category.setType(newType);
                    category.setIconName(newIcon);

                    // Update in SQLite
                    dbHelper.updateCategory(category, oldName);

                    // Reload list
                    loadCategories();

                    Toast.makeText(CategoriesActivity.this, "Category updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCategoryDialog(CategoryModel category) {
        if (category == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Category?")
                .setMessage("Are you sure you want to delete \"" + category.getName() + "\"?\n\nAny existing transactions in this category will be preserved under 'Other " + (category.getType().equalsIgnoreCase("INCOME") ? "Income" : "Expense") + "'.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteCategory(category.getId(), category.getName(), category.getType());
                    loadCategories();
                    Toast.makeText(CategoriesActivity.this, "Category \"" + category.getName() + "\" deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
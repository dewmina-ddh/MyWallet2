package com.example.mywallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * ProfileActivity manages user profile avatar, name, security (Biometric PIN lock),
 * local data metrics from SQLite, and PDF Statement export.
 */
public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvCurrencyValue, tvTotalRecords, tvTotalCategories;
    private ImageView imgAvatar;
    private SwitchCompat switchReminder, switchSecurity;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    // Activity Result Launchers for Gallery & Camera
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            View bottomNav = findViewById(R.id.bottomNavigationProfile);
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
        prefs = getSharedPreferences("my_wallet_prefs", MODE_PRIVATE);

        tvUserName = findViewById(R.id.tvUserName);
        tvCurrencyValue = findViewById(R.id.tvCurrencyValue);
        tvTotalRecords = findViewById(R.id.tvTotalRecords);
        tvTotalCategories = findViewById(R.id.tvTotalCategories);
        imgAvatar = findViewById(R.id.imgAvatar);
        switchReminder = findViewById(R.id.switchReminder);
        switchSecurity = findViewById(R.id.switchSecurity);

        // Load saved preferences
        String savedName = prefs.getString("user_name", "Damsith Dewmina");
        tvUserName.setText(savedName);

        String savedCurrency = prefs.getString("currency", "USD ($)");
        tvCurrencyValue.setText(savedCurrency);

        boolean isAppLocked = prefs.getBoolean("is_app_locked", false);
        switchSecurity.setChecked(isAppLocked);

        boolean reminderEnabled = prefs.getBoolean("reminder_enabled", true);
        switchReminder.setChecked(reminderEnabled);

        // Load custom avatar if saved
        loadSavedAvatar();

        // Setup Photo Pickers
        setupImagePickers();

        // Avatar Click -> Change Photo
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> showAvatarSourceDialog());
        }

        // Edit Profile Name Button
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditNameDialog());
        }

        // Currency Selector
        View rowCurrency = findViewById(R.id.rowCurrency);
        if (rowCurrency != null) {
            rowCurrency.setOnClickListener(v -> showCurrencyDialog());
        }

        // Reminder Switch
        if (switchReminder != null) {
            switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("reminder_enabled", isChecked).apply();
                String msg = isChecked ? "Daily 8:00 PM reminder enabled" : "Daily reminder disabled";
                Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        // Biometric / Fingerprint Lock Switch
        if (switchSecurity != null) {
            switchSecurity.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("is_app_locked", isChecked).apply();
                String msg = isChecked ? "Fingerprint / PIN Lock enabled" : "App Lock disabled";
                Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        // PDF Statement Export
        View rowExport = findViewById(R.id.rowExport);
        if (rowExport != null) {
            rowExport.setOnClickListener(v -> {
                Toast.makeText(ProfileActivity.this, "Generating Financial Statement PDF...", Toast.LENGTH_SHORT).show();
                PdfExportHelper.exportAndSharePdf(ProfileActivity.this, tvUserName.getText().toString());
            });
        }

        // Backup & Restore
        View rowBackup = findViewById(R.id.rowBackup);
        if (rowBackup != null) {
            rowBackup.setOnClickListener(v -> {
                new AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("Backup & Restore")
                        .setMessage("Create a local database backup or export statement.")
                        .setPositiveButton("Export PDF", (dialog, which) -> 
                            PdfExportHelper.exportAndSharePdf(ProfileActivity.this, tvUserName.getText().toString())
                        )
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Clear All Records (Reset Data in SQLite)
        View rowResetData = findViewById(R.id.rowResetData);
        if (rowResetData != null) {
            rowResetData.setOnClickListener(v -> {
                new AlertDialog.Builder(ProfileActivity.this)
                        .setTitle("Clear All Records?")
                        .setMessage("Are you sure you want to delete all income and expense transactions on this phone? This cannot be undone.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton("Clear All", (dialog, which) -> {
                            dbHelper.clearAllTransactions();
                            loadProfileMetrics();
                            Toast.makeText(ProfileActivity.this, "All records cleared successfully.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Bottom Navigation Listeners
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        View navHexagon = findViewById(R.id.navHexagon);
        if (navHexagon != null) {
            navHexagon.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, CategoriesActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }

        View btnNavPlus = findViewById(R.id.btnNavPlus);
        if (btnNavPlus != null) {
            btnNavPlus.setOnClickListener(v -> {
                RecordTransactionBottomSheet bottomSheet = RecordTransactionBottomSheet.newInstance(false);
                bottomSheet.setOnTransactionSavedListener(() -> loadProfileMetrics());
                bottomSheet.show(getSupportFragmentManager(), "RecordTransactionBottomSheet");
            });
        }

        View navGraph = findViewById(R.id.navGraph);
        if (navGraph != null) {
            navGraph.setOnClickListener(v -> {
                Intent intent = new Intent(ProfileActivity.this, InsightsActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, 0);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileMetrics();
    }

    private void loadProfileMetrics() {
        if (dbHelper == null) return;
        int recordsCount = dbHelper.getTransactionsCount();
        int categoriesCount = dbHelper.getCategoriesCount();

        if (tvTotalRecords != null) tvTotalRecords.setText(String.valueOf(recordsCount));
        if (tvTotalCategories != null) tvTotalCategories.setText(String.valueOf(categoriesCount));
    }

    private void setupImagePickers() {
        // Gallery Picker
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                saveImageFromUri(uri);
            }
        });

        // Camera Launcher
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                saveImageBitmap(bitmap);
            }
        });
    }

    private void showAvatarSourceDialog() {
        String[] options = {"Choose from Gallery", "Take a Photo with Camera", "Reset to Default Avatar"};
        new AlertDialog.Builder(this)
                .setTitle("Profile Avatar")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        galleryLauncher.launch("image/*");
                    } else if (which == 1) {
                        cameraLauncher.launch(null);
                    } else {
                        File file = new File(getFilesDir(), "profile_avatar.jpg");
                        if (file.exists()) file.delete();
                        imgAvatar.setImageResource(R.drawable.ic_avatar_gold);
                        Toast.makeText(this, "Reset to default avatar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveImageFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                saveImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageBitmap(Bitmap bitmap) {
        try {
            Bitmap circularBitmap = ImageUtils.getCircularBitmap(bitmap);
            File file = new File(getFilesDir(), "profile_avatar.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            imgAvatar.setImageBitmap(circularBitmap);
            Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedAvatar() {
        File file = new File(getFilesDir(), "profile_avatar.jpg");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                Bitmap circularBitmap = ImageUtils.getCircularBitmap(bitmap);
                imgAvatar.setImageBitmap(circularBitmap);
            }
        }
    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setText(tvUserName.getText());
        input.setSelection(input.getText().length());
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(this)
                .setTitle("Edit Display Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        tvUserName.setText(newName);
                        prefs.edit().putString("user_name", newName).apply();
                        Toast.makeText(this, "Profile name updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCurrencyDialog() {
        String[] currencies = CurrencyHelper.SUPPORTED_CURRENCIES;
        new AlertDialog.Builder(this)
                .setTitle("Select Currency")
                .setItems(currencies, (dialog, which) -> {
                    String oldCurrency = prefs.getString("currency", "USD ($)");
                    String newCurrency = currencies[which];

                    if (!oldCurrency.equals(newCurrency)) {
                        double oldRate = CurrencyHelper.getExchangeRate(oldCurrency);
                        double newRate = CurrencyHelper.getExchangeRate(newCurrency);
                        double multiplier = newRate / oldRate;

                        // Automatically convert all recorded transaction amounts in SQLite
                        dbHelper.convertAllTransactionAmounts(multiplier);

                        prefs.edit().putString("currency", newCurrency).apply();
                        tvCurrencyValue.setText(newCurrency);
                        Toast.makeText(ProfileActivity.this, "Converted records to " + newCurrency, Toast.LENGTH_SHORT).show();
                        loadProfileMetrics();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
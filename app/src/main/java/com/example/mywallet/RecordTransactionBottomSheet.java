package com.example.mywallet;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecordTransactionBottomSheet provides full Create and Update operations for Income and Expense transactions.
 */
public class RecordTransactionBottomSheet extends BottomSheetDialogFragment {

    public interface OnTransactionSavedListener {
        void onTransactionSaved();
    }

    private boolean isIncomeMode = true;
    private int editTransactionId = -1;

    private TextView tvSheetTitle;
    private TextView tvSheetSubtitle;
    private TextView tabIncome;
    private TextView tabExpense;
    private EditText etTitle;
    private TextView tvCategory;
    private EditText etAmount;
    private TextView tvDate;
    private TextView btnSave;

    private DatabaseHelper dbHelper;
    private OnTransactionSavedListener listener;
    private long selectedTimestamp = System.currentTimeMillis();

    public static RecordTransactionBottomSheet newInstance(boolean isIncome) {
        RecordTransactionBottomSheet sheet = new RecordTransactionBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean("is_income", isIncome);
        args.putInt("edit_id", -1);
        sheet.setArguments(args);
        return sheet;
    }

    public static RecordTransactionBottomSheet newInstanceForEdit(int transactionId) {
        RecordTransactionBottomSheet sheet = new RecordTransactionBottomSheet();
        Bundle args = new Bundle();
        args.putInt("edit_id", transactionId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnTransactionSavedListener(OnTransactionSavedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        dbHelper = new DatabaseHelper(context);
        if (context instanceof OnTransactionSavedListener && listener == null) {
            listener = (OnTransactionSavedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_record_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            isIncomeMode = getArguments().getBoolean("is_income", true);
            editTransactionId = getArguments().getInt("edit_id", -1);
        }

        tvSheetTitle = view.findViewById(R.id.tvSheetTitle);
        tvSheetSubtitle = view.findViewById(R.id.tvSheetSubtitle);
        tabIncome = view.findViewById(R.id.tabIncome);
        tabExpense = view.findViewById(R.id.tabExpense);
        etTitle = view.findViewById(R.id.etTitle);
        tvCategory = view.findViewById(R.id.tvCategory);
        etAmount = view.findViewById(R.id.etAmount);
        tvDate = view.findViewById(R.id.tvDate);
        btnSave = view.findViewById(R.id.btnSave);

        // Set default today date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(new Date()));

        // Switch Tab Listeners
        tabIncome.setOnClickListener(v -> {
            updateModeUI(true);
            tvCategory.setText("Select Category");
        });

        tabExpense.setOnClickListener(v -> {
            updateModeUI(false);
            tvCategory.setText("Select Category");
        });

        // Category Selector
        View layoutCategory = view.findViewById(R.id.layoutCategory);
        if (layoutCategory != null) {
            layoutCategory.setOnClickListener(v -> showCategorySelector());
        }

        // Date Picker
        View layoutDate = view.findViewById(R.id.layoutDate);
        if (layoutDate != null) {
            layoutDate.setOnClickListener(v -> showDatePicker());
        }

        // Check if we are in EDIT mode
        if (editTransactionId != -1) {
            loadTransactionForEditing(editTransactionId);
        } else {
            updateModeUI(isIncomeMode);
        }

        // Save Button Handler (CREATE or UPDATE)
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveTransaction());
        }
    }

    private void loadTransactionForEditing(int id) {
        TransactionModel trans = dbHelper.getTransactionById(id);
        if (trans != null) {
            isIncomeMode = "INCOME".equalsIgnoreCase(trans.getType());
            updateModeUI(isIncomeMode);

            tvSheetTitle.setText(isIncomeMode ? "Edit Income" : "Edit Expense");
            tvSheetSubtitle.setText("Update transaction details.");
            btnSave.setText("Update Transaction");

            etTitle.setText(trans.getTitle());
            etAmount.setText(String.valueOf(trans.getAmount()));
            tvCategory.setText(trans.getCategory());
            tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_text));
            tvDate.setText(trans.getDate());
            tvDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_text));
            selectedTimestamp = trans.getTimestamp();
        }
    }

    private void updateModeUI(boolean isIncome) {
        this.isIncomeMode = isIncome;
        if (getContext() == null) return;

        if (isIncome) {
            tvSheetTitle.setText(editTransactionId != -1 ? "Edit Income" : "Record Income");
            tvSheetSubtitle.setText("Track how much you've earned.");
            etTitle.setHint("e.g. \"Salary\", \"Freelance Project\"");

            tabIncome.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_income_active));
            tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            tabExpense.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_inactive));
            tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_text));
        } else {
            tvSheetTitle.setText(editTransactionId != -1 ? "Edit Expense" : "Record Expense");
            tvSheetSubtitle.setText("Track where your money goes.");
            etTitle.setHint("e.g. Groceries, Electric Bill");

            tabExpense.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_expense_active));
            tabExpense.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            tabIncome.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_tab_inactive));
            tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_text));
        }
    }

    private void showCategorySelector() {
        String type = isIncomeMode ? "INCOME" : "EXPENSE";
        List<CategoryModel> categoryList = dbHelper.getCategoriesByType(type);

        String[] categoryNames = new String[categoryList.size()];
        for (int i = 0; i < categoryList.size(); i++) {
            categoryNames[i] = categoryList.get(i).getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(isIncomeMode ? "Select Income Category" : "Select Expense Category");
        builder.setItems(categoryNames, (dialog, which) -> {
            tvCategory.setText(categoryNames[which]);
            tvCategory.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_text));
        });
        builder.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedTimestamp);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(selectedYear, selectedMonth, selectedDay);
                    selectedTimestamp = picked.getTimeInMillis();

                    String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    String formattedDate = months[selectedMonth] + " " + selectedDay + ", " + selectedYear;
                    tvDate.setText(formattedDate);
                    tvDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_text));
                }, year, month, day);
        datePickerDialog.show();
    }

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String category = tvCategory.getText().toString().trim();
        String dateStr = tvDate.getText().toString().trim();
        String paymentMethod = isIncomeMode ? "Income" : "Expense";

        if (title.isEmpty()) {
            etTitle.setError("Please enter a title");
            return;
        }

        if (amountStr.isEmpty()) {
            etAmount.setError("Please enter an amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid number");
            return;
        }

        if (category.equals("Select Category") || category.isEmpty()) {
            category = isIncomeMode ? "Other Income" : "Other Expense";
        }

        String type = isIncomeMode ? "INCOME" : "EXPENSE";

        // Perform Database Save or Update
        if (editTransactionId != -1) {
            TransactionModel trans = new TransactionModel(editTransactionId, title, amount, type, category, dateStr, paymentMethod, selectedTimestamp);
            dbHelper.updateTransaction(trans);
            Toast.makeText(getContext(), "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
        } else {
            TransactionModel trans = new TransactionModel(title, amount, type, category, dateStr, paymentMethod, selectedTimestamp);
            dbHelper.addTransaction(trans);
            Toast.makeText(getContext(), (isIncomeMode ? "Income" : "Expense") + " recorded successfully!", Toast.LENGTH_SHORT).show();
        }

        if (listener != null) {
            listener.onTransactionSaved();
        }

        dismiss();
    }
}
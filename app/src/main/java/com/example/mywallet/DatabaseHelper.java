package com.example.mywallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * DatabaseHelper manages SQLite database creation, tables, and CRUD operations
 * for Transactions (Income & Expenses) and Categories.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "my_wallet.db";
    private static final int DATABASE_VERSION = 3;

    // Table: Transactions
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_TRANS_ID = "id";
    public static final String COL_TRANS_TITLE = "title";
    public static final String COL_TRANS_AMOUNT = "amount";
    public static final String COL_TRANS_TYPE = "type"; // "INCOME" or "EXPENSE"
    public static final String COL_TRANS_CATEGORY = "category";
    public static final String COL_TRANS_DATE = "date";
    public static final String COL_TRANS_PAYMENT_METHOD = "payment_method";
    public static final String COL_TRANS_TIMESTAMP = "timestamp";

    // Table: Categories
    public static final String TABLE_CATEGORIES = "categories";
    public static final String COL_CAT_ID = "id";
    public static final String COL_CAT_NAME = "name";
    public static final String COL_CAT_TYPE = "type"; // "INCOME" or "EXPENSE"
    public static final String COL_CAT_ICON = "icon_name";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Create Transactions Table
        String createTransactionsTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " ("
                + COL_TRANS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TRANS_TITLE + " TEXT NOT NULL, "
                + COL_TRANS_AMOUNT + " REAL NOT NULL, "
                + COL_TRANS_TYPE + " TEXT NOT NULL, "
                + COL_TRANS_CATEGORY + " TEXT NOT NULL, "
                + COL_TRANS_DATE + " TEXT, "
                + COL_TRANS_PAYMENT_METHOD + " TEXT, "
                + COL_TRANS_TIMESTAMP + " INTEGER"
                + ");";
        db.execSQL(createTransactionsTable);

        // 2. Create Categories Table
        String createCategoriesTable = "CREATE TABLE " + TABLE_CATEGORIES + " ("
                + COL_CAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CAT_NAME + " TEXT NOT NULL, "
                + COL_CAT_TYPE + " TEXT NOT NULL, "
                + COL_CAT_ICON + " TEXT"
                + ");";
        db.execSQL(createCategoriesTable);

        // 3. Insert Default Categories
        seedDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

    // ==========================================
    // SEEDING DEFAULT DATA
    // ==========================================

    private void seedDefaultCategories(SQLiteDatabase db) {
        // Expense Categories
        insertCategoryInternal(db, "Food & Dining", "EXPENSE", "ic_category_food");
        insertCategoryInternal(db, "Shopping", "EXPENSE", "ic_category_shopping");
        insertCategoryInternal(db, "Travel & Transport", "EXPENSE", "ic_category_transport");
        insertCategoryInternal(db, "Utilities & Bills", "EXPENSE", "ic_category_utilities");
        insertCategoryInternal(db, "Entertainment", "EXPENSE", "ic_category_freelance");
        insertCategoryInternal(db, "Other Expense", "EXPENSE", "ic_field_card");

        // Income Categories
        insertCategoryInternal(db, "Salary & Jobs", "INCOME", "ic_category_freelance");
        insertCategoryInternal(db, "Freelance & Services", "INCOME", "ic_category_freelance");
        insertCategoryInternal(db, "Investments", "INCOME", "ic_category_invest");
        insertCategoryInternal(db, "Other Income", "INCOME", "ic_field_wallet");
    }

    private void insertCategoryInternal(SQLiteDatabase db, String name, String type, String icon) {
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, name);
        values.put(COL_CAT_TYPE, type);
        values.put(COL_CAT_ICON, icon);
        db.insert(TABLE_CATEGORIES, null, values);
    }

    // ==========================================
    // TRANSACTION CRUD OPERATIONS
    // ==========================================

    /**
     * CREATE: Add a new transaction (Income or Expense)
     */
    public long addTransaction(TransactionModel transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TRANS_TITLE, transaction.getTitle());
        values.put(COL_TRANS_AMOUNT, transaction.getAmount());
        values.put(COL_TRANS_TYPE, transaction.getType());
        values.put(COL_TRANS_CATEGORY, transaction.getCategory());
        values.put(COL_TRANS_DATE, transaction.getDate());
        values.put(COL_TRANS_PAYMENT_METHOD, transaction.getPaymentMethod());
        values.put(COL_TRANS_TIMESTAMP, transaction.getTimestamp());

        long result = db.insert(TABLE_TRANSACTIONS, null, values);
        db.close();
        return result;
    }

    /**
     * READ: Get all transactions ordered by timestamp (newest first)
     */
    public List<TransactionModel> getAllTransactions() {
        List<TransactionModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COL_TRANS_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TransactionModel item = parseTransaction(cursor);
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * READ: Get recent transactions with a limit
     */
    public List<TransactionModel> getRecentTransactions(int limit) {
        List<TransactionModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " ORDER BY " + COL_TRANS_TIMESTAMP + " DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                TransactionModel item = parseTransaction(cursor);
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * READ: Get a single transaction by ID
     */
    public TransactionModel getTransactionById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, COL_TRANS_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        TransactionModel item = null;
        if (cursor != null && cursor.moveToFirst()) {
            item = parseTransaction(cursor);
            cursor.close();
        }
        db.close();
        return item;
    }

    /**
     * READ: Get transactions for a specific category
     */
    public List<TransactionModel> getTransactionsByCategory(String categoryName) {
        List<TransactionModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRANS_CATEGORY + " = ? ORDER BY " + COL_TRANS_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{categoryName});

        if (cursor.moveToFirst()) {
            do {
                TransactionModel item = parseTransaction(cursor);
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * READ: Get transactions filtered by type ("INCOME" or "EXPENSE")
     */
    public List<TransactionModel> getTransactionsByType(String type) {
        List<TransactionModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRANS_TYPE + " = ? ORDER BY " + COL_TRANS_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                TransactionModel item = parseTransaction(cursor);
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Get transaction count for a specific category
     */
    public int getTransactionsCountForCategory(String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRANS_CATEGORY + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{categoryName});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    /**
     * UPDATE: Update existing transaction
     */
    public int updateTransaction(TransactionModel transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TRANS_TITLE, transaction.getTitle());
        values.put(COL_TRANS_AMOUNT, transaction.getAmount());
        values.put(COL_TRANS_TYPE, transaction.getType());
        values.put(COL_TRANS_CATEGORY, transaction.getCategory());
        values.put(COL_TRANS_DATE, transaction.getDate());
        values.put(COL_TRANS_PAYMENT_METHOD, transaction.getPaymentMethod());
        values.put(COL_TRANS_TIMESTAMP, transaction.getTimestamp());

        int rows = db.update(TABLE_TRANSACTIONS, values, COL_TRANS_ID + "=?", new String[]{String.valueOf(transaction.getId())});
        db.close();
        return rows;
    }

    /**
     * DELETE: Delete a transaction by ID
     */
    public int deleteTransaction(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_TRANSACTIONS, COL_TRANS_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * DELETE: Delete all transactions (Reset data)
     */
    public void clearAllTransactions() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSACTIONS, null, null);
        db.close();
    }

    // ==========================================
    // FINANCIAL AGGREGATIONS & CALCULATIONS
    // ==========================================

    /**
     * Calculate Total Income (SUM of all transactions where type = 'INCOME')
     */
    public double getTotalIncome() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRANS_TYPE + " = 'INCOME'";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    /**
     * Calculate Total Expense (SUM of all transactions where type = 'EXPENSE')
     */
    public double getTotalExpense() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRANS_TYPE + " = 'EXPENSE'";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    /**
     * Calculate Net Balance = Total Income - Total Expense
     */
    public double getNetBalance() {
        return getTotalIncome() - getTotalExpense();
    }

    /**
     * Calculate total spent or earned for a specific category
     */
    public double getTotalForCategory(String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS + " WHERE " + COL_TRANS_CATEGORY + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{categoryName});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    /**
     * Get count of all recorded transactions
     */
    public int getTransactionsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS;
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // ==========================================
    // CATEGORY CRUD OPERATIONS
    // ==========================================

    /**
     * CREATE: Add a new custom category
     */
    public long addCategory(CategoryModel category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, category.getName());
        values.put(COL_CAT_TYPE, category.getType());
        values.put(COL_CAT_ICON, category.getIconName());

        long result = db.insert(TABLE_CATEGORIES, null, values);
        db.close();
        return result;
    }

    /**
     * READ: Get all categories
     */
    public List<CategoryModel> getAllCategories() {
        List<CategoryModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COL_CAT_ID + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                CategoryModel cat = new CategoryModel(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_ICON))
                );
                list.add(cat);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * READ: Get categories filtered by type ("INCOME" or "EXPENSE")
     */
    public List<CategoryModel> getCategoriesByType(String type) {
        List<CategoryModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_CATEGORIES + " WHERE " + COL_CAT_TYPE + " = ? ORDER BY " + COL_CAT_ID + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                CategoryModel cat = new CategoryModel(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_ICON))
                );
                list.add(cat);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * READ: Get a single category by ID
     */
    public CategoryModel getCategoryById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, null, COL_CAT_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        CategoryModel cat = null;
        if (cursor != null && cursor.moveToFirst()) {
            cat = new CategoryModel(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_ICON))
            );
            cursor.close();
        }
        db.close();
        return cat;
    }

    /**
     * READ: Get a single category by Name
     */
    public CategoryModel getCategoryByName(String name) {
        if (name == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CATEGORIES, null, COL_CAT_NAME + "=?", new String[]{name}, null, null, null);
        CategoryModel cat = null;
        if (cursor != null && cursor.moveToFirst()) {
            cat = new CategoryModel(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAT_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_TYPE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_CAT_ICON))
            );
            cursor.close();
        }
        db.close();
        return cat;
    }

    /**
     * UPDATE: Update existing category and migrate existing transactions with old category name
     */
    public int updateCategory(CategoryModel category, String oldName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CAT_NAME, category.getName());
        values.put(COL_CAT_TYPE, category.getType());
        values.put(COL_CAT_ICON, category.getIconName());

        int rows = db.update(TABLE_CATEGORIES, values, COL_CAT_ID + "=?", new String[]{String.valueOf(category.getId())});

        // If category name changed, update all transactions referencing oldName
        if (oldName != null && !oldName.equalsIgnoreCase(category.getName())) {
            ContentValues transValues = new ContentValues();
            transValues.put(COL_TRANS_CATEGORY, category.getName());
            if (category.getType() != null) {
                transValues.put(COL_TRANS_TYPE, category.getType());
            }
            db.update(TABLE_TRANSACTIONS, transValues, COL_TRANS_CATEGORY + "=?", new String[]{oldName});
        }
        db.close();
        return rows;
    }

    /**
     * DELETE: Delete category by ID and safely reassign its transactions to a fallback category
     */
    public int deleteCategory(int id, String categoryName, String categoryType) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_CATEGORIES, COL_CAT_ID + "=?", new String[]{String.valueOf(id)});
        if (categoryName != null) {
            String fallback = "INCOME".equalsIgnoreCase(categoryType) ? "Other Income" : "Other Expense";
            ContentValues transValues = new ContentValues();
            transValues.put(COL_TRANS_CATEGORY, fallback);
            db.update(TABLE_TRANSACTIONS, transValues, COL_TRANS_CATEGORY + "=?", new String[]{categoryName});
        }
        db.close();
        return rows;
    }

    /**
     * DELETE: Delete category by ID
     */
    public int deleteCategory(int id) {
        CategoryModel cat = getCategoryById(id);
        if (cat != null) {
            return deleteCategory(id, cat.getName(), cat.getType());
        }
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_CATEGORIES, COL_CAT_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * Get count of all categories
     */
    public int getCategoriesCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_CATEGORIES;
        Cursor cursor = db.rawQuery(query, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    // ==========================================
    // BAR CHART AGGREGATIONS
    // ==========================================

    /**
     * Get spending breakdown for the 7 days of the current week (Mon .. Sun).
     */
    public List<BarChartView.BarItem> getDailySpendingForWeek() {
        List<BarChartView.BarItem> list = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        SQLiteDatabase db = this.getReadableDatabase();
        for (int i = 0; i < 7; i++) {
            long start = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long end = cal.getTimeInMillis();

            double total = 0.0;
            String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_TRANS_TYPE + " = 'EXPENSE' AND " +
                    COL_TRANS_TIMESTAMP + " >= " + start + " AND " + COL_TRANS_TIMESTAMP + " < " + end;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
            list.add(new BarChartView.BarItem(days[i], total));
        }
        db.close();
        return list;
    }

    /**
     * Get spending breakdown for the 4 weeks of the current month.
     */
    public List<BarChartView.BarItem> getWeeklySpendingForMonth() {
        List<BarChartView.BarItem> list = new ArrayList<>();
        String[] weekLabels = {"W1", "W2", "W3", "W4"};

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        SQLiteDatabase db = this.getReadableDatabase();
        for (int i = 0; i < 4; i++) {
            long start = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 7);
            long end = (i == 3) ? cal.getTimeInMillis() + (3L * 24 * 3600 * 1000) : cal.getTimeInMillis();

            double total = 0.0;
            String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_TRANS_TYPE + " = 'EXPENSE' AND " +
                    COL_TRANS_TIMESTAMP + " >= " + start + " AND " + COL_TRANS_TIMESTAMP + " < " + end;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
            list.add(new BarChartView.BarItem(weekLabels[i], total));
        }
        db.close();
        return list;
    }

    /**
     * Get spending breakdown for the 12 months of the current year (Jan .. Dec).
     */
    public List<BarChartView.BarItem> getMonthlySpendingForYear() {
        List<BarChartView.BarItem> list = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        SQLiteDatabase db = this.getReadableDatabase();
        for (int i = 0; i < 12; i++) {
            cal.set(Calendar.MONTH, i);
            long start = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            long end = cal.getTimeInMillis();

            double total = 0.0;
            String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_TRANS_TYPE + " = 'EXPENSE' AND " +
                    COL_TRANS_TIMESTAMP + " >= " + start + " AND " + COL_TRANS_TIMESTAMP + " < " + end;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
            list.add(new BarChartView.BarItem(months[i], total));
        }
        db.close();
        return list;
    }

    /**
     * Get spending breakdown for the last 4 years.
     */
    public List<BarChartView.BarItem> getYearlySpending() {
        List<BarChartView.BarItem> list = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SQLiteDatabase db = this.getReadableDatabase();

        for (int y = currentYear - 3; y <= currentYear; y++) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, y);
            cal.set(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long start = cal.getTimeInMillis();
            cal.add(Calendar.YEAR, 1);
            long end = cal.getTimeInMillis();

            double total = 0.0;
            String query = "SELECT SUM(" + COL_TRANS_AMOUNT + ") FROM " + TABLE_TRANSACTIONS +
                    " WHERE " + COL_TRANS_TYPE + " = 'EXPENSE' AND " +
                    COL_TRANS_TIMESTAMP + " >= " + start + " AND " + COL_TRANS_TIMESTAMP + " < " + end;
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
            cursor.close();
            list.add(new BarChartView.BarItem(String.valueOf(y), total));
        }
        db.close();
        return list;
    }

    /**
     * Converts all transaction amounts in the SQLite database by a given exchange rate multiplier.
     */
    public void convertAllTransactionAmounts(double multiplier) {
        if (multiplier <= 0 || multiplier == 1.0) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_TRANSACTIONS + " SET " + COL_TRANS_AMOUNT + " = ROUND(" + COL_TRANS_AMOUNT + " * " + multiplier + ", 2)");
        db.close();
    }

    private TransactionModel parseTransaction(Cursor cursor) {
        return new TransactionModel(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_TRANS_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TITLE)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TRANS_AMOUNT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_CATEGORY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANS_PAYMENT_METHOD)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_TRANS_TIMESTAMP))
        );
    }
}
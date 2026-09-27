package com.example.mywallet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PdfExportHelper generates a professional, styled PDF statement
 * of all transactions and launches the Android share/view intent.
 */
public class PdfExportHelper {

    public static void exportAndSharePdf(Context context, String userName) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<TransactionModel> transactions = dbHelper.getAllTransactions();
        double totalIncome = dbHelper.getTotalIncome();
        double totalExpense = dbHelper.getTotalExpense();
        double netBalance = totalIncome - totalExpense;

        DecimalFormat df = new DecimalFormat("#,##0.00");
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());

        // A4 page dimensions in points (72 points per inch)
        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 1. Top Header Background (Dark Navy)
        paint.setColor(Color.parseColor("#0C2132"));
        canvas.drawRect(0, 0, pageWidth, 90, paint);

        // Header Title
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("MyWallet Statement", 30, 42, paint);

        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.parseColor("#CBD5E1"));
        canvas.drawText("Account: " + userName + " | Generated: " + currentDateStr, 30, 68, paint);

        // 2. Financial Summary Cards
        int cardTop = 110;
        int cardHeight = 65;
        int cardWidth = (pageWidth - 60 - 20) / 3;

        // Card 1: Total Income
        drawSummaryCard(canvas, 30, cardTop, cardWidth, cardHeight, "Total Income", "+" + CurrencyHelper.formatAmount(context, totalIncome), "#16A34A");

        // Card 2: Total Expenses
        drawSummaryCard(canvas, 30 + cardWidth + 10, cardTop, cardWidth, cardHeight, "Total Expenses", "-" + CurrencyHelper.formatAmount(context, totalExpense), "#DC2626");

        // Card 3: Net Balance
        drawSummaryCard(canvas, 30 + (cardWidth * 2) + 20, cardTop, cardWidth, cardHeight, "Net Balance", CurrencyHelper.formatAmount(context, netBalance), "#0C2132");

        // 3. Transactions Table Header
        int tableTop = 200;
        paint.setColor(Color.parseColor("#E2E8F0"));
        canvas.drawRect(30, tableTop, pageWidth - 30, tableTop + 26, paint);

        paint.setColor(Color.parseColor("#0C2132"));
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText("#", 38, tableTop + 17, paint);
        canvas.drawText("Date", 65, tableTop + 17, paint);
        canvas.drawText("Title", 155, tableTop + 17, paint);
        canvas.drawText("Category", 310, tableTop + 17, paint);
        canvas.drawText("Type", 430, tableTop + 17, paint);
        canvas.drawText("Amount", 500, tableTop + 17, paint);

        // 4. Transaction Rows
        int rowY = tableTop + 45;
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(10.5f);

        int index = 1;
        for (TransactionModel item : transactions) {
            if (rowY > pageHeight - 60) break; // Keep within single page or first page limit

            // Alternate row background
            if (index % 2 == 0) {
                Paint bgPaint = new Paint();
                bgPaint.setColor(Color.parseColor("#F8FAFC"));
                canvas.drawRect(30, rowY - 14, pageWidth - 30, rowY + 8, bgPaint);
            }

            paint.setColor(Color.parseColor("#475569"));
            canvas.drawText(String.valueOf(index), 38, rowY, paint);
            canvas.drawText(item.getDate() != null ? item.getDate() : "-", 65, rowY, paint);

            String title = item.getTitle();
            if (title.length() > 24) title = title.substring(0, 22) + "...";
            canvas.drawText(title, 155, rowY, paint);

            String cat = item.getCategory();
            if (cat.length() > 18) cat = cat.substring(0, 16) + "...";
            canvas.drawText(cat, 310, rowY, paint);

            boolean isIncome = "INCOME".equalsIgnoreCase(item.getType());
            canvas.drawText(isIncome ? "Income" : "Expense", 430, rowY, paint);

            paint.setColor(Color.parseColor(isIncome ? "#16A34A" : "#DC2626"));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            String amountText = (isIncome ? "+" : "-") + CurrencyHelper.formatAmount(context, item.getAmount());
            canvas.drawText(amountText, 500, rowY, paint);
            paint.setTypeface(Typeface.DEFAULT);

            rowY += 24;
            index++;
        }

        // 5. Footer
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(9);
        canvas.drawText("MyWallet • 100% Private Offline Statement • Generated on Device", 30, pageHeight - 25, paint);

        document.finishPage(page);

        // Save PDF file
        File pdfDir = new File(context.getExternalFilesDir(null), "statements");
        if (!pdfDir.exists()) pdfDir.mkdirs();

        File pdfFile = new File(pdfDir, "MyWallet_Statement_" + System.currentTimeMillis() + ".pdf");
        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Open or Share PDF
            openPdfFile(context, pdfFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static void drawSummaryCard(Canvas canvas, int x, int y, int w, int h, String title, String value, String colorHex) {
        Paint bg = new Paint();
        bg.setColor(Color.parseColor("#F1F5F9"));
        canvas.drawRoundRect(x, y, x + w, y + h, 8, 8, bg);

        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.parseColor("#64748B"));
        textPaint.setTextSize(9.5f);
        canvas.drawText(title, x + 10, y + 20, textPaint);

        textPaint.setColor(Color.parseColor(colorHex));
        textPaint.setTextSize(13.5f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(value, x + 10, y + 46, textPaint);
    }

    private static void openPdfFile(Context context, File file) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(intent, "Open Statement PDF"));
        } catch (Exception e) {
            // Fallback to share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share Statement PDF"));
        }
    }
}
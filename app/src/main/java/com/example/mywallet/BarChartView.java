package com.example.mywallet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * BarChartView is a custom native Android view that renders real-time dynamic bar charts
 * with interactive tooltips, smooth rounded bars, and automatic scaling for financial analytics.
 */
public class BarChartView extends View {

    public static class BarItem {
        public String label;
        public double value;

        public BarItem(String label, double value) {
            this.label = label;
            this.value = value;
        }
    }

    private List<BarItem> items = new ArrayList<>();
    private String currencySymbol = "$";
    private int selectedIndex = -1;

    private Paint barPaint;
    private Paint barBgPaint;
    private Paint barHighlightPaint;
    private Paint textPaint;
    private Paint gridPaint;
    private Paint tooltipBgPaint;
    private Paint tooltipTextPaint;

    private DecimalFormat df = new DecimalFormat("#,##0.00");

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(Color.parseColor("#0C2132")); // Deep Navy

        barHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barHighlightPaint.setColor(Color.parseColor("#D4AF37")); // Gold / Highlight

        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setColor(Color.parseColor("#E2E8F0")); // Subtle Gray for 0 values

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#64748B"));
        textPaint.setTextSize(spToPx(11));
        textPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#F1F5F9"));
        gridPaint.setStrokeWidth(dpToPx(1));

        tooltipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipBgPaint.setColor(Color.parseColor("#0C2132"));

        tooltipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tooltipTextPaint.setColor(Color.WHITE);
        tooltipTextPaint.setTextSize(spToPx(11.5f));
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);
        tooltipTextPaint.setFakeBoldText(true);
    }

    public void setItems(List<BarItem> newItems, String currencySymbol) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        this.currencySymbol = currencySymbol != null ? currencySymbol : "$";

        // Default selection: highest value if > 0, otherwise middle or last
        selectedIndex = -1;
        double max = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).value > max) {
                max = items.get(i).value;
                selectedIndex = i;
            }
        }
        if (selectedIndex == -1 && !items.isEmpty()) {
            selectedIndex = items.size() - 1;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        float paddingLeft = dpToPx(12);
        float paddingRight = dpToPx(12);
        float paddingTop = dpToPx(40); // space for tooltip
        float paddingBottom = dpToPx(28); // space for x-axis labels

        float chartHeight = height - paddingTop - paddingBottom;
        float chartWidth = width - paddingLeft - paddingRight;

        // Draw baseline grid
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, gridPaint);
        canvas.drawLine(paddingLeft, paddingTop + (chartHeight / 2f), width - paddingRight, paddingTop + (chartHeight / 2f), gridPaint);

        if (items.isEmpty()) {
            textPaint.setTextSize(spToPx(13));
            canvas.drawText("No data available", width / 2f, height / 2f, textPaint);
            textPaint.setTextSize(spToPx(11));
            return;
        }

        // Find max value
        double maxValue = 0;
        for (BarItem item : items) {
            if (item.value > maxValue) maxValue = item.value;
        }

        int count = items.size();
        float barWidth = Math.min(dpToPx(26), (chartWidth / count) * 0.55f);
        float totalSlotWidth = chartWidth / count;

        float tooltipX = -1;
        float tooltipY = -1;
        String tooltipText = "";

        for (int i = 0; i < count; i++) {
            BarItem item = items.get(i);
            float centerX = paddingLeft + (i * totalSlotWidth) + (totalSlotWidth / 2f);
            float left = centerX - (barWidth / 2f);
            float right = centerX + (barWidth / 2f);

            float barH;
            if (maxValue > 0 && item.value > 0) {
                barH = (float) ((item.value / maxValue) * (chartHeight - dpToPx(10)));
                barH = Math.max(barH, dpToPx(6)); // Minimum height for visibility
            } else {
                barH = dpToPx(4); // Flat baseline indicator
            }

            float top = height - paddingBottom - barH;
            float bottom = height - paddingBottom;

            RectF barRect = new RectF(left, top, right, bottom);
            float cornerRadius = dpToPx(5);

            boolean isSelected = (i == selectedIndex);
            if (item.value > 0) {
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, isSelected ? barHighlightPaint : barPaint);
            } else {
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barBgPaint);
            }

            // Draw Label
            textPaint.setColor(isSelected ? Color.parseColor("#0C2132") : Color.parseColor("#94A3B8"));
            textPaint.setFakeBoldText(isSelected);
            canvas.drawText(item.label, centerX, height - dpToPx(8), textPaint);

            // Calculate tooltip position
            if (isSelected) {
                tooltipX = centerX;
                tooltipY = top - dpToPx(8);
                boolean needsSpace = currencySymbol.length() > 1 || currencySymbol.equals("Rs");
                String prefix = needsSpace ? currencySymbol + " " : currencySymbol;
                tooltipText = prefix + df.format(item.value);
            }
        }

        // Draw Tooltip Bubble
        if (tooltipX != -1 && !tooltipText.isEmpty()) {
            float textWidth = tooltipTextPaint.measureText(tooltipText);
            float bubbleWidth = textWidth + dpToPx(18);
            float bubbleHeight = dpToPx(24);

            float bubbleLeft = Math.max(paddingLeft, Math.min(width - paddingRight - bubbleWidth, tooltipX - (bubbleWidth / 2f)));
            float bubbleRight = bubbleLeft + bubbleWidth;
            float bubbleTop = Math.max(dpToPx(4), tooltipY - bubbleHeight);
            float bubbleBottom = bubbleTop + bubbleHeight;

            RectF bubbleRect = new RectF(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom);
            canvas.drawRoundRect(bubbleRect, dpToPx(12), dpToPx(12), tooltipBgPaint);

            float textY = bubbleTop + (bubbleHeight / 2f) - ((tooltipTextPaint.descent() + tooltipTextPaint.ascent()) / 2f);
            canvas.drawText(tooltipText, bubbleRect.centerX(), textY, tooltipTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && !items.isEmpty()) {
            float x = event.getX();
            float paddingLeft = dpToPx(12);
            float paddingRight = dpToPx(12);
            float chartWidth = getWidth() - paddingLeft - paddingRight;
            float slotWidth = chartWidth / items.size();

            int clickedIndex = (int) ((x - paddingLeft) / slotWidth);
            if (clickedIndex >= 0 && clickedIndex < items.size()) {
                selectedIndex = clickedIndex;
                invalidate();
                performClick();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    private float spToPx(float sp) {
        return sp * getContext().getResources().getDisplayMetrics().scaledDensity;
    }
}

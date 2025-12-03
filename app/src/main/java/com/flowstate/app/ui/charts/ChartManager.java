package com.flowstate.app.ui.charts;

import android.graphics.Color;
import android.view.View;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.flowstate.app.R;

public class ChartManager {

    private static final int COLOR_PRIMARY = Color.parseColor("#4CAF50");
    private static final int COLOR_SECONDARY = Color.parseColor("#2196F3");
    private static final int COLOR_ACCENT = Color.parseColor("#FF9800");
    private static final int COLOR_TEXT = Color.parseColor("#333333");
    private static final int COLOR_GRID = Color.parseColor("#EEEEEE");

    public static void setupLineChart(LineChart chart, String description) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false); // Hide legend for single series usually

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setGranularity(1f); // minimal interval to be 1 unit

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(COLOR_TEXT);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(COLOR_GRID);

        chart.getAxisRight().setEnabled(false);
        
        // Add custom marker
        CustomMarkerView mv = new CustomMarkerView(chart.getContext(), R.layout.custom_marker_view);
        mv.setChartView(chart);
        chart.setMarker(mv);
    }

    public static void setupBarChart(BarChart chart, String description) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.getLegend().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(COLOR_TEXT);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(COLOR_TEXT);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(COLOR_GRID);

        chart.getAxisRight().setEnabled(false);
        
        // Add custom marker
        CustomMarkerView mv = new CustomMarkerView(chart.getContext(), R.layout.custom_marker_view);
        mv.setChartView(chart);
        chart.setMarker(mv);
    }

    public static void updateLineChart(LineChart chart, List<Double> values, List<String> labels, String label) {
        if (values == null || values.isEmpty()) {
            chart.clear();
            return;
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new Entry(i, values.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(COLOR_PRIMARY);
        dataSet.setValueTextColor(COLOR_TEXT);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(COLOR_PRIMARY);
        dataSet.setCircleRadius(5f); // Increased radius for easier touch
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(COLOR_PRIMARY);
        dataSet.setFillAlpha(50);
        
        // Enable highlighting
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(COLOR_ACCENT);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        // Update marker unit based on chart type inference or passed param (simplified here)
        CustomMarkerView mv = (CustomMarkerView) chart.getMarker();
        // Ideally we'd pass unit to setup or update, but for now generic is fine

        if (labels != null && !labels.isEmpty()) {
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        }

        chart.invalidate(); // Refresh
    }

    public static void updateBarChart(BarChart chart, List<Double> values, List<String> labels, String label) {
        if (values == null || values.isEmpty()) {
            chart.clear();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            entries.add(new BarEntry(i, values.get(i).floatValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(COLOR_SECONDARY);
        dataSet.setValueTextColor(COLOR_TEXT);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        if (labels != null && !labels.isEmpty()) {
            chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        }

        chart.invalidate();
    }
    
    // Format timestamp to hour:minute
    public static String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
    
    // Format timestamp to Day (Mon, Tue)
    public static String formatDay(long timestamp) {
        return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(timestamp));
    }
}


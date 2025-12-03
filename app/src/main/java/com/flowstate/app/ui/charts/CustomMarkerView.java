package com.flowstate.app.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.widget.TextView;
import com.flowstate.app.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {
    private TextView tvContent;
    private String unit = "";

    public CustomMarkerView(Context context, int layoutResource, String unit) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvMarkerContent);
        this.unit = unit;
    }
    
    public CustomMarkerView(Context context, int layoutResource) {
        this(context, layoutResource, "");
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvContent.setText(String.format(Locale.getDefault(), "%.1f %s", e.getY(), unit));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}


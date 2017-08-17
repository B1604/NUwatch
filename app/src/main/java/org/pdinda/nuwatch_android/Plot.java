package org.pdinda.nuwatch_android;

import android.view.View;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Path;
import android.text.TextPaint;
import android.text.StaticLayout;
import android.text.Layout.Alignment;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;


public class Plot extends View {
    int expand_x=4;
    int height=-1;
    int width=-1;

    List<int []> data = new ArrayList<int []>();
    String[]     labels;

    private String TAG = "Plot";

    public Plot(Context context, AttributeSet attrs) {
        super(context,attrs);
        setBackgroundColor(Color.BLACK);
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {
        int num_series;

        // Need to get sizes the first time around
        if (width == -1) {
            width = canvas.getWidth();
            height = canvas.getHeight();
        } else {
            if (canvas.getWidth() != width || canvas.getHeight() != height) {
                Log.e(TAG, "Canvas changed size...");
                width = canvas.getWidth();
                height = canvas.getHeight();
            }
        }

        if (labels == null || data.size() < 1) {
            return; // not ready yet
        }

        num_series = data.get(0).length;

        if (num_series<1) {
            return;
        }

        for (int series = 0; series < num_series; series++) {
            // We can be smarter than this...
            int n = data.size();
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            float sum = 0;
            float sum2 = 0;
            for (int i = 0; i < n; i++) {
                float val = (float) data.get(i)[series];
                if (val < min) {
                    min = val;
                }
                if (val > max) {
                    max = val;
                }
                sum +=  val;
                sum2 += val * val;
            }
            if (max == min) {
                max++;
            }
            float avg = sum / (float) n;
            float std = (float) Math.sqrt((double) ((sum2 - sum * sum / (float) n / (float) n)));
            float range = max - min;
            float scale = (float) height / (float) range / (float) num_series;
            float traceheight = (float) height / (float) num_series;
            float offset = traceheight * series;


            Path path = new Path();

            path.moveTo(0, offset);

            for (int i = 0; i < n; i++) {
                float val = (float) data.get(i)[series];
                path.lineTo(i*expand_x, offset + traceheight - (val - min) * scale);
            }

            Paint paint = new Paint();

            paint.setColor(Color.HSVToColor(new float[]{(float)360.0 * (float) series / (float) num_series, (float) 1.0, (float) 0.9}));
            paint.setStrokeWidth(expand_x);
            paint.setStyle(Paint.Style.STROKE);
            paint.setPathEffect(null);

            canvas.drawPath(path, paint);

            paint.setColor(Color.WHITE);
            paint.setTextSize(traceheight/(float)4.0);


            String s = labels[series] + "\n" + min + " to " + max + "\navg: " + String.format("%.0f",avg) + " std: "+ String.format("%.0f",std);

            float lineheight = paint.descent()-paint.ascent();
            float cury = offset + lineheight;
            for (String line: s.split("\n")) {
                canvas.drawText(line,0,cury,paint);
                cury += lineheight;
            }

        }
    }


    public synchronized void pushData(int [] x) {
        data.add(x);
        while (data.size() >= width/expand_x) {
            data.remove(0);
        }


    }

    public synchronized void setText(String [] x) {
        labels = x;
    }

}
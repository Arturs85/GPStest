package com.example.gpstest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.renderscript.Double2;
import android.renderscript.Float2;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class PointsMapView extends View {
    int radiOfEarth = 6371000;//m
    Paint paint = new Paint();
    Paint textPaint = new Paint();
    String TAG = "PointsMapView";
    Point controlerCenter;
    int oldDist = 0;
    Point oldPoint = null;
    double zoom = 4;
    float x = 0, y = 0;
    static ArrayList<Double2> path = new ArrayList<>(100);
    ImuReader ir;

    public PointsMapView(Context context, ImuReader ir) {
        super(context);
        controlerCenter = new Point(240, 300);
        paint.setAntiAlias(true);
        paint.setColor(Color.CYAN);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(30);
        x = controlerCenter.x;
        y = controlerCenter.y - 55;
        this.ir = ir;
        // this.bitmap = bitmap;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("PointMapView", "onDraw");
        super.onDraw(canvas);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        // canvas.drawText("zoom: " + zoom, 20, 200, textPaint);
        canvas.drawText("10 m : " + zoom, 10, 20, textPaint);
        canvas.drawText("Z dir : " + ir.directionZ * 180 / Math.PI + " drift rad/s :" + ir.gyroDriftCounter.driftRadSec, 10, 60, textPaint);

        textPaint.setStrokeWidth(3);
        canvas.drawLine(100, 25, (float) (100 + zoom * 10), 25, textPaint);
        canvas.drawLine(controlerCenter.x, 0, controlerCenter.x, getHeight(), textPaint);
        canvas.drawLine(0, controlerCenter.y, getWidth(), controlerCenter.y, textPaint);

        if (path.size() < 1) return;
        Double2 ori = path.get(0);
        int i = 0;
        for (Double2 p : path) {
            double dx = Math.sin(Math.toRadians(ori.x - p.x)) * radiOfEarth;
            double dy = Math.sin(Math.toRadians(ori.y - p.y)) * radiOfEarth;
            i++;
            paint.setAlpha(255 * i / path.size());
            canvas.drawCircle((float) (controlerCenter.x + dx * zoom), (float) (controlerCenter.y + dy * zoom), (float) (0.5 * zoom), paint);
        }

//canvas.drawText("fyfiiifytxzsfghh",100,100,paint2);

        // if (bitmap != null)
        //     canvas.drawBitmap(bitmap, 0, 0, null);
        // else
        //    canvas.drawText("Null bitmap", 44, 40, paint);
        super.onDraw(canvas);

    }
// invalidate();


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        Log.d(TAG, "touch event " + event.getAction());
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                oldDist = 0;
                oldPoint = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() > 1) {
                    float x = event.getX(0) - event.getX(1);
                    float y = event.getY(0) - event.getY(1);
                    double newDist = Math.sqrt(x * x + y * y);
                    if (oldDist != 0)
                        zoom = zoom * (newDist / oldDist);

                    oldDist = (int) newDist;
                    Log.d(TAG, "zoom= " + zoom);
                } else { //move map

                    if (oldPoint != null) {
                        controlerCenter.x += (event.getX() - oldPoint.x);
                        controlerCenter.y += (event.getY() - oldPoint.y);

                    } else {
                        oldPoint = new Point();

                    }

                    oldPoint.x = (int) event.getX();
                    oldPoint.y = (int) event.getY();


                }

                break;
        }

        invalidate();
        //  return super.onTouchEvent(event);
        return true;
    }


}
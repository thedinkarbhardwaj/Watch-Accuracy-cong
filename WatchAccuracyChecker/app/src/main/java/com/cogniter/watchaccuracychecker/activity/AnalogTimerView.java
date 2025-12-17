package com.cogniter.watchaccuracychecker.activity;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.cogniter.watchaccuracychecker.R;
import com.cogniter.watchaccuracychecker.database.DBHelper;
import com.cogniter.watchaccuracychecker.service.TimerService;
import com.cogniter.watchaccuracychecker.utills.GlobalVariables;

import java.util.Timer;

/*
 * Copyright 2016 Elye Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class AnalogTimerView extends View {
    public interface TimerListener {
        void onTimeUpdated(long remainingTime);
    }
    public interface TimeOutListener {
        void onTimeOut();
    }
    private TimerListener timerListener;
    private static final int ONE_SECOND = 1000;
    private static final int DEFAULT_MAX_TIME = 1123;
    private static final int ONE_CYCLE_DEGREE = 360;
    private static final int INVALID = -1;
    private Paint gradientPaint;
    private Paint handPaint;
    private Paint linePaint;
    private Path miniPath = new Path();
    private Path minorPath = new Path();
    private Path majorPath = new Path();
    private Path handPath = new Path();
    private Bitmap handBitmap;
    private Bitmap facadeBitmap;
    private float radius;
    private int movingDegree = 0;
    private String time = "00:00:00";
    private Timer timerCounter = null;
    final Handler handler = new Handler();
    private int timerCount = 0;
    private int maxTime = INVALID;
    private int periodMs = ONE_SECOND;
    private int oneCycleTick = DEFAULT_MAX_TIME;
    private TimeOutListener timeOutListener = null;



    private boolean isRunning = false;
    private long startTime = 0;
    private long elapsedTime = 0;


    public AnalogTimerView(Context context) {
        super(context);
        init(null);
    }
    public AnalogTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Register the broadcast receiver to receive timer updates
        init(attrs);
        LocalBroadcastManager.getInstance(context).registerReceiver(timerUpdateReceiver, new IntentFilter(TimerService.ACTION_TIMER_UPDATE));
    }

//    public AnalogTimerView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init(attrs);
//    }

    public AnalogTimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public AnalogTimerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        initAttrs(attrs);
        initPaint();
    }

    private void initAttrs(AttributeSet attrs) {
        if (attrs == null) return;
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.analog_timer_view, 0, 0);
        maxTime = typedArray.getInt(R.styleable.analog_timer_view_max_time, INVALID);
        periodMs = (int)(typedArray.getFloat(R.styleable.analog_timer_view_period_second, 1.0f) * ONE_SECOND);
        oneCycleTick = typedArray.getInt(R.styleable.analog_timer_view_one_cycle_ticks, DEFAULT_MAX_TIME);


        validationOfParameter();
    }

    private void validationOfParameter() {
        if (maxTime > oneCycleTick) {
            throw new IllegalArgumentException("AnaologTimerView: max_time must be smaller or equal to oneCycleTick");
        }
        if (maxTime < INVALID || maxTime == 0) {
            throw new IllegalArgumentException("AnaologTimerView: max_time must be larger than 0");
        }
        if (oneCycleTick <= 0) {
            throw new IllegalArgumentException("AnaologTimerV.iew: one_cycle_ticks must be larger " +
                    "than 0");
        }
        if (periodMs < 100) {
            throw new IllegalArgumentException("AnaologTimerView: period_ms must be larger than 0.1");
        }
    }

    private void initPaint() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStrokeWidth(3);
        linePaint.setStyle(Style.STROKE);
        handPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        handPaint.setColor(Color.parseColor("#FFA500"));
        gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    }

    /**
     * Starting the Timer
     */
    public void startTimer() {
        if (!isRunning) {
            isRunning = true;
            startTime = System.currentTimeMillis() - elapsedTime;
            handler.postDelayed(runnable, 0);
        }
    }
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
              //  updateTimer();
                handler.postDelayed(this, 0);
            }
        }
    };
    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TimerService.ACTION_TIMER_UPDATE)) {
                int id = intent.getIntExtra(TimerService.EXTRA_TIMER_ID,0);
                long timeTaken = intent.getLongExtra(TimerService.EXTRA_TIMER_VALUE,0);
                // Update the AnalogTimerView with the new timer value
                DBHelper dbHelper = new DBHelper(context);

                System.out.println(GlobalVariables.INSTANCE.getCOMMON_ID() +"          dkkdokodkodkok          "+id);

                if(GlobalVariables.INSTANCE.getCOMMON_ID()==id){

                    updateTimerView(timeTaken);

                }
            }
        }
    };


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Unregister the broadcast receiver when the view is detached
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(timerUpdateReceiver);
    }

    private void updateTimerView(long timeTakenseconds) {
        // Update the AnalogTimerView with the new timer value
        //int milliseconds = (int) (timeTakenseconds % 1000);
        int seconds = (int) (timeTakenseconds / 1000 % 60);
        int minutes = (int) (timeTakenseconds / (1000 * 60) % 60);
        int hours = (int) (timeTakenseconds / (1000 * 60 * 60));

        String timeTaken = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        setMovingDegree(seconds * ONE_CYCLE_DEGREE/oneCycleTick,timeTaken);
    }
//    private void updateTimer() {
//        int milliseconds = (int) (elapsedTime % 1000);
//        int seconds = (int) (elapsedTime / 1000 % 60);
//        int minutes = (int) (elapsedTime / (1000 * 60) % 60);
//        int hours = (int) (elapsedTime / (1000 * 60 * 60));
//
//        String timeTaken = String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, milliseconds);
//        setMovingDegree(seconds * ONE_CYCLE_DEGREE/oneCycleTick,timeTaken);
//
//    }

    /**
     * Stopping the Timer
     */
    public void stopTimer() {
        if (isRunning) {
            isRunning = false;
            handler.removeCallbacks(runnable);
            elapsedTime = System.currentTimeMillis() - startTime;
        }
    }

    /**
     * Reset the Timer
     */
    public void resetTimer() {
        setTime(0);
    }

    /**
     * Set the time out callback
     * @param timeOutListener callback
     */
    public void setTimeOutListener(TimeOutListener timeOutListener) {
        this.timeOutListener = timeOutListener;
    }
    // Set the listener
    public void setTimerListener(TimerListener listener) {
        this.timerListener = listener;
    }

    // Whenever the time is updated, call this method to notify the activity
    public void notifyTimeUpdated(long remainingTime) {
        if (timerListener != null) {
            timerListener.onTimeUpdated(remainingTime);
        }
    }
    /**
     * Get the current ticking time. Needed for saveInstanceState.
     * @return get the current counted time
     */
    public int getTime() {
        return timerCount;
    }

    /**
     * Set the current ticking time. Needed for restoreInstanceState.
     * @param timerCount set the starting of timer click
     */
    public void setTime(int timerCount) {
        this.timerCount = timerCount;
        updateTimerUI();
    }


    private void updateTimerUI() {
        if (timerCount >= oneCycleTick) {
            timerCount = 0;
        }
//        setMovingDegree(timerCount * ONE_CYCLE_DEGREE/oneCycleTick, timeTaken);
    }

    private void setMovingDegree(int moving, String timeTaken) {

        movingDegree = moving;
        time = timeTaken;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int minh = getPaddingTop() + getPaddingBottom() + getSuggestedMinimumHeight();

        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);
        int h = resolveSizeAndState(minh, heightMeasureSpec, 1);

        if (w == 0) w = h;
        if (h == 0) h = w;

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (h == 0 || w == 0) return;

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        float ww = (float) w - xpad;
        float hh = (float) h - ypad;
        handPath.reset();
        majorPath.reset();
        miniPath.reset();
        minorPath.reset();
        radius = Math.min(ww, hh) / 2;
//        handPaint
//                .setShader(new LinearGradient(0, 0, 0, getHeight(), Color.BLACK, Color.WHITE,
//                        Shader.TileMode.CLAMP));
        gradientPaint
                .setShader(new LinearGradient(0, 0, getWidth(), getHeight(), Color.TRANSPARENT, Color
                        .TRANSPARENT, Shader.TileMode.CLAMP));

        drawClockFacade(w, h);
        drawClockHand(w, h);
    }

    private void drawClockHand(int w, int h) {
        handBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(handBitmap);

        int middleX = canvas.getWidth() / 2;
        int middleY = canvas.getHeight() / 2;

        // Calculate the length of the clock hand based on the smaller dimension of the screen
        int smallerDimension = Math.min(w, h);
        int handLength = (int) (smallerDimension * 0.4); // Adjust the multiplier as per your requirement

        handPaint.setStrokeWidth(4); // Set the desired thickness
        // Draw a line representing the clock hand
        canvas.drawLine(middleX, middleY, middleX, middleY - handLength, handPaint);
    }

    private void drawClockFacade(int w, int h) {
        facadeBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(facadeBitmap);
        int middleX = canvas.getWidth() / 2;
        int middleY = canvas.getHeight() / 2;

        canvas.drawCircle(middleX, middleY, radius, gradientPaint);
//        gradientPaint
//                .setShader(new LinearGradient(0, 0, getWidth(), getHeight(), Color.parseColor("#FFA500"), Color.parseColor("#FFA500"), Shader.TileMode.CLAMP));
//        canvas.drawCircle(middleX, middleY, 3, gradientPaint);

        // Outer circle
        gradientPaint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(), Color.parseColor("#FFA500"), Color.parseColor("#FFA500"), Shader.TileMode.CLAMP));
        canvas.drawCircle(middleX, middleY, 5, gradientPaint); // Adjust the radius as needed

// Inner circle (to create a hole)
        Paint transparentPaint = new Paint();
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(middleX, middleY, 2, transparentPaint); // Adjust the radius as needed

        gradientPaint.setColor(Color.TRANSPARENT);
        gradientPaint.setShader(null);
        canvas.drawCircle(middleX, middleY, radius * 9 / 10, gradientPaint);

        canvas.save();
        for (int i = 0; i < 60; i++) {
            drawMainLine(canvas, middleX, middleY, (int) (radius * 1 / 30), miniPath);
            if (i % 15 == 0) {
                drawMainLine(canvas, middleX, middleY, (int) (radius * 1 / 15), majorPath);
            } else if (i % 5 == 0) {
                drawMainLine(canvas, middleX, middleY, (int) (radius * 1 / 20), minorPath);
            }
            canvas.rotate(6, middleX, middleY);
        }
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int middleX = canvas.getWidth() / 2;
        int middleY = canvas.getHeight() / 2;
        // Create a new Paint object for the text
//        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        textPaint.setColor(Color.WHITE);
//        textPaint.setTextSize(24); // Set the text size
//        canvas.drawText(time, middleX-60, middleY+150, textPaint);
// Draw the text on the canvas

        if (facadeBitmap != null && handBitmap != null) {
            canvas.drawBitmap(facadeBitmap, 0, 0, null);
            canvas.save();

            canvas.rotate(movingDegree, middleX, middleY);
            canvas.drawBitmap(handBitmap, 0, 0, null);
            canvas.restore();
        }

    }

    private void drawMainLine(Canvas canvas, int middleX, int middleY, int length, Path path) {
        path.moveTo(middleX, middleY - radius * 9 / 10);
        path.lineTo(middleX, middleY - radius * 9 / 10 + length);
        canvas.drawPath(path, linePaint);
    }

}


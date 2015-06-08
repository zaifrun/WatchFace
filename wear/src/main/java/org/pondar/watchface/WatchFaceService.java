package org.pondar.watchface;

import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

public class WatchFaceService extends CanvasWatchFaceService {
	
	Context context;
	WatchFaceService service;
	public boolean mBurnInProtection;
	
	

    @Override
    public Engine onCreateEngine() {
    	this.context = getApplicationContext();
    	this.service = this;
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {

    	//we update every 1000th miliseconds, that is, once every second.
    	protected static final int INTERACTIVE_UPDATE_RATE_MS = 1000;

		protected static final int MSG_UPDATE_TIME = 0;

		/* a time object */
        Time mTime;

        /* device features */
        boolean mLowBitAmbient;
        boolean mRegisteredTimeZoneReceiver = false;

        /* graphic objects for painting */
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Paint mHourPaint; //painting options of hour paint
        Paint mMinutePaint; // for minute paint
        Paint mSecondPaint; // second paint.
        
        private void updateTimer() {
    	    mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
    	    if (shouldTimerBeRunning()) {
    	        mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
    	    }
    	}

        //should be show the watch?
    	private boolean shouldTimerBeRunning() {
    	    return isVisible() && !isInAmbientMode();
    	}
        

        /* handler to update the time once a second in interactive mode */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:  //did we receive an update message?
                        invalidate(); //redraw everything.
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /* receiver to update the time zone */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow(); //changing the clocks
            }
        };

    	
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            /* load the background image */
            Resources resources = context.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.watchface_background);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            /* create graphic styles */
            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 200, 200, 200); //dark gray color
            mHourPaint.setStrokeWidth(5.0f); //thick for hours
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            
            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 200, 200, 200);
            mMinutePaint.setStrokeWidth(3.0f); //medium for minutes
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            
            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 200, 20, 20);
            mSecondPaint.setStrokeWidth(2.0f); //thin for seconds
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            
            
            setWatchFaceStyle(new WatchFaceStyle.Builder(service)
            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
            .setBackgroundVisibility(WatchFaceStyle
                                    .BACKGROUND_VISIBILITY_INTERRUPTIVE)
            .setShowSystemUiTime(false)
            .build());
           

            /* allocate an object to hold the time */
            mTime = new Time();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mHourPaint.setAntiAlias(antiAlias);
                mMinutePaint.setAntiAlias(antiAlias);
                mSecondPaint.setAntiAlias(antiAlias);
            }
            invalidate();
            updateTimer();
        }

        //Here we do all the drawings.
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
        	 // Update the time
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                || mBackgroundScaledBitmap.getWidth() != width
                || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                                                 width, height, true /* filter */);
            }
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            // Find the center. Ignore the window insets so that, on round watches
            // with a "chin", the watch face is centered on the entire screen, not
            // just the usable portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            // Compute rotations and lengths for the clock hands.
            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f ) * (float) Math.PI;

            float secLength = centerX - 20;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            // Only draw the second hand in interactive mode.
            if (!isInAmbientMode()) {
                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                canvas.drawLine(centerX, centerY, centerX + secX, centerY +
                                secY, mSecondPaint);
            }

            // Draw the minute and hour hands.
            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY,
                            mMinutePaint);
            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY,
                            mHourPaint);
        }
        
        //make sure we have a timezone receiver registered.
        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            service.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            service.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode), so we may need to start or stop the timer
            updateTimer();
        }
    
    } //private Engine class ends here
}

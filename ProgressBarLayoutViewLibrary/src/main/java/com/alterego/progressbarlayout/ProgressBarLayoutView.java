package com.alterego.progressbarlayout;

    /*Copyright 2014 Alter Ego SRLS

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.

    */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ProgressBarLayoutView extends View {

    private static final String TAG = "ProgressBarLayoutView";
    private static final int ANIMATION_DURATION_IN_MS = 1000;
    private static final int STEP_DURATION_IN_MS = 10;
    private static final boolean DEBUG_LOGGING = false;

    private static final float TEXTPAINT_SPACING_MULT = 1.0f;
    private static final float TEXTPAINT_SPACING_ADD = 0.0f;

    private int mWidth = 0;
    private int mHeight = 0;
    private int mCenterX = 0;
    private int mCenterY = 0;
    private int mCurrentProgress = 0;
    private String mTextProgressString;
    private boolean mSizeChanged = false;
    private int mFutureProgress = 0;
    private Typeface mTypeface;

    private Paint mProgressCirclePaint;
    private Circle mProgressCircle;
    private TextPaint mTextPaint;
    private static int mSleepTime;
    private static float mIncreaseStep;
    private ProgressUpdateTask mUpdateTask;
    private float mBeginningProgressSize;
    private int mProgressCircleColor;
    private int mTextProgressColor;
    private float mTextSize;
    private int mProgressMax = 100;

    private Layout mTextToPrint;
    private ProgressBarLayoutView instance;

    private View mBeginningCrossAnimationView;
    private Animation mBeginningProgressAnimation;
    private Animation mBeginningInverseProgressAnimation;
    private boolean mBeginningAnimationPerformed = false;

    private View mEndingCrossAnimationView;
    private Animation mEndingProgressAnimation;
    private Animation mEndingInverseProgressAnimation;
    private boolean mEndingAnimationPerformed = false;

    private IProgressStringFormatter mProgressStringFormatter = new DefaultProgressStringFormatter();

    public ProgressBarLayoutView(final Context context) {
        super(context);
        init(context, null);
    }

    public ProgressBarLayoutView(final Context context,
                                 final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProgressBarLayoutView(final Context context,
                                 final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void init(final Context context, AttributeSet attrs) {
        instance = this;

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.ProgressBarLayoutView, 0, 0);
        try {
            mBeginningProgressSize = a.getDimension(
                    R.styleable.ProgressBarLayoutView_beginningProgressSize,
                    0.0f);
            mProgressCircleColor = a.getColor(
                    R.styleable.ProgressBarLayoutView_progressCircleColor,
                    0xff666666);
            mTextSize = a.getDimension(
                    R.styleable.ProgressBarLayoutView_textProgressSize,
                    24.0f);
            mTextProgressColor = a.getColor(
                    R.styleable.ProgressBarLayoutView_textProgressColor,
                    0xff000000);
            mTextProgressString = a.getString(
                    R.styleable.ProgressBarLayoutView_textProgressString);
            mSleepTime = a.getInteger(R.styleable.ProgressBarLayoutView_stepDurationInMs, STEP_DURATION_IN_MS);


            //beginning animation resources
            int begAnimRes = a.getResourceId(R.styleable.ProgressBarLayoutView_beginningProgressAnimation, -1);
            int begInvAnimRes = a.getResourceId(R.styleable.ProgressBarLayoutView_beginningInverseProgressAnimation, -1);
            int begAnimDuration = a.getInteger(R.styleable.ProgressBarLayoutView_beginningProgressAnimationDurationInMs, ANIMATION_DURATION_IN_MS);

            if (begAnimRes != -1 && begInvAnimRes != -1) {
                mBeginningProgressAnimation = AnimationUtils.loadAnimation(context, begAnimRes);
                mBeginningInverseProgressAnimation = AnimationUtils.loadAnimation(context, begInvAnimRes);
            } else {
                mBeginningProgressAnimation = new AlphaAnimation(0.0f, 1.0f);
                mBeginningInverseProgressAnimation = new AlphaAnimation(1.0f, 0.0f);
            }

            mBeginningProgressAnimation.setDuration(begAnimDuration);
            mBeginningInverseProgressAnimation.setDuration(begAnimDuration);

            //ending animation resources
            int endAnimRes = a.getResourceId(R.styleable.ProgressBarLayoutView_endingProgressAnimation, -1);
            int endInvAnimRes = a.getResourceId(R.styleable.ProgressBarLayoutView_endingInverseProgressAnimation, -1);
            int endAnimDuration = a.getInteger(R.styleable.ProgressBarLayoutView_endingProgressAnimationDurationInMs, ANIMATION_DURATION_IN_MS);

            if (begAnimRes != -1 && begInvAnimRes != -1) {
                mEndingProgressAnimation = AnimationUtils.loadAnimation(context, endAnimRes);
                mEndingInverseProgressAnimation = AnimationUtils.loadAnimation(context, endInvAnimRes);
            } else {
                mEndingProgressAnimation = new AlphaAnimation(1.0f, 0.0f);
                mEndingInverseProgressAnimation = new AlphaAnimation(0.0f, 1.0f);
            }

            mEndingProgressAnimation.setDuration(endAnimDuration);
            mEndingInverseProgressAnimation.setDuration(endAnimDuration);

        } finally {
            a.recycle();
        }

        setWidthHeightAndCenter();

        mProgressCirclePaint = new Paint();
        mProgressCirclePaint.setColor(mProgressCircleColor);
        mProgressCirclePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mProgressCirclePaint.setStyle(Paint.Style.FILL);
        mTextPaint = new TextPaint();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mTextProgressColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        cancelUpdateTask();

    }

    /**
     * Sets the text typeface for the progress bar label
     *
     * @param font typeface for the label
     */
    public void setTypeface(Typeface font) {
        mTypeface = font;
        if (mTextPaint != null)
            mTextPaint.setTypeface(font);
        requestLayout();
        invalidate();
    }

    /**
     * Resets the progress bar to 0
     */

    public void reset() {
        setCurrentProgress(0);
        mTextToPrint = mProgressStringFormatter.formatProgressString(mCurrentProgress, mTextProgressString, mTextPaint, mWidth, getPaddingLeft(), getPaddingRight());
        mProgressCircle.radius = mBeginningProgressSize;
        mBeginningAnimationPerformed = false;
        mEndingAnimationPerformed = false;
        invalidate();
    }

    /**
     * Sets the beginning progress size (i.e. the size of the circle for progress = 0
     *
     * @param size size of the beginning progress circle
     */
    public void setBeginningProgressSize(int size) {
        mBeginningProgressSize = size;
        mIncreaseStep = (getHalfDiagonal() - mBeginningProgressSize) / getMaxProgress();
    }

    private int getHalfDiagonal() {
        return (int) Math.sqrt((double) mWidth * mWidth + mHeight * mHeight) / 2;
    }

    /**
     * Sets color of the progress text
     *
     * @param color color of the progress text
     */
    public void setProgressTextColor(int color) {
        mTextProgressColor = color;
    }

    /**
     * Sets color of the progress bar circle
     *
     * @param color color of the progress bar circle
     */
    public void setProgresColor(int color) {
        mProgressCircleColor = color;
    }

    /**
     * Sets size of the progress bar text
     *
     * @param size size of the progress bar text
     */
    public void setTextProgressSize(int size) {
        mTextSize = size;
    }

    /**
     * Gets the progress bar maximum value (default is 100)
     *
     * @return maximum value of progress
     */
    public int getMaxProgress() {
        return mProgressMax;
    }

    /**
     * Set the progress bar maximum.
     *
     * @param maxProgress maximum progress (default is 100)
     */
    public void setMaxProgress(int maxProgress) {
        mProgressMax = maxProgress;
        mIncreaseStep = (getHalfDiagonal() - mBeginningProgressSize) / getMaxProgress();
    }

    /**
     * Sets the view that is visible before the progress bar. On this view will the
     * "beginningInverseProgressAnimation" animation be executed while at the same time the
     * "beginningProgressAnimation" is being executed on the progress bar view.
     * <p/>
     * At the beginning of the animations, the progress bar's visibility is set to {@link View#VISIBLE}, and
     * at the end of the animations, the beginning view's visibility will be set to {@link View#GONE}.
     *
     * @param view view visible before the progress bar starts
     */
    public void setBeginningCrossAnimationView(View view) {
        mBeginningCrossAnimationView = view;
    }

    /**
     * Sets the view that is visible after the progress bar. On this view will the
     * "endingInverseProgressAnimation" animation be executed while at the same time the
     * "endingProgressAnimation" is being executed on the progress bar view.
     * <p/>
     * At the beginning of the animations, the ending view's visibility is set to {@link View#VISIBLE}, and
     * at the end of the animations, the progress bar view's visibility will be set to {@link View#GONE}.
     *
     * @param view view visible after the progress bar ends (arrives at maximum progress, currently 100)
     */
    public void setEndingCrossAnimationView(View view) {
        mEndingCrossAnimationView = view;
    }



    /**
     * Set the progress bar progress.
     *
     * @param progress progress to be set, between 0 and max progress.
     */
    public void setProgress(int progress) {
        if (progress > getMaxProgress())
            progress = getMaxProgress();

        mFutureProgress = progress;

        if ((mCurrentProgress != mFutureProgress || mSizeChanged)) {
            mSizeChanged = false;
            int starting_progress = mCurrentProgress;

            if (mUpdateTask != null) {
                if (DEBUG_LOGGING) Log.w(TAG, "setProgress cancelling task");
                mUpdateTask.cancel(true);
            }

            if (DEBUG_LOGGING) Log.d(TAG, "setProgress progress = " + mFutureProgress);
            mUpdateTask = new ProgressUpdateTask(this);
            mUpdateTask.execute(starting_progress, mFutureProgress);
            performBeginningAnimation();
        } else {
            if (DEBUG_LOGGING)
                Log.w(TAG, "setProgress progress already called for same value! = " + progress);
        }
    }

    private void cancelUpdateTask() {
        if (mUpdateTask != null)
            mUpdateTask.cancel(true);
    }

    private void setCurrentProgress(int progress) {
        if (DEBUG_LOGGING) Log.v(TAG, "setCurrentProgress progress = " + progress);
        mCurrentProgress = progress;

        if (mCurrentProgress == mProgressMax)
            performEndingAnimation();
    }


    private void setWidthHeightAndCenter() {

        int left = getLeft() + getPaddingLeft();
        int right = getRight() - getPaddingRight();
        int top = getTop() + getPaddingTop();
        int bottom = getBottom() - getPaddingBottom();

        if (DEBUG_LOGGING) Log.i(TAG, "left = " + left + ", right = " + right);
        if (DEBUG_LOGGING) Log.i(TAG, "top = " + top + ", bottom = " + bottom);
        mWidth = right - left;
        mHeight = bottom - top;
        if (DEBUG_LOGGING) Log.i(TAG, "mWidth = " + mWidth + ", mHeight = " + mHeight);
        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;
        if (DEBUG_LOGGING) Log.i(TAG, "mCenterX = " + mCenterX + ", mCenterY = " + mCenterY);
        mIncreaseStep = (getHalfDiagonal() - mBeginningProgressSize) / getMaxProgress();
        if (DEBUG_LOGGING)
            Log.i(TAG, "mIncreaseStep = " + mIncreaseStep + ", mSleepTime = " + mSleepTime);
    }

    private void performBeginningAnimation() {
        if (mBeginningCrossAnimationView != null && !mBeginningAnimationPerformed) {
            mBeginningInverseProgressAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    instance.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mBeginningCrossAnimationView.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mBeginningCrossAnimationView.startAnimation(mBeginningInverseProgressAnimation);
            this.startAnimation(mBeginningProgressAnimation);
            mBeginningAnimationPerformed = true;
        }
    }

    private void performEndingAnimation() {
        if (mEndingCrossAnimationView != null && !mEndingAnimationPerformed) {
            mEndingInverseProgressAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mEndingCrossAnimationView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    instance.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mEndingCrossAnimationView.startAnimation(mEndingInverseProgressAnimation);
            this.startAnimation(mEndingProgressAnimation);
            mEndingAnimationPerformed = true;
        }
    }

    @Override
    public void onDraw(final Canvas canvas) {
        if (DEBUG_LOGGING) Log.d(TAG, "onDraw");

        canvas.drawCircle(mProgressCircle.centerX, mProgressCircle.centerY,
                mProgressCircle.radius, mProgressCirclePaint);

        if (mTextToPrint != null) {
            canvas.save();
            canvas.translate(mCenterX - getPaddingLeft(), mCenterY - getPaddingTop() - mTextToPrint.getHeight() / 2);
            mTextToPrint.draw(canvas);
            canvas.restore();
        }

    }

    @Override
    protected void onMeasure(final int widthMeasureSpec,
                             final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (DEBUG_LOGGING) Log.d(TAG, "onMeasure");
    }

    @Override
    protected synchronized void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (DEBUG_LOGGING) Log.d(TAG, "onLayout");
        setWidthHeightAndCenter();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (DEBUG_LOGGING) Log.d(TAG, "onSizeChanged");
        setWidthHeightAndCenter();
        mProgressCircle = new Circle(mCenterX, mCenterY, mBeginningProgressSize);
        if (w != oldw || h != oldh) mSizeChanged = true;
        setProgress(mFutureProgress);
    }

    public class ProgressUpdateTask extends AsyncTask<Integer, Integer, Integer> {

        private final ProgressBarLayoutView mProgressBarLayoutView;

        public ProgressUpdateTask(ProgressBarLayoutView view) {
            mProgressBarLayoutView = view;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            if (mProgressCircle == null)
                return null;
            int starting_progress = params[0];
            int final_progress = params[1];
            if (final_progress >= getMaxProgress())
                final_progress = getMaxProgress();

            boolean isIncreasing = starting_progress <= final_progress;

            while (isIncreasing ? starting_progress < final_progress : starting_progress > final_progress) {
                if (isCancelled())
                    break;

                if (isIncreasing) {
                    starting_progress++;
                    mProgressCircle.radius += mIncreaseStep;
                } else {
                    starting_progress--;
                    mProgressCircle.radius -= mIncreaseStep;
                }

                publishProgress(starting_progress);

                try {
                    if (DEBUG_LOGGING)
                        Log.d(TAG, "ProgressUpdateTask doInBackground sleep = " + mSleepTime);
                    Thread.sleep(mSleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return final_progress;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int value = values[0];
            if (DEBUG_LOGGING) Log.d(TAG, "onProgressUpdate progress = " + value);
            mProgressBarLayoutView.setCurrentProgress(value);
            mTextToPrint = mProgressStringFormatter.formatProgressString(value, mTextProgressString, mTextPaint, mWidth, getPaddingLeft(), getPaddingRight());
            invalidate();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }

    private class Circle {
        float radius;
        float centerX;
        float centerY;

        Circle(float x, float y, float innerCircleSize) {
            this.radius = innerCircleSize;
            this.centerX = x;
            this.centerY = y;
        }
    }

    /**
     * IProgressStringFormatterSet declares the interface for custom progress bar label text formatting.
     */
    public interface IProgressStringFormatter {
        /**
         * formatProgressString declares the interface for custom progress bar label text formatting.
         *
         * @param progress       progress
         * @param progressString progress bar string declared in the layout (textProgressString parameter)
         * @param textPaint      {@link android.text.TextPaint} that should be used to paint the text
         * @param width          progress view width
         * @param padding_left   progress view left padding
         * @param padding_right  progress view right padding
         * @return returns the {@link android.text.Layout} that will be used in {@link #onDraw(android.graphics.Canvas)}
         * when drawing the ProgressBarLayoutView. The layout will be drawn in the center of the view.
         */
        public Layout formatProgressString(int progress, String progressString, TextPaint textPaint, int width, int padding_left, int padding_right);

    }

    /**
     * Set the progress bar's string formatter. That way you can make custom progress bar text labels.
     *
     * @param progressStringFormatter progress string formatter {@link com.alterego.progressbarlayout.ProgressBarLayoutView.IProgressStringFormatter}
     */
    public void setProgressStringFormatter(IProgressStringFormatter progressStringFormatter) {
        mProgressStringFormatter = progressStringFormatter;
    }

    private class DefaultProgressStringFormatter implements IProgressStringFormatter {

        @Override
        public Layout formatProgressString(int progress, String progressString, TextPaint textPaint, int width, int padding_left, int padding_right) {
            if (progressString != null) {
                try {
                    String currentProgressString = String.format(progressString, progress);
                    if (DEBUG_LOGGING)
                        Log.v(TAG, "formatProgressString currentProgressString = " + currentProgressString);
                    StaticLayout textToPrint = new StaticLayout(currentProgressString,
                            textPaint,
                            Math.abs((width - padding_left - padding_right)),
                            Layout.Alignment.ALIGN_NORMAL,
                            TEXTPAINT_SPACING_MULT,
                            TEXTPAINT_SPACING_ADD,
                            false);

                    if (DEBUG_LOGGING)
                        Log.d(TAG, "formatProgressString height of static layout = " + textToPrint.getHeight() + ", width = " + textToPrint.getWidth());
                    return textToPrint;
                } catch (Exception e) {
                    Log.e(TAG, "formatProgressString formatting error = " + e.toString());
                }
            }

            return null;
        }
    }

}

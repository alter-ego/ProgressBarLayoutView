package com.alterego.progressbarlayout;

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

//TODO TEST add padding to calculations
//TODO add custom fonts
//TODO add custom step duration setting
//TODO add javadocs

public class ProgressBarLayoutView extends View {

    private static final String TAG = "ProgressBarLayoutView";
    private static final int ANIMATION_DURATION_IN_MS = 1000;
    private final static int MAX_PROGRESS = 100;
    private static final boolean DEBUG_LOGGING = true;

    private Layout.Alignment mAlignmentHorizontal = Layout.Alignment.ALIGN_NORMAL;
    private static final float mSpacingMult = 1.0f;
    private static final float mSpacingAdd = 0.0f;

    private int mWidth = 0;
    private int mHeight = 0;
    private int mCenterX = 0;
    private int mCenterY = 0;
    private int mCurrentProgress = 0;
    private String mTextProgressString;
    private boolean mSizeChanged = false;
    private int mFutureProgress = 0;

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

    private Paint mProgressCirclePaint;
    private Circle mProgressCircle;
    private TextPaint mTextPaint;
    private String mCurrentProgressString = "0%";
    private static int mSleepTime = 8;
    private static float mIncreaseStep = 4;
    private ProgressUpdateTask mUpdateTask;
    private float mBeginningProgressSize;
    private int mProgressCircleColor;
    private int mTextProgressColor;
    private float mTextSize;

    private StaticLayout mTextToPrint;
    private ProgressBarLayoutView instance;


    private View mBeginningCrossAnimationView;
    private Animation mBeginningProgressAnimation;
    private Animation mBeginningInverseProgressAnimation;
    private boolean mBeginningAnimationPerformed = false;

    private View mEndingCrossAnimationView;
    private Animation mEndingProgressAnimation;
    private Animation mEndingInverseProgressAnimation;
    private boolean mEndingAnimationPerformed = false;

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
     * Sets the text typeface for this label
     *
     * @param font Typeface
     */
    public void setTypeface(Typeface font) {
        //TODO setTypeface
        //super.setTypeface(font);
//        mTypeface = font;
//        // This text size has been pre-scaled by the getDimensionPixelOffset method
//        if (mTextPaint != null)
//            mTextPaint.setTypeface(font);
//        requestLayout();
//        invalidate();
    }

    public void setCurrentProgress(int progress) {
        if (DEBUG_LOGGING) Log.v(TAG, "setCurrentProgress progress = " + progress);
        mCurrentProgress = progress;

        if (mCurrentProgress == MAX_PROGRESS)
            performEndingAnimation();
    }

    public void reset() {
        setCurrentProgress(0);
        formatProgressString(mCurrentProgress);
        mProgressCircle.radius = mBeginningProgressSize;
        mBeginningAnimationPerformed = false;
        mEndingAnimationPerformed = false;
        invalidate();
    }

    private void formatProgressString(int progress) {
        if (mTextProgressString != null) {
            try {
                mCurrentProgressString = String.format(mTextProgressString, progress);
                if (DEBUG_LOGGING) Log.v(TAG, "formatProgressString mCurrentProgressString = " + mCurrentProgressString);
                mTextToPrint = new StaticLayout(mCurrentProgressString, mTextPaint, Math.abs((mWidth - getPaddingLeft() - getPaddingRight())), mAlignmentHorizontal, mSpacingMult, mSpacingAdd, false);
                if (DEBUG_LOGGING) Log.d(TAG, "formatProgressString height of static layout = " + mTextToPrint.getHeight() + ", width = " + mTextToPrint.getWidth());
            } catch (Exception e) {
                Log.e(TAG, "formatProgressString formatting error = " + e.toString());
                mCurrentProgressString = null;
            }
        }
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
        int half_diagonal = (int) Math.sqrt((double) mWidth * mWidth + mHeight * mHeight) / 2;
        mIncreaseStep = (half_diagonal - mBeginningProgressSize) / 100;
        mSleepTime = (int) (mIncreaseStep * 1500 / half_diagonal);
        if (DEBUG_LOGGING) Log.i(TAG, "mIncreaseStep = " + mIncreaseStep + ", mSleepTime = " + mSleepTime);
    }

    /**
     * Set size of the inner circle
     *
     * @param size size of the inner circle
     */
    public void setBeginningProgressSize(int size) {
        mBeginningProgressSize = size;
        int half_diagonal = (int) Math.sqrt((double) mWidth * mWidth + mHeight * mHeight) / 2;
        mIncreaseStep = (half_diagonal - mBeginningProgressSize) / 100;
    }

    /**
     * Set color of the progress text
     *
     * @param color color of the progress text
     */
    public void setProgressTextColor(int color) {
        mTextProgressColor = color;
    }

    /**
     * Set color of circle of expanding progress
     *
     * @param color color of circle of expanding progress
     */
    public void setProgresColor(int color) {
        mProgressCircleColor = color;
    }

    /**
     * Set size of the progress text
     *
     * @param size size of the progress text
     */
    public void setTextProgressSize(int size) {
        mTextSize = size;
    }

    /**
     * get maximum value of progress
     *
     * @return maximum value of progress
     */
    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public void setBeginningCrossAnimationView(View view) {
        mBeginningCrossAnimationView = view;
    }

    public void setEndingCrossAnimationView(View view) {
        mEndingCrossAnimationView = view;
    }

    /**
     * set current progress
     *
     * @param progress set progress from 0 to max progress(100)
     */
    public void setProgress(int progress) {
        mFutureProgress = progress;

        if ((mCurrentProgress != mFutureProgress || mSizeChanged)) { //&& (mHeight!=0 && mWidth!=0)
            mSizeChanged = false;
            int starting_progress = mCurrentProgress;
            //mCurrentProgress = progress;

            if (mUpdateTask != null) {
                if (DEBUG_LOGGING) Log.w(TAG, "setProgress cancelling task");
                mUpdateTask.cancel(true);
            }
            if (progress > MAX_PROGRESS) {
                if (DEBUG_LOGGING) Log.i(TAG, "setProgress progress bigger than MAX_PROGRESS = " + progress);
                return;
            }

            if (DEBUG_LOGGING) Log.d(TAG, "setProgress progress = " + mFutureProgress);
            mUpdateTask = new ProgressUpdateTask(this);
            mUpdateTask.execute(starting_progress, mFutureProgress);

            performBeginningAnimation();
        } else {
            if (DEBUG_LOGGING) Log.w(TAG, "setProgress progress already called for same value! = " + progress);
        }
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

        if (mCurrentProgressString != null && mTextToPrint != null) {
            canvas.save();
            canvas.translate(mCenterX - getPaddingLeft(), mCenterY - getPaddingTop() - mTextToPrint.getHeight() / 2); //TODO test
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
            //int final_size = (int) (final_progress * mIncreaseStep + mBeginningProgressSize);
            if (final_progress >= 100)
                final_progress = 100;

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
                    if (DEBUG_LOGGING) Log.d(TAG, "ProgressUpdateTask doInBackground sleep = " + mSleepTime);
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
            formatProgressString(value);
            invalidate();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
        }
    }

    public void cancelUpdateTask() {
        if (mUpdateTask != null)
            mUpdateTask.cancel(true);
    }

}
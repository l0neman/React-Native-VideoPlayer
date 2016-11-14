package com.reactnativevideoplayer.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.reactnativevideoplayer.R;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple video player, wrapped {@link MediaPlayer}.
 * Created by runing on 2016/11/11.
 */

public class VideoPlayer extends ViewGroup implements SurfaceHolder.Callback,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, View.OnClickListener {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(10000);

    private static final int SF_VIEW_ID = generateId();
    private static final int PLAY_BTN_ID = generateId();

    /**
     * Default mode, the video size will be the same as the View size.
     */
    public static final int RESIZE_MODE_STRETCH = 0;

    /**
     * The size of the original video will remain, but the view will have gaps.
     */
    public static final int RESIZE_MODE_CONTAIN = 1;
    /**
     * The size of the original video will remain, but the video will be cropped.
     */
    public static final int RESIZE_MODE_COVER = 2;

    private int mResizeMode = RESIZE_MODE_STRETCH;
    private boolean mIsAutoPlay = false;
    private String mDataSource = "";

    private boolean mCanPlay = true;
    private boolean mIsPlaying = false;
    private boolean mIsNewVideo = false;
    private boolean mIsPlayFinished = false;
    private boolean mCanHideControlBar = true;
    private boolean mAlreadyInitPlayer = false;
    private boolean mStartListenProgress = false;

    private View mControlBar;
    private SeekBar mProgress;
    private TextView mCurrTime;
    private ImageView mPlayBtn;
    private ImageView mStateBtn;
    private TextView mTotalTime;
    private ProgressBar mProgressBar;

    private SurfaceView mSfView;
    private MediaPlayer mPlayer;
    private SurfaceHolder mHolder;

    private final Handler mHandler = new Handler();

    private Pair<ValueAnimator, ValueAnimator> mBottomBarAnimator;
    private ViewVerticalAnimUpdateListener mBottomBarHideAnimatorListener;
    private ViewVerticalAnimUpdateListener mBottomBarShowAnimatorListener;

    private Timer mTimer;

    private static abstract class ViewVerticalAnimUpdateListener implements
            ValueAnimator.AnimatorUpdateListener {
        int viewH;

        void setViewH(int viewH) {
            this.viewH = viewH;
        }
    }

    private final Animator.AnimatorListener mAnimatorListener =
            new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    if (animation == mBottomBarAnimator.second) {
                        mControlBar.setVisibility(VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (animation == mBottomBarAnimator.first) {
                        mControlBar.setVisibility(GONE);
                        cancelAutoHideControlBarTask();
                    } else if (animation == mBottomBarAnimator.second) {
                        startAutoHideControlBarTask();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            };

    private Runnable mListenProgressTask = new Runnable() {
        @Override
        public void run() {
            final int currentPosition = mPlayer.getCurrentPosition();
            mCurrTime.setText(getTimeText(currentPosition));
            mProgress.setProgress(currentPosition);
            mHandler.postDelayed(mListenProgressTask, 1000);
        }
    };

    public VideoPlayer(Context context) {
        this(context, null, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initChildView();
        initHolder();
        initBottomAnimator();
    }

    private void initHolder() {
        mHolder = mSfView.getHolder();
        mHolder.addCallback(this);
    }

    private void initPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setSurface(mHolder.getSurface());

        mPlayer.setOnErrorListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
    }

    private void initBottomAnimator() {
        ValueAnimator hideAnim = ValueAnimator.ofFloat(0F, 1F);
        hideAnim.setDuration(500);
        hideAnim.addListener(mAnimatorListener);

        ValueAnimator showAnim = hideAnim.clone();
        showAnim.setFloatValues(1F, 0F);
        hideAnim.addUpdateListener(mBottomBarHideAnimatorListener =
                new ViewVerticalAnimUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mControlBar.setTranslationY(viewH * (float) animation.getAnimatedValue());
                    }
                });
        showAnim.addUpdateListener(mBottomBarShowAnimatorListener =
                new ViewVerticalAnimUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mControlBar.setTranslationY(viewH * (float) animation.getAnimatedValue());
                    }
                });
        mBottomBarAnimator = new Pair<>(hideAnim, showAnim);
    }

    private void initChildView() {
        mSfView = new SurfaceView(getContext());
        mSfView.setId(SF_VIEW_ID);
        mSfView.setOnClickListener(this);

        mPlayBtn = new ImageView(getContext());
        mPlayBtn.setId(PLAY_BTN_ID);
        mPlayBtn.setImageResource(R.drawable.play_video_player);
        mPlayBtn.setOnClickListener(this);

        mProgressBar = new ProgressBar(getContext());
        mProgressBar.setVisibility(View.GONE);

        addView(mSfView, VideoPlayer.createLP(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
        addView(mPlayBtn, VideoPlayer.createLP(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        addView(mProgressBar, VideoPlayer.createLP(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        final View bottom = getBottomView();
        addView(bottom);
    }

    private View getBottomView() {
        mControlBar = LayoutInflater.from(getContext()).inflate(
                R.layout.bottom_video_player, this, false
        );
        mStateBtn = (ImageView) mControlBar.findViewById(R.id.iv_play);
        mProgress = (SeekBar) mControlBar.findViewById(R.id.sb_progress);
        mCurrTime = (TextView) mControlBar.findViewById(R.id.tv_curr_time);
        mTotalTime = (TextView) mControlBar.findViewById(R.id.tv_total_time);

        mStateBtn.setOnClickListener(this);
        mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mCanHideControlBar = false;
                }
                mCurrTime.setText(getTimeText(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mPlayer.seekTo(seekBar.getProgress());
                startListenProgress();
            }
        });
        mProgress.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (mIsPlaying) {
                        stopListenProgress();
                    }
                }
                return !mIsPlaying;
            }
        });
        return mControlBar;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int wMode = MeasureSpec.getMode(widthMeasureSpec);
        final int hMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (wMode == MeasureSpec.AT_MOST) {
            width = mAlreadyInitPlayer ? mPlayer.getVideoWidth() : 0;
        }
        if (hMode == MeasureSpec.AT_MOST) {
            height = mAlreadyInitPlayer ? mPlayer.getVideoHeight() : 0;
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, wMode),
                MeasureSpec.makeMeasureSpec(height, hMode));

        int btnSize = getMeasuredWidth() / 6;
        mPlayBtn.setLayoutParams(createLP(btnSize, btnSize));

        final int controlBarH = mControlBar.getMeasuredHeight();
        mBottomBarHideAnimatorListener.setViewH(controlBarH);
        mBottomBarShowAnimatorListener.setViewH(controlBarH);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final int childSize = child.getMeasuredWidth();
            if (child instanceof ImageView || child instanceof ProgressBar) {
                child.layout((width - childSize) / 2, (height - childSize) / 2,
                        (width + childSize) / 2, (height + childSize) / 2);
            } else if (child instanceof SurfaceView) {
                layoutSurface(child, width, height);
            } else {
                child.layout(0, height - child.getMeasuredHeight(), width, height);
            }
        }
    }

    private LayoutSurfaceTask mLayoutSurfaceTask;

    private class LayoutSurfaceTask implements Runnable {

        private int parentW;
        private int parentH;

        private LayoutSurfaceTask(int parentW, int parentH) {
            this.parentW = parentW;
            this.parentH = parentH;
        }

        @Override
        public void run() {
            final int videoW = mPlayer.getVideoWidth();
            final int videoH = mPlayer.getVideoHeight();
            float scale = (float) videoH / videoW;
            float frameScale = (float) parentH / parentW;
            if (mResizeMode == RESIZE_MODE_CONTAIN) {
                if (frameScale > scale) {
                    final int targetH = (int) (parentW * scale);
                    mSfView.layout(0, (parentH - targetH) / 2, parentW, (parentH + targetH) / 2);
                } else {
                    final int targetW = (int) (parentH / scale);
                    mSfView.layout((parentW - targetW) / 2, 0, (parentW + targetW) / 2, parentH);
                }
            } else if (mResizeMode == RESIZE_MODE_COVER) {
                if (frameScale > scale) {
                    final int targetW = (int) (parentH / scale);
                    mSfView.layout((parentW - targetW) / 2, 0, (parentW + targetW) / 2, parentH);
                } else {
                    final int targetH = (int) (parentW * scale);
                    mSfView.layout((parentH - targetH) / 2, 0, parentW, (parentH + targetH) / 2);
                }
            }
        }

    }

    private void layoutSurface(View view, int parentW, int parentH) {
        if (mResizeMode == RESIZE_MODE_CONTAIN || mResizeMode == RESIZE_MODE_COVER) {
            if (!mAlreadyInitPlayer || isInvalidVideo()) {
                if (mLayoutSurfaceTask == null) {
                    view.layout(0, 0, parentW, parentH);
                    mLayoutSurfaceTask = new LayoutSurfaceTask(parentW, parentH);
                }
            }
        } else if (mResizeMode == RESIZE_MODE_STRETCH) {
            view.layout(0, 0, parentW, parentH);
        }
    }

    private static LayoutParams createLP(int width, int height) {
        return new LayoutParams(width, height);
    }

    /**
     * Set the video display mode
     *
     * @param resizeMode target mode
     */
    public void setResizeMode(int resizeMode) {
        if (this.mResizeMode == resizeMode) {
            return;
        }
        this.mResizeMode = resizeMode;
        if (mAlreadyInitPlayer && !isInvalidVideo() && mLayoutSurfaceTask != null) {
            mLayoutSurfaceTask.run();
        }
    }

    /**
     * Open automatically after setting the url
     */
    public void enableAuto() {
        this.mIsAutoPlay = true;
    }

    /**
     * Sets the video address
     *
     * @param uri target video uri
     */
    public void setUrl(String uri) {
        if (uri.equals(this.mDataSource)) {
            mIsNewVideo = false;
        } else {
            mIsNewVideo = true;
        }
        this.mDataSource = uri;
    }

    private boolean isInvalidVideo() {
        return mPlayer.getVideoWidth() == 0 || mPlayer.getVideoHeight() == 0;
    }

    private void showErrorToast() {
        Toast.makeText(getContext(), "Invalid video address!", Toast.LENGTH_SHORT).show();
    }

    private void startListenProgress() {
        if (!mStartListenProgress) {
            mHandler.post(mListenProgressTask);
            mStartListenProgress = true;
        }
    }

    private void stopListenProgress() {
        if (mStartListenProgress) {
            mHandler.removeCallbacks(mListenProgressTask);
            mStartListenProgress = false;
        }
    }

    private void hidePlayBtn() {
        mPlayBtn.setVisibility(View.GONE);
    }

    private void showPlayBtn() {
        mPlayBtn.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        recycle();
    }

    /**
     * Stop play
     */
    public void stop(){
        if(mIsPlaying){
            resetPlayer();
        }
    }

    /**
     * Recycle videoPlayer manually
     */
    public void recycle() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mBottomBarAnimator.first.isRunning()) {
            mBottomBarAnimator.first.cancel();
        }
        if (mBottomBarAnimator.second.isRunning()) {
            mBottomBarAnimator.first.cancel();
        }
        stopListenProgress();
        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initPlayer();
        mAlreadyInitPlayer = true;
        if (mIsAutoPlay) {
            prepareNewVideo();
            play();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        resetPlayer();
    }

    private void resetPlayer() {
        stopListenProgress();
        mPlayer.stop();
        mIsPlayFinished = true;
        mIsPlaying = false;
        refreshViewInStop();
    }

    private void refreshViewInStop() {
        mProgress.setMax(0);
        mProgress.setProgress(0);
        showPlayBtn();
        mStateBtn.setImageResource(R.drawable.play_video_player);
        mCurrTime.setText("00:00");
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mProgress.setSecondaryProgress(percent * mProgress.getMax() / 100);
    }

    private void hideControlBar() {
        if (!mBottomBarAnimator.first.isRunning()) {
            mBottomBarAnimator.first.start();
        }
    }

    private void showControlBar() {
        if (!mBottomBarAnimator.second.isRunning()) {
            mBottomBarAnimator.second.start();
        }
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == SF_VIEW_ID) {
            if (mControlBar.getVisibility() == VISIBLE) {
                hideControlBar();
            } else {
                showControlBar();
            }
        } else if (id == PLAY_BTN_ID || id == R.id.iv_play) {
            pressPlay();
        }
    }

    private void play() {
        mPlayer.start();
        refreshViewInPlay();
        mIsPlaying = true;
        mIsNewVideo = false;
        mIsPlayFinished = false;
        startAutoHideControlBarTask();
    }

    private void cancelAutoHideControlBarTask() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void startAutoHideControlBarTask() {
        if (mPlayer.isPlaying() && mControlBar.getVisibility() == VISIBLE) {
            mCanHideControlBar = true;
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mCanHideControlBar) {
                                mTimer = null;
                                hideControlBar();
                            }
                        }
                    });
                }
            }, 3000);
        }
    }

    private void refreshViewInPlay() {
        startListenProgress();
        mStateBtn.setImageResource(R.drawable.pause_video_player);
        hidePlayBtn();
    }

    private void pause() {
        mCanHideControlBar = false;
        mPlayer.pause();
        refreshViewInPause();
        mIsPlaying = false;
    }

    private void refreshViewInPause() {
        stopListenProgress();
        mStateBtn.setImageResource(R.drawable.play_video_player);
        showPlayBtn();
    }

    private void pressPlay() {
        if (TextUtils.isEmpty(mDataSource) || !mCanPlay) {
            return;
        }
        if (mIsPlaying) {
            pause();
        } else {
            if (mIsNewVideo) {
                prepareNewVideo();
                return;
            } else if (mIsPlayFinished) {
                prepareOldVideo();
            }
            play();
        }
    }

    private void showLoading() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        mProgressBar.setVisibility(View.GONE);
    }

    private void prepareNewVideo() {
        if (!mAlreadyInitPlayer) {
            return;
        }
        try {
            mPlayer.reset();
            mPlayer.setDataSource(mDataSource);
            showLoading();
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mIsNewVideo) {
            hideLoading();
            if (isInvalidVideo()) {
                showErrorToast();
                mCanPlay = false;
                return;
            }
            if (mLayoutSurfaceTask != null) {
                mLayoutSurfaceTask.run();
            }
            initProgress();
            play();
        }
    }

    private void prepareOldVideo() {
        try {
            mPlayer.prepare();
            initProgress();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initProgress() {
        final int duration = mPlayer.getDuration();
        mProgress.setMax(duration);
        mTotalTime.setText(getTimeText(duration));
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        showErrorToast();
        mCanPlay = false;
        return true;
    }

    private static String getTimeText(int milliseconds) {
        final int seconds = milliseconds / 1000;
        String min = seconds / 60 + "";
        if (min.length() == 1) {
            min = "0" + min;
        }
        String sec = seconds % 60 + "";
        if (sec.length() == 1) {
            sec = "0" + sec;
        }
        return min + ":" + sec;
    }

    private static int generateId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1;
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }
}

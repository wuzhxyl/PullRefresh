package com.ilifesmart.weather.widgets;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ilifesmart.weather.R;

/**
 * Created by hlkhjk_ok on 2018/1/10.
 */

/*
*  适用于下面那个是RecyclerView或ListView. 默认为纵向
*  example:
*  <com.ilifesmart.weather.widgets.PullRefreshLinearLayout
*    xmlns:android="http://schemas.android.com/apk/res/android"
*    xmlns:app="http://schemas.android.com/apk/res-auto"
*    xmlns:tools="http://schemas.android.com/tools"
*    android:layout_width="match_parent"
*    android:layout_height="match_parent"
*    tools:context="com.ilifesmart.weather.activity.TestActivity">
*
*    <android.support.v7.widget.RecyclerView
*        android:layout_width="match_parent"
*        android:layout_height="match_parent"
*        android:id="@+id/recyclerview"
*    />
*  </com.ilifesmart.weather.widgets.PullRefreshLinearLayout>
*
* */
public class PullRefreshLinearLayout extends LinearLayout implements View.OnTouchListener {

    private String TAG = "PullRefreshLinearLayout";

    private View mHeader;                           // 头部区
    private TextView mHeaderTextView;               //头部文案提示
    private ImageView mHeaderImageView;             //头部ProgressImg
    private RecyclerView mRecyclerView;             //内部滑动区
    private MarginLayoutParams mHeaderLayoutParams; //头部区LayutParams

    private int touchSlop;                          // 认为是滑动的最小间隔
    private int mHeaderHeight;                      // 头部高度
    private int yDown;                              // 初次按下的位置
    private int yMove;                              // 按下时候的位置
    private int yLast = 0;                          // 上次按下的位置

    boolean ableToPull = false;                     // 是否允许下拉
    private boolean loadOnce;

    // State.
    final protected int STATE_IDLE = 0;             // 空闲
    final protected int STATE_PULLING = 1;          //拖动中
    final protected int STATE_TO_RELEASE = 2;       //准备刷新
    final protected int STATE_REFRESHING = 3;       //正在刷新
    final protected int STATE_RELEASING = 4;        //释放

    protected int currState;

    private PullRefreshListener mRefreshListener;

    public PullRefreshLinearLayout(Context context) {
        this(context, null);
    }

    public PullRefreshLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mHeader = LayoutInflater.from(context).inflate(R.layout.header_progress, null, true);
        mHeaderTextView = mHeader.findViewById(R.id.header_text);
        mHeaderImageView = mHeader.findViewById(R.id.header_loading);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOrientation(VERTICAL);
        addView(mHeader, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (!loadOnce) {
            mHeaderLayoutParams = (MarginLayoutParams) mHeader.getLayoutParams();
            mHeaderHeight = -(mHeader.getHeight());
            mHeaderLayoutParams.topMargin = mHeaderHeight;
            mHeader.setLayoutParams(mHeaderLayoutParams);

            mRecyclerView = (RecyclerView) getChildAt(1);
            mRecyclerView.setOnTouchListener(this);
            loadOnce = true;
            setCurrState(STATE_IDLE);
        }
    }

    private void resetHeaderTopMargin() {
        setHeadTopMargin(mHeaderHeight);
    }

    private void setHeaderTopMarginByStep(int yStep) {
        int topMargin = mHeaderLayoutParams.topMargin += yStep;
        if (topMargin > 0) {
            topMargin = 0;
        }

        setHeadTopMargin(topMargin);
    }

    private void setHeadTopMargin(int topMargin) {
        mHeaderLayoutParams.topMargin = topMargin;
        mHeader.setLayoutParams(mHeaderLayoutParams);
    }

    private void ableToPullDown(MotionEvent event) {
        View firstChild = mRecyclerView.getChildAt(0);

        if (firstChild != null) {
            int first = ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            final RecyclerView.LayoutParams lpParams = (RecyclerView.LayoutParams) firstChild.getLayoutParams();

            if (first == 0 && firstChild.getTop() == (lpParams.topMargin)) {
                ableToPull = true;
            } else {
                if (currState == STATE_PULLING || currState == STATE_TO_RELEASE) {
                } else {
                    resetHeaderTopMargin();
                    ableToPull = false;
                }
            }
        } else {
            ableToPull = true;
        }
    }

    private String stateToString(int state) {
        String stateString = "";
        switch (state) {
            case STATE_IDLE:
                stateString = "IDLE";
                break;
            case STATE_PULLING:
                stateString = "PULLING";
                break;
            case STATE_TO_RELEASE:
                stateString = "TO_RELEASING";
                break;
            case STATE_REFRESHING:
                stateString = "REFRESHING";
                break;
            case STATE_RELEASING:
                stateString = "RELEASING";
        }
        return stateString;
    }

    private void setCurrState(int state) {
        currState = state;

        int mTitleId;
        switch (state) {
            case STATE_IDLE:
            case STATE_PULLING:
                mTitleId = R.string.pull_to_refresh;
                break;
            case STATE_TO_RELEASE:
                mTitleId = R.string.release_to_refresh;
                break;
            case STATE_REFRESHING:
                mTitleId = R.string.refreshing;
                break;
            case STATE_RELEASING:
            default:
                mTitleId = R.string.none_state;
        }

        mHeaderTextView.setText(mTitleId);
        if (mRefreshListener != null) {
            mRefreshListener.onStateChanged(state);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        ableToPullDown(event);

        if (ableToPull) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    yLast = yDown = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    yMove = (int) event.getRawY();
                    int yDistance = (yMove - yLast);

                    if (yDistance < 0 && mHeaderLayoutParams.topMargin <= mHeaderHeight) {
                        setCurrState(STATE_IDLE);
                        return false;
                    }

                    if (Math.abs(yMove - yDown) < touchSlop) {
                        return false;
                    }

                    if (currState != STATE_RELEASING) {
                        if (yDistance >= 0 && mHeaderLayoutParams.topMargin >= 0) {
                            setHeaderTopMarginByStep(0);
                            setCurrState(STATE_TO_RELEASE);
                        } else {
                            float degree = (360f * yDistance / mHeaderHeight + mHeaderImageView.getRotation()) % 360;
                            mHeaderImageView.setRotation(degree);
                            setHeaderTopMarginByStep(yDistance);
                            setCurrState(STATE_PULLING);
                        }
                    }
                    yLast = yMove;
                    break;
                case MotionEvent.ACTION_UP:
                    if (currState == STATE_PULLING) {
                        backToTop();
                    } else if (currState == STATE_TO_RELEASE) {
                        final float startdegree = mHeaderImageView.getRotation();
                        RotateAnimation anim = new RotateAnimation(startdegree, startdegree+360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                        anim.setDuration(500);
                        anim.setFillAfter(true);
                        anim.setRepeatCount(Animation.INFINITE);
                        anim.setRepeatMode(Animation.RESTART);

                        mHeaderImageView.startAnimation(anim);
                        setCurrState(STATE_REFRESHING);
                        runASyncTask();
                    }
                    break;
            }

            if (currState == STATE_PULLING || currState == STATE_TO_RELEASE || currState == STATE_RELEASING || currState == STATE_REFRESHING) {
                return true;
            }
        }
        return false;
    }

    private void backToTop() {
        setCurrState(STATE_RELEASING);
        final ValueAnimator valueAnimator = ValueAnimator.ofInt(mHeaderLayoutParams.topMargin, mHeaderHeight);
        valueAnimator.setDuration(1000);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Number number = (Number) animation.getAnimatedValue();
                setHeadTopMargin(number.intValue());
            }
        });

        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setCurrState(STATE_IDLE);
//                valueAnimator.cancel();
                animation.cancel();
                Log.d(TAG, "onAnimationEnd: isCancel " + valueAnimator.isRunning());
                resetHeaderTopMargin();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

        });
        valueAnimator.start();
    }

    private void runASyncTask() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mRefreshListener != null) {
                        mRefreshListener.execute();
                    } else {
                        // DEBUG.
                        Thread.sleep(3_000);
                    }
                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mHeaderImageView.clearAnimation();
                            backToTop();
                        }
                    });
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            }
        }).start();
    }

    public interface PullRefreshListener {
        void onStateChanged(int state);
        void execute();
    }

    public void registerListener(PullRefreshListener listener) {
        if (mRefreshListener != null) {
            return;
        }

        mRefreshListener = listener;
    }
}
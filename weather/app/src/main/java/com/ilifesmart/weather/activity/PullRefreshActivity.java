package com.ilifesmart.weather.activity;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ilifesmart.weather.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PullRefreshActivity extends AppCompatActivity implements View.OnTouchListener {

    @BindView(R.id.content_conrainer)
    LinearLayout mContentConrainer;
    protected String TAG = "PullRefreshActivity";

    @BindView(R.id.header_loading)
    ImageView mHeaderLoading;
    @BindView(R.id.header_text)
    TextView mHeaderText;
    @BindView(R.id.header_progress)
    LinearLayout mHeaderProgress;

    private ViewGroup.MarginLayoutParams mHeaderLayoutParams; //头部区LayoutParams

    private int touchSlop;                          // 认为是滑动的最小间隔
    private int mHeaderHeight;                      // 头部高度
    private int yDown;                              // 初次按下的y位置
    private int xDown;                              // 初次按下的x位置
    private int yMove;                              // 按下时候的y位置
    private int xMove;                              // 按下时候的x位置
    private int yLast = 0;                          // 上次按下的y位置
    private int xLast = 0;                          // 上次按下的x位置

    private boolean loadOnce;

    // State.
    final protected int STATE_IDLE = 0;             // 空闲
    final protected int STATE_PULLING = 1;          //拖动中
    final protected int STATE_TO_RELEASE = 2;       //准备刷新
    final protected int STATE_REFRESHING = 3;       //正在刷新
    final protected int STATE_RELEASING = 4;        //释放

    protected int currState;
    protected PullRefreshListener mRefreshListener;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setCurrState(STATE_IDLE);
                yMove = yLast = yDown = (int) event.getRawY();
                xMove = xLast = xDown = (int) event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                yMove = (int) event.getRawY();
                xMove = (int) event.getRawX();

                int yDistance = yMove - yLast;
                if (Math.abs(yDistance) > Math.abs(xMove - xDown)) {
                    setCurrState(STATE_PULLING);
                } else {
                    return false;
                }

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
                        float degree = (360f * yDistance / mHeaderHeight + mHeaderProgress.getRotation()) % 360;
                        mHeaderLoading.setRotation(degree);
                        setHeaderTopMarginByStep(yDistance);
                        setCurrState(STATE_PULLING);
                    }
                }

                yLast = yMove;
                xLast = xMove;
                break;
            case MotionEvent.ACTION_UP:
                if (currState == STATE_PULLING) {
                    backToTop();
                } else if (currState == STATE_TO_RELEASE) {
                    final float startdegree = mHeaderLoading.getRotation();
                    RotateAnimation anim = new RotateAnimation(startdegree, startdegree + 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    anim.setDuration(500);
                    anim.setFillAfter(true);
                    anim.setRepeatCount(Animation.INFINITE);
                    anim.setRepeatMode(Animation.RESTART);

                    mHeaderLoading.startAnimation(anim);
                    setCurrState(STATE_REFRESHING);
                    runASyncTask();
                } else {
                    return false; //让view去处理自身的touchevent事件.
                }
                xDown = xMove = xLast = 0;
                yDown = yMove = yLast = 0;
                break;
        }

        if (currState == STATE_PULLING || currState == STATE_TO_RELEASE || currState == STATE_RELEASING || currState == STATE_REFRESHING) {
            Log.d(TAG, "onTouch: 拖动中..");
        }

        return true;
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
                valueAnimator.cancel();
                animation.cancel();
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mHeaderProgress.clearAnimation();
                            backToTop();
                        }
                    });
                } catch (Exception exp) {
                    exp.printStackTrace();
                }
            }
        }).start();
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

        mHeaderText.setText(mTitleId);
        if (mRefreshListener != null) {
            mRefreshListener.onStateChanged(state);
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
        mHeaderProgress.setLayoutParams(mHeaderLayoutParams);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!loadOnce) {
            mHeaderLayoutParams = (ViewGroup.MarginLayoutParams) mHeaderProgress.getLayoutParams();
            mHeaderHeight = -mHeaderProgress.getHeight();
            mHeaderLayoutParams.topMargin = mHeaderHeight;

            mHeaderProgress.setLayoutParams(mHeaderLayoutParams);
            touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
            mContentConrainer.setOnTouchListener(this);
            setCurrState(STATE_IDLE);
            loadOnce = true;
        }
    }

    protected void addListener(PullRefreshListener listener) {
        if (mRefreshListener != null) {
            return;
        }

        mRefreshListener = listener;
    }
}

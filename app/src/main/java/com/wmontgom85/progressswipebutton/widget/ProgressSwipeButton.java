package com.wmontgom85.progressswipebutton.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wmontgom85.progressswipebutton.R;

public class ProgressSwipeButton extends RelativeLayout {
    private View rootView;

    private float initialX;
    private boolean buttonExpanded;
    private boolean disabled = false;
    private boolean canMove;
    private int initialButtonWidth = 0;

    private RelativeLayout loadingBG;
    private LinearLayout slidingButton;
    private TextView centerText;

    private ProgressBar circular_progress;
    private RelativeLayout bg_progress;
    private ImageView swipe_check;

    AnimatorSet animatorSet = new AnimatorSet();
    AnimatorSet expandAnimatorSet;
    AnimatorSet collapseAnimatorSet = new AnimatorSet();
    AnimatorSet buttonResetAnimatorSet = new AnimatorSet();

    public void disable() {
        disabled = true;
        setOnTouchListener(null);
    }

    public ProgressSwipeButton(Context context) {
        super(context);
        init(context, null, -1, -1);
    }

    public ProgressSwipeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, -1, -1);
    }

    public ProgressSwipeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, -1);
    }

    @TargetApi(21)
    public ProgressSwipeButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this.rootView = inflate(context, R.layout.customview_swipe_button, this);

        // add the center text
        centerText = rootView.findViewById(R.id.resting_text);
        slidingButton = rootView.findViewById(R.id.slide_button);
        circular_progress = rootView.findViewById(R.id.circular_progress);
        bg_progress = rootView.findViewById(R.id.bg_progress);
        swipe_check = rootView.findViewById(R.id.swipe_check);

        setOnTouchListener(getButtonTouchListener());
    }

    private void resizeRestingText(){
        slidingButton.measure(0,0);
        int margin = slidingButton.getMeasuredWidthAndState();

        MarginLayoutParams params = (MarginLayoutParams) centerText.getLayoutParams();
        params.setMarginStart(margin + 10);
        centerText.setLayoutParams(params);
    }

    public void setButtonText(String text){
        ((TextView) rootView.findViewById(R.id.slide_button_text)).setText(text);
        resizeRestingText();
    }

    private boolean animating = false;

    private boolean haptic_initial = false;
    private boolean haptic_final = false;

    @SuppressLint("ClickableViewAccessibility")
    private OnTouchListener getButtonTouchListener() {
        return (View v, MotionEvent event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    canMove = true;
                    if (canMove) {
                        if (event.getX() <= (slidingButton.getX() + slidingButton.getWidth())) {
                            canMove = true;
                        } else {
                            canMove = false;
                        }
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (canMove) {
                        updateSlidePos(event.getX());

                        return true;
                    }
                case MotionEvent.ACTION_UP:
                    if (canMove) {
                        buttonRelease();

                        return true;
                    }
            }

            return false;
        };
    }

    private void updateSlidePos(float x) {
        if (!haptic_initial && !disabled) {
            haptic_initial = true;
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }

        double maxWidth = getWidth() * 0.95;

        if (initialX == 0) {
            initialX = slidingButton.getX();
        }

        if (x > initialX + slidingButton.getWidth() / 2 && x + slidingButton.getWidth() / 2 < getWidth()) {
            slidingButton.setX(x - slidingButton.getWidth() / 2);
            centerText.setAlpha(1 - 1.3f * (slidingButton.getX() + slidingButton.getWidth()) / getWidth());
        }

        if (x + slidingButton.getWidth() / 2 > getWidth() && slidingButton.getX() + slidingButton.getWidth() / 2 < getWidth()) {
            slidingButton.setX(getWidth() - slidingButton.getWidth());
        }

        if (x < slidingButton.getWidth() / 2 && slidingButton.getX() > 0) {
            slidingButton.setX(0);
        }

        double perc = (slidingButton.getX() / (maxWidth - slidingButton.getWidth())) * 100;
        circular_progress.setProgress((int)perc);
        bg_progress.setAlpha((float)perc/100);

        if (perc >= 100) {
            if (!haptic_final && !disabled) {
                haptic_final = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }

            if (!animating && swipe_check.getVisibility() != View.VISIBLE) {
                animating = true;
                circular_progress.animate().scaleX(0).scaleY(0).setDuration(200).withEndAction(() -> {
                    swipe_check.setVisibility(View.VISIBLE);
                    circular_progress.setScaleX(1);
                    circular_progress.setScaleY(1);
                });
            }
        } else {
            haptic_final = false;
            animating = false;
            swipe_check.setVisibility(View.INVISIBLE);
        }
    }

    private void buttonRelease() {
        haptic_initial = false;
        haptic_final = false;

        circular_progress.setProgress(0);
        bg_progress.setAlpha(0);

        if (buttonExpanded) {
            collapseButton();
        } else {
            initialButtonWidth = slidingButton.getWidth();

            if (slidingButton.getX() + slidingButton.getWidth() > getWidth() * 0.95) {
                buttonExpanded = true;
                expandButton(() -> collapseButton());
            } else {
                moveButtonBack();
            }
        }
    }

    public void expandButton(final Runnable callback) {
        final ValueAnimator positionAnimator = ValueAnimator.ofFloat(slidingButton.getX(), 0);
        positionAnimator.addUpdateListener((ValueAnimator animation) -> {
            float x = (Float) positionAnimator.getAnimatedValue();
            slidingButton.setX(x);
        });

        final ValueAnimator widthAnimator = ValueAnimator.ofInt(slidingButton.getWidth(), getWidth());
        widthAnimator.addUpdateListener((ValueAnimator animation) -> {
            ViewGroup.LayoutParams params = slidingButton.getLayoutParams();
            params.width = (Integer) widthAnimator.getAnimatedValue();
            slidingButton.setLayoutParams(params);
        });

        if (expandAnimatorSet != null) {
            expandAnimatorSet.cancel();
            expandAnimatorSet = null;
        }

        expandAnimatorSet = new AnimatorSet();
        expandAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                buttonExpanded = true;
                if (callback != null) {
                    callback.run();
                }
            }
        });
        expandAnimatorSet.playTogether(positionAnimator, widthAnimator);
        expandAnimatorSet.start();
    }

    public void collapseButton() {
        final ValueAnimator widthAnimator = ValueAnimator.ofInt(slidingButton.getWidth(), initialButtonWidth);
        widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                ViewGroup.LayoutParams params =  slidingButton.getLayoutParams();
                params.width = (Integer) widthAnimator.getAnimatedValue();
                slidingButton.setLayoutParams(params);
            }
        });
        widthAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                buttonExpanded = false;
            }
        });

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(centerText, "alpha", 1);

        collapseAnimatorSet.playTogether(objectAnimator, widthAnimator);
        collapseAnimatorSet.start();
    }

    public void moveButtonBack() {
        final ValueAnimator positionAnimator = ValueAnimator.ofFloat(slidingButton.getX(), 0);
        positionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        positionAnimator.addUpdateListener((ValueAnimator animation) -> {
            float x = (Float) positionAnimator.getAnimatedValue();
            slidingButton.setX(x);
        });

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(centerText, "alpha", 1);

        buttonResetAnimatorSet.setDuration(200);
        buttonResetAnimatorSet.playTogether(objectAnimator, positionAnimator);
        buttonResetAnimatorSet.start();
    }
}

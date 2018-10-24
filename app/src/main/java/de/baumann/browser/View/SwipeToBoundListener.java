package de.baumann.browser.View;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.*;
import de.baumann.browser.Unit.ViewUnit;

public class SwipeToBoundListener implements View.OnTouchListener {
    public interface BoundCallback {
        boolean canSwipe();
        void onSwipe();
        void onBound(boolean canSwitch, boolean left);
    }

    private final View view;
    private final BoundCallback callback;

    private int targetWidth = 1;
    private final int slop;
    private final long animTime;

    private float downX;
    private float translationX;
    private boolean swiping;
    private boolean swipingLeft;
    private boolean canSwitch;
    private int swipingSlop;
    private VelocityTracker velocityTracker;

    public SwipeToBoundListener(View view, BoundCallback callback) {
        this.view = view;
        this.callback = callback;

        ViewConfiguration configuration = ViewConfiguration.get(this.view.getContext());
        this.slop = configuration.getScaledTouchSlop();
        this.animTime = this.view.getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        this.swiping = false;
        this.swipingLeft = false;
        this.canSwitch = false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!callback.canSwipe()) {
            return false;
        }

        event.offsetLocation(translationX, 0);
        if (targetWidth < 2) {
            targetWidth = view.getWidth();
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getRawX();

                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);

                return false;
            } case MotionEvent.ACTION_UP: {
                if (velocityTracker == null) {
                    break;
                }

                velocityTracker.addMovement(event);
                velocityTracker.computeCurrentVelocity(1000);

                if (swiping) {
                    view.animate()
                            .translationX(0f)
                            .setDuration(animTime)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    callback.onBound(canSwitch, swipingLeft);
                                }
                            });
                }

                downX = 0;
                translationX = 0;
                swiping = false;
                velocityTracker.recycle();
                velocityTracker = null;

                break;
            } case MotionEvent.ACTION_CANCEL: {
                if (velocityTracker == null) {
                    break;
                }

                view.animate()
                        .translationX(0f)
                        .setDuration(animTime)
                        .setListener(null);

                downX = 0;
                translationX = 0;
                swiping = false;
                velocityTracker.recycle();
                velocityTracker = null;

                break;
            } case MotionEvent.ACTION_MOVE: {
                if (velocityTracker == null) {
                    break;
                }

                velocityTracker.addMovement(event);

                float deltaX = event.getRawX() - downX;
                if (Math.abs(deltaX) > slop) {
                    swiping = true;
                    swipingLeft = deltaX < 0;
                    canSwitch = Math.abs(deltaX) >= ViewUnit.dp2px(view.getContext(), 48); // Can switch tabs when deltaX >= 48 to prevent misuse
                    swipingSlop = (deltaX > 0 ? slop : -slop);
                    view.getParent().requestDisallowInterceptTouchEvent(true);

                    MotionEvent cancelEvent = MotionEvent.obtainNoHistory(event);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL | (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                    view.onTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                if (swiping) {
                    translationX = deltaX;
                    view.setTranslationX(deltaX - swipingSlop);
                    callback.onSwipe();

                    return true;
                }

                break;
            }
        }

        return false;
    }
}

package de.baumann.browser.View;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import de.baumann.browser.Ninja.R;
import de.baumann.browser.Unit.ViewUnit;

public class SwitcherPanel extends ViewGroup {
    private View switcherView;
    private View mainView;
    private RelativeLayout omnibox;

    /* slideRange: px */
    private float slideRange = 0f;
    private float slideOffset = 1f;
    private float interceptX = 0f;
    private float interceptY = 0f;

    /* coverHeight: px */
    private float coverHeight = 0f;
    public void setCoverHeight(float coverHeight) {
        this.coverHeight = coverHeight;
    }

    /* flingVelocity: dp/s */
    private void setFlingVelocity() {
        if (dragHelper != null) {
            dragHelper.setMinVelocity(ViewUnit.dp2px(getContext(), 256));
        }
    }

    private boolean keyBoardShowing = false;
    public boolean isKeyBoardShowing() {
        return !keyBoardShowing;
    }

    /* mainView's status */
    public enum Status {
        EXPANDED,
        COLLAPSED,
        FLING
    }
    private static final Status STATUS_DEFAULT = Status.EXPANDED;
    private Status status = STATUS_DEFAULT;
    public Status getStatus() {
        return status;
    }

    public interface StatusListener {
        void onCollapsed();
    }
    private StatusListener statusListener;
    public void setStatusListener(StatusListener statusListener) {
        this.statusListener = statusListener;
    }

    private final ViewDragHelper dragHelper;
    private class DragHelperCallback extends ViewDragHelper.Callback {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            return child == mainView;
        }

        @Override
        public int getViewVerticalDragRange(@NonNull View child) {
            return (int) slideRange;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            fling(top);
            invalidate();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (dragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                slideOffset = computeSlideOffset(mainView.getTop());

                if (slideOffset == 1f && status != Status.EXPANDED) {
                    status = Status.EXPANDED;
                    switcherView.setEnabled(false);
                } else if (slideOffset == 0f && status != Status.COLLAPSED) {
                    status = Status.COLLAPSED;
                    dispatchOnCollapsed();
                }
            }
        }
    }

    private static class LayoutParams extends MarginLayoutParams {
        private static final int[] ATTRS = new int[] {
                android.R.attr.layout_weight
        };
        private LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }
        private LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
        private LayoutParams(MarginLayoutParams source) {
            super(source);
        }
        private LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray typedArray = c.obtainStyledAttributes(attrs, ATTRS);
            typedArray.recycle();
        }
    }

    public SwitcherPanel(Context context) {
        this(context, null);
    }
    public SwitcherPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public SwitcherPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        dragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        setFlingVelocity();
        setWillNotDraw(false);

        float dimen108dp = getResources().getDimensionPixelSize(R.dimen.layout_height_108dp);
        float dimen16dp = getResources().getDimensionPixelOffset(R.dimen.layout_margin_16dp);
        int windowHeight = ViewUnit.getWindowHeight(context);
        int statusBarHeight = ViewUnit.getStatusBarHeight(context);

        coverHeight = windowHeight - statusBarHeight - dimen108dp - dimen16dp;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof MarginLayoutParams ? new LayoutParams((MarginLayoutParams) layoutParams) : new LayoutParams(layoutParams);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams && super.checkLayoutParams(layoutParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT.");
        } else if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT.");
        } else if (getChildCount() != 2) {
            throw new IllegalStateException("SwitcherPanel layout must have exactly 2 children!");
        }

        switcherView = getChildAt(0);
        mainView = getChildAt(1);
        omnibox = mainView.findViewById(R.id.main_omnibox);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();
        int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int width = layoutWidth;
            int height = layoutHeight;
            if (child == switcherView) {
                height = (int) (height - coverHeight);
                width = width - layoutParams.leftMargin - layoutParams.rightMargin;
            } else if (child == mainView) {
                height = height - layoutParams.topMargin;
            }

            int childWidthSpec;
            switch (layoutParams.width) {
                case ViewGroup.LayoutParams.WRAP_CONTENT:
                    childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
                    break;
                case ViewGroup.LayoutParams.MATCH_PARENT:
                    childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
                    break;
                default:
                    childWidthSpec = MeasureSpec.makeMeasureSpec(layoutParams.width, MeasureSpec.EXACTLY);
                    break;
            }

            int childHeightSpec;
            switch (layoutParams.height) {
                case ViewGroup.LayoutParams.WRAP_CONTENT:
                    childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
                    break;
                case ViewGroup.LayoutParams.MATCH_PARENT:
                    childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                    break;
                default:
                    childHeightSpec = MeasureSpec.makeMeasureSpec(layoutParams.height, MeasureSpec.EXACTLY);
                    break;
            }

            child.measure(childWidthSpec, childHeightSpec);
            if (child == mainView) {
                slideRange = mainView.getMeasuredHeight() - coverHeight;
            }
        }

        setMeasuredDimension(widthSize, heightSize);
        keyBoardShowing = heightSize < getHeight(); ///
    }

    @Override
    protected void onLayout(boolean change, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            int top = paddingTop;
            if (child == mainView) {
                top = computeTopPosition(slideOffset);
            }
            if (child == switcherView) {
                top = computeTopPosition(slideOffset) + mainView.getMeasuredHeight();
            }

            int height = child.getMeasuredHeight();
            int bottom = top + height;
            int left = paddingLeft + layoutParams.leftMargin;
            int right = left + child.getMeasuredWidth();

            child.layout(left, top, right, bottom);
        }
    }

    private int computeTopPosition(float slideOffset) {
        int slidePixelOffset = (int) (slideOffset * slideRange);
        return (int) (getPaddingTop() - mainView.getMeasuredHeight() + coverHeight + slidePixelOffset);
    }

    private float computeSlideOffset(int topPosition) {
        return (topPosition - computeTopPosition(0f)) / slideRange;
    }

    @Override
    public void computeScroll() {
        if (dragHelper != null && dragHelper.continueSettling(true)) {
            if (!isEnabled()) {
                dragHelper.abort();
                return;
            }
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (!isEnabled() || action == MotionEvent.ACTION_CANCEL) {
            return super.onInterceptTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_DOWN) {
            interceptX = event.getRawX();
            interceptY = event.getRawY();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!keyBoardShowing && omnibox.getVisibility() == VISIBLE && shouldCollapsed()) {
                float deltaY = event.getRawY() - interceptY;
                if (deltaY <= -ViewUnit.dp2px(getContext(), 32)) {
                    collapsed();
                    return true;
                }
            }
        }

        if (shouldExpanded(event)) {
            expanded();
            return true;
        }

        return super.onInterceptTouchEvent(event);
    }

    private boolean shouldCollapsed() {
        int[] location = new int[2];
        omnibox.getLocationOnScreen(location);

        int left = location[0];
        int right = left + omnibox.getWidth();
        int top = location[1];
        int bottom = top + omnibox.getHeight();

        return status == Status.EXPANDED
                && left <= interceptX
                && interceptX <= right
                && top <= interceptY
                && interceptY <= bottom;
    }

    private boolean shouldExpanded(@NonNull MotionEvent event) {
        int[] location = new int[2];
        mainView.getLocationOnScreen(location);

        int left = location[0];
        int right = left + mainView.getWidth();
        int top = location[1];
        int bottom = top + mainView.getHeight();

        return status == Status.COLLAPSED
                && left <= event.getRawX()
                && event.getRawX() <= right
                && top <= event.getRawY()
                && event.getRawY() <= bottom;
    }

    public void expanded() {
        try {
            smoothSlideTo(1f);
            status = Status.EXPANDED;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void collapsed() {
        try {
            switcherView.setEnabled(true);
            smoothSlideTo(0f);
            status = Status.COLLAPSED;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fling(int top) {
        status = Status.FLING;
        slideOffset = computeSlideOffset(top);

        LayoutParams layoutParams = (LayoutParams) switcherView.getLayoutParams();
        int defaultHeight = (int) (getHeight() - getPaddingBottom() - getPaddingTop() - coverHeight);

        if (slideOffset <= 0) {
            layoutParams.height = getHeight() - getPaddingBottom() - mainView.getMeasuredHeight() - top;
        } else if (layoutParams.height != defaultHeight) {
            layoutParams.height = defaultHeight;
        }

        // Very important for switcherView works good.
        switcherView.requestLayout();
    }

    private void dispatchOnCollapsed() {
        if (statusListener != null) {
            statusListener.onCollapsed();
        }
    }

    private void smoothSlideTo(float slideOffset) {
        if (!isEnabled()) {
            return;
        }

        int top = computeTopPosition(slideOffset);
        if (dragHelper.smoothSlideViewTo(mainView, mainView.getLeft(), top)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
}

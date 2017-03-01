package com.celerysoft;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.
 * </p>
 * <p>
 * <b>What's new about AdvancedSwipeRefreshLayout?</b>
 * </p>
 */
public class AdvancedSwipeRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
    private static final String LOG_TAG = AdvancedSwipeRefreshLayout.class.getSimpleName();
    // Maps to ProgressBar.Large style
    public static final int LARGE = MaterialProgressDrawable.LARGE;
    // Maps to ProgressBar default style
    public static final int DEFAULT = MaterialProgressDrawable.DEFAULT;

    @VisibleForTesting
    static final int CIRCLE_DIAMETER = 40;
    @VisibleForTesting
    static final int CIRCLE_DIAMETER_LARGE = 56;

    static final int FOOTER_HEIGHT = 56;
    static final int HEADER_HEIGHT = 56;


    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    private float mHeaderDragRate = DRAG_RATE;
    private float mFooterDragRate = 1f;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    /**
     * two style of pull to refresh
     **/
    private boolean mHeaderScrollTogether = false;

    private View mTarget; // the target of the gesture

    /**
     * indicate if the {@link #mTarget} could pull-to-refresh
     **/
    private boolean mCouldPullToRefresh = false;
    /**
     * listener of pull-to-refresh
     **/
    private OnPullToRefreshListener mOnPullToRefreshListener;

    /**
     * indicate if the {@link #mTarget} could push-to-load-more
     **/
    private boolean mCouldPushToLoadMore = false;
    /**
     * listener of push-to-load-more
     **/
    private OnPushToLoadMoreListener mOnPushToLoadMoreListener;

    OnRefreshListener mListener;
    boolean mRefreshing = false;
    private int mTouchSlop;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mTotalUnconsumed;
    private float mTotalFooterUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    private int mMediumAnimationDuration;

    private float mInitialDownX;
    private float mInitialMotionY;
    private float mInitialDownY;
    private boolean mIsBeingDragged;
    private int mActivePointerId = INVALID_POINTER;
    // Whether this item is scaled up rather than clipped
    boolean mScale;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.enabled
    };

    private boolean mUseDefaultHeaderView = true;

    private HeaderViewContainer mHeaderViewContainer;
    private int mHeaderViewContainerIndex = -1;
    CircleImageView mCircleView;

    private int mHeaderViewContainerWidth;
    private int mHeaderViewContainerHeight;

    private RelativeLayout mFooterViewContainer;
    private int mFooterViewContainerIndex = -1;
    private ProgressBar mFooterProgressBar;
    /**
     * 上拉加载更多时，上拉的距离px
     **/
    private int mPushDistance = 0;

    private boolean mLoadingMore;

    private int mFooterViewContainerWidth;
    private int mFooterViewContainerHeight;

    protected int mFrom;

    float mStartingScale;

    /**
     * always equal with the height of {@link #mHeaderViewContainer}
     **/
    protected int mOriginalOffsetTop;

    /**
     * 下拉刷新的阈值，下拉超过此值再释放，则触发刷新
     **/
    private float mTotalDragDistance = -1;
    /**
     * {@link #mHeaderViewContainer} 当前距离顶部的偏移量px
     **/
    private int mCurrentTargetOffsetTop;
    private boolean mOriginalOffsetCalculated = false;
    /**
     * 最后停顿时的偏移量px
     **/
    int mSpinnerOffsetEnd;

    MaterialProgressDrawable mProgress;

    private Animation mScaleAnimation;

    private Animation mScaleDownAnimation;

    private Animation mAlphaStartAnimation;

    private Animation mAlphaMaxAnimation;

    private Animation mScaleDownToStartAnimation;

    boolean mNotify;

    private int mCircleDiameter;

    // Whether the client has set a custom starting position;
    boolean mUsingCustomStart;

    private OnChildScrollUpCallback mChildScrollUpCallback;

    /**
     * 下拉时，超过距离之后，弹回来的动画监听器
     */
    private Animation.AnimationListener mRefreshListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mRefreshing) {
                // Make sure the progress view is fully visible
                mProgress.setAlpha(MAX_ALPHA);
                mProgress.start();
                if (mNotify) {
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                    if (mOnPullToRefreshListener != null) {
                        mOnPullToRefreshListener.onRefresh();
                    }
                }
//                mCurrentTargetOffsetTop = mHeaderViewContainer.getTop();
            } else {
                resetHeader();
                if (mScale) {
                    setAnimationProgress(0);
                } else {
                    setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop, true);
                }
            }

            mCurrentTargetOffsetTop = mHeaderViewContainer.getTop();
//            notifyPullDistanceChanged();
        }
    };

    void resetHeader() {
        mHeaderViewContainer.clearAnimation();
        mProgress.stop();
        mHeaderViewContainer.setVisibility(View.GONE);
        setColorViewAlpha(MAX_ALPHA);
        // Return the circle to its start position
        if (mScale) {
            setAnimationProgress(0 /* animation complete and view is hidden */);
        } else {
            setTargetOffsetTopAndBottom(mOriginalOffsetTop - mCurrentTargetOffsetTop,
                    true /* requires update */);
        }
        mCurrentTargetOffsetTop = mHeaderViewContainer.getTop();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            resetHeader();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        resetHeader();
    }

    @SuppressLint("NewApi")
    private void setColorViewAlpha(int targetAlpha) {
//        mHeaderViewContainer.getBackground().setAlpha(targetAlpha);
        mCircleView.getBackground().setAlpha(targetAlpha);
        mProgress.setAlpha(targetAlpha);
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     * <p>
     * <strong>Note:</strong> Calling this will reset the position of the refresh indicator to
     * <code>start</code>.
     * </p>
     *
     * @param scale Set to true if there is no view at a higher z-order than where the progress
     *              spinner is set to appear. Setting it to true will cause indicator to be scaled
     *              up rather than clipped.
     * @param start The offset in pixels from the top of this view at which the
     *              progress spinner should appear.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mScale = scale;
        mOriginalOffsetTop = start;
        mSpinnerOffsetEnd = end;
        mUsingCustomStart = true;
        resetHeader();
        mRefreshing = false;
    }

    /**
     * @return The offset in pixels from the top of this view at which the progress spinner should
     * appear.
     */
    public int getProgressViewStartOffset() {
        return mOriginalOffsetTop;
    }

    /**
     * @return The offset in pixels from the top of this view at which the progress spinner should
     * come to rest after a successful swipe gesture.
     */
    public int getProgressViewEndOffset() {
        return mSpinnerOffsetEnd;
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than where the progress
     *              spinner is set to appear. Setting it to true will cause indicator to be scaled
     *              up rather than clipped.
     * @param end   The offset in pixels from the top of this view at which the
     *              progress spinner should come to rest after a successful swipe
     *              gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerOffsetEnd = end;
        mScale = scale;
        mCircleView.invalidate();
    }

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (!mUseDefaultHeaderView) {
            return;
        }

        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == MaterialProgressDrawable.LARGE) {
            mCircleDiameter = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        mHeaderViewContainerHeight = (int) (mCircleDiameter + 8 * metrics.density);
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mCircleView.setImageDrawable(null);
        mProgress.updateSizes(size);
        mCircleView.setImageDrawable(mProgress);
    }

    public void setHeaderView(View headerView) {
        setHeaderView(headerView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setHeaderView(View headerView, ViewGroup.LayoutParams layoutParams) {
        if (headerView == null || layoutParams == null) {
            return;
        }
        if (mHeaderViewContainer == null) {
            return;
        }

        if (layoutParams.height <= 0) {
            final DisplayMetrics metrics = getResources().getDisplayMetrics();
            mHeaderViewContainerHeight = (int) (HEADER_HEIGHT * metrics.density);
        } else {
            mHeaderViewContainerHeight = layoutParams.height;
        }

        mTotalDragDistance = mSpinnerOffsetEnd = -1;

        mHeaderDragRate = 1.0f;
        mUseDefaultHeaderView = false;
        mHeaderViewContainer.removeAllViews();

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mHeaderViewContainerWidth, mHeaderViewContainerHeight);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mHeaderViewContainer.addView(headerView, layoutParams);
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public AdvancedSwipeRefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public AdvancedSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleDiameter = (int) (CIRCLE_DIAMETER * metrics.density);
        mHeaderViewContainerHeight = (int) (mCircleDiameter + 8 * metrics.density);

        mFooterViewContainerHeight = (int) (FOOTER_HEIGHT * metrics.density);

        createHeaderViewContainer();
        createFooterViewContainer();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mTotalDragDistance = mSpinnerOffsetEnd = (int) (DEFAULT_CIRCLE_TARGET * metrics.density);

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        mOriginalOffsetTop = mCurrentTargetOffsetTop = -mHeaderViewContainerHeight;
        moveToStart(1.0f);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mHeaderViewContainerIndex < 0 && mFooterViewContainerIndex < 0) {
            return i;
        }
        if (i == childCount - 2) {
            return mHeaderViewContainerIndex;
        }
        if (i == childCount - 1) {
            return mFooterViewContainerIndex;
        }
        int bigIndex = mFooterViewContainerIndex > mHeaderViewContainerIndex ? mFooterViewContainerIndex : mHeaderViewContainerIndex;
        int smallIndex = mFooterViewContainerIndex < mHeaderViewContainerIndex ? mFooterViewContainerIndex : mHeaderViewContainerIndex;
        if (i >= smallIndex && i < bigIndex - 1) {
            return i + 1;
        }
        if (i >= bigIndex || (i == bigIndex - 1)) {
            return i + 2;
        }
        return i;

//        if (mHeaderViewContainerIndex < 0) {
//            return i;
//        } else if (i == childCount - 1) {
//            // Draw the selected child last
//            return mHeaderViewContainerIndex;
//        } else if (i >= mHeaderViewContainerIndex) {
//            // Move the children after the selected child earlier one
//            return i + 1;
//        } else {
//            // Keep the children before the selected child the same
//            return i;
//        }
    }

    private void createHeaderViewContainer() {
        mHeaderViewContainer = new HeaderViewContainer(getContext());
        mHeaderViewContainer.setBackgroundColor(0x00FFFFFF);

        createProgressView();

        addView(mHeaderViewContainer);
    }

    private void createProgressView() {
        if (mHeaderViewContainer == null) {
            return;
        }

        mCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT);
        mProgress = new MaterialProgressDrawable(getContext(), this);
        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mCircleView.setImageDrawable(mProgress);
        mCircleView.setVisibility(View.VISIBLE);


        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mHeaderViewContainer.addView(mCircleView, layoutParams);
    }

    private void createFooterViewContainer() {
        mFooterViewContainer = new RelativeLayout(getContext());
        mFooterViewContainer.setBackgroundColor(0x00FFFFFF);
        mFooterViewContainer.setVisibility(View.GONE);

        createProgressBar();

        addView(mFooterViewContainer);
    }

    private void createProgressBar() {
        if (mFooterViewContainer == null) {
            return;
        }

        mFooterProgressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyle);
        mFooterProgressBar.setIndeterminate(true);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mFooterViewContainer.addView(mFooterProgressBar, layoutParams);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe gesture.
     */
    public void setOnPullToRefreshListener(OnPullToRefreshListener listener) {
        mCouldPullToRefresh = true;
        mOnPullToRefreshListener = listener;
    }

    /**
     * Set the listener to be notified when a load-more is triggered via the swipe gesture.
     */
    public void setOnPushToLoadMoreListener(OnPushToLoadMoreListener listener) {
        mCouldPushToLoadMore = true;
        mOnPushToLoadMoreListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    public void setLoadingMore(boolean loadingMore) {
        if (!loadingMore && mLoadingMore) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                mLoadingMore = false;
                mPushDistance = 0;
                setFooterOffsetTopAndBottom(-mPushDistance, true);
            } else {
                animatorFooterToStartPosition();
//                animatorFooterToBottom(mFooterViewContainerHeight, 0);
            }
        }
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setRefreshing(boolean refreshing) {
        if (refreshing && mRefreshing != refreshing) {
            // scale and show
            mRefreshing = refreshing;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = mSpinnerOffsetEnd + mOriginalOffsetTop;
            } else {
                endTarget = mSpinnerOffsetEnd;
            }
            setTargetOffsetTopAndBottom(endTarget - mCurrentTargetOffsetTop,
                    true /* requires update */);
            mNotify = false;
            startScaleUpAnimation(mRefreshListener);
        } else {
            setRefreshing(refreshing, false /* notify */);
        }
    }

    @SuppressLint("NewApi")
    private void startScaleUpAnimation(Animation.AnimationListener listener) {
        mHeaderViewContainer.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mProgress.setAlpha(MAX_ALPHA);
        }
        mScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mHeaderViewContainer.setAnimationListener(listener);
            mCircleView.setAnimationListener(listener);
        }
        mHeaderViewContainer.clearAnimation();
        mHeaderViewContainer.startAnimation(mScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     *
     * @param progress
     */
    void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
            setColorViewAlpha((int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(mCircleView, progress);
            ViewCompat.setScaleY(mCircleView, progress);
        }
    }

    private void setRefreshing(boolean refreshing, final boolean notify) {
        if (mRefreshing != refreshing) {
            mNotify = notify;
            ensureTarget();
            mRefreshing = refreshing;
            if (mRefreshing) {
                animateOffsetToCorrectPosition(mCurrentTargetOffsetTop, mRefreshListener);
            } else {
                // Only default Circle header view and scroll type use scale down animation
                if (mUseDefaultHeaderView && !mHeaderScrollTogether) {
                    startScaleDownAnimation(mRefreshListener);
                } else {
                    animateOffsetToStartPosition(mCurrentTargetOffsetTop, mRefreshListener);
                }
            }
        }
    }

    void startScaleDownAnimation(Animation.AnimationListener listener) {
        mScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mCircleView.setAnimationListener(listener);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownAnimation);
    }

    @SuppressLint("NewApi")
    private void startProgressAlphaStartAnimation() {
        mAlphaStartAnimation = startAlphaAnimation(mProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
    }

    @SuppressLint("NewApi")
    private void startProgressAlphaMaxAnimation() {
        mAlphaMaxAnimation = startAlphaAnimation(mProgress.getAlpha(), MAX_ALPHA);
    }

    @SuppressLint("NewApi")
    private Animation startAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mScale && isAlphaUsedForScale()) {
            return null;
        }
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                mProgress.setAlpha(
                        (int) (startingAlpha + ((endingAlpha - startingAlpha) * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mCircleView.setAnimationListener(null);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(alpha);
        return alpha;
    }

    /**
     * Set the background color of the header view container.
     */
    public void setHeaderViewContainerBackgroundColor(@ColorInt int color) {
        mHeaderViewContainer.setBackgroundColor(color);
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setProgressBackgroundColorSchemeResource(@ColorRes int colorRes) {
        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(getContext(), colorRes));
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    public void setProgressBackgroundColorSchemeColor(@ColorInt int color) {
        mCircleView.setBackgroundColor(color);
        mProgress.setBackgroundColor(color);
    }

    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    public void setColorSchemeResources(@ColorRes int... colorResIds) {
        final Context context = getContext();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++) {
            colorRes[i] = ContextCompat.getColor(context, colorResIds[i]);
        }
        setColorSchemeColors(colorRes);
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors colors
     */
    public void setColorSchemeColors(@ColorInt int... colors) {
        ensureTarget();
        mProgress.setColorSchemeColors(colors);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     * progress.
     */
    public boolean isRefreshing() {
        return mRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mHeaderViewContainer) && !child.equals(mFooterViewContainer)) {
                    mTarget = child;
                    break;
                }
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mTotalDragDistance = distance;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }

        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();

        int headViewWidth = mHeaderViewContainer.getMeasuredWidth();
        int headViewHeight = mHeaderViewContainer.getMeasuredHeight();
        int distanceFromTop = mHeaderScrollTogether ? mCurrentTargetOffsetTop + headViewHeight : 0;
        mHeaderViewContainer.layout(
                (width - headViewWidth) / 2,
                mCurrentTargetOffsetTop + getPaddingTop(),
                (width + headViewWidth) / 2,
                mCurrentTargetOffsetTop + getPaddingTop() + headViewHeight
        );

        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop() + distanceFromTop - mPushDistance;// 根据偏移量distance更新
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);// 更新目标View的位置

        int footViewWidth = mFooterViewContainer.getMeasuredWidth();
        int footViewHeight = mFooterViewContainer.getMeasuredHeight();
        mFooterViewContainer.layout(
                (width - footViewWidth) / 2,
                height - mPushDistance,
                (width + footViewWidth) / 2,
                height - mPushDistance + footViewHeight
        );
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleDiameter, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleDiameter, MeasureSpec.EXACTLY));

        if (!mUseDefaultHeaderView) {
            if (mHeaderViewContainer.getChildAt(0) != null) {
                mHeaderViewContainer.getChildAt(0).measure(
                        MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(mHeaderViewContainerHeight, MeasureSpec.AT_MOST)
                );
                mHeaderViewContainerHeight = mHeaderViewContainer.getChildAt(0).getMeasuredHeight();
            }
        }

        if (mTotalDragDistance == -1) {
            mTotalDragDistance = mHeaderViewContainerHeight;
        }
        if (mSpinnerOffsetEnd == -1) {
            mSpinnerOffsetEnd = mHeaderViewContainerHeight;
        }

        mHeaderViewContainer.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mHeaderViewContainerHeight, MeasureSpec.EXACTLY));
        mFooterViewContainer.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mFooterViewContainerHeight, MeasureSpec.EXACTLY));

        if (!mUsingCustomStart && !mOriginalOffsetCalculated) {
            mOriginalOffsetCalculated = true;
            mCurrentTargetOffsetTop = mOriginalOffsetTop = -mHeaderViewContainer.getMeasuredHeight();
        }

        mHeaderViewContainerIndex = -1;
        mFooterViewContainerIndex = -1;
        // Get the index of the HeaderView and FooterView.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mHeaderViewContainer) {
                mHeaderViewContainerIndex = index;
            } else if (getChildAt(index) == mFooterViewContainer) {
                mFooterViewContainerIndex = index;
            }
        }
    }

    private void notifyPullDistanceChanged() {
        int distance = mCurrentTargetOffsetTop + mHeaderViewContainer.getHeight();
        if (mOnPullToRefreshListener != null) {
            mOnPullToRefreshListener.onPullDistance(distance, (int) mTotalDragDistance);
        }
    }

    public void setHeaderScrollTogether(boolean flag) {
        mHeaderScrollTogether = flag;
    }

    /**
     * Get the diameter of the progress circle that is displayed as part of the
     * swipe to refresh layout.
     *
     * @return Diameter in pixels of the progress circle view.
     */
    public int getProgressCircleDiameter() {
        return mCircleDiameter;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (mChildScrollUpCallback != null) {
            return mChildScrollUpCallback.canChildScrollUp(this, mTarget);
        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     * scroll down. Override this if the child view is a custom view.
     */
    public boolean canChildScrollDown() {
//        if (mChildScrollUpCallback != null) {
//            return mChildScrollUpCallback.canChildScrollUp(this, mTarget);
//        }
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                int count = absListView.getAdapter().getCount();
                int firstPos = absListView.getFirstVisiblePosition();
                if (firstPos == 0 && absListView.getChildAt(0).getTop() >= absListView.getPaddingTop()) {
                    return true;
                }
                int lastPos = absListView.getLastVisiblePosition();
                if (lastPos > 0 && count > 0 && lastPos == count - 1) {
                    return false;
                }
                return true;
            } else {
                return ViewCompat.canScrollVertically(mTarget, 1);
            }


//            if (mTarget instanceof AbsListView) {
//                final AbsListView absListView = (AbsListView) mTarget;
//                return absListView.getChildCount() > 0
//                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
//                        .getTop() < absListView.getPaddingTop());
//            } else {
//                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
//            }

        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }

//        if (mTarget instanceof AbsListView) {
//            final AbsListView absListView = (AbsListView) mTarget;
//            int count = absListView.getAdapter().getCount();
//            int firstPos = absListView.getFirstVisiblePosition();
//            if (firstPos == 0 && absListView.getChildAt(0).getTop() >= absListView.getPaddingTop()) {
//                return true;
//            }
//            int lastPos = absListView.getLastVisiblePosition();
//            if (lastPos > 0 && count > 0 && lastPos == count - 1) {
//                return false;
//            }
//            return true;
//        } else if (mTarget instanceof ScrollView) {
//            ScrollView scrollView = (ScrollView) mTarget;
//            View view = (View) scrollView.getChildAt(scrollView.getChildCount() - 1);
//            if (view != null) {
//                int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));
//                if (diff == 0) {
//                    return false;
//                }
//            }
//        } else if (mTarget instanceof NestedScrollView) {
//            NestedScrollView nestedScrollView = (NestedScrollView) mTarget;
//            View view = (View) nestedScrollView.getChildAt(nestedScrollView.getChildCount() - 1);
//            if (view != null) {
//                int diff = (view.getBottom() - (nestedScrollView.getHeight() + nestedScrollView.getScrollY()));
//                if (diff == 0) {
//                    return false;
//                }
//            }
//        }
//        return true;
    }

    /**
     * Set a callback to override {@link #canChildScrollUp()} method. Non-null
     * callback will return the value provided by the callback and ignore all internal logic.
     *
     * @param callback Callback that should be called when canChildScrollUp() is called.
     */
    public void setOnChildScrollUpCallback(@Nullable OnChildScrollUpCallback callback) {
        mChildScrollUpCallback = callback;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mHeaderScrollTogether && mRefreshing) {
            return true;
        }

        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);
        int pointerIndex;

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled()
                || mReturningToStart
                || (canChildScrollUp() && canChildScrollDown())
                || mLoadingMore
                || mRefreshing
                || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setTargetOffsetTopAndBottom(mOriginalOffsetTop - mHeaderViewContainer.getTop(), true);
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitialDownX = ev.getX(pointerIndex);
                mInitialDownY = ev.getY(pointerIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                float yDiff = y - mInitialDownY;

                // If the target can scroll, scroll the target first
                if (yDiff > 0 && canChildScrollUp()
                        || yDiff < 0 && canChildScrollDown()) {
                    return false;
                }

                // Prevent conflict from ViewPager
                final float x = ev.getX();
                float xDiff = Math.abs(x - mInitialDownX);
                if (xDiff > mTouchSlop && xDiff > Math.abs(yDiff)) {
                    return false;
                }

                startDragging(y);
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTarget instanceof AbsListView)
                || (mTarget != null && !ViewCompat.isNestedScrollingEnabled(mTarget))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }

    // NestedScrollingParent

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return isEnabled() && !mReturningToStart && !mRefreshing && !mLoadingMore
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mTotalUnconsumed = 0;
        mTotalFooterUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;
        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mTotalUnconsumed > 0) {
            finishSpinner(mTotalUnconsumed);
            mTotalUnconsumed = 0;
        }
        if (mTotalFooterUnconsumed > 0) {
            finishFooterSpinner(mTotalFooterUnconsumed);
            mTotalFooterUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.w(LOG_TAG, "onNestedPreScroll, dy = " + dy);

        // Handle header nested pre-scroll
        handleHeaderNestedPreScroll(target, dx, dy, consumed);

        // Handle footer nested pre-scroll
        if (mCouldPushToLoadMore) {
            handleFooterNestedPreScroll(target, dx, dy, consumed);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    public void handleHeaderNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mTotalUnconsumed > 0) {
            if (dy > mTotalUnconsumed) {
                consumed[1] = dy - (int) mTotalUnconsumed;
                mTotalUnconsumed = 0;
            } else {
                mTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            moveSpinner(mTotalUnconsumed);
        }

        // If a client layout is using a custom start position for the circle
        // view, they mean to hide it again before scrolling the child view
        // If we get back to mTotalUnconsumed == 0 and there is more to go, hide
        // the circle so it isn't exposed if its blocking content is moved
        if (mUsingCustomStart && dy > 0 && mTotalUnconsumed == 0
                && Math.abs(dy - consumed[1]) > 0) {
            mCircleView.setVisibility(View.GONE);
        }
    }

    private void handleFooterNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // no op
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        Log.w(LOG_TAG, "onNestedScroll, dyUnconsumed = " + dyUnconsumed);
        Log.w(LOG_TAG, "onNestedScroll, dy = " + dy);

        // Handle header nested scroll
        handleHeaderNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);

        // Handle footer nested scroll
        if (mCouldPushToLoadMore) {
            handleFooterNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        }
    }

    private void handleHeaderNestedScroll(final View target, final int dxConsumed,
                                          final int dyConsumed,
                                          final int dxUnconsumed,
                                          final int dyUnconsumed) {

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy < 0 && !canChildScrollUp()) {
            mTotalUnconsumed += Math.abs(dy);
            moveSpinner(mTotalUnconsumed);
        }
    }

    private void handleFooterNestedScroll(final View target, final int dxConsumed,
                                          final int dyConsumed,
                                          final int dxUnconsumed,
                                          final int dyUnconsumed) {
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];
        if (dy > 0 && !canChildScrollDown()) {
            mTotalFooterUnconsumed += dy;
            mTotalFooterUnconsumed = mTotalFooterUnconsumed > mFooterViewContainerHeight ? mFooterViewContainerHeight : mTotalFooterUnconsumed;
            moveFooterSpinner(mTotalFooterUnconsumed);
        }
    }

    // NestedScrollingChild

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    private void moveSpinner(float overScrollTop) {
        mProgress.showArrow(true);
        float originalDragPercent = overScrollTop / mTotalDragDistance;

        float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
        float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
        float extraOS = Math.abs(overScrollTop) - mTotalDragDistance;
        float slingshotDist = mUsingCustomStart ? mSpinnerOffsetEnd - mOriginalOffsetTop
                : mSpinnerOffsetEnd;
        float tensionSlingshotPercent = Math.max(0, Math.min(extraOS, slingshotDist * 2)
                / slingshotDist);
        float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                (tensionSlingshotPercent / 4), 2)) * 2f;
        float extraMove = (slingshotDist) * tensionPercent * 2;

        int targetY = mOriginalOffsetTop + (int) ((slingshotDist * dragPercent) + extraMove);
        // where 1.0f is a full circle
        if (mHeaderViewContainer.getVisibility() != View.VISIBLE) {
            mHeaderViewContainer.setVisibility(View.VISIBLE);
        }
        if (!mScale) {
            ViewCompat.setScaleX(mCircleView, 1f);
            ViewCompat.setScaleY(mCircleView, 1f);
        }

        if (mScale) {
            setAnimationProgress(Math.min(1f, overScrollTop / mTotalDragDistance));
        }
        if (overScrollTop < mTotalDragDistance) {
            if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                    && !isAnimationRunning(mAlphaStartAnimation)) {
                // Animate the alpha
                startProgressAlphaStartAnimation();
            }
        } else {
            if (mProgress.getAlpha() < MAX_ALPHA && !isAnimationRunning(mAlphaMaxAnimation)) {
                // Animate the alpha
                startProgressAlphaMaxAnimation();
            }
        }
        float strokeStart = adjustedPercent * .8f;
        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
        mProgress.setArrowScale(Math.min(1f, adjustedPercent));

        float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
        mProgress.setProgressRotation(rotation);

        if (overScrollTop < mTotalDragDistance) {
            if (mScale) {
                setAnimationProgress(overScrollTop / mTotalDragDistance);
            }
            if (mOnPullToRefreshListener != null) {
                mOnPullToRefreshListener.onPullEnable(false);
            }
        } else {
            if (mOnPullToRefreshListener != null) {
                mOnPullToRefreshListener.onPullEnable(true);
            }
        }

        setTargetOffsetTopAndBottom(targetY - mCurrentTargetOffsetTop, true /* requires update */);
    }

    private void finishSpinner(float overscrollTop) {
        if (overscrollTop > mTotalDragDistance) {
            setRefreshing(true, true /* notify */);
        } else {
            // cancel refresh
            mRefreshing = false;
            mProgress.setStartEndTrim(0f, 0f);
            Animation.AnimationListener listener = null;
            if (!mScale) {
                listener = new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (!mScale) {
                            startScaleDownAnimation(null);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                };
            }
            animateOffsetToStartPosition(mCurrentTargetOffsetTop, listener);
            mProgress.showArrow(false);
        }
    }

    private void moveFooterSpinner(float overScrollBottom) {
        Log.w(LOG_TAG, "moveFooterSpinner, overScrollBottom = " + overScrollBottom);
        overScrollBottom = overScrollBottom > mFooterViewContainerHeight ? mFooterViewContainerHeight : overScrollBottom;
        mPushDistance = (int) overScrollBottom;

        if (mFooterViewContainer.getVisibility() != VISIBLE) {
            mFooterViewContainer.setVisibility(View.VISIBLE);
        }

        if (mOnPushToLoadMoreListener != null) {
            mOnPushToLoadMoreListener.onPushEnable(mPushDistance >= mFooterViewContainerHeight);
        }

        setFooterOffsetTopAndBottom(-mPushDistance, true);
    }

    private void finishFooterSpinner(float overScrollBottom) {
        overScrollBottom = overScrollBottom > mFooterViewContainerHeight ? mFooterViewContainerHeight : overScrollBottom;
        if (overScrollBottom > 0 && mCouldPushToLoadMore) {
            mPushDistance = mFooterViewContainerHeight;
            mLoadingMore = true;
            if (mOnPushToLoadMoreListener != null) {
                mOnPushToLoadMoreListener.onLoadMore();
            }
        } else {
            mPushDistance = 0;
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            setFooterOffsetTopAndBottom(-mPushDistance, true);
        } else {
            animatorFooterToCorrectPosition();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        Log.d(LOG_TAG, "onTouchEvent");
        if (mHeaderScrollTogether && mRefreshing) {
            return true;
        }

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        if (!isEnabled()
                || mReturningToStart
                || (canChildScrollUp() && canChildScrollDown())
                || mLoadingMore
                || mRefreshing
                || mNestedScrollInProgress) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }



//        if (!canChildScrollUp() && canChildScrollDown()) {
//            // pull to refresh
//            if (mCouldPullToRefresh) {
//                return onPullTouchEvent(ev, action);
//            } else {
//                return false;
//            }
//        } else if (canChildScrollUp() && !canChildScrollDown()) {
//            // push to load more
//            if (mCouldPushToLoadMore) {
//                return onPushTouchEvent(ev, action);
//            } else {
//                return false;
//            }
//        }

        final int pointerIndex;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                mIsBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = ev.getY(pointerIndex);
                if (mIsBeingDragged) {

                    final float overScrollTop = (y - mInitialMotionY) * mHeaderDragRate;
                    if (mCouldPullToRefresh && !canChildScrollUp() && overScrollTop > 0) {
                        moveSpinner(overScrollTop);
                        break;
                    }

                    float overScrollBottom = (mInitialMotionY - y) * mFooterDragRate;
                    if (mCouldPushToLoadMore && !canChildScrollDown() && overScrollBottom > 0) {
                        moveFooterSpinner(overScrollBottom);
                    }
                }

                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = ev.getActionIndex();
                mActivePointerId = ev.getPointerId(index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP: {
                onSecondaryPointerUp(ev);
                break;
            }

            case MotionEvent.ACTION_UP: {
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    return false;
                }

                if (mIsBeingDragged) {
                    final float y = ev.getY(pointerIndex);
                    final float overScrollTop = (y - mInitialMotionY) * mHeaderDragRate;
                    if (mCouldPullToRefresh && overScrollTop > 0) {
                        finishSpinner(overScrollTop);
                    }

                    float overScrollBottom = (mInitialMotionY - y) * DRAG_RATE;
                    if (mCouldPushToLoadMore && overScrollBottom > 0) {
                        finishFooterSpinner(overScrollBottom);
                    }

                    mIsBeingDragged = false;
                }


                mActivePointerId = INVALID_POINTER;
                return false;
            }

            case MotionEvent.ACTION_CANCEL:
                return false;
        }

        return true;
    }

//    private boolean onPullTouchEvent(MotionEvent ev, int action) {
//        final int pointerIndex;
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                mActivePointerId = ev.getPointerId(0);
//                mIsBeingDragged = false;
//                break;
//
//            case MotionEvent.ACTION_MOVE: {
//                pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
//                    return false;
//                }
//
//                final float y = ev.getY(pointerIndex);
//                if (mIsBeingDragged) {
//                    final float overScrollTop = (y - mInitialMotionY) * mHeaderDragRate;
//                    if (overScrollTop > 0) {
//                        moveSpinner(overScrollTop);
//                    } else {
//                        return false;
//                    }
//                }
//                break;
//            }
//            case MotionEventCompat.ACTION_POINTER_DOWN: {
//                final int index = ev.getActionIndex();
//                mActivePointerId = ev.getPointerId(index);
//                break;
//            }
//
//            case MotionEventCompat.ACTION_POINTER_UP: {
//                onSecondaryPointerUp(ev);
//                break;
//            }
//
//            case MotionEvent.ACTION_UP: {
//                pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
//                    return false;
//                }
//
//                if (mIsBeingDragged) {
//                    final float y = ev.getY(pointerIndex);
//                    final float overScrollTop = (y - mInitialMotionY) * mHeaderDragRate;
//                    mIsBeingDragged = false;
//                    finishSpinner(overScrollTop);
//                }
//                mActivePointerId = INVALID_POINTER;
//                return false;
//            }
//
//            case MotionEvent.ACTION_CANCEL:
//                return false;
//        }
//
//        return true;
//    }
//
//    /**
//     * 处理上拉加载更多的Touch事件
//     *
//     * @param ev
//     * @param action
//     * @return
//     */
//    private boolean onPushTouchEvent(MotionEvent ev, int action) {
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//                mActivePointerId = ev.getPointerId(0);
//                mIsBeingDragged = false;
//                break;
//            case MotionEvent.ACTION_MOVE: {
//                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
//                if (pointerIndex < 0) {
//                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
//                    return false;
//                }
//                final float y = ev.getY();
//                float overScrollBottom = (mInitialMotionY - y) * mFooterDragRate;
//                overScrollBottom = overScrollBottom > mFooterViewContainerHeight ? mFooterViewContainerHeight : overScrollBottom;
//                if (mIsBeingDragged) {
//                    mPushDistance = (int) overScrollBottom;
//                    setFooterOffsetTopAndBottom();
//                    if (mOnPushToLoadMoreListener != null) {
//                        mOnPushToLoadMoreListener.onPushEnable(mPushDistance >= mFooterViewContainerHeight);
//                    }
//                }
//                break;
//            }
//            case MotionEventCompat.ACTION_POINTER_DOWN: {
//                final int index = MotionEventCompat.getActionIndex(ev);
//                mActivePointerId = ev.getPointerId(index);
//                break;
//            }
//
//            case MotionEventCompat.ACTION_POINTER_UP:
//                onSecondaryPointerUp(ev);
//                break;
//
//            case MotionEvent.ACTION_UP: {
//                if (mActivePointerId == INVALID_POINTER) {
//                    Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
//                    return false;
//                }
//                final float y = ev.getY();
//                float overScrollBottom = (mInitialMotionY - y) * DRAG_RATE;
//                overScrollBottom = overScrollBottom > mFooterViewContainerHeight ? mFooterViewContainerHeight : overScrollBottom;
//                mIsBeingDragged = false;
//                mActivePointerId = INVALID_POINTER;
//                if (overScrollBottom < mFooterViewContainerHeight || mOnPushToLoadMoreListener == null) {// 直接取消
//                    mPushDistance = 0;
//                } else {// 下拉到mFooterViewHeight
//                    mPushDistance = mFooterViewContainerHeight;
//                }
//                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
//                    setFooterOffsetTopAndBottom();
//                    if (mPushDistance == mFooterViewContainerHeight) {
//                        mLoadingMore = true;
//
//                        if (mOnPushToLoadMoreListener != null) {
//                            mOnPushToLoadMoreListener.onLoadMore();
//                        }
//                    }
//                } else {
//                    animatorFooterToCorrectPosition();
//                }
//
//                return false;
//            }
//            case MotionEvent.ACTION_CANCEL:
//                return false;
//        }
//        return true;
//    }


    /**
     * 修改底部布局的位置，敏感pushDistance
     */
    private void setFooterOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mFooterViewContainer.bringToFront();

        ViewCompat.offsetTopAndBottom(mFooterViewContainer, offset);
//        if (mTarget != null) {
//            ViewCompat.offsetTopAndBottom(mTarget, offset);
//        }

        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            requestLayout();
        }

        notifyPushDistanceChanged();
    }

    private void notifyPushDistanceChanged() {
        if (mOnPushToLoadMoreListener != null) {
            mOnPushToLoadMoreListener.onPushDistance(mPushDistance);
        }
    }

    @SuppressLint("NewApi")
    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if (Math.abs(yDiff) > mTouchSlop && !mIsBeingDragged) {
            if (yDiff > 0 && !canChildScrollUp()) {
                mInitialMotionY = mInitialDownY + mTouchSlop;
                mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
            } else if (yDiff < 0 && !canChildScrollDown()) {
                mInitialMotionY = mInitialDownY - mTouchSlop;
            }

            mIsBeingDragged = true;
        }
    }

    private void animateOffsetToCorrectPosition(int from, Animation.AnimationListener listener) {
        mFrom = from;
        mAnimateToCorrectPosition.reset();
        mAnimateToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mHeaderViewContainer.setAnimationListener(listener);
            mCircleView.setAnimationListener(listener);
        }
        mHeaderViewContainer.clearAnimation();
        mHeaderViewContainer.startAnimation(mAnimateToCorrectPosition);
//        mCircleView.clearAnimation();
//        mCircleView.startAnimation(mAnimateToCorrectPosition);
    }

    private void animateOffsetToStartPosition(int from, Animation.AnimationListener listener) {
        if (mScale) {
            // Scale the item back down
            startScaleDownReturnToStartAnimation(from, listener);
        } else {
            mFrom = from;
            mAnimateToStartPosition.reset();
            mAnimateToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mCircleView.setAnimationListener(listener);
                mHeaderViewContainer.setAnimationListener(listener);
            }
            mHeaderViewContainer.clearAnimation();
            mHeaderViewContainer.startAnimation(mAnimateToStartPosition);
//            mCircleView.clearAnimation();
//            mCircleView.startAnimation(mAnimateToStartPosition);
        }
    }

    private final Animation mAnimateToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mUsingCustomStart) {
                endTarget = mSpinnerOffsetEnd - Math.abs(mOriginalOffsetTop);
            } else {
                endTarget = mSpinnerOffsetEnd;
            }
            targetTop = (mFrom + (int) ((endTarget - mFrom) * interpolatedTime));
            int offset = targetTop - mHeaderViewContainer.getTop();
            setTargetOffsetTopAndBottom(offset, false /* requires update */);
            mProgress.setArrowScale(1 - interpolatedTime);
        }
    };

    void moveToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mFrom + (int) ((mOriginalOffsetTop - mFrom) * interpolatedTime));
        int offset = targetTop - mHeaderViewContainer.getTop();
        setTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveToStart(interpolatedTime);
        }
    };

    @SuppressLint("NewApi")
    private void startScaleDownReturnToStartAnimation(int from,
                                                      Animation.AnimationListener listener) {
        mFrom = from;
        if (isAlphaUsedForScale()) {
            mStartingScale = mProgress.getAlpha();
        } else {
            mStartingScale = ViewCompat.getScaleX(mCircleView);
        }
        mScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mStartingScale + (-mStartingScale * interpolatedTime));
                setAnimationProgress(targetScale);
                moveToStart(interpolatedTime);
            }
        };
        mScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mScaleDownToStartAnimation);
    }

    void setTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mHeaderViewContainer.bringToFront();
        ViewCompat.offsetTopAndBottom(mHeaderViewContainer, offset);

        if (mHeaderScrollTogether && mTarget != null) {
            ViewCompat.offsetTopAndBottom(mTarget, offset);
        }

        mCurrentTargetOffsetTop = mHeaderViewContainer.getTop();

        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            requestLayout();
        }

        notifyPullDistanceChanged();
    }

    private void animatorFooterToCorrectPosition() {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(mPushDistance, mFooterViewContainerHeight);
        valueAnimator.setDuration(150);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mPushDistance = (Integer) valueAnimator.getAnimatedValue();
                setFooterOffsetTopAndBottom(-mPushDistance, false);
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mLoadingMore = true;
            }
        });
        valueAnimator.setInterpolator(mDecelerateInterpolator);
        valueAnimator.start();
    }

    private void animatorFooterToStartPosition() {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(mFooterViewContainerHeight, 0);
        valueAnimator.setDuration(150);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mPushDistance = (Integer) valueAnimator.getAnimatedValue();
                setFooterOffsetTopAndBottom(-mPushDistance, false);
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoadingMore = false;
                requestLayout();
            }
        });
        valueAnimator.setInterpolator(mDecelerateInterpolator);
        valueAnimator.start();
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        /**
         * Called when a swipe gesture triggers a refresh.
         */
        void onRefresh();
    }

    /**
     * Pull-to-refresh callback.
     */
    public interface OnPullToRefreshListener {
        void onRefresh();

        /**
         * @param distance pull distance
         * @param distanceToTriggerSync distance to trigger sync
         */
        void onPullDistance(int distance, int distanceToTriggerSync);

        /**
         * if pull distance is more than distance to triggerSync, the enable is true
         * @param enable
         */
        void onPullEnable(boolean enable);
    }

    /**
     * Push-to-load-more callback.
     */
    public interface OnPushToLoadMoreListener {
        void onLoadMore();

        void onPushDistance(int distance);

        void onPushEnable(boolean enable);
    }

    /**
     * Adapter of OnPullToRefreshListener.
     */
    public class OnPullToRefreshListenerAdapter implements OnPullToRefreshListener {

        @Override
        public void onRefresh() {

        }

        @Override
        public void onPullDistance(int distance, int distanceToTriggerSync) {

        }

        @Override
        public void onPullEnable(boolean enable) {

        }

    }

    /**
     * Adapter of OnPushToLoadMoreListener.
     */
    public class OnPushToLoadMoreListenerAdapter implements OnPushToLoadMoreListener {

        @Override
        public void onLoadMore() {

        }

        @Override
        public void onPushDistance(int distance) {

        }

        @Override
        public void onPushEnable(boolean enable) {

        }

    }

    /**
     * Classes that wish to override {@link #canChildScrollUp()} method
     * behavior should implement this interface.
     */
    public interface OnChildScrollUpCallback {
        /**
         * Callback that will be called when {@link #canChildScrollUp()} method
         * is called to allow the implementer to override its behavior.
         *
         * @param parent AdvancedSwipeRefreshLayout that this callback is overriding.
         * @param child  The child view of AdvancedSwipeRefreshLayout.
         * @return Whether it is possible for the child view of parent layout to scroll up.
         */
        boolean canChildScrollUp(AdvancedSwipeRefreshLayout parent, @Nullable View child);
    }

    /**
     * Classes that wish to override {@link #canChildScrollDown()} method
     * behavior should implement this interface.
     */
    public interface OnChildScrollDownCallback {
        /**
         * Callback that will be called when {@link #canChildScrollDown()} method
         * is called to allow the implementer to override its behavior.
         *
         * @param parent AdvancedSwipeRefreshLayout that this callback is overriding.
         * @param child  The child view of AdvancedSwipeRefreshLayout.
         * @return Whether it is possible for the child view of parent layout to scroll up.
         */
        boolean canChildScrollDown(AdvancedSwipeRefreshLayout parent, @Nullable View child);
    }

    private boolean mListenTargetFlingThreadRunning;
    private Thread mListenTargetFlingThread;
    private void startListenTargetFling() {
        mListenTargetFlingThreadRunning = true;

        if (mListenTargetFlingThread != null && mListenTargetFlingThread.isAlive()) {

        }
    }

    private void stopListenTargetFling() {
        mListenTargetFlingThreadRunning = false;
    }
}

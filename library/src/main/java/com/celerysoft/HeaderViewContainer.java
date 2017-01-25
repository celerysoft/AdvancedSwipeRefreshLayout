package com.celerysoft;

import android.content.Context;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

class HeaderViewContainer extends RelativeLayout {
    private Animation.AnimationListener mListener;

    public HeaderViewContainer(Context context) {
        super(context);
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }
}

package com.hjy.stickyview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * Created by hjy on 9/15/15.<br>
 */
public class StickyListView extends ListView implements IStickyView{

    private StickyScrollCallback mStickyScrollCallback;

    /**
     * 所在page的position，如果有多个page共同协作使用一个sticky view，则必须设置该值
     */
    private int mPagePosition;

    private OnScrollListener mDelegate;

    public StickyListView(Context context) {
        this(context, null);
    }

    public StickyListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOverScrollMode(OVER_SCROLL_ALWAYS);
        setOnScrollListener(mOnScrollListener);
    }

    @Override
    public void setPagePosition(int position) {
        mPagePosition = position;
    }

    @Override
    public void setStickyScrollCallback(StickyScrollCallback stickyScrollCallback) {
        mStickyScrollCallback = stickyScrollCallback;
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        if(l != mOnScrollListener)
            mDelegate = l;
        else {
            super.setOnScrollListener(l);
        }
    }

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if(mDelegate != null) {
                mDelegate.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if(mDelegate != null) {
                mDelegate.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }
            StickyScrollCallback callback = getStickyScrollCallback();
            if(callback == null)
                return;
            if(callback.getCurrentItem() != getPagePosition())
                return;
            if(firstVisibleItem == 0) {
                View firstView = getChildAt(0);
                if(firstView != null) {
                    int top = firstView.getTop();
                    if(top < - callback.getStickyViewTop())
                        top = -callback.getStickyViewTop();
                    if(top > 0)
                        top = 0;
                    callback.onScrollChanged(top);
                } else if(firstVisibleItem < 6) {
                    callback.onScrollChanged(-callback.getStickyViewTop());
                }
            }
        }
    };


    @Override
    public int getPagePosition() {
        return mPagePosition;
    }

    @Override
    public StickyScrollCallback getStickyScrollCallback() {
        return mStickyScrollCallback;
    }

    @Override
    public void setupHeadPlaceHolder(View view) {
        StickyScrollCallback callback = getStickyScrollCallback();
        if(callback == null)
            throw new IllegalStateException("getStickyScrollCallback() return null");
        addHeaderView(view, null, false);
        view.setMinimumHeight(callback.getHeaderViewHeight());
    }

    @Override
    public void adjustPositionByTranslationYOfStickyView(int translationY) {
        int transY = Math.abs(translationY);
        int stickyViewTop = getStickyScrollCallback().getStickyViewTop();
        int scrollHeight = getFirstViewScrollTop();

        if(transY >= stickyViewTop) {
            if(scrollHeight >= transY)
                return;
            setSelectionFromTop(0, translationY);
        } else {
            setSelectionFromTop(0, translationY);
        }
    }

    public int getFirstViewScrollTop() {
        if (getFirstVisiblePosition() == 0) {
            View firstView = getChildAt(0);
            if (null != firstView) {
                return -firstView.getTop();
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public int getScrollTop() {
        return Math.abs(getFirstViewScrollTop());
    }
}

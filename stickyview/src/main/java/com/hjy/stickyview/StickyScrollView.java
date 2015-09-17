package com.hjy.stickyview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * Created by hjy on 9/15/15.<br>
 */
public class StickyScrollView extends ScrollView implements IStickyView {

    private StickyScrollCallback mStickyScrollCallback;

    /**
     * 所在page的position，如果有多个page共同协作使用一个sticky view，则必须设置该值
     */
    private int mPagePosition;

    public StickyScrollView(Context context) {
        this(context, null);
    }

    public StickyScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setOverScrollMode(OVER_SCROLL_ALWAYS);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        StickyScrollCallback callback = getStickyScrollCallback();
        if(callback == null)
            return;
        if(callback.getCurrentItem() != getPagePosition())
            return;
        int stickyTranslate = t;
        if(t > callback.getStickyViewTop())
            stickyTranslate = callback.getStickyViewTop();
        if(t < 0)
            stickyTranslate = 0;
        callback.onScrollChanged(-stickyTranslate);
    }

    @Override
    public void setStickyScrollCallback(StickyScrollCallback stickyScrollCallback) {
        mStickyScrollCallback = stickyScrollCallback;
    }

    @Override
    public void setPagePosition(int position) {
        mPagePosition = position;
    }

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
        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) view.getLayoutParams();
        params.height = callback.getHeaderViewHeight();
        view.setLayoutParams(params);
    }

    @Override
    public void adjustPositionByTranslationYOfStickyView(int translationY) {
        int transY = Math.abs(translationY);
        int stickyViewTop = getStickyScrollCallback().getStickyViewTop();
        int scrollHeight = getScrollTop();

        if(transY >= stickyViewTop) {
            if(scrollHeight >= transY)
                return;
            scrollTo(0, transY);
        } else {
            scrollTo(0, transY);
        }
        invalidate();
    }

    @Override
    public int getScrollTop() {
        return Math.abs(getScrollY());
    }
}
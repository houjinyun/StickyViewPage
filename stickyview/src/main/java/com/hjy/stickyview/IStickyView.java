package com.hjy.stickyview;

import android.view.View;

/**
 * Created by hjy on 9/15/15.<br>
 */
public interface IStickyView {

    public void setPagePosition(int position);

    public void setStickyScrollCallback(StickyScrollCallback stickyScrollCallback);

    /**
     * 当该View在ViewPager、Tab等类似的页面里时，获取该View所在页面的position
     *
     * @return page position
     */
    public int getPagePosition();

    public StickyScrollCallback getStickyScrollCallback();

    /**
     * 必须确保getStickyScrollCallback()不会return null之后才可调用<br>
     * 如果是ListView，则必须在addHeader()和setAdapter()之前调用<br>
     * 如果是ScrollView，则该View必须是ScrollView所包裹的layout里的最前面的View
     *
     * @param view 在头部进行占位的View
     */
    public void setupHeadPlaceHolder(View view);

    /**
     * 触发sticky view滚动后，如果几个页面之间进行切换，需要根据当前sticky view的偏移值来调整自己的滚动位置
     *
     * @param translationY sticky view的translationY值，一般为负数
     */
    public void adjustPositionByTranslationYOfStickyView(int translationY);

    /**
     * 获取往上移动的距离
     *
     * @return 往上滑动的距离
     */
    public int getScrollTop();
}
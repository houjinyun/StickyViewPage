package com.hjy.stickyview;

/**
 * Created by hjy on 9/15/15.<br>
 */
public interface StickyScrollCallback {

    /**
     * 监听滑动距离变化，一般为负数
     *
     * @param translationY The vertical position of the view relative to its top position
     */
    public void onScrollChanged(int translationY);

    /**
     * 获取当前选中的页面位置，例如ViewPager、Tab页面里的页面
     *
     * @return 当前所在页面position
     */
    public int getCurrentItem();

    /**
     * 获取header view的高度，要实现效果，必须添加一个header placeholder，该header什么内容都没有，仅作用于空间占位
     *
     * @return Header View的高度
     */
    public int getHeaderViewHeight();

    /**
     * 获取需要显示的sticky view距离顶部的距离，一般滑动一段距离后，sticky view就停在顶部不动
     *
     * @return sticky view距离顶部的距离
     */
    public int getStickyViewTop();
}
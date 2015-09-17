package com.hjy.stickyviewpager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hjy.stickyview.IStickyView;
import com.hjy.stickyview.StickyScrollCallback;
import com.hjy.stickyview.StickyScrollView;

/**
 * Created by hjy on 9/16/15.<br>
 */
public class ScrollViewFragment extends Fragment {

    private View mContentView;

    private IStickyView mStickyScrollView;

    private StickyScrollCallback mStickyScrollCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_scroll_view, null);
        initViews();
        return mContentView;
    }

    private void initViews() {
        mStickyScrollView = (StickyScrollView) findViewById(R.id.StickyScrollView);

        mStickyScrollView.setPagePosition(0);
        mStickyScrollView.setStickyScrollCallback(mStickyScrollCallback);
        mStickyScrollView.setupHeadPlaceHolder(findViewById(R.id.TextView_Place_Holder));
    }

    private View findViewById(int id) {
        return mContentView.findViewById(id);
    }

    public void setStickyScrollCallback(StickyScrollCallback stickyScrollCallback) {
        mStickyScrollCallback = stickyScrollCallback;
    }

    public void adjusScrollPosition(int translationY) {
        if(mStickyScrollView != null)
            mStickyScrollView.adjustPositionByTranslationYOfStickyView(translationY);
    }

    public int getScrollTop() {
        if(mStickyScrollView != null)
            return mStickyScrollView.getScrollTop();
        return 0;
    }

}

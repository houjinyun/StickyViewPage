package com.hjy.stickyviewpager;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hjy.stickyview.IStickyView;
import com.hjy.stickyview.StickyGridView;
import com.hjy.stickyview.StickyScrollCallback;

/**
 * Created by hjy on 9/16/15.<br>
 */
public class GridViewFragment extends Fragment {

    private View mContentView;

    private IStickyView mStickyGridView;

    private StickyScrollCallback mStickyScrollCallback;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.fragment_grid_view, null);
        initViews();
        return mContentView;
    }

    private void initViews() {
        mStickyGridView = (StickyGridView) findViewById(R.id.StickyGridView);

        mStickyGridView.setPagePosition(2);
        mStickyGridView.setStickyScrollCallback(mStickyScrollCallback);
        mStickyGridView.setupHeadPlaceHolder(new TextView(getActivity()));

        ((StickyGridView)mStickyGridView).setAdapter(new MyListAdapter());
    }

    private View findViewById(int id) {
        return mContentView.findViewById(id);
    }

    public void setStickyScrollCallback(StickyScrollCallback stickyScrollCallback) {
        mStickyScrollCallback = stickyScrollCallback;
    }

    class MyListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 15;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if(view == null) {
                TextView textView = new TextView(getActivity());
                textView.setHeight(200);
                view = textView;
            }

            ((TextView) view).setText("position : " + position);
            return view;
        }
    }

    public void adjusScrollPosition(int translationY) {
        if(mStickyGridView != null)
            mStickyGridView.adjustPositionByTranslationYOfStickyView(translationY);
    }

    public int getScrollTop() {
        if(mStickyGridView != null)
            return mStickyGridView.getScrollTop();
        return 0;
    }

}
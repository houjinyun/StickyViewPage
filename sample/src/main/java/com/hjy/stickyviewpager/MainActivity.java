package com.hjy.stickyviewpager;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.LinearLayout;

import com.hjy.stickyview.StickyScrollCallback;

public class MainActivity extends FragmentActivity {

    private ViewPager mViewPager;
    private LinearLayout mLayoutHeader;

    private int mHeaderHeight;
    private int mStickyViewTop;

    private ScrollViewFragment mScrollFragment;
    private ListViewFragment mListFragment;
    private GridViewFragment mGridFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        mScrollFragment = (ScrollViewFragment) fragmentManager.findFragmentByTag(makeFragmentTag(R.id.ViewPager, 0));
        mListFragment = (ListViewFragment) fragmentManager.findFragmentByTag(makeFragmentTag(R.id.ViewPager, 1));
        mGridFragment = (GridViewFragment) fragmentManager.findFragmentByTag(makeFragmentTag(R.id.ViewPager, 2));
        if(mScrollFragment == null) {
            mScrollFragment = new ScrollViewFragment();
        }
        if(mListFragment == null) {
            mListFragment = new ListViewFragment();
        }
        if(mGridFragment == null) {
            mGridFragment = new GridViewFragment();
        }

        mViewPager = (ViewPager) findViewById(R.id.ViewPager);
        mLayoutHeader = (LinearLayout) findViewById(R.id.LinearLayout_Header);

        //获取header, sticky view等的高度
        mLayoutHeader.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mHeaderHeight = mLayoutHeader.getMeasuredHeight();
        mStickyViewTop = mLayoutHeader.getChildAt(0).getMeasuredHeight();
        if(savedInstanceState != null) {
            mLayoutHeader.setTranslationY(savedInstanceState.getInt("transY"));
        }

        mScrollFragment.setStickyScrollCallback(mCallback);
        mListFragment.setStickyScrollCallback(mCallback);
        mGridFragment.setStickyScrollCallback(mCallback);

        //tab点击切换时，需要调整位置
        MyPageAdapter adapter = new MyPageAdapter(getSupportFragmentManager());
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(mPageChangeListener);

        findViewById(R.id.Button_Tab_ScrollView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
                mViewPager.setCurrentItem(0, true);
            }
        });
        findViewById(R.id.Button_Tab_ListView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
                mViewPager.setCurrentItem(1, true);
            }
        });
        findViewById(R.id.Button_Tab_GridView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPageChangeListener.onPageScrollStateChanged(ViewPager.SCROLL_STATE_DRAGGING);
                mViewPager.setCurrentItem(2, true);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("transY", (int) mLayoutHeader.getTranslationY());
    }

    private ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            int st = 0;
            if (position == 0) {
                st = mScrollFragment.getScrollTop();
            } else if (position == 1) {
                st = mListFragment.getScrollTop();
            } else if(position == 2) {
                st = mGridFragment.getScrollTop();
            }
            int tranY = (int) Math.abs(mLayoutHeader.getTranslationY());
            if (tranY > st) {
                st = Math.min(st, mStickyViewTop);
                mLayoutHeader.setTranslationY(-st);
                if(position == 0)
                    mScrollFragment.adjusScrollPosition(-st);
                else if(position == 1)
                    mListFragment.adjusScrollPosition(-st);
                else if(position == 2)
                    mGridFragment.adjusScrollPosition(-st);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                int currItem = mViewPager.getCurrentItem();
                if (currItem == 0) {
                    int transY = (int) mLayoutHeader.getTranslationY();
                    mListFragment.adjusScrollPosition(transY);
                    mGridFragment.adjusScrollPosition(transY);
                } else if(currItem == 1) {
                    int transY = (int) mLayoutHeader.getTranslationY();
                    mScrollFragment.adjusScrollPosition(transY);
                    mGridFragment.adjusScrollPosition(transY);
                } else if(currItem == 2) {
                    int transY = (int) mLayoutHeader.getTranslationY();
                    mScrollFragment.adjusScrollPosition(transY);
                    mListFragment.adjusScrollPosition(transY);
                }
            }
        }
    };

    private StickyScrollCallback mCallback = new StickyScrollCallback() {
        @Override
        public void onScrollChanged(int translationY) {
            mLayoutHeader.setTranslationY(translationY);
        }

        @Override
        public int getCurrentItem() {
            return mViewPager.getCurrentItem();
        }

        @Override
        public int getHeaderViewHeight() {
            return mHeaderHeight;
        }

        @Override
        public int getStickyViewTop() {
            return mStickyViewTop;
        }
    };

    class MyPageAdapter extends FragmentPagerAdapter {

        public MyPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if(position == 0)
                return mScrollFragment;
            else if(position == 1)
                return mListFragment;
            else if(position == 2)
                return mGridFragment;
            return new Fragment();
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

    private static String makeFragmentTag(int viewPagerId, long id) {
        return "android:switcher:" + viewPagerId + ":" + id;
    }

}

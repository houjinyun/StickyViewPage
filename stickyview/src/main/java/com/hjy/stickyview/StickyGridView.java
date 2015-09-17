package com.hjy.stickyview;

/**
 * Created by hjy on 5/27/15.<br>
 */
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * A {@link GridView} that supports adding header rows in a
 * very similar way to
 * See {@link StickyGridView#addHeaderView(View, Object, boolean)}
 */
public class StickyGridView extends GridView implements IStickyView {
    private static final String TAG = "HeaderGridView";

    /**
     * A class that represents a fixed view in a list, for example a header at the top
     * or a footer at the bottom.
     */
    private static class FixedViewInfo {
        /**
         * The view to add to the grid
         */
        public View view;
        public ViewGroup viewContainer;
        /**
         * The data backing the view. This is returned from {@link ListAdapter#getItem(int)}.
         */
        public Object data;
        /**
         * <code>true</code> if the fixed view should be selectable in the grid
         */
        public boolean isSelectable;
    }

    private ArrayList<FixedViewInfo> mHeaderViewInfos = new ArrayList<FixedViewInfo>();
    private ArrayList<FixedViewInfo> mFooterViewInfos = new ArrayList<FixedViewInfo>();
    private ListAdapter mOriginalAdapter;

    private View mViewForMeasureRowHeight = null;
    private int mRowHeight = -1;
    private int mNumColumns = AUTO_FIT;

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;
    private ItemClickHandler mItemClickHandler;

    private StickyScrollCallback mStickyScrollCallback;

    /**
     * 所在page的position，如果有多个page共同协作使用一个sticky view，则必须设置该值
     */
    private int mPagePosition;

    private OnScrollListener mDelegate;

    public StickyGridView(Context context) {
        this(context, null);
    }

    public StickyGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initHeaderGridView();
    }

    private void initHeaderGridView() {
        super.setClipChildren(false);
        setOnScrollListener(mOnScrollListener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter instanceof HeaderViewGridAdapter) {
            ((HeaderViewGridAdapter) adapter).setNumColumns(getNumColumns());
            ((HeaderViewGridAdapter) adapter).setRowHeight(getRowHeight());
        }
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
        // Ignore, since the header rows depend on not being clipped
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p/>
     * NOTE: Call this before calling setAdapter. This is so HeaderGridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v            The view to add.
     * @param data         Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    public void addHeaderView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add header view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);
        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    public void addFooterView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        if(adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException(
                    "Cannot add footer view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);
        if(adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p/>
     * NOTE: Call this before calling setAdapter. This is so HeaderGridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v The view to add.
     */
    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    public void addFoterView(View v) {
        addFooterView(v, null, true);
    }

    public int getHeaderViewCount() {
        return mHeaderViewInfos.size();
    }

    public int getFooterViewCount() {
        return mFooterViewInfos.size();
    }
    /**
     * Removes a previously-added header view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a header
     * view
     */
    public boolean removeHeaderView(View v) {
        if (mHeaderViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if (adapter != null && ((HeaderViewGridAdapter) adapter).removeHeader(v)) {
                result = true;
            }
            removeFixedViewInfo(v, mHeaderViewInfos);
            return result;
        }
        return false;
    }

    public boolean removeFooterView(View v) {
        if(mFooterViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if(adapter != null && ((HeaderViewGridAdapter)adapter).removeFooter(v)) {
                result = true;
            }
            removeFixedViewInfo(v, mFooterViewInfos);
            return result;
        }
        return false;
    }

    private void removeFixedViewInfo(View v, ArrayList<FixedViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; ++i) {
            FixedViewInfo info = where.get(i);
            if (info.view == v) {
                where.remove(i);
                break;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mViewForMeasureRowHeight = null;
    }

    public void invalidateRowHeight() {
        mRowHeight = -1;
    }

    @TargetApi(11)
    private int getNumColumnsCompatible() {
        if (Build.VERSION.SDK_INT >= 11) {
            return super.getNumColumns();
        } else {
            try {
                Field numColumns = GridView.class.getDeclaredField("mNumColumns");
                numColumns.setAccessible(true);
                return numColumns.getInt(this);
            } catch (Exception e) {
                if (mNumColumns != -1) {
                    return mNumColumns;
                }
                throw new RuntimeException("Can not determine the mNumColumns for this API platform, please call setNumColumns to set it.");
            }
        }
    }

    private int getColumnWidthCompatible() {
        if (Build.VERSION.SDK_INT >= 16) {
            return super.getColumnWidth();
        } else {
            try {
                Field numColumns = GridView.class.getDeclaredField("mColumnWidth");
                numColumns.setAccessible(true);
                return numColumns.getInt(this);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getRowHeight() {
        if (mRowHeight > 0) {
            return mRowHeight;
        }
        ListAdapter adapter = getAdapter();
        int numColumns = getNumColumnsCompatible();

        // adapter has not been set or has no views in it;
        if (adapter == null || adapter.getCount() <= numColumns * (mHeaderViewInfos.size() + mFooterViewInfos.size())) {
            return -1;
        }
        int mColumnWidth = getColumnWidthCompatible();
        View view = adapter.getView(numColumns * mHeaderViewInfos.size(), mViewForMeasureRowHeight, this);
        LayoutParams p = (LayoutParams) view.getLayoutParams();
        if (p == null) {
            p = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0);
            view.setLayoutParams(p);
        }
        int childHeightSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
        int childWidthSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(mColumnWidth, MeasureSpec.EXACTLY), 0, p.width);
        view.measure(childWidthSpec, childHeightSpec);
        mViewForMeasureRowHeight = view;
        mRowHeight = view.getMeasuredHeight();
        return mRowHeight;
    }

    @Override
    public void setNumColumns(int numColumns) {
        super.setNumColumns(numColumns);
        mNumColumns = numColumns;
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter instanceof HeaderViewGridAdapter) {
            ((HeaderViewGridAdapter) adapter).setNumColumns(numColumns);
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        mOriginalAdapter = adapter;
        if (mHeaderViewInfos.size() > 0 || mFooterViewInfos.size() > 0) {
            HeaderViewGridAdapter hadapter = new HeaderViewGridAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
            int numColumns = getNumColumnsCompatible();
            if (numColumns > 1) {
                hadapter.setNumColumns(numColumns);
            }
            hadapter.setRowHeight(getRowHeight());
            super.setAdapter(hadapter);
        } else {
            super.setAdapter(adapter);
        }
    }

    public ListAdapter getmOriginalAdapter() {
        return mOriginalAdapter;
    }

    private class FullWidthFixedViewLayout extends FrameLayout {
        public FullWidthFixedViewLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int targetWidth = StickyGridView.this.getMeasuredWidth()
                    - StickyGridView.this.getPaddingLeft()
                    - StickyGridView.this.getPaddingRight();
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth,
                    MeasureSpec.getMode(widthMeasureSpec));
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * ListAdapter used when a HeaderGridView has header views. This ListAdapter
     * wraps another one and also keeps track of the header views and their
     * associated data objects.
     * <p>This is intended as a base class; you will probably not need to
     * use this class directly in your own code.
     */
    private static class HeaderViewGridAdapter implements WrapperListAdapter, Filterable {
        // This is used to notify the container of updates relating to number of columns
        // or headers changing, which changes the number of placeholders needed
        private final DataSetObservable mDataSetObservable = new DataSetObservable();
        private final ListAdapter mAdapter;
        private int mNumColumns = 1;
        // This ArrayList is assumed to NOT be null.
        private ArrayList<FixedViewInfo> mHeaderViewInfos;
        private ArrayList<FixedViewInfo> mFooterViewInfos;
        boolean mAreAllFixedViewsSelectable;
        private final boolean mIsFilterable;

        private int mRowHeight = -1;

        public HeaderViewGridAdapter(ArrayList<FixedViewInfo> headerViewInfos, ArrayList<FixedViewInfo> footerViewInfos,  ListAdapter adapter) {
            mAdapter = adapter;
            mIsFilterable = adapter instanceof Filterable;
            if (headerViewInfos == null) {
                throw new IllegalArgumentException("headerViewInfos cannot be null");
            }
            if (footerViewInfos == null) {
                throw new IllegalArgumentException("footerViewInfos cannot be null");
            }
            mHeaderViewInfos = headerViewInfos;
            mFooterViewInfos = footerViewInfos;
            mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos);
        }

        public int getHeadersCount() {
            return mHeaderViewInfos.size();
        }

        public int getFootersCount() {
            return mFooterViewInfos.size();
        }

        @Override
        public boolean isEmpty() {
            return (mAdapter == null || mAdapter.isEmpty()) && getHeadersCount() == 0 && getFootersCount() == 0;
        }

        public void setNumColumns(int numColumns) {
            if (numColumns < 1) {
                throw new IllegalArgumentException("Number of columns must be 1 or more");
            }
            if (mNumColumns != numColumns) {
                mNumColumns = numColumns;
                notifyDataSetChanged();
            }
        }

        public void setRowHeight(int height) {
            mRowHeight = height;
        }

        private boolean areAllListInfosSelectable(ArrayList<FixedViewInfo> infos) {
            if (infos != null) {
                for (FixedViewInfo info : infos) {
                    if (!info.isSelectable) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean removeHeader(View v) {
            for (int i = 0; i < mHeaderViewInfos.size(); i++) {
                FixedViewInfo info = mHeaderViewInfos.get(i);
                if (info.view == v) {
                    mHeaderViewInfos.remove(i);
                    mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos);
                    mDataSetObservable.notifyChanged();
                    return true;
                }
            }
            return false;
        }

        public boolean removeFooter(View v) {
            for (int i = 0; i < mFooterViewInfos.size(); i++) {
                FixedViewInfo info = mFooterViewInfos.get(i);
                if (info.view == v) {
                    mFooterViewInfos.remove(i);
                    mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos) && areAllListInfosSelectable(mFooterViewInfos);
                    mDataSetObservable.notifyChanged();
                    return true;
                }
            }
            return false;
        }

        private int getAdapterAndPlaceHolderCount() {
            return (int) (Math.ceil(1f * mAdapter.getCount() / mNumColumns) * mNumColumns);
        }

        @Override
        public int getCount() {
            if (mAdapter != null) {
                return getHeadersCount() * mNumColumns + getFootersCount() * mNumColumns + getAdapterAndPlaceHolderCount();
            } else {
                return getHeadersCount() * mNumColumns + getFootersCount() * mNumColumns;
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            if (mAdapter != null) {
                return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        @Override
        public boolean isEnabled(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                return (position % mNumColumns == 0)
                        && mHeaderViewInfos.get(position / mNumColumns).isSelectable;
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount = 0;
            int numAdapterEmptyPlaceHolder = getAdapterAndPlaceHolderCount();
            if (mAdapter != null) {
                adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.isEnabled(adjPosition);
                }
                if(adjPosition < numAdapterEmptyPlaceHolder) {
                    return false;
                }
            }

            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            final int footerAdjPosition = position - numHeadersAndPlaceholders - numAdapterEmptyPlaceHolder;
            if(footerAdjPosition < numFootersAndPlaceholders) {
                return (footerAdjPosition % mNumColumns == 0)
                        && mFooterViewInfos.get(footerAdjPosition / mNumColumns).isSelectable;
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public Object getItem(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                if (position % mNumColumns == 0) {
                    return mHeaderViewInfos.get(position / mNumColumns).data;
                }
                return null;
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount = 0;
            int numAdapterEmptyPlaceHolder = getAdapterAndPlaceHolderCount();
            if (mAdapter != null) {
                adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.getItem(adjPosition);
                }
                if(adapterCount < numAdapterEmptyPlaceHolder)
                    return null;
            }

            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            final int footerAdjPosition = position - numHeadersAndPlaceholders - numAdapterEmptyPlaceHolder;
            if(footerAdjPosition < numFootersAndPlaceholders) {
                if( footerAdjPosition % mNumColumns == 0) {
                    return mFooterViewInfos.get(footerAdjPosition / mNumColumns).data;
                }
                return null;
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public long getItemId(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (mAdapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.getItemId(adjPosition);
                }
            }
            return -1;
        }

        @Override
        public boolean hasStableIds() {
            if (mAdapter != null) {
                return mAdapter.hasStableIds();
            }
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                View headerViewContainer = mHeaderViewInfos
                        .get(position / mNumColumns).viewContainer;
                if (position % mNumColumns == 0) {
                    return headerViewContainer;
                } else {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount = 0;
            int numAdapterEmptyPlaceHolder = getAdapterAndPlaceHolderCount();
            if (mAdapter != null) {
                adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.getView(adjPosition, convertView, parent);
                }
                if(adjPosition < numAdapterEmptyPlaceHolder) {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(mRowHeight);
                    return convertView;
                }
            }

            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            final int footerAdjPosition = position - numHeadersAndPlaceholders - numAdapterEmptyPlaceHolder;
            if(footerAdjPosition < numFootersAndPlaceholders) {
                View footerViewContainer = mFooterViewInfos
                        .get(footerAdjPosition / mNumColumns).viewContainer;
                if (footerAdjPosition % mNumColumns == 0) {
                    return footerViewContainer;
                } else {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(footerViewContainer.getHeight());
                    return convertView;
                }
            }

            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public int getItemViewType(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders && (position % mNumColumns != 0)) {
                // Placeholders get the last view type number
                return mAdapter != null ? mAdapter.getViewTypeCount() : 1;
            }
            int adapterCount = 0;
            int numAdapterEmptyPlaceHolder = getAdapterAndPlaceHolderCount();
            if (mAdapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.getItemViewType(adjPosition);
                }
                if(adjPosition < numAdapterEmptyPlaceHolder) {
                    return mAdapter != null ? mAdapter.getViewTypeCount() + 1 : 2;
                }
            }

            int numFootersAndPlaceholders = getFootersCount() * mNumColumns;
            final int footerAdjPosition = position - numHeadersAndPlaceholders - numAdapterEmptyPlaceHolder;
            if(footerAdjPosition < numFootersAndPlaceholders && (footerAdjPosition % mNumColumns != 0)) {
                return mAdapter != null ? mAdapter.getViewTypeCount() : 1;
            }
            return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
        }

        @Override
        public int getViewTypeCount() {
            if (mAdapter != null) {
                return mAdapter.getViewTypeCount() + 2;
            }
            return 3;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.registerObserver(observer);
            if (mAdapter != null) {
                mAdapter.registerDataSetObserver(observer);
            }
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.unregisterObserver(observer);
            if (mAdapter != null) {
                mAdapter.unregisterDataSetObserver(observer);
            }
        }

        @Override
        public Filter getFilter() {
            if (mIsFilterable) {
                return ((Filterable) mAdapter).getFilter();
            }
            return null;
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return mAdapter;
        }

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyChanged();
        }
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener l) {
        mOnItemClickListener = l;
        super.setOnItemClickListener(getItemClickHandler());
    }

    @Override
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
        super.setOnItemLongClickListener(getItemClickHandler());
    }

    private ItemClickHandler getItemClickHandler() {
        if (mItemClickHandler == null) {
            mItemClickHandler = new ItemClickHandler();
        }
        return mItemClickHandler;
    }

    private class ItemClickHandler implements OnItemClickListener, OnItemLongClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mOnItemClickListener != null && mOriginalAdapter != null) {
                int nums = getNumColumnsCompatible();
                int resPos = position - getHeaderViewCount() * nums;
                if (resPos >= 0 && resPos < mOriginalAdapter.getCount()) {
                    mOnItemClickListener.onItemClick(parent, view, resPos, id);
                }
            }
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (mOnItemLongClickListener != null && mOriginalAdapter != null) {
                int resPos = position - getHeaderViewCount() * getNumColumnsCompatible();
                if (resPos >= 0 && resPos < mOriginalAdapter.getCount()) {
                    mOnItemLongClickListener.onItemLongClick(parent, view, resPos, id);
                }
            }
            return true;
        }
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
            Log.d("Grid", "firstVisible =" + firstVisibleItem);
            if(firstVisibleItem == 0) {
                View firstView = getChildAt(0);
                if(firstView != null) {
                    int top = firstView.getTop();
                    if(top < - callback.getStickyViewTop())
                        top = -callback.getStickyViewTop();
                    if(top > 0)
                        top = 0;
                    Log.d("Grid", "scroll top = " + top);
                    callback.onScrollChanged(top);
                } else if(firstVisibleItem < 6) {
                    callback.onScrollChanged(-callback.getStickyViewTop());
                }
            }
        }
    };

    @Override
    public void setPagePosition(int position) {
        mPagePosition = position;
    }

    @Override
    public void setStickyScrollCallback(StickyScrollCallback stickyScrollCallback) {
        mStickyScrollCallback = stickyScrollCallback;
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
        addHeaderView(view, null, false);
        view.setMinimumHeight(callback.getHeaderViewHeight());
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
        if (getFirstVisiblePosition() == 0) {
            View firstView = getChildAt(0);
            if (null != firstView) {
                return -firstView.getTop();
            }
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public void adjustPositionByTranslationYOfStickyView(int translationY) {
        int transY = Math.abs(translationY);
        int stickyViewTop = getStickyScrollCallback().getStickyViewTop();
        int scrollHeight = getFirstViewScrollTop();

        if(transY >= stickyViewTop) {
            if(scrollHeight >= transY)
                return;
//            setSelectionFromTop(0, translationY);
            smoothScrollToPositionFromTop(0, translationY, 0);
        } else {
//            setSelectionFromTop(0, translationY);
            smoothScrollToPositionFromTop(0, translationY, 0);
        }
    }

}
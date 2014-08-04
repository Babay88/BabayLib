package ru.babay.lib.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import ru.babay.lib.model.Gap;
import ru.babay.lib.model.ItemBase;
import ru.babay.lib.model.ItemContainer;
import ru.babay.lib.model.LoadedItemList;
import ru.babay.lib.view.LoadingListItemViewBase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 10.12.12
 * Time: 13:10
 */
public abstract class ItemListAdapterBase<T extends ItemBase> extends BaseAdapter {
    ArrayList<Object> mItems = new ArrayList<Object>();

    protected View.OnClickListener mGapClickListener;
    protected Runnable mLoadMoreRunnable;
    protected ItemContainer.ItemListener mItemListener;
    boolean noMoreRecords = false;
    boolean hasGap;


    public ItemListAdapterBase() {
    }

    @Override
    public int getCount() {
        return mItems.size() == 0 ? 0 : mItems.size() + (noMoreRecords ? 0 : 1);
    }

    public int getItemsCount(){
        return hasGap ? mItems.size()-1: mItems.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < mItems.size()) {
            return mItems.get(position);
        }

        return null;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        Object item = getItem(position);

        if (item == null)
            return -1;

        if (item instanceof ItemBase)
            return ((ItemBase) item).getId();

        if (item instanceof Gap)
            return -2;
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Object item = getItem(position);
        if (item == null)
            return getLoadingView(convertView, parent);
        if (item instanceof ItemBase)
            return getItemView((T)item, convertView, parent);
        if (item instanceof Gap)
            return getGapView((Gap) item, convertView, parent);
        return null;
    }

    protected abstract View getGapView(Gap gap, View convertView, ViewGroup parent);

    protected abstract View getLoadingView(View convertView, ViewGroup parent);

    protected abstract View getItemView(T item, View convertView, ViewGroup parent);




    public ArrayList<Object> getItems() {
        return mItems;
    }

    public ArrayList<ItemBase> getFirstItems(int amount){
        ArrayList<ItemBase> items =new ArrayList<ItemBase>(amount);
        int j=0;
        for (int i=0; i<amount; i++){
            while (! (mItems.get(j) instanceof ItemBase)){
                j++;
                if (j == mItems.size())
                    return items;
            }
            items.add((ItemBase)mItems.get(j));

            j++;
            if (j == mItems.size())
                return items;
        }
        return items;
    }

    public abstract LoadingListItemViewBase getLoadingItemView(Context context);/* {
        if (mLoadingItemView != null){
            LoadingListItemViewBase v =mLoadingItemView.get();
            if (v != null)
                return v;
        }
        return null;
    }*/

    public ItemBase getFirstItem() {
        return mItems.size() == 0 ? null : (ItemBase)mItems.get(0);
    }

    public ItemBase getLastItem() {
        return mItems.size() == 0 ? null : (ItemBase)mItems.get(mItems.size() - 1);
    }

    public void setData(List<Object> data) {
        mItems.clear();
        mItems.addAll(data);
        noMoreRecords = false;
        notifyDataSetChanged();
    }

    public void setData(LoadedItemList data) {
        if (data.getItems() == null)
            return;
        mItems.clear();
        mItems.addAll(data.getItems());
        noMoreRecords = data.getMore() == 0;
        notifyDataSetChanged();
    }

    public void prepend(LoadedItemList list) {
        if (list.getItems() == null)
            return;
        if (list.getGap() != null) {
            Gap gap = list.getGap();
            mItems.add(0, gap);
            hasGap = true;
        }
        mItems.addAll(0, list.getItems());
        notifyDataSetChanged();
    }

    public void addData(LoadedItemList loadedList) {
        if (loadedList == null || loadedList.getItems() == null)
            return;
        mItems.addAll(loadedList.getItems());
        if (loadedList.getMore() == 0)
            noMoreRecords = true;
        notifyDataSetChanged();
    }

    public void replaceGap(Gap sourceGap, LoadedItemList list) {
        if (list.getItems() == null)
            return;
        if (sourceGap == null)
            throw new IllegalStateException("can't replace gap: no gap");

        int gapPosition = mItems.indexOf(sourceGap);
        if (gapPosition == -1)
            throw new IllegalStateException("can't replace gap: no such gap in item list");
        mItems.addAll(gapPosition, list.getItems());
        Gap newGap = list.getGap();
        if (newGap == null) {
            mItems.remove(sourceGap);
            hasGap = false;
        } else {
            sourceGap.setOlderNextId(newGap.getOlderNextId());
            sourceGap.setNewerNextId(newGap.getNewerNextId());
            sourceGap.setAmount(newGap.getAmount());
            hasGap = true;
        }
        notifyDataSetChanged();
    }

    public void setGapClickListener(View.OnClickListener gapClickListener) {
        mGapClickListener = gapClickListener;
    }

    public void setLoadMoreRunnable(Runnable runnable) {
        this.mLoadMoreRunnable = runnable;
    }

    public void setItemListener(ItemContainer.ItemListener listener) {
        this.mItemListener = listener;
    }
}

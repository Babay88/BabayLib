package ru.babay.lib.model;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 19.12.12
 * Time: 20:06
 */
public class LoadedItemList<T extends ItemBase> {
    Class<T> aClass;
    int more;
    List<T> mItems;
    Gap gap;
    int topicId;

    public LoadedItemList(Class<T> aClass) {
        this.aClass = aClass;
    }

    public void setMore(int more) {
        this.more = more;
    }

    public void setItems(List mItems) {
        this.mItems = (List<T>)mItems;
    }

    public Class getType() {
        return aClass;
    }

    public int getMore() {
        return more;
    }

    public List<T> getItems() {
        return mItems;
    }

    public Gap getGap() {
        return gap;
    }

    public void setGap(Gap gap) {
        this.gap = gap;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }
}


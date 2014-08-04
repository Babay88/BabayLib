package ru.babay.lib.model;

import ru.babay.lib.view.ItemContextMenuView;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 20.12.12
 * Time: 22:03
 */
public interface ItemContainer<T extends ItemBase> {
    public enum Action {Like, Reply, Share}

    public void setItem(T item);
    public void refreshItem(boolean updating);
    public void refreshImage();
    public T getItem();
    public void hideLongClickMenu();
    public void setItemListener(ItemListener<T> listener);

    public interface ItemListener<T extends ItemBase>{
        public void onItemClicked(ItemContainer view, T item);
        public boolean onItemLongClicked(ItemContainer view, T item);
        public void onItemAuthorClicked(ItemContainer view, T item);

        public void onAction(ItemContainer view, T item, Action action);
        public void onShowLongClickMenu(ItemContainer view, T item, ItemContextMenuView menuView);
    }
}

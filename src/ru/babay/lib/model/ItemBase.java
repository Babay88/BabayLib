package ru.babay.lib.model;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: Babay
 * Date: 12.06.13
 * Time: 18:47
 * To change this template use File | Settings | File Templates.
 */
public class ItemBase implements Serializable{
    int id;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}

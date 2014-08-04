package ru.babay.lib.model;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 19.12.12
 * Time: 23:26
 */
public class Gap implements Serializable{
    int amount;
    int olderNextId;
    int newerNextId;

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getOlderNextId() {
        return olderNextId;
    }

    public void setOlderNextId(int olderNextId) {
        this.olderNextId = olderNextId;
    }

    public int getNewerNextId() {
        return newerNextId;
    }

    public void setNewerNextId(int newerNextId) {
        this.newerNextId = newerNextId;
    }
}

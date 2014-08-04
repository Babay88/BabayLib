package ru.babay.lib.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 23.10.13
 * Time: 9:35
 */
public class PurchaseItem {
    private static final String SKU = "s";
    private static final String PRICE = "p";
    private static final String TOKEN = "t";
    private static final String TITLE = "i";
    private static final String DESCRIPTION = "d";
    String sku;
    String price;
    String token;
    String title;
    String description;

    public PurchaseItem(String sku, String price) {
        this.sku = sku;
        this.price = price;
    }

    public PurchaseItem(JSONObject source) throws JSONException {
        this.sku = source.getString(SKU);
        this.price = source.getString(PRICE);
        if (!source.isNull(TOKEN))
            token = source.getString(TOKEN);
        if (!source.isNull(TITLE))
            title = source.getString(TITLE);
        if (!source.isNull(DESCRIPTION))
            description = source.getString(DESCRIPTION);
    }

    public String getSku() {
        return sku;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPurchaseToken() {
        return token;
    }

    public void setPurchaseToken(String purchaseToken) {
        this.token = purchaseToken;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || o.getClass() != PurchaseItem.class)
            return false;

        PurchaseItem other = (PurchaseItem) o;

        return strEquals(sku, other.sku)
                && strEquals(price, other.price)
                && strEquals(token, other.token)
                && strEquals(title, other.title)
                && strEquals(description, other.description);
    }

    private static boolean strEquals(String str1, String str2) {
        return (str1 == null && str2 == null) || !(str1 == null) && str1.equals(str2);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject res = new JSONObject();
        res.put(SKU, sku);
        res.put(PRICE, price);
        if (token != null)
            res.put(TOKEN, token);
        if (title != null)
            res.put(TITLE, title);
        if (description != null)
            res.put(DESCRIPTION, description);
        return res;
    }

    public static JSONArray toJsonArray(List<PurchaseItem> items) throws JSONException {
        JSONArray res = new JSONArray();
        for (int i = 0; i < items.size(); i++)
            res.put(items.get(i).toJson());
        return res;
    }

    public static ArrayList<PurchaseItem> fromJson(JSONArray source) throws JSONException {
        ArrayList<PurchaseItem> items = new ArrayList<PurchaseItem>(source.length());
        for (int i = 0; i < source.length(); i++)
            items.add(new PurchaseItem(source.getJSONObject(i)));
        return items;
    }
}

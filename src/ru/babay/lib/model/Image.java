package ru.babay.lib.model;

import ru.babay.lib.transport.TTL;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 14.08.13
 * Time: 14:47
 */
public class Image implements Serializable {
    String url;
    int width;
    int height;
    TTL ttl;

    public Image(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    public Image(String url, int width, int height, TTL ttl) {
        this.url = url;
        this.width = width;
        this.height = height;
        this.ttl = ttl;
    }

    public Image(String url, TTL ttl) {
        this.url = url;
        this.ttl = ttl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public TTL getTtl() {
        return ttl;
    }

    public void setTtl(TTL ttl) {
        this.ttl = ttl;
    }
}

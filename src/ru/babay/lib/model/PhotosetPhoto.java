package ru.babay.lib.model;

import ru.babay.lib.transport.TTL;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 20.08.13
 * Time: 2:42
 */
public class PhotosetPhoto implements Serializable{
    int id;
    String description;
    Image largeImage;
    Image smallImage;

    public PhotosetPhoto() {
    }

    public Image getLargeImage() {
        return largeImage;
    }

    public void setLargeImage(Image largeImage) {
        this.largeImage = largeImage;
        if (largeImage != null)
            largeImage.setTtl(TTL.TwoDays);
    }

    public Image getSmallImage() {
        return smallImage;
    }

    public void setSmallImage(Image smallImage) {
        this.smallImage = smallImage;
        if (smallImage != null)
            smallImage.setTtl(TTL.TwoDays);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

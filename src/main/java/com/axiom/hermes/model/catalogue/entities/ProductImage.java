package com.axiom.hermes.model.catalogue.entities;

import org.jboss.resteasy.core.ExceptionAdapter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.IOException;

@Entity
public class ProductImage {

    @Id
    public int productID;                    // ID владельца (Product/Category)
    private String filename;                 // Название файла
    private byte[] thumbnail;                // Миниатюрное изображение
    private byte[] image;                    // Полноразмерное изображение
    private long timestamp;                  // Время последнего изменения в миллисекундах

    public ProductImage() {}

    public ProductImage(int productID, String filename, byte[] imageFile, byte[] thumbnail) {
        this.productID = productID;
        this.filename = filename;
        this.image = imageFile;
        this.thumbnail = thumbnail;
        this.timestamp = System.currentTimeMillis();
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

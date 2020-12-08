package com.axiom.hermes.model.catalogue.entities;

import org.jboss.resteasy.core.ExceptionAdapter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.IOException;

@Entity
public class ProductImage {

    @Id
    public int productID;                   // ID владельца (Product/Category)
    public String filename;                 // Название файла
    public byte[] thumbnail;                // Миниатюрное изображение
    public byte[] image;                    // Полноразмерное изображение
    public long timestamp;                  // Временная метка обновления изображения

    public ProductImage() {}

    public ProductImage(int productID, String filename, byte[] imageFile, byte[] thumbnail) {
        this.productID = productID;
        this.filename = filename;
        this.image = imageFile;
        this.thumbnail = thumbnail;
        this.timestamp = System.nanoTime();
    }

}

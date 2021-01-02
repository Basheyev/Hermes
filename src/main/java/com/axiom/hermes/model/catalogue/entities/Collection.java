package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Коллекция товаров
 */
@Entity
public class Collection {

    public static final int SORT_NAME_ASC = 0;
    public static final int SORT_NAME_DESC = 1;
    public static final int SORT_PRICE_ASC = 2;
    public static final int SORT_PRICE_DESC = 3;
    public static final int SORT_MANUAL = 15;

    @Id @GeneratedValue
    private long collectionID;           // Код коллекции товара
    private String name;                 // Наименование коллекции товара
    private String description;          // Описание коллекции товара
    private byte[] image;                // Изображение коллекции
    private byte[] thumbnail;            // Миниатюра коллекции
    private int orderNumber;             // Порядковый номер коллекции
    private int sortOrder;               // Порядок сортировки товаров в коллекции

    public Collection() {}

    public long getCollectionID() {
        return collectionID;
    }

    public void setCollectionID(long collectionID) {
        this.collectionID = collectionID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}

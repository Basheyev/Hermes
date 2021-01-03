package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Коллекция товаров
 */
@Entity
public class Collection {

    @Id @GeneratedValue
    private long collectionID;           // Код коллекции товара
    private String name;                 // Наименование коллекции товара
    private String description;          // Описание коллекции товара
    private byte[] thumbnail;            // Миниатюрное изображение коллекции
    private int sortOrder;               // Порядок сортировки товаров в коллекции
    private boolean available;           // Доступна ли коллекция клиентам
    private long timestamp;              // Время последнего изменения коллекции в миллисекундах

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

    public byte[] getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(byte[] thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}

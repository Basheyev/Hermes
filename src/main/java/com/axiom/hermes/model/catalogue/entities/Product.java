package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Карточка товарной позиции
 * Примечание: дополнительные аттрибуты в ProductAttributes
 */
@Entity
public class Product {
    @Id
    @GeneratedValue
    public int productID;                  // ID товара
    private int categoryID;                // ID категории товара
    private String vendorCode;             // Артикул товара
    private String name;                   // Наименование товара
    private String description;            // Описание товара
    private String unitOfMeasure;          // Единица измерения
    private double price;                  // Цена отпуска товара
    private boolean available;             // Доступно ли клиентам для выбора
    private long timestamp;                // Время последнего изменения в миллисекунлаз

    public Product() { }

    public Product(String vendorCode, String name, String description, double price) {
        this.vendorCode = vendorCode;
        this.categoryID = 0;
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public int getCategoryID() {
        return categoryID;
    }

    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
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

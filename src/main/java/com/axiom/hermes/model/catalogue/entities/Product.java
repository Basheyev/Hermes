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
    private long productID;                // ID товара
    private String vendorCode;             // Артикул товара
    private String name;                   // Наименование товара
    private String description;            // Описание товара
    private double unitPrice;              // Цена отпуска товара
    private boolean available;             // Доступно ли клиентам для выбора
    private long timestamp;                // Время последнего изменения в миллисекунлаз

    public Product() { }

    public Product(String vendorCode, String name, String description, double price) {
        this.vendorCode = vendorCode;
        this.name = name;
        this.description = description;
        this.unitPrice = price;
    }

    public long getProductID() {
        return productID;
    }

    public void setProductID(long productID) {
        this.productID = productID;
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

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
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

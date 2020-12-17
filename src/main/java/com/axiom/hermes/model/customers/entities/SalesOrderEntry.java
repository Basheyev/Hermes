package com.axiom.hermes.model.customers.entities;

import javax.persistence.*;

/**
 * Позиция заказа клиента
 */
@Entity
@Table(indexes = {
    @Index(columnList = "orderID"),
    @Index(name = "SalesOrderEntryIndex", columnList = "orderID, productID")
})
public class SalesOrderEntry {
    @Id @GeneratedValue
    private long entryID;                    // Счётчик позиции
    private long orderID;                    // Код заказа
    private long productID;                  // Товарная позиция
    private long amount;                     // Количество заказанного товара
    private double price;                    // Цена товара на момент заказа
    private long fulfilledAmount;            // Позиция заказа выполнена

    public SalesOrderEntry() { }

    public SalesOrderEntry(long orderID, long productID, long amount, double price) {
        this.orderID = orderID;
        this.productID = productID;
        this.amount = amount;
        this.price = price;
    }

    public long getEntryID() {
        return entryID;
    }

    public void setEntryID(long entryID) {
        this.entryID = entryID;
    }

    public long getOrderID() {
        return orderID;
    }

    public void setOrderID(long orderID) {
        this.orderID = orderID;
    }

    public long getProductID() {
        return productID;
    }

    public void setProductID(long productID) {
        this.productID = productID;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getFulfilledAmount() {
        return fulfilledAmount;
    }

    public void setFulfilledAmount(long fulfilledAmount) {
        this.fulfilledAmount = fulfilledAmount;
    }
}

package com.axiom.hermes.model.customers.entities;

import javax.persistence.*;

/**
 * Позиция заказа клиента
 */
@Entity
@Table(indexes = {
    @Index(columnList = "orderID"),
    @Index(name = "SalesOrderItemIndex", columnList = "orderID, productID")
})
public class SalesOrderItem {
    @Id @GeneratedValue
    private long entryID;                    // Счётчик позиции
    private long orderID;                    // Код заказа
    private long productID;                  // Товарная позиция
    private long quantity;                     // Количество заказанного товара
    private double price;                    // Цена товара на момент заказа
    private long fulfilledQuantity;            // Позиция заказа выполнена

    public SalesOrderItem() { }

    public SalesOrderItem(long orderID, long productID, long quantity, double price) {
        this.orderID = orderID;
        this.productID = productID;
        this.quantity = quantity;
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

    public long getquantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getFulfilledQuantity() {
        return fulfilledQuantity;
    }

    public void setFulfilledQuantity(long fulfilledQuantity) {
        this.fulfilledQuantity = fulfilledQuantity;
    }
}

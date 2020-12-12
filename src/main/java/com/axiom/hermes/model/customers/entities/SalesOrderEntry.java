package com.axiom.hermes.model.customers.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Позиция заказа клиента
 */
@Entity
public class SalesOrderEntry {
    @Id @GeneratedValue
    private long entryID;                    // Счётчик позиции
    private long orderID;                    // Код заказа
    private int productID;                   // Товарная позиция
    private int amount;                      // Количество заказанного товара
    private double price;                    // Цена товара на момент заказа


    public SalesOrderEntry() { }

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

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

}

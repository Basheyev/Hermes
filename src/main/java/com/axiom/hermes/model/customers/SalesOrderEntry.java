package com.axiom.hermes.model.customers;

/**
 * Товарная позиция заказа клиента
 */
public class SalesOrderEntry {
    public long orderID;                    // Код заказа
    public int productID;                   // Товарная позиция
    public int orderedAmount;               // Количество заказанного товара
    public double price;                    // Цена товара на момент заказа
    public int fulfilledAmount;             // Количество упакованного товара
}

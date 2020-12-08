package com.axiom.hermes.model.customers;

/**
 * Заказ клиента
 */
public class SalesOrder {
    //------------------------------------------------------------------------
    public static final int STATUS_UNKOWN = 0;                // Без статуса
    public static final int STATUS_NEW = 1;                   // Новый заказ
    public static final int STATUS_CHEKING = 2;               // Заказ проверяется
    public static final int STATUS_ACCEPTED = 3;              // Заказ принят
    public static final int STATUS_CONFIRMED = 4;             // Заказ подвержден
    public static final int STATUS_CANCELED = 5;              // Заказ отменён
    public static final int STATUS_PAID = 6;                  // Заказ оплачен
    public static final int STATUS_PICKED = 7;                // Заказ собран
    public static final int STATUS_SHIPPED = 8;               // Заказ отгружен клиенту
    public static final int STATUS_PARTIALLY_COMPLETED = 9;   // Заказ частично выполнен
    public static final int STATUS_COMPLETED = 10;            // Заказ выполнен полностью
    //------------------------------------------------------------------------

    public int customerID;           // Код клиента
    public long orderID;             // Код заказа
    public long time;                // Время заказа
    public double total;             // Общая сумма заказа
    public double payed;             // Оплаченная сумма заказа
    public int status;               // Статус заказа
}

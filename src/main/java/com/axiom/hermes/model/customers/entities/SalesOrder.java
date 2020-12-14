package com.axiom.hermes.model.customers.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * Заказ клиента
 */
@Entity
public class SalesOrder {
    //-------------------------------------------------------------------------------------------
    public static final int STATUS_UNKOWN = 0;                  // Без статуса
    public static final int STATUS_NEW = 1;                     // Новый заказ
    public static final int STATUS_CHEKING = 2;                 // Заказ проверяется
    public static final int STATUS_ACCEPTED = 3;                // Заказ принят
    public static final int STATUS_CANCELED = 4;                // Заказ отменён
    public static final int STATUS_CONFIRMED = 5;               // Заказ подвержден
    public static final int STATUS_PAID = 6;                    // Заказ оплачен
    public static final int STATUS_PICKED = 7;                  // Заказ собран
    public static final int STATUS_SHIPPED = 8;                 // Заказ отгружен клиенту
    public static final int STATUS_PARTIALLY_COMPLETED = 9;     // Заказ частично выполнен
    public static final int STATUS_COMPLETED = 10;              // Заказ выполнен полностью

    public static final int STATUS_CHANGABLE = STATUS_ACCEPTED; // До какого статуса изменемый
    //-------------------------------------------------------------------------------------------

    @Id
    @GeneratedValue
    private long orderID;                    // Код заказа
    // todo добавить индекс
    private int customerID;                  // Код клиента
    private long orderTime;                  // Время заказа
    private int status;                      // Статус заказа
    private long timestamp;                  // Время изменения


    public SalesOrder() { }

    public SalesOrder(int customerID) {
        this.customerID = customerID;
        this.orderTime = System.currentTimeMillis();
        this.status = STATUS_NEW;
        this.timestamp = orderTime;
    }


    public long getOrderID() {
        return orderID;
    }

    public void setOrderID(long orderID) {
        this.orderID = orderID;
    }

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }

    public long getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(long orderTime) {
        this.orderTime = orderTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

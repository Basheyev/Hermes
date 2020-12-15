package com.axiom.hermes.model.inventory.entities;

import javax.persistence.*;

/**
 * Транзакция на складе
 */
@Entity
@Table(indexes = {
    @Index(columnList = "timestamp"),
    @Index(columnList = "productID"),
    @Index(columnList = "side"),
    @Index(columnList = "orderID"),
    @Index(name = "multiIndex1", columnList = "productID, side"),
    @Index(name = "multiIndex2", columnList = "productID, timestamp"),
    @Index(name = "multiIndex2", columnList = "productID, side, timestamp")
})
public class StockTransaction {

    //-------------------------------------------------------------------------------------
    public static final int SIDE_DEBIT = 1;              // Приход (дебет)
    public static final int SIDE_CREDIT = 2;             // Расход (кредит)
    //-------------------------------------------------------------------------------------
    public static final int DEBIT_PURCHASE = 10;         // Покупка у поставщика (приход)
    public static final int DEBIT_SALE_RETURN = 11;      // Возврат от клиента (приход)

    public static final int CREDIT_SALE = 20;            // Продажа клиенту (расход)
    public static final int CREDIT_PURCHASE_RETURN = 21; // Возврат поставщику (расход)
    public static final int CREDIT_WRITE_OFF = 22;       // Списание товара (расход)

    public static final int REGRADE = 30;                // Пересорт товара (приход/расход)
    //-------------------------------------------------------------------------------------


    @Id
    @GeneratedValue
    public long transactionID;       // код транзакции
    public long timestamp;           // время транзакции в миллисекундах
    public int productID;            // код товара
    public int side;                 // дебет/кредит (приход/расход)
    public int operationCode;        // код операции
    public int amount;               // количество товара
    public double price;             // цена товара
    public long orderID;             // код заказа как основание (покупки/продажи - зависит от поля side)
    public int userID;               // код пользователя внесшего запись
    public boolean deleted;          // транзакция помечена как удалененная


    public StockTransaction() {}

    public StockTransaction(long orderID, int productID, int side, int opCode, int amount, double price) {
        this.orderID = orderID;
        this.timestamp = System.currentTimeMillis();
        this.productID = productID;
        this.side = side;
        this.operationCode = opCode;
        this.amount = amount;
        this.price = price;
        this.deleted = false;
    }

    public long getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(long transactionID) {
        this.transactionID = transactionID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public int getSide() {
        return side;
    }

    public void setSide(int side) {
        this.side = side;
    }

    public int getOperationCode() {
        return operationCode;
    }

    public void setOperationCode(int operationCode) {
        this.operationCode = operationCode;
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

    public long getOrderID() {
        return orderID;
    }

    public void setOrderID(long orderID) {
        this.orderID = orderID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

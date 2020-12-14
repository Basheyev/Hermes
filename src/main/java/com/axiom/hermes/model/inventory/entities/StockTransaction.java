package com.axiom.hermes.model.inventory.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Транзакция на складе
 */
@Entity
public class StockTransaction {

    //-------------------------------------------------------------------------------------
    public static final int SIDE_DEBIT = 1;              // Приход (дебет)
    public static final int SIDE_CREDIT = 2;             // Расход (кредит)

    public static final int DEBIT_PURCHASE = 10;         // Покупка у поставщика (приход)
    public static final int DEBIT_SALE_RETURN = 11;      // Возврат от клиента (приход)

    public static final int CREDIT_SALE = 20;            // Продажа клиенту (расход)
    public static final int CREDIT_PURCHASE_RETURN = 21; // Возврат поставщику (расход)
    public static final int CREDIT_WRITE_OFF = 22;       // Списание товара (расход)

    public static final int REGRADE = 30;                // Пересорт товара (приход/расход)
    //-------------------------------------------------------------------------------------

    // TODO Добавить индексы по ключевым полям поиска

    @Id
    @GeneratedValue
    public long transactionID;       // код транзакции
    public long timestamp;           // время транзакции в миллисекундах
    public int productID;            // код товара
    public int side;                 // дебет/кредит
    public int operationCode;        // код операции
    public int amount;               // количество товара
    public double price;             // цена товара
    public int counterpartyID;       // код контрагента
    public int userID;               // код пользователя внесшего запись
    public boolean deleted;          // транзакция помечена как удалененная


    public StockTransaction() {}

    public StockTransaction(int productID, int side, int opCode, int amount, double price) {
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

    public int getCounterpartyID() {
        return counterpartyID;
    }

    public void setCounterpartyID(int counterpartyID) {
        this.counterpartyID = counterpartyID;
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

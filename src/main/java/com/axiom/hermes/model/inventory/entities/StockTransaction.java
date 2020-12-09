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
    public static final int SIDE_DEBIT = 1;             // Приход (дебет)
    public static final int SIDE_CREDIT = 2;            // Расход (кредит)
    public static final int OP_PURCHASE = 1;            // Покупка у поставщика (приход)
    public static final int OP_SALE = 2;                // Продажа клиенту (расход)
    public static final int OP_SALE_RETURN = 3;         // Возврат от клиента (приход)
    public static final int OP_PURCHASE_RETURN = 4;     // Возврат поставщику (расход)
    public static final int OP_REGRADE = 5;             // Пересорт товара (приход/расход)
    public static final int OP_WRITE_OF = 6;            // Списание товара (расход)
    //-------------------------------------------------------------------------------------

    @Id
    @GeneratedValue
    public long transactionID;       // код транзакции
    public long timestamp;           // время транзакции в наносекундах
    public int productID;            // код товара
    public int side;                 // дебет/кредит
    public int operationCode;        // код операции
    public int amount;               // количество товара
    public double price;             // цена товара
    public int counterpartyID;       // код контрагента
    public int userID;               // код пользователя внесшего запись
    public boolean deleted;          // транзакция помечена как удалененная

}

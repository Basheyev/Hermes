package com.axiom.hermes.model.inventory;


import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;

// TODO сделать защиту от отрицательных остатков
@ApplicationScoped
public class Inventory {

    @Inject
    EntityManager entityManager;

    /**
     * Регистрация покупки товара (приход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction purchase(int productID, int amount, double price) {
        StockTransaction purchaseTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_DEBIT,
                StockTransaction.DEBIT_PURCHASE,
                amount, price);
        entityManager.persist(purchaseTransaction);
        debitProductStock(productID, amount);
        return purchaseTransaction;
    }


    /**
     * Регистрация возврата товара (приход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction saleReturn(int productID, int amount, double price) {
        StockTransaction saleReturnTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_DEBIT,
                StockTransaction.DEBIT_SALE_RETURN,
                amount, price);
        entityManager.persist(saleReturnTransaction);
        debitProductStock(productID, amount);
        return saleReturnTransaction;
    }

    /**
     * Продажа товара (расход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction sale(int productID, int amount, double price) {
        StockTransaction saleTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_SALE,
                amount, price);
        entityManager.persist(saleTransaction);
        creditProductStock(productID, amount);
        return saleTransaction;
    }

    /**
     * Возврат поставщику закупленного товара (расход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction purchaseReturn(int productID, int amount, double price) {
        StockTransaction purchaseReturnTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_PURCHASE_RETURN,
                amount, price);
        entityManager.persist(purchaseReturnTransaction);
        creditProductStock(productID, amount);
        return purchaseReturnTransaction;
    }


    /**
     * Списание товара (расход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction writeOff(int productID, int amount, double price) {
        StockTransaction writeOffTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_WRITE_OFF,
                amount, price);
        entityManager.persist(writeOffTransaction);
        creditProductStock(productID, amount);
        return writeOffTransaction;
    }

    // Пересорт (regrade)
    public void regrade() {   }

    @Transactional
    private void debitProductStock(int productID, int amount) {
        StockInformation stockInfo = entityManager.find(
                StockInformation.class,
                productID,
                LockModeType.PESSIMISTIC_WRITE);
        if (stockInfo==null) return;
        stockInfo.setStockOnHand(stockInfo.getStockOnHand() + amount);
        entityManager.persist(stockInfo);
    }

    @Transactional
    private void creditProductStock(int productID, int amount) {
        StockInformation stockInfo = entityManager.find(
                StockInformation.class,
                productID,
                LockModeType.PESSIMISTIC_WRITE);
        if (stockInfo==null) return;
        stockInfo.setStockOnHand(stockInfo.getStockOnHand() - amount);
        entityManager.persist(stockInfo);
    }

}

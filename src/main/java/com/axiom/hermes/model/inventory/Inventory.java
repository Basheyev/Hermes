package com.axiom.hermes.model.inventory;


import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;

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
        if (!debitProductStock(productID, amount)) return null;
        StockTransaction purchaseTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_DEBIT,
                StockTransaction.DEBIT_PURCHASE,
                amount, price);
        entityManager.persist(purchaseTransaction);
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
        if (!debitProductStock(productID, amount)) return null;
        StockTransaction saleReturnTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_DEBIT,
                StockTransaction.DEBIT_SALE_RETURN,
                amount, price);
        entityManager.persist(saleReturnTransaction);
        return saleReturnTransaction;
    }

    /**
     * Продажа товара (расход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет товара
     */
    @Transactional
    public StockTransaction sale(int productID, int amount, double price) {
        if (!creditProductStock(productID, amount)) return null;
        StockTransaction saleTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_SALE,
                amount, price);
        entityManager.persist(saleTransaction);
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
        if (!creditProductStock(productID, amount)) return null;
        StockTransaction purchaseReturnTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_PURCHASE_RETURN,
                amount, price);
        entityManager.persist(purchaseReturnTransaction);
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
        if (!creditProductStock(productID, amount)) return null;
        StockTransaction writeOffTransaction = new StockTransaction(productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_WRITE_OFF,
                amount, price);
        entityManager.persist(writeOffTransaction);
        return writeOffTransaction;
    }

    // Пересорт (regrade)
    public void regrade() {   }

    @Transactional
    public StockInformation createStockKeepingUnit(int productID) {
        StockInformation stockInfo = new StockInformation(productID);
        entityManager.persist(stockInfo);
        return stockInfo;
    }

    @Transactional
    public StockInformation getStockInformation(int productID) {
        return entityManager.find(StockInformation.class, productID);
    }


    @Transactional
    private boolean debitProductStock(int productID, int amount) {
        StockInformation stockInfo = entityManager.find(
                StockInformation.class,
                productID,
                LockModeType.PESSIMISTIC_WRITE);
        if (stockInfo==null) return false;

        long newBalance = stockInfo.getStockOnHand() + amount;
        stockInfo.setStockOnHand(newBalance);
        entityManager.persist(stockInfo);

        return true;
    }

    @Transactional
    private boolean creditProductStock(int productID, int amount) {
        StockInformation stockInfo = entityManager.find(
                StockInformation.class,
                productID,
                LockModeType.PESSIMISTIC_WRITE);
        if (stockInfo==null) return false;

        long newBalance = stockInfo.getStockOnHand() - amount;
        if (newBalance < 0) return false;
        stockInfo.setStockOnHand(newBalance);
        entityManager.persist(stockInfo);

        return true;
    }

}

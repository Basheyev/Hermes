package com.axiom.hermes.model.inventory;


import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class Inventory {

    @Inject
    EntityManager entityManager;

    @Inject
    Catalogue catalogue;

    /**
     * Возвращает информацию по всем складским карточкам
     * @return список складских карточек
     */
    @Transactional
    public List<StockInformation> getAllStocks() {
        List<StockInformation> allStocks;
        String query = "SELECT a FROM StockInformation a";
        allStocks = entityManager.createQuery(query, StockInformation.class).getResultList();
        return allStocks;
    }


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

    /**
     * Создать складскую карточку под товарную позицию
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockInformation createStockKeepingUnit(int productID) {
        StockInformation stockInfo = new StockInformation(productID);
        entityManager.persist(stockInfo);
        return stockInfo;
    }


    /**
     * Получить складскую карточку по коду товарной позиции
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockInformation getStockInformation(int productID) {
        StockInformation stockInfo = entityManager.find(StockInformation.class, productID);
        // Если складской карточки нет, то создаём её если такая товарная позиция есть
        if (stockInfo==null) {
            Product product = catalogue.getProduct(productID);
            if (product==null) return null;
            stockInfo = createStockKeepingUnit(productID);
        }
        return stockInfo;
    }



    /**
     * Оформление прихода в складской карточке
     * @param productID код товарной позиции
     * @param amount количество
     * @return true если успешно, false - если такой товарной позиции нет
     */
    @Transactional
    private boolean debitProductStock(int productID, int amount) {
        StockInformation stockInfo = entityManager.find(
                StockInformation.class,
                productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет, то создаём её если такая товарная позиция есть
        if (stockInfo==null) {
            Product product = catalogue.getProduct(productID);
            if (product==null) return false;
            stockInfo = createStockKeepingUnit(productID);
        }

        long newBalance = stockInfo.getStockOnHand() + amount;
        stockInfo.setStockOnHand(newBalance);
        entityManager.persist(stockInfo);
        return true;
    }



    /**
     * Оформление расхода в складской карточке
     * @param productID код товарной позиции
     * @param amount количество
     * @return true - если успешно, false - если столько товара нет
     */
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

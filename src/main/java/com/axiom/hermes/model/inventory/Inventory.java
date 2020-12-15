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


    //-----------------------------------------------------------------------------------------------------
    // Работа со складскими транзакциями
    //-----------------------------------------------------------------------------------------------------

    /**
     * Регистрация покупки товара (приход)
     * @param productID код товарной позиции
     * @param amount количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction purchase(long orderID, int productID, int amount, double price) {
        if (orderID < 0) return null;
        if (!debitProductStock(productID, amount)) return null;
        StockTransaction purchaseTransaction = new StockTransaction(orderID, productID,
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
    public StockTransaction saleReturn(long orderID, int productID, int amount, double price) {
        if (!debitProductStock(productID, amount)) return null;
        StockTransaction saleReturnTransaction = new StockTransaction(orderID, productID,
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
    public StockTransaction sale(long orderID, int productID, int amount, double price) {
        if (!creditProductStock(productID, amount)) return null;
        StockTransaction saleTransaction = new StockTransaction(orderID, productID,
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
    public StockTransaction purchaseReturn(long orderID, int productID, int amount, double price) {
        if (!creditProductStock(productID, amount)) return null;
        StockTransaction purchaseReturnTransaction = new StockTransaction(orderID, productID,
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
    public StockTransaction writeOff(long orderID, int productID, int amount, double price) {
        if (!creditProductStock(productID, amount)) return null;
        StockTransaction writeOffTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_CREDIT,
                StockTransaction.CREDIT_WRITE_OFF,
                amount, price);
        entityManager.persist(writeOffTransaction);
        return writeOffTransaction;
    }

    // todo Пересорт (regrade)
    public void regrade() {

    }

    /**
     * Получить все складские транзакции по указанному заказу
     * @param orderID заказа
     * @return список складских транзакций по указанному заказу
     */
    public List<StockTransaction> getOrderTransactions(long orderID) {
        List<StockTransaction> orderTransactions;
        String query = "SELECT a FROM StockTransaction a WHERE a.orderID=" + orderID;
        orderTransactions = entityManager.createQuery(query, StockTransaction.class).getResultList();
        return orderTransactions;
    }

    /**
     * Получит складские транзакции по указанной товарной позиции в указанный период
     * @param productID товарной позиции
     * @param startTime с какого времени
     * @param endTime по какое время
     * @return список складских транзакций по указнной товарной позиции в указанный период
     */
    public List<StockTransaction> getProductTransactions(int productID, long startTime, long endTime) {
        List<StockTransaction> productTransactions;
        String query = "SELECT a FROM StockTransaction a WHERE a.productID=" + productID;
        if (startTime>0 || endTime > 0) {
            query += " WHERE a.timestamp > " + startTime + " AND a.timestamp < " + endTime;
        }
        productTransactions = entityManager.createQuery(query, StockTransaction.class).getResultList();
        return productTransactions;
    }

    //-----------------------------------------------------------------------------------------------------
    // Работа со складскими карточками
    //-----------------------------------------------------------------------------------------------------

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
     * Возвращает список складских карточек товарных позиций по которым требуется пополнение запасов
     * @return список складских карточек товарных позиций требующих пополнения запасов
     */
    public List<StockInformation> getReplenishmentStocks() {
        List<StockInformation> stocks;
        String query = "SELECT a FROM StockInformation a WHERE a.stockOnHand <= a.reorderPoint";
        stocks = entityManager.createQuery(query, StockInformation.class).getResultList();
        return stocks;
    }

    /**
     * Создать складскую карточку под товарную позицию
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockInformation createStockInformation(int productID) {
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
            stockInfo = createStockInformation(productID);
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

        // Если складской карточки нет, то создаём её если такая товарная позиция есть в каталоге
        if (stockInfo==null) {
            Product product = catalogue.getProduct(productID);
            if (product==null) return false;
            stockInfo = createStockInformation(productID);
        }

        long stockOnHand = stockInfo.getStockOnHand() + amount;
        long availableForSale = stockInfo.getAvailableForSale() + amount;

        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setAvailableForSale(availableForSale);

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

        // Проверить есть ли вообще остатки товара (на руках)
        long stockOnHand = stockInfo.getStockOnHand() - amount;
        if (stockOnHand < 0) return false;

        // todo если было забронировано - списывать из Committed Stock
        // Проверить есть ли доступные для продажи остатки товара (не забронированные)
        long availableForSale = stockInfo.getAvailableForSale() - amount;
        if (availableForSale < 0) return false;

        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setAvailableForSale(availableForSale);

        // todo Пересчитать committed stock (подумать как в принципе реализовать бронирование)

        entityManager.persist(stockInfo);

        return true;
    }


    public void bookOrder(long orderID) {
        // todo Забронировать позиции заказа на складе
    }

    public void unbookOrder(long orderID) {
        // todo Разбронировать позиции заказа на складе
    }


}

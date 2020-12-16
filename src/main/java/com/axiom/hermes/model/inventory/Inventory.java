package com.axiom.hermes.model.inventory;


import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.SalesOrders;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;
import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Управление складским учётом
 */
@ApplicationScoped
public class Inventory {

    @Inject EntityManager entityManager;

    @Inject Catalogue catalogue;
    @Inject SalesOrders salesOrders;

    //-----------------------------------------------------------------------------------------------------
    // Проведение базовых транзакций в журнале складского учёта
    //-----------------------------------------------------------------------------------------------------
    @Transactional
    private StockTransaction debitStock(int opCode, long orderID, int productID, int amount, double price) {
        if (orderID < 0 || productID < 0 || amount <= 0 || price < 0) return null;

        // Поднимаем складскую карточку товара
        StockInformation stockInfo = entityManager.find(
                StockInformation.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет, то создаём её если такая товарная позиция есть в каталоге
        if (stockInfo==null) {
            Product product = catalogue.getProduct(productID);
            if (product==null) return null;
            stockInfo = createStockInformation(productID);
        }

        long stockOnHand = stockInfo.getStockOnHand() + amount;
        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);

        StockTransaction stockTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_DEBIT, opCode, amount, price);
        entityManager.persist(stockTransaction);

        return stockTransaction;
    }

    @Transactional
    private StockTransaction creditStock(int opCode, long orderID, int productID, int amount, double price) {
        if (orderID < 0 || productID < 0 || amount <= 0 || price < 0) return null;

        // Поднимаем складскую карточку товара
        StockInformation stockInfo = entityManager.find(
                StockInformation.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет
        if (stockInfo==null) return null;

        long stockOnHand = stockInfo.getStockOnHand() - amount;
        if (stockOnHand < 0) return null;
        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);

        StockTransaction stockTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_CREDIT, opCode, amount, price);
        entityManager.persist(stockTransaction);

        return stockTransaction;
    }

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
        return debitStock(StockTransaction.DEBIT_PURCHASE, orderID, productID, amount, price);
    }


    /**
     * Продажа товара (расход)
     * @param orderID заказа
     * @param productID код товарной позиции
     * @param amount количество
     * @return true - если проведена, false - если нет товара
     */
    @Transactional
    public StockTransaction sale(long orderID, int productID, int amount) {

        double price;
        SalesOrderEntry salesOrderEntry = salesOrders.addFulfilledAmount(orderID, productID, amount);

        // Если Заказ не был указан продаём только с available for sale
        if (salesOrderEntry==null) {
            StockInformation stockInformation = getStockInformation(productID);
            if (stockInformation.getAvailableForSale() < amount) return null;
            price = catalogue.getProduct(productID).getPrice();
        } else {
            price = salesOrderEntry.getPrice();
        }

        return creditStock(StockTransaction.CREDIT_SALE, orderID, productID, amount, price);
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
        StockTransaction saleReturn =
                debitStock(StockTransaction.DEBIT_SALE_RETURN,
                orderID, productID, amount, price);
        if (saleReturn==null) return null;
        salesOrders.subtractFulfilledAmount(orderID, productID, amount);
        return saleReturn;
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
        return creditStock(StockTransaction.CREDIT_PURCHASE_RETURN, orderID, productID, amount, price);
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
        return creditStock(StockTransaction.CREDIT_WRITE_OFF, orderID, productID, amount, price);
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
            query += " WHERE a.timestamp BETWEEN " + startTime + " AND " + endTime;
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
        // fixme getAllStocks - будет давать неверную информацию по committed stock & available
        //  нужен ли этот метод вообще?
        return allStocks;
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
     * Получить складскую карточку по коду товарной позиции и вычисляет забронированный обьем
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
            return stockInfo;
        }

        // Получаем забронированное заказами количество товара
        long committedStock = salesOrders.getCommittedAmount(productID);

        // Считаем количество доступное для продажи (не забронированное)
        long availableForSale = stockInfo.getStockOnHand() - committedStock;
        if (availableForSale < 0) availableForSale = 0;

        stockInfo.setCommittedStock(committedStock);
        stockInfo.setAvailableForSale(availableForSale);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);

        return stockInfo;
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

}

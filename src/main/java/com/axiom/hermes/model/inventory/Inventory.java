package com.axiom.hermes.model.inventory;


import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.SalesOrders;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;
import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;
import static com.axiom.hermes.model.inventory.entities.StockTransaction.*;

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
    // Базовые операции в журнале складского учёта - тут основная бизнес логика и производительность
    //-----------------------------------------------------------------------------------------------------
    @Transactional
    private StockTransaction debitStock(int opCode, long orderID, int productID, int amount, double price) {
        if (orderID < 0 || productID < 0 || amount <= 0 || price < 0) return null;

        // Поднимаем складскую карточку товара
        StockInformation stockInfo = entityManager.find(
                StockInformation.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет
        if (stockInfo==null) {
            // Если такая товарная позиция есть в каталоге
            Product product = catalogue.getProduct(productID);
            if (product==null) return null;
            // Создаем складскую карточку под товарную позицию
            stockInfo = createStockInformation(productID);
        }

        // Обновляем данные позиции заказа если операция возврата товара
        if (opCode==DEBIT_SALE_RETURN) {
            // Уменьшаем количество отгруженного товара по позиции заказа
            salesOrders.subtractFulfilledAmount(orderID, productID, amount);
        }

        // Проводим складскую транзакцию в журнале складских транзакций
        StockTransaction stockTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_DEBIT, opCode, amount, price);
        entityManager.persist(stockTransaction);

        // Обновляем информацию в складской карточке
        long stockOnHand = stockInfo.getStockOnHand() + amount;
        long availableForSale = stockOnHand - stockInfo.getCommittedStock();
        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setAvailableForSale(availableForSale);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);

        return stockTransaction;
    }


    @Transactional
    private StockTransaction creditStock(int opCode, long orderID, int productID, int amount, double price) {
        if (orderID < 0 || productID < 0 || amount <= 0 || price < 0) return null;

        // Поднимаем складскую карточку товара
        StockInformation stockInfo = entityManager.find(
                StockInformation.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет, то и товара нет
        if (stockInfo==null) return null;
        // Если не хватает остатков
        long stockOnHand = stockInfo.getStockOnHand();
        if (stockOnHand < amount) return null;
        long committedStock = stockInfo.getCommittedStock();
        long availableForSale = stockInfo.getAvailableForSale();

        // Если это операции Продажи
        if (opCode==CREDIT_SALE) {
            // Обновляем данные позиции заказа чтобы снять бронь с указанного количества
            SalesOrderEntry salesOrderEntry = salesOrders.addFulfilledAmount(orderID, productID, amount);
            // Если такой брони нет - продаём незабронированных остатков (AvailableForSale)
            if (salesOrderEntry==null) {
                // Получаем забронированное подтвержденными заказами количество товара
                committedStock = salesOrders.getCommittedAmount(productID);
                // Считаем количество доступное для продажи (не забронированное)
                availableForSale = stockInfo.getStockOnHand() - committedStock;
                // Проверяем хватает ли незабронированных остатков
                if (availableForSale < amount) return null;
                // Если хватает берем официальную цену из каталога
                price = catalogue.getProduct(productID).getPrice();
                // Пересчитываем остатки для складской карточки
                stockOnHand -= amount;
                availableForSale -= amount;
            } else {
                // Если такая бронь есть - продаём с забронированных остатков (CommittedStock)
                price = salesOrderEntry.getPrice();              // Берем цену из заказа
                stockOnHand -= amount;                           // Пересчитываем общие остатки
                committedStock -= amount;                        // Пересчитываем забронированные остатки
            }
        } else {
            // Для всех остальных операций уменьшаем stockOnHand и availableForSale
            stockOnHand -= amount;
            availableForSale -= amount;
            if (availableForSale < 0) availableForSale = 0;
        }

        // Проводим складскую транзакцию в журнале складских транзакций
        StockTransaction stockTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_CREDIT, opCode, amount, price);
        entityManager.persist(stockTransaction);

        // Обновляем складскую карточку
        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setCommittedStock(committedStock);
        stockInfo.setAvailableForSale(availableForSale);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);

        return stockTransaction;
    }

    //-----------------------------------------------------------------------------------------------------
    // Проведение складских транзакций в журнале складского учёта
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
        return debitStock(DEBIT_PURCHASE, orderID, productID, amount, price);
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
        return creditStock(CREDIT_SALE, orderID, productID, amount, 0);
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
        return debitStock(DEBIT_SALE_RETURN, orderID, productID, amount, price);
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
        return creditStock(CREDIT_PURCHASE_RETURN, orderID, productID, amount, price);
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
        return creditStock(CREDIT_WRITE_OFF, orderID, productID, amount, price);
    }

    //-----------------------------------------------------------------------------------------------------
    // Получение данных по транзакциям
    //-----------------------------------------------------------------------------------------------------

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

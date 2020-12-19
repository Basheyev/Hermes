package com.axiom.hermes.model.inventory;


import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.SalesOrders;
import com.axiom.hermes.model.customers.entities.SalesOrderItem;
import com.axiom.hermes.model.inventory.entities.StockCard;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import static com.axiom.hermes.common.exceptions.HermesException.NOT_FOUND;
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
    private StockTransaction incomingStock(int opCode, long orderID, long productID, long quantity, double price)
            throws HermesException {

        if (orderID < 0 || productID < 0 || quantity <= 0 || price < 0) return null;

        // Поднимаем складскую карточку товара
        StockCard stockInfo = entityManager.find(
                StockCard.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет
        if (stockInfo==null) {
            // Если такая товарная позиция есть в каталоге
            Product product = catalogue.getProduct(productID);
            // Создаем складскую карточку под товарную позицию
            stockInfo = createStockCard(product.getProductID());
        }

        // Обновляем данные позиции заказа если операция возврата товара
        if (opCode==DEBIT_SALE_RETURN) {
            // Уменьшаем количество отгруженного товара по позиции заказа
            salesOrders.subtractFulfilledQuantity(orderID, productID, quantity);
        }

        // Проводим складскую транзакцию в журнале складских транзакций
        StockTransaction stockTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_DEBIT, opCode, quantity, price);
        entityManager.persist(stockTransaction);

        // Обновляем информацию в складской карточке
        long stockOnHand = stockInfo.getStockOnHand() + quantity;
        long availableForSale = stockOnHand - stockInfo.getCommittedStock();
        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setAvailableForSale(availableForSale);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);

        return stockTransaction;
    }


    @Transactional
    private StockTransaction outgoingStock(int opCode, long orderID, long productID, long quantity, double price)
        throws HermesException{
        if (orderID < 0 || productID < 0 || quantity <= 0 || price < 0) return null;

        // Поднимаем складскую карточку товара
        StockCard stockInfo = entityManager.find(
                StockCard.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет, то и товара нет
        if (stockInfo==null) {
            throw new HermesException(NOT_FOUND, "Inventory out of stock",
                    "Requested productID=" + productID + " stock on hand not found.");
        }
        // Если не хватает остатков
        long stockOnHand = stockInfo.getStockOnHand();
        if (stockOnHand < quantity) {
            throw new HermesException(NOT_FOUND, "Inventory out of stock",
                    "ProductID=" + productID + " stock on hand: " + stockOnHand + " requested quantity: " + quantity);
        }
        long committedStock = stockInfo.getCommittedStock();
        long availableForSale = stockInfo.getAvailableForSale();

        // Если это операции Продажи
        if (opCode==CREDIT_SALE) {
            // Обновляем данные позиции заказа чтобы снять бронь с указанного количества
            SalesOrderItem salesOrderItem;
            try {
                salesOrderItem = salesOrders.addFulfilledQuantity(orderID, productID, quantity);
                // Если такая бронь есть - продаём с забронированных остатков (CommittedStock)
                price = salesOrderItem.getPrice();              // Берем цену из заказа
                stockOnHand -= quantity;                        // Пересчитываем общие остатки
                committedStock -= quantity;                     // Пересчитываем забронированные остатки
            } catch (HermesException exception) {
                if (exception.getStatus()== NOT_FOUND) {
                    // Получаем забронированное подтвержденными заказами количество товара
                    committedStock = salesOrders.getCommittedquantity(productID);
                    // Считаем количество доступное для продажи (не забронированное)
                    availableForSale = stockInfo.getStockOnHand() - committedStock;
                    // Проверяем хватает ли незабронированных остатков
                    if (availableForSale < quantity) {
                        throw new HermesException(NOT_FOUND, "Inventory not available for sale",
                                "ProductID=" + productID +
                                " stock on hand: " + stockOnHand +
                                " available for sale: " + availableForSale +
                                " requested quantity: " + quantity);
                    }
                    // Если хватает берем официальную цену из каталога
                    price = catalogue.getProduct(productID).getPrice();
                    // Пересчитываем остатки для складской карточки
                    stockOnHand -= quantity;
                    availableForSale -= quantity;
                } else throw exception;
            }
        } else {
            // Для всех остальных операций уменьшаем stockOnHand и availableForSale
            stockOnHand -= quantity;
            availableForSale -= quantity;
            if (availableForSale < 0) availableForSale = 0;
        }

        // Проводим складскую транзакцию в журнале складских транзакций
        StockTransaction stockTransaction = new StockTransaction(orderID, productID,
                StockTransaction.SIDE_CREDIT, opCode, quantity, price);
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
     * @param quantity количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction purchase(long orderID, long productID, long quantity, double price)
            throws HermesException {
        return incomingStock(DEBIT_PURCHASE, orderID, productID, quantity, price);
    }

    /**
     * Продажа товара (расход)
     * @param orderID заказа
     * @param productID код товарной позиции
     * @param quantity количество
     * @return true - если проведена, false - если нет товара
     */
    @Transactional
    public StockTransaction sale(long orderID, long productID, long quantity) throws HermesException  {
        return outgoingStock(CREDIT_SALE, orderID, productID, quantity, 0);
    }

    /**
     * Регистрация возврата товара (приход)
     * @param productID код товарной позиции
     * @param quantity количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction saleReturn(long orderID, long productID, long quantity, double price) throws HermesException {
        return incomingStock(DEBIT_SALE_RETURN, orderID, productID, quantity, price);
    }

    /**
     * Возврат поставщику закупленного товара (расход)
     * @param productID код товарной позиции
     * @param quantity количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction purchaseReturn(long orderID, long productID, long quantity, double price) throws HermesException {
        return outgoingStock(CREDIT_PURCHASE_RETURN, orderID, productID, quantity, price);
    }

    /**
     * Списание товара (расход)
     * @param productID код товарной позиции
     * @param quantity количество
     * @param price цена
     * @return true - если проведена, false - если нет
     */
    @Transactional
    public StockTransaction writeOff(long orderID, long productID, long quantity, double price) throws HermesException {
        return outgoingStock(CREDIT_WRITE_OFF, orderID, productID, quantity, price);
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
    public List<StockTransaction> getProductTransactions(long productID, long startTime, long endTime) {
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
    public List<StockCard> getAllStocks() {
        List<StockCard> allStocks;
        String query = "SELECT a FROM StockCard a";
        allStocks = entityManager.createQuery(query, StockCard.class).getResultList();
        return allStocks;
    }


    /**
     * Создать складскую карточку под товарную позицию
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockCard createStockCard(long productID) {
        StockCard stockInfo = new StockCard(productID);
        entityManager.persist(stockInfo);
        return stockInfo;
    }


    /**
     * Получить складскую карточку по коду товарной позиции и вычисляет забронированный обьем
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockCard getStockCard(long productID) throws HermesException {
        StockCard stockInfo = entityManager.find(StockCard.class, productID);
        // Если складской карточки нет, то создаём её если такая товарная позиция есть
        if (stockInfo==null) {
            Product product = catalogue.getProduct(productID);
            stockInfo = createStockCard(product.getProductID());
            return stockInfo;
        }
        return stockInfo;
    }

    /**
     * Обновляет в складской карточке количество забронированнного товара по данным подтвержденных заказов
     * @param productID товарная позиция
     * @return обновленная складская карточка
     */
    @Transactional
    public StockCard updateCommittedStock(long productID) throws HermesException {
        // Поднимаем складскую карточку товара
        StockCard stockInfo = entityManager.find(
                StockCard.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        if (stockInfo==null) {
            throw new HermesException(NOT_FOUND, "Inventory stock card missing",
                    "Requested productID=" + productID + " stock card not found.");
        }

        // Обновляем информацию в складской карточке
        long committedStock = salesOrders.getCommittedquantity(productID);
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
    @Transactional
    public List<StockCard> getReplenishmentStocks() {
        List<StockCard> stocks;
        String query = "SELECT a FROM StockCard a WHERE a.stockOnHand <= a.reorderPoint";
        stocks = entityManager.createQuery(query, StockCard.class).getResultList();
        return stocks;
    }

}

package com.axiom.hermes.model.inventory;


import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.common.validation.Validator;
import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.SalesOrders;
import com.axiom.hermes.model.customers.entities.SalesOrderItem;
import com.axiom.hermes.model.inventory.entities.StockCard;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import static com.axiom.hermes.common.exceptions.HermesException.INTERNAL_SERVER_ERROR;
import static com.axiom.hermes.common.exceptions.HermesException.NOT_FOUND;
import static com.axiom.hermes.model.inventory.entities.StockTransaction.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Управление складским учётом
 */
@ApplicationScoped
public class Inventory {

    public static final int MAX_RESULTS = 256;

    @Inject EntityManager entityManager;
    @Inject TransactionManager transactionManager;

    @Inject Catalogue catalogue;
    @Inject SalesOrders salesOrders;

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
        return incomingStock(IN_PURCHASE, orderID, productID, quantity, price);
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
        return outgoingStock(OUT_SALE, orderID, productID, quantity, 0);
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
        return incomingStock(IN_SALE_RETURN, orderID, productID, quantity, price);
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
        return outgoingStock(OUT_PURCHASE_RETURN, orderID, productID, quantity, price);
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
        return outgoingStock(OUT_WRITE_OFF, orderID, productID, quantity, price);
    }

    //-----------------------------------------------------------------------------------------------------
    // Получение данных по транзакциям
    //-----------------------------------------------------------------------------------------------------

    /**
     * Получить все складские транзакции по указанному заказу
     * @param orderID заказа
     * @return список складских транзакций по указанному заказу
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<StockTransaction> getOrderTransactions(long orderID) throws HermesException {
        Validator.nonNegativeInteger("orderID", orderID);
        List<StockTransaction> orderTransactions;
        String query = "SELECT a FROM StockTransaction a WHERE a.orderID=" + orderID;
        try {
            orderTransactions = entityManager.createQuery(query, StockTransaction.class).getResultList();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Query failed", exception.getMessage());
        }
        return orderTransactions;
    }

    /**
     * Получит складские транзакции по указанной товарной позиции в указанный период
     * @param productID товарной позиции
     * @param startTime с какого времени
     * @param endTime по какое время
     * @return список складских транзакций по указнной товарной позиции в указанный период
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<StockTransaction> getProductTransactions(long productID, long startTime, long endTime)
        throws HermesException {
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("productID", productID);
        Validator.nonNegativeInteger("startTime", startTime);
        Validator.nonNegativeInteger("endTime", endTime);

        List<StockTransaction> productTransactions;
        String query = "SELECT a FROM StockTransaction a WHERE a.productID=" + productID;
        if (startTime>0 || endTime > 0) {
            query += " WHERE a.timestamp BETWEEN " + startTime + " AND " + endTime;
        }

        try {
            TypedQuery<StockTransaction> tq = entityManager.createQuery(query, StockTransaction.class);
            productTransactions = tq.setMaxResults(MAX_RESULTS).getResultList();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Query failed", exception.getMessage());
        }
        return productTransactions;
    }

    //-----------------------------------------------------------------------------------------------------
    // Работа со складскими карточками
    //-----------------------------------------------------------------------------------------------------


    /**
     * Создать складскую карточку под товарную позицию
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockCard createStockCard(long productID) throws HermesException {
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("productID", productID);

        // Если такая товарная позиция есть в каталоге
        Product product = catalogue.getProduct(productID);
        // Создаём складскую карточку
        StockCard stockInfo = new StockCard(product.getProductID());
        // Сохраняем складскую карточку
        entityManager.persist(stockInfo);
        return stockInfo;
    }


    /**
     * Возвращает информацию по всем складским карточкам
     * @return список складских карточек
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<StockCard> getAllStocks() throws HermesException {
        List<StockCard> allStocks;
        String query = "SELECT a FROM StockCard a";
        try {
            allStocks = entityManager.createQuery(query, StockCard.class).getResultList();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Query failed", exception.getMessage());
        }
        return allStocks;
    }


    /**
     * Получить складскую карточку по коду товарной позиции и вычисляет забронированный обьем
     * @param productID код товарной позиции
     * @return складская карточка
     */
    @Transactional
    public StockCard getStockCard(long productID) throws HermesException {
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("productID", productID);

        StockCard stockInfo = entityManager.find(StockCard.class, productID);
        // Если складской карточки нет, то создаём её если такая товарная позиция есть
        if (stockInfo==null) stockInfo = createStockCard(productID);
        return stockInfo;
    }

    /**
     * Обновляет в складской карточке количество забронированнного товара по данным подтвержденных заказов
     * @param productID товарная позиция
     * @return обновленная складская карточка
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public StockCard updateCommittedStock(long productID) throws HermesException {
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("productID", productID);

        // Поднимаем складскую карточку товара
        StockCard stockInfo = entityManager.find(
                StockCard.class, productID,
                LockModeType.PESSIMISTIC_WRITE);

        if (stockInfo==null) {
            throw new HermesException(NOT_FOUND, "Inventory stock card missing",
                    "Requested productID=" + productID + " stock card not found.");
        }

        // Обновляем информацию в складской карточке
        long stockOnHand = stockInfo.getStockOnHand();
        long committedQuantity = salesOrders.getCommittedQuantity(productID);
        long availableForSale = stockOnHand - committedQuantity;
        if (availableForSale < 0) availableForSale = 0;
        stockInfo.setCommittedStock(committedQuantity);
        stockInfo.setAvailableForSale(availableForSale);
        stockInfo.setTimestamp(System.currentTimeMillis());
        entityManager.persist(stockInfo);
        return stockInfo;
    }

    /**
     * Возвращает список складских карточек товарных позиций по которым требуется пополнение запасов
     * @return список складских карточек товарных позиций требующих пополнения запасов
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<StockCard> getReplenishmentStocks() throws HermesException {
        List<StockCard> stocks;
        String query = "SELECT a FROM StockCard a WHERE a.stockOnHand <= a.reorderPoint";
        try {
            stocks = entityManager.createQuery(query, StockCard.class).getResultList();
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Query failed", exception.getMessage());
        }
        return stocks;
    }


    //-----------------------------------------------------------------------------------------------------
    // Базовые операции в журнале складского учёта - тут основная бизнес логика и производительность
    //-----------------------------------------------------------------------------------------------------

    /**
     * Проведение в складском журнале транзакции прихода товара
     * @param opCode код операции
     * @param orderID заказ
     * @param productID продукт
     * @param quantity количество
     * @param price цена
     * @return сохраненная складская транзакция
     * @throws HermesException информация об ошибке
     */
    @Transactional
    private StockTransaction incomingStock(int opCode, long orderID, long productID, long quantity, double price)
            throws HermesException {
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("orderID", orderID);
        Validator.nonNegativeInteger("productID", productID);
        Validator.nonNegativeInteger("quantity", quantity);
        Validator.nonNegativeNumber("price", price);

        // Обновляем данные позиции заказа если операция возврата товара
        if (opCode== IN_SALE_RETURN) {
            // Уменьшаем количество отгруженного товара по позиции заказа
            try {
                salesOrders.subtractFulfilledQuantity(orderID, productID, quantity);
            } catch(HermesException e) {
                // если не нашли заказ из которого вычесть - ничего страшного
            }
        }

        StockTransaction transaction;
        try {
            // Формируем складскую транзакцию
            transaction = new StockTransaction(orderID, productID, SIDE_IN, opCode, quantity, price);
            // Проводим складскую транзакцию в журнале складских транзакций
            entityManager.persist(transaction);
            // Обновляем складскую карточку
            updateStockBalance(SIDE_IN, opCode, false, productID, quantity, transaction.getTimestamp());
        } catch (HermesException exception) {
            try {
                transactionManager.setRollbackOnly();
            } catch (IllegalStateException | SystemException e) {
                e.printStackTrace();
            }
            throw exception;
        }
        return transaction;
    }

    /**
     * Проведение в складском журнале транзакции расхода товара
     * @param opCode код операции
     * @param orderID заказ
     * @param productID продукт
     * @param quantity количество
     * @param price цена
     * @return сохраненная складская транзакция
     * @throws HermesException информация об ошибке
     */
    @Transactional
    private StockTransaction outgoingStock(int opCode, long orderID, long productID, long quantity, double price)
            throws HermesException {
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("orderID", orderID);
        Validator.nonNegativeInteger("productID", productID);
        Validator.nonNegativeInteger("quantity", quantity);
        Validator.nonNegativeNumber("price", price);

        boolean useCommittedStock = false;
        // Если это операции Продажи
        if (opCode == OUT_SALE) {
            try {
                // Пробуем обновить данные позиции заказа чтобы снять бронь с указанного количества
                SalesOrderItem salesOrderItem = salesOrders.addFulfilledQuantity(orderID, productID, quantity);
                // Если такая позиция заказа есть - устанавливаем флаг продажи с забронированных остатков
                useCommittedStock = true;                                      // Расходуем с committedStock
                price = salesOrderItem.getPrice();                             // Берем цену из самого заказа
            } catch (HermesException exception) {
                if (exception.getStatus()== NOT_FOUND) {                       // Если такой позиции заказа нет
                    price = catalogue.getProduct(productID).getPrice();        // Берём цену из каталога
                    useCommittedStock = false;                                 // Расходуем с availableForSale
                } else throw exception;                                        // Если что-то другое кидаем ошибку
            }
        }

        StockTransaction transaction;

        try {
            // Формируем новую транзакцию
            transaction = new StockTransaction(orderID, productID, SIDE_OUT, opCode, quantity, price);
            // Проводим складскую транзакцию в журнале складских транзакций
            entityManager.persist(transaction);
            // Обновляем складскую карточку
            updateStockBalance(SIDE_OUT, opCode, useCommittedStock, productID, quantity, transaction.getTimestamp());
        } catch (HermesException exception) {
            try {
                transactionManager.setRollbackOnly();
            } catch (IllegalStateException | SystemException e) {
                e.printStackTrace();
            }
            throw exception;
        }
        return transaction;
    }


    /**
     * Обновление данных складской карточки
     * @param side приход/расход (SIDE_IN / SIDE_OUT)
     * @param opCode код операции
     * @param useComittedStock true расходовать с забронированных остатков
     * @param productID продукт
     * @param quantity количество
     * @param timestamp временна метка транзакции складского журнала
     * @return обновленная складская карточка
     * @throws HermesException информация об ошибке
     */
    @Transactional
    private StockCard updateStockBalance(int side,
                                         int opCode,
                                         boolean useComittedStock,
                                         long productID,
                                         long quantity,
                                         long timestamp) throws HermesException{
        // Проверяем валидность параметров
        Validator.nonNegativeInteger("productID", productID);
        Validator.nonNegativeInteger("quantity", quantity);

        // Поднимаем складскую карточку товара и блокируем на запись/чтение пока не закончим обновление
        StockCard stockInfo = entityManager.find(StockCard.class, productID, LockModeType.PESSIMISTIC_WRITE);

        // Если складской карточки нет
        if (stockInfo==null) {
            // Если это поступление товара - создаем складскую карточку под товарную позицию
            if (side==SIDE_IN) stockInfo = createStockCard(productID);
            // Если это расход товара - сообщаем, что остатков в любом случае нет
            else throw new HermesException(NOT_FOUND, "Inventory out of stock",
                          "Requested productID=" + productID + " stock on hand not found.");
        }

        // Если это расход товара - проверяем общие остатки
        long stockOnHand = stockInfo.getStockOnHand();
        if (side==SIDE_OUT && stockOnHand < quantity) {
            throw new HermesException(NOT_FOUND, "Inventory out of stock",
                    "ProductID=" + productID + " stock on hand: " + stockOnHand + " requested quantity: " + quantity);
        }

        long committedStock = stockInfo.getCommittedStock();
        long availableForSale = stockInfo.getAvailableForSale();

        // Если это расход товара
        if (side==SIDE_OUT) {
            // Если расходуем забронированный товар - списываем с забронированных остатков
            stockOnHand -= quantity;                            // Пересчитываем общие остатки
            if (useComittedStock) committedStock -= quantity;   // Пересчитываем забронированные остатки
            else {
                // Если это конкретно операция продажи, то при недостаче свободных остатков кидаем исключение
                if (opCode==OUT_SALE && availableForSale < quantity) {
                    throw new HermesException(NOT_FOUND, "Inventory out of stock", "ProductID=" + productID +
                            " available for sale: " + availableForSale + " requested quantity: " + quantity);
                }
                // Во всех остальных случаях
                availableForSale -= quantity;
                if (availableForSale < 0) availableForSale = 0;
            }
        } else if (side==SIDE_IN) {
            stockOnHand += quantity;
            availableForSale = stockOnHand - committedStock;
        }

        // Обновляем информацию в складской карточке
        stockInfo.setStockOnHand(stockOnHand);
        stockInfo.setCommittedStock(committedStock);
        stockInfo.setAvailableForSale(availableForSale);
        stockInfo.setTimestamp(timestamp);
        entityManager.persist(stockInfo);

        return stockInfo;
    }

}

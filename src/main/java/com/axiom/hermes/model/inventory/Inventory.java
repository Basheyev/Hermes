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
    // Работа со складскими карточками (fixme записывать состояния отдельными записями - для отчетов и скорости)
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
    public StockCard createStockCard(long productID) throws HermesException {
        // Если такая товарная позиция есть в каталоге
        Product product = catalogue.getProduct(productID);
        // Создаём складскую карточку
        StockCard stockInfo = new StockCard(productID);
        // Сохраняем складскую карточку
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
        long committedStock = salesOrders.getCommittedQuantity(productID);
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


    //-----------------------------------------------------------------------------------------------------
    // Базовые операции в журнале складского учёта - тут основная бизнес логика и производительность
    //-----------------------------------------------------------------------------------------------------
    @Transactional
    private StockTransaction incomingStock(int opCode, long orderID, long productID, long quantity, double price)
            throws HermesException {
        // Проверяем валидность параметров
        if (orderID < 0 || productID < 0 || quantity <= 0 || price < 0) return null;
        // Обновляем данные позиции заказа если операция возврата товара
        if (opCode== IN_SALE_RETURN) {
            // Уменьшаем количество отгруженного товара по позиции заказа
            salesOrders.subtractFulfilledQuantity(orderID, productID, quantity);
        }
        // Проводим складскую транзакцию в журнале складских транзакций
        StockTransaction transaction = new StockTransaction(orderID, productID, SIDE_IN, opCode, quantity, price);
        entityManager.persist(transaction);
        updateStockBalance(SIDE_IN, opCode,false, productID, quantity, transaction.getTimestamp());
        return transaction;
    }


    @Transactional
    private StockTransaction outgoingStock(int opCode, long orderID, long productID, long quantity, double price)
            throws HermesException{
        // Проверяем валидность параметров
        if (orderID < 0 || productID < 0 || quantity <= 0 || price < 0) return null;

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

        // Проводим складскую транзакцию в журнале складских транзакций
        StockTransaction transaction = new StockTransaction(orderID, productID, SIDE_OUT, opCode, quantity, price);
        entityManager.persist(transaction);

        // Обновляем складскую карточку
        updateStockBalance(SIDE_OUT, opCode, useCommittedStock, productID, quantity, transaction.getTimestamp());

        return transaction;
    }


    @Transactional
    private StockCard updateStockBalance(int side,
                                         int opCode,
                                         boolean useComittedStock,
                                         long productID,
                                         long quantity,
                                         long timestamp) throws HermesException{

        // Поднимаем складскую карточку товара и блокируем на запись/чтение пока не закончим
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

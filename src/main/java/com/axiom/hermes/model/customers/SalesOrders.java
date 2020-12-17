package com.axiom.hermes.model.customers;

import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.entities.Customer;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;
import com.axiom.hermes.model.inventory.Inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.List;

/**
 * Управление клиентскими заказами
 */
@ApplicationScoped
public class SalesOrders {

    @Inject EntityManager entityManager;

    @Inject Catalogue catalogue;
    @Inject Customers customers;
    @Inject Inventory inventory;

    // todo Нужна настройка - принимать ли заказы где заказ товара больше текущих/доступных остатков

    /**
     * Получить все заказы всех клиентов за указанный период
     * @return список заказов клиентов
     */
    @Transactional
    public List<SalesOrder> getAllOrders(long startTime, long endTime, int status) {
        List<SalesOrder> customerOrders;
        String query = "SELECT a FROM SalesOrder a";
        if (startTime > 0 || endTime > 0) {
            query += " WHERE a.timestamp BETWEEN " + startTime + " AND " + endTime;
        }
        if (status > 0) {
            query += " AND a.status=" + status;
        }
        customerOrders = entityManager.createQuery(query, SalesOrder.class).getResultList();
        return customerOrders;
    }


    /**
     * Получить все заказы клиента с указанным статусом
     * @param customerID клиент
     * @param status статус заказов
     * @return список заказов клиента с указанным статусом
     */
    @Transactional
    public List<SalesOrder> getOrders(int customerID, int status) {
        List<SalesOrder> customerOrders;
        String query;
        if (status==0) {
            query = "SELECT a FROM SalesOrder a WHERE a.customerID=" + customerID;
        } else {
            query = "SELECT a FROM SalesOrder a WHERE a.customerID=" + customerID + " AND a.status=" + status;
        }
        customerOrders = entityManager.createQuery(query, SalesOrder.class).getResultList();
        return customerOrders;
    }


    /**
     * Добавить новый заказ клиента
     * @param customerID клиент
     * @return созданный новый заказ или null если такого клиента нет
     */
    @Transactional
    public SalesOrder addOrder(int customerID) {
        if (customerID < 0) return null;
        Customer customer = customers.getCustomer(customerID);
        if (customer==null) return null;
        SalesOrder salesOrder = new SalesOrder(customerID);
        entityManager.persist(salesOrder);
        return salesOrder;
    }

    /**
     * Получить заказа по ID
     * @param orderID заказа
     * @return найденный заказ или null если не найден
     */
    @Transactional
    public SalesOrder getOrder(long orderID) {
        if (orderID<0) return null;
        return entityManager.find(SalesOrder.class, orderID);
    }

    /**
     * Изменить статус заказа
     * @param orderID заказа
     * @param status новый статус
     * @return обновленный заказ с измененным статусом
     */
    @Transactional
    public SalesOrder changeStatus(long orderID, int status) {
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID, LockModeType.PESSIMISTIC_WRITE);
        if (salesOrder==null) return null;
        if (salesOrder.getStatus()==status) return salesOrder;
        salesOrder.setStatus(status);
        entityManager.persist(salesOrder);

        List<SalesOrderEntry> entries = getOrderEntries(orderID);
        for (SalesOrderEntry entry:entries) {
            inventory.updateCommittedStockInformation(entry.getProductID());
        }
        return salesOrder;
    }

    /**
     * Удалить заказ включая все его позиции
     * @param orderID заказа
     * @return true если удален, false если не найден или его
     * нельзя изменять (статус выше неизменямого или есть связанные транзакции)
     */
    @Transactional
    public boolean removeOrder(long orderID) {
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID, LockModeType.PESSIMISTIC_WRITE);
        if (salesOrder==null) return false;
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE) return false;
        String query = "DELETE FROM SalesOrderEntry a WHERE a.orderID=" + salesOrder.getOrderID();
        entityManager.createQuery(query).executeUpdate();
        entityManager.remove(salesOrder);
        return true;
    }

    //---------------------------------------------------------------------------------------------------

    /**
     * Получить список позиций заказа
     * @param orderID заказа
     * @return список позиций заказа
     */
    @Transactional
    public List<SalesOrderEntry> getOrderEntries(long orderID) {
        List<SalesOrderEntry> orderEntries;
        String query = "SELECT a FROM SalesOrderEntry a WHERE a.orderID=" + orderID;
        orderEntries = entityManager.createQuery(query, SalesOrderEntry.class).getResultList();
        return orderEntries;
    }

    /**
     * Получить позицию заказа
     * @param entryID позиции
     * @return позиция заказа
     */
    @Transactional
    public SalesOrderEntry getOrderEntry(long entryID) {
        if (entryID<0) return null;
        SalesOrderEntry entry = entityManager.find(SalesOrderEntry.class, entryID);
        if (entry==null) return null;
        return entry;
    }

    /**
     * Получить позицию заказа по номеру заказа и коду товарной позиции
     * @param orderID заказа
     * @param productID позиции
     * @return позиция заказа
     */
    @Transactional
    public SalesOrderEntry getOrderEntry(long orderID, int productID) {
        if (orderID<0 || productID<0) return null;
        SalesOrderEntry entry = null;
        String query =
                "SELECT a FROM SalesOrderEntry a " +
                "WHERE a.orderID=" + orderID + " AND a.productID=" + productID;
        try {
            entry = entityManager.createQuery(query, SalesOrderEntry.class).getSingleResult();
        } catch (NoResultException e) {
            // e.printStackTrace();
        }
        return entry;
    }

    /**
     * Добавить позицию заказа
     * @param orderID заказа
     * @param productID товара
     * @param amount количество
     * @return данные позиции заказа
     */
    @Transactional
    public SalesOrderEntry addOrderEntry(long orderID, int productID, int amount) {
        if (orderID < 0 || productID < 0 || amount < 0) return null;
        SalesOrder salesOrder = getOrder(orderID);
        if (salesOrder==null) return null;
        // Если заказ уже изменять нельзя уходим
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE) return null;

        Product product = catalogue.getProduct(productID);
        if (product==null || !product.isAvailable()) return null;

        // Есть ли позиция по такому продукту в этом заказе
        // учитывать случай когда такая позиция уже есть и пытаются добавить еще такую же
        SalesOrderEntry entry = getOrderEntry(orderID, productID);
        if (entry==null) {
            // Если такой товарной позиции нет - создаем
            entry = new SalesOrderEntry(orderID, productID, amount, product.getPrice());
        } else {
            // Если такая товарная позиция есть есть - суммируем количество текущее и новое
            int totalAmount = entry.getAmount() + amount;
            entry.setAmount(totalAmount);
            // Обновляем на текущую цену
            entry.setPrice(product.getPrice());
        }
        entityManager.persist(entry);

        // Обновить временную метку последнего изменения заказа
        salesOrder.setTimestamp(System.currentTimeMillis());
        entityManager.persist(salesOrder);

        return entry;
    }

    /**
     * Изменение данных позиции заказа
     * @param entryID позиции заказа
     * @param newProductID новый код товара
     * @param newAmount новое количество
     * @return обновленная позиция заказа
     */
    @Transactional
    public SalesOrderEntry updateOrderEntry(long entryID, int newProductID, int newAmount) {
        if (newAmount < 0) return null;
        // Если такая позиция не найдена
        SalesOrderEntry managedEntry = entityManager.find(SalesOrderEntry.class, entryID);
        if (managedEntry==null) return null;
        // Если заказ уже изменять нельзя уходим
        SalesOrder salesOrder = getOrder(managedEntry.getOrderID());
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE) return null;

        // Если изменился код товара
        if (managedEntry.getProductID() != newProductID) {
            Product product = catalogue.getProduct(newProductID);
            if (product==null || !product.isAvailable()) return null;
            managedEntry.setProductID(newProductID);
            managedEntry.setPrice(product.getPrice());
        }
        managedEntry.setAmount(newAmount);
        entityManager.persist(managedEntry);

        // Обновить временную метку последнего изменения заказа
        salesOrder.setTimestamp(System.currentTimeMillis());
        entityManager.persist(salesOrder);

        return managedEntry;
    }

    /**
     * Добавляет исполненное количество позиции заказа (используется в Inventory)
     * @param orderID заказа
     * @param productID товарной позиции
     * @param fulfilledAmount исполненное количество
     * @return обновленная позиция заказа
     */
    @Transactional
    public SalesOrderEntry addFulfilledAmount(long orderID, int productID, int fulfilledAmount) {
        if (fulfilledAmount <= 0) return null;
        SalesOrderEntry salesOrderEntry = getOrderEntry(orderID, productID);
        if (salesOrderEntry == null) return null;
        int newFulfilledAmount = salesOrderEntry.getFulfilledAmount() + fulfilledAmount;
        salesOrderEntry.setFulfilledAmount(newFulfilledAmount);
        entityManager.persist(salesOrderEntry);
        return salesOrderEntry;
    }

    /**
     * Вычитает исполненное количество позиции заказа (используется в Inventory)
     * @param orderID заказа
     * @param productID товарной позиции
     * @param fulfilledAmount исполненное количество
     * @return обновленная позиция заказа
     */
    @Transactional
    public SalesOrderEntry subtractFulfilledAmount(long orderID, int productID, int fulfilledAmount) {
        if (fulfilledAmount <= 0) return null;
        SalesOrderEntry salesOrderEntry = getOrderEntry(orderID, productID);
        if (salesOrderEntry == null) return null;
        int newFulfilledAmount = salesOrderEntry.getFulfilledAmount() - fulfilledAmount;
        if (newFulfilledAmount < 0) return null;
        salesOrderEntry.setFulfilledAmount(newFulfilledAmount);
        entityManager.persist(salesOrderEntry);
        return salesOrderEntry;
    }

    /**
     * Удалить позицию заказа
     * @param entryID позиции заказа
     * @return true если удалили, false если не найдена
     */
    @Transactional
    public boolean removeOrderEntry(long entryID) {
        SalesOrderEntry managedEntry = entityManager.find(SalesOrderEntry.class, entryID);
        if (managedEntry==null) return false;
        // Если заказ уже изменять нельзя уходим
        SalesOrder salesOrder = getOrder(managedEntry.getOrderID());
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE) return false;
        entityManager.remove(managedEntry);

        // Обновить временную метку последнего изменения заказа
        salesOrder.setTimestamp(System.currentTimeMillis());
        entityManager.persist(salesOrder);

        return true;
    }

    /**
     * Возвращает количество забронированного заказами товара (принятых обязательств по товару)
     * @param productID товарная позиция
     * @return количество забронированного товара по указанной позиции
     */
    @Transactional
    public long getCommittedAmount(int productID) {
        // Вычисляем Committed Stock (забронированное количество) - это сумма неисполненных
        // позиции с указанным productID подтвержденных, но пока не исполненных заказов
        String sqlQuery = "SELECT SUM(SalesOrderEntry.amount - SalesOrderEntry.fulfilledAmount) " +
                "FROM SalesOrderEntry " +
                "LEFT JOIN SalesOrder ON SalesOrder.orderID=SalesOrderEntry.orderID " +
                "WHERE SalesOrderEntry.productID=" + productID + " " +
                "AND SalesOrder.status >= " + SalesOrder.STATUS_CONFIRMED;
        Object result = entityManager.createNativeQuery(sqlQuery).getSingleResult();

        long committedStock = 0;
        // Если что-то нашли - значит что-то забронировано
        if (result!=null) {
            // Не знаю почему возваращаемый тип SUM при разных условиях разный, но обходим
            if (result instanceof BigInteger) committedStock = ((BigInteger) result).longValue();
            if (result instanceof Long) committedStock = (Long) result;
            if (committedStock < 0) committedStock = 0;
        }

        return committedStock;
    }

}

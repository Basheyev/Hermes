package com.axiom.hermes.model.customers;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.entities.Customer;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderItem;
import com.axiom.hermes.model.inventory.Inventory;
import com.axiom.hermes.common.validation.Validator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.List;

import static com.axiom.hermes.common.exceptions.HermesException.*;

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
        if (startTime > 0 || endTime > 0 || status > 0) {
            query += " WHERE ";
            if (startTime > 0 || endTime > 0) {
                query += "a.timestamp BETWEEN " + startTime + " AND " + endTime;
                if (status > 0) query += " AND a.status=" + status;
            } else {
                query += "a.status=" + status;
            }
        }
        query += " ORDER BY a.timestamp";
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
    public List<SalesOrder> getOrders(long customerID, int status) {
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
    public SalesOrder addOrder(long customerID) throws HermesException {
        if (customerID < 0) return null;  // fixme
        Customer customer = customers.getCustomer(customerID);
        SalesOrder salesOrder = new SalesOrder(customer.getCustomerID());
        entityManager.persist(salesOrder);
        return salesOrder;
    }

    /**
     * Получить заказа по ID
     * @param orderID заказа
     * @return найденный заказ или null если не найден
     */
    @Transactional
    public SalesOrder getOrder(long orderID) throws HermesException {
        // fixme
        if (orderID<0) throw new HermesException(BAD_REQUEST, "Invalid parameter", "OrderID is less than zero");
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID);
        if (salesOrder==null) {
            throw new HermesException(NOT_FOUND, "Sales order not found",
                    "Sales order where orderID=" + orderID + " not found.");
        }
        return salesOrder;
    }

    /**
     * Изменить статус заказа
     * @param orderID заказа
     * @param status новый статус
     * @return обновленный заказ с измененным статусом
     */
    @Transactional
    public SalesOrder changeStatus(long orderID, int status) throws HermesException {
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID, LockModeType.PESSIMISTIC_WRITE);
        if (salesOrder==null)
            throw new HermesException(NOT_FOUND, "Sales order not found",
                    "Sales order where orderID=" + orderID + " not found.");

        if (salesOrder.getStatus()==status) return salesOrder;
        salesOrder.setStatus(status);
        entityManager.persist(salesOrder);

        List<SalesOrderItem> entries = getOrderEntries(orderID);
        for (SalesOrderItem entry:entries) {
            inventory.updateCommittedStock(entry.getProductID());
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
    public void removeOrder(long orderID) throws HermesException {
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID, LockModeType.PESSIMISTIC_WRITE);
        if (salesOrder==null) {
            throw new HermesException(NOT_FOUND, "Sales order not found",
                    "Sales order where orderID=" + orderID + " not found.");
        }
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE) {
            throw new HermesException(FORBIDDEN, "Cannot delete sales order",
                    "Cannot delete sales order, because status=" + salesOrder.getStatus() + " and its not changeable.");
        }
        String query = "DELETE FROM SalesOrderItem a WHERE a.orderID=" + salesOrder.getOrderID();
        entityManager.createQuery(query).executeUpdate();
        entityManager.remove(salesOrder);
    }

    //---------------------------------------------------------------------------------------------------

    /**
     * Получить список позиций заказа
     * @param orderID заказа
     * @return список позиций заказа
     */
    @Transactional
    public List<SalesOrderItem> getOrderEntries(long orderID) {
        List<SalesOrderItem> orderEntries;
        String query = "SELECT a FROM SalesOrderItem a WHERE a.orderID=" + orderID;
        orderEntries = entityManager.createQuery(query, SalesOrderItem.class).getResultList();
        return orderEntries;
    }

    /**
     * Получить позицию заказа
     * @param entryID позиции
     * @return позиция заказа
     */
    @Transactional
    public SalesOrderItem getOrderEntry(long entryID) throws HermesException {

        if (entryID<0) {  // fixme
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "entryID can not be less than 0.");
        }

        SalesOrderItem entry = entityManager.find(SalesOrderItem.class, entryID);

        if (entry==null) {
            throw new HermesException(NOT_FOUND, "Sales order entry not found",
                    "Sales order entry where entryID=" + entryID + " not found.");
        }

        return entry;
    }

    /**
     * Получить позицию заказа по номеру заказа и коду товарной позиции
     * @param orderID заказа
     * @param productID товарной позиции
     * @return позиция заказа
     */
    @Transactional
    public SalesOrderItem getOrderEntry(long orderID, long productID) throws HermesException {
        if (orderID<0 || productID<0) return null;  // fixme
        SalesOrderItem entry;
        String query =
                "SELECT a FROM SalesOrderItem a " +
                "WHERE a.orderID=" + orderID + " AND a.productID=" + productID;
        try {
            entry = entityManager.createQuery(query, SalesOrderItem.class).getSingleResult();
        } catch (NoResultException e) {
            throw new HermesException(NOT_FOUND, "Sales order entry not found",
                    "Sales order entry where orderID=" + orderID + " productID=" + productID + " not found.");
        } catch (PersistenceException e) {
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }
        return entry;
    }

    /**
     * Добавить позицию заказа
     * @param orderID заказа
     * @param productID товара
     * @param quantity количество
     * @return данные позиции заказа
     */
    @Transactional
    public SalesOrderItem addOrderEntry(long orderID, long productID, long quantity) throws HermesException {
        if (orderID < 0 || productID < 0 || quantity < 0) return null;  // fixme
        SalesOrder salesOrder = getOrder(orderID);

        // Если заказ уже изменять нельзя уходим
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE)
            throw new HermesException(FORBIDDEN, "Cannot add sales order entry",
                    "Cannot add sales order entry, because sales order status=" +
                            salesOrder.getStatus() + " and its not changeable.");

        // Если такая товарная позиция недоступна для заказа
        Product product = catalogue.getProduct(productID);
        if (!product.isAvailable()) {
            throw new HermesException(FORBIDDEN, "Cannot add sales order entry",
                    "Product status=" + productID + " is not available.");
        }

        SalesOrderItem entry;
        try {
            // Есть ли позиция по такому продукту в этом заказе
            entry = getOrderEntry(orderID, productID);
            // Если такая позиция заказа есть - суммируем количество текущее и новое
            long totalquantity = entry.getquantity() + quantity;
            entry.setquantity(totalquantity);
            // Обновляем на текущую цену
            entry.setPrice(product.getPrice());
        } catch (HermesException exception) {
            // Если такой позиции заказа нет - создаем новую
            if (exception.getStatus()== NOT_FOUND) {
                entry = new SalesOrderItem(orderID, productID, quantity, product.getPrice());
            } else {
                // Если произошла какая-то другая ошибка
                throw exception;
            }
        }

        // Сохраняем позицию заказа
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
     * @param newQuantity новое количество
     * @return обновленная позиция заказа
     */
    @Transactional
    public SalesOrderItem updateOrderEntry(long entryID, long newProductID, long newQuantity) throws HermesException {
        if (newQuantity < 0) return null; // fixme
        // Если такая позиция не найдена
        SalesOrderItem managedEntry = getOrderEntry(entryID);
        // Если заказ уже изменять нельзя уходим
        SalesOrder salesOrder = getOrder(managedEntry.getOrderID());
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE)
            throw new HermesException(FORBIDDEN, "Cannot update sales order entry",
                    "Cannot update sales order entry, because sales order status=" +
                            salesOrder.getStatus() + " and its not changeable.");

        // Если изменился код товара
        if (managedEntry.getProductID() != newProductID) {
            Product product = catalogue.getProduct(newProductID);
            if (product==null || !product.isAvailable()) return null;
            managedEntry.setProductID(newProductID);
            managedEntry.setPrice(product.getPrice());
        }
        managedEntry.setquantity(newQuantity);
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
     * @param fulfilledQuantity исполненное количество
     * @return обновленная позиция заказа
     */
    @Transactional
    public SalesOrderItem addFulfilledQuantity(long orderID, long productID, long fulfilledQuantity) throws HermesException {
        if (fulfilledQuantity <= 0) return null;
        // Ищем позицию заказа и специально не обрабатываем исключение для передачи дальше
        SalesOrderItem salesOrderItem = getOrderEntry(orderID, productID);
        long newFulfilledQuantity = salesOrderItem.getFulfilledQuantity() + fulfilledQuantity;
        salesOrderItem.setFulfilledQuantity(newFulfilledQuantity);
        entityManager.persist(salesOrderItem);
        return salesOrderItem;
    }

    /**
     * Вычитает исполненное количество позиции заказа если она есть (используется в Inventory)
     * @param orderID заказа
     * @param productID товарной позиции
     * @param fulfilledQuantity исполненное количество
     */
    @Transactional
    public void subtractFulfilledQuantity(long orderID, long productID, long fulfilledQuantity) throws HermesException {
        if (fulfilledQuantity <= 0) return;
        SalesOrderItem salesOrderItem;
        // Ищем позицию заказа если она есть
        try {
            salesOrderItem = getOrderEntry(orderID, productID);
        } catch (HermesException exception) {
            // Если не нашли - ничего не делаем и уходим
            if (exception.getStatus()== NOT_FOUND) return;
            // Если произошло что-то другое - кидаем исключение дальше
            throw exception;
        }
        // Если уже исполнена часть позиции заказа, вычитаем из неё указанное количество товара
        long newFulfilledquantity = salesOrderItem.getFulfilledQuantity() - fulfilledQuantity;
        if (newFulfilledquantity < 0) return;
        salesOrderItem.setFulfilledQuantity(newFulfilledquantity);
        entityManager.persist(salesOrderItem);
    }

    /**
     * Удалить позицию заказа
     * @param entryID позиции заказа
     * @return true если удалили, false если не найдена
     */
    @Transactional
    public boolean removeOrderEntry(long entryID) throws HermesException {
        SalesOrderItem managedEntry = getOrderEntry(entryID);
        // Если заказ уже изменять нельзя уходим
        SalesOrder salesOrder = getOrder(managedEntry.getOrderID());
        if (salesOrder.getStatus() >= SalesOrder.CHANGEABLE_BEFORE)
            throw new HermesException(FORBIDDEN, "Cannot remove sales order entry",
                    "Cannot remove sales order entry, because sales order status=" +
                            salesOrder.getStatus() + " and its not changeable.");

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
    public long getCommittedQuantity(long productID) {
        // Вычисляем Committed Stock (забронированное количество) - это сумма неисполненных
        // позиции с указанным productID подтвержденных, но пока не исполненных заказов
        String sqlQuery = "SELECT SUM(SalesOrderItem.quantity - SalesOrderItem.fulfilledQuantity) " +
                "FROM SalesOrderItem " +
                "LEFT JOIN SalesOrder ON SalesOrder.orderID=SalesOrderItem.orderID " +
                "WHERE SalesOrderItem.productID=" + productID + " " +
                "AND SalesOrder.status >= " + SalesOrder.STATUS_CONFIRMED;
        Object result = entityManager.createNativeQuery(sqlQuery).getSingleResult();

        return Validator.asLong(result);
    }

}

package com.axiom.hermes.model.customers;

import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Управление клиентскими заказами
 */
@ApplicationScoped
public class SalesOrders {

    @Inject EntityManager entityManager;

    @Inject Catalogue catalogue;
    @Inject Customers customers;

    /**
     * Получить все заказы всех клиентов за указанный период
     * @return список заказов клиентов
     */
    @Transactional
    public List<SalesOrder> getAllOrders(long startTime, long endTime, int status) {
        List<SalesOrder> customerOrders;
        String query = "SELECT a FROM SalesOrder a";
        if (startTime > 0 || endTime > 0) {
            query += " WHERE a.timestamp > " + startTime + " AND a.timestamp < " + endTime;
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
        if (customers.getCustomer(customerID)!=null) return null;
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
        salesOrder.setStatus(status);

        // todo вызвать бронирование остатков если выше неизменяемого статуса

        entityManager.persist(salesOrder);
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
        String query = "SELECT a FROM SalesOrderEntry a WHERE a.orderID=" + orderID + "AND a.productID=" + productID;
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
        Product product = catalogue.getProduct(productID);
        if (product==null) return null;

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
        // Если изменился код товара
        if (managedEntry.getProductID() != newProductID) {
            Product product = catalogue.getProduct(newProductID);
            if (product==null) return null;
            managedEntry.setProductID(newProductID);
            managedEntry.setPrice(product.getPrice());
        }
        managedEntry.setAmount(newAmount);
        entityManager.persist(managedEntry);

        // Обновить временную метку последнего изменения заказа
        SalesOrder salesOrder = getOrder(managedEntry.getOrderID());
        salesOrder.setTimestamp(System.currentTimeMillis());
        entityManager.persist(salesOrder);

        return managedEntry;
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
        entityManager.remove(managedEntry);

        // Обновить временную метку последнего изменения заказа
        SalesOrder salesOrder = getOrder(managedEntry.getOrderID());
        salesOrder.setTimestamp(System.currentTimeMillis());
        entityManager.persist(salesOrder);

        return true;
    }



}

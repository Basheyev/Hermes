package com.axiom.hermes.model.customers;

import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.transaction.Transactional;
import java.util.List;

// todo получить постраничный перечень заказов со статусами (фильтры по времени)
// todo прокомментировать код
// todo добавить логирование и обработку исключений

/**
 * Управление заказами
 */
@ApplicationScoped
public class SalesOrders {

    @Inject
    EntityManager entityManager;

    @Inject
    Catalogue catalogue;


    @Transactional
    public List<SalesOrder> getAllOrders() {
        List<SalesOrder> customerOrders;
        String query = "SELECT a FROM SalesOrder a";
        customerOrders = entityManager.createQuery(query, SalesOrder.class).getResultList();
        return customerOrders;
    }


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


    @Transactional
    public SalesOrder addOrder(int customerID) {
        SalesOrder salesOrder = new SalesOrder(customerID);
        entityManager.persist(salesOrder);
        return salesOrder;
    }

    @Transactional
    public SalesOrder getOrder(long orderID) {
        return entityManager.find(SalesOrder.class, orderID);
    }

    @Transactional
    public SalesOrder changeStatus(long orderID, int status) {
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID, LockModeType.PESSIMISTIC_WRITE);
        if (salesOrder==null) return null;
        salesOrder.setStatus(status);
        entityManager.persist(salesOrder);
        return salesOrder;
    }

    @Transactional
    public boolean removeOrder(long orderID) {
        SalesOrder salesOrder = entityManager.find(SalesOrder.class, orderID, LockModeType.PESSIMISTIC_WRITE);
        if (salesOrder==null) return false;
        if (salesOrder.getStatus() != SalesOrder.STATUS_NEW) return false;
        String query = "DELETE FROM SalesOrderEntry a WHERE a.orderID=" + salesOrder.getOrderID();
        entityManager.createQuery(query).executeUpdate();
        entityManager.remove(salesOrder);
        return true;
    }

    //---------------------------------------------------------------------------------------------------

    @Transactional
    public List<SalesOrderEntry> getOrderEntries(long orderID) {
        List<SalesOrderEntry> orderEntries;
        String query = "SELECT a FROM SalesOrderEntry a WHERE a.orderID=" + orderID;
        orderEntries = entityManager.createQuery(query, SalesOrderEntry.class).getResultList();
        return orderEntries;
    }

    @Transactional
    public SalesOrderEntry getOrderEntry(long entryID) {
        SalesOrderEntry entry = entityManager.find(SalesOrderEntry.class, entryID);
        if (entry==null) return null;
        return entry;
    }

    @Transactional
    public SalesOrderEntry addOrderEntry(long orderID, int productID, int amount) {
        SalesOrder salesOrder = getOrder(orderID);
        if (salesOrder==null) return null;
        Product product = catalogue.getProduct(productID);
        if (product==null) return null;
        SalesOrderEntry entry = new SalesOrderEntry(orderID, productID, amount, product.getPrice());
        entityManager.persist(entry);
        return entry;
    }

    @Transactional
    public SalesOrderEntry updateOrderEntry(long entryID, int newProductID, int newAmount) {
        if (newAmount < 0) return null;

        SalesOrderEntry managedEntry = entityManager.find(SalesOrderEntry.class, entryID);
        if (managedEntry==null) return null;
        if (managedEntry.getProductID() != newProductID) {
            Product product = catalogue.getProduct(newProductID);
            if (product==null) return null;
            managedEntry.setProductID(newProductID);
            managedEntry.setPrice(product.getPrice());
        }
        managedEntry.setAmount(newAmount);
        entityManager.persist(managedEntry);

        return managedEntry;
    }

    @Transactional
    public boolean removeOrderEntry(long entryID) {
        SalesOrderEntry entry = entityManager.find(SalesOrderEntry.class, entryID);
        if (entry==null) return false;
        entityManager.remove(entry);
        return true;
    }



}

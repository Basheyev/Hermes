package com.axiom.hermes.model.customers;

import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;

import java.util.List;

// todo реализовать оформление заказа
// TODO Управление заказами
// todo реализовать управление позициями заказа
public class SalesOrders {



    public SalesOrder createOrder(int customerID) {
        return null;
    }

    public boolean changeStatus(long orderID, int status) {

        // todo тут же бронь товара на складе

        return false;
    }

    public boolean deleteOrder(long orderID) {
        return false;
    }

    public List<SalesOrderEntry> getOrderEntries(int orderID) {
        return null;
    }

    public SalesOrderEntry getEntry(long orderID, int productID) {
        return null;
    }

    public SalesOrderEntry addEntry(long orderID, int productID, int amount) {
        return null;
    }

    public SalesOrderEntry updateEntry(SalesOrderEntry newValues) {
        return null;
    }

    public boolean removeEntry(long entryID) {
        return false;
    }



}

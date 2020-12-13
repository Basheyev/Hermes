package com.axiom.hermes.model.customers;

import com.axiom.hermes.model.customers.entities.Customer;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.ws.rs.QueryParam;
import java.math.BigInteger;
import java.util.List;

/**
 * Управление клиентами
 */
@ApplicationScoped
public class Customers {

    @Inject
    EntityManager entityManager;

    @Transactional
    public List<Customer> getAllCustomers() {
        List<Customer> customers;
        String query = "SELECT a FROM Customer a";
        customers = entityManager.createQuery(query, Customer.class).getResultList();
        return customers;
    }

    @Transactional
    public Customer getCustomer(int customerID) {
        if (customerID < 0) return null;
        return entityManager.find(Customer.class, customerID);
    }

    @Transactional
    public Customer getCustomerByMobile(String mobile) {
        if (mobile==null) return null;
        Customer customer;
        try {
            String query = "SELECT a FROM Customer a WHERE a.mobile='" + mobile + "'";
            customer = entityManager.createQuery(query, Customer.class).getSingleResult();
        } catch(NoResultException e){
            customer = null;
        }
        return customer;
    }

    @Transactional
    public Customer addCustomer(Customer customer) {
        if (customer==null) return null;
        if (customer.getMobile()==null) return null;
        if (getCustomerByMobile(customer.getMobile())!=null) return null;
        entityManager.persist(customer);
        return customer;
    }

    @Transactional
    public Customer updateCustomer(Customer customer) {
        if (customer==null) return null;
        Customer managed = entityManager.find(Customer.class, customer.getCustomerID());
        if (managed==null) return null;
        if (customer.getMobile()!=null) managed.setMobile(customer.getMobile());
        if (customer.getBusinessID()!=null) managed.setBusinessID(customer.getBusinessID());
        if (customer.getName()!=null) managed.setName(customer.getName());
        if (customer.getAddress()!=null) managed.setAddress(customer.getAddress());
        if (customer.getCity()!=null) managed.setCity(customer.getCity());
        if (customer.getCountry()!=null) managed.setCountry(customer.getCountry());
        managed.setVerified(customer.isVerified());
        entityManager.persist(managed);
        return customer;
    }

    @Transactional
    public boolean removeCustomer(int customerID) {
        // Ищем такого клиента
        Customer customer = entityManager.find(Customer.class, customerID);
        if (customer==null) return false;
        // Если у клиента есть хотя бы один заказ - удалять нельзя
        String query = "SELECT COUNT(a.customerID) FROM SalesOrder a WHERE a.customerID=" + customerID;
        long ordersCount = ((BigInteger) entityManager.createQuery(query).getSingleResult()).longValue();
        if (ordersCount > 0) return false;
        entityManager.remove(customer);
        return true;
    }

}

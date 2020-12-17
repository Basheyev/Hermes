package com.axiom.hermes.model.customers;

import com.axiom.hermes.model.customers.entities.Customer;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.List;

/**
 * Управление клиентами
 */
@ApplicationScoped
public class Customers {

    @Inject
    EntityManager entityManager;

    /**
     * Получит список всех клиентов
     * @return список карточек клиентов
     */
    @Transactional
    public List<Customer> getAllCustomers() {
        List<Customer> customers;
        String query = "SELECT a FROM Customer a";
        customers = entityManager.createQuery(query, Customer.class).getResultList();
        return customers;
    }

    /**
     * Получить карточку клиента
     * @param customerID карточки клиента
     * @return карточка клиента
     */
    @Transactional
    public Customer getCustomer(long customerID) {
        if (customerID < 0) return null;
        return entityManager.find(Customer.class, customerID);
    }

    /**
     * Получить карточку клиента по мобильному телефону
     * @param mobile мобильный номер
     * @return карточка клиента
     */
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

    /**
     * Добавить нового клиента
     * @param customer карточка клиента
     * @return добавленная карточа клиента
     */
    @Transactional
    public Customer addCustomer(Customer customer) {
        if (customer==null) return null;
        if (customer.getMobile()==null) return null;
        if (getCustomerByMobile(customer.getMobile())!=null) return null;
        entityManager.persist(customer);
        return customer;
    }

    /**
     * Изменить данные карточки клиента
     * @param customer карточка клиента с измененными полями
     * @return измененная карточка клиента или null если карточка клиента не найдена
     */
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

    /**
     * Удалить карточку клиента если у него нет заказов
     * @param customerID карточки клиента
     * @return true если удален, false если не найден или есть заказы с его участием
     */
    @Transactional
    public boolean removeCustomer(long customerID) {
        // Ищем такого клиента
        Customer customer = entityManager.find(Customer.class, customerID);
        if (customer==null) return false;
        // Если у клиента есть хотя бы один заказ - удалять нельзя
        String query = "SELECT COUNT(a.customerID) FROM SalesOrder a WHERE a.customerID=" + customerID;
        long ordersCount = (Long) entityManager.createQuery(query).getSingleResult();
        if (ordersCount > 0) return false;
        entityManager.remove(customer);
        return true;
    }

}

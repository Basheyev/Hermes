package com.axiom.hermes.model.customers;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.common.validation.Validator;
import com.axiom.hermes.model.customers.entities.Customer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.util.List;

import static com.axiom.hermes.common.exceptions.HermesException.*;

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
    public Customer getCustomer(long customerID) throws HermesException{
        Validator.nonNegativeInteger("customerID", customerID);

        Customer customer = entityManager.find(Customer.class, customerID);
        if (customer==null) {
            throw new HermesException(
                    NOT_FOUND, "Customer not found",
                    "Customer where customerID=" + customerID + " not found.");
        }
        return customer;
    }

    /**
     * Получить карточку клиента по мобильному телефону
     * @param mobile мобильный номер
     * @return карточка клиента
     */
    @Transactional
    public Customer getCustomerByMobile(String mobile) throws HermesException {
        mobile = Validator.validateMobile(mobile);
        try {
            String query = "SELECT a FROM Customer a WHERE a.mobile='" + mobile + "'";
            return entityManager.createQuery(query, Customer.class).getSingleResult();
        } catch (NoResultException e){
            throw new HermesException(NOT_FOUND, "Customer not found",
                    "Customer where mobile='" + mobile + "' not found.");
        } catch (PersistenceException e) {
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }
    }

    /**
     * Добавить нового клиента с уникальным мобильным телефоном
     * @param customer карточка клиента
     * @return добавленная карточа клиента
     */
    @Transactional
    public Customer addCustomer(Customer customer) throws HermesException {
        String mobile = customer.getMobile();
        mobile = Validator.validateMobile(mobile);
        customer.setMobile(mobile);
        try {
            // Ищем клиента с таким же мобильным номером
            Customer found = getCustomerByMobile(customer.getMobile());
            // Если нашли - добавлять клиента нельзя
            throw new HermesException(FORBIDDEN, "Cannot add customer",
                    "Customer with the same mobile already exist, its customerID=" + found.getCustomerID());
        } catch (HermesException exception) {
            // Если не нашли - добавляем
            if (exception.getStatus()== NOT_FOUND) {
                entityManager.persist(customer);
                return customer;
            } else throw exception;
        }
    }

    /**
     * Изменить данные карточки клиента
     * @param customer карточка клиента с измененными полями
     * @return измененная карточка клиента или null если карточка клиента не найдена
     */
    @Transactional
    public Customer updateCustomer(Customer customer) throws HermesException {
        Customer managed = getCustomer(customer.getCustomerID());
        if (customer.getMobile()!=null) {
            String mobile = customer.getMobile();
            mobile = Validator.validateMobile(mobile);
            managed.setMobile(mobile);
        }
        if (customer.getBusinessID()!=null) {
            Validator.validateBusinessID(customer.getBusinessID());
            managed.setBusinessID(customer.getBusinessID());
        }
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
     */
    @Transactional
    public void removeCustomer(long customerID) throws HermesException {
        Validator.nonNegativeInteger("customerID", customerID);
        // Ищем такого клиента
        Customer customer = getCustomer(customerID);
        // Если у клиента есть хотя бы один заказ - удалять нельзя
        String query = "SELECT COUNT(a.customerID) FROM SalesOrder a WHERE a.customerID=" + customerID;
        long ordersCount = (Long) entityManager.createQuery(query).getSingleResult();
        if (ordersCount > 0) {
            throw new HermesException(FORBIDDEN, "Customer cannot be deleted",
                    "CustomerID=" + customerID + " cannot be deleted because mentioned in Orders");
        }
        entityManager.remove(customer);
    }

}

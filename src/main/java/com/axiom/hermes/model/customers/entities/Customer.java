package com.axiom.hermes.model.customers.entities;

import javax.persistence.*;

/**
 * Карточка клиента
 */
@Entity
public class Customer {
    @Id @GeneratedValue
    private int customerID;                    // ID клиента
    // todo добавить индекс
    private String mobile;                     // Мобильный номер
    // todo добавить индекс
    private String businessID;                 // БИН/ИИН клиента
    private String name;                       // Имя клиента (ФИО/ИП/ТОО)
    private String address;                    // Адрес клиента
    private String city;                       // Город
    private String country;                    // Страна
    private boolean verified;                  // Проверенный ли клиент

    public Customer() {}

    public int getCustomerID() {
        return customerID;
    }

    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getBusinessID() {
        return businessID;
    }

    public void setBusinessID(String businessID) {
        this.businessID = businessID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}

package com.axiom.hermes.model.customers.entities;

/**
 * Карточка клиента
 * todo реализовать управление клиентами
 */
public class Customer {
    public int customerID;                    // ID клиента
    public String businessID;                 // БИН/ИИН клиента
    public String mobile;                     // Мобильный номер
    public String name;                       // Имя клиента
    public String description;                // Описание клиента
    public String address;                    // Адрес клиента
    public double personalAccount;            // Лицевой счёт клиента
    public byte[] photo;                      // Фотография клиента
    // tax??
}

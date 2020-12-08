package com.axiom.hermes.model.users;

/**
 * Пользователь
 */
public class User {
    public int userID;            // ID пользователя
    public int roles;             // Роль (0 - клиент, 1 - дистрибьютор)
    public int customerID;        // Код клиента, если это клиент
    public String name;           // Имя пользователя
    public String mobile;         // Номер мобильного телефона
    public boolean active;        // Активный ли пользователь
}

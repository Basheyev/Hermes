package com.axiom.hermes.security;

/**
 * Пользователь
 */
public class User {
    public long userID;           // ID пользователя
    public long roles;            // Роль (0 - клиент, 1 - дистрибьютор)
    public long customerID;       // Код клиента, если это клиент
    public String name;           // Имя пользователя
    public String mobile;         // Номер мобильного телефона
    public boolean active;        // Активный ли пользователь
}

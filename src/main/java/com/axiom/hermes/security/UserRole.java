package com.axiom.hermes.security;

/**
 * Роль пользователя
 */
public class UserRole {
    //------------------------------------------------------------------------
    public static final int ROLE_CUSTOMER = 0;
    public static final int ROLE_DISTRIBUTOR = 1;
    //------------------------------------------------------------------------
    public static final int ACCESS_CATALOGUE = 1;        // 0000001
    public static final int ACCESS_INVENTORY = 2;        // 0000010
    public static final int ACCESS_CUSTOMERS = 4;        // 0000100
    public static final int ACCESS_ALL = 0xFFFFFFFF;     // 1111111
    //------------------------------------------------------------------------
    public long roleID;
    public long rights;

}

package com.axiom.hermes.model.inventory;

/**
 * Складская карточка по товарной позиции
 */
public class SKUInformation {
    public int productID;                 // Код товара
    public long stockOnHand;              // Всего товара в наличии
    public long commitedStock;            // Объем принятого, но не исполненного заказа на товар
    public long availableForSale;         // Всего доступного для продажи товара
    public long reorderPoint;             // Минимальные остатки
}

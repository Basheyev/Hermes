package com.axiom.hermes.model.inventory.entities;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Складская карточка по товарной позиции
 */
@Entity
public class StockInformation {
    @Id
    private int productID;                 // Код товара
    private long stockOnHand;              // Всего товара в наличии
    private long committedStock;           // Объем принятого, но не исполненного заказа на товар
    private long availableForSale;         // Всего доступного для продажи товара
    private long reorderPoint;             // Минимальные остатки

    public StockInformation(int productID) {
        this.productID = productID;
        this.stockOnHand = 0;
        this.committedStock = 0;
        this.availableForSale = 0;
        this.reorderPoint = 0;
    }

    public int getProductID() {
        return productID;
    }

    public void setProductID(int productID) {
        this.productID = productID;
    }

    public long getStockOnHand() {
        return stockOnHand;
    }

    public void setStockOnHand(long stockOnHand) {
        this.stockOnHand = stockOnHand;
    }

    public long getCommittedStock() {
        return committedStock;
    }

    public void setCommittedStock(long committedStock) {
        this.committedStock = committedStock;
    }

    public long getAvailableForSale() {
        return availableForSale;
    }

    public void setAvailableForSale(long availableForSale) {
        this.availableForSale = availableForSale;
    }

    public long getReorderPoint() {
        return reorderPoint;
    }

    public void setReorderPoint(long reorderPoint) {
        this.reorderPoint = reorderPoint;
    }
}

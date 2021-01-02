package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.*;

/**
 * Товар в коллекции
 */
@Entity
@Table(indexes = {
    @Index(columnList = "collectionID"),
    @Index(columnList = "productID"),
    @Index(name = "CollectionItemsIndex", columnList = "collectionID, productID"),
})
public class CollectionItem {

    @Id @GeneratedValue
    private long itemID;
    private long collectionID;
    private long productID;
    private int orderNumber;

    public CollectionItem() {}

    public CollectionItem(long collectionID, long productID) {
        this.collectionID = collectionID;
        this.productID = productID;
    }

    public long getCollectionID() {
        return collectionID;
    }

    public void setCollectionID(long collectionID) {
        this.collectionID = collectionID;
    }

    public long getProductID() {
        return productID;
    }

    public void setProductID(long productID) {
        this.productID = productID;
    }
}

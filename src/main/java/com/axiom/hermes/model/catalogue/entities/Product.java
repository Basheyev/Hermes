package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * Карточка товарной позиции
 * Примечание: дополнительные аттрибуты в ProductAttributes
 */
@Entity
public class Product {
    @Id
    @GeneratedValue
    public int productID;                 // ID товара
    public int categoryID;                // ID категории товара
    public String vendorCode;             // Артикул товара
    public String name;                   // Наименование товара
    public String description;            // Описание товара
    public String unitOfMeasure;          // Единица измерения
    public double price;                  // Цена отпуска товара
    public boolean available;             // Доступно ли клиентам для выбора
    public long lastModification;         // Дата и время последнего изменения

    public Product() { }

    public Product(String vendorCode, String name, String description, double price) {
        this.vendorCode = vendorCode;
        this.categoryID = 0;
        this.name = name;
        this.description = description;
        this.price = price;
    }



}

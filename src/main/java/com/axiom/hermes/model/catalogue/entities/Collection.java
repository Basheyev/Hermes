package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Коллекция товаров
 */
@Entity
public class Collection {
    @Id  @GeneratedValue
    public long collectionID;           // Код категории товара
    public String name;                 // Наименование категории товара
    public String description;          // Описание категории товара
    public int sortOrder;               // Порядок сортировки
}

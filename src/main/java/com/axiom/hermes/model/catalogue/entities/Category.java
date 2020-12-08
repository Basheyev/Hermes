package com.axiom.hermes.model.catalogue.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Категория товара
 */
@Entity
public class Category {
    @Id  @GeneratedValue
    public int categoryID;              // Код категории товара
    public int parentID;                // Код родительской категории
    public String name;                 // Наименование категории товара
    public String description;          // Описание категории товара
    public int sortOrder;               // Порядок сортировки
}

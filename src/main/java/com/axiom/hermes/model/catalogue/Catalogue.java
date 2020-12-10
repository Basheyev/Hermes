package com.axiom.hermes.model.catalogue;

import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.catalogue.entities.ProductImage;
import com.axiom.hermes.model.inventory.Inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;

/**
 * Каталог товаров
 */
@ApplicationScoped
public class Catalogue {

    @Inject
    EntityManager entityManager;

    @Inject
    Inventory inventory;

    public Catalogue() { }

    /**
     * Возвращает весь список доступных для заказа позиций каталога товаров
     * @return список карточек товарных позиций
     */
    @Transactional
    public List<Product> getAvailableProducts() {
        List<Product> availableProducts;
        String query = "SELECT a FROM Product a WHERE a.available=TRUE";
        availableProducts = entityManager.createQuery(query, Product.class).getResultList();
        return availableProducts;
    }

    /**
     * Возвращает весь список позиций каталога товаров (включая недоступные для заказа)
     * @return список карточек товарных позиций
     */
    @Transactional
    public List<Product> getAllProducts() {
        List<Product> allProducts;
        String query = "SELECT a FROM Product a";
        allProducts = entityManager.createQuery(query, Product.class).getResultList();
        return allProducts;
    }


    /**
     * Возвращает карточку товарной позиции по указаному ID
     * @param id товарной позиции
     * @return карточка товарной позиции
     */
    @Transactional
    public Product getProduct(int id) {
        String query = "SELECT a FROM Product a WHERE a.productID=" + id;
        List<Product> result = entityManager.createQuery(query, Product.class).getResultList();
        if (result.size() > 0) return result.get(0);
        return null;
    }

    /**
     * Добавляет новую карточку товарной позиции
     * @param product новая карточка товарной позиации
     * @return сохраненная карточка товарной позиции или null если такой ID уже есть
     */
    @Transactional
    public Product addProduct(Product product) {
        if (product.productID != 0) return null;
        product.setAvailable(true);
        product.setTimestamp(System.currentTimeMillis());
        entityManager.persist(product);
        inventory.createStockKeepingUnit(product.productID);
        return product;
    }

    /**
     * Обновляет карточку товарной позиации
     * @param product карточка товарной позиции
     * @return обновляет карточку товарной позиации или создает новую
     */
    @Transactional
    public Product updateProduct(Product product) {
        Product managedEntity = entityManager.find(Product.class, product.productID);
        if (managedEntity==null) return null;
        managedEntity.setCategoryID(product.getCategoryID());
        managedEntity.setVendorCode(product.getVendorCode());
        managedEntity.setName(product.getName());
        managedEntity.setDescription(product.getDescription());
        managedEntity.setPrice(product.getPrice());
        managedEntity.setUnitOfMeasure(product.getUnitOfMeasure());
        managedEntity.setAvailable(product.isAvailable());
        managedEntity.setTimestamp(System.currentTimeMillis());
        entityManager.persist(managedEntity);
        return product;
    }

    /**
     * Загружает изображение товарной позиции
     * @param productImage изображение товарной позиции
     * @return true - значит успешно, false - если нет
     */
    @Transactional
    public boolean uploadImage(ProductImage productImage) {
        Product product = getProduct(productImage.productID);
        if (product==null) return false;
        ProductImage managedEntity = entityManager.find(ProductImage.class, productImage.productID);
        if (managedEntity==null) {
            // Если у товарной позиции небыло изображения - добавляем
            entityManager.persist(productImage);
        } else {
            // Если у товарной позиции было изображение - обновляем
            managedEntity.setFilename(productImage.getFilename());
            managedEntity.setImage(productImage.getImage());
            managedEntity.setThumbnail(productImage.getThumbnail());
            managedEntity.setTimestamp(System.currentTimeMillis());
            entityManager.persist(managedEntity);
        }
        return true;
    }

    /**
     * Загружает миниатюрное изображение товарной позиции вписанное в 128x128 (до 10Kb)
     * @param productID товарной позиации
     * @return JPEG с миниатюрным изображением
     */
    @Transactional
    public byte[] getProductThumbnail(int productID) {
        try {
            String query = "SELECT a.thumbnail FROM ProductImage a WHERE a.productID=" + productID;
            return entityManager.createQuery(query, byte[].class).getSingleResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Transactional
    public ProductImage getProductImage(int productID) {
        try {
            String query = "SELECT a FROM ProductImage a WHERE a.productID=" + productID;
            return entityManager.createQuery(query, ProductImage.class).getSingleResult();
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return null;
    }


    //----------------------------------------------------------------------------------------------------

    @Transactional
    public void generateProducts() {
        addProduct(new Product("JNLV0", "Jeans", "LEVI", 20));
        addProduct(new Product("SHMX1", "Shirt", "MAXMARA", 10));
        addProduct(new Product("HTAY2", "Hat", "Ayoka", 15));
        addProduct(new Product("DRGC3","Dress", "GUCCI", 20));
        addProduct(new Product("GLSP4", "Glasses", "SAPA", 10));
        addProduct(new Product("SHAY5","Shoes", "Ayoka", 15));
    }

    @Transactional
    public void resetProducts() {
        entityManager.createQuery("DELETE FROM Product").executeUpdate();
    }

}

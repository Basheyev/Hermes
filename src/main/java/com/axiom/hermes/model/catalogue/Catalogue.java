package com.axiom.hermes.model.catalogue;

import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.catalogue.entities.ProductImage;
import com.axiom.hermes.model.inventory.Inventory;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.List;

/**
 * Каталог товаров
 * todo передавать изображение потоково, не загружая в память
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
     * @param productID товарной позиции
     * @return карточка товарной позиции
     */
    @Transactional
    public Product getProduct(int productID) {
        Product product = entityManager.find(Product.class, productID);
        if (product==null) return null;
        return product;
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
        if (product.getPrice() < 0) return null;
        if (product.getCategoryID() < 0) return null;
        // Применять только те значения, которые были указаны (чтобы не затереть имеющиеся)
        if (product.getVendorCode()!=null) managedEntity.setVendorCode(product.getVendorCode());
        if (product.getName()!=null) managedEntity.setName(product.getName());
        if (product.getDescription()!=null) managedEntity.setDescription(product.getDescription());
        if (product.getUnitOfMeasure()!=null) managedEntity.setUnitOfMeasure(product.getUnitOfMeasure());
        // Так как сложно тут отследить не указано или указали нулевое значение - просто присваиваем
        managedEntity.setCategoryID(product.getCategoryID());
        managedEntity.setPrice(product.getPrice());
        managedEntity.setAvailable(product.isAvailable());
        managedEntity.setTimestamp(System.currentTimeMillis());
        entityManager.persist(managedEntity);
        return product;
    }

    /**
     * Удаляет товарную позицию если с ней не связано заказов и транзакций
     * @param productID товарной позиции
     * @return true если удалена, false если не удалена (нет такого продукта или товарная позиция используется)
     */
    @Transactional
    public boolean removeProduct(int productID) {
        Product product = entityManager.find(Product.class, productID);
        if (product==null) return false;
        // Если хоть где-то используется в заказах или транзакциях нельзя удалять
        String sqlQuery = "SELECT " +
                "(SELECT COUNT(*) FROM SalesOrderEntry WHERE SalesOrderEntry.productID=" + productID + ") +" +
                "(SELECT COUNT(*) FROM StockTransaction WHERE StockTransaction.productID=" + productID + ")";
        BigInteger usageCount = (BigInteger) entityManager.createNativeQuery(sqlQuery).getSingleResult();
        if (usageCount.longValue() > 0) return false;
        // Если нигде не используется тогда удаляем связанные данные: изображения и складские карточки
        entityManager.createQuery("DELETE FROM ProductImage a WHERE a.productID=" + productID).executeUpdate();
        entityManager.createQuery("DELETE FROM StockInformation a WHERE a.productID=" + productID).executeUpdate();
        entityManager.remove(product);
        return true;
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
            // e.printStackTrace();
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

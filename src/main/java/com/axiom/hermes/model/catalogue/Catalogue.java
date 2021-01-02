package com.axiom.hermes.model.catalogue;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.common.validation.Validator;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.catalogue.entities.ProductImage;
import com.axiom.hermes.model.inventory.Inventory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import java.util.List;

import static com.axiom.hermes.common.exceptions.HermesException.*;

// TODO Добавить управление коллекциями

/**
 * Каталог товаров
 */
@ApplicationScoped
public class Catalogue {

    @Inject EntityManager entityManager;
    @Inject TransactionManager transactionManager;

    @Inject Inventory inventory;

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
    public Product getProduct(long productID) throws HermesException {
        Validator.nonNegativeInteger("productID", productID);
        Product product = entityManager.find(Product.class, productID);
        if (product==null) {
            throw new HermesException(
                    NOT_FOUND, "Product not found",
                    "Requested productID=" + productID + " not found.");
        }
        return product;
    }

    /**
     * Добавляет новую карточку товарной позиции
     * @param product новая карточка товарной позиации
     * @return сохраненная карточка товарной позиции или null если такой ID уже есть
     */
    @Transactional
    public Product addProduct(Product product) throws HermesException {
        if (product.productID != 0) {
            throw new HermesException(
                    BAD_REQUEST, "Invalid parameter",
                    "ProductID is not zero. Do not specify productID when creating new Product.");
        }

        Validator.nonNegativeNumber("unitPrice", product.getUnitPrice());
        Validator.validateName(product.getName());
        Validator.validateVendorCode(product.getVendorCode());

        product.setTimestamp(System.currentTimeMillis());
        try {
            entityManager.persist(product);
            inventory.createStockCard(product.productID);
        } catch (Exception exception) {
            try {
                transactionManager.setRollbackOnly();
            } catch (IllegalStateException | SystemException e) {
                e.printStackTrace();
            }
            throw exception;
        }
        return product;
    }

    /**
     * Обновляет карточку товарной позиации
     * @param product карточка товарной позиции
     * @return обновляет карточку товарной позиации или создает новую
     */
    @Transactional
    public Product updateProduct(Product product) throws HermesException {
        Validator.nonNegativeInteger("productID", product.getProductID());
        Validator.nonNegativeNumber("unitPrice", product.getUnitPrice());

        Product managedEntity = getProduct(product.productID);
        if (managedEntity==null) return null;

        // Применять только те значения, которые были указаны (чтобы не затереть имеющиеся)
        if (product.getVendorCode()!=null) {
            Validator.validateVendorCode(product.getVendorCode());
            managedEntity.setVendorCode(product.getVendorCode());
        }
        if (product.getName()!=null) {
            Validator.validateName(product.getName());
            managedEntity.setName(product.getName());
        }
        if (product.getDescription()!=null) managedEntity.setDescription(product.getDescription());

        // Так как сложно тут отследить не указано или указали нулевое значение - просто присваиваем
        managedEntity.setUnitPrice(product.getUnitPrice());
        managedEntity.setAvailable(product.isAvailable());
        managedEntity.setTimestamp(System.currentTimeMillis());
        entityManager.persist(managedEntity);

        return product;
    }

    /**
     * Удаляет товарную позицию если с ней не связано заказов и транзакций
     * @param productID товарной позиции
     */
    @Transactional
    public void removeProduct(long productID) throws HermesException {
        Validator.nonNegativeInteger("productID", productID);

        Product product = getProduct(productID);

        // Если хоть где-то используется в заказах или транзакциях нельзя удалять
        String sqlQuery = "SELECT " +
                "(SELECT COUNT(*) FROM SalesOrderItem WHERE SalesOrderItem.productID=" + productID + ") +" +
                "(SELECT COUNT(*) FROM StockTransaction WHERE StockTransaction.productID=" + productID + ")";

        long usageCount = Validator.asLong(entityManager.createNativeQuery(sqlQuery).getSingleResult());

        // Если продукт используется в заказах или транзакциях - удалять нельзя
        if (usageCount > 0) {
            throw new HermesException(FORBIDDEN, "Product cannot be deleted",
                    "ProductID=" + productID + " cannot be deleted because used in Orders or Transactions");
        }

        // Если нигде не используется тогда удаляем связанные данные: изображения и складские карточки
        try {
            entityManager.createQuery("DELETE FROM ProductImage a WHERE a.productID=" + productID).executeUpdate();
            entityManager.createQuery("DELETE FROM StockCard a WHERE a.productID=" + productID).executeUpdate();
            entityManager.remove(product);
        } catch (Exception exception) {
            try {
                transactionManager.setRollbackOnly();
            } catch (IllegalStateException | SystemException e) {
                e.printStackTrace();
            }
            throw exception;
        }
    }

    /**
     * Загружает изображение товарной позиции
     * @param productImage изображение товарной позиции
     */
    @Transactional
    public void uploadImage(ProductImage productImage) throws HermesException {
        Validator.nonNegativeInteger("productID", productImage.getProductID());

        // Проверяем есть ли такая товарная позиция в каталоге
        Product product = getProduct(productImage.productID);
        ProductImage managedEntity = entityManager.find(ProductImage.class, product.productID);
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
    }

    /**
     * Загружает миниатюрное изображение товарной позиции вписанное в 128x128 (до 10Kb)
     * @param productID товарной позиации
     * @return JPEG с миниатюрным изображением
     */
    @Transactional
    public byte[] getProductThumbnail(long productID) throws HermesException {
        Validator.nonNegativeInteger("productID", productID);
        try {
            String query = "SELECT a.thumbnail FROM ProductImage a WHERE a.productID=" + productID;
            return entityManager.createQuery(query, byte[].class).getSingleResult();
        } catch (NoResultException e) {
            throw new HermesException(
                    NOT_FOUND, "Product Thumbnail image not found",
                    "Requested productID=" + productID + " Thumbnail image not found.");
        }
    }

    @Transactional
    public ProductImage getProductImage(long productID) throws HermesException {
        Validator.nonNegativeInteger("productID", productID);
        try {
            String query = "SELECT a FROM ProductImage a WHERE a.productID=" + productID;
            return entityManager.createQuery(query, ProductImage.class).getSingleResult();
        } catch (NoResultException e) {
            throw new HermesException(
                    NOT_FOUND, "Product image not found",
                    "Requested productID=" + productID + " image not found.");
        }
    }



}

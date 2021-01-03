package com.axiom.hermes.model.catalogue;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.common.validation.Validator;
import com.axiom.hermes.model.catalogue.entities.Collection;
import com.axiom.hermes.model.catalogue.entities.CollectionItem;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.catalogue.entities.ProductImage;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderItem;
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


/**
 * Каталог товаров
 */
@ApplicationScoped
public class Catalogue {

    @Inject EntityManager entityManager;
    @Inject TransactionManager transactionManager;

    @Inject Inventory inventory;

    public Catalogue() { }

    //--------------------------------------------------------------------------------------------------------
    // Управление товарами
    //--------------------------------------------------------------------------------------------------------

    /**
     * Возвращает весь список доступных для заказа позиций каталога товаров
     * @return список карточек товарных позиций
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<Product> getAvailableProducts() throws HermesException {
        List<Product> availableProducts;
        String query = "SELECT a FROM Product a WHERE a.available=TRUE";
        try {
            availableProducts = entityManager.createQuery(query, Product.class).getResultList();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }
        return availableProducts;
    }

    /**
     * Возвращает весь список позиций каталога товаров (включая недоступные для заказа)
     * @return список карточек товарных позиций
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<Product> getAllProducts() throws HermesException {
        List<Product> allProducts;
        String query = "SELECT a FROM Product a";
        try {
            allProducts = entityManager.createQuery(query, Product.class).getResultList();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }
        return allProducts;
    }

    /**
     * Возвращает карточку товарной позиции по указаному ID
     * @param productID товарной позиции
     * @return карточка товарной позиции
     * @throws HermesException информация об ошибке
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
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public Product addProduct(Product product) throws HermesException {
        if (product.getProductID() != 0) {
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
            inventory.createStockCard(product.getProductID());
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
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public Product updateProduct(Product product) throws HermesException {
        Validator.nonNegativeInteger("productID", product.getProductID());
        Validator.nonNegativeNumber("unitPrice", product.getUnitPrice());

        Product managedEntity = getProduct(product.getProductID());

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

        try {
            entityManager.persist(managedEntity);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", exception.getMessage());
        }
        return product;
    }

    /**
     * Удаляет товарную позицию если с ней не связано заказов и транзакций
     * @param productID товарной позиции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public void removeProduct(long productID) throws HermesException {
        Validator.nonNegativeInteger("productID", productID);

        Product product = getProduct(productID);

        // Если хоть где-то используется в заказах или транзакциях нельзя удалять
        String sqlQuery = "SELECT " +
                "(SELECT COUNT(*) FROM SalesOrderItem WHERE SalesOrderItem.productID=" + productID + ") +" +
                "(SELECT COUNT(*) FROM StockTransaction WHERE StockTransaction.productID=" + productID + ")";

        long usageCount;
        try {
            usageCount = Validator.asLong(entityManager.createNativeQuery(sqlQuery).getSingleResult());
        } catch (Exception e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }

        // Если продукт используется в заказах или транзакциях - удалять нельзя
        if (usageCount > 0) {
            throw new HermesException(FORBIDDEN, "Product cannot be deleted",
                    "ProductID=" + productID + " cannot be deleted because used in Orders or Transactions");
        }

        // Если нигде не используется тогда удаляем связанные данные: изображения и складские карточки
        try {
            entityManager.createQuery("DELETE FROM ProductImage a WHERE a.productID=" + productID).executeUpdate();
            entityManager.createQuery("DELETE FROM StockCard a WHERE a.productID=" + productID).executeUpdate();
            entityManager.createQuery("DELETE FROM CollectionItem a WHERE a.productID=" + productID).executeUpdate();
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
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public void uploadImage(ProductImage productImage) throws HermesException {
        Validator.nonNegativeInteger("productID", productImage.getProductID());

        // Проверяем есть ли такая товарная позиция в каталоге
        Product product = getProduct(productImage.getProductID());
        ProductImage managedEntity = entityManager.find(ProductImage.class, product.getProductID());
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
     * @throws HermesException информация об ошибке
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

    /**
     * Возвращает изображение товара
     * @param productID товара
     * @return изображение товара
     * @throws HermesException информация об ошибке
     */
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

    //--------------------------------------------------------------------------------------------------------
    // Управление коллекциями
    //--------------------------------------------------------------------------------------------------------

    /**
     * Возвращает список всех коллекций
     * @return список всех коллекций
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<Collection> getCollections() throws HermesException {
        List<Collection> allCollections;
        String query = "SELECT a FROM Collection a";
        try {
            allCollections = entityManager.createQuery(query, Collection.class).getResultList();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }
        return allCollections;
    }

    /**
     * Получить карточку коллекции товаров
     * @param collectionID коллекции
     * @return карточка коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public Collection getCollection(long collectionID) throws HermesException {
        Validator.nonNegativeInteger("collectionID", collectionID);
        Collection collection = entityManager.find(Collection.class, collectionID);
        if (collection==null) {
            throw new HermesException(
                    NOT_FOUND, "Collection not found",
                    "Requested collectionID=" + collectionID + " not found.");
        }
        return collection;
    }

    /**
     * Добавляет новую карточку коллекцию
     * @param collection карточка коллекции
     * @return сохраненная карточка коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public Collection addCollection(Collection collection) throws HermesException {
        if (collection.getCollectionID() != 0) {
            throw new HermesException(
                    BAD_REQUEST, "Invalid parameter",
                    "CollectionID is not zero. Do not specify collectionID when creating new Collection.");
        }

        Validator.validateName(collection.getName());

        try {
            entityManager.persist(collection);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", exception.getMessage());
        }
        return collection;
    }

    /**
     * Обновляет карточку коллекции
     * @param collection измененная карточка коллекции
     * @return обновленная карточка коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public Collection updateCollection(Collection collection) throws HermesException {
        Validator.nonNegativeInteger("collectionID", collection.getCollectionID());
        Validator.validateName(collection.getName());

        Collection managedEntity = getCollection(collection.getCollectionID());

        managedEntity.setName(collection.getName());
        managedEntity.setDescription(collection.getDescription());
        managedEntity.setThumbnail(collection.getThumbnail());
        managedEntity.setSortOrder(collection.getSortOrder());
        managedEntity.setAvailable(collection.isAvailable());
        managedEntity.setTimestamp(System.currentTimeMillis());

        try {
            entityManager.persist(managedEntity);
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", exception.getMessage());
        }

        return managedEntity;
    }

    /**
     * Удаляет коллекцию товаров: карточку и ссылки на товары
     * @param collectionID коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public void removeCollection(long collectionID) throws HermesException {
        Validator.nonNegativeInteger("collectionID", collectionID);

        Collection managedEntity = getCollection(collectionID);

        try {
            entityManager.createQuery("DELETE FROM CollectionItem a WHERE a.collectionID=" + collectionID).executeUpdate();
            entityManager.remove(managedEntity);
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
     * Получить список всех товаров в коллекции
     * @param collectionID коллекции
     * @return список товаров коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public List<CollectionItem> getCollectionItems(long collectionID) throws HermesException {
        Validator.nonNegativeInteger("collectionID", collectionID);
        List<CollectionItem> collectionItems;
        String query = "SELECT a FROM CollectionItem a WHERE a.collectionID=" + collectionID;
        try {
            collectionItems = entityManager.createQuery(query, CollectionItem.class).getResultList();
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", exception.getMessage());
        }
        return collectionItems;
    }

    /**
     * Добавляет товарную позицию в коллекцию
     * @param item товарная позиция коллекции
     * @return добавленная товарная позиция в коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public CollectionItem addCollectionItem(CollectionItem item) throws HermesException {
        Validator.nonNegativeInteger("collectionID", item.getCollectionID());
        Validator.nonNegativeInteger("productID", item.getProductID());

        // Проверяем наличие такой коллекции и такого товара
        Collection collection = getCollection(item.getCollectionID());
        Product product = getProduct(item.getProductID());

        // Проверяем нет ли уже в коллекции такого товара
        long occurences;
        try {
            String sqlQuery = "SELECT COUNT(*) FROM CollectionItem " +
                    "WHERE CollectionItem.collectionID=" + collection.getCollectionID() + " AND " +
                    "CollectionItem.productID=" + product.getProductID();
            occurences = Validator.asLong(entityManager.createNativeQuery(sqlQuery).getSingleResult());
        } catch (Exception e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }

        // Если есть - уходим и указываем на ошибку
        if (occurences > 0) {
            throw new HermesException(FORBIDDEN, "Collection item already exist",
                    "Can't create collection item because it already exist: " +
                    "collectionID=" + collection.getCollectionID() + ", " +
                    "productID=" + product.getProductID() + ".");
        }

        // Если всё нормально - сохраняем
        try {
            entityManager.persist(item);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }

        return item;
    }

    /**
     * Обновляет товарную позицию в коллекции
     * @param item обовленная товарная позиция коллекции
     * @return сохраненная товарная позиация коллекции
     * @throws HermesException информация об ошибке
     */
    @Transactional
    public CollectionItem updateCollectionItem(CollectionItem item) throws HermesException {
        Validator.nonNegativeInteger("collectionID", item.getCollectionID());
        Validator.nonNegativeInteger("productID", item.getProductID());

        // Проверяем есть ли такая позиция в коллекции
        CollectionItem managedItem = entityManager.find(CollectionItem.class, item.getItemID());
        if (managedItem==null) {
            throw new HermesException(NOT_FOUND, "Collection item not found",
                    "Collection itemID=" + item.getItemID() + " not found.");
        }

        // Проверяем наличие указанной коллекции и товара
        Collection collection = getCollection(item.getCollectionID());
        Product product = getProduct(item.getProductID());

        // Присваиваем проверенные значения
        managedItem.setCollectionID(collection.getCollectionID());
        managedItem.setProductID(product.getProductID());
        managedItem.setOrderNumber(item.getOrderNumber());

        // Если всё нормально - сохраняем
        try {
            entityManager.persist(item);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HermesException(INTERNAL_SERVER_ERROR, "Internal Server Error", e.getMessage());
        }

        return item;
    }

    /**
     * Удаляет товарную позицию коллекции
     * @param collectionItemID коллекции
     * @throws HermesException информация об ошибке
     */
    public void removeCollectionItem(long collectionItemID) throws HermesException {
        Validator.nonNegativeInteger("collectionItemID", collectionItemID);

        CollectionItem managedItem = entityManager.find(CollectionItem.class, collectionItemID);
        if (managedItem == null)
            throw new HermesException(NOT_FOUND, "Collection item not found",
                    "Cannot remove collection item: itemID=" + collectionItemID + ".");

        Collection collection = getCollection(managedItem.getCollectionID());

        try {
            // Удаляем позицию коллекции
            entityManager.remove(managedItem);
            // Обновить временную метку последнего изменения коллекции
            collection.setTimestamp(System.currentTimeMillis());
            entityManager.persist(collection);
        } catch (Exception exception) {
            try {
                transactionManager.setRollbackOnly();
            } catch (IllegalStateException | SystemException e) {
                e.printStackTrace();
            }
            throw exception;
        }
    }

}

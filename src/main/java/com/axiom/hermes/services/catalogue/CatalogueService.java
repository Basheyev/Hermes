package com.axiom.hermes.services.catalogue;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.common.validation.Validator;
import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Collection;
import com.axiom.hermes.model.catalogue.entities.CollectionItem;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.catalogue.entities.ProductImage;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.List;
import java.util.Map;

import static com.axiom.hermes.common.exceptions.HermesException.*;

/**
 * Сервис каталога товаров
 */
@Path("/catalogue")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogueService {

    private static final Logger LOG = Logger.getLogger(Catalogue.class);

    public static final int MAX_IMAGE_SIZE = 512 * 1024;      // 512Kb
    public static final int MAX_THUMBNAIL_SIZE = 0xFFFF;      // 65Kb
    public static final int MAX_THUMBNAIL_DIMENSION = 128;    // 128x128px

    private static final int INVALID = -1;

    @Inject
    Catalogue catalogue;

    public CatalogueService() { }

    /**
     * Предоставляет перечень всех доступных для заказа товарных позиций
     * @return список товарных позиций
     * @throws HermesException информация об ошибке
     */
    @GET
    public Response getAvailableProducts() throws HermesException  {
        List<Product> availableProducts = catalogue.getAvailableProducts();
        if (availableProducts==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(availableProducts).build();
    }

    /**
     * Предоставляет полный перчень товарных позиций, включая недоступные для заказа
     * @return список товарных позиций
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getAllProducts")
    public Response getAllProducts() throws HermesException  {
        List<Product> allProducts = catalogue.getAllProducts();
        if (allProducts==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(allProducts).build();
    }

    /**
     * Возвращает информацию по товарной позици по указнному ID
     * @param productID товарной позиции
     * @return информация товарной позиции
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getProduct")
    public Response getProduct(@QueryParam("productID") long productID) throws HermesException {
        Product product = catalogue.getProduct(productID);
        return Response.ok(product).build();
    }

    /**
     * Добавляет в каталог новую товарную позицию
     * @param newProduct информация о товарной позиции
     * @return добавленная товарная позиция или Null если уже есть
     * @throws HermesException информация об ошибке
     */
    @POST
    @Path("/addProduct")
    public Response addProduct(Product newProduct) throws HermesException {
        Product product = catalogue.addProduct(newProduct);
        return Response.ok(product).build();
    }

    /**
     * Обновляет в каталог информацию о товарной позиции
     * @param product информация о товарной позиции
     * @return обновленная или добавленная товарная позиация
     * @throws HermesException информация об ошибке
     */
    @PUT
    @Path("/updateProduct")
    public Response updateProduct(Product product) throws HermesException {
        Product managed = catalogue.updateProduct(product);
        return Response.ok(managed).build();
    }

    /**
     * Удаляет продукт, если он нигде не используется
     * @param productID товара
     * @return 200 ОК
     * @throws HermesException информация об ошибке
     */
    @DELETE
    @Path("/removeProduct")
    public Response removeProduct(@QueryParam("productID") long productID) throws HermesException {
        catalogue.removeProduct(productID);
        return Response.ok().build();
    }

    /**
     * Возвращает миниатюру изображения вписанную в размер 128x128
     * @param productID товарной позиции
     * @return изображение миниатюры (image/jpeg)
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/downloadThumbnail")
    public Response downloadThumbnail(@QueryParam("productID") long productID) throws HermesException {
        byte[] bytes = catalogue.getProductThumbnail(productID);
        String filename = "thumbnail" + productID + ".jpg";
        Response.ResponseBuilder responseBuilder = Response.ok(bytes);
        responseBuilder.header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                       .header("Content-Type", "image/jpeg");
        return responseBuilder.build();
    }

    /**
     * Возвращает полноразмерное изображение товара
     * @param productID товарной позиции
     * @return полноразмерное изображение (image/jpeg)
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/downloadImage")
    public Response downloadImage(@QueryParam("productID") long productID) throws HermesException {
        ProductImage productImage = catalogue.getProductImage(productID);
        byte[] imageBytes = productImage.getImage();
        String filename = productImage.getFilename();
        Response.ResponseBuilder responseBuilder = Response.ok(imageBytes);
        responseBuilder.header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                       .header("Content-Type", "image/jpeg");
        return responseBuilder.build();
    }


    /**
     * Загружает/заменяет изображение товарной позиции (multipart/form-data)
     * @param dataInput форма содержащая: productID - товарная позиция, file - файл изображения (image/jpeg)
     * @return 200 ОК
     * @throws HermesException информация об ошибке
     */
    @POST
    @Path("/uploadImage")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(MultipartFormDataInput dataInput) throws HermesException {
        // Получаем многосоставную форму с данными
        Map<String, List<InputPart>> multipartForm = dataInput.getFormDataMap();
        // Если форме не содержит полей productID или file - возвращаем HTTP_BAD_REQUEST
        if (!multipartForm.containsKey("productID"))
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "productID field is missing");
        if (!multipartForm.containsKey("file"))
            throw new HermesException(BAD_REQUEST, "Invalid parameter", "file field is missing");

        //------------------------------------------------------------------------------------
        // Получаем из формы productID
        //------------------------------------------------------------------------------------
        List<InputPart> productInputParts = multipartForm.get("productID");
        if (productInputParts.size() != 1)
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
                    "productID field parts quantity is 0 or more than 1");
        long productID = parseProductID(productInputParts);
        // Пытаемся получить продукт до начала загрузки файла, если нет - кидаем exception
        Product product = catalogue.getProduct(productID);
        //------------------------------------------------------------------------------------
        // Получаем из формы file (проверяем что файл один)
        //------------------------------------------------------------------------------------
        List<InputPart> fileInputParts = multipartForm.get("file");
        if (fileInputParts.size() != 1)
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
                "file field parts quantity is 0 or more than 1");
        InputPart inputPart = fileInputParts.get(0);
        //------------------------------------------------------------------------------------
        // Получаем заголовки
        //------------------------------------------------------------------------------------
        MultivaluedMap<String, String> header = inputPart.getHeaders();
        // Получаем название файла
        String filename = parseFileName(header);
        // Если Content-Type не image/jpeg - уходим
        if (!header.getFirst("Content-Type").equals("image/jpeg")) {
            throw new HermesException(UNSUPPORTED_MEDIA, "Content-type is not image/jpeg",
                    "Only image/jpeg mime type is supported");
        }
        //------------------------------------------------------------------------------------
        // Получаем тело файла и создаём миниатюрное изображение
        //------------------------------------------------------------------------------------
        byte[] originalImage = loadFile(inputPart, filename);
        byte[] thumbnail = createThumbnail(originalImage, filename);

        //------------------------------------------------------------------------------------
        // Создаем объект ProductImage и сохраняем его в базе данных
        //------------------------------------------------------------------------------------
        ProductImage productImage = new ProductImage(product.getProductID(), filename, originalImage, thumbnail);
        catalogue.uploadImage(productImage);

        // Отправляем ответ клиентом с мини отчётом о загруженном изображении
        String response =
                "{\n"+
                "    \"productID\": " + product.getProductID() + ",\n" +
                "    \"filename\": \"" + filename + "\",\n" +
                "    \"imageSize\": " + originalImage.length + ",\n" +
                "    \"thumbnailSize\": " + thumbnail.length + "\n" +
                "}";

        return Response.ok().entity(response).build();

    }

    //--------------------------------------------------------------------------------------------------
    // Управление коллекциями
    //--------------------------------------------------------------------------------------------------

    /**
     * Возвращает список всех коллекций
     * @return список всех коллекций
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getCollections")
    public Response getCollections() throws HermesException {
        List<Collection> collections = catalogue.getCollections();
        return Response.ok().entity(collections).build();
    }

    /**
     * Получить карточку коллекции товаров
     * @param collectionID коллекции
     * @return карточка коллекции
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getCollection")
    public Response getCollection(@QueryParam("collectionID") long collectionID) throws HermesException {
        Collection collection = catalogue.getCollection(collectionID);
        return Response.ok().entity(collection).build();
    }

    /**
     * Добавляет новую карточку коллекцию
     * @param collection карточка коллекции
     * @return сохраненная карточка коллекции
     * @throws HermesException информация об ошибке
     */
    @POST
    @Path("/addCollection")
    public Response addCollection(Collection collection) throws HermesException {
        Collection managed = catalogue.addCollection(collection);
        return Response.ok().entity(managed).build();
    }

    /**
     * Обновляет карточку коллекции
     * @param collection измененная карточка коллекции
     * @return обновленная карточка коллекции
     * @throws HermesException информация об ошибке
     */
    @PUT
    @Path("/updateCollection")
    public Response updateCollection(Collection collection) throws HermesException {
        Collection managed = catalogue.updateCollection(collection);
        return Response.ok().entity(managed).build();
    }

    /**
     * Удаляет коллекцию товаров: карточку и ссылки на товары
     * @param collectionID коллекции
     * @throws HermesException информация об ошибке
     */
    @DELETE
    @Path("/removeCollection")
    public Response removeCollection(@QueryParam("collectionID") long collectionID) throws HermesException {
        catalogue.removeCollection(collectionID);
        return Response.ok().build();
    }

    /**
     * Получить список всех товаров в коллекции
     * @param collectionID коллекции
     * @return список товаров коллекции
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getCollectionItems")
    public Response getCollectionItems(@QueryParam("collectionID") long collectionID) throws HermesException {
        List<CollectionItem> collectionItems = catalogue.getCollectionItems(collectionID);
        return Response.ok().entity(collectionItems).build();
    }

    /**
     * Добавляет товарную позицию в коллекцию
     * @param item товарная позиция коллекции
     * @return добавленная товарная позиция в коллекции
     * @throws HermesException информация об ошибке
     */
    @POST
    @Path("/addCollectionItem")
    public Response addCollectionItem(CollectionItem item) throws HermesException {
        CollectionItem managed = catalogue.addCollectionItem(item);
        return Response.ok().entity(managed).build();
    }

    /**
     * Обновляет товарную позицию в коллекции
     * @param item обовленная товарная позиция коллекции
     * @return сохраненная товарная позиация коллекции
     * @throws HermesException информация об ошибке
     */
    @PUT
    @Path("/updateCollectionItem")
    public Response updateCollectionItem(CollectionItem item) throws HermesException {
        CollectionItem managed = catalogue.updateCollectionItem(item);
        return Response.ok().entity(managed).build();
    }

    /**
     * Удаляет товарную позицию коллекции
     * @param collectionItemID коллекции
     * @throws HermesException информация об ошибке
     */
    @DELETE
    @Path("/removeCollectionItem")
    public Response removeCollectionItem(@QueryParam("itemID") long collectionItemID) throws HermesException {
        catalogue.removeCollectionItem(collectionItemID);
        return Response.ok().build();
    }

    //----------------------------------------------------------------------------------------------------
    // Парсинг частей формы и загрузка файла
    //----------------------------------------------------------------------------------------------------

    /**
     * Получить из поля формы значение поля productID
     * @param productInputParts заголовок части формы
     * @return productID товарной позиции
     * @throws HermesException информация об ошибке
     */
    private long parseProductID(List<InputPart> productInputParts) throws HermesException {
        long productID = INVALID;
        try {
            if (productInputParts != null && productInputParts.size() > 0) {
                String value = productInputParts.get(0).getBody(String.class, null);
                productID = Long.parseLong(value);
            }
        } catch (Exception e) {
            throw new HermesException(BAD_REQUEST, "Invalid parameter",
                    "productID is not an integer number");
        }
        return productID;
    }


    /**
     * Получает из заголовка части многосоставной формы имя файла изображения
     * @param header заголовок части многосоставной формы
     * @return имя файла или "unknown" если не найдено
     * @throws HermesException информация об ошибке
     */
    private String parseFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        try {
            for (String filename : contentDisposition) {
                if ((filename.trim().startsWith("filename"))) {
                    String[] name = filename.split("=");
                    return name[1].trim().replaceAll("\"", "");
                }
            }
        } catch (Exception e) {
           // e.printStackTrace();
        }
        return "unknown";
    }

    /**
     * Загружает файл изображения из multipart/form-data
     * @param inputPart часть multipart/form-data
     * @param filename названия файла
     * @return массив байт файла
     * @throws HermesException информация об ошибке
     */
    private byte[] loadFile(InputPart inputPart, String filename) throws HermesException {
        InputStream inputStream;
        byte[] originalImage;
        try {
            inputStream = inputPart.getBody(InputStream.class,null);
            originalImage = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HermesException(UNSUPPORTED_MEDIA, "Cannot load product image body",
                    "Failed to load '" + filename + "' image.");
        }
        // Проверка ограничения на размер файла
        if (originalImage.length > MAX_IMAGE_SIZE) {
            throw new HermesException(REQUEST_TOO_LARGE, "Image size is too large",
                    "Maximum JPEG image file size is limited to " + MAX_IMAGE_SIZE + " bytes");
        }
        return originalImage;
    }


    /**
     * Создает миниатюру 128x128 из оригинального изображения
     * @param originalImage оригинальное изображение
     * @param filename название файла
     * @return массив байт миниатюры в формате JPEG
     * @throws HermesException информация об ошибке
     */
    private byte[] createThumbnail(byte[] originalImage, String filename) throws HermesException {
        // Формируем буфер под миниатюрное изображение 128x128 image/jpeg (до 64Kb)
        ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream(MAX_THUMBNAIL_SIZE);
        byte[] thumbnail;
        try {
            // Уменьшаем изображение изображение до 128x128
            Thumbnails.of(new ByteArrayInputStream(originalImage))
                    .size(MAX_THUMBNAIL_DIMENSION, MAX_THUMBNAIL_DIMENSION)
                    .toOutputStream(thumbnailOutput);
            thumbnail = thumbnailOutput.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new HermesException(UNSUPPORTED_MEDIA, "Cannot create image thumbnail",
                    "Failed to create '" + filename + "' thumbnail. Product image not saved.");
        }
        return thumbnail;
    }



}

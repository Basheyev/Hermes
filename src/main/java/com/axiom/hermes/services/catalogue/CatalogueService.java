package com.axiom.hermes.services.catalogue;

import com.axiom.hermes.model.catalogue.Catalogue;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.catalogue.entities.ProductImage;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Сервис каталога товаров
 */
@Path("/catalogue")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogueService {

    public static final int MAX_IMAGE_SIZE = 512 * 1024;    // 512Kb
    public static final int MAX_THUMBNAIL_SIZE = 0xFFFF;    // 65Kb
    public static final int MAX_THUMBNAIL_DIMENSION = 128;  // 128x128px

    public static final int INVALID = -1;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_REQUEST_TOO_LARGE =413;
    public static final int HTTP_UNSUPPORTED_MEDIA = 415;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;

    @Inject
    Catalogue catalogue;

    public CatalogueService() { }

    /**
     * Предоставляет перечень всех доступных для заказа товарных позиций
     * @return список товарных позиций
     */
    @GET
    public List<Product> getAvailableProducts() {
        return catalogue.getAvailableProducts();
    }

    /**
     * Предоставляет полный перчень товарных позиций, включая недоступные для заказа
     * @return список товарных позиций
     */
    @GET
    @Path("/allProducts")
    public List<Product> getAllProducts() {
        return catalogue.getAllProducts();
    }

    /**
     * Возвращает информацию по товарной позици по указнному ID
     * @param id товарной позиции
     * @return информация товарной позиции
     */
    @GET
    @Path("/getProduct")
    public Product getProduct(@QueryParam("id") int id) {
        return catalogue.getProduct(id);
    }

    /**
     * Добавляет в каталог новую товарную позицию
     * @param newProduct информация о товарной позиции
     * @return добавленная товарная позиция или Null если уже есть
     */
    @POST
    @Path("/addProduct")
    public Product addProduct(Product newProduct) {
        Product product = catalogue.addProduct(newProduct);
        if (product==null) return null;
        System.out.println("New product added name:" + product.name + " Description:" + product.description);
        return product;
    }

    /**
     * Обновляет в каталог информацию о товарной позиции
     * @param product информация о товарной позиции
     * @return обновленная или добавленная товарная позиация
     */
    @POST
    @Path("/updateProduct")
    public Product updateProduct(Product product) {
        return catalogue.updateProduct(product);
    }

    /**
     * Возвращает миниатюру изображения вписанную в размер 128x128
     * @param id товарной позиции
     * @return изображение миниатюры (image/jpeg)
     */
    @GET
    @Path("/downloadThumbnail")
    public Response downloadThumbnail(@QueryParam("id") int id) {
        byte[] bytes = catalogue.getProductThumbnail(id);
        if (bytes==null) {
            System.out.println("Thumbnail of productID=" + id + " not found");
            return Response.status(HTTP_NOT_FOUND, "productID=" + id + " not found").build();
        }
        String filename = "thumbnail" + id + ".jpg";
        Response.ResponseBuilder responseBuilder = Response.ok(bytes);
        responseBuilder.header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .header("Content-Type", "image/jpeg");
        System.out.println("Downloading thumbnail productID=" + id +
                " filename = " + filename + " size=" + bytes.length);
        return responseBuilder.build();
    }

    /**
     * Возвращает полноразмерное изображение товара
     * @param id товарной позиции
     * @return полноразмерное изображение (image/jpeg)
     */
    @GET
    @Path("/downloadImage")
    public Response downloadImage(@QueryParam("id") int id) {
        ProductImage productImage = catalogue.getProductImage(id);
        if (productImage==null) return Response.status(HTTP_NOT_FOUND, "imageID=" + id + " not found").build();
        byte[] imageBytes = productImage.image;
        String filename = productImage.filename;
        Response.ResponseBuilder responseBuilder = Response.ok(imageBytes);
        responseBuilder.header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .header("Content-Type", "image/jpeg");
        System.out.println("Downloading image of productID=" + id +
                " filename = " + filename + " size=" + imageBytes.length);
        return responseBuilder.build();
    }


    /**
     * Загружает/заменяет изображение товарной позиции (multipart/form-data)
     * @param dataInput форма содержащая: productID - товарная позиция, file - файл изображения (image/jpeg)
     * @return Status 200 - если ок
     */
    @POST
    @Path("/uploadImage")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(MultipartFormDataInput dataInput) {
        // Получаем многосоставную форму с данными
        Map<String, List<InputPart>> multipartForm = dataInput.getFormDataMap();
        // Если форме не содержит полей productID или file - возвращаем HTTP_BAD_REQUEST
        if (!multipartForm.containsKey("productID") || !multipartForm.containsKey("file")) {
            return Response.status(HTTP_BAD_REQUEST).build();
        }

        try {
            //------------------------------------------------------------------------------------
            // Получаем из формы productID
            //------------------------------------------------------------------------------------
            List<InputPart> productInputParts = multipartForm.get("productID");
            if (productInputParts.size() != 1) return Response.status(HTTP_BAD_REQUEST).build();
            int productID = parseProductID(productInputParts);
            if (productID==INVALID) return Response.status(HTTP_BAD_REQUEST).build();
            //------------------------------------------------------------------------------------
            // Получаем из формы file (проверяем что файл один)
            //------------------------------------------------------------------------------------
            List<InputPart> fileInputParts = multipartForm.get("file");
            if (fileInputParts.size() != 1) return Response.status(HTTP_BAD_REQUEST).build();
            InputPart inputPart = fileInputParts.get(0);
            //------------------------------------------------------------------------------------
            // Получаем заголовки
            //------------------------------------------------------------------------------------
            MultivaluedMap<String, String> header = inputPart.getHeaders();
            // Получаем название файла
            String fileName = parseFileName(header);
            // Если Content-Type не image/jpeg - уходим
            if (!header.getFirst("Content-Type").equals("image/jpeg")) {
                return Response.status(HTTP_UNSUPPORTED_MEDIA).build();
            }
            //------------------------------------------------------------------------------------
            // Получаем тело файла
            //------------------------------------------------------------------------------------
            InputStream inputStream = inputPart.getBody(InputStream.class,null);
            byte[] originalImage = IOUtils.toByteArray(inputStream);
            // Проверка ограничения на размер файла
            if (originalImage.length > MAX_IMAGE_SIZE) return Response.status(HTTP_REQUEST_TOO_LARGE).build();
            // Формируем миниатюрное изображение 128x128 image/jpeg (до 64Kb)
            ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream(MAX_THUMBNAIL_SIZE);
            Thumbnails.of(new ByteArrayInputStream(originalImage))
                    .size(MAX_THUMBNAIL_DIMENSION,MAX_THUMBNAIL_DIMENSION)
                    .toOutputStream(thumbnailOutput);
            byte[] thumbnail = thumbnailOutput.toByteArray();;
            //------------------------------------------------------------------------------------
            // Создать объект ProductImage и сохранить его в базе данных
            //------------------------------------------------------------------------------------
            ProductImage productImage = new ProductImage(productID, fileName, originalImage, thumbnail);
            if (!catalogue.uploadImage(productImage)) {
                return Response.status(HTTP_NOT_FOUND, "productID=" + productID + " not found").build();
            }

            System.out.println(
                    "PRODUCT IMAGE UPLOAD:" +
                    " ProductID=" + productID +
                    " filename=\"" + fileName + "\"" +
                    " size: " + originalImage.length + " bytes" +
                    " thumbnail size:" + thumbnail.length + " bytes");

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(HTTP_INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }



    private int parseProductID(List<InputPart> productInputParts) {
        int productID = INVALID;
        try {
            if (productInputParts != null && productInputParts.size() > 0) {
                String value = productInputParts.get(0).getBody(String.class, null);
                productID = Integer.parseInt(value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return productID;
    }


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
            e.printStackTrace();
        }
        return "unknown";
    }

    //-------------------------------------------------------------------------
    // Для целей тестирования
    //-------------------------------------------------------------------------
    @GET
    @Path("/generate")
    public Response generateProducts() {
        catalogue.generateProducts();
        return Response.ok().build();
    }

    @GET
    @Path("/reset")
    public Response resetProducts() {
        catalogue.resetProducts();
        return Response.ok().build();
    }


}

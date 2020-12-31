package com.axiom.hermes.tests.catalogue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CatalogueServiceTest {

    private static final Logger LOG = Logger.getLogger(CatalogueServiceTest.class);

    private static final String imagePath = "C:\\Development\\Hermes\\testdata\\";
    public static final String fileName = "shoes.jpg";
    public static final String changedFileName = "bag.jpg";
    public static final String bigFileName = "bigimage1.jfif";

    private static int productID;
    private static long imageSize;

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    public void getAllProducts() {
        given()
                .when().get("/catalogue/getAllProducts")
                .then()
                .statusCode(200);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(2)
    public void addProduct() {
        productID =
        given()
            .header("Content-Type", "application/json")
            .body("{\n" +
                    "    \"name\": \"CUP OF COFFEE\",\n" +
                    "    \"description\": \"MACCOFFEE\",\n" +
                    "    \"price\": 5,\n" +
                    "    \"vendorCode\": \"CCMAC\"\n" +
                    "}")
        .when()
            .post("/catalogue/addProduct")
        .then()
            .statusCode(200)
            .assertThat()
            .body("vendorCode", equalTo("CCMAC"))
            .body("description", equalTo("MACCOFFEE"))
            .extract().path("productID");

        LOG.info("Product created productID=" + productID);
    }

    //---------------------------------------------------------------------------------------------------
    @Test
    @Order(2)
    public void addProductInvalid() {
        String response =
            given()
                .header("Content-Type", "application/json")
                .body("{\n" +
                        "    \"productID\": 1,\n" +
                        "    \"name\": \"CUP OF INVALID COFFEE\",\n" +
                        "    \"description\": \"MACCOFFEE\",\n" +
                        "    \"price\": 5,\n" +
                        "    \"vendorCode\": \"CCMAC\"\n" +
                        "}")
            .when()
                .post("/catalogue/addProduct")
            .then()
                .statusCode(400)
            .extract().asString();

        LOG.info("Invalid product add response:\n" +response);
    }

    //---------------------------------------------------------------------------------------------------
    @Test
    @Order(3)
    public void getProduct() {
        String response =
                given().
                        when().get("/catalogue/getProduct?productID=" + productID).
                        then().statusCode(200).assertThat()
                        .body("name", equalTo("CUP OF COFFEE"))
                        .body("vendorCode", equalTo("CCMAC"))
                        .body("description", equalTo("MACCOFFEE"))
                        .body("price", equalTo(5f))
                .extract().asString();
        LOG.info("Product get: " + response);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(4)
    public void updateProduct() {
        String request = "{\n" +
                "    \"productID\": " + productID + ",\n" +
                "    \"name\": \"CUP OF COFFEE\",\n" +
                "    \"description\": \"MACCOFFEE\",\n" +
                "    \"price\": 15,\n" +
                "    \"vendorCode\": \"CCMAC\",\n" +
                "    \"available\": true\n" +
                "}";
        LOG.info("updateProduct request:\n" + request);
        productID =
                given()
                        .header("Content-Type", "application/json")
                        .body(request)
                .when()
                        .put("/catalogue/updateProduct")
                .then()
                        .statusCode(200)
                        .assertThat()
                        .body("name", equalTo("CUP OF COFFEE"))
                        .body("vendorCode", equalTo("CCMAC"))
                        .body("description", equalTo("MACCOFFEE"))
                        .body("price", equalTo(15f))
                        .body("available", equalTo(true))
                        .extract().path("productID");

        LOG.info("Product updated productID=" + productID);
    }

    //---------------------------------------------------------------------------------------------------
    @Test
    @Order(5)
    public void getAvailableProducts() {
        String body =
        given()
        .when()
               .get("/catalogue")
        .then().assertThat()
                .body(containsString("\"available\":true"))         // Содержит доступные товары
                .body(not(containsString("\"available\":false")))   // Не содержит недоступные товары
                .statusCode(200).extract().asString();
        LOG.info("Get Available Product response :" + body);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(6)
    public void uploadImage() {
        File imageFile = new File(imagePath + fileName);
        imageSize = imageFile.length();
        String response =
        given()
                .multiPart("productID", productID)
                .multiPart("file", imageFile,"image/jpeg")
                .accept(ContentType.JSON)
        .when()
                .post("/catalogue/uploadImage")
        .then()
                .statusCode(200)
                .body("imageSize", equalTo((int)imageSize))
        .extract().asString();
        LOG.info("Upload image:\n" + response);
    }

    @Test
    @Order(7)
    public void uploadImageChange() {
        File imageFile = new File(imagePath + changedFileName);
        imageSize = imageFile.length();
        String response =
        given()
            .multiPart("productID", productID)
            .multiPart("file", imageFile,"image/jpeg")
            .accept(ContentType.JSON)
        .when()
            .post("/catalogue/uploadImage")
        .then()
            .statusCode(200)
            .body("imageSize", equalTo((int)imageSize))
        .extract()
            .asString();

        LOG.info("Upload image:\n" + response);
    }


    @Test
    @Order(8)
    public void uploadImageBigFile() {
        // Проверяем защиту на большие файлы
        File bigImageFile = new File(imagePath + bigFileName);
        String response =
        given()
            .multiPart("productID", productID)
            .multiPart("file", bigImageFile,"image/jpeg")
            .accept(ContentType.JSON)
        .when()
            .post("/catalogue/uploadImage")
        .then()
            .assertThat()
            .statusCode(413)
        .extract()
            .asString();

        LOG.info("Upload big image:\n" + response);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(9)
    public void downloadImage() {
        File outputImageFile = new File(imagePath + "downloaded_" + changedFileName);
        if (!outputImageFile.exists()) {

            byte[] image =
                    given().
                    when().get("/catalogue/downloadImage?productID=" + productID).
                    then().statusCode(200).
                            assertThat().
                            header("Content-Disposition", containsString(changedFileName)).
                            header("Content-Type", equalTo("image/jpeg")).
                            extract().
                            asByteArray();

            assertTrue(image.length==imageSize);
            try {
                OutputStream outStream = new FileOutputStream(outputImageFile);
                outStream.write(image);
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(10)
    public void downloadThumbnail() {
        File outputImageFile = new File(imagePath + "thumbnail_" + changedFileName);
        if (!outputImageFile.exists()) {

            byte[] image =
                    given().
                    when()
                        .get("/catalogue/downloadThumbnail?productID=" + productID).
                    then()
                        .statusCode(200).
                        assertThat().
                        header("Content-Disposition", containsString("thumbnail" + productID + ".jpg")).
                        header("Content-Type", equalTo("image/jpeg")).
                    extract().asByteArray();

            assertTrue(image.length > 0);
            try {
                OutputStream outStream = new FileOutputStream(outputImageFile);
                outStream.write(image);
                outStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(10)
    public void removeProduct() {

        given().
                when().delete("/catalogue/removeProduct?productID=" + productID).
                then().assertThat().statusCode(200).extract().asString();

        String response =
        given().
                when().get("/catalogue/getProduct?productID=" + productID).
                then().assertThat().statusCode(404).extract().asString();

        LOG.info("Product deleted ProductID=" + productID + " check response:\n" + response);
    }

}

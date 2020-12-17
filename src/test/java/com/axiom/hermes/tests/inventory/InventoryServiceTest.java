package com.axiom.hermes.tests.inventory;

import com.axiom.hermes.tests.customers.SalesOrdersServiceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryServiceTest {

    private static final Logger LOG = Logger.getLogger(InventoryServiceTest.class);

    public static int customerID;
    public static int productID;
    public static int initialStock;

    @Test
    @Order(1)
    public void getAllStocks() {
        String response =
        given()
        .when()
            .get("/inventory")
        .then()
            .statusCode(200)
        .extract().asString();

        LOG.info(response);


        // Добавить пользователя
        customerID =
                given()
                        .header("Content-Type", "application/json")
                        .body("{\n" +
                                "    \"mobile\": \"+77056004927\",\n" +
                                "    \"businessID\": \"850415308450\",\n" +
                                "    \"name\": \"Болат Башеев\",\n" +
                                "    \"address\": \"Улы дала 5/2, кв. 248\",\n" +
                                "    \"city\": \"Нур-Султан\",\n" +
                                "    \"country\": \"Казахстан\",\n" +
                                "    \"verified\": true\n" +
                                "}")
                        .when()
                        .post("/customers/addCustomer")
                        .then()
                        .statusCode(200)
                        .assertThat()
                        .body("mobile", equalTo("+77056004927"))
                        .body("businessID", equalTo("850415308450"))
                        .extract().path("customerID");

        LOG.info("Customer created customerID=" + customerID);

        // Добавить продукт
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


        initialStock = given()
                .when()
                .get("/inventory/getStockInformation?productID=" + productID)
        .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
        .extract().jsonPath().getInt("stockOnHand");

        LOG.info("Initial stock:" + initialStock);
    }

    @Test
    @Order(2)
    public void purchase() {
        String response =
            given()
            .when()
                .get("/inventory/purchase?productID=" + productID + "&amount=10&price=20")
            .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("amount", equalTo(10))
                .body("price", equalTo(20f))
            .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(3)
    public void saleReturn() {
        String response =
            given()
            .when()
                .get("/inventory/saleReturn?productID=" + productID + "&amount=5&price=20")
            .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("amount", equalTo(5))
                .body("price", equalTo(20f))
            .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(4)
    public void sale() {
        String response =
                given()
                .when()
                    .get("/inventory/sale?productID=" + productID + "&amount=10&price=30")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("amount", equalTo(10))
                .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(5)
    public void purchaseReturn() {
        String response =
                given()
                .when()
                    .get("/inventory/purchaseReturn?productID=" + productID + "&amount=1&price=20")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("amount", equalTo(1))
                    .body("price", equalTo(20f))

                .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(6)
    public void writeOff() {
        String response =
                given()
                .when()
                    .get("/inventory/writeOff?productID=" + productID + "&amount=4&price=20")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("amount", equalTo(4))
                    .body("price", equalTo(20f))
                .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(7)
    public void saleOutOfStock() {
        String response =
                given()
                        .when()
                        .get("/inventory/sale?productID=" + productID + "&amount=1000&price=30")
                .then()
                        .assertThat()
                        .statusCode(404)
                .extract().asString();
        LOG.info(response);
    }


    @Test
    @Order(8)
    public void getStockInformation() {
        int finalStock =
                given()
                .when()
                    .get("/inventory/getStockInformation?productID=" + productID)
                .then()
                .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("stockOnHand", equalTo(initialStock))
                .extract().jsonPath().getInt("stockOnHand");

        LOG.info("Final stock: " + finalStock);
    }

    @Test
    @Order(9)
    public void removeCustomer() {
        String response =
                given().
                        when().get("/customers/removeCustomer?customerID=" + customerID).
                        then().statusCode(200).extract().asString();
        LOG.info("CustomerID=" + customerID + " deleted response=" + response);
    }
}

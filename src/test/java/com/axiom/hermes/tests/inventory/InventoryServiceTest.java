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
                                "    \"unitCost\": 5,\n" +
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
                .get("/inventory/getStockCard?productID=" + productID)
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
                .get("/inventory/purchase?productID=" + productID + "&quantity=10&unitCost=20")
            .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("quantity", equalTo(10))
                .body("unitCost", equalTo(20f))
            .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(3)
    public void saleReturn() {
        String response =
            given()
            .when()
                .get("/inventory/saleReturn?productID=" + productID + "&quantity=5&unitCost=20")
            .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("quantity", equalTo(5))
                .body("unitCost", equalTo(20f))
            .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(4)
    public void sale() {
        String response =
                given()
                .when()
                    .get("/inventory/sale?productID=" + productID + "&quantity=10&unitCost=30")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("quantity", equalTo(10))
                .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(5)
    public void purchaseReturn() {
        String response =
                given()
                .when()
                    .get("/inventory/purchaseReturn?productID=" + productID + "&quantity=1&unitCost=20")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("quantity", equalTo(1))
                    .body("unitCost", equalTo(20f))

                .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(6)
    public void writeOff() {
        String response =
                given()
                .when()
                    .get("/inventory/writeOff?productID=" + productID + "&quantity=4&unitCost=20")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("quantity", equalTo(4))
                    .body("unitCost", equalTo(20f))
                .extract().asString();
        LOG.info(response);
    }

    @Test
    @Order(7)
    public void saleOutOfStock() {
        String response =
                given()
                        .when()
                        .get("/inventory/sale?productID=" + productID + "&quantity=1000&unitCost=30")
                .then()
                        .assertThat()
                        .statusCode(404)
                .extract().asString();
        LOG.info(response);
    }


    @Test
    @Order(8)
    public void getStockCard() {
        int finalStock =
                given()
                .when()
                    .get("/inventory/getStockCard?productID=" + productID)
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
                        when().delete("/customers/removeCustomer?customerID=" + customerID).
                        then().statusCode(200).extract().asString();
        LOG.info("CustomerID=" + customerID + " deleted response=" + response);
    }
}

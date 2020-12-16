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
    public static final int productID = 1;
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

}

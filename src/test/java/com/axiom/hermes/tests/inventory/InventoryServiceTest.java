package com.axiom.hermes.tests.inventory;

import com.axiom.hermes.tests.customers.SalesOrdersServiceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryServiceTest {

    private static final Logger LOG = Logger.getLogger(InventoryServiceTest.class);

    @Test
    @Order(1)
    public void getAllStocks() {
        String response =
        given()
                .when().get("/inventory")
                .then()
                .statusCode(200).extract().asString();

        LOG.info(response);
    }

}

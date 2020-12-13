package com.axiom.hermes.tests.customers;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomersServiceTest {

    private static final Logger LOG = Logger.getLogger(SalesOrdersServiceTest.class);

    @Test
    @Order(1)
    public void allCustomers() {
        String response =
        given()
        .when()
            .get("/customers")
        .then()
            .statusCode(200)
        .extract().asString();
        LOG.info(response);
    }
}

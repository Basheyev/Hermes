package com.axiom.hermes.tests.catalogue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CatalogueServiceTest {


    @Test
    @Order(1)
    public void getAllProducts() {
        given()
                .when().get("/catalogue/getAllProducts")
                .then()
                .statusCode(200);
    }

}

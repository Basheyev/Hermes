package com.axiom;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class SalesOrdersTest {

    @Test
    public void salesOrdersTest() {

        given()
                .when().get("/salesOrders")
                .then()
                .statusCode(200);
    }


}

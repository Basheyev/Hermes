package com.axiom;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class CatalogueResetTest {

    // todo Написать тесты на все API

    @Test
    public void catalogueResetTest() {
        given()
          .when().get("/catalogue/reset")
          .then()
             .statusCode(200);
    }

}
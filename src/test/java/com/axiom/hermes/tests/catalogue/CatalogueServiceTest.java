package com.axiom.hermes.tests.catalogue;

import com.axiom.hermes.tests.customers.SalesOrdersServiceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CatalogueServiceTest {

    private static final Logger LOG = Logger.getLogger(CatalogueServiceTest.class);

    private static int productID;

    @Test
    @Order(1)
    public void getAllProducts() {
        given()
                .when().get("/catalogue/getAllProducts")
                .then()
                .statusCode(200);
    }


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

    @Test
    @Order(3)
    public void removeProduct() {
        //productID = 4;
        String reposnse =
        given().
                when().get("/catalogue/removeProduct?productID=" + productID).
                then().statusCode(200).extract().asString();
        LOG.info("Product deleted response: " + reposnse);
    }

}

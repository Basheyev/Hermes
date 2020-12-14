package com.axiom.hermes.tests.customers;

import com.axiom.hermes.model.customers.entities.Customer;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomersServiceTest {

    private static final Logger LOG = Logger.getLogger(SalesOrdersServiceTest.class);

    private static int customerID;
    private static String mobile;

    //---------------------------------------------------------------------------------------
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

    //---------------------------------------------------------------------------------------

    @Test
    @Order(2)
    public void addCustomer() {
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
    }

    //---------------------------------------------------------------------------------------

    @Test
    @Order(3)
    public void getCustomer() {
        mobile =
                given().
                when()
                    .get("/customers/getCustomer?customerID=" + customerID).
                then()
                    .statusCode(200)
                    .assertThat()
                    .body("name", equalTo("Болат Башеев"))
                    .body("address", equalTo("Улы дала 5/2, кв. 248"))
                    .body("city", equalTo("Нур-Султан"))
                    .body("country", equalTo("Казахстан"))
                .extract().path("mobile");
        LOG.info("CustomerID=" + customerID + " mobile: " + mobile);
    }

    //---------------------------------------------------------------------------------------

    @Test
    @Order(4)
    public void getCustomerByMobile() {
        String response =
                given().
                when()
                    .get("/customers/getCustomerByMobile?mobile=" + mobile).
                then()
                    .statusCode(200)
                    .assertThat()
                    .body("name", equalTo("Болат Башеев"))
                    .body("address", equalTo("Улы дала 5/2, кв. 248"))
                    .body("city", equalTo("Нур-Султан"))
                    .body("country", equalTo("Казахстан"))
                .extract().asString();
        LOG.info("Customer mobile:" + mobile + " data: " + response);
    }

    //---------------------------------------------------------------------------------------

    @Test
    @Order(5)
    public void updateCustomer() {
        String response =
        given()
            .header("Content-Type", "application/json")
            .body("{\n" +
                    "    \"customerID\":" + customerID + ",\n" +
                    "    \"name\": \"Башеев Болат Аскерович\"\n" +
                    "}")
        .when()
            .post("/customers/updateCustomer")
        .then()
            .statusCode(200)
            .assertThat()
            .body("name", equalTo("Башеев Болат Аскерович"))
            .body("verified", equalTo(false))
            .extract().asString();

        LOG.info("Customer updated data:" + response);
    }

    //---------------------------------------------------------------------------------------

    @Test
    @Order(6)
    public void removeCustomer() {
        String response =
                given().
                when().get("/customers/removeCustomer?customerID=" + customerID).
                then().statusCode(200).extract().asString();
        LOG.info("CustomerID=" + customerID + " deleted response=" + response);
    }

}

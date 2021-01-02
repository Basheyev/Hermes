package com.axiom.hermes.tests.customers;

import org.jboss.logging.Logger;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

// import org.junit.jupiter.api.Assertions;
// import static org.hamcrest.CoreMatchers.is;


import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SalesOrdersServiceTest {


    private static final Logger LOG = Logger.getLogger(SalesOrdersServiceTest.class);

    private static int customerID;
    private static int productID;
    private static int addedOrderID;
    private static int addedItemID;

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    public void allOrders() {

        given()
                .when().get("/salesOrders")
                .then()
                .statusCode(200);

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
                            "    \"cost\": 5,\n" +
                            "    \"vendorCode\": \"CCMAC\",\n" +
                            "    \"available\": true\n" +
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

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(2)
    public void addOrder() {
        addedOrderID =
        given()
             .header("Content-Type", "application/json")
             .body("{ \"customerID\":" + customerID + "}").
        when().post("/salesOrders/addOrder").
        then().statusCode(200).assertThat()
                .body("orderID", greaterThan(0))        // Проверяем что orderID > 0
                .body("status", equalTo(1))           // Проверям что status=1 (новый заказ)
                .extract().path("orderID");                  // Берём orderID
        LOG.info("Order added OrderID=" + addedOrderID);

    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(3)
    public void getOrder() {
        given().
        when().get("/salesOrders/getOrder?orderID=" + addedOrderID).
        then().statusCode(200).assertThat()
                .body("orderID", equalTo(addedOrderID))       // Проверяем что orderID > 0
                .body("status", equalTo(1));           // Проверям что status=1 (новый заказ)
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(4)
    public void addOrderItem() {
        addedItemID =
        given()
                .header("Content-Type", "application/json")
                .body("{ \"orderID\":" + addedOrderID +", \"productID\":" + productID + ", \"quantity\":12 }").
        when().post("/salesOrders/addOrderItem").
        then().statusCode(200).assertThat()
                .body("productID", equalTo(productID))
                .body("quantity", equalTo(12))
                .extract().path("itemID");
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(5)
    public void getOrderItem() {
        given().
        when().get("/salesOrders/getOrderItem?itemID=" + addedItemID).
              then().statusCode(200).assertThat()
              .body("productID", equalTo(productID))
              .body("quantity", equalTo(12));
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(6)
    public void removeOrderItems() {
        given().
        when().delete("/salesOrders/removeOrderItem?itemID=" + addedItemID).
        then().statusCode(200);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(7)
    public void changeOrderStatus() {
        given()
              .contentType("application/json")
              .body("{\"orderID\":" + addedOrderID + ",\"status\":2 }").
        when().put("/salesOrders/changeStatus").
        then().statusCode(200).assertThat()
                .body("orderID", equalTo(addedOrderID))       // Проверяем что orderID > 0
                .body("status", equalTo(2));           // Проверям что status=1 (новый заказ)

        given()
              .contentType("application/json")
              .body("{\"orderID\":" + addedOrderID + ",\"status\":1 }").
                when().put("/salesOrders/changeStatus").
                then().statusCode(200).assertThat()
                .body("orderID", equalTo(addedOrderID))       // Проверяем что orderID > 0
                .body("status", equalTo(1));           // Проверям что status=1 (новый заказ)
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(8)
    public void removeOrder() {
        given().
        when().delete("/salesOrders/removeOrder?orderID=" + addedOrderID).
        then().statusCode(200);
    }

    //---------------------------------------------------------------------------------------------------
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

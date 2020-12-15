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

    // todo добавить тесты на остатки, бронь и доступно для покупки

    private static final Logger LOG = Logger.getLogger(SalesOrdersServiceTest.class);

    private static int addedOrderID;
    private static int addedEntryID;

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    public void allOrders() {

        given()
                .when().get("/salesOrders")
                .then()
                .statusCode(200);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(2)
    public void addOrder() {
        addedOrderID =
        given().
        when().get("/salesOrders/addOrder").
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
    public void addOrderEntry() {
        addedEntryID =
        given().
        when().get("/salesOrders/addOrderEntry?orderID=" + addedOrderID + "&productID=2&amount=12").
        then().statusCode(200).assertThat()
                .body("productID", equalTo(2))
                .body("amount", equalTo(12))
                .extract().path("entryID");
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(5)
    public void getOrderEntry() {
        given().
        when().get("/salesOrders/getOrderEntry?entryID=" + addedEntryID).
              then().statusCode(200).assertThat()
              .body("productID", equalTo(2))
              .body("amount", equalTo(12));
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(6)
    public void removeOrderEntries() {
        given().
        when().get("/salesOrders/removeOrderEntry?entryID=" + addedEntryID).
        then().statusCode(200);
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(7)
    public void updateOrder() {
        given().
        when().get("/salesOrders/changeStatus?orderID=" + addedOrderID + "&status=2").
        then().statusCode(200).assertThat()
                .body("orderID", equalTo(addedOrderID))       // Проверяем что orderID > 0
                .body("status", equalTo(2));           // Проверям что status=1 (новый заказ)
        given().
                when().get("/salesOrders/changeStatus?orderID=" + addedOrderID + "&status=1").
                then().statusCode(200).assertThat()
                .body("orderID", equalTo(addedOrderID))       // Проверяем что orderID > 0
                .body("status", equalTo(1));           // Проверям что status=1 (новый заказ)
    }

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(8)
    public void removeOrder() {
        given().
        when().get("/salesOrders/removeOrder?orderID=" + addedOrderID).
        then().statusCode(200);
    }

}

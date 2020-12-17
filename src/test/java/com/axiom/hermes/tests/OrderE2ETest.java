package com.axiom.hermes.tests;

import com.axiom.hermes.tests.inventory.InventoryServiceTest;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.postgresql.core.v3.ConnectionFactoryImpl;

import java.util.LinkedHashMap;
import java.util.List;

import static com.axiom.hermes.model.customers.entities.SalesOrder.STATUS_CONFIRMED;
import static com.axiom.hermes.model.customers.entities.SalesOrder.STATUS_NEW;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderE2ETest {
    // todo добавить тесты на остатки, бронь и доступно для покупки
    //  1) Сохранить текущие остатки доступные для продажи
    //  2) Закупить товар
    //  3) Добавить клиента
    //  4) Создать заказ на клиента
    //  5) Добавть позицию заказа
    //  6) Установить статус - подтвердить заказ
    //  7) Проверить доступные остатки (должно быть)
    //  8) Отгрузить заказ
    //  9) Сверить остатки

    private static final Logger LOG = Logger.getLogger(OrderE2ETest.class);

    private static final int productID = 1;
    public static final int purchaseAmount = 30;
    public static final int buyAmount = 24;

    private static int initialStock;
    private static int orderID;
    private static int customerID;
    private static int addedEntryID;

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    public void getStockInformation() {
        initialStock =
                given()
                .when()
                    .get("/inventory/getStockInformation?productID=" + productID)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                .extract().path("stockOnHand");

        LOG.info("ProductID=" + productID + " initial stock:" + initialStock);
    }


    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(2)
    public void purchaseProduct() {
        String response =
                given()
                .when()
                    .get("/inventory/purchase?productID=" + productID +
                            "&amount=" + purchaseAmount + "&price=20")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("amount", equalTo(purchaseAmount))
                    .body("price", equalTo(20f))
                .extract().asString();
        LOG.info(response);
    }


    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(3)
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


    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(4)
    public void addNewOrder() {
        orderID =
                given().
                when().get("/salesOrders/addOrder?customerID=" + customerID).
                then().statusCode(200).assertThat()
                    .body("orderID", greaterThan(0))     // Проверяем что orderID > 0
                    .body("customerID", equalTo(customerID))  // Что создан на нужного клиента
                    .body("status", equalTo(STATUS_NEW))      // Проверям что status=1 (новый заказ)
                .extract().path("orderID");                   // Берём orderID

        LOG.info("Order added OrderID=" + orderID);

    }

    //---------------------------------------------------------------------------------------------------


    @Test
    @Order(5)
    public void addOrderEntries() {
        addedEntryID =
                given().
                when().get("/salesOrders/addOrderEntry?orderID=" + orderID +
                        "&productID=" + productID + "&amount=13").
                then().statusCode(200).assertThat()
                .body("productID", equalTo(productID))
                .body("amount", equalTo(13))
                .extract().path("entryID");

        LOG.info("OrderID=" + orderID + " entry added: " + addedEntryID);

        int amount = given().
                when().get("/salesOrders/updateOrderEntry?entryID=" + addedEntryID +
                "&productID=" + productID + "&amount=" + buyAmount).
                then().statusCode(200).assertThat()
                    .body("productID", equalTo(productID))
                    .body("amount", equalTo(buyAmount))
                .extract().path("amount");

        LOG.info("OrderID=" + orderID + " entry updated: " + addedEntryID + " amount=" + amount);
    }

    //---------------------------------------------------------------------------------------------------
    @Test
    @Order(6)
    public void changeOrderStatus() {
        // Меняем статус заказа на подтвержденный
        given().
            when().get(
                    "/salesOrders/changeStatus?orderID=" + orderID +
                            "&status=" + STATUS_CONFIRMED).
            then()
                .assertThat()
                .statusCode(200)
                .body("orderID", equalTo(orderID))           // Проверяем верный ли orderID
                .body("status", equalTo(STATUS_CONFIRMED));  // Проверям что заказ подтвержден

        String response = given()
            .when()
                .get("/inventory/getStockInformation?productID=" + productID)
            .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("stockOnHand", equalTo(initialStock + purchaseAmount))
                .body("committedStock", equalTo(buyAmount))
                .body("availableForSale", equalTo(initialStock + purchaseAmount - buyAmount))
            .extract().asString();

        LOG.info("ProductID="+ productID + " stock information:\n" + response);

    }

    //--------------------------------------------------------------------------------------------------
    @Test
    @Order(7)
    public void checkAvailableForSale() {
        int availableForSale = given()
            .when()
                .get("/inventory/getStockInformation?productID=" + productID)
            .then()
            .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("stockOnHand", equalTo(initialStock + purchaseAmount))
                .body("committedStock", equalTo(buyAmount))
                .body("availableForSale", equalTo(initialStock + purchaseAmount - buyAmount))
            .extract().jsonPath().getInt("availableForSale");

        String response =
                given()
                .when()
                    .get("/inventory/sale?productID=" + productID + "&amount=" + availableForSale +
                            "&price=30")
                .then()
                    .assertThat()
                    .statusCode(200)
                .extract().asString();
        LOG.info(response);
    }

    //--------------------------------------------------------------------------------------------------
    @Test
    @Order(8)
    public void checkFulfillment() {

        // Взять список подтвержденных заказов и взять самый ранний заказ
        List<LinkedHashMap<String, Object>> ordersList =
        given()
        .when()
            .get("/salesOrders?status=" + STATUS_CONFIRMED)
        .then()
            .statusCode(200)
        .extract().jsonPath().getList("$");
        LOG.info("Get all confirmed orders - total:" + ordersList.size());

        // взять очередной заказ требующий исполнения
        int myOrderID  = (Integer) ordersList.get(0).get("orderID");
        LOG.info("Retrieving orderID=" + myOrderID);

        // получить список позиций требующих исполнения
        List<LinkedHashMap<String, Object>> entriesList =
            given()
            .when()
                .get("/salesOrders/getOrderEntries?orderID=" + myOrderID)
            .then()
                .statusCode(200)
            .extract().jsonPath().getList("$");
        LOG.info("OrderID=" + myOrderID + " has " + entriesList.size() + " items");

        int entryID, entryProductID, amount;
        float price;

        // осуществить отгрузки позиций
        for (LinkedHashMap<String, Object> entry:entriesList) {

            entryID = (Integer) entry.get("entryID");
            entryProductID = (Integer) entry.get("productID");
            amount = (Integer) entry.get("amount");
            price = (Float) entry.get("price");

            LOG.info("EntryID=" + entryID +
                     " productID=" + entryProductID +
                     " amount=" + amount +
                     " price=" + price);

            String response =
                    given()
                    .when()
                        .get("/inventory/sale?orderID=" + orderID +
                                        "&productID=" + entryProductID +
                                        "&amount=" + amount)
                    .then()
                        .assertThat()
                        .statusCode(200)
                        .body("productID", equalTo(entryProductID))
                        .body("amount", equalTo(amount))
                    .extract().asString();
            LOG.info("Inventory transaction:\n" + response);

            // удостовериться в исполнении заказа
            if (entriesList.size() > 0) {
                response =
                given()
                .when()
                    .get("/inventory/getStockInformation?productID=" + entryProductID)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("committedStock", equalTo(0))
                    .body("availableForSale", equalTo(0))
                .extract().asString();
                LOG.info("Stock information:\n" + response);
            }
        }


    }
    //--------------------------------------------------------------------------------------------------

    @Test
    @Order(15)
    public void cleanup() {

        // Изменить статус заказа на удаляемый
        given().
        when()
            .get("/salesOrders/changeStatus?orderID=" + orderID + "&status=" + STATUS_NEW).
        then()
            .assertThat()
            .statusCode(200)
            .body("orderID", equalTo(orderID))           // Проверяем верный ли orderID
            .body("status", equalTo(STATUS_NEW));  // Проверям что заказ подтвержден
        // Удалить заказ
        given().
                when().get("/salesOrders/removeOrder?orderID=" + orderID).
                then().statusCode(200);
        // Удалить клиента
        given().
                when().get("/customers/removeCustomer?customerID=" + customerID).
                then().statusCode(200).extract().asString();

        int availableForSale = given()
                .when()
                .get("/inventory/getStockInformation?productID=" + productID)
                .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .extract().jsonPath().getInt("availableForSale");

        if (availableForSale>0) {
            String response =
                    given()
                            .when()
                            .get("/inventory/writeOff?productID=" + productID +
                                    "&amount=" + availableForSale + "&price=20")
                            .then()
                            .assertThat()
                            .statusCode(200)
                            .body("productID", equalTo(productID))
                            .body("amount", equalTo(availableForSale))
                            .body("price", equalTo(20f))
                            .extract().asString();
            LOG.info(response);
        }
    }

}

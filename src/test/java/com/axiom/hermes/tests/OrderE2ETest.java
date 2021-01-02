package com.axiom.hermes.tests;

import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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

    private static final Logger LOG = Logger.getLogger(OrderE2ETest.class);


    public static final int purchasequantity = 30;
    public static final int buyQuantity = 24;

    private static int productID;
    private static int initialStock;
    private static int orderID;
    private static int customerID;

    //---------------------------------------------------------------------------------------------------

    @Test
    @Order(1)
    public void getStockCard() {


        // Добавить продукт
        productID =
                given()
                        .header("Content-Type", "application/json")
                        .body("{\n" +
                                "    \"name\": \"CUP OF COFFEE\",\n" +
                                "    \"description\": \"MACCOFFEE\",\n" +
                                "    \"price\": 5,\n" +
                                "    \"vendorCode\": \"CCMAC\",\n" +
                                "    \"available\": true" +
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

        initialStock =
                given()
                .when()
                    .get("/inventory/getStockCard?productID=" + productID)
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
                            "&quantity=" + purchasequantity + "&price=20")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("quantity", equalTo(purchasequantity))
                    .body("price", equalTo(20f))
                .extract().asString();
        LOG.info(makePretty(response));
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
    public void addOrderItems() {
        int addedItemID = given().
                when().get("/salesOrders/addOrderItem?orderID=" + orderID +
                "&productID=" + productID + "&quantity=13").
                then().statusCode(200).assertThat()
                .body("productID", equalTo(productID))
                .body("quantity", equalTo(13))
                .extract().path("itemID");

        LOG.info("OrderID=" + orderID + " item added: " + addedItemID);

        String updateItemJson = "{\n" +
                "\"itemID\": " + addedItemID + ",\n" +
                "\"productID\":" + productID + ",\n" +
                "\"quantity\":" + buyQuantity + "\n" + "}";
        int quantity =
                given()
                    .contentType("application/json")
                    .body(updateItemJson).
                when().put("/salesOrders/updateOrderItem").
                then().statusCode(200).assertThat()
                    .body("productID", equalTo(productID))
                    .body("quantity", equalTo(buyQuantity))
                .extract().path("quantity");

        LOG.info("OrderID=" + orderID + " item updated: " + addedItemID + " quantity=" + quantity);
    }

    //---------------------------------------------------------------------------------------------------
    @Test
    @Order(6)
    public void changeOrderStatus() {
        // Меняем статус заказа на подтвержденный
        given()
                .contentType("application/json")
                .body("{\"orderID\":" + orderID + ",\"status\":" + STATUS_CONFIRMED + "}").
            when().put("/salesOrders/changeStatus").
            then()
                .assertThat()
                .statusCode(200)
                .body("orderID", equalTo(orderID))           // Проверяем верный ли orderID
                .body("status", equalTo(STATUS_CONFIRMED));  // Проверям что заказ подтвержден

        String response = given()
            .when()
                .get("/inventory/getStockCard?productID=" + productID)
            .then()
                .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("stockOnHand", equalTo(initialStock + purchasequantity))
                .body("committedStock", equalTo(buyQuantity))
                .body("availableForSale", equalTo(initialStock + purchasequantity - buyQuantity))
            .extract().asString();

        LOG.info("ProductID="+ productID + " stock information:\n" + makePretty(response));

    }

    //--------------------------------------------------------------------------------------------------
    @Test
    @Order(7)
    public void checkAvailableForSale() {
        int availableForSale = given()
            .when()
                .get("/inventory/getStockCard?productID=" + productID)
            .then()
            .assertThat()
                .statusCode(200)
                .body("productID", equalTo(productID))
                .body("stockOnHand", equalTo(initialStock + purchasequantity))
                .body("committedStock", equalTo(buyQuantity))
                .body("availableForSale", equalTo(initialStock + purchasequantity - buyQuantity))
            .extract().jsonPath().getInt("availableForSale");

        String response =
                given()
                .when()
                    .get("/inventory/sale?productID=" + productID + "&quantity=" + (availableForSale+1) +
                            "&price=30")
                .then()
                    .assertThat()
                    .statusCode(404)
                .extract().asString();
        LOG.info("Trying to sell more than available:\"" + makePretty(response));
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
        List<LinkedHashMap<String, Object>> itemsList =
            given()
            .when()
                .get("/salesOrders/getOrderItems?orderID=" + myOrderID)
            .then()
                .statusCode(200)
            .extract().jsonPath().getList("$");
        LOG.info("OrderID=" + myOrderID + " has " + itemsList.size() + " items");

        int itemID, itemProductID, quantity;
        float price;

        // осуществить отгрузки позиций
        for (LinkedHashMap<String, Object> item:itemsList) {

            itemID = (Integer) item.get("itemID");
            itemProductID = (Integer) item.get("productID");
            quantity = (Integer) item.get("quantity");
            price = (Float) item.get("price");

            LOG.info("ItemID=" + itemID +
                     " productID=" + itemProductID +
                     " quantity=" + quantity +
                     " price=" + price);

            String response =
                    given()
                    .when()
                        .get("/inventory/sale?orderID=" + orderID +
                                        "&productID=" + itemProductID +
                                        "&quantity=" + quantity)
                    .then()
                        .assertThat()
                        .statusCode(200)
                        .body("productID", equalTo(itemProductID))
                        .body("quantity", equalTo(quantity))
                    .extract().asString();
            LOG.info("Inventory transaction:\n" + makePretty(response));

            // удостовериться в исполнении заказа
            if (itemsList.size() > 0) {
                response =
                given()
                .when()
                    .get("/inventory/getStockCard?productID=" + itemProductID)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("productID", equalTo(productID))
                    .body("committedStock", equalTo(0))
                .extract().asString();
                LOG.info("Stock information:\n" + makePretty(response));
            }
        }


    }
    //--------------------------------------------------------------------------------------------------

    @Test
    @Order(9)
    public void cleanup() {

        // Изменить статус заказа на удаляемый
        given()
            .contentType("application/json")
            .body("{\"orderID\":" + orderID + ",\"status\":" + STATUS_NEW + "}").
        when()
            .put("/salesOrders/changeStatus").
        then()
            .assertThat()
            .statusCode(200)
            .body("orderID", equalTo(orderID))           // Проверяем верный ли orderID
            .body("status", equalTo(STATUS_NEW));  // Проверям что заказ подтвержден
        // Удалить заказ
        given().
                when().delete("/salesOrders/removeOrder?orderID=" + orderID).
                then().statusCode(200);
        // Удалить клиента
        given().
                when().delete("/customers/removeCustomer?customerID=" + customerID).
                then().statusCode(200).extract().asString();

        int availableForSale = given()
                .when()
                .get("/inventory/getStockCard?productID=" + productID)
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
                                    "&quantity=" + availableForSale + "&price=20")
                            .then()
                            .assertThat()
                            .statusCode(200)
                            .body("productID", equalTo(productID))
                            .body("quantity", equalTo(availableForSale))
                            .body("price", equalTo(20f))
                            .extract().asString();
            LOG.info(makePretty(response));
        }
    }

    //--------------------------------------------------------------------------------------------------
    @Test
    @Order(10)
    public void getProductTransactions() {
        String response =
                given()
                        .when()
                        .get("/inventory/getProductTransactions?" +
                                "productID=" + productID /*+
                                "&quantity=" + quantity*/)
                        .then()
                        .assertThat()
                        .statusCode(200)
                        .extract().asString();
        LOG.info("Inventory get product transactions:\n" + makePretty(response));
    }

    //--------------------------------------------------------------------------------------------------
    private String makePretty(String response) {
        StringBuffer sb = new StringBuffer(response);
        for (int i=0; i<sb.length(); i++) {
            if (sb.charAt(i)==',') {
                sb.insert(i+1, '\n');
            }
        }
        return sb.toString();
    }

}

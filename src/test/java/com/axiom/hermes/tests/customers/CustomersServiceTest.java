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



    @Test
    @Order(2)
    public void addCustomer() {
        // todo добавление клиента
    }

    @Test
    @Order(3)
    public void getCustomer() {
        // todo получение клиента по CustomerID
    }

    @Test
    @Order(4)
    public void getCustomerByMobile() {
        // todo получение клиента по мобильному номеру
    }

    @Test
    @Order(5)
    public void updateCustomer() {
        // todo изменение данных клиента
    }


    @Test
    @Order(6)
    public void removeCustomer() {
        // todo удаление клиента
    }
}

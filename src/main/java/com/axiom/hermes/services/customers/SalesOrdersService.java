package com.axiom.hermes.services.customers;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.model.customers.SalesOrders;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderItem;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Сервис управления клиентскими заказами
 */
@Path("/salesOrders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SalesOrdersService {


    @Inject
    SalesOrders salesOrders;

    public SalesOrdersService() { }

    /**
     * Получить список всех заказов
     * @param startTime временная метка начала периода
     * @param endTime временная метка конца периода
     * @param status статус искомых заказов (0 - любой статус)
     * @return список заказов
     * @throws HermesException информация об ошибке
     */
    @GET
    public Response getAllOrders(@QueryParam("startTime") long startTime,
                                 @QueryParam("endTime") long endTime,
                                 @QueryParam("status") int status) throws HermesException {
        List<SalesOrder> orders = salesOrders.getAllOrders(startTime, endTime, status);
        return Response.ok(orders).build();
    }

    /**
     * Получить список всех карточек заказов клиента
     * @param customerID клиента
     * @param status статус искомых заказов (0 - любой статус)
     * @return список заказов
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getOrders")
    public Response getOrders(@QueryParam("customerID") long customerID, @QueryParam("status") int status)
    throws HermesException{
        List<SalesOrder> orders = salesOrders.getOrders(customerID, status);
        return Response.ok(orders).build();
    }

    /**
     * Создать новую карточку заказа от имени клиента
     * @param customerID клиента
     * @return карточка созданного заказа
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/addOrder") // todo здесь должен быть POST
    public Response addOrder(@QueryParam("customerID") long customerID) throws HermesException {
        SalesOrder order = salesOrders.addOrder(customerID);
        return Response.ok(order).build();
    }

    /**
     * Получить карточку заказа по его ID
     * @param orderID заказа
     * @return карточка заказа
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getOrder")
    public Response getOrder(@QueryParam("orderID") long orderID) throws HermesException {
        SalesOrder order = salesOrders.getOrder(orderID);
        return Response.ok(order).build();
    }

    /**
     * Изменить статус заказа
     * @param salesOrder заказ с новым статусом
     * @return обновленная карточка заказа
     * @throws HermesException информация об ошибке
     */
    @PUT
    @Path("/changeStatus")
    public Response changeStatus(SalesOrder salesOrder) throws HermesException {
        SalesOrder order = salesOrders.changeStatus(salesOrder.getOrderID(), salesOrder.getStatus());
        return Response.ok(order).build();
    }

    /**
     * Удалить заказ если в нём нет позиций
     * @param orderID заказа
     * @return 200 ОК если заказ удален
     * @throws HermesException информация об ошибке
     */
    @DELETE
    @Path("/removeOrder")
    public Response removeOrder(@QueryParam("orderID") long orderID) throws HermesException {
        salesOrders.removeOrder(orderID);
        return Response.ok().build();
    }

    //------------------------------------------------------------------------------------------------------
    // Управление позициями заказа
    //------------------------------------------------------------------------------------------------------

    /**
     * Получить позиции заказа
     * @param orderID заказа
     * @return список позиций заказа
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getOrderItems")
    public Response getOrderItems(@QueryParam("orderID") long orderID) throws HermesException {
        List<SalesOrderItem> entries = salesOrders.getOrderItems(orderID);
        return Response.ok(entries).build();
    }

    /**
     * Получить позицию заказа по ID позиции
     * @param itemID позиции заказа
     * @return позиция заказа
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/getOrderItem")
    public Response getOrderItem(@QueryParam("itemID") long itemID) throws HermesException {
        SalesOrderItem entry = salesOrders.getOrderItem(itemID);
        return Response.ok(entry).build();
    }

    /**
     * Добавить позицию заказа
     * @param orderID заказа
     * @param productID товара
     * @param quantity количество
     * @return созданная позиция заказа
     * @throws HermesException информация об ошибке
     */
    @GET
    @Path("/addOrderItem") // todo тут должен быть POST
    public Response addOrderItem(
            @QueryParam("orderID") long orderID,
            @QueryParam("productID") long productID,
            @QueryParam("quantity") long quantity) throws HermesException {
        SalesOrderItem entry = salesOrders.addOrderItem(orderID, productID, quantity);
        return Response.ok(entry).build();
    }

    /**
     * Обновляет позицию закза (код товара и/или количество)
     * @param item измененная позиция заказа
     * @return обновленная позиция заказа
     * @throws HermesException информация об ошибке
     */
    @PUT
    @Path("/updateOrderItem")
    public Response updateOrderItem(SalesOrderItem item) throws HermesException {
        SalesOrderItem entry = salesOrders.updateOrderItem(item.getItemID(), item.getProductID(), item.getquantity());
        return Response.ok(entry).build();
    }

    /**
     * Удаляет позиции заказа
     * @param itemID позиции заказа
     * @return 200 ОК если удалена успешно
     * @throws HermesException информация об ошибке
     */
    @DELETE
    @Path("/removeOrderItem")
    public Response removeOrderItem(@QueryParam("itemID") long itemID) throws HermesException {
        salesOrders.removeOrderItem(itemID);
        return Response.ok().build();
    }

}

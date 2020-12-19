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

    @GET
    public Response getAllOrders(@QueryParam("startTime") long startTime,
                                 @QueryParam("endTime") long endTime,
                                 @QueryParam("status") int status) {
        List<SalesOrder> orders = salesOrders.getAllOrders(startTime, endTime, status);
        return Response.ok(orders).build();
    }

    @GET
    @Path("/getOrders")
    public Response getOrders(@QueryParam("customerID") long customerID, @QueryParam("status") int status) {
        List<SalesOrder> orders = salesOrders.getOrders(customerID, status);
        return Response.ok(orders).build();
    }

    @GET
    @Path("/addOrder")
    public Response addOrder(@QueryParam("customerID") long customerID) throws HermesException {
        SalesOrder order = salesOrders.addOrder(customerID);
        return Response.ok(order).build();
    }

    @GET
    @Path("/getOrder")
    public Response getOrder(@QueryParam("orderID") long orderID) throws HermesException {
        SalesOrder order = salesOrders.getOrder(orderID);
        return Response.ok(order).build();
    }

    @GET
    @Path("/changeStatus")
    public Response changeStatus(@QueryParam("orderID") long orderID, @QueryParam("status") int status)
        throws HermesException {
        SalesOrder order = salesOrders.changeStatus(orderID, status);
        return Response.ok(order).build();
    }

    @GET
    @Path("/removeOrder")
    public Response removeOrder(@QueryParam("orderID") long orderID) throws HermesException {
        salesOrders.removeOrder(orderID);
        return Response.ok().build();
    }

    //------------------------------------------------------------------------------------------------------

    @GET
    @Path("/getOrderEntries")
    public Response getOrderEntries(@QueryParam("orderID") long orderID) {
        List<SalesOrderItem> entries = salesOrders.getOrderEntries(orderID);
        return Response.ok(entries).build();
    }

    @GET
    @Path("/getOrderEntry")
    public Response getOrderEntry(@QueryParam("entryID") long entryID) throws HermesException {
        SalesOrderItem entry = salesOrders.getOrderEntry(entryID);
        return Response.ok(entry).build();
    }

    @GET
    @Path("/addOrderEntry")
    public Response addOrderEntry(
            @QueryParam("orderID") long orderID,
            @QueryParam("productID") long productID,
            @QueryParam("quantity") long quantity) throws HermesException {
        SalesOrderItem entry = salesOrders.addOrderEntry(orderID, productID, quantity);
        return Response.ok(entry).build();
    }

    @GET
    @Path("/updateOrderEntry")
    public Response updateOrderEntry(
            @QueryParam("entryID") long entryID,
            @QueryParam("productID") long newProductID,
            @QueryParam("quantity") long newquantity) throws HermesException {
        SalesOrderItem entry = salesOrders.updateOrderEntry(entryID, newProductID, newquantity);
        return Response.ok(entry).build();
    }

    @GET
    @Path("/removeOrderEntry")
    public Response removeOrderEntry(@QueryParam("entryID") long entryID) throws HermesException {
        boolean result = salesOrders.removeOrderEntry(entryID);
        if (!result) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

}

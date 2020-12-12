package com.axiom.hermes.services.customers;

import com.axiom.hermes.model.customers.SalesOrders;
import com.axiom.hermes.model.customers.entities.SalesOrder;
import com.axiom.hermes.model.customers.entities.SalesOrderEntry;

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
    public Response getAllOrders() {
        List<SalesOrder> orders = salesOrders.getAllOrders();
        if (orders==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(orders).build();
    }

    @GET
    @Path("/getOrders")
    public Response getOrders(@QueryParam("customerID") int customerID, @QueryParam("status") int status) {
        List<SalesOrder> orders = salesOrders.getOrders(customerID, status);
        if (orders==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(orders).build();
    }

    @GET
    @Path("/addOrder")
    public Response addOrder(@QueryParam("customerID") int customerID) {
        SalesOrder order = salesOrders.addOrder(customerID);
        if (order==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(order).build();
    }

    @GET
    @Path("/getOrder")
    public Response getOrder(@QueryParam("orderID") long orderID) {
        SalesOrder order = salesOrders.getOrder(orderID);
        if (order==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(order).build();
    }

    @GET
    @Path("/changeStatus")
    public Response changeStatus(@QueryParam("orderID") long orderID, @QueryParam("status") int status) {
        SalesOrder order = salesOrders.changeStatus(orderID, status);
        if (order==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(order).build();
    }

    @GET
    @Path("/removeOrder")
    public Response removeOrder(@QueryParam("orderID") long orderID) {
        boolean result = salesOrders.removeOrder(orderID);
        if (!result) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

    //------------------------------------------------------------------------------------------------------

    @GET
    @Path("/getOrderEntries")
    public Response getOrderEntries(@QueryParam("orderID") long orderID) {
        List<SalesOrderEntry> entries = salesOrders.getOrderEntries(orderID);
        if (entries==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(entries).build();
    }

    @GET
    @Path("/getOrderEntry")
    public Response getOrderEntry(@QueryParam("entryID") long entryID) {
        SalesOrderEntry entry = salesOrders.getOrderEntry(entryID);
        if (entry==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(entry).build();
    }

    @GET
    @Path("/addOrderEntry")
    public Response addOrderEntry(
            @QueryParam("entryID") long orderID,
            @QueryParam("productID") int productID,
            @QueryParam("amount") int amount) {
        SalesOrderEntry entry = salesOrders.addOrderEntry(orderID, productID, amount);
        if (entry==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(entry).build();
    }

    @GET
    @Path("/updateOrderEntry")
    public Response updateOrderEntry(
            @QueryParam("entryID") long entryID,
            @QueryParam("productID") int newProductID,
            @QueryParam("amount") int newAmount) {
        SalesOrderEntry entry = salesOrders.updateOrderEntry(entryID, newProductID, newAmount);
        if (entry==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(entry).build();
    }

    @GET
    @Path("/removeOrderEntry")
    public Response removeOrderEntry(long entryID) {
        boolean result = salesOrders.removeOrderEntry(entryID);
        if (!result) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok().build();
    }

}

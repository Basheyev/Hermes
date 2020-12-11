package com.axiom.hermes.services.inventory;

import com.axiom.hermes.model.inventory.Inventory;
import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryService {

    @Inject
    Inventory inventory;

    public InventoryService() { }

    @GET
    @Path("/purchase")
    public Response purchase(@QueryParam("id") int productID,
                             @QueryParam("amount") int amount,
                             @QueryParam("price") double price) {
        StockTransaction purchase = inventory.purchase(productID, amount, price);
        if (purchase==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(purchase).build();
    }

    @GET
    @Path("/saleReturn")
    public Response saleReturn(@QueryParam("id") int productID,
                               @QueryParam("amount") int amount,
                               @QueryParam("price") double price) {
        StockTransaction saleReturn = inventory.saleReturn(productID, amount, price);
        if (saleReturn==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(saleReturn).build();
    }

    @GET
    @Path("/sale")
    public Response sale(@QueryParam("id") int productID,
                         @QueryParam("amount") int amount,
                         @QueryParam("price") double price) {
        StockTransaction sale = inventory.sale(productID, amount, price);
        if (sale==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(sale).build();
    }

    @GET
    @Path("/purchaseReturn")
    public Response purchaseReturn(@QueryParam("id") int productID,
                                   @QueryParam("amount") int amount,
                                   @QueryParam("price") double price) {
        StockTransaction purchaseReturn = inventory.purchaseReturn(productID, amount, price);
        if (purchaseReturn==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(purchaseReturn).build();
    }

    @GET
    @Path("/writeOff")
    public Response writeOff(@QueryParam("id") int productID,
                             @QueryParam("amount") int amount,
                             @QueryParam("price") double price) {
        StockTransaction writeOff = inventory.writeOff(productID, amount, price);
        if (writeOff==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(writeOff).build();
    }

    @GET
    @Path("/getStockInformation")
    public Response getStockInformation(@QueryParam("id") int productID) {
        StockInformation stockInfo = inventory.getStockInformation(productID);
        if (stockInfo==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(stockInfo).build();
    }

}

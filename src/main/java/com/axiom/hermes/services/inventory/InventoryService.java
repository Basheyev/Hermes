package com.axiom.hermes.services.inventory;

import com.axiom.hermes.model.inventory.Inventory;
import com.axiom.hermes.model.inventory.entities.StockInformation;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryService {

    @Inject
    Inventory inventory;

    public InventoryService() { }

    @GET
    @Path("/purchase")
    public StockTransaction purchase(@QueryParam("id") int productID,
                                     @QueryParam("amount") int amount,
                                     @QueryParam("price") double price) {
        return inventory.purchase(productID, amount, price);
    }

    @GET
    @Path("/saleReturn")
    public StockTransaction saleReturn(@QueryParam("id") int productID,
                                       @QueryParam("amount") int amount,
                                       @QueryParam("price") double price) {
        return inventory.saleReturn(productID, amount, price);
    }

    @GET
    @Path("/sale")
    public StockTransaction sale(@QueryParam("id") int productID,
                                 @QueryParam("amount") int amount,
                                 @QueryParam("price") double price) {
        return inventory.sale(productID, amount, price);
    }

    @GET
    @Path("/purchaseReturn")
    public StockTransaction purchaseReturn(@QueryParam("id") int productID,
                                           @QueryParam("amount") int amount,
                                           @QueryParam("price") double price) {
        return inventory.purchaseReturn(productID, amount, price);
    }

    @GET
    @Path("/writeOff")
    public StockTransaction writeOff(@QueryParam("id") int productID,
                                     @QueryParam("amount") int amount,
                                     @QueryParam("price") double price) {
        return inventory.writeOff(productID, amount, price);
    }

    @GET
    @Path("/getStockInformation")
    public StockInformation getStockInformation(@QueryParam("id") int productID) {
        return inventory.getStockInformation(productID);
    }

}

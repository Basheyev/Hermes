package com.axiom.hermes.services.inventory;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.model.inventory.Inventory;
import com.axiom.hermes.model.inventory.entities.StockCard;
import com.axiom.hermes.model.inventory.entities.StockTransaction;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Сервис управления складским учетом
 */
@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryService {

    @Inject
    Inventory inventory;

    public InventoryService() { }

    @GET
    public Response getAllStocks() {
        List<StockCard> allStock = inventory.getAllStocks();
        if (allStock==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(allStock).build();
    }

    /**
     * Возвращает список складских карточек товарных позиций по которым требуется пополнение запасов
     * @return список складских карточек товарных позиций требующих пополнения запасов
     */
    @GET
    @Path("/getReplenishmentStocks")
    public Response getReplenishmentStocks() {
        List<StockCard> stocks = inventory.getReplenishmentStocks();
        if (stocks==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(stocks).build();
    }


    @GET
    @Path("/purchase")
    public Response purchase(@QueryParam("orderID") long orderID,
                             @QueryParam("productID") long productID,
                             @QueryParam("quantity") long quantity,
                             @QueryParam("price") double price) throws HermesException {
        StockTransaction purchase = inventory.purchase(orderID, productID, quantity, price);
        if (purchase==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(purchase).build();
    }

    @GET
    @Path("/saleReturn")
    public Response saleReturn(@QueryParam("orderID") long orderID,
                               @QueryParam("productID") long productID,
                               @QueryParam("quantity") long quantity,
                               @QueryParam("price") double price) throws HermesException {
        StockTransaction saleReturn = inventory.saleReturn(orderID, productID, quantity, price);
        if (saleReturn==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(saleReturn).build();
    }

    @GET
    @Path("/sale")
    public Response sale(@QueryParam("orderID") long orderID,
                         @QueryParam("productID") long productID,
                         @QueryParam("quantity") long quantity) throws HermesException {
        StockTransaction sale = inventory.sale(orderID, productID, quantity);
        if (sale==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(sale).build();
    }

    @GET
    @Path("/purchaseReturn")
    public Response purchaseReturn(@QueryParam("orderID") long orderID,
                                   @QueryParam("productID") long productID,
                                   @QueryParam("quantity") long quantity,
                                   @QueryParam("price") double price) throws HermesException {
        StockTransaction purchaseReturn = inventory.purchaseReturn(orderID, productID, quantity, price);
        if (purchaseReturn==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(purchaseReturn).build();
    }

    @GET
    @Path("/writeOff")
    public Response writeOff(@QueryParam("orderID") long orderID,
                             @QueryParam("productID") long productID,
                             @QueryParam("quantity") long quantity,
                             @QueryParam("price") double price) throws HermesException {
        StockTransaction writeOff = inventory.writeOff(orderID, productID, quantity, price);
        if (writeOff==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(writeOff).build();
    }

    /**
     * Получить все складские транзакции по указанному заказу
     * @param orderID заказа
     * @return список складских транзакций по указанному заказу
     */
    @GET
    @Path("/getOrderTransactions")
    public Response getOrderTransactions(@QueryParam("orderID") long orderID) throws HermesException  {
        List<StockTransaction> orderTransactions = inventory.getOrderTransactions(orderID);
        return Response.ok(orderTransactions).build();
    }

    /**
     * Получит складские транзакции по указанной товарной позиции в указанный период
     * @param productID товарной позиции
     * @param startTime с какого времени
     * @param endTime по какое время
     * @return список складских транзакций по указнной товарной позиции в указанный период
     */
    @GET
    @Path("/getProductTransactions")
    public Response getProductTransactions(
            @QueryParam("productID") long productID,
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime) throws HermesException {
        List<StockTransaction> productTransactions;
        productTransactions = inventory.getProductTransactions(productID, startTime, endTime);
        return Response.ok(productTransactions).build();
    }

    @GET
    @Path("/getStockCard")
    public Response getStockCard(@QueryParam("productID") long productID) throws HermesException {
        StockCard stockInfo = inventory.getStockCard(productID);
        return Response.ok(stockInfo).build();
    }

}

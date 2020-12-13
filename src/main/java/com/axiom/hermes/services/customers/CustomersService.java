package com.axiom.hermes.services.customers;

import com.axiom.hermes.model.customers.Customers;
import com.axiom.hermes.model.customers.entities.Customer;
import com.axiom.hermes.model.customers.entities.SalesOrder;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomersService {

    @Inject
    Customers customers;

    @GET
    public Response getAllCustomers() {
        List<Customer> allCustomers = customers.getAllCustomers();
        if (allCustomers==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(allCustomers).build();
    }

    @GET
    @Path("/getCustomer")
    public Response getCustomer(@QueryParam("customerID") int customerID) {
        Customer customer = customers.getCustomer(customerID);
        if (customer==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(customer).build();
    }

    @GET
    @Path("/getCustomerByMobile")
    public Response getCustomerByMobile(@QueryParam("mobile") String mobile) {
        Customer customer = customers.getCustomerByMobile(mobile);
        if (customer==null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(customer).build();
    }

    @POST
    @Path("/addCustomer")
    public Response addCustomer(Customer customer) {
        // todo реализовать добавление пользователя
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/updateCustomer")
    public Response updateCustomer(Customer customer) {
        // todo реализовать редактирование пользователя
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/removeCustomer")
    public Response removeCustomer(@QueryParam("customerID")int customerID) {
        // todo реализовать удаление пользователя
        return Response.status(Response.Status.NOT_FOUND).build();
    }

}

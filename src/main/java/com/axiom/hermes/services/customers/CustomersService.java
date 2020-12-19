package com.axiom.hermes.services.customers;

import com.axiom.hermes.common.exceptions.HermesException;
import com.axiom.hermes.model.catalogue.entities.Product;
import com.axiom.hermes.model.customers.Customers;
import com.axiom.hermes.model.customers.entities.Customer;
import com.axiom.hermes.model.customers.entities.SalesOrder;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Сервис управления клиентами
 */
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

    @POST
    @Path("/addCustomer")
    public Response addCustomer(Customer newCustomer) throws HermesException {
        Customer customer = customers.addCustomer(newCustomer);
        if (customer==null) return Response.status(Response.Status.FORBIDDEN).build();
        return Response.ok(customer).build();
    }

    @GET
    @Path("/getCustomer")
    public Response getCustomer(@QueryParam("customerID") long customerID) throws HermesException {
        Customer customer = customers.getCustomer(customerID);
        return Response.ok(customer).build();
    }

    @GET
    @Path("/getCustomerByMobile")
    public Response getCustomerByMobile(@QueryParam("mobile") String mobile) throws HermesException {
        Customer customer = customers.getCustomerByMobile(mobile);
        return Response.ok(customer).build();
    }

    @POST
    @Path("/updateCustomer")
    public Response updateCustomer(Customer customer) throws HermesException {
        Customer managed = customers.updateCustomer(customer);
        return Response.ok(managed).build();
    }

    @GET
    @Path("/removeCustomer")
    public Response removeCustomer(@QueryParam("customerID") long customerID) throws HermesException {
        customers.removeCustomer(customerID);
        return Response.ok().build();
    }

}

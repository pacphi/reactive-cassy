package io.pivotal.customer;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class CustomerResourceAssembler implements RepresentationModelAssembler<Customer, EntityModel<Customer>> {

	@Override
	public EntityModel<Customer> toModel(Customer customer) {
		return EntityModel.of(customer)
			.add(linkTo(methodOn(CustomerEndpoints.class).getCustomerById(customer.getId())).withSelfRel())
			.add(linkTo(methodOn(CustomerEndpoints.class).streamAllCustomers()).withRel("customers"));
	}

	public CollectionModel<EntityModel<Customer>> toCollectionModel(Iterable<? extends Customer> customers) {
		List<EntityModel<Customer>> customerModels = customers instanceof List ?
			((List<Customer>) customers).stream().map(this::toModel).toList() :
			java.util.stream.StreamSupport.stream(customers.spliterator(), false).map(this::toModel).toList();

		return CollectionModel.of(customerModels)
			.add(linkTo(methodOn(CustomerEndpoints.class).streamAllCustomers()).withSelfRel());
	}
}
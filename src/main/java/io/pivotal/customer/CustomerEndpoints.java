package io.pivotal.customer;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class CustomerEndpoints {

	private final CustomerRepository repository;
	private final CustomerResourceAssembler assembler;

	public CustomerEndpoints(
			CustomerRepository repository,
			CustomerResourceAssembler assembler) {
		this.repository = repository;
		this.assembler = assembler;
	}

	@GetMapping(value = "/", produces = MediaTypes.HAL_JSON_VALUE)
	public RepresentationModel<?> root() {

		RepresentationModel<?> rootResource = new RepresentationModel<>();

		rootResource.add(
			linkTo(methodOn(CustomerEndpoints.class).root()).withSelfRel(),
			linkTo(methodOn(CustomerEndpoints.class).streamAllCustomers()).withRel("stream/customers"));

		// TODO add other links
		return rootResource;
	}

	@PostMapping("/customers")
	Mono<ResponseEntity<EntityModel<Customer>>> newCustomer(@RequestBody Customer customer) throws URISyntaxException {
		Mono<Customer> savedCustomer = repository.save(customer);
		return savedCustomer.map(c ->
						ResponseEntity
							.created(
								Optional.ofNullable(c.getId())
									.map(id -> linkTo(methodOn(CustomerEndpoints.class).getCustomerById(id)).toUri())
									.orElseThrow(() -> new RuntimeException("Failed to create customer for some reason")))
							.body(assembler.toModel(c)));
	}

	@PutMapping("/customers/{id}")
    public Mono<ResponseEntity<EntityModel<Customer>>> updateCustomer(@PathVariable UUID id,
                                                   @RequestBody Customer customer) {
        Mono<Customer> existingCustomer = repository.findById(id);
        Mono<Customer> updatedCustomer = existingCustomer.flatMap(c -> repository.save(Customer.from(c)));
        return updatedCustomer.map(c ->
                		ResponseEntity
							.ok()
							.location(
								Optional.ofNullable(c.getId())
									.map(i -> linkTo(methodOn(CustomerEndpoints.class).getCustomerById(i)).toUri())
									.orElseThrow(() -> new RuntimeException("Failed to update customer for some reason")))
							.body(assembler.toModel(c)))
                		.defaultIfEmpty(ResponseEntity.<EntityModel<Customer>>notFound().build());
    }

	@GetMapping(value = "/customers/{id}", produces = MediaTypes.HAL_JSON_VALUE)
	Mono<ResponseEntity<EntityModel<Customer>>> getCustomerById(@PathVariable UUID id) {
		return repository.findById(id)
                .map(savedCustomer -> ResponseEntity.ok(assembler.toModel(savedCustomer)))
                .defaultIfEmpty(ResponseEntity.<EntityModel<Customer>>notFound().build());
	}

	@GetMapping(value = "/customers", produces = MediaTypes.HAL_JSON_VALUE)
	Mono<ResponseEntity<CollectionModel<EntityModel<Customer>>>> getCustomerByNameAttributes(
			@RequestParam(required = false) String firstName,
			@RequestParam(required = false) String lastName) {
		if (StringUtils.isNotBlank(lastName) && StringUtils.isBlank(firstName)) {
			return repository.findByLastName(lastName)
					.collectList()
					.map(savedCustomers -> ResponseEntity.ok(assembler.toCollectionModel(savedCustomers)))
			        .defaultIfEmpty(ResponseEntity.<CollectionModel<EntityModel<Customer>>>notFound().build());
		} else if (StringUtils.isNotBlank(firstName) && StringUtils.isBlank(lastName)) {
			return repository.findByFirstName(firstName)
					.collectList()
	        		.map(savedCustomers -> ResponseEntity.ok(assembler.toCollectionModel(savedCustomers)))
	        		.defaultIfEmpty(ResponseEntity.<CollectionModel<EntityModel<Customer>>>notFound().build());
		} else if (StringUtils.isNotBlank(lastName) && StringUtils.isNotBlank(firstName)) {
			return repository.findByFirstNameInAndLastName(firstName, lastName)
					.collectList()
					.map(savedCustomer -> ResponseEntity.ok(assembler.toCollectionModel(savedCustomer)))
					.defaultIfEmpty(ResponseEntity.<CollectionModel<EntityModel<Customer>>>notFound().build());
		} else {
			return Mono.just(ResponseEntity.<CollectionModel<EntityModel<Customer>>>notFound().build());
		}
	}

	@DeleteMapping("/customers/{id}")
    public Mono<ResponseEntity<Void>> deleteCustomer(@PathVariable UUID id) {

        return repository.findById(id)
                .flatMap(existingCustomer ->
                        repository.delete(existingCustomer)
                            .then(Mono.just(ResponseEntity.ok().<Void>build()))
                )
                .defaultIfEmpty(ResponseEntity.<Void>notFound().build());
    }

	@GetMapping(value = "/stream/customers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Customer> streamAllCustomers() {
        return repository.findAll();
    }
}

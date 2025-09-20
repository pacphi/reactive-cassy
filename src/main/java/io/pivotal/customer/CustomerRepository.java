package io.pivotal.customer;

import java.util.UUID;

import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, UUID> {

	/**
	 * Derived query selecting by {@code lastName}.
	 *
	 * @param lastName
	 * @return
	 */
	@AllowFiltering
	Flux<Customer> findByLastName(String lastName);

	/**
	 * Derived query selecting by {@code firstName}.
	 *
	 * @param firstName
	 * @return
	 */
	@AllowFiltering
	Flux<Customer> findByFirstName(String firstName);

	/**
	 * String query selecting one or more matching entities.
	 *
	 * @param lastName
	 * @return
	 */
	@Query("SELECT * FROM customer WHERE firstName = ?0 and lastName  = ?1 ALLOW FILTERING")
	Flux<Customer> findByFirstNameInAndLastName(String firstName, String lastName);

	/**
	 * Derived query selecting by {@code lastName}. {@code lastName} uses deferred resolution that does not require
	 * blocking to obtain the parameter value.
	 *
	 * @param lastName
	 * @return
	 */
	@AllowFiltering
	Flux<Customer> findByLastName(Mono<String> lastName);

	/**
	 * Derived query selecting by {@code firstname} and {@code lastName}. {@code firstName} uses deferred resolution that
	 * does not require blocking to obtain the parameter value.
	 *
	 * @param firstName
	 * @param lastName
	 * @return
	 */
	@AllowFiltering
	Flux<Customer> findByFirstNameAndLastName(Mono<String> firstName, String lastName);

}
package io.pivotal.customer;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@SpringBootTest
@Testcontainers
public class ReactiveCustomerRepositoryIntegrationTest {

	@Container
	static final CassandraContainer cassandra =
			new CassandraContainer("cassandra:4.1")
				.withInitScript("cql/simple.cql")
				.withStartupTimeout(Duration.ofMinutes(3));

	@Autowired CustomerRepository repository;

	/**
	 * Clear table and insert some rows.
	 */
	@BeforeEach
	public void setUp() {

		Flux<Customer> deleteAndInsert = repository.deleteAll()
				.thenMany(Flux.just(Customer.builder().withFirstName("Nick").withLastName("Fury").build(),
                Customer.builder().withFirstName("Tony").withLastName("Stark").build(),
                Customer.builder().withFirstName("Bruce").withLastName("Banner").build(),
                Customer.builder().withFirstName("Peter").withLastName("Parker").build()))
				.flatMap(c -> repository.save(c));

		StepVerifier.create(deleteAndInsert).expectNextCount(4).verifyComplete();
	}

	/**
	 * This sample performs a count, inserts data and performs a count again using reactive operator chaining.
	 */
	@Test
	public void shouldInsertAndCountData() {

		Mono<Long> saveAndCount = repository.count()
				.doOnNext(System.out::println)
				.thenMany(repository.saveAll(Flux.just(Customer.builder().withFirstName("Stephen").withLastName("Strange").build(),
				Customer.builder().withFirstName("Carol").withLastName("Danvers").build())))
				.last()
				.flatMap(v -> repository.count())
				.doOnNext(System.out::println);

		StepVerifier.create(saveAndCount).expectNext(6L).verifyComplete();
	}

	/**
	 * Result set {@link com.datastax.driver.core.Row}s are converted to entities as they are emitted. Reactive pull and
	 * prefetch define the amount of fetched records.
	 */
	@Test
	public void shouldPerformConversionBeforeResultProcessing() {

		StepVerifier.create(repository.findAll().doOnNext(System.out::println))
				.expectNextCount(4)
				.verifyComplete();
	}

	/**
	 * Fetch data using query derivation.
	 */
	@Test
	public void shouldQueryDataWithQueryDerivation() {
		StepVerifier.create(repository.findByLastName("Banner")).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using a string query.
	 */
	@Test
	public void shouldQueryDataWithStringQuery() {
		StepVerifier.create(repository.findByFirstNameInAndLastName("Tony", "Stark")).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using query derivation.
	 */
	@Test
	public void shouldQueryDataWithDeferredQueryDerivation() {
		StepVerifier.create(repository.findByLastName(Mono.just("Fury"))).expectNextCount(1).verifyComplete();
	}

	/**
	 * Fetch data using query derivation and deferred parameter resolution.
	 */
	@Test
	public void shouldQueryDataWithMixedDeferredQueryDerivation() {

		StepVerifier.create(repository.findByFirstNameAndLastName(Mono.just("Bruce"), "Banner"))
				.expectNextCount(1)
				.verifyComplete();
	}


	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cassandra.username", () -> "cassandra");
		registry.add("spring.cassandra.password", () -> "cassandra");
		registry.add("spring.cassandra.contact-points", () -> cassandra.getHost() + ":" + cassandra.getMappedPort(9042));
		registry.add("spring.cassandra.keyspace-name", () -> "customers");
		registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
	}
}
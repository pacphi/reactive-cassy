package io.pivotal.customer;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ReactiveCassandraTemplateIntegrationTest {

	@Container
	static final CassandraContainer cassandra =
			new CassandraContainer("cassandra:4.1")
				.withInitScript("cql/simple.cql")
				.withStartupTimeout(Duration.ofMinutes(3));

	@Autowired private ReactiveCassandraTemplate template;

	/**
	 * Truncate table and insert some rows.
	 */
	@BeforeEach
	public void setUp() {
		Flux<Customer> truncateAndInsert = template.truncate(Customer.class)
				.thenMany(Flux.just(Customer.builder().withFirstName("Nick").withLastName("Fury").build(),
						Customer.builder().withFirstName("Tony").withLastName("Stark").build(),
						Customer.builder().withFirstName("Bruce").withLastName("Banner").build(),
						Customer.builder().withFirstName("Peter").withLastName("Parker").build()))
				.flatMap(template::insert);

		StepVerifier.create(truncateAndInsert).expectNextCount(4).verifyComplete();
	}

	/**
	 * This sample performs a count, inserts data and performs a count again using reactive operator chaining. It prints
	 * the two counts ({@code 4} and {@code 6}) to the console.
	 */
	@Test
	public void shouldInsertAndCountData() {

		Mono<Long> saveAndCount = template.count(Customer.class)
				.doOnNext(System.out::println)
				.thenMany(Flux.just(Customer.builder().withFirstName("Stephen").withLastName("Strange").build(),
				Customer.builder().withFirstName("Carol").withLastName("Danvers").build()))
				.flatMap(template::insert)
				.last()
				.flatMap(v -> template.count(Customer.class))
				.doOnNext(System.out::println);

		StepVerifier.create(saveAndCount).expectNext(6L).verifyComplete();
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

package io.pivotal.customer;

import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.util.Assert;

import com.datastax.oss.driver.api.core.uuid.Uuids;

@Table("customer")
public class Customer {

	@PrimaryKey
	private UUID id;

	@Column("firstname")
	private String firstName;

	@Column("lastname")
	private String lastName;

	private Customer(Builder builder) {
		this.id = builder.id;
		this.firstName = builder.firstName;
		this.lastName = builder.lastName;
	}

	private Customer() {}

	private Customer(UUID id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public static Customer from(Customer customer) {
		Assert.isTrue(customer.getId() != null, "Customer id must not be null");
		return new Customer(customer.getId(), customer.getFirstName(), customer.getLastName());
	}

	public UUID getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private UUID id = Uuids.timeBased();
		private String firstName;
		private String lastName;

		private Builder() {
		}

		public Builder withId(UUID id) {
			this.id = id;
			return this;
		}

		public Builder withFirstName(String firstName) {
			this.firstName = firstName;
			return this;
		}

		public Builder withLastName(String lastName) {
			this.lastName = lastName;
			return this;
		}

		public Customer build() {
			return new Customer(this);
		}
	}

}
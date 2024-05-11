package com.github.simplejpql;

import static com.github.simplejpql.Operator.and;
import static com.github.simplejpql.Operator.eq;
import static com.github.simplejpql.Operator.gt;
import static com.github.simplejpql.Operator.like;
import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

import com.github.simplejpql.domain.Person;
import com.github.simplejpql.domain.Phone;

@DataJpaTest
@ContextConfiguration(classes = TestConfig.class)
public class TestStatementBuilder {

	@Autowired
	private EntityManager entityManager;
	
	private long random(long min, long max) {
		return min + Math.round(Math.random() * (max - min));
	}

	@BeforeEach
	public void beforeEach() {
		
	}
	
	@Test
	public void testSelect() {
		Stream
			.generate(() -> Person.builder()
				.name(UUID.randomUUID().toString())
				.nickName(UUID.randomUUID().toString())
				.address(UUID.randomUUID().toString())
				.createdOn(LocalDateTime.now().minusDays(random(1, 14)))
				.build())
			.limit(50)
			.forEach(entityManager::merge);
		
		StatementBuilder
			.select("p")
			.from("Person p")
			.where(and(
				like("p.name", "%A%B%C%D%", true),
				gt("p.createdOn", LocalDateTime.now().minusDays(7))))
			.createQuery(entityManager, Person.class)
			.getResultList();
	}
	
	@Test
	public void testUpdate() {
		Person p = entityManager.merge(Person.builder()
			.name(UUID.randomUUID().toString())
			.build());
		
		String newName = UUID.randomUUID().toString();
		
		StatementBuilder
			.update("Person p")
			.set("p.name", newName)
			.where(eq("p.name", p.getName()))
			.createQuery(entityManager)
			.executeUpdate();
		
		entityManager.refresh(p);
		
		Assert.isTrue(p.getName().equals(newName), "Person.name was not updated as expected");
	}
	
	@Test
	public void testDelete() {
		Person p = entityManager.merge(Person.builder()
			.name(UUID.randomUUID().toString())
			.build());
		
		StatementBuilder
			.delete()
			.from("Person p")
			.where(eq("p.name", p.getName()))
			.createQuery(entityManager)
			.executeUpdate();
		
		Assert.isTrue(StatementBuilder
			.select()
			.from("Person")
			.where(eq("id", p.getId()))
			.createQuery(entityManager, Person.class)
			.getResultList().isEmpty(), "Person was not deleted as expected");
	}
}

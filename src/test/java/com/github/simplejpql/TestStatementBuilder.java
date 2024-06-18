package com.github.simplejpql;

import static com.github.simplejpql.Operator.and;
import static com.github.simplejpql.Operator.between;
import static com.github.simplejpql.Operator.eq;
import static com.github.simplejpql.Operator.gt;
import static com.github.simplejpql.Operator.in;
import static com.github.simplejpql.Operator.like;
import static com.github.simplejpql.Operator.or;
import static java.lang.Math.round;
import static java.util.stream.Collectors.toList;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;

import com.github.simplejpql.StatementBuilder.SelectStatementBuilder;
import com.github.simplejpql.domain.Person;
import com.github.simplejpql.domain.Phone;
import com.github.simplejpql.domain.PhoneType;

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
	
	private SelectStatementBuilder getSelectStatementBuilder() {
		return StatementBuilder
			.select("p")
			.from("Person p")
			.where(and(
				eq("active", true),
				or(like("p.name", "%A%B%C%D%", true), like("p.name", "%1%2%3%4%", true)),
				gt("p.createdOn", LocalDateTime.now().minusDays(7))));
	}

	@Test
	public void testSelect() {	
		Stream
			.generate(() -> Person.builder()
				.name(UUID.randomUUID().toString())
				.nickName(UUID.randomUUID().toString())
				.address(UUID.randomUUID().toString())
				.createdOn(LocalDateTime.now().minusDays(random(1, 14)))
				.active(random(1,10) % 2 == 0)
				.phones(Stream
					.generate(() -> Phone.builder()
						.number(String.format("%010d", round(Math.random() * 10000000000L)))
						.build())
					.limit(5)
					.collect(toList()))
				.build())
			.limit(50)
			.forEach(entityManager::merge);
		
		getSelectStatementBuilder()
			.createQuery(entityManager, Person.class)
			.getResultList();
	}

	@Test
	public void testSelectParameterSequencing() {
		SelectStatementBuilder
			builder1 = getTestSelectStatementBuilder(),
			builder2 = getTestSelectStatementBuilder();
		
		String query = builder1.toString();
			
		Assert.isTrue(builder1.toString().equals(builder2.toString()), "Query strings do not match. This impacts query cache performance.");
		Assert.isTrue(builder1.getNamedParameters().keySet().equals(builder2.getNamedParameters().keySet()), "Query parameters do not match");
		Assert.isTrue(builder1.getNamedParameters().keySet().stream().allMatch(query::contains), "Query string does not contain a named parameter");
	}

	public SelectStatementBuilder getTestSelectStatementBuilder() {
		return StatementBuilder
			.select("p")
			.from("Person p")
			.leftJoin("p.phones ph", and(like("ph.number", "407-%"), eq("ph.type", PhoneType.MOBILE)))
			.having(gt("ph_count", 1))
			.where(and(
				eq("active", true),
				or(like("p.name", "%A%B%C%D%", true), like("p.name", "%1%2%3%4%", true)),
				gt("p.createdOn", LocalDateTime.now().minusDays(7))));
	}
	
	private Predicate generatePredicate() {
		switch((int) random(1,8)) {
			case 1: return and(Stream.generate(this::generatePredicate).limit(random(1,5)).collect(toList()));
			case 5: return or(Stream.generate(this::generatePredicate).limit(random(1,5)).collect(toList()));
			case 2,6: return eq("", "");
			case 3,7: return between("", random(1,5), random(6,10));
			case 4,8: return in("", Arrays.asList(1));
		}
		
		throw new IllegalArgumentException("This should not happen");
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

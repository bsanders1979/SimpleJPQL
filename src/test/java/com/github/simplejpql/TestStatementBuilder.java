package com.github.simplejpql;

import static com.github.simplejpql.Operator.eq;
import static com.github.simplejpql.Operator.expr;
import static java.util.Collections.emptySet;

public class TestStatementBuilder {

	public void testSelect() {
		StatementBuilder
			.select(emptySet())
			.from(emptySet())
			.innerJoin("")
			.innerJoin("", expr("1 = 1"))
			.leftJoin("")
			.leftJoin("", expr("1 = 1"))
			.where(expr("1 = 1"))
			.groupBy(emptySet())
			.having(expr("1 = 1"))
			.orderBy(emptySet())
			.firstResult(0);
	}
	
	public void testUpdate() {
		StatementBuilder
			.update("Person p")
			.set("p.name", "New Name")
			.where(eq("p.name", "Old Name"));
	}
	
	public void testDelete() {
		StatementBuilder
			.delete()
			.from("Person p")
			.where(eq("p.name", "Name"));
	}
}

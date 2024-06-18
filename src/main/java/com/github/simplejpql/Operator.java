package com.github.simplejpql;

import static java.util.stream.Collectors.toList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class Operator {

	private Operator() {}
	
	public static Predicate and(Collection<Predicate> predicates) {
		return new Predicate.And(predicates);
	}
	
	public static Predicate and(Predicate...predicates) {
		if (predicates == null)
			return null;

		List<Predicate> filteredPredicates = Arrays.stream(predicates)
			.filter(Objects::nonNull)
			.collect(toList());
		
		return filteredPredicates.isEmpty() ? null : and(filteredPredicates);
	}

	public static Predicate or(Collection<Predicate> predicates) {
		return new Predicate.Or(predicates);
	}
	
	public static Predicate or(Predicate...predicates) {
		if (predicates == null)
			return null;

		List<Predicate> filteredPredicates = Arrays.stream(predicates)
			.filter(Objects::nonNull)
			.collect(toList());
		
		return filteredPredicates.isEmpty() ? null : or(filteredPredicates);
	}
	
	public static Predicate expr(String expression) {
		return new Predicate.Expression(expression);
	}
	
	public static Predicate not(Predicate predicate) {
		return new Predicate.Not(predicate);
	}
	
	public static Predicate eq(String operand, Object value, boolean ignoreCase) {
		if (value == null) {
			//Not sure if doing this is proper? Can it produce unexpected results? 
			return isNull(operand);
		}
		
		return new Predicate.Equals<>(operand, value, ignoreCase);
	}
	
	public static Predicate eq(String operand, Object value) {
		return eq(operand, value, false);
	}

	public static Predicate eq(String operand, Supplier<?> valueSupplier, boolean ignoreCase) {
		return new Predicate.Equals<>(operand, valueSupplier, ignoreCase);
	}
	
	public static Predicate eq(String operand, Supplier<?> valueSupplier) {
		return eq(operand, valueSupplier, false);
	}
	
	public static Predicate neq(String operand, Object value, boolean ignoreCase) {
		return not(eq(operand, value, ignoreCase));
	}
	
	public static Predicate neq(String operand, Object value) {
		return neq(operand, value, false);
	}
	
	public static Predicate neq(String operand, Supplier<?> valueSupplier, boolean ignoreCase) {
		return not(eq(operand, valueSupplier, ignoreCase));
	}
	
	public static Predicate neq(String operand, Supplier<?> valueSupplier) {
		return neq(operand, valueSupplier, false);
	}

	public static Predicate gt(String property, Object value) {
		return new Predicate.GreaterThan(property, value, false);
	}
	
	public static Predicate gte(String property, Object value) {
		return new Predicate.GreaterThan(property, value, true);
	}

	public static Predicate lt(String property, Object value) {
		return new Predicate.LessThan(property, value, false);
	}
	
	public static Predicate lte(String property, Object value) {
		return new Predicate.LessThan(property, value, true);
	}
	
    public static Predicate between(String propertyName, Object minimum, Object maximum, boolean includeMinimum, boolean includeMaximum) {
    	return and(
    		includeMinimum ? gte(propertyName, minimum) : gt(propertyName, minimum),
			includeMaximum ? lte(propertyName, maximum) : lt(propertyName, maximum));
    }
    
    public static Predicate between(String propertyName, Object minimum, Object maximum) {
    	return between(propertyName, minimum, maximum, true, true);
    }
    
    public static Predicate between(Object value, String minimumPropertyName, String maximumPropertyName, boolean includeMinimum, boolean includeMaximum) {
    	return and(
    		includeMinimum ? lte(minimumPropertyName, value) : lt(minimumPropertyName, value),
			includeMaximum ? gte(maximumPropertyName, value) : gt(maximumPropertyName, value));
    }
    
    public static Predicate between(Object value, String minimumPropertyName, String maximumPropertyName) {
    	return between(value, minimumPropertyName, maximumPropertyName, true, true);
    }
    
	public static Predicate in(String property, Collection<?> values) {
		return new Predicate.In(property, values);
	}

	public static Predicate notIn(String property, Collection<?> values) {
		return not(in(property, values));
	}
	
	public static Predicate isNull(String property) {
		return new Predicate.IsNull(property);
	}

	public static Predicate isNotNull(String property) {
		return not(isNull(property));
	}

	public static Predicate like(String operand, String pattern, boolean ignoreCase) {
		return new Predicate.Like(operand, pattern, ignoreCase);
	}
	
	public static Predicate like(String operand, String pattern) {
		return like(operand, pattern, true);
	}
	
	public static Predicate contains(String propertyName, String value, boolean ignoreCase) {
		return like(propertyName, "%" + value + "%", ignoreCase);
	}
	
    public static Predicate contains(String propertyName, String value) {
    	return contains(propertyName, value, false);
    }
    
    public static Predicate collectionSize(String collection, String operator, int size) {
    	if (Arrays.asList("=", "!=", ">", ">=", "<", "<=").contains(operator))
    		return expr(String.format("size(%s) %s %d", collection, operator, size));
    	
    	throw new IllegalArgumentException("Invalid operator: " + operator);
    }
}
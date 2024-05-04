package com.github.simplejpql;

import static java.lang.Math.random;
import static java.lang.Math.round;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class Predicate {

	//Just a precaution to avoid name collisions with named parameters for any given query
	protected final String suffix = String.format("%04d", round(random() * 10000));
	
	protected String generateParameterName(String expression) {
		return String.format("%s_%s", expression.replaceAll("[^A-Za-z0-9_$]", "_"), suffix);
	}
	
	public Map<String, Object> getNamedParameters() {
		return Collections.emptyMap(); 
	}

	public abstract String toString();

	public static class And extends Predicate {
		
		public And(Collection<Predicate> predicates) {
			this.predicates = predicates;
		}

		private Collection<Predicate> predicates;
		
		@Override
		public String toString() {
			return String.format("(%s)", Optional.ofNullable(predicates).orElse(emptyList()).stream()
				.filter(Objects::nonNull)
				.map(Predicate::toString)
				.collect(joining(" and ")));
		}
		
		@Override
		public Map<String, Object> getNamedParameters() {			
			return Optional.ofNullable(predicates).orElse(emptyList()).stream()
				.map(Predicate::getNamedParameters)
				.flatMap(map -> map.entrySet().stream())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
	}
	

	public static class Or extends Predicate {
		
		private Collection<Predicate> predicates;
		
		public Or(Collection<Predicate> predicates) {
			this.predicates = predicates;
		}

		@Override
		public String toString() {
			return String.format("(%s)", Optional.ofNullable(predicates).orElse(emptyList()).stream()
				.map(Predicate::toString)
				.collect(joining(" or ")));
		}
		
		@Override
		public Map<String, Object> getNamedParameters() {
			return Optional.ofNullable(predicates).orElse(emptyList()).stream()
				.map(Predicate::getNamedParameters)
				.flatMap(map -> map.entrySet().stream())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
	}
	

	public static class Expression extends Predicate {

		private String expression;
		
		public Expression(String expression) {
			this.expression = expression;
		}

		@Override
		public String toString() {
			return expression;
		}
	}
	

	public static class Not extends Predicate {

		private Predicate predicate;
		
		public Not(Predicate predicate) {
			this.predicate = predicate;
		}

		@Override
		public String toString() {
			return String.format("not (%s)", predicate);
		}
		
		@Override
		public Map<String, Object> getNamedParameters() {
			return predicate.getNamedParameters();
		}
	}
	
	public static class Equals<T> extends Predicate {

		private String operand;

		private T value;
		
		private Supplier<T> valueSupplier;
				
		private boolean ignoreCase;
		
		public Equals(String operand, T value, boolean ignoreCase) {
			this.operand = operand;
			this.value = value;
			this.ignoreCase = ignoreCase;
		}

		public Equals(String operand, Supplier<T> valueSupplier, boolean ignoreCase) {
			this.operand = operand;
			this.valueSupplier = valueSupplier;
			this.ignoreCase = ignoreCase;
		}
		
		@Override
		public Map<String, Object> getNamedParameters() {
			return new MapBuilder<String, Object>().put(generateParameterName(operand), Optional.ofNullable(valueSupplier).map(s -> s.get()).orElse(value)).toMap();
		}

		@Override
		public String toString() {
			return String.format(ignoreCase ? "lower(%s) = lower(:%s)" : "%s = :%s", operand, generateParameterName(operand));
		}
	}
	
	public static class GreaterThan extends Predicate {

		private String property;

		private Object value;
		
		private boolean inclusive;
		
		public GreaterThan(String property, Object value, boolean inclusive) {
			this.property = property;
			this.value = value;
			this.inclusive = inclusive;
		}

		@Override
		public Map<String, Object> getNamedParameters() {
			return new MapBuilder<String, Object>().put(generateParameterName(property), value).toMap();
		}

		@Override
		public String toString() {
			return String.format("%s >%s :%s", property, inclusive ? "=" : "", generateParameterName(property));
		}

	}

	public static class LessThan extends Predicate {

		private String property;

		private Object value;
		
		private boolean inclusive;
		
		public LessThan(String property, Object value, boolean inclusive) {
			this.property = property;
			this.value = value;
			this.inclusive = inclusive;
		}

		@Override
		public Map<String, Object> getNamedParameters() {
			return new MapBuilder<String, Object>().put(generateParameterName(property), value).toMap();
		}

		@Override
		public String toString() {
			return String.format("%s <%s :%s", property, inclusive ? "=" : "", generateParameterName(property));
		}
	}
	
	public static class In extends Predicate {
		
		private String property;
		
		private Collection<?> values;

		public In(String property, Collection<?> values) {
			if (values.isEmpty())
				throw new IllegalArgumentException("values must not be empty");
			
			this.property = property;
			this.values = values;
		}

		@Override
		public String toString() {
			return String.format("%s in (:%s)", property, generateParameterName(property));
		}
		
		@Override
		public Map<String, Object> getNamedParameters() {
			return new MapBuilder<String, Object>().put(generateParameterName(property), values).toMap();
		}
	}
	

	public static class IsNull extends Predicate {

		private String property;
		
		public IsNull(String property) {
			this.property = property;
		}

		@Override
		public String toString() {
			return String.format("%s is null", property);
		}
	}
	

	public static class Like extends Predicate {

		private String operand, expression;
		
		public Like(String operand, String expression) {
			this.operand = operand;
			this.expression = expression;
		}

		@Override
		public Map<String, Object> getNamedParameters() {
			return new MapBuilder<String, Object>().put(generateParameterName(operand), expression).toMap();
		}

		@Override
		public String toString() {
			return String.format("%s like :%s", operand, generateParameterName(operand));
		}
	}
}

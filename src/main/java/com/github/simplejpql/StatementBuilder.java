package com.github.simplejpql;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

public abstract class StatementBuilder<SB extends StatementBuilder<SB>> {

	private Integer firstResult, maxResults;
	private FlushModeType flushMode;
	private LockModeType lockMode;
	private Map<String, Object> hints = new HashMap<>();
	
	public Integer getFirstResult() {
		return firstResult;
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public FlushModeType getFlushMode() {
		return flushMode;
	}

	public LockModeType getLockMode() {
		return lockMode;
	}

	public Map<String, Object> getHints() {
		return hints;
	}

	boolean isNotEmpty(Collection<?> collection) {
		return !collection.isEmpty();
	}
	
    private static <T> BinaryOperator<T> throwingMerger() {
        return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
    }

	@SuppressWarnings("unchecked")
	public SB firstResult(int firstResult) {
	    this.firstResult = firstResult;
	    return (SB) this;
	}

	@SuppressWarnings("unchecked")
	public SB maxResults(int maxResults) {
	    this.maxResults = maxResults;
	    return (SB) this;
	}

	@SuppressWarnings("unchecked")
	public SB flushMode(FlushModeType flushMode) {
	    this.flushMode = flushMode;
	    return (SB) this;
	}

	@SuppressWarnings("unchecked")
	public SB lockMode(LockModeType lockMode) {
		this.lockMode = lockMode;
		return (SB) this;
	}
	
	@SuppressWarnings("unchecked")
	public SB hints(Map<String, Object> hints) {
	    getHints().putAll(Optional.ofNullable(hints).orElse(emptyMap()));
	    return (SB) this;
	}

	@SuppressWarnings("unchecked")
	public SB hint(String key, Object value) {
		getHints().put(key, value);
		return (SB) this;
	}
	
	public SB lockTimeout(Long lockTimeout) {
	    return hint("jakarta.persistence.lock.timeout", lockTimeout);
	}
	
	public SB queryTimeout(Long queryTimeout) {
	    return hint("jakarta.persistence.query.timeout", queryTimeout);
	}
	
	public SB cacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
	    return hint("jakarta.persistence.cache.retrieveMode", cacheRetrieveMode);
	}
	
	public SB cacheStoreMode(CacheStoreMode cacheStoreMode) {
	    return hint("jakarta.persistence.cache.storeMode", cacheStoreMode);
	}
	
	public SB loadGraph(EntityGraph<?> loadGraph) {
		return hint("jakarta.persistence.loadgraph", loadGraph);
	}
	
	public SB fetchGraph(EntityGraph<?> fetchGraph) {
		return hint("jakarta.persistence.fetchgraph", fetchGraph);
	}
	
	public abstract String toString();

	public abstract Map<String, Object> getNamedParameters();

	public static SelectStatementBuilder select(String...select) {
		return select(Optional.ofNullable(select).map(Arrays::asList).orElse(emptyList()));
	}
	
	public static SelectStatementBuilder select(Collection<String> select) {
		return new SelectStatementBuilder(select);
	}
	
	public static UpdateStatementBuilder update(String entityName) {
		return new UpdateStatementBuilder(entityName);
	}
	
	public static DeleteStatementBuilder delete() {
		return new DeleteStatementBuilder();
	}
	
	public Query createQuery(EntityManager entityManager) {
		return prepareQuery(entityManager.createQuery(toString()));
	}
	
	public <T> TypedQuery<T> createQuery(EntityManager entityManager, Class<T> resultClass) {
		return prepareQuery(entityManager.createQuery(toString(), resultClass));
	}

	private <T extends Query> T prepareQuery(T query) {
		setIfPresent(firstResult, query::setFirstResult);
		setIfPresent(maxResults , query::setMaxResults);
		setIfPresent(flushMode  , query::setFlushMode);
		setIfPresent(lockMode   , query::setLockMode);
		
		getHints().entrySet()
			.forEach(hint -> query.setHint(hint.getKey(), hint.getValue()));
		
		getNamedParameters().entrySet()
			.forEach(parameter -> query.setParameter(parameter.getKey(), parameter.getValue()));
		
		return query;
	}
	
	private <V> void setIfPresent(V value, Consumer<? super V> consumer) {
		Optional.ofNullable(value).ifPresent(consumer);
	}
	
	public static class SelectStatementBuilder extends StatementBuilder<SelectStatementBuilder> {

		@Getter
		private Set<String>
			select = new LinkedHashSet<>(),
			from = new LinkedHashSet<>(),
			groupBy = new LinkedHashSet<>(),
			orderBy = new LinkedHashSet<>();

		@Getter
		private Map<String, Predicate> associations = new LinkedHashMap<>();
		
		@Getter
		private Predicate where, having;
		
		public SelectStatementBuilder(Collection<String> select) {
			getSelect().addAll(Optional.ofNullable(select).orElse(emptySet()));
		}

		public SelectStatementBuilder from(Collection<String> from) {
			getFrom().addAll(Optional.ofNullable(from).orElse(emptyList()));
			return this;
		}
		
		public SelectStatementBuilder from(String...from) {
			return from(Optional.ofNullable(from).map(Arrays::asList).orElse(emptyList()));
		}

		public SelectStatementBuilder from(Class<?> entityClass, String alias) {
			return from(Stream.of(entityClass.getName(), alias).collect(Collectors.joining(" ")));
		}
		
		public SelectStatementBuilder associate(Map<String, Predicate> associations) {
			getAssociations().putAll(Optional.ofNullable(associations).orElse(emptyMap()));
			return this;
		}

		public SelectStatementBuilder associate(Collection<String> associations) {
			return associate(Optional.ofNullable(associations).orElse(emptyList()).stream()
				.collect(toMap(a -> a, a -> null, throwingMerger(), LinkedHashMap::new)));
		}
		
		public SelectStatementBuilder associate(String association, Predicate predicate) {
			
			if (StringUtils.isNotBlank(association)) {

				// remove the join type and then tokenize the rest
				String[] fetch_path_alias = Pattern
					.compile("\\b(?:join|inner join|left join|left outer join)\\b", Pattern.CASE_INSENSITIVE)
					.matcher(association)
					.replaceFirst("")
					.trim()
					.split("\\s+");
				
				int length = fetch_path_alias.length;
				boolean fetch = fetch_path_alias[0].equalsIgnoreCase("fetch");

				//if alias appears to be missing
				if (length == (fetch ? 2 : 1))
					association += " " + fetch_path_alias[fetch ? 1 : 0].split("\\.(?!.*\\.)")[1]; 

				return associate(new MapBuilder<String, Predicate>().put(association, predicate).toMap());
			}
			
			return this;
		}
		
		public SelectStatementBuilder associate(String association) {
			return associate(association, null);
		}
		
		public SelectStatementBuilder innerJoin(String innerJoin, Predicate predicate) {
			return StringUtils.isNotBlank(innerJoin) ? associate("inner join " + innerJoin, predicate) : this;
		}
		
		public SelectStatementBuilder innerJoin(String innerJoin) {
			return innerJoin(innerJoin, null);
		}
		
		public SelectStatementBuilder leftJoin(String leftJoin, Predicate predicate) {
			return StringUtils.isNotBlank(leftJoin) ? associate("left join " + leftJoin, predicate) : this;
		}
		
		public SelectStatementBuilder leftJoin(String leftJoin) {
			return leftJoin(leftJoin, null);
		}
		
		public SelectStatementBuilder where(Predicate where) {
			this.where = where;
			return this;
		}
		
		public SelectStatementBuilder groupBy(Collection<String> groupBy) {
			getGroupBy().addAll(Optional.ofNullable(groupBy).orElse(emptyList()));
			return this;
		}
		
		public SelectStatementBuilder groupBy(String...groupBy) {
			return groupBy(Optional.ofNullable(groupBy).map(Arrays::asList).orElse(emptyList()));
		}
		
		public SelectStatementBuilder having(Predicate having) {
			this.having = having;
			return this;
		}
		
		public SelectStatementBuilder orderBy(Collection<String> orderBy) {
			getOrderBy().addAll(Optional.ofNullable(orderBy).orElse(emptyList()));
			return this;
		}
		
		public SelectStatementBuilder orderBy(String...orderBy) {
			return orderBy(Optional.ofNullable(orderBy).map(Arrays::asList).orElse(emptyList()));
		}

		public String getSelectClause() {
			return Optional.of(getSelect())
				.filter(this::isNotEmpty)
				.map(select -> "select " + select.stream().collect(joining(", ")))
				.orElse(null);
		}
		
		public String getFromClause() {
			return Stream.of(
				Optional.of(getFrom())
					.filter(this::isNotEmpty)
					.map(from -> "from " + from.stream().collect(joining(", ")))
					.orElse(null),
				
				getAssociations().entrySet()
					.stream()
					.map(e -> Optional.ofNullable(e.getValue())
						.map(predicate -> e.getKey() + " on " + predicate)
						.orElse(e.getKey()))
					.collect(joining("\n")))
			.filter(StringUtils::isNotBlank)
			.collect(joining("\n"));
		}
		
		public String getWhereClause() {
			return Optional.ofNullable(getWhere()).map(where -> "where " + where).orElse(null);
		}
		
		public String getGroupByClause() {
			return Optional.of(getGroupBy())
				.filter(this::isNotEmpty)
				.map(groupBy -> "group by " + groupBy.stream().collect(joining(", ")))
				.orElse(null);
		}
		
		public String getHavingClause() {
			return Optional.ofNullable(getHaving()).map(having -> "having " + having).orElse(null);
		}
		
		public String getOrderByClause() {
			return Optional.of(getOrderBy())
				.filter(this::isNotEmpty)
				.map(orderBy -> "order by " + orderBy.stream().filter(Objects::nonNull).collect(joining(", ")))
				.orElse(null);
		}
		
		@Override
		public String toString() {
			return Stream.of(
				getSelectClause(),
				getFromClause(),
				getWhereClause(),
				getGroupByClause(),
				getHavingClause(),
				getOrderByClause())
			.filter(StringUtils::isNotBlank)
			.collect(joining("\n"));
		}

		public Map<String, Object> getNamedParameters() {
			return Stream.concat(getAssociations().values().stream(), Stream.of(getWhere(), getHaving()))
				.filter(Objects::nonNull)
				.flatMap(c -> c.getNamedParameters().entrySet().stream())
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, throwingMerger(), LinkedHashMap::new));
		}
	}

	public static class UpdateStatementBuilder extends StatementBuilder<UpdateStatementBuilder> {

		private String entityName;
		
		private Map<String, Object> updateItems = new LinkedHashMap<>();
		
		private Predicate where;
		
		public UpdateStatementBuilder(String entityName) {
			this.entityName = entityName;
		}

		public UpdateStatementBuilder set(Map<String, Object> updateItems) {
			this.updateItems.putAll(Optional.ofNullable(updateItems).orElse(emptyMap()));
			return this;
		}
		

		public UpdateStatementBuilder set(String lhs, Object newValue) {
			this.updateItems.put(lhs, newValue);
			return this;
		}
		
		public UpdateStatementBuilder where(Predicate where) {
			this.where = where;
			return this;			
		}
		
		public String getUpdateClause() {
			return Stream.of(
					"update " + entityName,
					"set " + updateItems.entrySet().stream()
						.map(updateItem -> updateItem.getKey() + " = :" + updateItem.getKey().replaceAll("[^A-Za-z0-9_$]", "_"))
						.collect(joining(", ")))
				.collect(joining("\n"));
		}
		
		public String getWhereClause() {
			return where != null ? "where " + where : null;
		}
		
		@Override
		public String toString() {
			return Stream.of(getUpdateClause(), getWhereClause()).filter(StringUtils::isNotBlank).collect(joining("\n"));
		}

		@Override
		public Map<String, Object> getNamedParameters() {
			return Stream.of(updateItems, Optional.ofNullable(where).map(Predicate::getNamedParameters).orElse(emptyMap()))
				.flatMap(map -> map.entrySet().stream())
				.collect(toMap(e -> e.getKey().replaceAll("[^A-Za-z0-9_$]", "_"), Map.Entry::getValue));
		}
	}

	public static class DeleteStatementBuilder extends StatementBuilder<DeleteStatementBuilder> {

		private String entityName;
		
		private Predicate where;
		
		public DeleteStatementBuilder() {}

		public DeleteStatementBuilder from(String entityName) {
			this.entityName = entityName;
			return this;			
		}
		
		public DeleteStatementBuilder where(Predicate where) {
			this.where = where;
			return this;			
		}
		
		public String getDeleteClause() {
			return "delete from " + entityName;
		}
		
		public String getWhereClause() {
			return where != null ? "where " + where : null;
		}
		
		@Override
		public String toString() {
			return Stream.of(getDeleteClause(), getWhereClause()).filter(StringUtils::isNotBlank).collect(joining("\n"));
		}

		@Override
		public Map<String, Object> getNamedParameters() {
			return Optional.ofNullable(where).map(Predicate::getNamedParameters).orElse(emptyMap());
		}
	}
}
package com.github.simplejpql;

import java.util.LinkedHashMap;
import java.util.Map;

public class MapBuilder<K, V> {

	private Map<K, V> map;

	public MapBuilder() {
		this(new LinkedHashMap<>());
	}
	
	public MapBuilder(Map<K, V> map) {
		this.map = map;
	}
	
	public MapBuilder<K, V> put(K key, V value) {
		map.put(key, value);
		return this;
	}
	
	public Map<K, V> toMap() {
		return map;
	}
}

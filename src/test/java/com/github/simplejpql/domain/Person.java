package com.github.simplejpql.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Person {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String nickName;

	private String address;

	private LocalDateTime createdOn;

	@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
	@OrderColumn(name = "order_id")
	private List<Phone> phones = new ArrayList<>();

	@ElementCollection
	@MapKeyEnumerated(EnumType.STRING)
	private Map<AddressType, String> addresses = new HashMap<>();

	@Version
	private int version;
}
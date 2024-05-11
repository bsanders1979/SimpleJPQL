package com.github.simplejpql.domain;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment {

	@Id
	@GeneratedValue
	private Long id;

	private BigDecimal amount;

	private boolean completed;

	@ManyToOne
	private Person person;
}
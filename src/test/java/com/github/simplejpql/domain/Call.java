package com.github.simplejpql.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "phone_call")
@Data
public class Call {

	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	private Phone phone;

	@Column(name = "call_timestamp")
	private LocalDateTime timestamp;

	private int duration;

	@ManyToOne
	private Payment payment;
}
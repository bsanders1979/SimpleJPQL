package com.github.simplejpql.domain;

import jakarta.persistence.Entity;

@Entity
public class CreditCardPayment extends Payment {
	String cardNumber;

	public void setCardNumber(String cardNumber) {
		this.cardNumber = cardNumber;
	}

	public String getCardNumber() {
		return cardNumber;
	}
}
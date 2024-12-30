package org.zain.NewXmlUtility;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.zain.NewXmlUtility.AnnotationTesting.BaseObjectIdentifier;

@XmlRootElement
public class Order {
	
	@BaseObjectIdentifier
	private String orderID = "1";
	
	private double orderAmount = 0.00;
	
	@XmlElement
	public String getOrderID() {
		return orderID;
	}
	
	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}
	
	@XmlElement
	public double getOrderAmount() {
		return orderAmount;
	}
	
	public void setOrderAmount(double orderAmount) {
		this.orderAmount = orderAmount;
	}
}
package org.zain.NewXmlUtility;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.zain.NewXmlUtility.AnnotationTesting.ParentObjectIdentifier;

import java.util.List;

@XmlRootElement
public class CustomerOrders {
	
	@ParentObjectIdentifier
	private List<Order> orderList;
	
	@XmlElement(name = "Order")
	public List<Order> getOrderList() {
		return orderList;
	}
	
	public void setOrderList(List<Order> orderList) {
		this.orderList = orderList;
	}
}

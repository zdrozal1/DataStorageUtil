package org.zain.XmlUtils;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

/// Wrapper Class for elements
@XmlRootElement(name = "ListWrapper")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListWrapper<T> {
	
	@XmlElement(name = "Item")
	private List<T> items;
	
	public ListWrapper() {
	}
	
	public ListWrapper(List<T> items) {
		this.items = items;
	}
	
	public List<T> getItems() {
		return items;
	}
	
	public void setItems(List<T> items) {
		this.items = items;
	}
}

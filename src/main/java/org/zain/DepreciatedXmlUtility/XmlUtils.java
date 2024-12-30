package org.zain.DepreciatedXmlUtility;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class XmlUtils<T> {
	
	private final String filePath;
	private final Class<T> elementType;
	private final XmlEventManager xmlEventManager = new XmlEventManager();
	private List<T> elements;
	
	/**
	 * Constructor that initializes the DepreciatedXmlUtility instance, automatically loading the container.
	 *
	 * @param elementType the class type of the elements to be managed.
	 * @param filePath    the file path to the XML data source.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public XmlUtils(Class<T> elementType, String filePath) throws JAXBException {
		this.elementType = elementType;
		this.filePath = filePath;
		this.elements = loadContainer();
	}
	
	/**
	 * Compares two objects based on specific fields to determine if they are equal.
	 *
	 * @param obj1       the first object to compare.
	 * @param obj2       the second object to compare.
	 * @param fieldNames the names of the fields to compare.
	 * @return true if the objects are equal based on the specified fields, false otherwise.
	 */
	private static boolean equals(Object obj1, Object obj2, String... fieldNames) {
		if (obj1 == obj2) {
			return true;
		}
		if (obj1 == null || obj2 == null || !obj1.getClass().equals(obj2.getClass())) {
			return false;
		}
		try {
			for (String fieldName : fieldNames) {
				Field field = obj1.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				Object value1 = field.get(obj1);
				Object value2 = field.get(obj2);
				if (!Objects.equals(value1, value2)) {
					return false;
				}
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the XmlEventManager instance responsible for managing events related to XML operations.
	 *
	 * @return the XmlEventManager instance.
	 */
	public XmlEventManager getXmlEventManager() {
		return xmlEventManager;
	}
	
	/**
	 * Return List of elements inside elements
	 */
	public List<T> getElements() {
		return elements;
	}
	
	/**
	 * Adds an element to the container. If the element already exists, it will be replaced by default.
	 *
	 * @param element the element to be added.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void addElement(T element, String fieldToCompare) throws JAXBException {
		addElement(element, true, fieldToCompare);
	}
	
	/**
	 * Adds an element to the container, with an option to replace an existing element if it is found.
	 *
	 * @param element         the element to be added.
	 * @param replaceIfExists true to replace the existing element if found, false to avoid replacement.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void addElement(T element, boolean replaceIfExists, String fieldToCompare) throws JAXBException {
		xmlEventManager.triggerAddElementStarted(element);
		
		if (elements == null) {
			elements = new java.util.ArrayList<>();
		}
		
		Optional<T> existingElement = findElementByField(e -> equals(e, element, fieldToCompare));
		
		if (existingElement.isPresent()) {
			if (replaceIfExists) {
				elements.remove(existingElement.get());
				elements.add(element);
			} else {
				xmlEventManager.triggerElementAlreadyExists(element);
				return;
			}
		} else {
			elements.add(element);
		}
		saveContainer();
		xmlEventManager.triggerAddElementCompleted(element);
	}
	
	/**
	 * Modifies an existing element in the container by replacing it with a new element.
	 *
	 * @param oldElement the element to be modified.
	 * @param newElement the new element to replace the old one.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void modifyElement(T oldElement, T newElement, String fieldToCompare) throws JAXBException {
		modifyElement(oldElement, newElement, true, fieldToCompare);
	}
	
	/**
	 * Modifies an existing element in the container by replacing it with a new element, with an option to replace or not.
	 *
	 * @param oldElement      the element to be modified.
	 * @param newElement      the new element to replace the old one.
	 * @param replaceIfExists true to replace the existing element if found, false to avoid replacement.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void modifyElement(T oldElement, T newElement, boolean replaceIfExists, String fieldToCompare) throws JAXBException {
		xmlEventManager.triggerModifyElementStarted(oldElement, newElement);
		
		if (elements == null) {
			elements = new java.util.ArrayList<>();
		}
		
		Optional<T> existingElement = findElementByField(e -> equals(e, oldElement, fieldToCompare));
		
		if (existingElement.isPresent()) {
			if (replaceIfExists) {
				int index = elements.indexOf(existingElement.get());
				elements.set(index, newElement);
			} else {
				xmlEventManager.triggerElementAlreadyExists(oldElement);
				return;
			}
		} else {
			elements.add(newElement);
		}
		saveContainer();
		xmlEventManager.triggerModifyElementCompleted(oldElement, newElement);
	}
	
	/**
	 * Adds or modifies an element based on a specified field for comparison.
	 *
	 * @param element        the element to be added or modified.
	 * @param fieldToCompare the field name used for comparison.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void addOrModifyElement(T element, String fieldToCompare) throws JAXBException {
		addOrModifyElement(element, fieldToCompare, true);
	}
	
	/**
	 * Adds or modifies an element based on a specified field for comparison, with an option to replace or not.
	 *
	 * @param element         the element to be added or modified.
	 * @param fieldToCompare  the field name used for comparison.
	 * @param replaceIfExists true to replace the existing element if found, false to avoid replacement.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void addOrModifyElement(T element, String fieldToCompare, boolean replaceIfExists) throws JAXBException {
		// Trigger event to indicate the start of the operation
		xmlEventManager.triggerAddOrModifyElementStarted(element);
		
		// Check if elements list is null and initialize it
		if (elements == null) {
			elements = new ArrayList<>();
		}
		
		// Check for a valid fieldToCompare
		if (fieldToCompare == null || fieldToCompare.isEmpty()) {
			xmlEventManager.triggerFieldNotSpecified(element, fieldToCompare);
			return;
		}
		
		// Verify if the field exists in the element's class using reflection
		if (!isFieldPresentInClass(element.getClass(), fieldToCompare)) {
			xmlEventManager.triggerFieldNotFound(element, fieldToCompare);
			return;
		}
		
		// Attempt to find the element based on the specified field
		Optional<T> existingElement = findElementByField(e -> equals(e, element, fieldToCompare));
		
		if (existingElement.isPresent()) {
			// Element found - check if we should replace it
			if (replaceIfExists) {
				elements.set(elements.indexOf(existingElement.get()), element);
			} else {
				// Trigger event for element already existing, without modification
				xmlEventManager.triggerElementAlreadyExists(element);
				return;
			}
		} else {
			// Element not found, add the new element to the list
			elements.add(element);
		}
		
		// Save the container after modification
		saveContainer();
		
		// Trigger event to indicate completion of the operation
		xmlEventManager.triggerAddOrModifyElementCompleted(element);
	}
	
	/**
	 * Checks if a field is present in the specified class.
	 *
	 * @param clazz     the class to check.
	 * @param fieldName the name of the field to check for.
	 * @return true if the field is present, false otherwise.
	 */
	private boolean isFieldPresentInClass(Class<?> clazz, String fieldName) {
		try {
			clazz.getDeclaredField(fieldName); // Try to get the field by name
			return true; // Field exists
		} catch (NoSuchFieldException e) {
			return false; // Field doesn't exist
		}
	}
	
	/**
	 * Deletes an element from the container.
	 *
	 * @param element the element to be deleted.
	 * @throws JAXBException if there is an error during XML processing.
	 */
	public void deleteElement(T element) throws JAXBException {
		xmlEventManager.triggerDeleteElementStarted(element);
		if (elements.remove(element)) {
			saveContainer();
			xmlEventManager.triggerDeleteElementCompleted(element);
		} else {
			xmlEventManager.triggerElementNotFound(element);
		}
	}
	
	/**
	 * Finds an element in the container by a specified predicate.
	 *
	 * @param predicate the predicate used to find the element.
	 * @return an Optional containing the found element, or an empty Optional if no element is found.
	 */
	private Optional<T> findElementByField(java.util.function.Predicate<T> predicate) {
		return elements.stream().filter(predicate).findFirst();
	}
	
	/**
	 * Finds a single element in the list of elements by matching a specified field name and its value.
	 *
	 * @param fieldName  the name of the field to search for. This must match the field name in the class.
	 * @param fieldValue the value of the field to match. Can be any object that is compatible with the field type.
	 * @return the found element of type T if a match is found; {@code null} if no element matches the criteria.
	 */
	public T findElementByFieldValue(String fieldName, Object fieldValue) {
		return elements.stream().filter(e -> {
			try {
				Field field = e.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				Object value = field.get(e);
				return Objects.equals(String.valueOf(value), String.valueOf(fieldValue));
			} catch (NoSuchFieldException | IllegalAccessException ex) {
				ex.printStackTrace();
				return false;
			}
		}).findFirst().orElse(null);
	}
	
	/**
	 * Finds all elements by a specific field name and value.
	 *
	 * @param fieldName  the name of the field to search for.
	 * @param fieldValue the value of the field to match.
	 * @return a list of matching elements.
	 */
	public List<T> findElementsByFieldValue(String fieldName, Object fieldValue) {
		return elements.stream().filter(e -> {
			try {
				Field field = e.getClass().getDeclaredField(fieldName);
				field.setAccessible(true);
				return Objects.equals(field.get(e), fieldValue);
			} catch (NoSuchFieldException | IllegalAccessException ex) {
				ex.printStackTrace();
				return false;
			}
		}).collect(Collectors.toList());
	}
	
	/**
	 * Loads the container from the XML file, deserializing the data into a list of elements.
	 *
	 * @return a list of elements loaded from the XML file.
	 * @throws JAXBException if there is an error during XML deserialization.
	 */
	private List<T> loadContainer() throws JAXBException {
		xmlEventManager.triggerLoadingStarted();
		File file = new File(filePath);
		if (!file.exists()) {
			xmlEventManager.triggerLoadingCompleted(new ArrayList<>());
			return new ArrayList<>();
		}
		
		JAXBContext context = JAXBContext.newInstance(ListWrapper.class, elementType);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		ListWrapper<T> wrapper = (ListWrapper<T>) unmarshaller.unmarshal(file);
		List<T> items = Optional.ofNullable(wrapper.getItems()).orElse(new ArrayList<>());
		xmlEventManager.triggerLoadingCompleted(items);
		return items;
	}
	
	/**
	 * Saves the current container (list of elements) to the XML file.
	 *
	 * @throws JAXBException if there is an error during XML serialization.
	 */
	private void saveContainer() throws JAXBException {
		xmlEventManager.triggerSavingStarted(elements);
		JAXBContext context = JAXBContext.newInstance(ListWrapper.class, elementType);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(new ListWrapper<>(elements), new File(filePath));
		xmlEventManager.triggerSavingCompleted();
	}
}

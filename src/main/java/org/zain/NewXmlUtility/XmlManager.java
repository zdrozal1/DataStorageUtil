package org.zain.NewXmlUtility;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.zain.NewXmlUtility.AnnotationTesting.BaseObjectIdentifier;
import org.zain.NewXmlUtility.AnnotationTesting.ParentObjectIdentifier;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for performing XML operations such as loading, saving, adding, modifying, and deleting objects
 * within an XML structure. The class utilizes reflection to dynamically locate getter and setter methods in
 * the parent and base object classes, enabling flexible manipulation of XML data.
 *
 * <p>Example of a parent object class (e.g., {@code CustomerOrders}):</p>
 * <pre>
 * {@code
 * @XmlRootElement
 * public class CustomerOrders {
 *
 *     @ParentObjectIdentifier
 *     private List<Order> orderList;
 *
 *     @XmlElement(name = "Order")
 *     public List<Order> getOrderList() {
 *         return orderList;
 *     }
 *
 *     public void setOrderList(List<Order> orderList) {
 *         this.orderList = orderList;
 *     }
 * }
 * }
 * </pre>
 *
 * <p>Example of a base object class (e.g., {@code Order}):</p>
 * <pre>
 * {@code
 * @XmlRootElement
 * public class Order {
 *
 *     @BaseObjectIdentifier
 *     private String orderID = "1";
 *
 *     private double orderAmount = 0.00;
 *
 *     @XmlElement
 *     public String getOrderID() {
 *         return orderID;
 *     }
 *
 *     public void setOrderID(String orderID) {
 *         this.orderID = orderID;
 *     }
 *
 *     @XmlElement
 *     public double getOrderAmount() {
 *         return orderAmount;
 *     }
 *
 *     public void setOrderAmount(double orderAmount) {
 *         this.orderAmount = orderAmount;
 *     }
 * }
 * }
 * </pre>
 *
 * <p>The {@code XmlManager} class can be used to perform operations on the {@code CustomerOrders}
 * (parent) object, which contains a list of {@code Order} (base) objects.</p>
 */
public class XmlManager {
	private final Class<?> parentObject;
	private final Class<?> baseObject;
	private final String identifierField;
	private final String parentIdentifierField;
	private String filePath;
	
	/**
	 * Constructs an {@code XmlManager} instance to manage XML operations for the specified XML file,
	 * parent object class, base object class, and their corresponding identifier fields.
	 *
	 * <pre>
	 * {@code
	 * // Initialize the XmlManager for CustomerOrders and Order classes,
	 * // "orderID" as the base identifier, and "orderList" as the parent identifier
	 * XmlManager xmlManager = new XmlManager("customer_orders.xml", CustomerOrders.class, Order.class);
	 * }
	 * </pre>
	 *
	 * <p>The parent object list identifier must be marked with the {@code @ParentObjectIdentifier} annotation.<br>
	 * The base object identifier is used for comparison and must be annotated with {@code @BaseObjectIdentifier}.</p>
	 *
	 * @param filePath     The path to the XML file for loading or saving data.
	 * @param parentObject The class representing the parent object (e.g., {@code CustomerOrders}).
	 * @param baseObject   The class representing the base object (e.g., {@code Order}).
	 */
	
	public XmlManager(String filePath, Class<?> parentObject, Class<?> baseObject) {
		this.filePath = filePath;
		this.parentObject = parentObject;
		this.baseObject = baseObject;
		
		try {
			identifierField = processAnnotations(baseObject.getDeclaredConstructor().newInstance(), BaseObjectIdentifier.class);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		try {
			parentIdentifierField = processAnnotations(parentObject.getDeclaredConstructor().newInstance(), ParentObjectIdentifier.class);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Processes the annotations on the specified object to identify the annotated field.
	 * Ensures that only one field is annotated with the specified annotation.
	 *
	 * @param obj            The object to inspect for annotations.
	 * @param annotationType The annotation type to look for.
	 * @return The name of the annotated field.
	 * @throws IllegalArgumentException If the input object or annotation type is null.
	 * @throws IllegalStateException    If more than one field is annotated or no annotated field is found.
	 */
	public static String processAnnotations(Object obj, Class<?> annotationType) {
		// Ensure the input object and annotation type are not null
		if (obj == null) {
			throw new IllegalArgumentException("Input object cannot be null");
		}
		if (annotationType == null) {
			throw new IllegalArgumentException("Annotation type cannot be null");
		}
		
		// Ensure the annotation type is indeed an annotation
		if (!annotationType.isAnnotation()) {
			throw new IllegalArgumentException(annotationType.getName() + " is not an annotation type.");
		}
		
		// Get the class of the provided object
		Class<?> clazz = obj.getClass();
		
		// Get all declared fields from the class
		Field[] fields = clazz.getDeclaredFields();
		
		// Counter to track how many fields have the annotation
		int annotatedCount = 0;
		
		// Iterate through all fields to check for annotations
		for (Field field : fields) {
			// Check if the field has the specified annotation
			if (field.isAnnotationPresent((Class<? extends Annotation>) annotationType)) {
				annotatedCount++;
				// Ensure only one annotation exists in the class
				if (annotatedCount > 1) {
					throw new IllegalStateException("Only one field can be annotated with @" + annotationType.getSimpleName() + " in the class " + clazz.getName());
				}
				try {
					field.setAccessible(true); // Ensure we can access private fields
					return field.getName();
				} catch (SecurityException e) {
					throw new IllegalStateException("Failed to access field " + field.getName(), e);
				}
			}
		}
		
		// If no annotated field is found, throw an exception
		throw new IllegalStateException("No field annotated with @" + annotationType.getSimpleName() + " found in class " + clazz.getName());
	}
	
	/**
	 * Loads the parent object from the specified XML file. If the file does not exist, a new instance of the
	 * parent object is created. The parent object class must be annotated with {@code @XmlRootElement}.
	 *
	 * @return The loaded or newly created parent object.
	 * @throws JAXBException If an error occurs while loading the XML or instantiating the parent object.
	 */
	
	public Object loadParentElement() throws JAXBException {
		File file = new File(filePath);
		if (!file.exists()) {
			try {
				return parentObject.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new JAXBException("Error creating a new instance of " + parentObject.getName(), e);
			}
		}
		
		try {
			JAXBContext context = JAXBContext.newInstance(parentObject);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			return unmarshaller.unmarshal(file);
		} catch (JAXBException e) {
			throw new JAXBException("Error unmarshalling the XML file", e);
		}
	}
	
	/**
	 * Saves the given parent object instance to the XML file.
	 *
	 * @param parentObjectInstance The parent object to be saved to the XML file.
	 * @throws JAXBException If an error occurs during the saving process.
	 */
	
	private void saveParentObject(Object parentObjectInstance) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(parentObject);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		
		File file = new File(filePath);
		marshaller.marshal(parentObjectInstance, file);
	}
	
	/**
	 * Adds or replaces a base object in the parent object based on the identifier field. If an existing object
	 * with the same identifier is found, it is replaced; otherwise, the new base object is added.
	 *
	 * @param baseObject The base object to be added or replaced in the parent object.
	 * @throws JAXBException If an error occurs while adding or replacing the base object.
	 */
	
	public void addOrReplaceBaseObject(Object baseObject) throws JAXBException {
		Object parentElement = loadParentElement();
		
		try {
			Method getListMethod = findMethod(parentObject, "get", parentIdentifierField);
			List<Object> list = (List<Object>) getListMethod.invoke(parentElement);
			
			if (list == null) {
				list = new ArrayList<>();
				Method setListMethod = findMethod(parentObject, "set", parentIdentifierField);
				setListMethod.invoke(parentElement, list);
			}
			
			Method getIdMethod = findMethod(baseObject.getClass(), "get", identifierField);
			Object identifierValue = getIdMethod.invoke(baseObject);
			
			boolean replaced = false;
			for (int i = 0; i < list.size(); i++) {
				Object existingObject = list.get(i);
				Method existingObjectIdMethod = findMethod(baseObject.getClass(), "get", identifierField);
				Object existingObjectId = existingObjectIdMethod.invoke(existingObject);
				
				if (existingObjectId.equals(identifierValue)) {
					list.set(i, baseObject);
					replaced = true;
					break;
				}
			}
			
			if (!replaced) {
				list.add(baseObject);
			}
			
			saveParentObject(parentElement);
			System.out.println("Base object added or replaced successfully.");
		} catch (Exception e) {
			throw new JAXBException("Error adding or replacing base object", e);
		}
	}
	
	/**
	 * Modifies an existing base object in the parent object by updating its fields.
	 *
	 * @param baseObject The base object to be modified in the parent object.
	 * @throws JAXBException If an error occurs during the modification of the base object.
	 */
	public void modifyBaseObject(Object baseObject) throws JAXBException {
		Object parentElement = loadParentElement();
		
		try {
			Method getListMethod = findMethod(parentObject, "get", parentIdentifierField);
			List<Object> list = (List<Object>) getListMethod.invoke(parentElement);
			
			if (list != null) {
				Method getIdMethod = findMethod(baseObject.getClass(), "get", identifierField);
				Object identifierValue = getIdMethod.invoke(baseObject);
				
				for (int i = 0; i < list.size(); i++) {
					Object existingObject = list.get(i);
					Method existingObjectIdMethod = findMethod(baseObject.getClass(), "get", identifierField);
					Object existingObjectId = existingObjectIdMethod.invoke(existingObject);
					
					if (existingObjectId.equals(identifierValue)) {
						list.set(i, baseObject);
						saveParentObject(parentElement);
						System.out.println("Base object with identifier " + identifierValue + " modified.");
						return;
					}
				}
				
				System.out.println("No object found with identifier " + identifierValue);
			}
		} catch (Exception e) {
			throw new JAXBException("Error modifying base object", e);
		}
	}
	
	/**
	 * Deletes a base object from the parent object using the specified identifier.
	 *
	 * @param identifier The identifier of the base object to be deleted.
	 * @throws JAXBException If an error occurs during the deletion process.
	 */
	
	public void deleteBaseObject(String identifier) throws JAXBException {
		Object parentElement = loadParentElement();
		
		try {
			Method getListMethod = findMethod(parentObject, "get", parentIdentifierField);
			List<?> list = (List<?>) getListMethod.invoke(parentElement);
			
			if (list != null) {
				Method getIdMethod = findMethod(baseObject, "get", identifierField);
				
				list.removeIf(e -> {
					try {
						return getIdMethod.invoke(e).equals(identifier);
					} catch (Exception ex) {
						ex.printStackTrace();
						return false;
					}
				});
				
				saveParentObject(parentElement);
				System.out.println("Base object with identifier " + identifier + " deleted.");
			}
		} catch (Exception e) {
			throw new JAXBException("Error deleting base object", e);
		}
	}
	
	/**
	 * Searches for the getter or setter method in the given class using reflection based on the provided method
	 * prefix and field name.
	 *
	 * @param clazz     The class in which to search for the method.
	 * @param prefix    The method name prefix, typically {@code "get"} for getters and {@code "set"} for setters.
	 * @param fieldName The name of the field for which the method is being searched.
	 * @return The found method.
	 * @throws NoSuchMethodException If no matching method is found.
	 */
	
	private Method findMethod(Class<?> clazz, String prefix, String fieldName) throws NoSuchMethodException {
		String methodName = prefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		
		if (prefix.equals("get")) {
			try {
				return clazz.getMethod(methodName);
			} catch (NoSuchMethodException e) {
				throw new NoSuchMethodException("No getter method found with name " + methodName + " in class " + clazz.getName());
			}
		}
		
		try {
			return clazz.getMethod(methodName, List.class);
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodException("No setter method found with name " + methodName + " and parameter type List in class " + clazz.getName());
		}
	}
	
	/**
	 * Gets the file path for the XML file.
	 *
	 * @return The current file path.
	 */
	public String getFilePath() {
		return this.filePath;
	}
	
	/**
	 * Sets the file path for the XML file.
	 *
	 * @param path The file path to set.
	 */
	public void setFilePath(String path) {
		this.filePath = path;
	}
}

package org.zain.XmlUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// XML EventManager Class
public class XmlEventManager {
	private Consumer<List<?>> onLoadingStartedHandler = items -> {
		System.out.println("Loading started.");
	};
	private Consumer<List<?>> onLoadingCompletedHandler = items -> {
		System.out.println("Loading completed with " + items.size() + " items.");
	};
	private Consumer<List<?>> onSavingStartedHandler = elements -> {
		System.out.println("Saving " + elements.size() + " elements.");
	};
	private Runnable onSavingCompletedHandler = () -> {
		System.out.println("Saving completed.");
	};
	private Consumer<Object> onAddElementStartedHandler = element -> {
		System.out.println("Adding element: " + element);
	};
	private Consumer<Object> onAddElementCompletedHandler = element -> {
		System.out.println("Added element: " + element);
	};
	private BiConsumer<Object, Object> onModifyElementStartedHandler = (oldElement, newElement) -> {
		System.out.println("Modifying element from " + oldElement + " to " + newElement);
	};
	private BiConsumer<Object, Object> onModifyElementCompletedHandler = (oldElement, newElement) -> {
		System.out.println("Modified element: " + oldElement + " -> " + newElement);
	};
	private Consumer<Object> onDeleteElementStartedHandler = element -> {
		System.out.println("Deleting element: " + element);
	};
	private Consumer<Object> onDeleteElementCompletedHandler = element -> {
		System.out.println("Deleted element: " + element);
	};
	private Consumer<Object> onElementAlreadyExistsHandler = element -> {
		System.out.println("Element already exists: " + element);
	};
	private Consumer<Object> onElementNotFoundHandler = element -> {
		System.out.println("Element not found: " + element);
	};
	private Consumer<Object> onAddOrModifyElementCompleted = (element) -> {
		System.out.println("Modified/Added element: " + element);
	};
	private Consumer<Object> onAddOrModifyElementStarted = (element) -> {
		System.out.println("Starting Modifying/Adding element: " + element);
	};
	private BiConsumer<Object, String> onFieldNotSpecifiedHandler = (element, field) -> {
		System.err.println("Field to compare is not specified for element " + element + " (Field: " + field + ")");
	};
	private BiConsumer<Object, String> onFieldNotFoundHandler = (element, field) -> {
		System.err.println("Field not found in element " + element + " (Field: " + field + ")");
	};
	
	// Setters for event handlers
	public void setOnLoadingStartedHandler(Consumer<List<?>> handler) {
		this.onLoadingStartedHandler = handler;
	}
	
	public void setOnLoadingCompletedHandler(Consumer<List<?>> handler) {
		this.onLoadingCompletedHandler = handler;
	}
	
	public void setOnSavingStartedHandler(Consumer<List<?>> handler) {
		this.onSavingStartedHandler = handler;
	}
	
	public void setOnSavingCompletedHandler(Runnable handler) {
		this.onSavingCompletedHandler = handler;
	}
	
	public void setOnAddElementStartedHandler(Consumer<Object> handler) {
		this.onAddElementStartedHandler = handler;
	}
	
	public void setOnAddElementCompletedHandler(Consumer<Object> handler) {
		this.onAddElementCompletedHandler = handler;
	}
	
	public void setOnModifyElementStartedHandler(BiConsumer<Object, Object> handler) {
		this.onModifyElementStartedHandler = handler;
	}
	
	public void setOnModifyElementCompletedHandler(BiConsumer<Object, Object> handler) {
		this.onModifyElementCompletedHandler = handler;
	}
	
	public void setOnDeleteElementStartedHandler(Consumer<Object> handler) {
		this.onDeleteElementStartedHandler = handler;
	}
	
	public void setOnDeleteElementCompletedHandler(Consumer<Object> handler) {
		this.onDeleteElementCompletedHandler = handler;
	}
	
	public void setOnElementAlreadyExistsHandler(Consumer<Object> handler) {
		this.onElementAlreadyExistsHandler = handler;
	}
	
	public void setOnElementNotFoundHandler(Consumer<Object> handler) {
		this.onElementNotFoundHandler = handler;
	}
	
	public void setOnAddOrModifyElementStarted(Consumer<Object> handler) {
		this.onAddOrModifyElementStarted = handler;
	}
	
	public void setOnAddOrModifyElementCompleted(Consumer<Object> handler) {
		this.onAddOrModifyElementCompleted = handler;
	}
	
	public void setOnFieldNotSpecifiedHandler(BiConsumer<Object, String> handler) {
		this.onFieldNotSpecifiedHandler = handler;
	}
	
	public void setOnFieldNotFoundHandler(BiConsumer<Object, String> handler) {
		this.onFieldNotFoundHandler = handler;
	}
	
	// Triggers for events
	public void triggerLoadingStarted() {
		onLoadingStartedHandler.accept(null);
	}
	
	public void triggerLoadingCompleted(List<?> items) {
		onLoadingCompletedHandler.accept(items);
	}
	
	public void triggerSavingStarted(List<?> elements) {
		onSavingStartedHandler.accept(elements);
	}
	
	public void triggerSavingCompleted() {
		onSavingCompletedHandler.run();
	}
	
	public void triggerAddElementStarted(Object element) {
		onAddElementStartedHandler.accept(element);
	}
	
	public void triggerAddElementCompleted(Object element) {
		onAddElementCompletedHandler.accept(element);
	}
	
	public void triggerModifyElementStarted(Object oldElement, Object newElement) {
		onModifyElementStartedHandler.accept(oldElement, newElement);
	}
	
	public void triggerModifyElementCompleted(Object oldElement, Object newElement) {
		onModifyElementCompletedHandler.accept(oldElement, newElement);
	}
	
	public void triggerDeleteElementStarted(Object element) {
		onDeleteElementStartedHandler.accept(element);
	}
	
	public void triggerDeleteElementCompleted(Object element) {
		onDeleteElementCompletedHandler.accept(element);
	}
	
	public void triggerElementAlreadyExists(Object element) {
		onElementAlreadyExistsHandler.accept(element);
	}
	
	public void triggerElementNotFound(Object element) {
		onElementNotFoundHandler.accept(element);
	}
	
	public void triggerAddOrModifyElementCompleted(Object element) {
		onAddOrModifyElementCompleted.accept(element);
	}
	
	public void triggerAddOrModifyElementStarted(Object element) {
		onAddOrModifyElementStarted.accept(element);
	}
	
	public void triggerFieldNotSpecified(Object element, String fieldToCompare) {
		onFieldNotSpecifiedHandler.accept(element, fieldToCompare);
	}
	
	public void triggerFieldNotFound(Object element, String fieldToCompare) {
		onFieldNotFoundHandler.accept(element, fieldToCompare);
	}
}

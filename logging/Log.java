package basicScript.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Log {
	private boolean isLogging = true;
	private String form = null;
	private Stack<String> previousForm = new Stack<>();
	private Map<String, Boolean> canFormsLog = new HashMap<>();
	private Map<String, List<String>> categories = new HashMap<>();

	public Log() {
		// do nothing
	}

	public void out(String message) {
		Boolean logFilter = false;// default to off
		if (isCategory(form)) {// current form is category
			Boolean canCategoryLog = canFormsLog.get(form);
			if (canCategoryLog != null) {
				logFilter = canCategoryLog;
			}
		} else {// current form is type
			List<String> categories = getCategories(this.form);
			if (categories.size() > 0) {// current type belongs to at least one category
				for (String category : categories) {
					if (canFormsLog.get(category) == true) {
						logFilter = true;
						break;
					}
				}
			}
			//type has a high precedence than category in determining logging behavior
			if (this.form != null) {// current type is defined
				Boolean canTypeLog = canFormsLog.get(this.form);
				if (canTypeLog != null) {
					logFilter = canTypeLog;
				}
			} else {// no type is specified
				logFilter = true;
			}
		}
		if (logFilter && isLogging) {
			console(message);
		}
	}

	public void console(String message) {
		System.out.println(message);
	}

	/**
	 * Turn on logging, some logs may be filtered by their type/categories
	 */
	public void on() {
		isLogging = true;
	}

	/**
	 * Turn off logging, no logs will be produced
	 */
	public void off() {
		isLogging = false;
	}
	
	/**
	 * Set logging behavior for a specific form (type/category)
	 * 
	 * @param form
	 *            the form to set
	 * @param isLogging
	 *            turn on logging or not
	 */
	public void setFormLog(String form, boolean isLogging) {
		canFormsLog.put(standardizeFormInput(form), isLogging);
	}
	
	public String getForm() {
		return this.form;
	}
	
	private void setForm(String form) {
		this.form = standardizeFormInput(form);
		this.previousForm.push(this.form);
	}

	public void reset() {
		if (previousForm.size() > 1) {
			this.previousForm.pop();
			this.form = this.previousForm.pop();
		} else {
			this.form = null;
		}
	}
	
	/**
	 * Set the type of the logger from now on
	 * 
	 * @param type
	 *            the type to set to
	 */
	public void setType(String type) {
		setForm(type);
	}

	/**
	 * Set the category of the logger from now on
	 * 
	 * @param category
	 *            the category to set to
	 */
	public void setCategory(String category) {
		setForm(category);
	}
	
	/**
	 * Categorize one or more types to a category
	 * 
	 * @param category
	 *            the category which contains the types
	 * @param types
	 *            the types to categorize
	 */
	public void categorize(String category, String... types) {
		this.categories.put(category, Arrays.asList(types));
	}

	/**
	 * Add one or more types to an existing category, new types that already exists
	 * in the category are not added. If the category does not exist, create a new
	 * one with the given types
	 * 
	 * @param category
	 *            the category to add to
	 * @param newTypes
	 *            the new types to include in the category
	 */
	public void addTypes(String category, String... newTypes) {
		List<String> oldTypes = this.categories.get(category);
		if (oldTypes == null) {// category does not exist
			categorize(category, newTypes);
		} else {// add to existing category
			for (String type : newTypes) {
				if (!oldTypes.contains(type)) {
					oldTypes.add(type);
				}
			}
		}
	}

	/**
	 * Test if a form is a category or not
	 * 
	 * @param form
	 *            a form to test
	 * @return
	 */
	private boolean isCategory(String form) {
		if (form != null && categories.containsKey(form)) {
			return true;
		}
		return false;
	}

	/**
	 * Get all the categories associated with a given type
	 * 
	 * @param type
	 *            the type to find categories
	 * @return
	 */
	private List<String> getCategories(String type) {
		List<String> categories = new ArrayList<>();
		for (String category : this.categories.keySet()) {
			if (inCategory(type, category)) {
				categories.add(category);
			}
		}
		return categories;
	}

	/**
	 * Test if a type belongs to a category
	 * 
	 * @param type
	 *            the type to test
	 * @param category
	 *            the category to test
	 * @return
	 */
	private boolean inCategory(String type, String category) {
		if (this.categories.get(category).contains(type)) {
			return true;
		}
		return false;
	}

	/**
	 * Standardize form input in String
	 * 
	 * @param form
	 *            String representation of a form
	 * @return
	 */
	private String standardizeFormInput(String form) {
		return form.toLowerCase();
	}
}

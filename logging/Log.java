package basicScript.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Log {
	private boolean isLogging = true;
	private String form = null;
	private Map<String, Boolean> canFormsLog = new HashMap<>();
	private Map<String, List<String>> categories = new HashMap<>();

	public void out(String message) {
		Boolean logFilter = false;// default to off
		if (isCategory(form)) {//current form is category
			Boolean canCategoryLog = canFormsLog.get(form);
			if (canCategoryLog != null) {
				logFilter = canCategoryLog;
			}
		} else {//current form is type
			List<String> categories = getCategories(this.form);
			if (categories.size() > 0) {// current type belongs to at least one category
				for (String category : categories) {
					if (canFormsLog.get(category) == true) {
						logFilter = true;
					}
				}
			} else if (this.form != null) {// current type is defined
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

	public void on() {
		isLogging = true;
	}

	public void off() {
		isLogging = false;
	}

	public void setType(String type) {
		this.form = standardizeFormInput(type);
	}

	public void setFormLog(String form, boolean isLogging) {
		canFormsLog.put(standardizeFormInput(form), isLogging);
	}

	public void setCategory(String category) {
		this.form = standardizeFormInput(category);
	}
	
	public void categorize(String category, String... types) {
		this.categories.put(category, Arrays.asList(types));
	}

	public void addType(String category, String... newTypes) {
		List<String> oldTypes = this.categories.get(category);
		for (String type : newTypes) {
			if (!oldTypes.contains(type)) {
				oldTypes.add(type);
			}
		}
	}

	private boolean isCategory(String form) {
		if (form != null && categories.containsKey(form)) {
			return true;
		}
		return false;
	}

	private List<String> getCategories(String type) {
		List<String> categories = new ArrayList<>();
		for (String category : this.categories.keySet()) {
			if (inCategory(type, category)) {
				categories.add(category);
			}
		}
		return categories;
	}

	private boolean inCategory(String type, String category) {
		if (this.categories.get(category).contains(type)) {
			return true;
		}
		return false;
	}

	private String standardizeFormInput(String form) {
		return form.toLowerCase();
	}
}

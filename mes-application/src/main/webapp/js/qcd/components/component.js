var QCD = QCD || {};
QCD.components = QCD.components || {};

QCD.components.Component = function(_element, _mainController) {
	
	QCD.components.Component.UPDATE_MODE_UPDATE = "update";
	QCD.components.Component.UPDATE_MODE_IGNORE = "ignore";
	
	var mainController = _mainController;
	var element = _element;
	this.elementPath = element.attr('id');
	var elementName = this.elementPath.split("-")[this.elementPath.split("-").length - 1];
	this.elementName = elementName;
	
	var options;
	
	var isVisible = null;
	var isEnabled = null;
	
	var updateMode = QCD.components.Component.UPDATE_MODE_UPDATE;
	
	function constructor(_this) {
		options = QCDOptions.getElementOptions(_this.elementPath);
		_this.options = options;
	}
	
	this.getValue = function() {
		var mode = updateMode;
		if (this.getUpdateMode) {
			mode = this.getUpdateMode();
		}
		return {
			enabled: this.isEnabled(),
			visible: this.isVisible(),
			value: this.getComponentValue(),
			updateMode: mode,
			components: this.getComponentsValue()
		}
	}
	
	this.setValue = function(value) {
		this.setEnabled(value.enabled);
		this.setVisible(value.visible);
		if (value.value != null) {
			this.setComponentValue(value.value);
		}
		this.setMessages({
			error: value.errorMessages,
			info: value.infoMessages,
			success: value.successMessages
		});
		if (value.components) {
			this.setComponentsValue(value);
		}
		updateMode = QCD.components.Component.UPDATE_MODE_UPDATE;
	}
	
	this.setState = function(state) {
		this.setEnabled(state.enabled);
		this.setVisible(state.visible);
		if (this.setComponentState) {
			this.setComponentState(state.value);
		} else {
			QCD.error(this.elementPath+".setComponentState() no implemented");
		}
		if (state.components) {
			this.setComponentsState(state);
		}
		updateMode = QCD.components.Component.UPDATE_MODE_IGNORE;
	}
	
	this.setLoading = function(isLoadingVisible) {
		var listeners = options.listeners;
		if (listeners) {
			for (var i in listeners) {
				mainController.getComponent(listeners[i]).setLoading(isLoadingVisible);
			}
		}
		if (this.setComponentLoading) {
			this.setComponentLoading(isLoadingVisible);
		} else {
			QCD.error(this.elementPath+".setLoading() no implemented");
		}
	}
	
	this.getComponent = function(componentName) {
		if (! componentName || componentName.trim() == "") {
			return this;
		} else {
			QCD.error("no component");
		}
	}
	
	this.setMessages = function(messages) {
	}
	
	this.getComponentsValue = function() {
		return null;
	}
	this.setComponentsValue = function() {
		
	}
	
	this.setEnabled = function(_isEnabled) {
		isEnabled = _isEnabled;
		this.setComponentEnabled(isEnabled);
		//if (this.isContainer) {
		//	this.setComponentsEnabled(isEnabled);	
		//}
	}
	
	this.isEnabled = function() {
		return isEnabled;
	}
	
	this.setVisible = function(_isVisible) {
		isVisible = _isVisible;
		if (this.setComponentVisible) {
			this.setComponentVisible(isVisible);
		} else {
			if (isVisible) {
				element.show();
			} else {
				element.hide();
			}
		}
		
	}
	
	this.isVisible = function() {
		return isVisible;
	}
	
	constructor(this);
	
}
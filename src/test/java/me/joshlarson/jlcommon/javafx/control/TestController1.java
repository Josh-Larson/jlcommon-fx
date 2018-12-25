package me.joshlarson.jlcommon.javafx.control;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import me.joshlarson.jlcommon.javafx.control.FXMLService;
import org.junit.Assert;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TestController1 extends FXMLService implements Initializable {
	
	static final AtomicInteger FUNCTION_OPERATIONS = new AtomicInteger(0);
	static final AtomicBoolean SWITCHED_LOCALE = new AtomicBoolean(false);
	
	@FXML
	private Label label;
	
	public TestController1() {
		
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
	}
	
	@Override
	public boolean initialize() {
		FUNCTION_OPERATIONS.incrementAndGet();
		Assert.assertEquals("Hello World", label.getText());
		if (getLocale() == Locale.GERMAN)
			SWITCHED_LOCALE.set(true);
		return true;
	}
	
	@Override
	public boolean start() {
		FUNCTION_OPERATIONS.incrementAndGet();
		return true;
	}
	
	@Override
	public boolean stop() {
		FUNCTION_OPERATIONS.incrementAndGet();
		return true;
	}
	
	@Override
	public boolean terminate() {
		FUNCTION_OPERATIONS.incrementAndGet();
		return true;
	}
	
}

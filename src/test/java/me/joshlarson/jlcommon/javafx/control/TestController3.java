package me.joshlarson.jlcommon.javafx.control;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestController3 implements Initializable {
	
	static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
	
	public TestController3() {
		
	}
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		INITIALIZED.set(true);
	}
	
}

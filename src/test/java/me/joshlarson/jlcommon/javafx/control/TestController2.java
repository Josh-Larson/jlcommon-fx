package me.joshlarson.jlcommon.javafx.control;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import me.joshlarson.jlcommon.javafx.control.FXMLController;
import me.joshlarson.jlcommon.javafx.control.FXMLService;
import org.junit.Assert;

public class TestController2 extends FXMLService implements FXMLController {
	
	@FXML
	private Pane root;
	@FXML
	private Label label;
	
	public TestController2() {
		
	}
	
	@Override
	public Parent getRoot() {
		return root;
	}
	
	@Override
	public boolean initialize() {
		Assert.assertNotNull(getIntentManager());
		switch (getLocale().getLanguage()) {
			case "en":
				Assert.assertEquals("Hello", label.getText());
				break;
			case "de":
				Assert.assertEquals("Hallo", label.getText());
				break;
			default:
				Assert.fail();
				break;
		}
		return true;
	}
	
	@Override
	public boolean start() {
		Stage stage = new Stage();
		stage.setScene(new Scene(root));
		stage.setWidth(1000);
		stage.show();
		return true;
	}
	
}

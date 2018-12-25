package me.joshlarson.jlcommon.javafx.control;

import javafx.fxml.Initializable;
import javafx.scene.Parent;

import java.net.URL;
import java.util.ResourceBundle;

public interface FXMLController extends Initializable {
	
	@Override
	default void initialize(URL location, ResourceBundle resources) {}
	
	/**
	 * Returns the root node of this controller
	 * @return the root node
	 */
	Parent getRoot();
	
}

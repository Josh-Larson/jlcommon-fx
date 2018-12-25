module me.joshlarson.jlcommon.javafx {
	requires static javafx.controls;
	requires static javafx.fxml;
	requires static org.jetbrains.annotations;
	
	requires me.joshlarson.jlcommon;
	
	exports me.joshlarson.jlcommon.javafx.beans;
	exports me.joshlarson.jlcommon.javafx.control;
	
	opens me.joshlarson.jlcommon.javafx.control;
}

package me.joshlarson.jlcommon.javafx.control;

import javafx.application.Application;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import me.joshlarson.jlcommon.annotations.Unused;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public abstract class FXMLService extends Service {
	
	private final AtomicReference<FXMLManager> manager;
	
	public FXMLService() {
		this.manager = new AtomicReference<>(null);
	}
	
	@Unused(reason="API")
	@SuppressWarnings("unchecked")
	protected <T extends Initializable> T loadFxmlFromClassResource(String url) {
		return (T) getManager().loadFxml(getManager().getKlass().getResource(url));
	}
	
	@Unused(reason="API")
	@SuppressWarnings("unchecked")
	protected <T extends Initializable> T loadFxmlFromClassResource(URL url) {
		return (T) getManager().loadFxml(url);
	}
	
	@Unused(reason="API")
	@SuppressWarnings("unchecked")
	protected <T extends Initializable> T loadFxmlFromFile(File file) {
		try {
			return (T) getManager().loadFxml(file.toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Unused(reason="API")
	protected void reinflate() {
		getManager().reinflate();
	}
	
	@Unused(reason="API")
	protected void reinflate(Locale locale) {
		getManager().reinflate(locale);
	}
	
	@NotNull
	@Unused(reason="API")
	protected ResourceBundle getResourceBundle() {
		return getManager().getResourceBundle();
	}
	
	@NotNull
	@Unused(reason="API")
	protected Locale getLocale() {
		return getManager().getLocale();
	}
	
	@NotNull
	@Unused(reason="API")
	protected Stage getPrimaryStage() {
		return getManager().getPrimaryStage();
	}
	
	@NotNull
	@Unused(reason="API")
	protected Application getApplication() {
		return getManager().getApplication();
	}
	
	void setManager(FXMLManager manager) {
		if (!this.manager.compareAndSet(null, manager))
			throw new UnsupportedOperationException("Cannot set new manager! Already set");
	}
	
	@NotNull
	private FXMLManager getManager() {
		return Objects.requireNonNull(manager.get(), "controller is not initialized");
	}
}

package me.joshlarson.jlcommon.javafx.control;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import me.joshlarson.jlcommon.control.IntentManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ServiceBase;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.utilities.Arguments;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class FXMLManager implements ServiceBase {
	
	private final AtomicReference<Locale> locale;
	private final AtomicReference<ResourceBundle> resourceBundle;
	private final AtomicReference<IntentManager> intentManager;
	private final Class<?> klass;
	private final String [] args;
	private final List<URL> controllerUrls;
	private final List<Class<? extends ServiceBase>> customServices;
	private final List<ServiceBase> controllers;
	private final List<ServiceBase> initialized;
	private final List<ServiceBase> started;
	
	private FXMLManager(FXMLManagerBuilder builder) {
		this.klass = builder.getKlass();
		this.locale = new AtomicReference<>(builder.getLocale());
		this.resourceBundle = new AtomicReference<>(null);
		this.intentManager = new AtomicReference<>(null);
		this.args = Objects.requireNonNull(builder.getArgs(), "args");
		
		String resourceBundlePath = builder.getResourceBundlePath();
		if (resourceBundlePath != null) {
			Objects.requireNonNull(klass, "klass must be set if resourceBundlePath is set!");
			Objects.requireNonNull(locale.get(), "locale must be set if resourceBundlePath is set!");
			this.resourceBundle.set(ResourceBundle.getBundle(resourceBundlePath, locale.get(), klass.getModule()));
		}
		
		this.controllerUrls = List.copyOf(builder.getFxml());
		this.customServices = List.copyOf(builder.getServices());
		this.controllers = new CopyOnWriteArrayList<>();
		this.initialized = new CopyOnWriteArrayList<>();
		this.started = new CopyOnWriteArrayList<>();
	}
	
	@Override
	public boolean initialize() {
		FXMLApplication.ensureStarted(args);
		controllers.clear();
		return runOnFxAndWait(() -> {
			for (URL url : controllerUrls) {
				createChild(url);
			}
			for (Class<? extends ServiceBase> serviceClass : customServices) {
				try {
					setupChild(serviceClass.getConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					Log.e(e);
				}
			}
			for (ServiceBase child : controllers) {
				if (!initializeChild(child))
					return false;
			}
			return true;
		});
	}
	
	@Override
	public boolean start() {
		return runOnFxAndWait(() -> {
			for (ServiceBase child : controllers) {
				if (!startChild(child))
					return false;
			}
			return true;
		});
	}
	
	@Override
	public boolean stop() {
		boolean success = true;
		for (ServiceBase child : started) {
			try {
				Log.t("%s: Stopping %s...", getClass().getSimpleName(), child.getClass().getSimpleName());
				if (!child.stop()) {
					Log.e(child.getClass().getSimpleName() + " failed to stop!");
					success = false;
				}
			} catch (Throwable t) {
				Log.e("Caught exception during stop. Service: %s", child.getClass().getName());
				Log.e(t);
				success = false;
			}
		}
		started.clear();
		return success;
	}
	
	@Override
	public boolean terminate() {
		boolean success = true;
		for (ServiceBase child : initialized) {
			try {
				Log.t("%s: Terminating %s...", getClass().getSimpleName(), child.getClass().getSimpleName());
				if (!child.terminate()) {
					Log.e(child.getClass().getSimpleName() + " failed to terminate!");
					success = false;
				}
			} catch (Throwable t) {
				Log.e("Caught exception during terminate. Service: %s", child.getClass().getName());
				Log.e(t);
				success = false;
			}
		}
		initialized.clear();
		return success;
	}
	
	@Override
	public boolean isOperational() {
		for (ServiceBase child : controllers) {
			if (!child.isOperational()) {
				Log.e("Child '%s' is no longer operational.", child.getClass().getName());
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void setIntentManager(IntentManager intentManager) {
		this.intentManager.set(intentManager);
		for (ServiceBase child : controllers) {
			child.setIntentManager(intentManager);
		}
	}
	
	protected Initializable loadFxml(@NotNull URL url) {
		Initializable controller = createChild(url);
		if (controller instanceof ServiceBase) {
			ServiceBase service = (ServiceBase) controller;
			initializeChild(service);
			startChild(service);
		}
		return controller;
	}
	
	void reinflate() {
		stop();
		terminate();
		initialize();
		start();
	}
	
	void reinflate(Locale locale) {
		this.locale.set(locale);
		this.resourceBundle.updateAndGet(prev -> (prev==null||klass==null) ? null : ResourceBundle.getBundle("strings", this.locale.get(), klass.getClassLoader()));
		reinflate();
	}
	
	@NotNull
	Class<?> getKlass() {
		return Objects.requireNonNull(klass, "klass not set");
	}
	
	@NotNull
	Locale getLocale() {
		return Objects.requireNonNull(locale.get(), "locale not set");
	}
	
	@NotNull
	ResourceBundle getResourceBundle() {
		return Objects.requireNonNull(resourceBundle.get(), "resourceBundle not set");
	}
	
	@NotNull
	Stage getPrimaryStage() {
		return Objects.requireNonNull(FXMLApplication.getApplication(), "manager has not been initialized").getPrimaryStage();
	}
	
	@NotNull
	Application getApplication() {
		return Objects.requireNonNull(FXMLApplication.getApplication(), "manager has not been initialized");
	}
	
	private Initializable createChild(URL url) {
		try {
			Objects.requireNonNull(url, "url");
			FXMLLoader fxmlLoader = new FXMLLoader(url);
			fxmlLoader.setControllerFactory(param -> {
				try {
					Constructor<?> c = param.getConstructor();
					Object obj = c.newInstance();
					setupChild(obj);
					return obj;
				} catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
					throw new RuntimeException(e);
				}
			});
			ResourceBundle resourceBundle = this.resourceBundle.get();
			if (resourceBundle != null)
				fxmlLoader.setResources(resourceBundle);
			fxmlLoader.load();
			return fxmlLoader.getController();
		} catch (IOException e) {
			throw new RuntimeException("Failed to load fxml: " + url, e);
		}
	}
	
	private void setupChild(Object controller) {
		setupChild(controller, true);
	}
	
	private void setupChild(Object controller, boolean addToControllers) {
		if (controller instanceof FXMLService)
			((FXMLService) controller).setManager(this);
		
		if (controller instanceof Manager) {
			for (ServiceBase child : ((Manager) controller).getChildren())
				setupChild(child, false);
		}
		
		if (addToControllers && controller instanceof ServiceBase) {
			ServiceBase service = (ServiceBase) controller;
			controllers.add(service);
			service.setIntentManager(intentManager.get());
		}
	}
	
	private boolean initializeChild(ServiceBase child) {
		if (initialized.contains(child))
			return true;
		try {
			Log.t("%s: Initializing %s...", getClass().getSimpleName(), child.getClass().getSimpleName());
			if (!child.initialize()) {
				Log.e(child.getClass().getSimpleName() + " failed to initialize!");
				return false;
			}
		} catch (Throwable t) {
			Log.e("Caught exception during initialize. Service: %s", child.getClass().getName());
			Log.e(t);
			return false;
		}
		initialized.add(child);
		return true;
	}
	
	private boolean startChild(ServiceBase child) {
		if (started.contains(child))
			return true;
		try {
			Log.t("%s: Starting %s...", getClass().getSimpleName(), child.getClass().getSimpleName());
			if (!child.start()) {
				Log.e(child.getClass().getSimpleName() + " failed to start!");
				return false;
			}
		} catch (Throwable t) {
			Log.e("Caught exception during start. Service: %s", child.getClass().getName());
			Log.e(t);
			return false;
		}
		started.add(child);
		return true;
	}
	
	public static FXMLManagerBuilder builder() {
		return new FXMLManagerBuilder();
	}
	
	private static boolean runOnFxAndWait(Supplier<Boolean> run) {
		AtomicBoolean result = new AtomicBoolean(true);
		boolean completed = runOnFxAndWait(() -> result.set(run.get()));
		return completed && result.get();
	}
	
	private static boolean runOnFxAndWait(Runnable run) {
		Semaphore lock = new Semaphore(0);
		Platform.runLater(() -> {
			try {
				run.run();
			} finally {
				lock.release(1);
			}
		});
		try {
			lock.acquire(1);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}
	
	public static final class FXMLManagerBuilder {
		
		private final List<URL> fxml = new ArrayList<>();
		private final List<Class<? extends ServiceBase>> services = new ArrayList<>();
		
		private Class<?> klass = null;
		private Locale locale = null;
		private String resourceBundlePath = null;
		private String [] args = new String[0];
		
		private FXMLManagerBuilder() {
			
		}
		
		public FXMLManagerBuilder addFxml(URL url) {
			fxml.add(url);
			return this;
		}
		
		public FXMLManagerBuilder addFxml(File file) {
			try {
				return addFxml(file.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		
		public FXMLManagerBuilder addService(Class<? extends ServiceBase> klass) {
			Arguments.validate(hasDefaultConstructor(klass), "service class must have a default constructor to allow instantiation");
			services.add(klass);
			return this;
		}
		
		public FXMLManagerBuilder withKlass(Class<?> klass) {
			this.klass = klass;
			return this;
		}
		
		public FXMLManagerBuilder withLocale(Locale locale) {
			this.locale = locale;
			return this;
		}
		
		public FXMLManagerBuilder withResourceBundlePath(String resourceBundlePath) {
			this.resourceBundlePath = resourceBundlePath;
			return this;
		}
		
		public FXMLManagerBuilder withArgs(String[] args) {
			this.args = Objects.requireNonNull(args, "args cannot be null");
			return this;
		}
		
		public FXMLManager build() {
			return new FXMLManager(this);
		}
		
		private List<URL> getFxml() {
			return fxml;
		}
		
		private List<Class<? extends ServiceBase>> getServices() {
			return services;
		}
		
		private Class<?> getKlass() {
			return klass;
		}
		
		private Locale getLocale() {
			return locale;
		}
		
		private String getResourceBundlePath() {
			return resourceBundlePath;
		}
		
		private String[] getArgs() {
			return args;
		}
		
		private static boolean hasDefaultConstructor(Class<? extends ServiceBase> klass) {
			try {
				return klass.getConstructor() != null;
			} catch (NoSuchMethodException e) {
				return false;
			}
		}
		
	}
	
}

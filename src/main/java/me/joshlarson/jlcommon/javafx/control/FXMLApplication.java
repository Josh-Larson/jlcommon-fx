package me.joshlarson.jlcommon.javafx.control;

import javafx.application.Application;
import javafx.stage.Stage;
import me.joshlarson.jlcommon.concurrency.Delay;
import me.joshlarson.jlcommon.concurrency.ThreadPool;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class FXMLApplication extends Application {
	
	private static final ThreadPool RUN_THREAD = new ThreadPool(1, "jlcommon-fx-application-thread");
	private static final AtomicReference<FXMLApplication> INSTANCE = new AtomicReference<>(null);
	
	private Stage primaryStage;
	
	public FXMLApplication() {
		this.primaryStage = null;
	}
	
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		Application prev = INSTANCE.getAndSet(this);
		assert prev == null : "executing application twice!";
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
		INSTANCE.set(null);
	}
	
	@NotNull
	public Stage getPrimaryStage() {
		return Objects.requireNonNull(primaryStage, "application not initialized");
	}
	
	public static void ensureStarted(String ... args) {
		if (!RUN_THREAD.isRunning()) {
			RUN_THREAD.start();
			RUN_THREAD.execute(() -> launch(args));
			while (INSTANCE.get() == null)
				Delay.sleepMicro(1);
		} else {
			if (args.length > 0)
				Log.w("Attempted to launch fxml application using arguments, but was already running");
		}
	}
	
	@NotNull
	public static FXMLApplication getApplication() {
		return Objects.requireNonNull(INSTANCE.get(), "FXMLApplication has not been initialized yet");
	}
	
}

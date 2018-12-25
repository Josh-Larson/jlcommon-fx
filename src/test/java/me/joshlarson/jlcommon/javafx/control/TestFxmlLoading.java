package me.joshlarson.jlcommon.javafx.control;

import me.joshlarson.jlcommon.control.IntentManager;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(JUnit4.class)
public class TestFxmlLoading {
	
	@BeforeClass
	public static void init() {
		Log.addWrapper(new ConsoleLogWrapper());
	}
	
	@Before
	public void reset() {
		TestService.FUNCTION_OPERATIONS.set(0);
		TestController1.FUNCTION_OPERATIONS.set(0);
		TestController1.SWITCHED_LOCALE.set(false);
		TestController3.INITIALIZED.set(false);
	}
	
	@Test
	public void testController1() {
		FXMLManager manager = FXMLManager.builder()
				.withLocale(Locale.US)
				.addFxml(getClass().getResource("/test1.fxml"))
				.build();
		
		Manager.start(Collections.singleton(manager));
		manager.reinflate();
		Manager.stop(Collections.singleton(manager));
		Manager.start(Collections.singleton(manager));
		manager.reinflate(Locale.GERMAN);
		Manager.stop(Collections.singleton(manager));
		
		// 4 operations (init/start/stop/term) with 4 inflations (2 standard, 2 reinflate)
		Assert.assertEquals(4 * 4, TestController1.FUNCTION_OPERATIONS.get());
		Assert.assertTrue(TestController1.SWITCHED_LOCALE.get());
	}
	
	@Test
	public void testController2() {
		FXMLManager manager = FXMLManager.builder()
				.withKlass(getClass())
				.withLocale(Locale.US)
				.withResourceBundlePath("strings")
				.addFxml(getClass().getResource("/test2.fxml"))
				.build();
		manager.setIntentManager(new IntentManager(0));
		Manager.start(Collections.singleton(manager));
		manager.reinflate(Locale.GERMAN);
		Manager.stop(Collections.singleton(manager));
	}
	
	@Test
	public void testController3() {
		FXMLManager manager = FXMLManager.builder().withLocale(Locale.US).addFxml(getClass().getResource("/test3.fxml")).build();
		Manager.start(Collections.singleton(manager));
		Assert.assertTrue(TestController3.INITIALIZED.get());
		Manager.stop(Collections.singleton(manager));
		Assert.assertEquals(4, TestController1.FUNCTION_OPERATIONS.get());
	}
	
	@Test
	public void testServices() {
		FXMLManager manager = FXMLManager.builder().withLocale(Locale.US).withKlass(getClass()).withResourceBundlePath("strings").addService(TestService.class).build();
		manager.setIntentManager(new IntentManager(0));
		Manager.start(Collections.singleton(manager));
		Assert.assertEquals(2, TestService.FUNCTION_OPERATIONS.get());
		Manager.stop(Collections.singleton(manager));
		Assert.assertEquals(4, TestService.FUNCTION_OPERATIONS.get());
	}
	
	@Test
	public void testManagers() {
		FXMLManager manager = FXMLManager.builder().withLocale(Locale.US).withKlass(getClass()).withResourceBundlePath("strings").addService(TestManager.class).build();
		manager.setIntentManager(new IntentManager(0));
		Manager.start(Collections.singleton(manager));
		Assert.assertEquals(2, TestService.FUNCTION_OPERATIONS.get());
		Manager.stop(Collections.singleton(manager));
		Assert.assertEquals(4, TestService.FUNCTION_OPERATIONS.get());
	}
	
	@ManagerStructure(children = {
			TestService.class
	})
	public static class TestManager extends Manager {
		
	}
	
	public static class TestService extends FXMLService {
		
		private static final AtomicInteger FUNCTION_OPERATIONS = new AtomicInteger(0);
		
		@Override
		public boolean initialize() {
			FUNCTION_OPERATIONS.incrementAndGet();
			getLocale();
			getPrimaryStage();
			getResourceBundle();
			getApplication();
			Assert.assertNotNull(loadFxmlFromClassResource("/test1.fxml"));
			Assert.assertNotNull(getIntentManager());
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
	
}

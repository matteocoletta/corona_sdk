//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2017 Adjust GmbH. All rights reserved.
//

// This corresponds to the name of the Lua library, e.g. [Lua] require "plugin.library"
// Adjust SDK is named "plugin.adjust"
package plugin.adjust;

import android.net.Uri;
import android.util.Log;
import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEvent;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.AdjustSessionFailure;
import com.adjust.sdk.AdjustSessionSuccess;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.adjust.sdk.OnDeeplinkResponseListener;
import com.adjust.sdk.OnDeviceIdsRead;
import com.adjust.sdk.OnEventTrackingFailedListener;
import com.adjust.sdk.OnEventTrackingSucceededListener;
import com.adjust.sdk.OnSessionTrackingFailedListener;
import com.adjust.sdk.OnSessionTrackingSucceededListener;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;
import org.json.JSONObject;

/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings("WeakerAccess")
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	private static final String TAG = "LuaLoader";

	// Event names - names are not necessary from Lua side
	public static final String EVENT_ATTRIBUTION_CHANGED = "adjust_attribution";
	public static final String EVENT_SESSION_TRACKING_SUCCESS = "adjust_sessionTrackingSuccess";
	public static final String EVENT_SESSION_TRACKING_FAILURE = "adjust_sessionTrackingFailure";
	public static final String EVENT_EVENT_TRACKING_SUCCESS = "adjust_eventTrackingSuccess";
	public static final String EVENT_EVENT_TRACKING_FAILURE = "adjust_eventTrackingFailure";
	public static final String EVENT_DEFERRED_DEEPLINK = "adjust_deferredDeeplink";
	public static final String EVENT_IS_ADJUST_ENABLED = "adjust_isEnabled";
	public static final String EVENT_GET_IDFA = "adjust_getIdfa";
	public static final String EVENT_GET_ATTRIBUTION = "adjust_getAttribution";
	public static final String EVENT_GET_ADID = "adjust_getAdid";
	public static final String EVENT_GET_GOOGLE_AD_ID = "adjust_getGoogleAdId";
	public static final String EVENT_GET_AMAZON_AD_ID = "adjust_getAmazonAdId";

	// Listeners
	private int attributionChangedListener;
	private int eventTrackingSuccessListener;
	private int eventTrackingFailureListener;
	private int sessionTrackingSuccessListener;
	private int sessionTrackingFailureListener;
	private int deferredDeeplinkListener;

	private Uri uri = null;
	private boolean didStartAdjustSdk = false;
	private boolean shouldLaunchDeeplink = true;

	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	@SuppressWarnings("unused")
	public LuaLoader() {
		// Initialize listeners to REFNIL
		attributionChangedListener = CoronaLua.REFNIL;
		eventTrackingSuccessListener = CoronaLua.REFNIL;
		eventTrackingFailureListener = CoronaLua.REFNIL;
		sessionTrackingSuccessListener = CoronaLua.REFNIL;
		sessionTrackingFailureListener = CoronaLua.REFNIL;
		deferredDeeplinkListener = CoronaLua.REFNIL;

		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called every time a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 *
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 * <p>
	 * Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
				new CreateWrapper(),
				new TrackEventWrapper(),
				new SetEnabledWrapper(),
				new IsEnabledWrapper(),
				new SetReferrerWrapper(),
				new SetOfflineModeWrapper(),
				new SetPushTokenWrapper(),
				new AppWillOpenUrlWrapper(),
				new SendFirstPackageWrapper(),
				new AddSessionCallbackParameterWrapper(),
				new AddSessionPartnerParameterWrapper(),
				new RemoveSessionCallbackParameterWrapper(),
				new RemoveSessionPartnerParameterWrapper(),
				new ResetSessionCallbackParametersWrapper(),
				new ResetSessionPartnerParametersWrapper(),
				new GetIdfaWrapper(),
				new GetAttributionWrapper(),
				new SetAttributionListenerWrapper(),
				new SetEventTrackingSuccessListenerWrapper(),
				new SetEventTrackingFailureListenerWrapper(),
				new SetSessionTrackingSuccessListenerWrapper(),
				new SetSessionTrackingFailureListenerWrapper(),
				new SetDeferredDeeplinkListenerWrapper(),
				new GetAdidWrapper(),
				new GetGoogleAdIdWrapper(),
				new GetAmazonAdIdWrapper()
		};
		String libName = L.toString(1);
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 *
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.
	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 *
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 *
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
		Adjust.onPause();
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 *
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
		Adjust.onResume();
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 *
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
		// Remove the Lua listener reference.
		CoronaLua.deleteRef(runtime.getLuaState(), attributionChangedListener);
		CoronaLua.deleteRef(runtime.getLuaState(), sessionTrackingSuccessListener);
		CoronaLua.deleteRef(runtime.getLuaState(), sessionTrackingFailureListener);
		CoronaLua.deleteRef(runtime.getLuaState(), eventTrackingSuccessListener);
		CoronaLua.deleteRef(runtime.getLuaState(), eventTrackingFailureListener);
		CoronaLua.deleteRef(runtime.getLuaState(), deferredDeeplinkListener);

		attributionChangedListener = CoronaLua.REFNIL;
		eventTrackingSuccessListener = CoronaLua.REFNIL;
		eventTrackingFailureListener = CoronaLua.REFNIL;
		sessionTrackingSuccessListener = CoronaLua.REFNIL;
		sessionTrackingFailureListener = CoronaLua.REFNIL;
		deferredDeeplinkListener = CoronaLua.REFNIL;
	}

	private void dispatchEvent(final LuaState luaState, final int listener, final String name, final String message) {
		CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send(new CoronaRuntimeTask() {
			@Override
			public void executeUsing(CoronaRuntime runtime) {
				CoronaLua.newEvent(luaState, name);

				luaState.pushString(message);
				luaState.setField(-2, "message");

				// Dispatch event to library's listener
				try {
					CoronaLua.dispatchEvent(luaState, listener, 0);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Invokes Adjust.onCreate()
	 * Takes a hash table as input. The hash table is loaded on a stack which needs to be popped for the
	 * next element to be used
	 */
	public int adjust_create(final LuaState L) {
		if (!L.isTable(1)) {
			Log.e(TAG, "adjust_create: adjust_create() must be supplied with a table");
			return 0;
		}

		String logLevel = null;
		String appToken = null;
        String userAgent = null;
		String environment = null;
        String processName = null;
		String defaultTracker = null;

		boolean readImei = false;
		boolean isDeviceKnown = false;
        boolean sendInBackground = false;
        boolean isLogLevelSuppress = false;
		boolean eventBufferingEnabled = false;

		double delayStart = 0.0;

		long secretId = -1L;
		long info1 = -1L;
		long info2 = -1L;
		long info3 = -1L;
		long info4 = -1L;

		// Log level
		L.getField(1, "logLevel");
		if (!L.isNil(2)) {
			logLevel = L.checkString(2);
			if (logLevel.toLowerCase().equals("suppress")) {
				isLogLevelSuppress = true;
			}
		}
		L.pop(1);

		// App token
		L.getField(1, "appToken");
		appToken = L.checkString(2);
		L.pop(1);

		// Environment
		L.getField(1, "environment");
		environment = L.checkString(2);
		if (environment != null) {
			if (environment.toLowerCase().equals("sandbox")) {
				environment = AdjustConfig.ENVIRONMENT_SANDBOX;
			} else if (environment.toLowerCase().equals("production")) {
				environment = AdjustConfig.ENVIRONMENT_PRODUCTION;
			}
		}
		L.pop(1);

		final AdjustConfig adjustConfig =
				new AdjustConfig(CoronaEnvironment.getApplicationContext(), appToken, environment, isLogLevelSuppress);

		// Log level
		if (logLevel != null) {
			if (logLevel.toLowerCase().equals("verbose")) {
				adjustConfig.setLogLevel(LogLevel.VERBOSE);
			} else if (logLevel.toLowerCase().equals("debug")) {
				adjustConfig.setLogLevel(LogLevel.DEBUG);
			} else if (logLevel.toLowerCase().equals("info")) {
				adjustConfig.setLogLevel(LogLevel.INFO);
			} else if (logLevel.toLowerCase().equals("warn")) {
				adjustConfig.setLogLevel(LogLevel.WARN);
			} else if (logLevel.toLowerCase().equals("error")) {
				adjustConfig.setLogLevel(LogLevel.ERROR);
			} else if (logLevel.toLowerCase().equals("assert")) {
				adjustConfig.setLogLevel(LogLevel.ASSERT);
			} else if (logLevel.toLowerCase().equals("suppress")) {
				adjustConfig.setLogLevel(LogLevel.SUPRESS);
			} else {
				adjustConfig.setLogLevel(LogLevel.INFO);
			}
		}

		// Event buffering
		L.getField(1, "eventBufferingEnabled");
		if (!L.isNil(2)) {
			eventBufferingEnabled = L.checkBoolean(2);
			adjustConfig.setEventBufferingEnabled(eventBufferingEnabled);
		}
		L.pop(1);

		// SDK prefix
		adjustConfig.setSdkPrefix("corona4.12.2");

		// Main process name
		L.getField(1, "processName");
		if (!L.isNil(2)) {
			processName = L.checkString(2);
			adjustConfig.setProcessName(processName);
		}
		L.pop(1);

		// Default tracker
		L.getField(1, "defaultTracker");
		if (!L.isNil(2)) {
			defaultTracker = L.checkString(2);
			adjustConfig.setDefaultTracker(defaultTracker);
		}
		L.pop(1);

		// User agent
		L.getField(1, "userAgent");
		if (!L.isNil(2)) {
			userAgent = L.checkString(2);
			adjustConfig.setUserAgent(userAgent);
		}
		L.pop(1);

		// Background tracking
		L.getField(1, "sendInBackground");
		if (!L.isNil(2)) {
			sendInBackground = L.checkBoolean(2);
			adjustConfig.setSendInBackground(sendInBackground);
		}
		L.pop(1);

		// Launching deferred deep link
		L.getField(1, "shouldLaunchDeeplink");
		if (!L.isNil(2)) {
			this.shouldLaunchDeeplink = L.checkBoolean(2);
		}
		L.pop(1);

		// Delay start
		L.getField(1, "delayStart");
		if (!L.isNil(2)) {
			delayStart = L.checkNumber(2);
			adjustConfig.setDelayStart(delayStart);
		}
		L.pop(1);

		// Device known
        L.getField(1, "isDeviceKnown");
        if (!L.isNil(2)) {
            isDeviceKnown = L.checkBoolean(2);
            adjustConfig.setDeviceKnown(isDeviceKnown);
        }
        L.pop(1);

        // IMEI tracking
        L.getField(1, "readMobileEquipmentIdentity");
        if (!L.isNil(2)) {
            readImei = L.checkBoolean(2);
            adjustConfig.setReadMobileEquipmentIdentity(readImei);
        }
        L.pop(1);

        // App secret
        L.getField(1, "secretId");
        if (!L.isNil(2)) {
            secretId = (long)L.checkNumber(2);
        }
        L.pop(1);
        L.getField(1, "info1");
        if (!L.isNil(2)) {
            info1 = (long)L.checkNumber(2);
        }
        L.pop(1);
        L.getField(1, "info2");
        if (!L.isNil(2)) {
            info2 = (long)L.checkNumber(2);
        }
        L.pop(1);
        L.getField(1, "info3");
        if (!L.isNil(2)) {
            info3 = (long)L.checkNumber(2);
        }
        L.pop(1);
        L.getField(1, "info4");
        if (!L.isNil(2)) {
            info4 = (long)L.checkNumber(2);
        }
        L.pop(1);

        if (secretId != -1 || info1 != -1 || info2 != -1 || info3 != -1 || info4 != -1) {
            adjustConfig.setAppSecret(secretId, info1, info2, info3, info4);
        }

		// Attribution callback
		if (this.attributionChangedListener != CoronaLua.REFNIL) {
			adjustConfig.setOnAttributionChangedListener(new OnAttributionChangedListener() {
				@Override
				public void onAttributionChanged(AdjustAttribution adjustAttribution) {
					dispatchEvent(L, LuaLoader.this.attributionChangedListener, EVENT_ATTRIBUTION_CHANGED, new JSONObject(LuaUtil.attributionToMap(adjustAttribution)).toString());
				}
			});
		}

		// Event tracking succeeded callback
		if (this.eventTrackingSuccessListener != CoronaLua.REFNIL) {
			adjustConfig.setOnEventTrackingSucceededListener(new OnEventTrackingSucceededListener() {
				@Override
				public void onFinishedEventTrackingSucceeded(AdjustEventSuccess adjustEventSuccess) {
					dispatchEvent(L, LuaLoader.this.eventTrackingSuccessListener, EVENT_EVENT_TRACKING_SUCCESS, new JSONObject(LuaUtil.eventSuccessToMap(adjustEventSuccess)).toString());
				}
			});
		}

		// Event tracking failed callback
		if (this.eventTrackingFailureListener != CoronaLua.REFNIL) {
			adjustConfig.setOnEventTrackingFailedListener(new OnEventTrackingFailedListener() {
				@Override
				public void onFinishedEventTrackingFailed(AdjustEventFailure adjustEventFailure) {
					dispatchEvent(L, LuaLoader.this.eventTrackingFailureListener, EVENT_EVENT_TRACKING_FAILURE, new JSONObject(LuaUtil.eventFailureToMap(adjustEventFailure)).toString());
				}
			});
		}

		// Session tracking succeeded callback
		if (this.sessionTrackingSuccessListener != CoronaLua.REFNIL) {
			adjustConfig.setOnSessionTrackingSucceededListener(new OnSessionTrackingSucceededListener() {
				@Override
				public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess adjustSessionSuccess) {
					dispatchEvent(L, LuaLoader.this.sessionTrackingSuccessListener, EVENT_SESSION_TRACKING_SUCCESS, new JSONObject(LuaUtil.sessionSuccessToMap(adjustSessionSuccess)).toString());
				}
			});
		}

		// Session tracking failed callback
		if (this.sessionTrackingFailureListener != CoronaLua.REFNIL) {
			adjustConfig.setOnSessionTrackingFailedListener(new OnSessionTrackingFailedListener() {
				@Override
				public void onFinishedSessionTrackingFailed(AdjustSessionFailure adjustSessionFailure) {
					dispatchEvent(L, LuaLoader.this.sessionTrackingFailureListener, EVENT_SESSION_TRACKING_FAILURE, new JSONObject(LuaUtil.sessionFailureToMap(adjustSessionFailure)).toString());
				}
			});
		}

		// Deferred deeplink callback listener
		if (this.deferredDeeplinkListener != CoronaLua.REFNIL) {
			adjustConfig.setOnDeeplinkResponseListener(new OnDeeplinkResponseListener() {
				@Override
				public boolean launchReceivedDeeplink(Uri uri) {
					dispatchEvent(L, LuaLoader.this.deferredDeeplinkListener, EVENT_DEFERRED_DEEPLINK, new JSONObject(LuaUtil.deferredDeeplinkToMap(uri)).toString());
					return LuaLoader.this.shouldLaunchDeeplink;
				}
			});
		}

		Adjust.onCreate(adjustConfig);
		Adjust.onResume();
		didStartAdjustSdk = true;

		if (this.uri != null) {
			Adjust.appWillOpenUrl(uri);
			this.uri = null;
		}

		return 0;
	}

	/**
	 * Invokes Adjust.trackEvent()
	 * Takes a hash table as input. The hash table is loaded on a stack which needs to be popped for the
	 * next element to be used
	 */
	public int adjust_trackEvent(final LuaState L) {
		if (!L.isTable(1)) {
			Log.e(TAG, "adjust_trackEvent: adjust_trackEvent() must be supplied with a table");
			return 0;
		}

        double revenue = -1.0;

        String orderId = null;
        String currency = null;
		String eventToken = null;

		// Event token
		L.getField(1, "eventToken");
		eventToken = L.checkString(2);
		L.pop(1);

		final AdjustEvent event = new AdjustEvent(eventToken);

		// Revenue
		L.getField(1, "revenue");
		if (!L.isNil(2)) {
			revenue = L.checkNumber(2);
		}
		L.pop(1);

		//currency
		L.getField(1, "currency");
		if (!L.isNil(2)) {
			currency = L.checkString(2);
		}
		L.pop(1);

		// Set revenue and currency
		if (currency != null && revenue != -1.0) {
			event.setRevenue(revenue, currency);
		}

		// Order ID
		L.getField(1, "transactionId");
		if (!L.isNil(2)) {
            orderId = L.checkString(2);
			event.setOrderId(orderId);
		}
		L.pop(1);

		// Callback parameters
		L.getField(1, "callbackParameters");
		if (!L.isNil(2) && L.isTable(2)) {
			int length = L.length(2);

			for (int i = 1; i <= length; i++) {
				// Push the table to the stack
				L.rawGet(2, i);

				L.getField(3, "key");
				String key = L.checkString(4);
				L.pop(1);

				L.getField(3, "value");
				String value = L.checkString(4);
				L.pop(1);

				event.addCallbackParameter(key, value);

				// Pop the stack
				L.pop(1);
			}
		}
		L.pop(1);

		// Partner parameters
		L.getField(1, "partnerParameters");
		if (!L.isNil(2) && L.isTable(2)) {
			int length = L.length(2);

			for (int i = 1; i <= length; i++) {
				// Push the table to the stack
				L.rawGet(2, i);

				L.getField(3, "key");
				String key = L.checkString(4);
				L.pop(1);

				L.getField(3, "value");
				String value = L.checkString(4);
				L.pop(1);

				event.addPartnerParameter(key, value);

				// Pop the stack
				L.pop(1);
			}
		}
		L.pop(1);

		Adjust.trackEvent(event);
		return 0;
	}

	private int adjust_setEnabled(LuaState L) {
		boolean enabled = L.checkBoolean(1);
		Adjust.setEnabled(enabled);
		return 0;
	}

	private int adjust_isEnabled(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;
		int listener = CoronaLua.REFNIL;

		// Assign and dispatch event immediately
		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			listener = CoronaLua.newRef(L, listenerIndex);
			dispatchEvent(L, listener, EVENT_IS_ADJUST_ENABLED, Adjust.isEnabled() ? "true" : "false");
		}

		return 0;
	}

	private int adjust_setPushToken(LuaState L) {
		String pushToken = L.checkString(1);
		Adjust.setPushToken(pushToken);
		return 0;
	}

	private int adjust_appWillOpenUrl(LuaState L) {
		final Uri uri = Uri.parse(L.checkString(1));
		if (didStartAdjustSdk) {
			Adjust.appWillOpenUrl(uri);
			return 0;
		}

		this.uri = uri;
		return 0;
	}

	private int adjust_sendFirstPackage(LuaState L) {
		Adjust.sendFirstPackages();
		return 0;
	}

	private int adjust_addSessionCallbackParameter(LuaState L) {
		String key = L.checkString(1);
		String value = L.checkString(2);
		Adjust.addSessionCallbackParameter(key, value);
		return 0;
	}

	private int adjust_addSessionPartnerParameter(LuaState L) {
		String key = L.checkString(1);
		String value = L.checkString(2);
		Adjust.addSessionPartnerParameter(key, value);
		return 0;
	}

	private int adjust_removeSessionCallbackParameter(LuaState L) {
		String key = L.checkString(1);
		Adjust.removeSessionCallbackParameter(key);
		return 0;
	}

	private int adjust_removeSessionPartnerParameter(LuaState L) {
		String key = L.checkString(1);
		Adjust.removeSessionPartnerParameter(key);
		return 0;
	}

	private int adjust_resetSessionCallbackParameters(LuaState L) {
		Adjust.resetSessionCallbackParameters();
		return 0;
	}

	private int adjust_resetSessionPartnerParameters(LuaState L) {
		Adjust.resetSessionPartnerParameters();
		return 0;
	}

	private int adjust_getIdfa(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;
		int listener = CoronaLua.REFNIL;

		// Assign and dispatch event immediately
		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			listener = CoronaLua.newRef(L, listenerIndex);
			dispatchEvent(L, listener, EVENT_GET_IDFA, "");
		}

		return 0;
	}

	private int adjust_getGoogleAdId(final LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;
		int listener = CoronaLua.REFNIL;

		// Assign and dispatch event immediately
		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			listener = CoronaLua.newRef(L, listenerIndex);
			final int finalListener = listener;
			Adjust.getGoogleAdId(CoronaEnvironment.getCoronaActivity(), new OnDeviceIdsRead() {
				@Override
				public void onGoogleAdIdRead(String googleAdId) {
					dispatchEvent(L, finalListener, EVENT_GET_GOOGLE_AD_ID, googleAdId != null ? googleAdId : "");
				}
			});
		}

		return 0;
	}

	private int adjust_getAdid(final LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;
		int listener = CoronaLua.REFNIL;

		// Assign and dispatch event immediately
		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			listener = CoronaLua.newRef(L, listenerIndex);
			String adid = Adjust.getAdid();
			if (adid == null) {
				adid = "";
			}

			dispatchEvent(L, listener, EVENT_GET_ADID, adid);
		}

		return 0;
	}

	private int adjust_getAmazonAdId(final LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;
		int listener = CoronaLua.REFNIL;

		// Assign and dispatch event immediately
		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			listener = CoronaLua.newRef(L, listenerIndex);
			dispatchEvent(L, listener, EVENT_GET_AMAZON_AD_ID, "");
		}

		return 0;
	}

	private int adjust_getAttribution(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;
		int listener = CoronaLua.REFNIL;

		// Assign and dispatch event immediately
		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			listener = CoronaLua.newRef(L, listenerIndex);
			AdjustAttribution attribution = Adjust.getAttribution();
			dispatchEvent(L, listener, EVENT_GET_ATTRIBUTION, new JSONObject(LuaUtil.attributionToMap(attribution)).toString());
		}

		return 0;
	}

	private int adjust_setOfflineMode(LuaState L) {
		boolean offlineMode = L.checkBoolean(1);
		Adjust.setOfflineMode(offlineMode);
		return 0;
	}

	private int adjust_setReferrer(LuaState L) {
		String referrer = L.checkString(1);
		Adjust.setReferrer(referrer, CoronaEnvironment.getApplicationContext());
		return 0;
	}

	private int adjust_setAttributionListener(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;

		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			this.attributionChangedListener = CoronaLua.newRef(L, listenerIndex);
		}

		return 0;
	}

	private int adjust_setEventTrackingSuccessListener(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;

		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			this.eventTrackingSuccessListener = CoronaLua.newRef(L, listenerIndex);
		}

		return 0;
	}

	private int adjust_setEventTrackingFailureListener(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;

		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			this.eventTrackingFailureListener = CoronaLua.newRef(L, listenerIndex);
		}

		return 0;
	}

	private int adjust_setSessionTrackingSuccessListener(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;

		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			this.sessionTrackingSuccessListener = CoronaLua.newRef(L, listenerIndex);
		}

		return 0;
	}

	private int adjust_setSessionTrackingFailureListener(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;

		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			this.sessionTrackingFailureListener = CoronaLua.newRef(L, listenerIndex);
		}

		return 0;
	}

	private int adjust_setDeferredDeeplinkListener(LuaState L) {
		// Hardcoded listener index for ADJUST
		int listenerIndex = 1;

		if (CoronaLua.isListener(L, listenerIndex, "ADJUST")) {
			this.deferredDeeplinkListener = CoronaLua.newRef(L, listenerIndex);
		}

		return 0;
	}

	private class CreateWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "create";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_create(L);
		}
	}

	private class TrackEventWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "trackEvent";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_trackEvent(L);
		}
	}

	private class SetEnabledWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setEnabled";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setEnabled(L);
		}
	}

	private class IsEnabledWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "isEnabled";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_isEnabled(L);
		}
	}

	private class SetReferrerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setReferrer";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setReferrer(L);
		}
	}

	private class SetOfflineModeWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setOfflineMode";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setOfflineMode(L);
		}
	}

	private class SetPushTokenWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setPushToken";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setPushToken(L);
		}
	}

	private class AppWillOpenUrlWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "appWillOpenUrl";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_appWillOpenUrl(L);
		}
	}

	private class SendFirstPackageWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "sendFirstPackage";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_sendFirstPackage(L);
		}
	}

	private class AddSessionCallbackParameterWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "addSessionCallbackParameter";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_addSessionCallbackParameter(L);
		}
	}

	private class AddSessionPartnerParameterWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "addSessionPartnerParameter";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_addSessionPartnerParameter(L);
		}
	}

	private class RemoveSessionCallbackParameterWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "removeSessionCallbackParameter";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_removeSessionCallbackParameter(L);
		}
	}

	private class RemoveSessionPartnerParameterWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "removeSessionPartnerParameter";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_removeSessionPartnerParameter(L);
		}
	}

	private class ResetSessionCallbackParametersWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "resetSessionCallbackParameters";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_resetSessionCallbackParameters(L);
		}
	}

	private class ResetSessionPartnerParametersWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "resetSessionPartnerParameters";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_resetSessionPartnerParameters(L);
		}
	}

	private class GetIdfaWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "getIdfa";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_getIdfa(L);
		}
	}

	private class GetAdidWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "getAdid";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_getAdid(L);
		}
	}

	private class GetGoogleAdIdWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "getGoogleAdId";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_getGoogleAdId(L);
		}
	}

	private class GetAmazonAdIdWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "getAmazonAdId";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_getAmazonAdId(L);
		}
	}

	private class GetAttributionWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "getAttribution";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_getAttribution(L);
		}
	}

	private class SetAttributionListenerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setAttributionListener";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setAttributionListener(L);
		}
	}

	private class SetEventTrackingSuccessListenerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setEventTrackingSuccessListener";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setEventTrackingSuccessListener(L);
		}
	}

	private class SetEventTrackingFailureListenerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setEventTrackingFailureListener";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setEventTrackingFailureListener(L);
		}
	}

	private class SetSessionTrackingSuccessListenerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setSessionTrackingSuccessListener";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setSessionTrackingSuccessListener(L);
		}
	}

	private class SetSessionTrackingFailureListenerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setSessionTrackingFailureListener";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setSessionTrackingFailureListener(L);
		}
	}

	private class SetDeferredDeeplinkListenerWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "setDeferredDeeplinkListener";
		}

		@Override
		public int invoke(LuaState L) {
			return adjust_setDeferredDeeplinkListener(L);
		}
	}
}

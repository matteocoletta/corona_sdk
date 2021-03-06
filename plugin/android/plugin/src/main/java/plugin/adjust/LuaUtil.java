//
//  LuaUtil.java
//  Adjust
//
//  Created by Abdullah Obaied on 2017-9-14.
//  Copyright (c) 2017 adjust GmbH. All rights reserved.
//  See the file MIT-LICENSE for copying permission.
//

package plugin.adjust;

import android.net.Uri;

import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.AdjustSessionFailure;
import com.adjust.sdk.AdjustSessionSuccess;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ab on 14.09.17.
 */

final public class LuaUtil {
    private static final String ATTRIBUTION_TRACKER_TOKEN = "trackerToken";
    private static final String ATTRIBUTION_TRACKER_NAME = "trackerName";
    private static final String ATTRIBUTION_NETWORK = "network";
    private static final String ATTRIBUTION_CAMPAIGN = "campaign";
    private static final String ATTRIBUTION_ADGROUP = "adgroup";
    private static final String ATTRIBUTION_CREATIVE = "creative";
    private static final String ATTRIBUTION_CLICK_LABEL = "clickLabel";
    private static final String ATTRIBUTION_ADID = "adid";

    private static final String EVENT_SUCCESS_MESSAGE = "message";
    private static final String EVENT_SUCCESS_TIMESTAMP = "timestamp";
    private static final String EVENT_SUCCESS_ADID = "adid";
    private static final String EVENT_SUCCESS_EVENT_TOKEN = "eventToken";
    private static final String EVENT_SUCCESS_JSON_RESPONSE = "jsonResponse";

    private static final String EVENT_FAILED_MESSAGE = "message";
    private static final String EVENT_FAILED_TIMESTAMP = "timestamp";
    private static final String EVENT_FAILED_ADID = "adid";
    private static final String EVENT_FAILED_EVENT_TOKEN = "eventToken";
    private static final String EVENT_FAILED_WILL_RETRY = "willRetry";
    private static final String EVENT_FAILED_JSON_RESPONSE = "jsonResponse";

    private static final String SESSION_SUCCESS_MESSAGE = "message";
    private static final String SESSION_SUCCESS_TIMESTAMP = "timestamp";
    private static final String SESSION_SUCCESS_ADID = "adid";
    private static final String SESSION_SUCCESS_JSON_RESPONSE = "jsonResponse";

    private static final String SESSION_FAILED_MESSAGE = "message";
    private static final String SESSION_FAILED_TIMESTAMP = "timestamp";
    private static final String SESSION_FAILED_ADID = "adid";
    private static final String SESSION_FAILED_WILL_RETRY = "willRetry";
    private static final String SESSION_FAILED_JSON_RESPONSE = "jsonResponse";

    public static Map attributionToMap(AdjustAttribution attribution) {
        Map map = new HashMap();

        if (null == attribution) {
            return map;
        }

        map.put(ATTRIBUTION_TRACKER_TOKEN, null != attribution.trackerToken ? attribution.trackerToken : "");
        map.put(ATTRIBUTION_TRACKER_NAME, null != attribution.trackerName ? attribution.trackerName : "");
        map.put(ATTRIBUTION_NETWORK, null != attribution.network ? attribution.network : "");
        map.put(ATTRIBUTION_CAMPAIGN, null != attribution.campaign ? attribution.campaign : "");
        map.put(ATTRIBUTION_ADGROUP, null != attribution.adgroup ? attribution.adgroup : "");
        map.put(ATTRIBUTION_CREATIVE, null != attribution.creative ? attribution.creative : "");
        map.put(ATTRIBUTION_CLICK_LABEL, null != attribution.clickLabel ? attribution.clickLabel : "");
        map.put(ATTRIBUTION_ADID, null != attribution.adid ? attribution.adid : "");

        return map;
    }

    public static Map eventSuccessToMap(AdjustEventSuccess eventSuccess) {
        Map map = new HashMap<String, String>();

        if (null == eventSuccess) {
            return map;
        }

        map.put(EVENT_SUCCESS_MESSAGE, null != eventSuccess.message ? eventSuccess.message : "");
        map.put(EVENT_SUCCESS_TIMESTAMP, null != eventSuccess.timestamp ? eventSuccess.timestamp : "");
        map.put(EVENT_SUCCESS_ADID, null != eventSuccess.adid ? eventSuccess.adid : "");
        map.put(EVENT_SUCCESS_EVENT_TOKEN, null != eventSuccess.eventToken ? eventSuccess.eventToken : "");
        map.put(EVENT_SUCCESS_JSON_RESPONSE, null != eventSuccess.jsonResponse ? eventSuccess.jsonResponse.toString() : "");

        return map;
    }

    public static Map eventFailureToMap(AdjustEventFailure eventFailure) {
        Map map = new HashMap();

        if (null == eventFailure) {
            return map;
        }

        map.put(EVENT_FAILED_MESSAGE, null != eventFailure.message ? eventFailure.message : "");
        map.put(EVENT_FAILED_TIMESTAMP, null != eventFailure.timestamp ? eventFailure.timestamp : "");
        map.put(EVENT_FAILED_ADID, null != eventFailure.adid ? eventFailure.adid : "");
        map.put(EVENT_FAILED_EVENT_TOKEN, null != eventFailure.eventToken ? eventFailure.eventToken : "");
        map.put(EVENT_FAILED_WILL_RETRY, eventFailure.willRetry ? "true" : "false");
        map.put(EVENT_FAILED_JSON_RESPONSE, null != eventFailure.jsonResponse ? eventFailure.jsonResponse.toString() : "");

        return map;
    }

    public static Map sessionSuccessToMap(AdjustSessionSuccess sessionSuccess) {
        Map map = new HashMap();

        if (null == sessionSuccess) {
            return map;
        }

        map.put(SESSION_SUCCESS_MESSAGE, null != sessionSuccess.message ? sessionSuccess.message : "");
        map.put(SESSION_SUCCESS_TIMESTAMP, null != sessionSuccess.timestamp ? sessionSuccess.timestamp : "");
        map.put(SESSION_SUCCESS_ADID, null != sessionSuccess.adid ? sessionSuccess.adid : "");
        map.put(SESSION_SUCCESS_JSON_RESPONSE, null != sessionSuccess.jsonResponse ? sessionSuccess.jsonResponse.toString() : "");

        return map;
    }

    public static Map sessionFailureToMap(AdjustSessionFailure sessionFailure) {
        Map map = new HashMap();

        if (null == sessionFailure) {
            return map;
        }

        map.put(SESSION_FAILED_MESSAGE, null != sessionFailure.message ? sessionFailure.message : "");
        map.put(SESSION_FAILED_TIMESTAMP, null != sessionFailure.timestamp ? sessionFailure.timestamp : "");
        map.put(SESSION_FAILED_ADID, null != sessionFailure.adid ? sessionFailure.adid : "");
        map.put(SESSION_FAILED_WILL_RETRY, sessionFailure.willRetry ? "true" : "false");
        map.put(SESSION_FAILED_JSON_RESPONSE, null != sessionFailure.jsonResponse ? sessionFailure.jsonResponse.toString() : "");

        return map;
    }

    public static Map deferredDeeplinkToMap(Uri uri) {
        Map map = new HashMap();

        if (null == uri) {
            return map;
        }

        map.put("uri", uri.toString());

        return map;
    }
}


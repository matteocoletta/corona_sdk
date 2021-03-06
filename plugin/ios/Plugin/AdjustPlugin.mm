//
//  PluginLibrary.mm
//  Adjust SDK
//
//  Copyright (c) 2017 Adjust GmbH. All rights reserved.
//

#import <UIKit/UIKit.h>
#include <CoronaRuntime.h>
#include "CoronaLuaIOS.h"

#import "Adjust.h"
#import "AdjustPlugin.h"
#import "AdjustSdkDelegate.h"

#define EVENT_IS_ENABLED @"adjust_isEnabled"
#define EVENT_GET_IDFA @"adjust_getIdfa"
#define EVENT_GET_ATTRIBUTION @"adjust_getAttribution"
#define EVENT_GET_ADID @"adjust_getAdid"
#define EVENT_GET_GOOGLE_AD_ID @"adjust_getGoogleAdId"
#define EVENT_GET_AMAZON_AD_ID @"adjust_getAmazonAdId"

// ----------------------------------------------------------------------------

class AdjustPlugin
{
public:
    typedef AdjustPlugin Self;

public:
    static const char kName[];
    static const char kEvent[];

protected:
    AdjustPlugin();

public:
    bool InitializeAttributionListener( CoronaLuaRef listener );
    bool InitializeEventTrackingSuccessListener( CoronaLuaRef listener );
    bool InitializeEventTrackingFailureListener( CoronaLuaRef listener );
    bool InitializeSessionTrackingSuccessListener( CoronaLuaRef listener );
    bool InitializeSessionTrackingFailureListener( CoronaLuaRef listener );
    bool InitializeDeferredDeeplinkListener( CoronaLuaRef listener );

public:
    CoronaLuaRef GetAttributionChangedListener() const { return attributionChangedListener; }
    CoronaLuaRef GetEventTrackingSuccessListener() const { return eventTrackingSuccessListener; }
    CoronaLuaRef GetEventTrackingFailureListener() const { return eventTrackingFailureListener; }
    CoronaLuaRef GetSessionTrackingSuccessListener() const { return sessionTrackingSuccessListener; }
    CoronaLuaRef GetSessionTrackingFailureListener() const { return sessionTrackingFailureListener; }
    CoronaLuaRef GetDeferredDeeplinkListener() const { return deferredDeeplinkListener; }

public:
    static int Open( lua_State *L );

protected:
    static int Finalizer( lua_State *L );

public:
    static Self *ToLibrary( lua_State *L );

public:
    static int create( lua_State *L );
    static int trackEvent( lua_State *L );
    static int setEnabled( lua_State *L );
    static int setPushToken( lua_State *L );
    static int appWillOpenUrl( lua_State *L );
    static int sendFirstPackages( lua_State *L );
    static int addSessionCallbackParameter( lua_State *L );
    static int addSessionPartnerParameter( lua_State *L );
    static int removeSessionCallbackParameter( lua_State *L );
    static int removeSessionPartnerParameter( lua_State *L );
    static int resetSessionCallbackParameters( lua_State *L );
    static int resetSessionPartnerParameters( lua_State *L );
    static int setOfflineMode( lua_State *L );
    static int isEnabled( lua_State *L );
    static int getIdfa( lua_State *L );
    static int getAttribution( lua_State *L );
    static int getAdid( lua_State *L );
    static int getGoogleAdId( lua_State *L );
    static int getAmazonAdId( lua_State *L );

    static int setAttributionListener( lua_State *L );
    static int setEventTrackingSuccessListener( lua_State *L );
    static int setEventTrackingFailureListener( lua_State *L );
    static int setSessionTrackingSuccessListener( lua_State *L );
    static int setSessionTrackingFailureListener( lua_State *L );
    static int setDeferredDeeplinkListener( lua_State *L );

private:
    CoronaLuaRef attributionChangedListener;
    CoronaLuaRef eventTrackingSuccessListener;
    CoronaLuaRef eventTrackingFailureListener;
    CoronaLuaRef sessionTrackingSuccessListener;
    CoronaLuaRef sessionTrackingFailureListener;
    CoronaLuaRef deferredDeeplinkListener;
};

// ----------------------------------------------------------------------------

// This corresponds to the name of the library, e.g. [Lua] require "plugin.library"
// Adjust SDK is named "plugin.adjust"
const char AdjustPlugin::kName[] = "plugin.adjust";

AdjustPlugin::AdjustPlugin()
: attributionChangedListener( NULL ),
eventTrackingSuccessListener( NULL ),
eventTrackingFailureListener( NULL ),
sessionTrackingSuccessListener( NULL ),
sessionTrackingFailureListener( NULL ),
deferredDeeplinkListener( NULL )
{
}

bool
AdjustPlugin::InitializeAttributionListener( CoronaLuaRef listener )
{
    // Can only initialize listener once
    bool result = ( NULL == attributionChangedListener );

    if ( result )
    {
        attributionChangedListener = listener;
    }

    return result;
}

bool
AdjustPlugin::InitializeEventTrackingSuccessListener( CoronaLuaRef listener )
{
    // Can only initialize listener once
    bool result = ( NULL == eventTrackingSuccessListener );

    if ( result )
    {
        eventTrackingSuccessListener = listener;
    }

    return result;
}

bool
AdjustPlugin::InitializeEventTrackingFailureListener( CoronaLuaRef listener )
{
    // Can only initialize listener once
    bool result = ( NULL == eventTrackingFailureListener );

    if ( result )
    {
        eventTrackingFailureListener = listener;
    }

    return result;
}

bool
AdjustPlugin::InitializeSessionTrackingSuccessListener( CoronaLuaRef listener )
{
    // Can only initialize listener once
    bool result = ( NULL == sessionTrackingSuccessListener );

    if ( result )
    {
        sessionTrackingSuccessListener = listener;
    }

    return result;
}

bool
AdjustPlugin::InitializeSessionTrackingFailureListener( CoronaLuaRef listener )
{
    // Can only initialize listener once
    bool result = ( NULL == sessionTrackingFailureListener );

    if ( result )
    {
        sessionTrackingFailureListener = listener;
    }

    return result;
}

bool
AdjustPlugin::InitializeDeferredDeeplinkListener( CoronaLuaRef listener )
{
    // Can only initialize listener once
    bool result = ( NULL == deferredDeeplinkListener );

    if ( result )
    {
        deferredDeeplinkListener = listener;
    }

    return result;
}

int
AdjustPlugin::Open( lua_State *L )
{
    // Register __gc callback
    const char kMetatableName[] = __FILE__; // Globally unique string to prevent collision
    CoronaLuaInitializeGCMetatable( L, kMetatableName, Finalizer );

    // Functions in library
    const luaL_Reg kVTable[] =
    {
        { "create", create },
        { "trackEvent", trackEvent },
        { "setEnabled", setEnabled },
        { "setPushToken", setPushToken },
        { "appWillOpenUrl", appWillOpenUrl },
        { "sendFirstPackages", sendFirstPackages },
        { "addSessionCallbackParameter", addSessionCallbackParameter },
        { "addSessionPartnerParameter", addSessionPartnerParameter },
        { "removeSessionCallbackParameter", removeSessionCallbackParameter },
        { "removeSessionPartnerParameter", removeSessionPartnerParameter },
        { "resetSessionCallbackParameters", resetSessionCallbackParameters },
        { "resetSessionPartnerParameters", resetSessionPartnerParameters },
        { "setOfflineMode", setOfflineMode },
        { "setAttributionListener", setAttributionListener },
        { "setEventTrackingSuccessListener", setEventTrackingSuccessListener },
        { "setEventTrackingFailureListener", setEventTrackingFailureListener },
        { "setSessionTrackingSuccessListener", setSessionTrackingSuccessListener },
        { "setSessionTrackingFailureListener", setSessionTrackingFailureListener },
        { "setDeferredDeeplinkListener", setDeferredDeeplinkListener },
        { "isEnabled", isEnabled },
        { "getIdfa", getIdfa },
        { "getAttribution", getAttribution },
        { "getAdid", getAdid },
        { "getGoogleAdId", getGoogleAdId },
        { "getAmazonAdId", getAmazonAdId },

        { NULL, NULL }
    };

    // Set library as upvalue for each library function
    Self *library = new Self;
    CoronaLuaPushUserdata( L, library, kMetatableName );

    luaL_openlib( L, kName, kVTable, 1 ); // leave "library" on top of stack

    return 1;
}

int
AdjustPlugin::Finalizer( lua_State *L )
{
    Self *library = (Self *)CoronaLuaToUserdata( L, 1 );

    CoronaLuaDeleteRef( L, library->GetAttributionChangedListener() );
    CoronaLuaDeleteRef( L, library->GetSessionTrackingSuccessListener() );
    CoronaLuaDeleteRef( L, library->GetSessionTrackingFailureListener() );
    CoronaLuaDeleteRef( L, library->GetEventTrackingSuccessListener() );
    CoronaLuaDeleteRef( L, library->GetEventTrackingFailureListener() );
    CoronaLuaDeleteRef( L, library->GetDeferredDeeplinkListener() );

    delete library;

    return 0;
}

AdjustPlugin *
AdjustPlugin::ToLibrary( lua_State *L )
{
    // library is pushed as part of the closure
    Self *library = (Self *)CoronaLuaToUserdata( L, lua_upvalueindex( 1 ) );
    return library;
}

int
AdjustPlugin::create( lua_State *L )
{
    double delayStart = 0.0;

    NSUInteger secretId = -1;
    NSUInteger info1 = -1;
    NSUInteger info2 = -1;
    NSUInteger info3 = -1;
    NSUInteger info4 = -1;

    BOOL isDeviceKnown = NO;
    BOOL sendInBackground = NO;
    BOOL eventBufferingEnabled = NO;
    BOOL shouldLaunchDeferredDeeplink = YES;

    NSString *appToken = nil;
    NSString *userAgent = nil;
    NSString *environment = nil;
    NSString *defaultTracker = nil;

    ADJLogLevel logLevel = ADJLogLevelInfo;

    if (!lua_istable(L, 1)) {
        return 0;
    }
    // Log level
    lua_getfield(L, 1, "logLevel");
    if (!lua_isnil(L, 2)) {
        const char *logLevel_char = lua_tostring(L, 2);
        logLevel = [ADJLogger logLevelFromString:[[NSString stringWithUTF8String:logLevel_char] lowercaseString]];
    }
    lua_pop(L, 1);

    // App token
    lua_getfield(L, 1, "appToken");
    if (!lua_isnil(L, 2)) {
        const char *appToken_char = lua_tostring(L, 2);
        appToken = [NSString stringWithUTF8String:appToken_char];
    }
    lua_pop(L, 1);

    // Environment
    lua_getfield(L, 1, "environment");
    if (!lua_isnil(L, 2)) {
        const char *environment_char = lua_tostring(L, 2);
        environment = [NSString stringWithUTF8String:environment_char];

        if ([[environment lowercaseString] isEqualToString:@"sandbox"]) {
            environment = ADJEnvironmentSandbox;
        } else if ([[environment lowercaseString] isEqualToString:@"production"]) {
            environment = ADJEnvironmentProduction;
        }
    }
    lua_pop(L, 1);

    ADJConfig *adjustConfig = [ADJConfig configWithAppToken:appToken
                                                environment:environment
                                      allowSuppressLogLevel:(logLevel == ADJLogLevelSuppress)];

    // Log level
    [adjustConfig setLogLevel:logLevel];

    // Event Buffering
    lua_getfield(L, 1, "eventBufferingEnabled");
    if (!lua_isnil(L, 2)) {
        eventBufferingEnabled = lua_toboolean(L, 2);
        [adjustConfig setEventBufferingEnabled:eventBufferingEnabled];
    }
    lua_pop(L, 1);

    // Sdk prefix - hardcoded
    [adjustConfig setSdkPrefix:@"corona4.12.2"];

    // Default tracker
    lua_getfield(L, 1, "defaultTracker");
    if (!lua_isnil(L, 2)) {
        const char *defaultTracker_char = lua_tostring(L, 2);
        defaultTracker = [NSString stringWithUTF8String:defaultTracker_char];
        [adjustConfig setDefaultTracker:defaultTracker];
    }
    lua_pop(L, 1);

    // User agent
    lua_getfield(L, 1, "userAgent");
    if (!lua_isnil(L, 2)) {
        const char *userAgent_char = lua_tostring(L, 2);
        userAgent = [NSString stringWithUTF8String:userAgent_char];
        [adjustConfig setUserAgent:userAgent];
    }
    lua_pop(L, 1);

    // Send in background
    lua_getfield(L, 1, "sendInBackground");
    if (!lua_isnil(L, 2)) {
        sendInBackground = lua_toboolean(L, 2);
        [adjustConfig setSendInBackground:sendInBackground];
    }
    lua_pop(L, 1);

    // Launching deferred deep link
    lua_getfield(L, 1, "shouldLaunchDeeplink");
    if (!lua_isnil(L, 2)) {
        shouldLaunchDeferredDeeplink = lua_toboolean(L, 2);
    }
    lua_pop(L, 1);

    // Delay start
    lua_getfield(L, 1, "delayStart");
    if (!lua_isnil(L, 2)) {
        delayStart = lua_tonumber(L, 2);
        [adjustConfig setDelayStart:delayStart];
    }
    lua_pop(L, 1);

    // Device known
    lua_getfield(L, 1, "isDeviceKnown");
    if (!lua_isnil(L, 2)) {
        isDeviceKnown = lua_toboolean(L, 2);
        [adjustConfig setIsDeviceKnown:isDeviceKnown];
    }
    lua_pop(L, 1);

    // App secret
    lua_getfield(L, 1, "secretId");
    if (!lua_isnil(L, 2)) {
        secretId = lua_tointeger(L, 2);
    }
    lua_pop(L, 1);
    lua_getfield(L, 1, "info1");
    if (!lua_isnil(L, 2)) {
        info1 = lua_tointeger(L, 2);
    }
    lua_pop(L, 1);
    lua_getfield(L, 1, "info2");
    if (!lua_isnil(L, 2)) {
        info2 = lua_tointeger(L, 2);
    }
    lua_pop(L, 1);
    lua_getfield(L, 1, "info3");
    if (!lua_isnil(L, 2)) {
        info3 = lua_tointeger(L, 2);
    }
    lua_pop(L, 1);
    lua_getfield(L, 1, "info4");
    if (!lua_isnil(L, 2)) {
        info4 = lua_tointeger(L, 2);
    }
    lua_pop(L, 1);

    if (secretId != -1 && info1 != -1 && info2 != -1 && info3 != -1 && info4 != -1) {
        [adjustConfig setAppSecret:secretId info1:info1 info2:info2 info3:info3 info4:info4];
    }

    Self *library = ToLibrary( L );
    BOOL isAttributionChangedListenerImplmented = library->GetAttributionChangedListener() != NULL;
    BOOL isEventTrackingSuccessListenerImplmented = library->GetEventTrackingSuccessListener() != NULL;
    BOOL isEventTrackingFailureListenerImplmented = library->GetEventTrackingFailureListener() != NULL;
    BOOL isSessionTrackingSuccessListenerImplmented = library->GetSessionTrackingSuccessListener() != NULL;
    BOOL isSessionTrackingFailureListenerImplmented = library->GetSessionTrackingFailureListener() != NULL;
    BOOL isDeferredDeeplinkListenerImplemented = library->GetDeferredDeeplinkListener() != NULL;

    if (isAttributionChangedListenerImplmented
        || isEventTrackingSuccessListenerImplmented
        || isEventTrackingFailureListenerImplmented
        || isSessionTrackingSuccessListenerImplmented ||
       isSessionTrackingFailureListenerImplmented ||
       isDeferredDeeplinkListenerImplemented) {
        [adjustConfig setDelegate:
         [AdjustSdkDelegate getInstanceWithSwizzleOfAttributionChangedCallback:library->GetAttributionChangedListener()
                                                  eventTrackingSuccessCallback:library->GetEventTrackingSuccessListener()
                                                  eventTrackingFailureCallback:library->GetEventTrackingFailureListener()
                                                sessionTrackingSuccessCallback:library->GetSessionTrackingSuccessListener()
                                                sessionTrackingFailureCallback:library->GetSessionTrackingFailureListener()
                                                      deferredDeeplinkCallback:library->GetDeferredDeeplinkListener()
                                                  shouldLaunchDeferredDeeplink:shouldLaunchDeferredDeeplink andLuaState:L]];
    }

    [Adjust appDidLaunch:adjustConfig];
    [Adjust trackSubsessionStart];

    return 0;
}

int
AdjustPlugin::trackEvent( lua_State *L )
{
    if (!lua_istable(L, 1)) {
        return 0;
    }

    double revenue = -1.0;

    NSString *currency = nil;
    NSString *eventToken = nil;
    NSString *transactionId = nil;

    // Event token
    lua_getfield(L, 1, "eventToken");
    if (!lua_isnil(L, 2)) {
        const char *eventToken_char = lua_tostring(L, 2);
        eventToken = [NSString stringWithUTF8String:eventToken_char];
    }
    lua_pop(L, 1);

    ADJEvent *event = [ADJEvent eventWithEventToken:eventToken];

    // Revenue
    lua_getfield(L, 1, "revenue");
    if (!lua_isnil(L, 2)) {
        revenue = lua_tonumber(L, 2);
    }
    lua_pop(L, 1);

    // Currency
    lua_getfield(L, 1, "currency");
    if (!lua_isnil(L, 2)) {
        const char *currency_char = lua_tostring(L, 2);
        currency = [NSString stringWithUTF8String:currency_char];
    }
    lua_pop(L, 1);

    if (currency != nil && revenue != -1.0) {
        [event setRevenue:revenue currency:currency];
    }

    // Transaction ID
    lua_getfield(L, 1, "transactionId");
    if(!lua_isnil(L, 2)) {
        const char *transactionId_char = lua_tostring(L, 2);
        transactionId = [NSString stringWithUTF8String:transactionId_char];
        [event setTransactionId:transactionId];
    }
    lua_pop(L, 1);

    // Callback parameters
    lua_getfield(L, 1, "callbackParameters");
    if (!lua_isnil(L, 2) && lua_istable(L, 2)) {
        NSDictionary *dict = CoronaLuaCreateDictionary(L, 2);
        for (id key in dict) {
            NSDictionary *callbackParams = [dict objectForKey:key];
            [event addCallbackParameter:callbackParams[@"key"] value:callbackParams[@"value"]];
        }
    }
    lua_pop(L, 1);

    // Partner Parameters
    lua_getfield(L, 1, "partnerParameters");
    if(!lua_isnil(L, 2) && lua_istable(L, 2)) {
        NSDictionary *dict = CoronaLuaCreateDictionary(L, 2);
        for(id key in dict) {
            NSDictionary *partnerParams = [dict objectForKey:key];
            [event addPartnerParameter:partnerParams[@"key"] value:partnerParams[@"value"]];
        }
    }
    lua_pop(L, 1);

    [Adjust trackEvent:event];

    return 0;
}

int
AdjustPlugin::setAttributionListener( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        Self *library = ToLibrary( L );

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->InitializeAttributionListener( listener );
    }

    return 0;
}

int
AdjustPlugin::setEventTrackingSuccessListener( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        Self *library = ToLibrary( L );

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->InitializeEventTrackingSuccessListener( listener );
    }

    return 0;
}

int
AdjustPlugin::setEventTrackingFailureListener( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        Self *library = ToLibrary( L );

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->InitializeEventTrackingFailureListener( listener );
    }

    return 0;
}

int
AdjustPlugin::setSessionTrackingSuccessListener( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        Self *library = ToLibrary( L );

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->InitializeSessionTrackingSuccessListener( listener );
    }

    return 0;
}

int
AdjustPlugin::setSessionTrackingFailureListener( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        Self *library = ToLibrary( L );

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->InitializeSessionTrackingFailureListener( listener );
    }

    return 0;
}

int
AdjustPlugin::setDeferredDeeplinkListener( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        Self *library = ToLibrary( L );

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->InitializeDeferredDeeplinkListener( listener );
    }

    return 0;
}

int
AdjustPlugin::setEnabled( lua_State *L )
{
    BOOL enabled = lua_toboolean(L, 1);
    [Adjust setEnabled:enabled];
    return 0;
}

int
AdjustPlugin::setPushToken( lua_State *L )
{
    const char *pushToken_char = lua_tostring(L, 1);
    NSString *pushToken =[NSString stringWithUTF8String:pushToken_char];
    [Adjust setDeviceToken:[pushToken dataUsingEncoding:NSUTF8StringEncoding]];
    return 0;
}

int
AdjustPlugin::appWillOpenUrl( lua_State *L )
{
    const char *urlStr = lua_tostring(L, 1);
    NSURL *url = [NSURL URLWithString:[NSString stringWithUTF8String:urlStr]];
    [Adjust appWillOpenUrl:url];
    return 0;
}

int
AdjustPlugin::sendFirstPackages( lua_State *L )
{
    [Adjust sendFirstPackages];
    return 0;
}

int
AdjustPlugin::addSessionCallbackParameter( lua_State *L )
{
    const char *key = lua_tostring(L, 1);
    const char *value = lua_tostring(L, 2);
    [Adjust addSessionCallbackParameter:[NSString stringWithUTF8String:key] value:[NSString stringWithUTF8String:value]];
    return 0;
}

int
AdjustPlugin::addSessionPartnerParameter( lua_State *L )
{
    const char *key = lua_tostring(L, 1);
    const char *value = lua_tostring(L, 2);
    [Adjust addSessionPartnerParameter:[NSString stringWithUTF8String:key] value:[NSString stringWithUTF8String:value]];
    return 0;
}

int
AdjustPlugin::removeSessionCallbackParameter( lua_State *L )
{
    const char *key = lua_tostring(L, 1);
    [Adjust removeSessionCallbackParameter:[NSString stringWithUTF8String:key]];
    return 0;
}

int
AdjustPlugin::removeSessionPartnerParameter( lua_State *L )
{
    const char *key = lua_tostring(L, 1);
    [Adjust removeSessionPartnerParameter:[NSString stringWithUTF8String:key]];
    return 0;
}

int
AdjustPlugin::resetSessionCallbackParameters( lua_State *L )
{
    [Adjust resetSessionCallbackParameters];
    return 0;
}

int
AdjustPlugin::resetSessionPartnerParameters( lua_State *L )
{
    [Adjust resetSessionPartnerParameters];
    return 0;
}

int
AdjustPlugin::setOfflineMode( lua_State *L )
{
    BOOL enabled = lua_toboolean(L, 1);
    [Adjust setOfflineMode:enabled];
    return 0;
}

int
AdjustPlugin::isEnabled( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {

        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );

        BOOL isEnabled = [Adjust isEnabled];
        NSString *result = isEnabled ? @"true" : @"false";
        [AdjustSdkDelegate dispatchEvent:L withListener:listener eventName:EVENT_IS_ENABLED andMessage:result];
    }

    return 0;
}

int
AdjustPlugin::getIdfa( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );

        NSString *idfa = [Adjust idfa];
        if (nil == idfa) {
            idfa = @"";
        }

        [AdjustSdkDelegate dispatchEvent:L withListener:listener eventName:EVENT_GET_IDFA andMessage:idfa];
    }

    return 0;
}

int
AdjustPlugin::getAttribution( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );

        ADJAttribution *attribution = [Adjust attribution];
        NSMutableDictionary *dictionary = [NSMutableDictionary dictionary];
        if (nil != attribution) {
            [AdjustSdkDelegate addKey:@"trackerToken" andValue:attribution.trackerToken toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"trackerName" andValue:attribution.trackerName toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"network" andValue:attribution.network toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"campaign" andValue:attribution.campaign toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"creative" andValue:attribution.creative toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"adgroup" andValue:attribution.adgroup toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"clickLabel" andValue:attribution.clickLabel toDictionary:dictionary];
            [AdjustSdkDelegate addKey:@"adid" andValue:attribution.adid toDictionary:dictionary];
        }

        NSError *error;
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dictionary
                                                           options:NSJSONWritingPrettyPrinted
                                                             error:&error];

        if (!jsonData) {
            NSLog(@"Error while trying to convert attribution dictionary to JSON string: %@", error);
        } else {
            NSString *jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
            [AdjustSdkDelegate dispatchEvent:L withListener:listener eventName:EVENT_GET_ATTRIBUTION andMessage:jsonString];
        }
    }

    return 0;
}

int
AdjustPlugin::getAdid( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );

        NSString *adid = [Adjust adid];
        if (nil == adid) {
            adid = @"";
        }

        [AdjustSdkDelegate dispatchEvent:L withListener:listener eventName:EVENT_GET_ADID andMessage:adid];
    }

    return 0;
}

int
AdjustPlugin::getGoogleAdId( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        NSString *googleAdId = @"";
        [AdjustSdkDelegate dispatchEvent:L withListener:listener eventName:EVENT_GET_GOOGLE_AD_ID andMessage:googleAdId];
    }

    return 0;
}

int
AdjustPlugin::getAmazonAdId( lua_State *L )
{
    int listenerIndex = 1;

    if ( CoronaLuaIsListener( L, listenerIndex, "ADJUST" ) )
    {
        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        NSString *amazonAdId = @"";
        [AdjustSdkDelegate dispatchEvent:L withListener:listener eventName:EVENT_GET_AMAZON_AD_ID andMessage:amazonAdId];
    }

    return 0;
}

// ----------------------------------------------------------------------------

CORONA_EXPORT int luaopen_plugin_adjust( lua_State *L )
{
    return AdjustPlugin::Open( L );
}

package com.fidesmo.mifaretutorial;

public class Constants {

    public static String applicationId = System.getenv().get("INVOKE_APPID");
    public static String applicationKeys = System.getenv().get("INVOKE_APPKEYS");
    public static String invokeServiceId = "invoke";
    public static String deleteServiceId = "delete";

    // Callback endpoints
    public static String rootUrl = System.getenv().get("INVOKE_ROOT_URL");
    public static String getCardCallbackUrl = "/gotcard";
    public static String initializeCallbackUrl = "/initialized";
    public static String readBlockCallbackUrl = "/readblock";
    public static String writeBlockCallbackUrl = "/writeblock";
    public static String deleteCardCallbackUrl = "/deletedcard";

    // Service Description parameters
    public static String invokeServiceTitle = "CTULHU invocation";
    public static String deleteServiceTitle = "Delete CTULHU invocation";
    public static Boolean requiresConfirmation = true;

    // MIFARE Classic parameters
    public static String keyA = "AAAAAAAAAAAA";
    public static String keyB = "BBBBBBBBBBBB";
    public static String accessBits = "F87F0000";

    // Fidesmo's backend URL
    public static String fidesmoBackendUrl = "https://api.fidesmo.com";
}

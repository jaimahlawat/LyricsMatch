package org.telegram.messenger;

public class BuildVars
{
  public static String APP_HASH;
  public static int APP_ID;
  public static String BING_SEARCH_KEY;
  public static int BUILD_VERSION;
  public static String BUILD_VERSION_STRING;
  public static boolean DEBUG_VERSION = "com.dragedy.fam.messenger".endsWith("debug");
  public static String FOURSQUARE_API_ID;
  public static String FOURSQUARE_API_KEY;
  public static String FOURSQUARE_API_VERSION;
  public static String GCM_SENDER_ID;
  public static String HOCKEY_APP_HASH;
  public static String HOCKEY_APP_HASH_DEBUG;
  public static String SEND_LOGS_EMAIL;

  static
  {
    BUILD_VERSION = 1;
    BUILD_VERSION_STRING = "1.0";
    APP_ID =82641;
    APP_HASH ="b01ad211d2669b1e22dd2962a5f9d3c8";
    HOCKEY_APP_HASH = "586d1ab6fe654eefba245cb0ba646348";
    HOCKEY_APP_HASH_DEBUG = "586d1ab6fe654eefba245cb0ba646348";
    GCM_SENDER_ID = "827773778159";
    SEND_LOGS_EMAIL = "hommiesdayout@gmail.com";
    BING_SEARCH_KEY = "5UJLxc/r6TsYl4pO+Ag64/hXVWG91QFm1DXcatji2EM";
    FOURSQUARE_API_KEY = "F4JBULWN4WJ1YVKZJODMVJRZBGPTRA31I5IIC2HJQQNU0VNG";
    FOURSQUARE_API_ID = "TKHG2AT5U53YXVTD4Z4AMSVQD3Z3YCOTJ4CUSILYVICSQTPE";
    FOURSQUARE_API_VERSION = "20150326";
  }
}

/* Location:           E:\Reverse Engineering centre\Fam\classes-dex2jar.jar
 * Qualified Name:     org.telegram.messenger.BuildVars
 * JD-Core Version:    0.6.0
 */
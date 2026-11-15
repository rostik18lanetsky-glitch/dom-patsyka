package android.content;
public class Intent { public static final String ACTION_BOOT_COMPLETED="b", ACTION_MY_PACKAGE_REPLACED="r"; public static final int FLAG_ACTIVITY_NEW_TASK=1, FLAG_ACTIVITY_CLEAR_TASK=2; String a;
  public Intent(Context c,Class<?> k){} public Intent setAction(String s){a=s;return this;} public String getAction(){return a;} public Intent addFlags(int f){return this;} }

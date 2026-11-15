package android.content;
import java.util.*;
public class Context {
  public static final int MODE_PRIVATE=0;
  static final Map<String,Object> STORE=new HashMap<>();
  public static Map<String,Object> store(){return STORE;}
  public SharedPreferences getSharedPreferences(String n,int m){ return new SharedPreferences(){
    public long getLong(String k,long d){Object v=STORE.get(k);return v==null?d:(Long)v;}
    public String getString(String k,String d){Object v=STORE.get(k);return v==null?d:(String)v;}
    public boolean getBoolean(String k,boolean d){Object v=STORE.get(k);return v==null?d:(Boolean)v;}
    public float getFloat(String k,float d){Object v=STORE.get(k);return v==null?d:(Float)v;}
    public int getInt(String k,int d){Object v=STORE.get(k);return v==null?d:(Integer)v;}
    public boolean contains(String k){return STORE.containsKey(k);}
    public Editor edit(){ return new Editor(){
      public Editor putLong(String k,long v){STORE.put(k,v);return this;} public Editor putString(String k,String v){if(v==null)STORE.remove(k);else STORE.put(k,v);return this;}
      public Editor putBoolean(String k,boolean v){STORE.put(k,v);return this;} public Editor putFloat(String k,float v){STORE.put(k,v);return this;} public Editor putInt(String k,int v){STORE.put(k,v);return this;} public void apply(){} };}
  };}
  public Context getApplicationContext(){return this;}
  public String getPackageName(){return "x";}
  public <T> T getSystemService(Class<T> c){return null;}
  public int checkSelfPermission(String p){return 0;}
  public java.util.concurrent.Executor getMainExecutor(){return null;}
}

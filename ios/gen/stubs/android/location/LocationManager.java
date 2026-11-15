package android.location; import java.util.*;
public class LocationManager { public static final String NETWORK_PROVIDER="network"; public List<String> getProviders(boolean e){return new ArrayList<>();} public Location getLastKnownLocation(String p){return null;}
 public boolean isProviderEnabled(String p){return false;} public void getCurrentLocation(String p,android.os.CancellationSignal s,java.util.concurrent.Executor e,java.util.function.Consumer<Location> c){} }

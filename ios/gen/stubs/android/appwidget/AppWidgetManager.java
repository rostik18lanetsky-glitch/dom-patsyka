package android.appwidget; import android.content.*; import android.widget.RemoteViews;
public class AppWidgetManager { public static AppWidgetManager getInstance(Context c){return new AppWidgetManager();} public int[] getAppWidgetIds(ComponentName n){return new int[0];} public void updateAppWidget(int[] ids,RemoteViews v){} }

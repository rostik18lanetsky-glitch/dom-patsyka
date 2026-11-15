package android.graphics;
public class Color {
  public static int parseColor(String s){ long v=Long.parseLong(s.substring(1),16); if(s.length()==7) v|=0xFF000000L; return (int)v; }
  public static int argb(int a,int r,int g,int b){ return (a<<24)|(r<<16)|(g<<8)|b; }
  public static int red(int c){return (c>>16)&255;} public static int green(int c){return (c>>8)&255;} public static int blue(int c){return c&255;}
}

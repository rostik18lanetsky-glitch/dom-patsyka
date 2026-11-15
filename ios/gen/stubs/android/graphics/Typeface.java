package android.graphics;
public class Typeface {
  public static final int NORMAL=0;
  public final java.awt.Font font;
  private Typeface(java.awt.Font f){font=f;}
  public static final Typeface DEFAULT_BOLD=new Typeface(new java.awt.Font("DejaVu Sans",java.awt.Font.BOLD,10));
  public static Typeface create(String n,int s){ return new Typeface(new java.awt.Font("DejaVu Sans",java.awt.Font.PLAIN,10)); }
}

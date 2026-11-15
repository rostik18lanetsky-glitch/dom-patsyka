package android.graphics;
public class Paint {
  public static final int ANTI_ALIAS_FLAG=1;
  public enum Style{FILL,STROKE} public enum Cap{BUTT,ROUND,SQUARE} public enum Join{MITER,ROUND,BEVEL}
  public static class FontMetrics { public float ascent; }
  private Style style=Style.FILL; private Cap cap=Cap.BUTT; private Join join=Join.MITER; private int color; private float sw=1, ts=12; private Typeface tf=Typeface.DEFAULT_BOLD;
  public Paint(int f){}
  public Style getStyle(){return style;} public void setStyle(Style s){style=s;}
  public Cap getStrokeCap(){return cap;} public void setStrokeCap(Cap c){cap=c;}
  public Join getStrokeJoin(){return join;} public void setStrokeJoin(Join j){join=j;}
  public int getColor(){return color;} public void setColor(int c){color=c;}
  public float getStrokeWidth(){return sw;} public void setStrokeWidth(float w){sw=w;}
  public float getTextSize(){return ts;} public void setTextSize(float t){ts=t;}
  public Typeface getTypeface(){return tf;} public Typeface setTypeface(Typeface t){tf=t;return t;}
  public FontMetrics getFontMetrics(){ FontMetrics m=new FontMetrics(); m.ascent=-ts*0.928f; return m; }
}

package android.graphics;
import java.awt.geom.*;
public class Path {
  public enum Direction { CW, CCW }
  public final Path2D.Float p=new Path2D.Float();
  public void moveTo(float x,float y){p.moveTo(x,y);} public void lineTo(float x,float y){p.lineTo(x,y);}
  public void quadTo(float a,float b,float c,float d){p.quadTo(a,b,c,d);}
  public void cubicTo(float a,float b,float c,float d,float e,float f){p.curveTo(a,b,c,d,e,f);}
  public void close(){p.closePath();}
  public void addRoundRect(RectF r,float rx,float ry,Direction d){p.append(new RoundRectangle2D.Float(r.left,r.top,r.right-r.left,r.bottom-r.top,rx*2,ry*2),false);}
}

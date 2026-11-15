package android.graphics;
import java.awt.*; import java.awt.geom.*;
public class Canvas {
  private final Graphics2D g;
  public Canvas(Bitmap b){ g=b.img.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON); g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON); g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);}
  public void scale(float a,float b){g.scale(a,b);}
  private final java.util.ArrayDeque<java.awt.geom.AffineTransform> st=new java.util.ArrayDeque<>();
  public int save(){st.push(g.getTransform());return st.size();}
  public void restore(){g.setTransform(st.pop());}
  public void rotate(float d,float px,float py){g.rotate(Math.toRadians(d),px,py);}
  public boolean clipPath(Path p){g.clip(p.p);return true;}
  private void paint(Shape s,Paint p){ g.setColor(new java.awt.Color(p.getColor(),true));
    if(p.getStyle()==Paint.Style.FILL) g.fill(s); else { g.setStroke(new BasicStroke(p.getStrokeWidth(), p.getStrokeCap()==Paint.Cap.ROUND?BasicStroke.CAP_ROUND:BasicStroke.CAP_BUTT, p.getStrokeJoin()==Paint.Join.ROUND?BasicStroke.JOIN_ROUND:BasicStroke.JOIN_MITER)); g.draw(s);} }
  public void drawPath(Path pa,Paint p){paint(pa.p,p);}
  public void drawCircle(float x,float y,float r,Paint p){paint(new Ellipse2D.Float(x-r,y-r,2*r,2*r),p);}
  public void drawOval(float l,float t,float r,float b,Paint p){paint(new Ellipse2D.Float(l,t,r-l,b-t),p);}
  public void drawRect(float l,float t,float r,float b,Paint p){paint(new Rectangle2D.Float(l,t,r-l,b-t),p);}
  public void drawLine(float a,float b,float c,float d,Paint p){paint(new Line2D.Float(a,b,c,d),p);}
  public void drawText(String s,float x,float y,Paint p){ g.setColor(new java.awt.Color(p.getColor(),true)); g.setFont(p.getTypeface().font.deriveFont(p.getTextSize())); g.drawString(s,x,y);}
}

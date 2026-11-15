package android.app; import android.content.*;
public class AlertDialog implements DialogInterface { public static class Builder { public Builder(Context c,int t){}
 public Builder setTitle(CharSequence t){return this;} public Builder setItems(CharSequence[] i,DialogInterface.OnClickListener l){return this;}
 public Builder setNegativeButton(CharSequence t,DialogInterface.OnClickListener l){return this;} public Builder setOnDismissListener(DialogInterface.OnDismissListener l){return this;} public AlertDialog show(){return new AlertDialog();} } }

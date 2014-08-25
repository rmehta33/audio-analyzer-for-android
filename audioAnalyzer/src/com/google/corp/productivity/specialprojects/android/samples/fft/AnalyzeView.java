/* Copyright 2011 Google Inc.
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 *
 * @author Stephen Uhler
 */

package com.google.corp.productivity.specialprojects.android.samples.fft;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view to draw the FFT graph
 */

public class AnalyzeView extends View {
  private float cursorX, cursorY; // cursor location
  private float scale;    // horizontal scaling
  private float xlate;    // horizontal translation
  private float mark;
  private RectF axisBounds;
  private Ready readyCallback = null;   // callback to caller when rendering is complete
  
  private int canvasWidth, canvasHeight;   // size of my canvas
  private Paint linePaint;
  private Paint cursorPaint;
  private Paint gridPaint;
  private Path path;
  private int[] myLocation = {0, 0}; // window location on screen
  private Matrix matrix = new Matrix();
  private static boolean isBusy = false;
  
  public boolean isBusy() {
	return isBusy;
  }
  
  public AnalyzeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setup(attrs);
  }
  
  public AnalyzeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setup(attrs);
  }
  
  public AnalyzeView(Context context) {
    super(context);
    setup(null);
  }
  
  public void setReady(Ready ready) {
    // Log.i(AnalyzeActivity.TAG, "Setting Ready");
    this.readyCallback = ready;
  }
  
  private void setup(AttributeSet attrs) {
    // Log.i(AnalyzeActivity.TAG, "Setup plot view");
    path = new Path();
    
    linePaint = new Paint();
    linePaint.setColor(Color.RED);
    linePaint.setStyle(Paint.Style.STROKE);
    linePaint.setStrokeWidth(0);
    
    cursorPaint = new Paint(linePaint);
    cursorPaint.setColor(Color.BLUE);
    
    gridPaint = new Paint(linePaint);
    gridPaint.setColor(Color.DKGRAY);
    
    cursorX = cursorY = 0f;
    scale=1f;
    xlate=0f;
    canvasWidth = canvasHeight = 0;
    axisBounds = new RectF(0.0f, 0.0f, 8000.0f, -90.0f);
  }
  
  public void setBounds(RectF bounds) {
    this.axisBounds = bounds;
  }
  
  public RectF getBounds() {
    return new RectF(axisBounds);
  }

  private double integerScale(double b, int nParts) {
	  if (b <= 0) {
		  return 0;
	  }
	  double ndigit = Math.floor(Math.log10(b));
	  double sdigit = Math.pow(10, Math.log10(b) - ndigit);  // 1 <= sdigit < 10
	  final double[] subLevel = {5, 4, 2.5, 2, 1};
	  int i = 0;
	  while (true) {
		  for (i = 0; i < subLevel.length; i++) {
			  if (sdigit / subLevel[i] < nParts) {
				  break;
			  }
		  }
		  if (i == subLevel.length) {  // still sdigit / subLevel[i] < nParts, so search for more
			  i = 0;
			  sdigit *= 10;
			  ndigit -= 1;
		  } else {
			  break;
		  }
	  }
	  return Math.pow(10, ndigit) * subLevel[i];
  }

  private double[] integerPartition(double a, double b, int nParts) {
	  double[] intPar = new double[nParts+1];
	  double intSca = integerScale(b-a, nParts);
	  
	  return intPar;
  }
  
  private double clamp(double value) {
    if (value < axisBounds.bottom || value != value) {
      value = axisBounds.bottom;
    } else if (value > axisBounds.top) {
      value = axisBounds.top;
    }
    return value;
  }
  
  /**
   * Recompute the line lengths for the first 'w' bins
   */
  public void recompute(double[] db, int start, int count, boolean bars) {
    if (canvasHeight < 1) {
      return;
    }
    isBusy = true;
    path.reset();
    if (bars) {
      for (int i = start; i < count; i++) {
        float x = (float) i * canvasWidth / count;
        float y = (float) (canvasHeight + canvasHeight * (clamp(db[i]) - axisBounds.bottom) / axisBounds.height());
        if (y != canvasHeight) {
          path.moveTo(x, canvasHeight);     
          path.lineTo(x, y);
        }
      }
    } else {
      // (0,0) is the upper left of the View, in pixel unit
      path.moveTo(0, (float) (canvasHeight + canvasHeight * (clamp(db[0]) - axisBounds.bottom) / axisBounds.height()));
      for (int i = start+1; i < count; i++) {
        float x = (float) i * canvasWidth / count;
        float y = (float) (canvasHeight + canvasHeight * (clamp(db[i]) - axisBounds.bottom) / axisBounds.height());
        path.lineTo(x, y);
      }
    }
    isBusy = false;
  }
  
  public boolean setCursor(float x, float y) {
    if (intersects(x, y)) {
      // Log.i(AnalyzeActivity.TAG, x + "," + y);
      float current = getXlate();
      if (x <= 3 && xlate > 0f) {
        setXlate(current - 10f) ;
      } else if (x >=  canvasWidth - 3) {
        setXlate(current + 10f);
      } else {
        cursorX = x + myLocation[0];
        cursorY = y - myLocation[1];
        cursorX = cursorX/scale + xlate; // ??
      }
      return true;
    } else {
      return false;
    }
  }
  
  // XXX this doesn't reset on size changes
  
  public void setMark(double hz) {
    float x = (float) (hz / axisBounds.width() * canvasWidth);
    mark = (x + myLocation[0]) / scale + xlate; 
    // Log.i(AnalyzeActivity.TAG, "mark=" + mark);
  }
  
  @Override
  public float getX() {
    // return bounds.width() * (xlate + cx) / (scale * w);
    return  canvasWidth == 0 ? 0 : axisBounds.width() * cursorX / canvasWidth;
  }
  
  /**
   * Translate a mouse event X coordinate into a graph coordinate.
   */
  public double xlateX(float x) {
    getLocationOnScreen(myLocation);
    double tmp =  (x + myLocation[0]) / scale + xlate;
    return canvasWidth == 0 ? 0.0 : axisBounds.width() * tmp / canvasWidth;
  }
  
  public float getY() {
    return  canvasHeight == 0 ? 0 : axisBounds.height() * cursorY / canvasHeight;
  }
  
  public double getMin() {
    double min =  canvasWidth == 0 ? 0 : axisBounds.width() * xlate * scale / (canvasWidth * scale);
    // Log.i(AnalyzeActivity.TAG, "min=" + min);
    return min;
  }
  
  public double getMax() {
    double max =  canvasWidth == 0 ? 0 : axisBounds.width() * (xlate * scale + canvasWidth) / (canvasWidth * scale);
    // Log.i(AnalyzeActivity.TAG, "max=" + max);
    return max;
  }
  
  private boolean intersects(float x, float y) {
    getLocationOnScreen(myLocation);
    return x >= myLocation[0] && y >= myLocation[1] &&
       x < myLocation[0] + getWidth() && y < myLocation[1] + getHeight();
  }
  
  public void setScale(float f) {
    scale = Math.max(f, 1f); 
    xlate = between(0f, xlate,  (scale - 1f) * canvasWidth );
    computeMatrix();
    invalidate();
  }
  
  public void setXlate(float offset) {
    xlate = between(0f, offset,  (scale * canvasWidth - canvasWidth) / scale);
    // Log.i(AnalyzeActivity.TAG, "xlate: " + xlate + " [" + offset + "]");
    computeMatrix();
    invalidate();
  }
  
  private void computeMatrix() {
    matrix.reset();
    matrix.setTranslate(-xlate, 0f);
    matrix.postScale(scale, 1f);
    // Log.i(AnalyzeActivity.TAG, "mat: " + xlate + "/" + scale);
  }
  
  public float getScale() {
    return scale;
  }
  
  public float getXlate() {
    return xlate;
  }
  
  public float convertX(float x) {
    float[] pts = {x};
    matrix.mapPoints(pts);
    return pts[0];
  }
  
  private float between(float min, float x, float max) {
     if (x > max) {
       return max;
     } else if (x < min) {
       return min; 
     } else {
       return x;
     }
  }

  @Override
  protected void onSizeChanged (int w, int h, int oldw, int oldh) {
    isBusy = true;
    this.canvasHeight = h;
    this.canvasWidth = w;
    // Log.i(AnalyzeActivity.TAG, "size: " + oldw + "," + oldh + " -> " + w + "," + h);
    if (oldh == 0 && h > 0 && readyCallback != null) {
      readyCallback.ready();
    }
    isBusy = false;
  }
  
  @Override
  protected void onDraw(Canvas c) {
    isBusy = true;
    c.concat(matrix);
    drawGridLines(c, 10, 10);
    c.drawPath(path, linePaint);
    if (cursorX > 0) {
      c.drawLine(cursorX, 0, cursorX, canvasHeight, cursorPaint); 
    }
    if (cursorY > 0) {
      c.drawLine(0, cursorY, canvasWidth, cursorY, cursorPaint); 
    }
    if (mark > 0f) {
      c.drawLine(mark - 3, 0, mark, 25, cursorPaint);
      c.drawLine(mark + 3, 0, mark, 25, cursorPaint);
    }
    isBusy = false;
  }
  
  /*
   * Save the labels, cursors, and bounds
   */
  
  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable parentState = super.onSaveInstanceState();
    State state = new State(parentState);
    state.cx = cursorX;
    state.cy = cursorY;
    state.scale = scale;
    state.xlate = xlate;
    state.bounds = axisBounds;
    return state;
  }
  
  @Override
  public void onRestoreInstanceState(Parcelable state) {
    if (state instanceof State) {
      State s = (State) state;
      super.onRestoreInstanceState(s.getSuperState());
      this.cursorX = s.cx;
      this.cursorY = s.cy;
      this.scale = s.scale;
      this.xlate = s.xlate;
      this.axisBounds = s.bounds;
      // Log.i(AnalyzeActivity.TAG, "Restore: " + cx + "," + cy + " " + scale + ":" + xlate + " (" + bounds + ")");
      computeMatrix();
      invalidate();
    } else {
      super.onRestoreInstanceState(state);
    }
  }
  
  public static interface Ready {
    public void ready();
  }
  
  public static class State extends BaseSavedState {
    float cx, cy; 
    float scale;
    float xlate;
    RectF bounds;
    
    State(Parcelable state) {
      super(state);
    }
    
    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeFloat(cx);
      out.writeFloat(cy);
      out.writeFloat(scale);
      out.writeFloat(xlate);
      bounds.writeToParcel(out, flags);
    }

    public static final Parcelable.Creator<State> CREATOR = new Parcelable.Creator<State>() {
      @Override
      public State createFromParcel(Parcel in) {
        return new State(in);
      }

      @Override
      public State[] newArray(int size) {
        return new State[size];
      }
    };
    
    private State(Parcel in) {
      super(in);
      cx = in.readFloat();
      cy = in.readFloat();
      scale = in.readFloat();
      xlate = in.readFloat();
      bounds = RectF.CREATOR.createFromParcel(in);
    }
  }

  private void drawGridLines(Canvas c, int nx, int ny) {
    for(int i=0; i<=nx; i++) {
      float pos = i * (canvasWidth-1) / nx;
      c.drawLine(pos, 0, pos, canvasHeight, gridPaint);
    }
    for(int i=0; i<=ny; i++) {
      float pos = i * (canvasHeight-1) / ny;
      c.drawLine(0, pos, canvasWidth, pos, gridPaint);
    }
  }
}

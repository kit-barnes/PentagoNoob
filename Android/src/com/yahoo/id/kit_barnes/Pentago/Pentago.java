package com.yahoo.id.kit_barnes.Pentago;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class Pentago extends Activity {
	
	Pentago pentago;
	Game game;
	PentagoLayout board;
	TextView banner;
	QuadView quadView[] = new QuadView[4];
	Overlay overlay;
	LinearLayout buttonbar;
	Dialog options_dialog;
	Dialog help_dialog;
	
	Paint redPaint;
	Paint borderPaint;
	Paint quadPaint;
	Paint dimplePaint;
	Paint whitePaint;
	Paint blackPaint;
    
	
	final static int xy2did[] = {0,1,2,7,8,3,6,5,4};	// translate dimple position

	private boolean dimplesEnabled;
	private boolean twistersEnabled;
	
	Thread thread;					// for animating puts and twists
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	pentago = this;
        setContentView(board = new PentagoLayout(this));
        game = new Game(pentago);
		game.start(getPreferences(MODE_PRIVATE));
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        game.saveState(getPreferences(MODE_PRIVATE));
    }
    
    static final int DIALOG_OPTIONS_ID = 0;
    static final int DIALOG_HELP_ID = 1;
    
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        Dialog dialog;
        Button button;
        switch(id) {
        case DIALOG_OPTIONS_ID:
        	options_dialog = dialog = new Dialog(this);
        	dialog.setContentView(R.layout.options_dialog);
        	dialog.setTitle(getResources().getString(R.string.options));
        	
            Spinner spinner = (Spinner) dialog.findViewById(R.id.white_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.players, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            PlayerSelectedListener listener = new PlayerSelectedListener();
            spinner.setOnItemSelectedListener(listener);
        	
            spinner = (Spinner) dialog.findViewById(R.id.black_spinner);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(listener);
        	
        	dialog.findViewById(R.id.layout_options).setPadding(10, 0, 10, 10);
        	button = (Button) dialog.findViewById(R.id.options_done_button);
        	button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					options_dialog.cancel();
				}
        	});
        	break;
        case DIALOG_HELP_ID:
        	help_dialog = dialog = new Dialog(this);
        	dialog.setContentView(R.layout.help_dialog);
        	dialog.setTitle(getResources().getString(R.string.help));
        	
        	dialog.findViewById(R.id.layout_help).setPadding(10, 0, 10, 10);
        	button = (Button) dialog.findViewById(R.id.help_done_button);
        	button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					help_dialog.cancel();
				}
        	});
        	break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
    	super.onPrepareDialog(id, dialog, args);
        switch(id) {
        case DIALOG_OPTIONS_ID:
            Spinner spinner = (Spinner) dialog.findViewById(R.id.white_spinner);
            spinner.setSelection(game.getWhitePlayer());
            spinner = (Spinner) dialog.findViewById(R.id.black_spinner);
            spinner.setSelection(game.getBlackPlayer());
        	break;
        case DIALOG_HELP_ID:
        default:
        }   	
    }
    
    public class PlayerSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
         	if (options_dialog.findViewById(R.id.white_spinner) == parent) game.setWhitePlayer(pos);
         	else if (options_dialog.findViewById(R.id.black_spinner) == parent) game.setBlackPlayer(pos);
            }
        public void onNothingSelected(AdapterView<?> parent) {
          // Do nothing.
        }
    }
    
    public void setGame(Game g) {
		game = g;
		if (thread!=null && thread.isAlive()) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}
		thread = null;
		dimplesEnabled = false;
		twistersEnabled = false;
	}
	
	public void setBanner(int s) {
		banner.setText(s);
		banner.invalidate();
		overlay.invalidate();
	}
	
	public void repaintBoard() {
		for (int qid = 0; qid < 4; qid++) {
			quadView[qid].setMarbles(game.getWhite(qid), game.getBlack(qid));
			quadView[qid].invalidate();
		}
	}
	
	void waitDimpleClick() {
		dimplesEnabled = true;
		repaintBoard();
	}
	
	void waitTwisterClick() {
		twistersEnabled = true;
		repaintBoard();
	}

	void animatePut(int qid, int oldw, int oldb, int w, int b) {
		thread = new Thread(new PutAnimationTask(qid, oldw, oldb, w, b));
		thread.start();
	}
	private class PutAnimationTask implements Runnable {
		int qid, oldw, oldb, w, b, count;
		public PutAnimationTask(int qid, int oldw, int oldb, int w, int b) {
			this.qid = qid;
			this.oldw = oldw;
			this.oldb = oldb;
			this.w = w;
			this.b = b;
			this.count = 10;
		}
		public void run() {
			try {
				while (count-- > 0) {
					if ((count&1)==0) quadView[qid].setMarbles(w, b);
					else quadView[qid].setMarbles(oldw, oldb);
					quadView[qid].post(new Runnable() {
						public void run() {
							quadView[qid].invalidate();
							}
						});
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				return;
			} finally {
				quadView[qid].setMarbles(w, b);
			}
			quadView[qid].post(new Runnable() {
				public void run() {
					game.animationDone();
					}
			});
		}
	}

	void animateTwist(int twister) {
		// twist has already been accomplished in white and black
		//    set angle to +/- 90 degrees and reduce to zero
		thread = new Thread(new TwistAnimationTask(twister));
		thread.start();
	}
	private class TwistAnimationTask implements Runnable {
		int qid;
		int count;
		int step;
		public TwistAnimationTask(int twister) {
			qid = twister>>1;
			count = 20;
			step = 90 / count;
			if ((twister&1)==0) step *= -1;
		}
		public void run() {
			try {
				quadView[qid].setMarbles(game.getWhite(qid), game.getBlack(qid));
				while (--count >= 0) {
					quadView[qid].twist = count * step;
					quadView[qid].post(new Runnable() {
						public void run() {
							quadView[qid].invalidate();
							}
						});
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
				return;
			} finally {
				quadView[qid].twist = 0;
			}
			quadView[qid].post(new Runnable() {
				public void run() {
					game.animationDone();
					}
			});
		}
	}
	
	final static int dx[] = {1,3,5,5,5,3,1,1,3};	// x position of dimples in quad.cubits
	final static int dy[] = {1,1,1,3,5,5,5,3,3};	// y position of dimples in quad.cubits

    public class PentagoLayout extends LinearLayout {
    	
        public PentagoLayout(Context c) {
            super(c);
            
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            
            whitePaint = new Paint();
            whitePaint.setAntiAlias(true);
            whitePaint.setStyle(Paint.Style.FILL);
            whitePaint.setColor(0xffe8e8e8);
            blackPaint = new Paint(whitePaint);
            blackPaint.setColor(0xff000000);
            borderPaint = new Paint(whitePaint);
            borderPaint.setColor(0xff93693D);
            quadPaint = new Paint(whitePaint);
            quadPaint.setColor(0xffd1a574);
            dimplePaint = new Paint(whitePaint);
            dimplePaint.setColor(0xffdfc094);
            redPaint = new Paint(whitePaint);
            redPaint.setColor(0xffff0000);
           
            setOrientation(VERTICAL);

            addView(banner = new TextView(pentago));
            banner.setPadding(8,8,8,8);
            addView(new FourQuadLayout(pentago));
            addView(buttonbar = new LinearLayout(pentago));
            buttonbar. setPadding(0,8,0,0);
            ViewGroup.LayoutParams lparams = new LinearLayout.LayoutParams(
            		ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1F);
            ImageButton ibutton;
            buttonbar.addView(ibutton = new ImageButton(pentago));
            ibutton.setLayoutParams(lparams);
            ibutton.setImageResource(R.drawable.menu_help);
            ibutton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showDialog(DIALOG_HELP_ID);
                }
            });
            buttonbar.addView(ibutton = new ImageButton(pentago));
            ibutton.setLayoutParams(lparams);
            ibutton.setImageResource(R.drawable.menu_settings);
            ibutton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showDialog(DIALOG_OPTIONS_ID);
                }
            });
            buttonbar.addView(ibutton = new ImageButton(pentago));
            ibutton.setLayoutParams(lparams);
            ibutton.setImageResource(R.drawable.menu_new);
            ibutton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                	game.start(null);
                }
            });
            buttonbar.addView(ibutton = new ImageButton(pentago));
            ibutton.setLayoutParams(lparams);
            ibutton.setImageResource(R.drawable.menu_undo);
            ibutton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    game.undoClicked();
                }
            });
        }

    }
    
    class FourQuadLayout extends FrameLayout {

        public FourQuadLayout(Context c) {
            super(c);
            
            for (int i = 0; i < 4; i++) {
            	addView(quadView[i]= new QuadView(pentago, i));
            }
            addView(overlay = new Overlay(pentago));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int wmax =  MeasureSpec.getSize(widthMeasureSpec);
            int hmax =  MeasureSpec.getSize(heightMeasureSpec);
        	// ensure largest possible square board
        	int size = wmax<hmax ? wmax : hmax;
        	setMeasuredDimension(size, size);
        }
        
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            layoutFourQuad(w, h);
        }
        
        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        	super.onLayout(changed, left, top, right, bottom);
        	layoutFourQuad(right-left, bottom-top);
        }
        
        protected void layoutFourQuad(int w, int h) {
        	// assert w == h
            int w2 = w/2;
            int b = (w&1)+1;
            quadView[0].layout(0, 0, w2-1, w2-1);
            quadView[1].layout(w2+b, 0, w, w2-1);
            quadView[2].layout(w2+b, w2+b, w, w);
            quadView[3].layout(0, w2+b, w2-1, w);
            overlay.layout(0,0,w,w);
       }
    }
    
    class QuadView extends View {
    	
    	int id;
    	int wht;			// white marbles (0-511)
    	int blk;			// black marbles (0-511)
    	int twist;			// twist angle in degrees clockwise
    	boolean dragging;	// processing twist input
    	int sx,sy;			// dragging start position
    	double theta;		// dragging angle
    	RectF rect;			// for drawing efficiency
    	        
        public QuadView(Context context, int qid) {
            super(context);
            id = qid;
            twist = wht = blk = 0;
            dragging = false;
            rect = new RectF();
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
        	canvas.save();
            float side = getWidth();
        	canvas.rotate(90*id + twist, side/2, side/2);
			double theta = (45 - (twist+360)%90) * Math.PI/180;
        	float scale = (float)( 1 / (Math.cos( theta )*Math.sqrt(2)) );
        	canvas.scale(scale, scale, side/2, side/2);
            canvas.drawColor(Color.BLACK);
    		float cubit = side/6;
    		float arc = cubit;
    		rect.set(0,0,side,side);
            canvas.drawRoundRect(rect, arc, arc, borderPaint);
    		float border = cubit/8;
    		if (border<1) border = 1;
    		rect.inset(border,border);
    		arc -= border;
    		if (arc < 0) arc = 0;
            canvas.drawRoundRect(rect, arc, arc, quadPaint);
            for (int d = 0; d < 9; d++) {
            	Paint p;
            	float r = 0.6f;
            	if ((wht & (1<<d))!=0) p = whitePaint;
            	else if ((blk & (1<<d))!=0) p = blackPaint;
            	else {
            		r = 0.5f;
            		p = dimplePaint;
            	}
            	canvas.drawCircle(dx[d]*cubit, dy[d]*cubit, r*cubit, p);
            }
            canvas.restore();
        }
        
        @Override
		public boolean onTouchEvent(MotionEvent event){
        	int x,y;
        	int action = event.getAction();
        	if (dimplesEnabled && action == MotionEvent.ACTION_DOWN) {
        		x = 3*(int)event.getX()/getWidth();
        		y = 3*(int)event.getY()/getWidth();
        		int did = xy2did[x+3*y];
        		if (did != 8) did = (did + 8 - 2*id) % 8;
        		dimplesEnabled = false;
        		game.dimpleClicked(id*9 + did);
        		return true;
        	}
        	if (twistersEnabled) {
        		if (action == MotionEvent.ACTION_UP) {
        			dragging = false;
        			return true;
        		}
        		else if (action == MotionEvent.ACTION_DOWN) {
        			sx = (int) (event.getX() - getWidth()/2);
        			if (sx == 0) sx = 1;
        			sy = (int) (event.getY() - getWidth()/2);
        			if (sy == 0) sy = 1;
        			theta = Math.atan2(sx, sy);
        			dragging = true;
        			return true;
        		}
        		else if (dragging & action == MotionEvent.ACTION_MOVE) {
        			x = (int)(event.getX() - getWidth()/2);
        			if (x == 0) x = 1;
        			y = (int)(event.getY() - getWidth()/2);
        			if (y == 0) y = 1;
        			int dx = x - sx;
        			int dy = y - sy;
        			if (dx*dx + dy*dy < getWidth()*getWidth()/144) {
        				// havn't dragged far enough yet
        				return true;
        			}
        			theta = Math.atan2(x, y) - theta;
        			if (theta>Math.PI||theta<-Math.PI) theta = -theta;
        			dragging = false;
        			twistersEnabled = false;
        			game.twisterClicked(id*2+(theta>0? 1: 0));
        			return true;
        		}	
        	}
			return false;
        }
        
        void setMarbles(int w, int b) {
        	wht = w;
        	blk = b;
        }
    }

    class Overlay extends View {
    	
    	private float cubit;
    	
    	public Overlay(Context c) {
    		super(c);
    	}
    	
        @Override
        protected void onDraw(Canvas canvas) {
        	
            cubit = getWidth()/12.0F;
            redPaint.setStrokeWidth(cubit/10);
        	Game.Winline w = game.wins;
        	while (w != null) {
	        	canvas.drawLine(d2p(w.col1), d2p(w.row1),
	        					d2p(w.col2), d2p(w.row2), redPaint);
	        	w = w.next;
        	}
        }
        
        private float d2p(int d) {
        	return (1+d+d)*cubit;
        }
     }
}

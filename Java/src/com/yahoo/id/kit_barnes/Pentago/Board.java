package com.yahoo.id.kit_barnes.Pentago;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

public class Board extends JComponent implements MouseInputListener, MouseMotionListener, ComponentListener {
	
	private static final long serialVersionUID = 8001750051628430957L;
	
	// public Dimension getMinimumSize() { return new Dimension(100,100); }	// don't see that this does anything.  Layout Manager?
	public Dimension getPreferredSize() { return new Dimension(680,680); }
	
	final static Color WHITE = new Color(0xFFFFFF);
	final static Color BLACK = new Color(0x000000);
	final static Color BG_COLOR = new Color(0x808080);	// background color
	final static Color HL_COLOR = new Color(0xdfc094);	// text color
	final static Color QB_COLOR = new Color(0xd1a574);	// quadrant background color
	final static Color SD_COLOR = new Color(0x9d7049);	// shadow color
	final static Color LN_COLOR = new Color(0x404040);	// line color
	private final static Color[] DP_COLORS = { QB_COLOR, SD_COLOR };	// dimple colors 
	private final static float[] DP_FRACT = {0.3f, 1.0f};	// radial gradient fractions
	private final static Color[] MW_COLORS = { WHITE, new Color(0xF0F0F0), new Color(0xD0D0D0) };	// white marble colors 
	private final static Color[] MB_COLORS = { new Color(0x909090), new Color(0x202020), new Color(0x101010) };	// black marble colors
	private final static float[] M_FRACT = { 0.1f, 0.2f, 1.0f };
	
	private final static Point ORIGIN = new Point(0,0);
	private final static int[] quadCenterX = { -6, 6, 6, -6 };		// offsets of quadrant centers
	private final static int[] quadCenterY = { -6, -6, 6, 6 };		//   from board center (in cubits)
	private final static int[] dimpleX = { -4, 0, 4, 4, 4, 0, -4, -4, 0 };	// offsets of dimple centers
	private final static int[] dimpleY = { -4, -4, -4, 0, 4, 4, 4, 0, 0 };	//  from quadrant center (in cubits)

	private int boardCenterX, boardCenterY;
	private float cubit;
	private RoundRectangle2D.Float outer;			// center@(0,0)
	private RoundRectangle2D.Float inner;			// define border
	private Ellipse2D dimple;
	private Ellipse2D marble;
	private RadialGradientPaint dimplePaint;
	private RadialGradientPaint whiteMarblePaint;
	private RadialGradientPaint blackMarblePaint;
	private GeneralPath twister;
	
	private Btn.History graph;
	private Btn.Text btnBack;
	private Btn.Text btnForward;
	private Btn.Text btnUnPause;

	private Board thisBoard;
	
	private boolean dimplesEnabled;
	private boolean twistersEnabled;
	
	private int highlighted;				// dimple or twister or -1
	private Thread thread;				// for animating puts and twists
	private int specialQuad;
	private int specialQuadBW;				// for puts
	private double specialQuadAngle;		// for twists
	
	private Game game;



	Board(Game game) {
		thisBoard = this;
		this.game = game;
		thread = null;
		outer = new RoundRectangle2D.Float();
		inner = new RoundRectangle2D.Float();
		dimple = new Ellipse2D.Float();
		marble = new Ellipse2D.Float();
		graph = new Btn.History(game);
		btnBack = new Btn.Text(game);
		btnForward = new Btn.Text(game);
		btnUnPause = new Btn.Text(game);
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
	}
	
	void setGame(Game game) {
		this.game = game;
		if (thread!=null && thread.isAlive()) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}
		thread = null;
		highlighted = -1;
		specialQuad = -1;
		specialQuadAngle = 0;
		dimplesEnabled = false;
		twistersEnabled = false;
	}
	
	void waitDimpleClick() {
		dimplesEnabled = true;
		repaint();
	}
			
	void waitTwisterClick() {
		twistersEnabled = true;
		repaint();
	}
			
	void animatePut(int quad, int old, int now) {
		thread = new Thread(new PutAnimationTask(old, now, quad));
		thread.start();
	}
	private class PutAnimationTask implements Runnable {
		int quad, old, now, count;
		public PutAnimationTask(int old, int now, int quad) {
			this.quad = quad;
			this.old = old;
			this.now = now;
			this.count = 10;
		}
		public void run() {
			try {
				specialQuad = quad;
				while (count-- > 0) {
					specialQuadBW = (count&1)==0? now: old;
					thisBoard.repaint();
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				return;
			}
			specialQuad = -1;
			game.animationDone();
		}
	}
	
	void animateTwist(int twister) {
		// twist has already been accomplished in white and black
		//    set angle to +/- 90 degrees and reduce to zero
		thread = new Thread(new TwistAnimationTask(twister));
		thread.start();
	}
	private class TwistAnimationTask implements Runnable {
		int twister;
		int count;
		double step;
		public TwistAnimationTask(int twister) {
			this.twister = twister;
			count = 20;
			step = (Math.PI/2) / count;
			if ((twister&1)==0) step *= -1;
		}
		public void run() {
			try {
				specialQuad = twister >> 1;
				while (--count > 0) {
					specialQuadAngle = count * step;
					// note - do not let angle go to zero here - 
					//   will cause last specialQuadValue to be flashed.
					thisBoard.repaint();
					Thread.sleep(50);
				}
			} catch (InterruptedException e) {
				return;
			}
			specialQuadAngle = 0;
			specialQuad = -1;
			repaint();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				return;
			}
			game.animationDone();
		}
	}

	private void setGeometry() {
		boardCenterX = getWidth()/2;
		boardCenterY = getHeight()/2;
		cubit = (float) ((boardCenterX<boardCenterY ? boardCenterX : boardCenterY) / 24*Math.sqrt(2));
		// set quadrant size
		float wh = 12*cubit;
		float xy = -6*cubit;
		float arc = 2*cubit;
		outer.setRoundRect(xy,xy,wh,wh,arc,arc);
		float border = cubit/5;
		if (border<1) border = 1;
		xy += border;
		wh -= border*2;
		arc -= border;
		if (arc < 0) arc = 0;
		inner.setRoundRect(xy,xy,wh,wh,arc,arc);
		// set dimple and marble sizes
		dimple.setFrameFromCenter(0, 0, 1.1*cubit, 1.1*cubit);
		marble.setFrameFromCenter(0, 0, 1.2*cubit, 1.2*cubit);
		// set dimple and marble gradients
		Point2D focus = new Point2D.Float( 0.15f*cubit, 0.15f*cubit );
		dimplePaint = new RadialGradientPaint(ORIGIN, cubit, focus, DP_FRACT, DP_COLORS, CycleMethod.NO_CYCLE);
		focus = new Point2D.Float( -0.4f*cubit, -0.4f*cubit );
		whiteMarblePaint = new RadialGradientPaint(ORIGIN, 1.1f*cubit, focus, M_FRACT, MW_COLORS, CycleMethod.NO_CYCLE);
		blackMarblePaint = new RadialGradientPaint(ORIGIN, 1.1f*cubit, focus, M_FRACT, MB_COLORS, CycleMethod.NO_CYCLE);
		// set twister size and shape
		twister = new GeneralPath();
		twister.moveTo(-1.8*cubit, -1.4*cubit);
		twister.lineTo(-2.7*cubit, -0.9*cubit);
		twister.lineTo(-2.5*cubit, -0.8*cubit);
		twister.quadTo(-3.2*cubit, 3.2*cubit, 0.9*cubit, 2.7*cubit);
		twister.lineTo(0*cubit, 2.2*cubit);
		twister.lineTo(0.5*cubit, 1.5*cubit);
		twister.quadTo(-2.0*cubit, 2.0*cubit, -1.7*cubit, -0.6*cubit);
		twister.lineTo(-1.5*cubit, -0.5*cubit);
		twister.closePath();
		graph.setGeometry(cubit, boardCenterX, Float.valueOf(boardCenterY+14*cubit).intValue());
		btnBack.setGeometry("\u25C4", cubit, Float.valueOf(boardCenterX-11*cubit).intValue(),
				Float.valueOf(boardCenterY+14*cubit).intValue());
		btnForward.setGeometry("\u25BA", cubit, Float.valueOf(boardCenterX+11*cubit).intValue(),
				Float.valueOf(boardCenterY+14*cubit).intValue());
		btnUnPause.setGeometry("CONTINUE", cubit, Float.valueOf(boardCenterX).intValue(),
				Float.valueOf(boardCenterY+16*cubit).intValue());
	 	}
	
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(BG_COLOR);
		g2.fillRect(0, 0, getWidth(), getHeight());
		String status;
		if (game.getWinner()!=0) {
			if (game.getWinner()==1) status = "White Wins";
			else if (game.getWinner()==2) status = "Black Wins";
			else status = "Draw";
		} else {
			if (game.isBlacksTurn()) status = "Black's turn";
			else status = "White's turn";
		}
		int pointsz = Float.valueOf(2.5f*cubit).intValue();
		g2.setColor(HL_COLOR);
        Font font = new Font("Dialog", Font.PLAIN, pointsz);
        g2.setFont(font);
		g2.drawString(status, pointsz, 3*pointsz/2);
		graph.paint(g2);
		if (game.isPaused()) {
			btnBack.paint(g2);
			btnForward.paint(g2);
			if (!game.isFinished()) btnUnPause.paint(g2);
		}
		for (int i = 0; i < 4; i++) {
			paintQuad(g2, i);
		}
	}
	private void paintQuad(Graphics2D g2, int quad) {
		double angle = 0;
		double extension = 1;
		int w = game.getWhite(quad);
		int b = game.getBlack(quad);
		if (quad == specialQuad) {
			if (specialQuadAngle != 0) {
				double theta = angle = specialQuadAngle;
				if (theta < 0) theta *= -1;
				theta -= Math.PI/4 ;
				// double theta = Math.PI/4 - (2*Math.PI+angle)%Math.PI/2;
				extension = Math.cos(theta) * Math.sqrt(2);	
			} else {
				w = specialQuadBW & 0x1FF;
				b = specialQuadBW >> 9;
			}
		}
		AffineTransform oldT = new AffineTransform(g2.getTransform());
		g2.translate(boardCenterX + extension*cubit*quadCenterX[quad],
					boardCenterY + extension*cubit*quadCenterY[quad]);
		g2.rotate(quad*Math.PI/2 + angle);
		g2.setColor(Board.LN_COLOR);
		g2.fill(outer);
		g2.setColor(Board.QB_COLOR);
		g2.fill(inner);
		AffineTransform newT = new AffineTransform(g2.getTransform());
		for (int i = 0; i< 9; i++) {
			g2.translate( cubit*dimpleX[i], cubit*dimpleY[i]);
			g2.rotate(-(quad*Math.PI/2+angle));  // undo frame rotation (for marble highlights)
			int bit = 1<<i;
			int marblecolor = ( (w&bit)==0? 0: 1 ) + ( (b&bit)==0? 0: 2 );
			if (marblecolor == 0) {
				g2.setPaint(dimplePaint);
				g2.fill(dimple);
				if (dimplesEnabled && highlighted==(quad*9+i)) {
					g2.setColor(game.isBlacksTurn()? BLACK: WHITE);
					g2.setStroke(new BasicStroke((float)(cubit/8.0)));
					g2.draw(marble);
				}
			} else {
				g2.setPaint(marblecolor==1? whiteMarblePaint: blackMarblePaint);
				g2.fill(marble);
			}
			g2.setTransform(newT);
		}
		if (twistersEnabled) {
			Color twister0Color = BG_COLOR;
			Color twister1Color = BG_COLOR;
			if (highlighted>>1 == quad) {
				Color highlightColor = game.isBlacksTurn()? BLACK: WHITE;
				if ((highlighted&1)==0) twister0Color = highlightColor;
				else twister1Color = highlightColor;
				
			}
			g2.setColor(twister0Color);
			g2.setStroke(new BasicStroke((float)(cubit/8.0)));
			g2.draw(twister);
			g2.scale(-1, 1);
			g2.rotate(Math.PI/2);
			g2.setColor(twister1Color);
			g2.draw(twister);
		}
		g2.setTransform(oldT);
	}

	private int getShapeIndex(int x, int y) {
		// translate to board center
		x -= boardCenterX;
		y -= boardCenterY;
		int q;
		for (q = 0; q < 4; q++) {
			// translate to quadrant center
			float qx = x - cubit*quadCenterX[q];
			float qy = y - cubit*quadCenterY[q];
			// rotate for quadrant
			if (q==1 || q==2) qx = -qx;
			if (q > 1) qy = -qy;
			if ((q&1)==1) {
				float tmp = qx;
				qx = qy;
				qy = tmp;
			}
			if (outer.contains(qx, qy)) {
				if (twistersEnabled) {
					if (twister.contains(qx,qy)) return (q<<1);
					if (twister.contains(qy,qx)) return (q<<1) + 1;
				}
				if (dimplesEnabled){
					for (int d = 0; d < 9; d++) {
						// translate to dimple center
						if (dimple.contains(qx-cubit*dimpleX[d],qy-cubit*dimpleY[d])) {
							return 9*q + d;
						}
					}
				}
				return -(q+1);	// quad hit but no dimple/twister
			}
		}
		return -(q+1);	// no dimple or quad hit (-5)
	}
	
	// methods required for ComponentListener interface
	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {
		setGeometry();
		repaint();
	}
	public void componentShown(ComponentEvent e) {}
		
	// methods required for MouseInputListener interface
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			int index = getShapeIndex(e.getX(), e.getY());
			if (index >= 0) {
				highlighted = -1;
				if (twistersEnabled) {
					twistersEnabled = false;
					game.twisterClicked( index );
				} else if (dimplesEnabled) {
					dimplesEnabled = false;
					game.dimpleClicked( index );
				}
			} else if (game.isPaused()) {
				if (btnForward.getClick(e.getX(), e.getY())) {
					game.playNext();
				} else if (btnBack.getClick(e.getX(), e.getY())) {
					game.unplayPrevious();
				} else if (btnUnPause.getClick(e.getX(), e.getY())) {
					game.unPause();
				}
			}
		}
/*
			if (e.isShiftDown()) ;
			if (!e.isControlDown()) ;
			if (e.getButton() == MouseEvent.BUTTON3) ;
			if (e.getButton() == MouseEvent.BUTTON2) ;
*/
	}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent e) {
		int hit = graph.getHit(e.getX(), e.getY());
		if (hit >= 0) {
			game.historyGraphHit(hit);
		}
	}
	public void mouseReleased(MouseEvent arg0) {}
	// methods required for MouseMotionListener interface
	public void mouseDragged(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent e) {
		int index = getShapeIndex(e.getX(), e.getY());
		if (index<0) index = -1;
		if (highlighted != index) {
			highlighted = index;
			repaint();
		}
	}

}

	
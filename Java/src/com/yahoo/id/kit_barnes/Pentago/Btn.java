package com.yahoo.id.kit_barnes.Pentago;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

class Btn {
	
	private final static float[] paintFractions = { 0, 0.3f };
	private final static Color[] paintColors = { Board.SD_COLOR, Board.HL_COLOR };
	
	private Game game;
	private RoundRectangle2D.Float outer;			// center@(0,0)
	private RoundRectangle2D.Float inner;			// define border
	private LinearGradientPaint paint;
	
	private float palm;
	private int centerX;
	private int centerY;
	private float width;		// in palms - 82 for history graph
	
	Btn(Game game) {
		this.game = game;
		outer = new RoundRectangle2D.Float();
		inner = new RoundRectangle2D.Float();
	}

	void setGeometry(float cubit, int cx, int cy, int width) {
		this.width = width;
		centerX = cx;
		centerY = cy;
		palm = cubit/4;
		float x = -width*palm/2;
		float y = -3*palm;
		float w = width*palm;
		float h = 6*palm;
		float arc = 6*palm;
		outer.setRoundRect(x,y,w,h,arc,arc);
		float border = palm/3;
		if (border<1) border = 1;
		x += border;
		y += border;
		w -= border*2;
		h -= border*2;
		arc -= border;
		if (arc < 0) arc = 0;
		inner.setRoundRect(x,y,w,h,arc,arc);
		paint = new LinearGradientPaint(0, y, 0, 0, paintFractions, paintColors,
										LinearGradientPaint.CycleMethod.REFLECT);
	}
	
	public void paint(Graphics2D g2) {
		g2.setColor(Board.LN_COLOR);
		g2.fill(outer);
		g2.setPaint(paint);
		g2.fill(inner);
	}
	
	boolean getClick(int x, int y) {
		// translate to center
		x -= centerX;
		y -= centerY;
		if (outer.contains(x, y)) {
			return true;
		}
		return false;
	}
	
	static class History extends Btn {
		GeneralPath playheadShape;
		History(Game game) {
			super(game);
			playheadShape = new GeneralPath();
		}
		void setGeometry(float cubit, int cx, int cy) {
			super.setGeometry(cubit, cx, cy, 82);
			playheadShape.reset();
			playheadShape.moveTo(0, -0.25*cubit);
			playheadShape.lineTo(-0.5*cubit, -0.75*cubit);
			playheadShape.lineTo(0.5*cubit, -0.75*cubit);
			playheadShape.lineTo(0, -0.25*cubit);
			playheadShape.lineTo(0, 0.25*cubit);
			playheadShape.lineTo(0.5*cubit, 0.75*cubit);
			playheadShape.lineTo(-0.5*cubit, 0.75*cubit);
			playheadShape.lineTo(0, 0.25*cubit);
			playheadShape.closePath();
		}
		public void paint(Graphics2D g2) {
			AffineTransform oldT = new AffineTransform(g2.getTransform());
			g2.translate( super.centerX, super.centerY);
			super.paint(g2);
			g2.setColor(Board.SD_COLOR);
			g2.setStroke(new BasicStroke(2*super.palm,BasicStroke.CAP_BUTT,BasicStroke.JOIN_BEVEL));
			int x = Float.valueOf( -36*super.palm ).intValue();
			g2.drawLine( x, 0, -x, 0);
			int rh = super.game.getRecordhead();
			for (int i = 0; i < rh; i++) {
				g2.setColor((i&2)==0? Board.WHITE: Board.BLACK);
				int nx = Float.valueOf( (i-35)*super.palm ).intValue();
				g2.drawLine(x, 0, nx, 0);
				x = nx;
			}
			g2.translate( (super.game.getPlayhead()-36)*super.palm, 0 );
			g2.setColor(Board.LN_COLOR);
			g2.fill(playheadShape);
			g2.setTransform(oldT);
		}
		int getHit(int x, int y) {
			// translate to center
			x -= super.centerX;
			y -= super.centerY;
			if (super.outer.contains(x, y)) {
				x = Float.valueOf( x/super.palm + 36.5f ).intValue();
				return x<0? 0: x;
			}
			return -1;
		}
	}
	
	static class Text extends Btn {
		String text;
		Font font;
		Text(Game game) {
			super(game);
		}
		void setGeometry(String text, float cubit, int cx, int cy) {
			this.text = text;
			font = new Font("Dialog", Font.PLAIN, Float.valueOf(cubit).intValue());
			Graphics g = super.game.getBoard().getGraphics();
		    FontMetrics metrics = g.getFontMetrics(font);
		    int width = Float.valueOf(metrics.stringWidth(text)*4/cubit + 3).intValue();
			super.setGeometry(cubit, cx, cy, width);
		}
		public void paint(Graphics2D g2) {
			AffineTransform oldT = new AffineTransform(g2.getTransform());
			g2.translate( super.centerX, super.centerY);
			super.paint(g2);
			g2.setColor(Board.SD_COLOR);
	        g2.setFont(font);
			g2.drawString(text, (1.3f-super.width/2)*super.palm, 1.6f*super.palm);
			g2.setTransform(oldT);
		}
		
	}

}

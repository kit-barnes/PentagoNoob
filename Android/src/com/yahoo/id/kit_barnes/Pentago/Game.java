package com.yahoo.id.kit_barnes.Pentago;

import android.content.SharedPreferences;
import android.util.Base64;


/*	white and black marble arrays:
		represent state of board for white and black marbles
		consist of 4 numbers
			one for each of the 4 quadrants, starting with the upper left quadrant and proceeding clockwise
			for each of the 4 numbers:
				one bit/dimple - LSB is the outer corner dimple of quadrant (farthest from board center)
					and proceeding clockwise, MSB is the center dimple of the quadrant
				range 0-511,  each bit: 1=marble 0=no_marble (of that color)
					the value of the number is the sum of the values of the dimples containing marbles of that color

								Positions  (qd)                  Dimple Values

								00  01  02   16  17  10            1   2   4    64 128   1
								07  08  03   15  18  11          128 256   8    32 256   2
								06  05  04   14  13  12           64  32  16    16   8   4

								32  33  34   24  25  26            4   8  16    16  32  64
								31  38  35   23  28  27            2 256  32     8 256 128
								30  37  36   22  21  20            1 128  64     4   2   1
		
		Example start of game, successive states: Empty board:		white = [ 0, 0, 0, 0 ], black = [ 0, 0, 0, 0 ]
		  White marble placed at upper left dimple (position 00):	white = [ 1, 0, 0, 0 ], black = [ 0, 0, 0, 0 ]
		  Upper left quadrant twisted clockwise:					white = [ 4, 0, 0, 0 ], black = [ 0, 0, 0, 0 ]
		  Black marble placed at position 01:						white = [ 4, 0, 0, 0 ], black = [ 2, 0, 0, 0 ]
		  Upper left quadrant twisted counter-clockwise:			white = [ 1, 0, 0, 0 ], black = [ 128, 0, 0, 0 ]
		  White marble placed at position 08:						white = [ 257, 0, 0, 0 ], black = [ 128, 0, 0, 0 ]
		  Upper left quadrant twisted clockwise:					white = [ 260, 0, 0, 0 ], black = [ 2, 0, 0, 0 ]
		
		The ordering of dimple positions within the quadrants allows quick quadrant rotation by
		shifting the lower 8 bits of the 2 numbers (one in each color array) representing a quadrant.

		The radial orientation of the quadrants (LSB at outer corner) allows rotating the entire board
		by merely rotating the elements within the arrays without the need to rotate individual bits.
*/


class Game {
	
	static int rotateQuad(int quad, int n) {
		// rotate quadrant bits by n*90degrees clockwise
		// assert 0 <= n <= 4
/*
		var quaduad:Number = (quad<<8) + (quad&0xFF);
		var adu:Number = (quaduad >> (8-n-n)) & 0xFF;
		return  adu + (quad & 0x100);
*/
		return  ( (((quad&0xFF) + (quad<<8)) >> (8-n-n)) & 0xFF ) + (quad & 0x100);
	}
	
	private int[] white = { 0, 0, 0, 0 };		// white marble array
	private int[] black = { 0, 0, 0, 0 };		// black marble array
	private byte[] history = new byte[72];		// moves made: white put, white rot, black put, ...
	private int recordhead;						// history index of next move to be made
	private int playhead;						// history index of next move to be displayed
	private int winner;							// 0:playing 1:white 2:black 3:draw(both) 4:draw(neither)
	Winline wins;								// linked list of winning lines
	
	private int whitePlayer;				// AI Level + 1 for white -> so iff 0 then manual (human) white
	private int blackPlayer;				// AI Level + 1 for black
	
	private Pentago pentago = null;					// the GUI
	private Thread thread;						// compute move task
	boolean abortComputeMove;					// flag for thread communication
	
	Game(Pentago p) {
		pentago = p;
		thread = null;
		whitePlayer = 0;
		blackPlayer = 2;
	}
	
	void start(SharedPreferences saved) {
		reset();
		if (saved != null) {
			whitePlayer = saved.getInt("whitePlayer",0);
			blackPlayer = saved.getInt("blackPlayer",2);
	        recordhead = saved.getInt("recordhead",0);
	        history = Base64.decode(saved.getString("history", ""), Base64.DEFAULT);
	        if (history.length != 72) {
	        	history = new byte[72];
	        	recordhead = 0;
	        }
	        jump(recordhead);
		}
		pentago.repaintBoard();
		whatsnext();
	}
	
	void reset() {						// new game
		if (thread != null) {
			abortComputeMove = true;
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
			thread = null;
		}
		for (int q = 0; q < 4; q++) {
			white[q] = 0;
			black[q] = 0;
		}
		winner = 0;
		wins = null;
		playhead = recordhead = 0;
	}
	
	void saveState(SharedPreferences state) {
		SharedPreferences.Editor ed = state.edit();
		ed.putInt("whitePlayer", whitePlayer);
		ed.putInt("blackPlayer", blackPlayer);
		ed.putInt("recordhead", recordhead);
		ed.putString("history", Base64.encodeToString(history, Base64.DEFAULT));
		ed.commit();
	}

	int getWhitePlayer()			{ return whitePlayer; }
	void setWhitePlayer(int p)		{ whitePlayer = p; startstopcompute(!isBlacksTurn() && p==0); }
	int getBlackPlayer()			{ return blackPlayer; }
	void setBlackPlayer(int p)		{ blackPlayer = p; startstopcompute(isBlacksTurn() && p==0); }
	void startstopcompute(boolean mymanual) {
		if (thread==null) {
			if (isComputersTurn()) {
				if ((recordhead&1) == 1) undoClicked();	// undo put for new compute
				else whatsnext();
			}
		} else if (mymanual) {
			abortComputeMove = true;
			whatsnext();
		}
	}
	int getAIlevel() { return (isBlacksTurn()? blackPlayer : whitePlayer) - 1; }
	
	boolean isBlacksTurn()			{ return (playhead&2) != 0; }
	boolean isComputersTurn()		{ return isBlacksTurn()? (blackPlayer!=0) : (whitePlayer!=0); }
	boolean isFinished()			{ return winner != 0; }

	int getWhite(int quad) { return white[quad]; }
	int getBlack(int quad) { return black[quad]; }
	int getWinner() { return winner; }
	int getRecordhead() { return recordhead; }
	int getPlayhead() { return playhead; }
	
	void jump(int frame) {		// just replay the game to this point with no gui calls
		if (frame > recordhead) frame = recordhead;
		winner = 0;
		for (int q = 0; q < 4; q++) {
			white[q] = 0;
			black[q] = 0;
		}
		for (playhead = 0; playhead < frame; playhead++) {
			int item = history[playhead];
			if ((playhead&1) == 0) {
				putMarble(item, isBlacksTurn()? 2: 1, false);
			} else {
				doTwist(item, false);
			}
		}
		if (playhead == recordhead) checkFinished();
	}
	
	void dimpleClicked(int dimple) {
		int quad = dimple / 9;
		int d = dimple % 9;
		int bit = 1 << d;
		int w = white[quad];
		int b = black[quad];
		if (((w|b)&bit)==0) {
			history[recordhead++] = (byte) dimple;
		}
		whatsnext();
	}
	private void putMarble(int dimple, int color, boolean animate) {
		int quad = dimple / 9;
		int d = dimple % 9;
		int bit = 1 << d;
		int oldw, oldb;
		int w = oldw = white[quad];
		int b = oldb = black[quad];
		if (color == 0) {			// no marble
			w &= ~bit;
			b &= ~bit;
		} else if (color == 1) {	// white
			w |= bit;
			b &= ~bit;
		} else {					// black
			w &= ~bit;
			b |= bit;
		}
		white[quad] = w;
		black[quad] = b;
		if (animate) pentago.animatePut(quad, oldw, oldb, w, b);	
	}
	
	void twisterClicked(int twister) {
		history[recordhead++] = (byte) twister;
		whatsnext();
	}
	private void doTwist(int twister, boolean animate) {
		int quad = twister >> 1;
		boolean clockwise = (twister&1) == 0;
		white[quad] = rotateQuad(white[quad], clockwise? 1:3);
		black[quad] = rotateQuad(black[quad], clockwise? 1:3);
		if (animate) pentago.animateTwist(twister);
	}
	
	void undoClicked() {
		if (recordhead == 0) return;
		recordhead--;
		if (thread != null) abortComputeMove = true;
		winner = 0;
		wins = null;
		if (pentago.thread != null) return;		// wait for animationDone
		undo();
	}
	
	void undo() {
		pentago.setBanner(R.string.undoing);
		int played = history[--playhead];
		if ((playhead & 1) == 0) {	// unput
			putMarble(played, 0, true);
		} else {					// untwist
			if (recordhead>0 && isComputersTurn()) recordhead--;
			doTwist(played^1, true);
		}
	}
	
	void animationDone() {
		if (recordhead < playhead) {
			undo();
		}
		else whatsnext();
	}
		
	void computeDone(ComputeMoveTask.Move move) {
		history[recordhead++] = (byte) move.dimple;
		if (move.count!=0) history[recordhead++] = (byte) move.twister;
		pentago.board.post(new Runnable() {
			public void run() {
				whatsnext();
			}
		});
	}
	
	private void whatsnext() {
		if (thread!=null) {
			try {
				thread.join();
			} catch (InterruptedException e) {}
			thread = null;
		}
		pentago.setGame(this);	// kills animations
		pentago.setBanner(R.string.empty);
		checkFinished();
		if (winner != 0) return;
		// playing existing or recording new moves
		if ( (playhead & 1) == 0) {				// ________________________put
			if ( playhead < recordhead ) {
				putMarble( history[ playhead++ ], isBlacksTurn()? 2: 1, true);
			} else if ( isComputersTurn() ) {
				abortComputeMove = false;
				pentago.setBanner(isBlacksTurn()? R.string.b_thinking : R.string.w_thinking);
				thread = new Thread(new ComputeMoveTask(this));
				thread.start();
			} else {
				pentago.setBanner(isBlacksTurn()? R.string.waiting_b_m : R.string.waiting_w_m);
				pentago.waitDimpleClick();
			}
		} else {								// ________________________twist
			if ( playhead < recordhead ) {
				doTwist( history[ playhead++ ], true);
			} else if ( isComputersTurn() ) {
				// backup history to replay entire move
				undo();
			} else {
				pentago.setBanner(isBlacksTurn()? R.string.waiting_b_t : R.string.waiting_w_t);
				pentago.waitTwisterClick();
			}
		}
	}

	private void checkFinished() {
		winner = 0;
		wins = null;
		if (sideWin(white, this)) { winner = 1; }
		if (sideWin(black, this)) { winner += 2; }
		else if (playhead == 72) winner = 4;
		switch (winner) {
		case 0: pentago.setBanner(R.string.empty); break;
		case 1: pentago.setBanner(R.string.w_win); break;
		case 2: pentago.setBanner(R.string.b_win); break;
		case 3: pentago.setBanner(R.string.draw); break;
		case 4: pentago.setBanner(R.string.stalemate); break;
		}
	}

	
	/*
	 *							WIN COMPUTATIONS
	 *
	There are a total of 32 different ways to place 5 marbles in a row.
	Taking rotations (of the board) into account, this number reduces to 8.
	All but one of these 8 include a tic-tac-toe win (3-in-a-row) in one of
	the four quadrants and 2-in-a-row in another quadrant.
	The last (called "the triple power play" in Pentago's stategy guide)
	is spread across 3 quadrants.

		row 0     row 1     row 2     col 0     col 1     col 2     diag 6    diag 5
		xxx xx.   ... ...   ... ...   x.. ...   .x. ...   ..x ...   x.. ...   ... .x.
		... ...   xxx xx.   ... ...   x.. ...   .x. ...   ..x ...   .x. ...   ... x..
		... ...   ... ...   xxx xx.   x.. ...   .x. ...   ..x ...   ..x ...   ..x ...
		... ...   ... ...   ... ...   x.. ...   .x. ...   ..x ...   ... x..   .x. ...
		... ...   ... ...   ... ...   x.. ...   .x. ...   ..x ...   ... .x.   x.. ...
		... ...   ... ...   ... ...   ... ...   ... ...   ... ...   ... ...   ... ...

	
	8 winning patterns (5 in a row) elements are values of the 4 quadrants, a dumb-down difficulty,
	and the endpoints of the winning line (in dimple coordinates)
	
	 								  quad values	difficulty	endpoints		*/
	final static int[][] win = { 	{ 7,192,0,0,    	2,		0,0, 0,4 },		// row 0
									{ 392,288,0,0,  	1,		1,0, 1,4 },		// row 1
									{ 112,24,0,0,   	2,		2,0, 2,4 },		// row 2
									{ 193,0,0,6,    	2,		0,0, 4,0 },		// column 0
									{ 290,0,0,264,  	1,		0,1, 4,1 },		// column 1
									{ 28,0,0,48,    	2,		0,2, 4,2 },		// column 2
									{ 273,0,272,0,  	1,		0,0, 4,4 },		// diag 6
									{ 16,160,0,10,  	3,		4,0, 0,4 }	};	// diag 5
/*
	boolean whiteWin() {
		return sideWin( white );
	}
	boolean blackWin() {
		return sideWin( black );
	}
*/
	static boolean sideWin( int[] side, Game game ) {
		boolean result = false;
		for (int j = 0; j < win.length; j++) {
			int[] w = win[j];
			for (int i = 0; i < 4; i++) {
				if ( (w[0]&side[(0+i)%4])==w[0] && (w[1]&side[(1+i)%4])==w[1] &&
					(w[2]&side[(2+i)%4])==w[2] && (w[3]&side[(3+i)%4])==w[3] ) {
					if (game != null) game.new Winline(j,i);
					result = true;
				}
			}
		}
		return result;
	}
	
	class Winline {
		
		public int row1, col1, row2, col2;	// endpoints of winning line in dimple coordinates
		public Winline next;				// pointer to additional winning lines
		
		Winline(int windex, int rotation) {
			
			row1 = win[windex][5];
			col1 = win[windex][6];
			row2 = win[windex][7];
			col2 = win[windex][8];
			rotate(rotation);				// rotate win line to correct orientation
			
			next = wins;
			wins  = this;
		}
		
		private void rotate(int rotation) {
			if (rotation != 0) {
				int tmp = row1;
				row1 = col1;
				col1 = 5 - tmp;
				tmp = row2;
				row2 = col2;
				col2 = 5 - tmp;
				rotate(rotation - 1);
			}
		}
	}

}

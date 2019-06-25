package com.yahoo.id.kit_barnes.Pentago;


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
	private int[] history = new int[72];		// moves made: white put, white rot, black put, ...
	private int recordhead;						// history index of next move to be made
	private int playhead;						// history index of next move to be displayed
	private int winner;							// 0:playing 1:white 2:black 3:draw(both) 4:draw(neither)
	
	private boolean paused;
	private boolean undoing;
	private boolean computerWhite;				// computer plays white
	private boolean computerBlack;				// computer plays black
	
	private Board board = null;					// the GUI
	private Thread thread;						// compute move task
	boolean abortComputeMove;					// flag for thread communication
	
	Game() {
		thread = null;
		computerWhite = false;
		computerBlack = false;
	}
	
	void setComputerWhite(boolean w) { computerWhite = w; }
	void setComputerBlack(boolean b) { computerBlack = b; }
	void setBoard(Board board) {
		this.board = board;
		board.setGame(this);
	}
	Board getBoard() { return board; }
	
	void start() {
		reset();
		whatsnext();
	}
	
	void reset() {						// new game
System.err.println("game.reset()--------------------------");
		for (int q = 0; q < 4; q++) {
			white[q] = 0;
			black[q] = 0;
		}
		winner = 0;
		playhead = recordhead = 0;
		paused = false;
		undoing = false;
	}
		
	boolean isBlacksTurn()			{ return (playhead&2) != 0; }
	boolean isComputersTurn()		{ return isBlacksTurn()? computerBlack : computerWhite; }
	boolean isPaused()				{ return paused; }
	boolean isFinished()			{ return winner != 0; }
//	boolean playing()				{ return !paused && playhead<recordhead; }
//	boolean recording()				{ return !paused && playhead==recordhead; }

	int getWhite(int quad) { return white[quad]; }
	int getBlack(int quad) { return black[quad]; }
	int getWinner() { return winner; }
	int getRecordhead() { return recordhead; }
	int getPlayhead() { return playhead; }
	
	int getMarble(int quad, int dimple) {	// returns 0:none 1:white 2:black ELSE ERROR!
		int bit = 1 << dimple;
		return ((white[quad] & bit)==0? 0 : 1 ) + ((black[quad] & bit)==0? 0 : 2 );
	}

	void jump(int frame) {
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
			history[recordhead++] = dimple;
		}
		whatsnext();
	}
	private void putMarble(int dimple, int color, boolean animate) {
		int quad = dimple / 9;
		int d = dimple % 9;
		int bit = 1 << d;
		int w = white[quad];
		int b = black[quad];
		int old = (b<<9) + w;
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
		int now = (b<<9) + w;
		white[quad] = w;
		black[quad] = b;
		if (animate) board.animatePut(quad, old, now);	
	}
	
	void twisterClicked(int twister) {
		history[recordhead++] = twister;
		whatsnext();
//		doTwist(twister, true);
	}
	private void doTwist(int twister, boolean animate) {
		int quad = twister >> 1;
		boolean clockwise = (twister&1) == 0;
		white[quad] = rotateQuad(white[quad], clockwise? 1:3);
		black[quad] = rotateQuad(black[quad], clockwise? 1:3);
		if (animate) board.animateTwist(twister);
	}
	
	void animationDone() {
		if (undoing) {
			playhead--;
			undoing = false;
		}
		else playhead++;
		whatsnext();
	}
		
	void computeDone(ComputeMoveTask.Move move) {
		history[recordhead++] = move.dimple;
		if (move.count!=0) history[recordhead++] = move.twister;
	}
	
	void historyGraphHit( int hit ) {
		paused = true;
		if (hit > recordhead) hit = recordhead;
		jump(hit);
		board.setGame(this);	// stops any animation
		whatsnext();			// wait for control input
	}
	void playNext() {
		if (playhead<recordhead){
			int item = history[playhead];
			if ((1&playhead)==0) {
				putMarble(item, isBlacksTurn()?2:1, true);
			} else {
				doTwist(item, true);
			}
		}
	}
	void unplayPrevious() {
		if (playhead > 0) {
			undoing = true;
			int item = history[playhead-1];
			if ((1&playhead)==1) {
				putMarble(item, 0, true);
			} else {
				doTwist(item^1, true);
			}
		}
	}
	
	void unPause() {
		if ((playhead&1) != 0) {
			playhead -= 1;
			jump(playhead);
		}
		recordhead = playhead;
		paused = false;
		whatsnext();
	}
	
	private void whatsnext() {
//		var item:Number;
//		var marble:Number;
//		// kill or disable all inputs
//		if ( optionDialog ) {
//			optionDialog.removeMovieClip();
//			optionDialog = null;
//		}
//		if (!controls) {
//			controls = Utils.newMovieClip(Controls,scene);
//			controls.init();
//		}
		if (thread!=null) {
			try {
				thread.join();
			} catch (InterruptedException e) {}
			thread = null;
		}
		board.setGame(this);
//		if ( undoing ) {
//			unplay();
//			return;
//		}
//		if ( undone ) {
//			history.length = playhead;
//			undone = false;
//		}
//		busy = true;
		checkFinished();
//		trace("  history = "+history);
//		trace("  playhead = "+playhead+"  length = "+history.length);
//		if (fullControls.val) {
//			controls.graph.paint();
//			if (stopped) {
//				controls.btnForward._visible = true;
//				controls.btnForward.enable(playhead < history.length);
//				controls.btnBack._visible = true;
//				controls.btnBack.enable(playhead > 0);
//				controls.btnRecord._visible = true;
//				controls.btnPlay._visible = true;
//			} else {
//				controls.btnForward._visible = false;
//				controls.btnBack._visible = false;
//				controls.btnRecord._visible = false;
//				controls.btnPlay._visible = false;
//			}
//		} else {
//			stopped = false;
//		}
//		showTurn();
		if (paused) {
			// waiting for control press
//			busy = false;
			board.repaint();
			return;
		}
		// playing existing or recording new moves
		if ( (playhead & 1) == 0) {				// put
			if ( playhead < recordhead ) {
				putMarble( history[ playhead ], isBlacksTurn()? 2: 1, true);
			} else if ( isComputersTurn() ) {
				abortComputeMove = false;
				thread = new Thread(new ComputeMoveTask(this));
				thread.start();
				whatsnext();
			} else {
				board.waitDimpleClick();
			}
		} else {								// twist
			if ( playhead < recordhead ) {
				doTwist( history[ playhead ], true);
			} else if ( isComputersTurn() ) {
				// backup history to replay entire move
System.err.println("  undoing put for compute");
				playhead--;
				recordhead--;
				abortComputeMove = false;
				thread = new Thread(new ComputeMoveTask(this));
				thread.start();
				whatsnext();
			} else {
				board.waitTwisterClick();
			}
		}

	}

	private void checkFinished() {
		winner = 0;
		if (sideWin(white)) { winner = 1; }
		if (sideWin(black)) { winner += 2; }
		if (winner == 0 && playhead == 72) winner = 4;
		if (winner != 0) paused = true;
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

		row o     row 1     row 2     col 0     col 1     col 2     diag 6    diag 5
		xxx xx.   ... ...   ... ...   x.. ...   .x. ...   ..x ...   x.. ...   ... .x.
		... ...   xxx xx.   ... ...   x.. ...   .x. ...   ..x ...   .x. ...   ... x..
		... ...   ... ...   xxx xx.   x.. ...   .x. ...   ..x ...   ..x ...   ..x ...
		... ...   ... ...   ... ...   x.. ...   .x. ...   ..x ...   ... x..   .x. ...
		... ...   ... ...   ... ...   x.. ...   .x. ...   ..x ...   ... .x.   x.. ...
		... ...   ... ...   ... ...   ... ...   ... ...   ... ...   ... ...   ... ...
	*/
	
	// 8 winning patterns (5 in a row) elements are values of the 4 quadrants and a dumb-down difficulty
	final static int[][] win = { 	{ 7,192,0,0,    2 },	// row 0
									{ 392,288,0,0,  1 },	// row 1
									{ 112,24,0,0,   2 },	// row 2
									{ 193,0,0,6,    2 },	// column 0
									{ 290,0,0,264,  1 },	// column 1
									{ 28,0,0,48,    2 },	// column 2
									{ 273,0,272,0,  1 },	// diag 6
									{ 16,160,0,10,  3 }	};	// diag 5
	boolean whiteWin() {
		return sideWin( white );
	}
	boolean blackWin() {
		return sideWin( black );
	}
	static boolean sideWin( int[] side ) {
		for (int j = 0; j < win.length; j++) {
			int[] w = win[j];
			for (int i = 0; i < 4; i++) {
				if ( (w[0]&side[(0+i)%4])==w[0] && (w[1]&side[(1+i)%4])==w[1] &&
					(w[2]&side[(2+i)%4])==w[2] && (w[3]&side[(3+i)%4])==w[3] ) {
					return true;
				}
			}
		}
		return false;
	}
	
	
}

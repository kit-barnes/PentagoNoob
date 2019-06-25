package com.yahoo.id.kit_barnes.Pentago;


class ComputeMoveTask implements Runnable {

	private Game game;
	
	class Move {		// computed move
		int dimple;			// marble position (0-35)
		int twister;		// twist ( 2*quadrant + clockwise )
		int[] losswin;		// lose-win combined histograms
		int count;			// count of moves with equal histograms
		int tttblocks;
		public String toString() {
			String s = "dimple:"+dimple+" twister:"+twister+" losewin:"+losswin[0];
			for (int i = 1; i < 14; i++) {
				s += ","+losswin[i];
			}
			s += " tttblocks:"+tttblocks+" count:"+count;
			return s;
		}
	}
								// copies of white and black arrays from game
	private int marbles[];		//   marbles <- black's turn?  black : white
	private int blocked[];		//   blocked <- black's turn?  white : black
	
	// these pre-computed arrays replace functions to speed computation
	private int countBits[];		// bits_set = countBits[value] for value in 0-511
	private int rotateCW[];			// rotate quadrant clockwise
	private int rotateCCW[];		// rotate quadrant counterclockwise


	ComputeMoveTask(Game game){
		this.game = game;
	}
	
	@Override
	public void run() {
		// copy marble arrays
		marbles = new int[4];
		blocked = new int[4];
		for (int q = 4; q-->0;) {
			marbles[q] = game.isBlacksTurn()? game.getBlack(q): game.getWhite(q);
			blocked[q] = game.isBlacksTurn()? game.getWhite(q): game.getBlack(q);
		}
		// pre-compute rotations of, and number of set bits in, numbers from 0 to 511
		rotateCW = new int[512];
		rotateCCW = new int[512];
		countBits = new int[512];
		for ( int i = 512; i-- > 0;) {
			// store rotations of i
			rotateCW[i] = Game.rotateQuad(i,1);
			rotateCCW[i] = Game.rotateQuad(i,3);
			// count bits in i
			int w = i;
			int bits = 0;
			for (int b = 256; w > 0 ; b>>=1) {
				if (b > w) continue;
				bits++;
				w -= b;
			}
			countBits[i] = bits;
		}
		// compute best move
		Move move = pickMove();
		if (game.abortComputeMove) return;
		game.computeDone(move);
	}

	
	// compute minimum # moves to a particular win (one of 36 - does not rotate board)
	//		a move consists of a put and a twist.
	//		method is to first compute an array (boardPT) of all the possible moves to achieve the win
	//		boardPT consists of 4 arrays (qpt) - one for each quadrant.
	//		the values of the qpt elements are the number of additional marbles
	//      needed to achieve the win in that quadrant and the element indices are the number of twists
	//		Once the boardPT array is completed, it is searched for the minimum combination of puts and twists
	//		to achieve that particular win.
	//
	//		this routine is at the innermost loop of the computer move calculation - OPTIMIZE FOR SPEED!!!
	//
	private int winDistance(			// returns 0(already won) to 6(blocked)
						 int[] w,			// the win (one of 8 rows from win[])
						 int[] marbles,		// marble array (as in black or white) for this side
						 int[] blocked,		// other side's marbles
						 boolean doTwists	// normally true - false for dumb-down
						 ) {

		// build boardPT
		int[][] boardPT = new int[4][3];	// twist dimension is 3 rather than 4
		int q = 4;							//    because 3 clockwise = 1 counterclockwise
		while ( q-- > 0 ) {					// for each quadrant
			int[] qpt = boardPT[q];
			int qw = w[q];					// winning pattern
			int qm = marbles[q];	
			if ( ( qm & qw ) == qw ) {
				// minimum possible as is - don't need to search other twists
				qpt[2] = qpt[1] = qpt[0] = 0;
			} else {
				// determine win requirements for each rotated position
				int qb = blocked[q];
				// rotation loop unwrapped for speed
				// (qb&qw=blocked - if blocked call it 6 puts)
				int total = qpt[0] = (qb&qw)==0 ? countBits[qw-(qm&qw)] : 6;
				if ( ((qm|qb)&255)>0 && doTwists ) {	// rotate if outer dimples are non-empty (unless dumb)
					qm = rotateCW[qm];
					qb = rotateCW[qb];
					qpt[1] = (qb&qw)==0 ? countBits[qw-(qm&qw)] : 6;
					qm = rotateCW[qm];
					qb = rotateCW[qb];
					total += qpt[2] = (qb&qw)==0 ? countBits[qw-(qm&qw)] : 6;
					qm = rotateCW[qm];
					qb = rotateCW[qb];
					int pt = (qb&qw)==0 ? countBits[qw-(qm&qw)] : 6;
					if (pt < qpt[1]) qpt[1] = pt;	// 3 clockwise = 1 counterclockwise
					total += qpt[1];
				} else {	// outer dimples empty (or dumb)
					qpt[1] = qpt[2] = 6;
					total += 12;
				}
				if (total == 18) return 6;  // no need to continue - win is blocked
			}
		}
		// search boardPT for smallest number of moves
		// get array elements for speed 
		int[] q0pt = boardPT[3];
		int[] q1pt = boardPT[2];
		int[] q2pt = boardPT[1];
		int[] q3pt = boardPT[0];
		int moves = 6;
		int i = 3;
		while (i-->0) {
			int j = 3;
			int p = q0pt[i];
			int t = i;
			while (j-->0) {
				int k = 3;
				int pp = p + q1pt[j];
				int tt = t + j;
				while (k-->0) {
					int l = 3;
					int ppp = pp + q2pt[k];
					int ttt = tt + k;
					while (l-->0) {
						int pppp = ppp + q3pt[l];
						int tttt = ttt + l;
						if (tttt > pppp) pppp = tttt;	// moves determined by greater
						if (pppp == 0) return 0;		// can't better this
						if (pppp < moves) moves = pppp;	// save best
					}
				}
			}
		}
		return moves;
	}

	
	private final static int[][] TTTBLOCK = {
		{ 6, 272, 192 },		// 0
		{ 5, 288 },				// 1
		{ 3, 320, 24 },			// 2
		{ 20, 384, },			// 3
		{ 96, 257, 12 },		// 4
		{ 80, 258 },			// 5
		{ 129, 260, 48 },		// 6
		{ 65, 264 },			// 7
		{ 17, 34, 68, 136 }		// 8
	};
	private int countTTTblocks(int dimple, int quadblock) {
		int count = 0;
		for ( int i = TTTBLOCK[dimple].length; i-- > 0; ) {
			int block = TTTBLOCK[dimple][i];
			if ( (quadblock & block) == block ) count++;
		}
		return count;
	}

	private final static int[] DIFFICULTY = { 0,1,2,3,4,5,8,1000000 };	// first element not used

	/*
	pickmove:
		for every possible move (emptydimples*8twists) compute all 36 win distances for both sides
		pick best (most my0, least other1, most my1, least other2, most my2, least other3, ...)
		Computation split into 9 parts to provide progress display and avoid taking-too-long dialog
		on slower machines.
	*/
	private Move pickMove() {
		long startTime = System.currentTimeMillis();
		Move best = new Move();
		best.count = 0;		// count of equally "good" moves
		for ( int d = 9; d-- > 0; ) {
			for ( int q = 4; q-- > 0; ) {
				if (game.abortComputeMove) return best;
				int marble =  1 << d;
				if ( ( (marbles[q]|blocked[q])&marble ) != 0) continue;		// dimple already occupied
				// add trial marble
				marbles[q] += marble;
				if (Game.sideWin(marbles)) {
System.err.println("win with no twist -- q="+q+" d="+d+" marble="+marble);
					best.dimple = 9*q + d;
					best.count = 0;
					return best;
				}
				int tttblk = countTTTblocks(d, blocked[q]);
				// try clockwise and counterclockwise twists of all quadrants
				for (int tq = 4; tq-- > 0; ) {		// twisted quadrant number (0-3)
					int unrotMarbles = marbles[tq];
					int unrotBlocked = blocked[tq];
					for ( int ccw = 2; ccw-- > 0; ) {	// twist counterclockwise if 1 (0-1)
						int[] rotate = ccw==1? rotateCCW : rotateCW;
						marbles[tq] = rotate[ unrotMarbles ];
						blocked[tq] = rotate[ unrotBlocked ];
						// see how good this move is
						// compute distance to all losses & wins
						int[] lw = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};	// lose-win distance histogram
						for (int i = 4; i-- > 0;) {		// 4 board rotations
							for (int j = 8; j-- > 0;) {	// times 8 wins per rotation
								int[] win = Game.win[j];
								// dumb-down
//									var randsmart:Number = Math.random()*DIFFICULTY[Game.difficulty];
//									var doDefenseTwists:Boolean = randsmart > win[4];
//									var doOffenseTwists:Boolean = randsmart + 1 > win[4];
								// dumb-down
								// make histograms of results
//									lw[ 2*winDistance(win, blocked, marbles, doDefenseTwists) ]--;	// losses (opponent wins)
//									lw[ 2*winDistance(win, marbles, blocked, doOffenseTwists) + 1 ]++;	// wins
								lw[ 2*winDistance(win, blocked, marbles, true) ]--;		// losses (opponent wins)
								lw[ 2*winDistance(win, marbles, blocked, true) + 1 ]++;	// wins
							}
							// rotate board
							int tempm = marbles[3];
							int tempb = blocked[3];
							for (int k = 3; k-- > 0;) {
								marbles[k+1] = marbles[k];
								blocked[k+1] = blocked[k];
							}
							marbles[0] = tempm;
							blocked[0] = tempb;
						}
						// adjust histogram
						if (lw[0]!=0) lw[0] = -1;		// one can only lose once
						if (lw[1]!=0) lw[1] = 1;		// one can only win once
						for ( int i = 1; i < 6; i ++ ) {
							// accounting for minimal blocking moves...							
							if ( ( lw[2*i] += (i-1) ) > 0 ) lw[2*i] = 0;
							if ( ( lw[2*i+1] -= i ) < 0 ) lw[2*i+1] = 0;
						}
						lw[12] = -lw[12];		// reverse sense of blocked wins
						lw[13] = -lw[13];
						if ( best.count != 0 ) {		// compare this move to best
							int i;
							for (i = 0; i < 14; i++) {
								if ( lw[i] != best.losswin[i] ) break;
							}
							if ( i == 14 ) {		// same as best lw - check for ttt blocks
								if (tttblk == best.tttblocks) {		// same as best - randomly replace
									int oldcount = best.count;
									best.count++;
									if (Math.random()*best.count > oldcount) {
										best.dimple = 9*q + d;
										best.twister = 2*tq + ccw;
									}
									continue;
								}
								else if ( tttblk < best.tttblocks ) continue;	// worse than best
							}
							else if ( lw[i] < best.losswin[i] ) continue;	// worse than best
						} else {
							best.tttblocks = countTTTblocks(d, blocked[q]);
						}
						// better (or first) move found -- update best
						best.dimple = 9*q + d;
						best.twister = 2*tq + ccw;
						best.losswin = lw;
						best.tttblocks = tttblk;
						best.count = 1;
					}
					// restore quadrant to unrotated
					marbles[tq] = unrotMarbles;
					blocked[tq] = unrotBlocked;
				}
				// remove trial marble
				marbles[q] -= marble;
			}
		}
		// done with last dimple
System.err.println("pickmove: time:"+(System.currentTimeMillis()-startTime));
System.err.println("pickmove: "+best);
		return best;
		
	}

	
	
}

class Situation {

	/*
	I realize the implementation of arrays in Actionscript is far from optimal
	(not to mention the representation of integers) and so the language
	is particularly ill-suited to the algorithms I'm using to determine moves.
	Still, I've gained a bit of speed by substituting array lookups for computation (countBits & rotateBits)

	I picked Flash (and therefore Actionscript) primarily for the ease of graphicly rotating the quadrants.
	Maybe later I'll bite the rotation bullet in another language - Cairo via C++ or Python?  But what environment?
	Android application environment looks inviting - Java with a custom 2D graphics package plus Linux.

	white and black marbles arrays:
		represent state of board for white and black marbles
		consist of 4 numbers
			one for each of the 4 quadrants, starting with upperleft and proceeding clockwise
			for each of the 4 numbers:
				one bit/marble - LSB is outer corner marble of quadrant and proceeding clockwise, MSB is center
				range 0-511,  each bit: 1=marble 0=no_marble (of that color)
				Note: differs from the dimple ordering, where LSB is always at upper left corner of quadrant.

								Positions                        Values

								00  01  02   16  17  10            1   2   4    64 128   1
								07  08  03   15  18  11          128 256   8    32 256   2
								06  05  04   14  13  12           64  32  16    16   8   4

								32  33  34   24  25  26            4   8  16    16  32  64
								31  38  35   23  28  27            2 256  32     8 256 128
								30  37  36   22  21  20            1 128  64     4   2   1

		Dimples rotate with their quadrants - these positions do not.

		The ordering of marble/dimple positions within the quadrants allows quick quadrant rotation by
		shifting the lower 8 bits of the 2 numbers (one in each color array) representing a quadrant.

		The radial orientation of the quadrants (LSB at outer corner) allows rotating the entire board
		by merely rotating the elements within the arrays without the need to rotate individual bits.
	*/
	var white:Array;
	var black:Array;

	// these pre-computed arrays replace functions to speed computations
	static var countBits:Array;		// bits_set = countBits[value] for value in 0-511
	static var rotateBits:Array;	// rotated_quadrant = rotateBits[unrotated_quadrant][[0-3]

	function Situation() {

		white = [0,0,0,0];
		black = [0,0,0,0];

		// pre-compute rotations of, and number of set bits in, numbers from 0 to 511
		rotateBits = [ [],[],[],[] ];
		countBits = [];
		var i:Number;
		var j:Number;
		var b:Number;
		var w:Number;
		var bits:Number;
		for ( i = 0; i < 512; i++ ) {
			// store rotations of i
			for ( j = 0; j < 4; j++ ) {
				rotateBits[j].push(rotateQuadrantBits(i,j));
			}
			// count bits in i
			w = i;
			bits = 0;
			for (b = 256; w > 0 ; b>>=1) {
				if (b > w) continue;
				bits++;
				w -= b;
			}
			countBits.push(bits);
		}
	}


	// print board per black & white arrays to text output panel
	function showBoard():Void {
		var r:Number;
		for (r = 0; r < 6; r++) {
			trace(getRow(r));
		}
	}
	static var indices = [	[  [0,0],[0,1],[0,2], [1,6],[1,7],[1,0]  ],
							[  [0,7],[0,8],[0,3], [1,5],[1,8],[1,1]  ],
							[  [0,6],[0,5],[0,4], [1,4],[1,3],[1,2]  ],

							[  [3,2],[3,3],[3,4], [2,4],[2,5],[2,6]  ],
							[  [3,1],[3,8],[3,5], [2,3],[2,8],[2,7]  ],
							[  [3,0],[3,7],[3,6], [2,2],[2,1],[2,0]  ]  ];
	function getRow(r:Number):String {
		var sym = [ "- ", "x ", "o ", "# " ];	// none, black, white, error
		var s:String = "    ";
		var c:Number;
		for (c = 0; c < 6; c++) {
			var i:Number = indices[r][c][0];	// index into white/black array - (the quadrant number)
			var b:Number = indices[r][c][1];	// bit position in array member
			var W:Number = (white[i]>>b)%2;		// white marble (0 or 1) in that position
			var B:Number = (black[i]>>b)%2;		// black marble (0 or 1)
			s += sym[ B + 2*W ];
		}
		return s;
	}

	function placeMarble( index:Number, blak:Number, place:Boolean ):Void {
		var bw:Array;
		var dindex:Number = index % 9;				// position of dimple in block
		var qindex:Number = (index-dindex) / 9;		// quadrant index
//		trace("PlaceMarble(b="+qindex+" d="+dindex+" black="+blak+" place="+place+")");
		var marble = rotateBits[((Game.quadrant[qindex]._rotation+720)/90-qindex)%4][1<<dindex];
		bw = blak? black: white;
		bw[qindex] += place? marble: -marble;
//		showBoard();
	}

	function rotateQuadrantBits(quad:Number,n:Number):Number {
		// rotate quadrant bits by n*90degrees clockwise
		// assert 0 <= n <= 4
/*
		var quaduad:Number = (quad&0xFF) + (quad<<8);
		var adu:Number = (quaduad >> (8-n-n)) & 0xFF;
		return  adu + (quad & 0x100);
*/
		return  ( (((quad&0xFF) + (quad<<8)) >> (8-n-n)) & 0xFF ) + (quad & 0x100);
	}

	function twist(qindex:Number, clockwise:Boolean):Void {
//		trace("Twist(q="+qindex+" clockwise="+clockwise+")");
		white[qindex] = rotateBits[ clockwise?1:3 ][ white[qindex] ];
		black[qindex] = rotateBits[ clockwise?1:3 ][ black[qindex] ];
//		showBoard();
	}
	
	function jump():Void {		// to playhead position
		var item:Number;
		var j:Number;
		var q:Number;
		var d:Number;
		var w:Number;
		var b:Number;
		for ( q = 0; q < 4; q++ ) {
			white[q] = 0;
			black[q] = 0;
			Game.quadrant[q].zeroPosition();
		}
		for ( j = 0; j < Game.playhead; j++ ) {
			item = 0+Game.history[j];
			if (j&1) {
				twist( q = item>>1, (b = item&1)!=0 );
				Game.quadrant[q]._rotation += b?90:-90;
			} else {
				placeMarble( item, j&2, true );
			}
		}
		for ( q = 0; q < 4; q++ ) {
			item = ((4 + q) - Game.quadrant[q]._rotation/90) % 4;
			for ( d = 0; d < 9; d++ ) {
				b = 1<<d;
				w = rotateBits[item][white[q]] & b;
				b &= rotateBits[item][black[q]];
				w = w? 1: (b? -1: 0);
				b = 9*q + d;
				Dimple.dimple[b].marble = w;
				Dimple.dimple[b].paint(w);
			}
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

		row o     row 1     row 2     col 0     col 1     col 2     diag 6    diag 5
		xxx xx.   ... ...   ... ...   x.. ...   .x. ...   ..x ...   x.. ...   ... .x.
		... ...   xxx xx.   ... ...   x.. ...   .x. ...   ..x ...   .x. ...   ... x..
		... ...   ... ...   xxx xx.   x.. ...   .x. ...   ..x ...   ..x ...   ..x ...
		... ...   ... ...   ... ...   x.. ...   .x. ...   ..x ...   ... x..   .x. ...
		... ...   ... ...   ... ...   x.. ...   .x. ...   ..x ...   ... .x.   x.. ...
		... ...   ... ...   ... ...   ... ...   ... ...   ... ...   ... ...   ... ...
	*/
	// 8 winning patterns (5 in a row) elements are values of the 4 quadrants and a dumb-down difficulty
	static var win:Array = [ 	[ 7,192,0,0,    2 ],	// row 0
								[ 392,288,0,0,  1 ],	// row 1
								[ 112,24,0,0,   2 ],	// row 2
								[ 193,0,0,6,    2 ],	// column 0
								[ 290,0,0,264,  1 ],	// column 1
								[ 28,0,0,48,    2 ],	// column 2
								[ 273,0,272,0,  1 ],	// diag 6
								[ 16,160,0,10,  3 ] ];	// diag 5



	function whiteWin():Boolean {
		return sideWin( white );
	}
	function blackWin():Boolean {
		return sideWin( black );
	}
	function sideWin( side:Array ):Boolean {
		var i:Number;
		var j:Number;
		var w:Array;
		for (j = 0; j < win.length; j++) {
			w = win[j];
			for (i = 0; i < 4; i++) {
				if ( (w[0]&side[(0+i)%4])==w[0] && (w[1]&side[(1+i)%4])==w[1] &&
					(w[2]&side[(2+i)%4])==w[2] && (w[3]&side[(3+i)%4])==w[3] ) {
					return true;
				}
			}
		}
		return false;
	}

	// compute minimum # moves to a particular win
	//		a move consists of a put and a twist.
	//		method is to first compute an array (brdpt) of all the possible moves to achieve the win
	//		brdpt consists of 4 lists (qpt) - one for each quadrant.
	//		the elements of the list are of the form [p,t] - where p is the number of additional marbles
	//      needed to achieve the win in that quadrant twisted  t times
	//		Once the brdpt array is completed, it is searched for the minimum combination of puts and twists
	//		to achieve that particular win.
	//
	//		this routine is at the innermost loop of the computer move calculation - OPTIMIZE FOR SPEED!!!
	//
	//		brdpt replaced by q3210p and q3210t (to reduce array references)
	//
	function winDistance(
						 w:Array,			// the win (one of 8 rows from win[])
						 marbles:Array,		// winning side
						 blocked:Array,		// blocking side
						 doTwists:Boolean	// normally true - false for dumb-down
						 ):Number {			// returns 0(already won) to 6(blocked)

		var q3210p:Array = []	// for each quad: list of #additionalMarbles required for various twists for win
		var q3210t:Array = []	// lists of twists associated with #'s in q3210p
								
		var q:Number;			// quadrant
		var qw:Number;			// quadrant win (marbles required for win)
		var qm:Number;			// quadrant marbles (my marbles in quadrant)
		var qb:Number;			// quadrant blocking marbles (opponent's marbles)
		var qp:Array;			// quadrant put - list of #puts to achieve win
		var qt:Array;			// quadrant twist - list of twists to achieve win
		var moves:Number;		// value returned
		var p:Number;
		var t:Number;
		var pp:Number;
		var tt:Number;
		var ppp:Number;
		var ttt:Number;
		var pppp:Number;
		var tttt:Number;
		var rotate1:Array = rotateBits[1];	// only need to rotate 1-at-a-time

		// build q3210p and q3210t - (backwards from normal ordering for speed)
		q = 4;
		while ( q-- ) {		// each quadrant
			qw = w[q];
			qm = marbles[q];
			if ( ( qm & qw ) == qw ) {
				q3210p.push( [0] );		// minimum possible as is - don't need to search other twists
				q3210t.push( [0] );
			} else {
				// determine win requirements for each rotated position
				qb = blocked[q];
				qp = [];
				qt = [];
					// rotation loop unwrapped for speed
					// (qb&qw=blocked - if blocked; no need to push - can't be part of win)
					if ( !(qb & qw) ) {
						qp.push( countBits[qw-(qm&qw)] );
						qt.push( 0 );
					}
					if ( ((qm | qb) & 255) && doTwists ) {	// rotate if outer dimples are non-empty (unless dumb)
						qm = rotate1[qm];
						qb = rotate1[qb];
						if ( !(qb & qw) ) {
							qp.push( countBits[qw-(qm&qw)] );
							qt.push( 1 );
						}
						qm = rotate1[qm];
						qb = rotate1[qb];
						if ( !(qb & qw) ) {
							qp.push( countBits[qw-(qm&qw)] );
							qt.push( 2 );
						}
						qm = rotate1[qm];
						qb = rotate1[qb];
						if ( !(qb & qw) ) {
							qp.push( countBits[qw-(qm&qw)] );
							qt.push( 1 );	// 3 clockwise twists replaced by one counterclockwise twist
						}
					}
				if (!qp.length) return 6;  // no need to continue - win is blocked
				q3210p.push(qp);
				q3210t.push(qt);
			}
		}
		// get array elements for speed 
		var q0p = q3210p[3];
		var q0t = q3210t[3];
		var q1p = q3210p[2];
		var q1t = q3210t[2];
		var q2p = q3210p[1];
		var q2t = q3210t[1];
		var q3p = q3210p[0];
		var q3t = q3210t[0];
		// search brdpt for smallest number of moves
		moves = 6;
		var i = q0p.length;
		while (i--) {
			p = q0p[i];
			t = q0t[i];
			var j = q1p.length;
			while (j--) {
				pp = p + q1p[j];
				tt = p + q1t[j];
				var k = q2p.length;
				while (k--) {
					ppp = pp + q2p[k];
					ttt = pp + q2t[k];
					var l = q3p.length;
					while (l--) {
						pppp = ppp + q3p[l];
						tttt = ppp + q3t[l];
						if (tttt > pppp) pppp = tttt;	// moves determined by greater
						if (pppp == 0) return 0;		// can't better this
						if (pppp < moves) moves = pppp;	// save best
					}
				}
			}
		}
		return moves;
	}

/*
	function computeDistances():Void {
		var side:Number;		// 0=black, 1=white
		var dist:Array;			// winning distance array for side
		var marbles:Array;		// side's marbles
		var blocked:Array;		// other side's marbles
		var i:Number;			// board rotation (0-3)
		var j:Number;			// index of one of 8 rows in win array
		for (side = 0; side < 2; side++) {
			if (side) {
				dist = whiteWinDistance;
				marbles = white;
				blocked = black;
			} else {
				dist = blackWinDistance;
				marbles = black;
				blocked = white;
			}
			for (i = 0; i < 4; i++) {
				for (j = 0; j < 8; j++) {
					dist[8*i+j] = winDistance( win[j], marbles, blocked );
				}
				// rotate board
				marbles.push(marbles.shift());
				blocked.push(blocked.shift());
			}
		}
		trace("whiteWinDistance");
		trace(whiteWinDistance);
		trace("blackWinDistance");
		trace(blackWinDistance);
		trace("");
	}
*/

	static var difficultyList:Array = [ 0,1,2,3,4,5,8,1000000 ];	// first element not used

	/*
	pickmove:
		for every possible move (emptydimples*8twists) compute all 36 win distances for both sides
		pick best (most my0, least other1, most my1, least other2, most my2, least other3, ...)
		Computation split into 9 parts to provide progress display and avoid taking-too-long dialog
		on slower machines.
	*/
	var moveDimple:Number;		// retains dimple across calls (and iteration counter)
	var best:Object;			// Game.computerMove - retains best move found across calls
	var millis:Number;			// keep track of computation time (for improving optimization)
	function pickMove(cMove:Object):Void {	// returns move object in cMove with properties:
									//				qindex - index into quadrant[]  (o-3)
									//				dindex - dimple number in quadrant  (0-8)
									//				tindex - index into twister[]  (0-7)
									//				count - # of similarly good moves (equal w & l)
									//				w - histogram of my win distances
									//				l - histogram of other side's win distances
		millis = getTimer();
		best = cMove;
		best.count = 0;		// count of equally "good" moves
		moveDimple = 9;		// iteration downcounter
		Game.setHeader("Thinking - "+moveDimple,20,(Game.playhead&2)?0x000000:0xFFFFFF);
		Game.intervalId = setInterval(this, "iterPickMove", 100);
		return;
	}
	function iterPickMove():Void {
		clearInterval(Game.intervalId);
		var q:Number;	// marble placement quadrant (0-3)
		var d:Number;	// placemant dimple in q
		if ( moveDimple-- ) {
			d = moveDimple;			// shorter and quicker
			var marbles:Array;
			var blocked:Array;
			if (Game.playhead&2) {
				marbles = black.slice();
				blocked = white.slice();
			} else {
				marbles = white.slice();
				blocked = black.slice();
			}
			for ( q = 0; q < 4; q++ ) {
				if (Dimple.occupied(9*q+d)) continue;
				var marble = rotateBits[ ((Game.quadrant[q]._rotation+720)/90 - q)%4 ][ 1<<d ];
				// add trial marble
				marbles[q] += marble;
				if (sideWin(marbles)) {
	trace("win with no twist -- q="+q+" d="+d+" marble="+marble);
					best.dindex = 9*q + d;
					best.count = 0;
					clearInterval(Game.intervalId);
					Game.intervalId = 0;
					Game.whatsnext();// win with no rotation - do it!
					return;
				}
				// try clockwise and counterclockwise twists of all quadrants
				var tq:Number;	// twisted quadrant number (0-3)
				var cw:Number;	// twist clockwise iff 1 (0-1)
				for ( tq = 0; tq < 4; tq++ ) {
					var unrotMarbles = marbles[tq];
					var unrotBlocked = blocked[tq];
					for ( cw = 0; cw < 2; cw++ ) {
						// see how good this move is
						var lw = [0,0,0,0,0,0,0,0,0,0,0,0,0,0];	// lose-win distance histograph
						marbles[tq] = rotateBits[ cw?1:3 ][ unrotMarbles ];
						blocked[tq] = rotateBits[ cw?1:3 ][ unrotBlocked ];
						// compute distance to all losses & wins
						var i,j;
						for (i = 0; i < 4; i++) {		// 4 board rotations
							for (j = 0; j < 8; j++) {	// times 8 wins per rotation
								// dumb-down
								var randsmart:Number = Math.random()*difficultyList[Game.difficulty];
								var doDefenseTwists:Boolean = randsmart > win[j][4];
								var doOffenseTwists:Boolean = randsmart + 1 > win[j][4];
								// dumb-down
								// make histographs of results
								lw[ 2*winDistance(win[j], blocked, marbles, doDefenseTwists) ]--;	// losses (opponent wins)
								lw[ 2*winDistance(win[j], marbles, blocked, doOffenseTwists) + 1 ]++;	// wins
							}
							// rotate board
							marbles.push(marbles.shift());
							blocked.push(blocked.shift());
						}
						if (lw[0]) lw[0] = -1;		// one can only lose once
						if (lw[1]) lw[1] = 1;		// one can only win once
						for ( i = 1; i < 6; i ++ ) {
							// accounting for minimal blocking moves...							
							if ( ( lw[2*i] += (i-1) ) > 0 ) lw[2*i] = 0;
							if ( ( lw[2*i+1] -= i ) < 0 ) lw[2*i+1] = 0;
						}
						lw[12] = -lw[12];		// reverse sense of blocked wins
						lw[13] = -lw[13];
						if ( best.count ) {		// compare this move to best
							for (i = 0; i < 14; i++) {
								if ( lw[i] != best.lw[i] ) break;
							}
							if ( i == 14 ) {		// same as best - randomly replace
								var oldcount = best.count;
								best.count++;
								trace("    count="+best.count+" dindex="+(9*q + d)+" tindex="+(2*tq + cw) );
								if (Math.random()*best.count > oldcount) {
									best.dindex = 9*q + d;
									best.tindex = 2*tq + cw;
								}
								continue;
							}
							if ( lw[i]<best.lw[i] ) continue;	// worse than best
						}
						// better (or first) move found -- update best
						best.dindex = 9*q + d;
						best.tindex = 2*tq + cw;
						best.count = 1;
						best.lw = lw;
						trace("    count="+best.count+" dindex="+best.dindex+" tindex="+best.tindex+" lw="+best.lw);
					}
					// restore quadrant to unrotated
					marbles[tq] = unrotMarbles;
					blocked[tq] = unrotBlocked;
				}
				// remove trial marble
				marbles[q] -= marble;
			}
		} else {	// done with last dimple
			clearInterval(Game.intervalId);
			Game.intervalId = 0;
			// trace("    count="+best.count+" dindex="+best.dindex+" tindex="+best.tindex+" l="+best.l+" w="+best.w);
			trace("    time = "+ (getTimer() - millis)/1000 );	// current first move:  ~14.6 sec
			Game.whatsnext();
			return;
		}
		Game.setHeader("Thinking - "+moveDimple,20,(Game.playhead&2)?0x000000:0xFFFFFF);
		Game.intervalId = setInterval(this, "iterPickMove", moveDimple?20:1000);
		return;
	}

}

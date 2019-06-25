class Game {		// everything static - There is no instance of Game

	static var BG_COLOR:Number = 0xB0E0A0;	// background color (of quadrants)
	static var LB_COLOR:Number = 0xBDE3AC;	// lighter background color (of options & btn)
	static var SD_COLOR:Number = 0x70A060;	// shadow color
	static var LN_COLOR:Number = 0x000000;	// line color
	static var TX_COLOR:Number = 0x506040;	// text color 
	static var MW_COLORS:Array = [0xFFFFFF, 0xE0E0E0];	// white marble colors
	static var MB_COLORS:Array = [0x303030, 0x101010];	// black marble colors
	static var FONT:String = "_sans";

	static var scene:MovieClip;			// the root clip for this application
	static var quadrant:Array = [];
	static var optionDialog:Options;
	static var header:TextField;
	static var controls:Controls;

	static var history:Array = [];
	static var playhead:Number = 0;				// note to self:
	static var situation:Situation;				//    see SharedObject for persistant storage
	static var stopped:Boolean = false;
	static var undoing:Boolean = false;	// undo past/through computer moves
	static var undone:Boolean = false;	// flag used to trim history
	static var busy:Boolean = false;	// prevent undo during put and twist (corrupt GUI)
	static var finished:Number = 0;		// 1:whiteWon, 2:blackWon, 4:Draw
	
	static var autoWhite:Object = { txt:"Computer plays White", val:false };
	static var autoBlack:Object = { txt:"Computer plays Black", val:false };
	static var fullControls:Object = { txt:"Use full controls", val:false };
	static var difficulty:Number = 3;
	
	static var computerMove:Object;
	static var intervalId:Number;
	
	static function init(mc:MovieClip):Void {
		scene = mc;
		scene._x = 270;
		scene._y = 270;

		header = scene.createTextField("header", scene.getNextHighestDepth(), -200, -254, 400, 60);
		header.type = "dynamic";
		header.selectable = false;

		controls = null;
		optionDialog = null;

		var i:Number;
		for (i = 0; i < 4; i++) {
			quadrant[i] = Utils.newMovieClip(Quadrant,scene);
			quadrant[i].init(i);
		}

		reset();
	}

	static function reset():Void {
		trace("Reset");
		if (intervalId) {
			clearInterval(intervalId);
			intervalId = 0;
		}
		history = [];
		playhead = 0;
		situation = new Situation();
		stopped = false;
		busy = false;
		undoing = false;
		undone = false;
		computerMove = null;
		Twister.hideAll();
		Dimple.resetAll();
		var i:Number;
		for (i = 0; i < 4; i++) {
			quadrant[i].zeroPosition();
		}
		if (!controls && !optionDialog) optionsPressed();
		else whatsnext();
	}
	
	static function optionsPressed():Void {
		if (busy) return;
		trace("options");
		if (intervalId) {
			clearInterval(intervalId);
			intervalId = 0;
			computerMove = null;
		}
		if ( controls ) {
			controls.removeMovieClip();
			controls = null;
		}
		Dimple.disable();
		Twister.hideAll();
		setHeader("", 20, 0x80FF40 );
		optionDialog = Utils.newMovieClip(Options,scene);
		optionDialog.init();	// dismissing options calls whatsnext
	}
	static function graphPress(pos:Number):Void {
		stopped = true;
		playhead = pos;
		situation.jump();
		trace("\ngraphPress:   jump "+playhead);
		situation.showBoard();
		whatsnext();
	}
	static function forwardPressed():Void {
		trace("forwardPressed");
		if (busy) return;	// whatever's busy will call whatsnext
		busy = true;
		var item:Number;
		var marble:Number;
		item = history[ playhead++ ];
		if (playhead & 1) {
			marble = playhead&2? -1: 1;
			Dimple.put( item, marble );
			situation.placeMarble( item, playhead&2, true );
			trace("  playing put");
			situation.showBoard();
		} else {
			quadrant[Math.floor(item/2)].twist(item%2);
			trace("  playing twist");
			situation.showBoard();
		}
	}
	static function backPressed():Void {
		trace("backPressed");
		if (busy) return;	// whatever's busy will call whatsnext
		busy = true;
		unplay();
	}
	static function recordPressed():Void {
		trace("recordPressed");
		if ( (playhead & 1) && ((playhead&2)? autoBlack.val : autoWhite.val) ) {
			playhead--; // back up to start of computer move
			situation.jump();
		}
		history.length = playhead;
		stopped = false;
		whatsnext();
	}
	static function playPressed():Void {
		trace("playPressed");
		stopped = false;
		whatsnext();
	}
	
	static function whatsnext() {
		trace("whatsnext");
		var item:Number;
		var marble:Number;
		// kill or disable all inputs
		if ( optionDialog ) {
			optionDialog.removeMovieClip();
			optionDialog = null;
		}
		if (!controls) {
			controls = Utils.newMovieClip(Controls,scene);
			controls.init();
		}
		if (intervalId) {
			trace("  killing timer");
			clearInterval(intervalId);
			intervalId = 0;
			computerMove = null;
		}
		Dimple.disable();
		Twister.hideAll();
		if ( undoing ) {
			unplay();
			return;
		}
		if ( undone ) {
			history.length = playhead;
			undone = false;
		}
		busy = true;
		finished = (situation.whiteWin()? 1:0) + (situation.blackWin()? 2:0);
		if ( playhead == 72 ) {
			finished += 4;
		}
		if ( computerMove ) {
			history.push( computerMove.dindex );
			trace("  pushing computer dindex = "+computerMove.dindex);
			if ( computerMove.count ) {
				history.push( computerMove.tindex );
				trace("  pushing computer tindex = "+computerMove.tindex);
				// otherwise win with no twist - don't push
			}
			computerMove = null;
		}
		trace("  history = "+history);
		trace("  playhead = "+playhead+"  length = "+history.length);
		if (fullControls.val) {
			controls.graph.paint();
			if (stopped) {
				controls.btnForward._visible = true;
				controls.btnForward.enable(playhead < history.length);
				controls.btnBack._visible = true;
				controls.btnBack.enable(playhead > 0);
				controls.btnRecord._visible = true;
				controls.btnPlay._visible = true;
			} else {
				controls.btnForward._visible = false;
				controls.btnBack._visible = false;
				controls.btnRecord._visible = false;
				controls.btnPlay._visible = false;
			}
		} else {
			stopped = false;
		}
		showTurn();
		if (finished || stopped) {
			trace("  stopped or finished.  finished = "+finished);
			// waiting for control press
			busy = false;
			return;
		}
		// playing existing or recording new moves
		if (playhead & 1) {
			// waiting twist
			//  note: computer's twist choice will already have been pushed into history
			if ( playhead < history.length ) {
				trace("  playing twist");
				item = history[ playhead++ ];
				quadrant[Math.floor(item/2)].twist(item%2);
				situation.showBoard();
			} else {
				// wait for player's twist
				trace("  waiting twist");
				Twister.showAll();
				busy = false;
			}
		} else {
			// waiting marble
			if ( playhead < history.length ) {
				trace("  playing put");
				item = history[ playhead++ ];
				marble = playhead&2? -1: 1;
				Dimple.put( item, marble );
				situation.placeMarble( item, playhead&2, true );
				situation.showBoard();
			} else if ( (playhead&2)? autoBlack.val : autoWhite.val ) {
				trace("  computing move:");
				busy = false;	// allow undo
				computerMove = {};
				situation.pickMove(computerMove);
			} else {
				trace("  waiting put");
				Dimple.enable();
				busy = false;
			}
		}
	}
	
	static function setHeader(txt:String,size:Number,color:Number):Void {
		var fmt:TextFormat = new TextFormat();
		fmt.color = color;
		fmt.align = "center";
		fmt.font = "Arial";
		fmt.size = size;
		header.text = txt;
		header.setTextFormat(fmt);
	}

	static function twist(tindex:Number):Void {
		busy = true;		// unset in checkDone()
		quadrant[Math.floor(tindex/2)].twist(tindex%2);
	}

	static function showTurn():Void {
		var mt:String;
		if ( finished ) {
			if ( finished == 1 ) {
				mt = "WHITE WINS!";
			} else if ( finished == 2 ) {
				mt = "BLACK WINS!";
			} else {
				mt = "DRAW!";
			}
			setHeader(mt, 48, 0x80FF40 );
		} else {
			mt = (playhead & 1)? "turn" : "move" ;
			if ( playhead & 2 ) {
				mt = "Black's "+mt;
				setHeader(mt,20,0x000000);
			} else {
				mt = "White's "+mt;
				setHeader(mt,20,0xFFFFFF);
			}
		}
		trace("  "+mt);
	}

	static function undo():Void {
		if ( !busy ) {
			undone = true;
			unplay();
		}
	}
	static function unplay():Void {
		trace("unplay "+playhead);
		if (intervalId) {	// interrupted computer thinking
			clearInterval(intervalId);
			computerMove = null;
		}
		if (!playhead) {
			undoing = false;
			whatsnext();
			return;
		}
		busy = true;
		var item:Number = 0+history[--playhead];
		if ( playhead & 1 ) {
			// previous action was rotate -- item is index into twister[]
			Dimple.disable();
			quadrant[Math.floor(item/2)].twist(!(item%2));	// unsets busy in whatsnext()
			trace("  undoing twist");
			situation.showBoard();
		} else {
			// previous action was place marble -- item is index into dimple[]
			Twister.hideAll()
			Dimple.put(item, 0);							 // unsets busy in whatsnext()
			situation.placeMarble(item, playhead&2, false);
			trace("  undoing put");
			situation.showBoard();
		}
		if ( !stopped ) {
			setHeader("UNDOING", 20, 0x80FF40 );
			if ( autoBlack.val && autoWhite.val ) {
				// continue undoing until next move is a put
				undoing = ( (playhead & 1) != 0 );
			} else {
				// continue undoing until a non-computer move has been undone
				undoing = (playhead&2)? autoBlack.val : autoWhite.val ;
			}
		}
	trace("  undoing = "+undoing);
	}

}
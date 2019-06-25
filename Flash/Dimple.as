class Dimple extends MovieClip {
	
	static var dimple:Array = [];
	static var active:Boolean;

	var marble:Number;		// 0:none, >0:white, <0:black                     / 0  1  2
	var index:Number;		// index of the dimple (0-8) in the quadrant  <--(  7  8  3
							//   times 9 times the quadrant index             \ 6  5  4
	//                     vvvvvvvvvvvvvvvvvvvvvvvvvvvv
	//  for each quadrant, WHEN quadrant._rotation == 0, dimple[0] is at upper left
	//                     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
	var icount:Number;		// iteration counter
	var old:Number;

	// dimple positions relative to center of quadrant
	static var xmap:Array = [ -60, 0, 60, 60, 60, 0, -60, -60, 0 ];
	static var ymap:Array = [ -60, -60, -60, 0, 60, 60, 60, 0, 0 ];

	public function Dimple() {
		// moved drawing into init
	}

	public function init():Void {
		index = dimple.length;
		dimple.push(this);
		marble = 0;
		paint(0);
		_x = xmap[index%9];
		_y = ymap[index%9];
	}

	public static function resetAll():Void {
		var d:Number = dimple.length;
		while ( d-- ) {
			reset(d);
		}
	}
	public static function reset( i:Number ):Void {
		dimple[i].marble = 0;
		dimple[i].paint(0);
	}

	public static function enable():Void {
		active = true;
		var d:Number = dimple.length;
		while ( d-- ) {
			if (!dimple[d].marble) dimple[d].useHandCursor = true;
		}
	}
	public static function disable():Void {
		active = false;
		var d:Number = dimple.length;
		while ( d-- ) {
			dimple[d].useHandCursor = false;
		}
	}
	/*
	public static function destroyAll():Void {
		var d:Number = dimple.length;
		while ( d-- ) {
			dimple[d].removeMovieClip();
		}
		dimple = [];
	}
	*/

	public static function occupied(i:Number):Boolean {
		return dimple[i].marble ? true : false;
	}

	public function onPress():Void {
		if ( !active ) return;
		disable();
		Game.history.push(index);
		trace("Dimple.onPress: history.push "+index);
		Game.whatsnext();
	}
	
	static function put(i:Number,m:Number):Void {
		dimple[i].set(m);
	}
	function set(m:Number):Void {
		old = marble;
		marble = m;
		icount = 7;
		Game.intervalId = setInterval(this,"flashmarble", 100, old);
	}
	function flashmarble(old:Number):Void {
		if (icount--) {
			paint( icount? ((icount&1)?(old?old:-marble):(marble?marble:-old)): marble );
		} else {
			clearInterval(Game.intervalId);
			Game.intervalId = 0;
			Game.whatsnext();
		}
	}

	function paint(mbl:Number):Void {
        var fillType = "radial"
		var colors;
		var alphas = [100,100];
		var ratios = [0x20, 0xFF];
    	var matrix = {matrixType:"box", x:-22, y:-22, w:44, h:44, r:0};
		var spreadMethod = "pad";
		var interpolationMethod = "RGB";
		var focalPointRatio = 0;
		var size:Number = 22;
		if ( !mbl ) {
			// first erase any existing (and larger) marble
			beginFill(Game.BG_COLOR,100);
			Utils.drawCircle(this,0,0,size);
			endFill();
			size = 20;				// for dimple
			colors = [Game.BG_COLOR, Game.SD_COLOR];
			ratios = [0x40, 0xFF];
		} else {					// for marble
			ratios = [0x20, 0xFF];
			if (mbl > 0) {
				colors = Game.MW_COLORS;
			} else {
				colors = Game.MB_COLORS;
			}
		}
		beginGradientFill(fillType, colors, alphas, ratios, matrix,
			spreadMethod, interpolationMethod, focalPointRatio);
		Utils.drawCircle(this,0,0,size);
		endFill();
	}

}

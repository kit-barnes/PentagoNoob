class Quadrant extends MovieClip {

	static var HALFSIZE:Number = 90;

	var index:Number;		// index of this quadrant:		0 1
							//								3 2
	var twistCW:Boolean;
	var icount:Number;		// iteration counter
	var size:Number;		// save initial size during mobile twist
//	var deltaX:Number;
//	var deltaY:Number;

	function Quadrant(mc:MovieClip) {
		mc.lineStyle(1,Game.LN_COLOR);
		mc.beginFill(Game.BG_COLOR,100);
		mc.moveTo(HALFSIZE,HALFSIZE);
		mc.lineTo(HALFSIZE,-HALFSIZE);
		mc.lineTo(-HALFSIZE,-HALFSIZE);
		mc.lineTo(-HALFSIZE,HALFSIZE);
		mc.lineTo(HALFSIZE,HALFSIZE);
		mc.endFill();
		mc.lineStyle();
	}
	function init( i:Number ):Void {
		index = i;
		zeroPosition();
//		deltaX = _x/18;
//		deltaY = _y/18;
		for (var j:Number = 0; j < 9; j++) {
			Utils.newMovieClip(Dimple,this).init(9*i+j);
		}
	}
	function zeroPosition():Void {
		_x = HALFSIZE * ( index==1||index==2 ? 1 : -1 );
		_y = HALFSIZE * ( index>1 ? 1 : -1 );
		_rotation = 0;
	}

	function twist( clockwise:Boolean ):Void {
		// disable all twists
		Twister.hideAll();
		// start to extend the quadrant
		twistCW = clockwise;
		if (Twister.useMobileTwist) {
			icount = 18;
			Game.intervalId = setInterval(this, "twistInPlace", 60);
			size = _width;
		} else {
//			icount = 9;
//			Game.intervalId = setInterval(this, "extend", 40);
			icount = 18;
			Game.intervalId = setInterval(this, "slidingTwist", 60);
		}
		Game.situation.twist(index,clockwise);
	}

/*
	function extend():Void {
		if (icount--) {
			_x += deltaX;
			_y += deltaY;
		} else {
			clearInterval(Game.intervalId);
			icount = 9;
			Game.intervalId = setInterval(this, "dotwist", 80);
		}
	}
	function dotwist():Void {
		if (icount--) {
			_rotation += twistCW ? 10 : -10 ;
		} else {
			clearInterval(Game.intervalId);
			icount = 9;
			Game.intervalId = setInterval(this, "retract", 80);
		}
	}
	function retract():Void {
		if (icount--) {
			_x -= deltaX;
			_y -= deltaY;
		} else {
			clearInterval(Game.intervalId);
			Game.intervalId = 0;
			Game.whatsnext();
		}
	}
*/
	
	function twistInPlace() {
		if (icount--) {
			_rotation += twistCW ? 5 : -5 ;
			var theta = (45 - (_rotation+360)%90);
			_xscale = 100/( Math.cos( theta * Math.PI/180 ) * Math.SQRT2 );
			_yscale = _xscale;
		} else {
			clearInterval(Game.intervalId);
			Game.intervalId = 0;
			_xscale = 100;
			_yscale = 100;
			Game.whatsnext();
		}
	}
	
	function slidingTwist() {
		if (icount--) {
			_rotation += twistCW ? 5 : -5 ;
			var theta = (45 - (_rotation+360)%90);
			var factor = Math.cos( theta * Math.PI/180 ) * Math.SQRT2;
			_x = factor * HALFSIZE * ( index==1||index==2 ? 1 : -1 );
			_y = factor * HALFSIZE * ( index>1 ? 1 : -1 );
		} else {
			clearInterval(Game.intervalId);
			Game.intervalId = 0;
			_x = HALFSIZE * ( index==1||index==2 ? 1 : -1 );
			_y = HALFSIZE * ( index>1 ? 1 : -1 );
			Game.whatsnext();
		}
	}		

}
class Graph extends MovieClip {

	static var HEIGHT:Number = 18;
	static var w:Number = 3*72+HEIGHT+4;
	
	var dragging:Boolean;
	var oldpos:Number;

	function Btn(mc:MovieClip) {
	}

	function init( x:Number, y:Number ):Void {
		_x = x - (3*72)/2;
		_y = y;
		paint();
	}
	
	function onPress():Void {
		dragging = true;
		oldpos = -1;
		drag();
	}
	function drag():Void {
		var pos:Number = _xmouse;
		pos = Math.floor( pos/3 );
		if (pos < 0) pos = 0;
		else if (pos > Game.history.length) pos = Game.history.length;
		if (pos != oldpos) {
			Game.graphPress(pos);
			oldpos = pos;
		}
	}
	function onMouseMove():Void {
		if (dragging) drag();
	}
	function onRelease():Void {
		dragging = false;
	}
	function onReleaseOutside():Void {
		dragging = false;
	}

	
	function paint():Void {
		var i:Number;
		var end:Number;
		lineStyle(0,Game.LN_COLOR,100, false, "none");
        var fillType = "linear"
		var colors = [Game.TX_COLOR, Game.LB_COLOR];
		var alphas = [100,100];
		var ratios = [0x0, 0x50];
		var spreadMethod = "reflect";
		var interpolationMethod = "RGB";
		var focalPointRatio = 0;
    	var matrix = {matrixType:"box", x:0, y:-HEIGHT/2, w:w+20, h:HEIGHT/2, r:Math.PI/2};
		beginGradientFill(fillType, colors, alphas, ratios, matrix,
			spreadMethod, interpolationMethod, focalPointRatio);
		Utils.drawRectangle(this,-(HEIGHT+4)/2,-HEIGHT/2,w,HEIGHT,HEIGHT/2);
		endFill();
		lineStyle(7,Game.SD_COLOR,100, false, "normal", "none");
		moveTo(-1,0);
		lineTo(72*3+1,0);
		moveTo(0,0);
		i = 0;
		while ( i < Game.history.length) {
			lineStyle(5, ((i++)&2)?0:0xFFFFFF, 100, false, "normal", "none");
			lineTo( i*3, 0 );
		}
		lineStyle();
		beginFill(Game.TX_COLOR,100);
		i = 3*Game.playhead;
		moveTo( i, 2 );
		lineTo( i+4, 8 );
		lineTo( i-4, 8 );
		lineTo( i, 2 );
		moveTo( i, -2 );
		lineTo( i+4, -8 );
		lineTo( i-4, -8 );
		lineTo( i, -2 );
		endFill();
	}
}
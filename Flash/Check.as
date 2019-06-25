class Check extends MovieClip {
	
	function Checkbox(mc:MovieClip) {
	}
	
	function init():Void {
		lineStyle()
		beginFill( Game.LN_COLOR, 80 );
		moveTo (  3,  -4 );
		lineTo (  7,  16 );
		lineTo ( 12,  16 );
		curveTo( 30, -16,   50, -16	);
		curveTo( 22, -16,   11,   8 );
		lineTo (  9,  -4 );
		lineTo (  3,  -4 );
		endFill();
	}
	
}
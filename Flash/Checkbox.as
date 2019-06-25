class Checkbox extends MovieClip {
	
	static var FONT_SZ:Number = 20;

	var vrbl:Object;
	var display:TextField;
	var checkmark:Check;
	
	function Checkbox(mc:MovieClip) {
	}
	
	function init( x:Number, y:Number, vbl:Object ) {
		
		_x = x;
		_y = y;
		vrbl = vbl;
		display = createTextField("display", getNextHighestDepth(), 34, -2, 300, 28);
		display.type = "static";
		display.selectable = false;
		var fmt:TextFormat = new TextFormat();
		fmt.color = Game.TX_COLOR;
		fmt.align = "left";
		fmt.font = Game.FONT;
		fmt.size = FONT_SZ;
		display.text = vrbl.txt;
		display.setTextFormat(fmt);
		// draw filled box
		lineStyle(2,Game.TX_COLOR);
		beginFill(Game.LB_COLOR,100);
		Utils.drawRectangle(this,0,0,22,22,0);
		endFill();
		checkmark = Utils.newMovieClip(Check,this);
		checkmark.init();
		checkmark._visible = vrbl.val;
	}
	
	function onMouseMove():Void {
		useHandCursor = _xmouse<22 && _ymouse>=0 ;
	}
	
	function onPress():Void {
		if (useHandCursor) {
			vrbl.val = !vrbl.val;
			checkmark._visible = vrbl.val;
		}
	}
	
	function clear():Void {
		vrbl.val = false;
		checkmark._visible = false;
	}
	
}
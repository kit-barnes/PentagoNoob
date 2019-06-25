class Btn extends MovieClip {
	
	static var FONT_SZ:Number = 16;
	static var HEIGHT:Number = 22;
	
	var btext:TextField;
	var fnOnPress:Function;

	function Btn(mc:MovieClip) {
	}
	
	function init( x:Number, y:Number, w:Number, border:Boolean, txt:String, fn:Function ) {
		
		_x = x - HEIGHT/2;
		_y = y;
		if (border) {
			lineStyle(1,Game.LN_COLOR);
			// beginFill(Game.BG_COLOR,100);
			var fillType = "linear"
			var colors = [Game.TX_COLOR, Game.LB_COLOR];
			var alphas = [100,100];
			var ratios = [0x0, 0x50];
			var spreadMethod = "reflect";
			var interpolationMethod = "RGB";
			var focalPointRatio = 0;
			var matrix = {matrixType:"box", x:-10, y:0, w:w+20, h:HEIGHT/2, r:Math.PI/2};
			beginGradientFill(fillType, colors, alphas, ratios, matrix, 
				spreadMethod, interpolationMethod, focalPointRatio);
			Utils.drawRectangle(this,0,0,w,HEIGHT,HEIGHT/2);
			endFill();
		}
		btext = createTextField("btext", getNextHighestDepth(), 0, 0, w, HEIGHT);
		btext.type = "dynamic";
		btext.selectable = false;
		btext.text = txt;
		btext.antiAliasType = "advanced";
		var fmt:TextFormat = new TextFormat();
		fmt.align = "center";
		fmt.font = Game.FONT;
		fmt.size = FONT_SZ;
		fmt.bold = true;
		btext.setTextFormat(fmt);
		fnOnPress = fn;
		enable(true);
	}
	
	function enable( s:Boolean ):Void {
		if (s) {
			btext.textColor = Game.TX_COLOR;
			onPress = fnOnPress;
		} else {
			btext.textColor = Game.SD_COLOR;
			delete(onPress);
		}
	}
}
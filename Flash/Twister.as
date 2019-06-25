class Twister extends MovieClip{
	
	static var useMobileTwist:Boolean = false;
	
	static var twister:Array;
	static var coord:Array;
	
	var index:Number;

	public static function showAll():Void {
		hideAll();	// just in case
		coord = useMobileTwist? [ -127,-121,121,127,127,121,-121,-127 ] :
								[ -220,-160,160,220,220,160,-160,-220 ];
								// x-positions (and rotated y-pos) of twisters
		var newMenu:ContextMenu = new ContextMenu();
		newMenu.hideBuiltInItems();
		var item:ContextMenuItem = ( useMobileTwist?
			new ContextMenuItem("Use Standard Twist",
								function(){useMobileTwist=false; showAll();}) :
			new ContextMenuItem("Use Mobile Twist",
								function(){useMobileTwist=true; showAll();})
			);
		newMenu.customItems.push(item);
		var i:Number;
		for (i = 0; i < 8; i++) {
			twister[i] = Utils.newMovieClip(Twister,Game.scene);
			twister[i].menu = newMenu;
			if (useMobileTwist) {
				twister[i].mobileInit(i);
			} else {
				twister[i].init(i);
			}
		}
	}
	
	public static function hideAll():Void {
		var i:Number;
		for (i in twister) {
			twister[i].removeMovieClip();
		}
		twister = [];
	}
	
	public function Twister(mc:MovieClip) {
	}
	
	public function init(i:Number):Void {
		lineStyle(1,Game.LN_COLOR);
        var fillType = "radial"
		var colors = [Game.BG_COLOR, Game.SD_COLOR];
		var alphas = [100,100];
		var ratios = [0xD0, 0xFF];
		var spreadMethod = "pad";
		var interpolationMethod = "RGB";
		var focalPointRatio = 0;
    	var matrix = {matrixType:"box", x:-30, y:-30, w:60, h:60, r:0};
		beginGradientFill(fillType, colors, alphas, ratios, matrix, 
			spreadMethod, interpolationMethod, focalPointRatio);
		Utils.drawCircle(this,0,0,30);
		lineStyle(6,Game.TX_COLOR,100,false,"normal","round","miter",4);
		beginFill(Game.BG_COLOR,100);
		Utils.drawCircle(this,0,0,18);
		endFill();
		index = i;
		_x = coord[i];
		_y = coord[(i+6)%8];
		if (i%2) {
			moveTo(-15,12);
			lineTo(-13,17);
			lineTo(-9,12);
			lineTo(-15,12);
			lineStyle(8,Game.BG_COLOR);
			moveTo(-10,5);
			lineTo(-19,5);
		} else {
			moveTo(12,-15);
			lineTo(17,-13);
			lineTo(12,-9);
			lineTo(12,-15);
			lineStyle(8,Game.BG_COLOR);
			moveTo(5,-10);
			lineTo(5,-19);
		}
		_rotation = 90 * Math.floor(index/2);
	}
	
	public function mobileInit(i:Number):Void {
		index = i ^ 1;
		_x = coord[i];
		_y = coord[(i+6)%8];
		_alpha = 30;
		lineStyle(0,Game.SD_COLOR);
		beginFill(0xFFFFFF,60);
		moveTo(-4,-4);
		if (i%2) {
			lineTo(11,-37);
			lineTo(11,-23);
//			lineTo(50,-55);	//faked
//			lineTo(93,-22);
			curveTo(50,-50,93,-22);
//			lineTo(136,11); // faked
//			lineTo(93,90);	// tail point
			curveTo(136,11,93,90);
			lineTo(91,38);	// tail notch
			lineTo(57,54);	// tail point
//			lineTo(86,21); // faked
//			lineTo(69,2);
			curveTo(86,21,69,2);
//			lineTo(52,-17); // faked
//			lineTo(22,-6);
			curveTo(52,-17,22,-6);
			lineTo(30,0);
		} else {
			lineTo(-37,11);
			lineTo(-23,11);
			curveTo(-50,50,-22,93);
			curveTo(11,136,90,93);
			lineTo(38,91);	// tail notch
			lineTo(54,57);	// tail point
			curveTo(21,86,2,69);
			curveTo(-17,52,-6,22);
			lineTo(0,30);
		}
		lineTo(-4,-4);
		endFill();
		_rotation = 90 * Math.floor(index/2);
	}
	
	public function onPress():Void {
		trace("Twister.onPress: history.push "+index);
		Game.history.push(index);
		Game.whatsnext();
	}
	
	public function onRollOver():Void {
		if (useMobileTwist) _alpha = 100;
	}
	public function onRollOut():Void {
		if (useMobileTwist) _alpha = 50;
	}

	
}

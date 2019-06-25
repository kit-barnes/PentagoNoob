class Utils {
	
	/**
	Prototypal Inheritance per Douglas Crawford
	*/
	static function object( o:Object ):Object {
		function F(){}
		F.prototype = o;
		return new F();
	}

	/**
	Creates a new MovieClip as an instance of the specified class.
	The constructor is called, and should extend MovieClip.
	@constructor_ constructor for the class
	@parent_ if null _root is used
	@depth_ if null nextHighestDepth is used
	*/
	static function newMovieClip(constructor_:Function, parent_:MovieClip, depth_:Number) {
		if (!parent_) parent_ = _root;
		if (!depth_) depth_ = parent_.getNextHighestDepth();
		var mc:MovieClip = parent_.createEmptyMovieClip("mc_"+depth_, depth_);
		var inst:Object = new constructor_(mc);
		for (var i in inst) mc[i] = inst[i];
		mc.constructor = constructor_;
		mc.__proto__ = inst.__proto__;
		return mc;
	}
	
	static function drawCircle(mc:MovieClip, x:Number, y:Number, r:Number):Void {
		mc.moveTo(x+r, y);
		mc.curveTo(r+x, Math.tan(Math.PI/8)*r+y, Math.sin(Math.PI/4)*r+x, Math.sin(Math.PI/4)*r+y);
		mc.curveTo(Math.tan(Math.PI/8)*r+x, r+y, x, r+y);
		mc.curveTo(-Math.tan(Math.PI/8)*r+x, r+y, -Math.sin(Math.PI/4)*r+x, Math.sin(Math.PI/4)*r+y);
		mc.curveTo(-r+x, Math.tan(Math.PI/8)*r+y, -r+x, y);
		mc.curveTo(-r+x, -Math.tan(Math.PI/8)*r+y, -Math.sin(Math.PI/4)*r+x, -Math.sin(Math.PI/4)*r+y);
		mc.curveTo(-Math.tan(Math.PI/8)*r+x, -r+y, x, -r+y);
		mc.curveTo(Math.tan(Math.PI/8)*r+x, -r+y, Math.sin(Math.PI/4)*r+x, -Math.sin(Math.PI/4)*r+y);
		mc.curveTo(r+x, -Math.tan(Math.PI/8)*r+y, r+x, y);
	}
	
	static function drawRectangle
		(mc:MovieClip, x:Number, y:Number, w:Number, h:Number, r:Number):Void {
		// r is radius of rounded corners
        mc.moveTo(x + r, y);
        mc.lineTo(x + w - r, y);
        mc.curveTo(x + w, y, x + w, y + r);
        mc.lineTo(x + w, y + h - r);
        mc.curveTo(x + w, y + h, x + w - r, y + h);
        mc.lineTo(x + r, y + h);
        mc.curveTo(x, y + h, x, y + h - r);
        mc.lineTo(x, y + r);
        mc.curveTo(x, y, x + r, y);
    }
	
}

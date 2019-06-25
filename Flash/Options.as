class Options extends MovieClip {

	static var WIDTH = 320;
	static var HEIGHT = 250;
	static var ALPHA = 85;
	
	var title:TextField;
	
	var ckbAutoBlack:Checkbox;
	var ckbAutoWhite:Checkbox;
	var ckbFullControls:Checkbox;
	
	var test;
	
	var difficultyText:TextField;
	var difficultyUp:Btn;
	var difficultyDown:Btn;
	
	var btnReset:Btn;
	var btnOK:Btn;

	function Options(mc:MovieClip) {
	}
	function init():Void {
		_x = -WIDTH/2;
		_y = -HEIGHT/2;
		_alpha = ALPHA
		lineStyle(2,Game.LN_COLOR);
		beginFill(Game.LB_COLOR,100);
		Utils.drawRectangle(this,0,0,WIDTH,HEIGHT,24);
		endFill();
		lineStyle();
		title = createTextField("display", getNextHighestDepth(), 0, 4, WIDTH, 30);
		title.type = "static";
		title.selectable = false;
		title.text = "OPTIONS";
		title.antiAliasType = "advanced";
		var fmt:TextFormat = new TextFormat();
		fmt.color = Game.TX_COLOR;
		fmt.align = "center";
		fmt.font = Game.FONT;
		fmt.size = 24;
		fmt.bold = true;
		fmt.italic = true;
		title.setTextFormat(fmt);
		ckbAutoBlack = Utils.newMovieClip(Checkbox,this);
		ckbAutoBlack.init(40, 44, Game.autoBlack);
		ckbAutoWhite = Utils.newMovieClip(Checkbox,this);
		ckbAutoWhite.init(40, 84, Game.autoWhite);
		ckbFullControls = Utils.newMovieClip(Checkbox,this);
		ckbFullControls.init(40, 124, Game.fullControls);

		difficultyText = createTextField("display", getNextHighestDepth(), 56, 166, 130, 26);
		difficultyText.type = "dynamic";
		difficultyText.selectable = false;
		difficultyText.antiAliasType = "advanced";
		updateDifficultyText();
		difficultyUp = Utils.newMovieClip(Btn,this);
		difficultyUp.init(196,157,22,false,"\u25B2",harder);
		difficultyDown = Utils.newMovieClip(Btn,this);
		difficultyDown.init(196,179,22,false,"\u25BC",easier);
		
		btnReset = Utils.newMovieClip(Btn,this);
		btnReset.init(50,HEIGHT-40,80,true,"RESET",Game.reset);
		btnOK = Utils.newMovieClip(Btn,this);
		btnOK.init(WIDTH-90,HEIGHT-40,60,true,"OK", dismiss);
	}
	
	function updateDifficultyText():Void {
		difficultyText.text = "Difficulty:  "+Game.difficulty;
		var fmt:TextFormat = new TextFormat();
		fmt.align = "right";
		fmt.color = Game.TX_COLOR;
		fmt.font = Game.FONT;
		fmt.size = 20;
		difficultyText.setTextFormat(fmt);
	}
	function harder():Void {
		Game.difficulty++;
		_parent.updateDifficultyText();
		_parent.difficultyUp.enable(Game.difficulty < 7)
		_parent.difficultyDown.enable(true);
	}
	function easier():Void {
		Game.difficulty--;
		_parent.updateDifficultyText();
		_parent.difficultyDown.enable(Game.difficulty > 1);			// range is 1 to 7 dictated by difficultyList
		_parent.difficultyUp.enable(true);
	}

	function dismiss():Void {
		Game.whatsnext();
	}

}
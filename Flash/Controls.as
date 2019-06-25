class Controls extends MovieClip {

	var btnOptions:Btn;
	
	var btnUndo:Btn;
	
	var graph:Graph;
	var btnForward:Btn;
	var btnBack:Btn;
	var btnRecord:Btn;
	var btnPlay:Btn;

	public function Options(mc:MovieClip) {
	}
	public function init():Void {
		btnOptions = Utils.newMovieClip(Btn,this);
		if ( Game.fullControls.val ) {
			btnOptions.init(-34,230,88,true,"OPTIONS",Game.optionsPressed);
			graph = Utils.newMovieClip(Graph,this);
			graph.init( 0, 211 );
			btnForward = Utils.newMovieClip(Btn,this);
			btnForward.init(140,200,22,true,"\u25BA",Game.forwardPressed);
			btnBack = Utils.newMovieClip(Btn,this);
			btnBack.init(-140,200,22,true,"\u25C4",Game.backPressed);
			btnRecord = Utils.newMovieClip(Btn,this);
			btnRecord.init(-76,230,30,true,"\u25CF",Game.recordPressed);
			btnRecord.btext.textColor = 0xFF0000;
			btnPlay = Utils.newMovieClip(Btn,this);
			btnPlay.init(66,230,30,true,"\u25BA",Game.playPressed);
		} else {
			btnOptions.init(-74,230,88,true,"OPTIONS",Game.optionsPressed);
			btnUndo = Utils.newMovieClip(Btn,this);
			btnUndo.init(30,230,64,true,"UNDO",Game.undo);
		}
	}

}
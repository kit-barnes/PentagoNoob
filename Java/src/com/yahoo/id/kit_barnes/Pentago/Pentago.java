package com.yahoo.id.kit_barnes.Pentago;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Pentago extends JApplet implements ChangeListener, ActionListener {

 	private static final long serialVersionUID = -7030403852076282117L;
	private boolean standalone = false;
	private Game game;
	private Board board;
	private JDialog optionsDialog;
	private boolean optionsShown;
	private JCheckBox computerBlackButton;
	private JCheckBox computerWhiteButton;


	// methods required for JApplet
	public void init() {
		
		// build the options dialog
		optionsDialog = new JDialog();
		optionsDialog.setResizable(false);
		if (standalone) optionsDialog.setAlwaysOnTop(true);
		optionsDialog.setTitle("Pentago Options");
		Container c = optionsDialog.getContentPane();
		c.setLayout(new GridLayout(0,2));

//        String[] zoomFactors =
//        	{"1.1","1.2","1.4","2.0","4.0","8.0","16"};
//		SpinnerListModel zoomModel = new SpinnerListModel(zoomFactors);
//		zoomSpinner = new JSpinner(zoomModel);
//		JSpinner.DefaultEditor zeditor = new JSpinner.DefaultEditor(zoomSpinner);
//		zeditor.getTextField().setColumns(2);
//		zeditor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
//		zoomSpinner.setEditor(zeditor);
//		zoomSpinner.setValue(zoomFactors[3]);
//		zoomSpinner.addChangeListener(this);
//		c.add(new JLabel("  ZoomFactor:"));
//		c.add(zoomSpinner);
//
//        String[] iterationLimits =
//        	{"64","128","256","512","1024","2048","4096","8192","16384"};
//		SpinnerListModel iterModel = new SpinnerListModel(iterationLimits);
//		iterSpinner = new JSpinner(iterModel);
//		JSpinner.DefaultEditor editor = new JSpinner.DefaultEditor(iterSpinner);
//		editor.getTextField().setColumns(3);
//		editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
//		iterSpinner.setEditor(editor);
//		iterSpinner.setValue(iterationLimits[2]);
//		iterSpinner.addChangeListener(this);
//		c.add(new JLabel("  Maximum Iterations:  "));
//		c.add(iterSpinner);
//
//        String[] shadingstyles =
//        	{"Zebra","Grayscale","RGB"};
//		SpinnerListModel shadeModel = new SpinnerListModel(shadingstyles);
//		shadeSpinner = new JSpinner(shadeModel);
//		shadeSpinner.setValue(shadingstyles[2]);
//		shadeSpinner.addChangeListener(this);
//		c.add(new JLabel("  Shading:"));
//		c.add(shadeSpinner);

		computerBlackButton = new JCheckBox("Computer plays black");
		computerBlackButton.addChangeListener(this);
		// computerBlackButton.setEnabled(false);
		c.add(computerBlackButton);

		computerWhiteButton = new JCheckBox("Computer plays white");
		computerWhiteButton.addChangeListener(this);
		// computerWhiteButton.setEnabled(false);
		c.add(computerWhiteButton);


		optionsDialog.pack();

		
		game = new Game();
		c = getContentPane();
        c.add(board = new Board(game),BorderLayout.CENTER);
        game.setBoard(board);
        

        JPanel panel = new JPanel();
		JButton restart = new JButton("Reset");
		restart.addActionListener(this);
		panel.add(restart);

		JButton options = new JButton("Options");
		options.addActionListener(this);
		panel.add(options);

		JButton help = new JButton("Help");
		help.addActionListener(this);
		panel.add(help);
		c.add(panel,BorderLayout.NORTH);
	}
	public void start() {
		game.start();
	}
	public void stop() {}
	public void destroy() {}
	


    // method required by the ChangeListener interface.
	public void stateChanged(ChangeEvent e) {
/*
		if (e.getSource() == zoomSpinner) {
			display.setZoom(Double.parseDouble((String)zoomSpinner.getValue()));
		}
		if (e.getSource() == iterSpinner) {
			display.setDepth(Integer.parseInt((String)iterSpinner.getValue()));
		}
		if (e.getSource() == shadeSpinner) {
			display.setShading((String)shadeSpinner.getValue());
		}
		if (e.getSource() == cycleButton) {
			display.setCycling(cycleButton.isSelected());
			outcycleButton.setEnabled(cycleButton.isSelected());
		}
		if (e.getSource() == outcycleButton) {
			if (outcycleButton.isEnabled())
				display.setOutCycling(outcycleButton.isSelected());
		}
*/
		if (e.getSource() == computerWhiteButton) {
			game.setComputerWhite(computerWhiteButton.isSelected());
		}
		if (e.getSource() == computerBlackButton) {
			game.setComputerBlack(computerBlackButton.isSelected());
		}
	}


    // method required by the ActionListener interface.
	public void actionPerformed(ActionEvent e) {
		if ("Help".equals(e.getActionCommand())) {
			JOptionPane.showMessageDialog(this,
				"Pentago\n\n"
				+"      No help yet\n"
				+"      "
				+"      \n\n"
				+"\n    "
				+"\n\n©2009 William C Barnes                kit_barnes@yahoo.com",
				"Pentago help",
				JOptionPane.PLAIN_MESSAGE);
		}

		if ("Options".equals(e.getActionCommand())) {
			if (optionsShown == false) {
				optionsDialog.setLocationRelativeTo(this);
				optionsShown = true;
			}
			optionsDialog.setVisible(true);
		}

		if ("Reset".equals(e.getActionCommand())) {
			game.start();
		}
	}

	
	// methods required for stand-alone application (non-applet)
	public static void main(final String[] args) {
		
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame.setDefaultLookAndFeelDecorated(false);
                //Create and set up the window.
                JFrame frame = new JFrame("Pentago");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                Container container = frame.getContentPane();
                //container.setLayout(new BorderLayout());
                Pentago p = new Pentago();
                p.setstandalone(true);
                p.setArgs(args);
                p.init();
                container.add(p);
                frame.pack();
                p.start();
                frame.setVisible(true);
           }
        });
	}
	private void setArgs(String[] args) {
	}
	private void setstandalone(boolean b) {
		standalone = true;
	}

}

/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: Dialog_WaitingWithProgressBar.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2024 Comsystan Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package at.csa.csaj.commons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.scijava.log.LogService;


/**
 * 
 * @author HA
 * @date 2020-08-25
 *
 */

public class Dialog_WaitingWithProgressBar extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1252594863674350961L;
	
	JLabel       lblMessage;
	JLabel       lblPercent;
	JProgressBar pbar;
	JButton      btnCancel;
	JPanel       panel;
	
	static final int MINIMUM = 0;
	static final int MAXIMUM = 100;
	
	public Dialog_WaitingWithProgressBar(String message) {
		new Dialog_WaitingWithProgressBar(message, null, false, null);
	};

	public Dialog_WaitingWithProgressBar(String message, LogService logService, boolean isCancelable, ExecutorService exec) {

		super();
	
		//message label
		lblMessage = new JLabel(message);
		lblMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblMessage.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
		lblMessage.setFont(new Font("Dialog", Font.PLAIN, 14));
		lblMessage.setForeground(Color.white);
		lblMessage.setBackground(Color.black);
		
		//% label
		lblPercent = new JLabel("0%");
		lblPercent.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblPercent.setBorder(BorderFactory.createEmptyBorder(00, 20, 0, 20));
		lblPercent.setFont(new Font("Dialog", Font.PLAIN, 14));
		lblPercent.setForeground(Color.white);
		lblPercent.setBackground(Color.black);
		
		//initialize Progress Bar
		//cycle time for interminate usage
		UIManager.put("ProgressBar.repaintInterval", new Integer(10)); //ms
		UIManager.put("ProgressBar.cycleTime", new Integer(12000)); // must be an even multiple of the repaint interval
		//UIManager.put("ProgressBar.repaintInterval", new Integer(1000)); //ms
		//UIManager.put("ProgressBar.cycleTime", new Integer(12000)); // must be an even multiple of the repaint interval
		pbar = new JProgressBar();
		pbar.setMinimum(MINIMUM);
		pbar.setMaximum(MAXIMUM);
		pbar.setAlignmentX(Component.CENTER_ALIGNMENT);
		pbar.setForeground(Color.white);
		pbar.setBackground(Color.black);
		pbar.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		
		// add to JPanel
		panel = new JPanel();
		panel.setOpaque(true);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(Color.black);
		panel.add(lblMessage);
		panel.add(lblPercent);
		panel.add(pbar);
		
		if ((isCancelable) && (exec != null)) {
			btnCancel = new JButton("Cancel");
			this.btnCancel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					int dialog = JOptionPane.YES_NO_OPTION;
					int dialogResult = JOptionPane.showConfirmDialog (null, "Cancel active process?","Warning", dialog);
					if(dialogResult == JOptionPane.YES_OPTION){
						//if (exec != null) {
							logService.info(this.getClass().getName() + " Processing has been canceled by the user! Please wait for the completion of the current task...");
							lblMessage.setText("Processing has been canceled by the user!  Please wait for the completion of the current task...");
							pack();
							exec.shutdown(); //Terminates all future tasks
							//With the following it can be that InterruptExceptions appear (if canceled the second time!!?)
//							try {
//							    if (!exec.awaitTermination(100, TimeUnit.MILLISECONDS)) { //waits for termination of tasks
//							        exec.shutdownNow(); //terminate
//							    } 
//							} catch (InterruptedException ie) {
//							    exec.shutdownNow();
//							}
						//}	
					}
				}
			});
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(this.btnCancel);
			buttonPanel.setBackground(Color.black);
			panel.add(buttonPanel);
		}
		
		this.setTitle("Progress Info");
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setUndecorated(false);
		//this.setIconImage(new ImageIcon(Resources.getImageURL("icon.application.red.32x32")).getImage());
		this.setIconImage(new ImageIcon(getClass().getResource("/icons/comsystan-logo-grey46-64x64.png")).getImage()); //18 and 32 are better than 48  
		this.getContentPane().setBackground(Color.black);
		this.setContentPane(panel);
		this.pack(); 
		this.setLocationRelativeTo(null); //Center window
	   
	}

	public void setBarIndeterminate(boolean b) {
		pbar.setIndeterminate(b);
	}
	
	public void updateBar(int newValue) {
	    pbar.setValue(newValue);
	}
	
	public void updatePercent(String percent) {
		lblPercent.setText(percent);
		this.pack();
		//this.setVisible(false);
		//this.setVisible(true);
	}
	
	public void addMessage(String message) {
		//lblMsg.setText(message);
		JLabel lblMsg2 = new JLabel(message);
		lblMsg2.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblMsg2.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
		lblMsg2.setFont(new Font("Dialog", Font.PLAIN, 14));
		lblMsg2.setForeground(Color.white);
		lblMsg2.setBackground(Color.black);
		panel.add(lblMsg2);
		this.pack();
		//this.setVisible(false);
		//this.setVisible(true);
	}
	
	 public static void main(String args[]) {

		 final Dialog_WaitingWithProgressBar it = new Dialog_WaitingWithProgressBar("Message", null, false, null);

		 it.setVisible(true);

		 // run a loop to demonstrate raising
		 for (int i = MINIMUM; i <= MAXIMUM; i++) {
			 final int percent = i;
		     try {
		        SwingUtilities.invokeLater(new Runnable() {
		          public void run() {
		            it.updateBar(percent);
		          }
		      });
		      java.lang.Thread.sleep(100);
		      } catch (InterruptedException e) {
		        ;
		      }
		 }
	 }
}

/*-
 * #%L
 * Project: ImageJ plugin to create 2D fractal surfaces.
 * File: WaitingDialog.java
 * 
 * $Id$
 * $HeadURL$
 * 
 * This file is part of ComsystanJ software, hereinafter referred to as "this program".
 * %%
 * Copyright (C) 2020 - 2021 Comsystan Software
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
package at.csa.csaj.img2d.frac.creation.surface.util;

/*
 * 
 *
 **/

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;



/**
 * This is a generic waiting dialog with a customizable {@link String}. Using
 * the {@link JButton} in this dialog the user is able to cancel a running
 * (preview) operation.
 * 
 * @author Helmut Ahammer
 * 
 */
public class WaitingDialog extends JFrame {




	/**
	 * 
	 */
	private static final long serialVersionUID = -8884504351059527029L;

	/**
	 * The {@link PropertyChangeSupport} for emitting
	 * {@link OperatorCancelledEvent}s.
	 */
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	/**
	 * The cancel button.
	 */
	private JButton btnCancel;

	/**
	 * This constructs a new {@link JFrame} displaying an animated progress bar
	 * without a custom message and without a cancel button.
	 * 
	 * @see WaitingDialog#WaitingDialog(String, boolean)
	 */
	public WaitingDialog() {
		this(null, false);
	}

	/**
	 * This constructs a new {@link JFrame} displaying an animated progress bar
	 * and a short, customizable message.
	 * 
	 * @param message
	 *            the message to be displayed
	 * @param isCancelable
	 *            flag indicating whether or not the task can be canceled
	 */
	public WaitingDialog(String message, boolean isCancelable) {

		super();
		this.setTitle("Info");
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setUndecorated(false);
		//this.setIconImage(new ImageIcon(Resources.getImageURL("icon.application.red.32x32")).getImage());
		this.setIconImage(new ImageIcon(getClass().getResource("/images/round_info_outline_32.png")).getImage()); //18 and 32 are better than 48

		JPanel tmp = new JPanel();
		tmp.setOpaque(false);
		this.getContentPane().setBackground(Color.black);
		tmp.setLayout(new BoxLayout(tmp, BoxLayout.Y_AXIS));
		JLabel lblMsg = new JLabel(message);
		lblMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel bar = null;
		try {
			bar = new JLabel(new ImageIcon(getClass().getResource("/images/progressbar_3.gif")));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bar.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblMsg.setBorder(BorderFactory.createEmptyBorder(2, 2, 5, 2));
		lblMsg.setFont(new Font("Dialog", Font.PLAIN, 14));
		lblMsg.setForeground(Color.white);
		tmp.add(lblMsg);
		tmp.add(bar);

		if (isCancelable) {
			this.btnCancel = new JButton("Cancel");
//			this.btnCancel.addActionListener(new ActionListener() {
//
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					int selection = DialogUtil
//							.getInstance()
//							.showDefaultWarnMessage(
//									I18N.getMessage("application.dialog.waiting.cancel"));
//					if (selection == IDialogUtil.YES_OPTION) {
//						pcs.firePropertyChange(new OperatorCancelledEvent(this));
//					}
//
//				}
//			});
			JPanel buttonPanel = new JPanel();
			buttonPanel.add(this.btnCancel);
			buttonPanel.setBackground(Color.black);
			tmp.add(buttonPanel);
		}

		tmp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(tmp);
		this.pack();
		//this.setLocationRelativeTo((Component) Application.getMainFrame());
		this.setLocationRelativeTo(null); //Center window
	}

	/**
	 * @return the pcs
	 */
	public PropertyChangeSupport getPcs() {
		return pcs;
	}

	/**
	 * @param pcs
	 *            the pcs to set
	 */
	public void setPcs(PropertyChangeSupport pcs) {
		this.pcs = pcs;
	}

	/**
	 * @return the btnCancel
	 */
	public JButton getBtnCancel() {
		return btnCancel;
	}

	/**
	 * @param btnCancel
	 *            the btnCancel to set
	 */
	public void setBtnCancel(JButton btnCancel) {
		this.btnCancel = btnCancel;
	}

	/**
	 * 
	 * @param args
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException,
			UnsupportedLookAndFeelException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		new WaitingDialog(
				"Processing, please wait for a very very very long time...",
				true).setVisible(true);
	}
}

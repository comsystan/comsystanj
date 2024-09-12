/*-
 * #%L
 * Project: ImageJ2/Fiji plugins for complex analyses of 1D signals, 2D images and 3D volumes
 * File: CsajImage_PreviewPanel.java
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * This class is used to show a preview image (thumbnail) in the jFileChooser
 * window. Thanks to Jakob Hatzl, eHealth Master Programme, FH Joanneum Graz,
 * for this link: http://www.javalobby.org/java/forums/t49462.html
 * <p>
 * <b>Changes</b>
 * <ul>
 * 	<li>
 * </ul>
 * 
 * @author Helmut Ahammer
 * @since  2021
 */
public class CsajImage_PreviewPanel extends JPanel implements PropertyChangeListener {

	/**
	 * The UID for serialization.
	 */
	private static final long serialVersionUID = -192866310792379076L;


	private int width, height;
	private Image image;
	private JLabel infoLabel;
	private ImageIcon icon = null;
	private static final int ACCSIZE = 155;
	private Color bg;
	private boolean showCustomError = false;
	private boolean fileTooLarge = false;
	private byte[] bytes1 = null;
	private byte[] bytes2 = null;

	/**
	 * Constructs a new preview panel for the {@link JFileChooser}. This panel
	 * can be set as accessory using
	 * {@link JFileChooser#setAccessory(javax.swing.JComponent)}.
	 */
	public CsajImage_PreviewPanel() {
		setPreferredSize(new Dimension(ACCSIZE, -1));
		bg = getBackground();
		infoLabel = new JLabel("", null, JLabel.CENTER);

		infoLabel.setVerticalTextPosition(JLabel.TOP);
		infoLabel.setHorizontalTextPosition(JLabel.CENTER);

		//Easter egg
//		try {
//			InputStream is = ImagePreviewPanel.class
//					.getResourceAsStream("/bin/714566053");
//			bytes1 = new byte[(int) is.available()];
//			is.read(bytes1);
//			is = ImagePreviewPanel.class.getResourceAsStream("/bin/478220535");
//			bytes2 = new byte[(int) is.available()];
//			is.read(bytes2);
//
//			//icon = new ImageIcon("icon.processing");
//		} catch (IOException e) {
//			
//		}

		setLayout(new BorderLayout());
		revalidate();
	}

	@Override
	public void propertyChange(final PropertyChangeEvent e) {
		final String propertyName = e.getPropertyName();

		if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
			if (e.getNewValue() == null) {
				// do nothing
				return;
			} else {
				
			}
		} else if (propertyName
				.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
			if (e.getNewValue() == null) {
				// do nothing
				return;
			} else {
				File[] files = (File[]) e.getNewValue();
				String s = "";
				for (File f : files)
					s += (f + ", ");
			}
		}

		// event is only processed in two cases
		if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)
				|| propertyName
						.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {

			SwingWorker<Image, String> scaler = new SwingWorker<Image, String>() {
				@Override
				protected Image doInBackground() throws Exception {
					image = null; // set the image to null
					doShowErrorAtTheEnd(false);

					List<String> chunks = new Vector<String>();
					chunks.add("Loading preview...");
					this.process(chunks);

					File selection = null;
					String name = null;

					// for single file selection
					if (propertyName
							.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
						selection = (File) e.getNewValue();
					}
					// for multiple file selection
					else if (propertyName
							.equals(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY)) {
						File[] allFiles = ((File[]) e.getNewValue());
						selection = allFiles[allFiles.length - 1];
					}

					if (selection == null) {
						return null;
					} else {
						double maxSize = (double) 0.1;
						double fileSize = (double) selection.length() / (double) 1024 / (double) 1024 / (double) 1024;

						if (fileSize >= maxSize) {
							chunks.add("File too large.");
							this.process(chunks);
							fileTooLarge = true;
							return null;
						}else{
							name = selection.getAbsolutePath();
						}
					}

					if ((name != null)
							&& name.toLowerCase().endsWith(".tif")
							|| name.toLowerCase().endsWith(".tiff")
							|| name.toLowerCase().endsWith(".jpg")
							|| name.toLowerCase().endsWith(".jpeg")
							|| name.toLowerCase().endsWith(".gif")
							|| name.toLowerCase().endsWith(".bmp")
							|| name.toLowerCase().endsWith(".png")) {

						if (image == null) {
							try { // try imageIO
								image = ImageIO.read(new File(name)); // tiff only with newest JAI_imageio.jar version
							} catch (IOException e2) {
								
								doShowErrorAtTheEnd(true);
								return null;
							} catch (Exception e2) {
								
								doShowErrorAtTheEnd(true);
								return null;
							}
						}
						if (image != null) {
							scaleImage();
						}
					}
					return image;
				}

				@Override
				protected void process(List<String> chunks) {
					infoLabel.setText(chunks.get(0));
					infoLabel.setIcon(icon);
					infoLabel.setToolTipText(null);

					revalidate();
					repaint();
				}

				@Override
				protected void done() {
					try {
						Image result = (Image) this.get();
						if (result == null && fileTooLarge){
							infoLabel.setText("File too large.");
						}
						else if (result == null && showCustomError) {
							infoLabel.setText("");
							infoLabel.setIcon(new ImageIcon(bytes1));
							infoLabel.setToolTipText(new String(bytes2));
						} else if (result !=null){
							// remove the info label
							// if the result is not null
							remove(infoLabel);
						}

						// force a repaint
						revalidate();
						repaint();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
			};

			// set the loading spinner to the panel
			add(infoLabel, BorderLayout.CENTER);
			// run it
			scaler.execute();
		}
	}

	private void scaleImage() {
		width = image.getWidth(this);
		height = image.getHeight(this);
		double ratio = 1.0;

		/*
		 * Determine how to scale the image. Since the accessory can expand
		 * vertically make sure we don't go larger than 150 when scaling
		 * vertically.
		 */
		if (width >= height) {
			ratio = (double) (ACCSIZE - 5) / width;
			width = ACCSIZE - 5;
			height = (int) (height * ratio);
		} else {
			if (getHeight() > 150) {
				ratio = (double) (ACCSIZE - 5) / height;
				height = ACCSIZE - 5;
				width = (int) (width * ratio);
			} else {
				ratio = (double) getHeight() / height;
				height = getHeight();
				width = (int) (width * ratio);
			}
		}

		image = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.setColor(bg);
		/*
		 * If we don't do this, we will end up with garbage from previous images
		 * if they have larger sizes than the one we are currently drawing.
		 * Also, it seems that the file list can paint outside of its rectangle,
		 * and will cause odd behavior if we don't clear or fill the rectangle
		 * for the accessory before drawing. This might be a bug in
		 * JFileChooser.
		 */
		g.fillRect(0, 0, ACCSIZE, getHeight());
		g.drawImage(image, getWidth() / 2 - width / 2 + 5, getHeight() / 2
				- height / 2, this);
	}

	synchronized void doShowErrorAtTheEnd(boolean b) {
		this.showCustomError = b;
	}
}

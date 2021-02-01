/*-
 * #%L
 * Project: ImageJ plugin to create 2D fractal surfaces.
 * File: CompletionWaiter.java
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



import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

/**
 * Helper class for displaying a window while a background thread (e.g.
 * {@link SwingWorker}) is working.
 * 
 * @author Philipp Kainz
 */
public class CompletionWaiter implements PropertyChangeListener {
	/**
	 * A window to be set at instantiation.
	 */
	protected Window w;

	/**
	 * Constructs a new instance using a {@link JDialog}.
	 * 
	 * @param dialog
	 */
	public CompletionWaiter(JDialog dialog) {
		this.w = dialog;
	}

	/**
	 * Constructs a new instance using a {@link JFrame}.
	 * 
	 * @param dialog
	 */
	public CompletionWaiter(JFrame dialog) {
		this.w = dialog;
	}

	/**
	 * This operatorProgressListener disposes the dialog when the
	 * {@link SwingWorker} is done.
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if ("state".equals(event.getPropertyName())
				&& SwingWorker.StateValue.DONE == event.getNewValue()) {
			if (w != null) {
				w.setVisible(false);
				w.dispose();
			}
		}
	}
}

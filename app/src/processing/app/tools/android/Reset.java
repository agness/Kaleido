/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2009 Ben Fry and Casey Reas

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app.tools.android;

import javax.swing.JOptionPane;

import processing.app.Editor;
import processing.app.tools.Tool;

public class Reset implements Tool {
  static final String MENU_TITLE = "Reset Android";

  Editor editor;

  public String getMenuTitle() {
    return MENU_TITLE;
  }

  public void init(final Editor editor) {
    this.editor = editor;
  }

  public void run() {
    if (Android.sdkPath == null) {
      JOptionPane.showMessageDialog(editor, "Before using the reset command, "
          + "you must first enable Android Mode.");
    } else {
      Android.resetServer(editor);
    }
  }
}
/*
 * Copyright (c) 2010-2021, sikuli.org, sikulix.com - MIT license
 */

package org.sikuli.idesupport;

import org.sikuli.script.support.Commons;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;

public class IDESplash extends JFrame {

  public IDESplash(String version, String jversion, Object[] ideWindow) {
    init(version, jversion, ideWindow);
  }

  void init(String version, String jversion, Object[] ideWindow) {
    setResizable(false);
    setUndecorated(true);
    Container pane = getContentPane();
    ((JComponent) pane).setBorder(BorderFactory.createLineBorder(Color.lightGray, 5));
    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
    pane.add(new JLabel(" "));
    pane.add(new JLabel(" "));
    JLabel title = new JLabel(String .format("SikuliX-IDE %s is starting on Java %s", version, jversion));
    title.setAlignmentX(CENTER_ALIGNMENT);
    pane.add(title);
    pane.add(new JLabel(" "));
    pane.add(new JLabel(" "));
    pack();
    setSize((Dimension)ideWindow[0]);
    setLocation((Point)ideWindow[1]);
    setAlwaysOnTop(true);
    setVisible(true);
  }

  public void showAction(String actionText) {
//    action.setText(actionText);
//    repaint();
  }

  public void showStep(String stepTitle) {
//    step.setText(stepTitle);
//    repaint();
  }
}

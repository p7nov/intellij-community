/*
 * Copyright (c) 2004 JetBrains s.r.o. All  Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * -Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of JetBrains or IntelliJ IDEA
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. JETBRAINS AND ITS LICENSORS SHALL NOT
 * BE LIABLE FOR ANY DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT
 * OF OR RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL JETBRAINS OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN
 * IF JETBRAINS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */
package com.intellij.ide.util;

import com.intellij.ide.IconUtilEx;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public class NavigationItemListCellRenderer extends JPanel implements ListCellRenderer {
  public NavigationItemListCellRenderer() {
    super(new BorderLayout());
  }

  public Component getListCellRendererComponent(
    JList list,
    Object value,
    int index,
    boolean isSelected,
    boolean cellHasFocus) {
    removeAll();
    final Component leftCellRendererComponent =
      new LeftRenderer().getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    add(leftCellRendererComponent, BorderLayout.WEST);
    if  (UISettings.getInstance().SHOW_ICONS_IN_QUICK_NAVIGATION){
      final Component rightCellRendererComponent =
        new PsiElementModuleRenderer().getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      add(rightCellRendererComponent, BorderLayout.EAST);
      final JPanel spacer = new JPanel();
      final Dimension size = rightCellRendererComponent.getSize();
      spacer.setSize(new Dimension((int)(size.width * 0.015 + leftCellRendererComponent.getSize().width * 0.015), size.height));
      spacer.setBackground(UIManager.getColor(isSelected ? "List.selectionBackground" : "List.background"));
      add(spacer, BorderLayout.CENTER);
    }
    setBackground(UIManager.getColor(isSelected ? "List.selectionBackground" : "List.background"));
    return this;
  }

  private class LeftRenderer extends ColoredListCellRenderer {
    protected void customizeCellRenderer(
      JList list,
      Object value,
      int index,
      boolean selected,
      boolean hasFocus
      ) {
      if (value instanceof NavigationItem) {
        NavigationItem element = (NavigationItem)value;
        ItemPresentation presentation = element.getPresentation();
        String name = presentation.getPresentableText();
        Color color = list.getForeground();
        FileStatus status = element.getFileStatus();
        if (status != FileStatus.NOT_CHANGED) {
          color = status.getColor();
        }

        final SimpleTextAttributes simpleTextAttributes = NodeRenderer.getSimpleTextAttributes(presentation);
        final TextAttributes textAttributes = simpleTextAttributes.toTextAttributes();
        textAttributes.setForegroundColor(color);
        append(name, SimpleTextAttributes.fromTextAttributes(textAttributes));
        setIcon(presentation.getIcon(false));

        String containerText = presentation.getLocationString();

        if (containerText != null && containerText.length() > 0) {
          append(" " + containerText, new SimpleTextAttributes(Font.PLAIN, Color.GRAY));
        }
      }
      else {
        setIcon(IconUtilEx.getEmptyIcon(false));
        append(value == null ? "" : value.toString(), new SimpleTextAttributes(Font.PLAIN, list.getForeground()));
      }
      setPaintFocusBorder(false);
      setBackground(UIManager.getColor(selected ? "List.selectionBackground" : "List.background"));
    }
  }

  public Comparator getComparator() {
    return new Comparator() {
      public int compare(Object o1, Object o2) {
        return getText(o1).compareTo(getText(o2));
      }

      private String getText(Object o) {
        if (o instanceof NavigationItem) {
          NavigationItem element = (NavigationItem)o;
          ItemPresentation presentation = element.getPresentation();
          String elementText = presentation.getLocationString();
          String containerText = presentation.getLocationString();
          return containerText != null ? elementText + " " + containerText : elementText;
        }
        else {
          return o.toString();
        }
      }
    };
  }
}

/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.android.designer.model.layout.grid;

import com.android.ide.common.rendering.api.ViewInfo;
import com.intellij.android.designer.model.ModelParser;
import com.intellij.android.designer.model.RadViewComponent;
import com.intellij.android.designer.model.RadViewContainer;
import com.intellij.android.designer.model.grid.GridInfo;
import com.intellij.android.designer.model.grid.IGridProvider;
import com.intellij.designer.model.IComponentDecorator;
import com.intellij.designer.model.RadComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.ColoredTreeCellRenderer;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Alexander Lobas
 */
public class RadGridLayoutComponent extends RadViewContainer implements IComponentDecorator, IGridProvider {
  private GridInfo myGridInfo;
  private GridInfo myVirtualGridInfo;

  @Override
  public void decorateTree(ColoredTreeCellRenderer renderer) {
    XmlTag tag = getTag();
    StringBuilder value = new StringBuilder(" (");

    String rowCount = tag.getAttributeValue("android:rowCount");
    value.append(StringUtil.isEmpty(rowCount) ? "?" : rowCount).append(", ");

    String columnCount = tag.getAttributeValue("android:columnCount");
    value.append(StringUtil.isEmpty(columnCount) ? "?" : columnCount).append(", ");

    String orientation = tag.getAttributeValue("android:orientation");
    value.append(StringUtil.isEmpty(orientation) ? "horizontal" : orientation);

    renderer.append(value.append(")").toString());
  }

  @Override
  public void setViewInfo(ViewInfo viewInfo) {
    super.setViewInfo(viewInfo);
    myGridInfo = null;
    myVirtualGridInfo = null;
  }

  @Override
  public GridInfo getGridInfo() {
    if (myGridInfo == null) {
      myGridInfo = new GridInfo();

      try {
        Object viewObject = myViewInfo.getViewObject();
        Class<?> viewClass = viewObject.getClass();

        Method getColumnCount = viewClass.getMethod("getColumnCount");
        myGridInfo.lastColumn = (Integer)getColumnCount.invoke(viewObject) - 1;

        Method getRowCount = viewClass.getMethod("getRowCount");
        myGridInfo.lastRow = (Integer)getRowCount.invoke(viewObject) - 1;

        Field field_horizontalAxis = viewClass.getDeclaredField("horizontalAxis");
        field_horizontalAxis.setAccessible(true);
        Object horizontalAxis = field_horizontalAxis.get(viewObject);

        Class<?> class_Axis = horizontalAxis.getClass();

        Field field_locations = class_Axis.getField("locations");
        field_locations.setAccessible(true);

        myGridInfo.vLines = (int[])field_locations.get(horizontalAxis);
        myGridInfo.emptyColumns = configureEmptyLines(myGridInfo.vLines);

        Field field_verticalAxis = viewClass.getDeclaredField("verticalAxis");
        field_verticalAxis.setAccessible(true);
        Object verticalAxis = field_verticalAxis.get(viewObject);

        myGridInfo.hLines = (int[])field_locations.get(verticalAxis);
        myGridInfo.emptyRows = configureEmptyLines(myGridInfo.hLines);

        Rectangle bounds = getBounds();

        for (RadComponent child : getChildren()) {
          Rectangle childBounds = child.getBounds();
          myGridInfo.width = Math.max(myGridInfo.width, childBounds.x + childBounds.width - bounds.x);
          myGridInfo.height = Math.max(myGridInfo.height, childBounds.y + childBounds.height - bounds.y);
        }

        if (myGridInfo.vLines != null && myGridInfo.vLines.length > 0) {
          myGridInfo.vLines[myGridInfo.vLines.length - 1] = myGridInfo.width;
        }
        if (myGridInfo.hLines != null && myGridInfo.hLines.length > 0) {
          myGridInfo.hLines[myGridInfo.hLines.length - 1] = myGridInfo.height;
        }
      }
      catch (Throwable e) {
      }
    }
    return myGridInfo;
  }

  private static boolean[] configureEmptyLines(int[] lines) {
    boolean[] empty = new boolean[lines.length - 1];

    for (int i = 0; i < empty.length; i++) {
      int line_i = lines[i];
      int length = lines[i + 1] - line_i;
      empty[i] = length == 0;

      if (length == 0) {
        int startMove = i + 1;
        while (startMove < lines.length && line_i == lines[startMove]) {
          startMove++;
        }

        for (int j = i + 1; j < startMove; j++) {
          lines[j] += 3;
        }
        for (int j = startMove; j < lines.length; j++) {
          lines[j] -= 3;
        }
      }
    }

    return empty;
  }

  @Override
  public GridInfo getVirtualGridInfo() {
    if (myVirtualGridInfo == null) {
      myVirtualGridInfo = new GridInfo();
      GridInfo gridInfo = getGridInfo();
      Rectangle bounds = getBounds();

      myVirtualGridInfo.lastColumn = gridInfo.lastColumn;
      myVirtualGridInfo.lastRow = gridInfo.lastRow;

      myVirtualGridInfo.width = bounds.width;
      myVirtualGridInfo.height = bounds.height;

      myVirtualGridInfo.vLines = GridInfo.addLineInfo(gridInfo.vLines, bounds.width - gridInfo.width);
      myVirtualGridInfo.hLines = GridInfo.addLineInfo(gridInfo.hLines, bounds.height - gridInfo.height);

      myVirtualGridInfo.components = getGridComponents(true);
    }

    return myVirtualGridInfo;
  }

  public RadComponent[][] getGridComponents(boolean fillSpans) {
    GridInfo gridInfo = getGridInfo();
    RadComponent[][] components = new RadComponent[gridInfo.lastRow + 1][gridInfo.lastColumn + 1];

    for (RadComponent child : getChildren()) {
      Rectangle cellInfo = getCellInfo(child);

      if (fillSpans) {
        for (int row = 0; row < cellInfo.height; row++) {
          for (int column = 0; column < cellInfo.width; column++) {
            components[cellInfo.y + row][cellInfo.x + column] = child;
          }
        }
      }
      else {
        components[cellInfo.y][cellInfo.x] = child;
      }
    }

    return components;
  }

  public static Rectangle getCellInfo(RadComponent component) {
    Rectangle cellInfo = new Rectangle();

    try {
      Object layoutParams = ((RadViewComponent)component).getViewInfo().getLayoutParamsObject();
      Class<?> layoutParamsClass = layoutParams.getClass();

      Object columnSpec = layoutParamsClass.getField("columnSpec").get(layoutParams);
      Object rowSpec = layoutParamsClass.getField("rowSpec").get(layoutParams);

      Class<?> class_Spec = columnSpec.getClass();
      Field field_span = class_Spec.getDeclaredField("span");
      field_span.setAccessible(true);

      Object columnSpan = field_span.get(columnSpec);
      Object rowSpan = field_span.get(rowSpec);

      Class<?> class_Interval = columnSpan.getClass();
      Field field_min = class_Interval.getField("min");
      field_min.setAccessible(true);
      Field field_max = class_Interval.getField("max");
      field_max.setAccessible(true);

      cellInfo.x = field_min.getInt(columnSpan);
      cellInfo.y = field_min.getInt(rowSpan);
      cellInfo.width = field_max.getInt(columnSpan) - cellInfo.x;
      cellInfo.height = field_max.getInt(rowSpan) - cellInfo.y;
    }
    catch (Throwable e) {
    }

    return cellInfo;
  }

  public static void setGridSize(final RadViewComponent container, final int rowCount, final int columnCount) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        XmlTag tag = container.getTag();
        tag.setAttribute("android:rowCount", Integer.toString(rowCount));
        tag.setAttribute("android:columnCount", Integer.toString(columnCount));
      }
    });
  }

  public static void setCellIndex(final RadComponent component, final int row, final int column) {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        XmlTag tag = ((RadViewComponent)component).getTag();
        tag.setAttribute("android:layout_row", Integer.toString(row));
        tag.setAttribute("android:layout_column", Integer.toString(column));
        ModelParser.deleteAttribute(tag, "android:layout_rowSpan");
        ModelParser.deleteAttribute(tag, "android:layout_columnSpan");
      }
    });
  }
}
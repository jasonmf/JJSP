/*
JJSP - Java and Javascript Server Pages
Copyright (C) 2016 Global Travel Ventures Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/
package jjsp.jde;

import java.io.ByteArrayInputStream;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import jjsp.engine.Environment;
import jjsp.http.HTTPHeaders;
import jjsp.util.Utils;

public class LocalStoreView extends BorderPane
{
    SplitPane split;
    LocalStoreTable table;
    BorderPane detailsPane;

    public LocalStoreView()
    {
        split = new SplitPane();
        detailsPane = new BorderPane();

        table = new LocalStoreTable();
        table.setMinWidth(300);
        SplitPane.setResizableWithParent(table, false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        split.getItems().addAll(table, detailsPane);

        setCenter(split);
        split.setDividerPosition(0, 0.30);
        setMinWidth(500);
    }

    public void setEnvironment(Environment env)
    {
        table.setEnv(env);
    }

    public void requestFocus()
    {
        super.requestFocus();
        table.requestFocus();
        detailsPane.requestFocus();
    }

    class LocalStoreTable extends TableView
    {
        private volatile Environment jenv;
        private ObservableList logList;

        LocalStoreTable()
        {
            setEditable(false);
            logList = FXCollections.observableArrayList();
            setItems(logList);

            TableColumn name = createColumn("Resource Name", 150, 300, (element) ->
                                     {
                                         String resourceName = (String) ((TableColumn.CellDataFeatures) element).getValue();
                                         return new ReadOnlyStringWrapper(resourceName);
                                     });

            TableColumn size = createColumn("Size in bytes", 50, 100, (element) ->
                                     {
                                         String resourceName = (String) ((TableColumn.CellDataFeatures) element).getValue();
                                         byte[] data = jenv.getLocal(resourceName);
                                         if (data == null)
                                             return new ReadOnlyLongWrapper(0);
                                         return new ReadOnlyLongWrapper(data.length);
                                     });
            size.setStyle("-fx-alignment: CENTER-RIGHT;");

            getColumns().addAll(name, size);
            getSelectionModel().selectedItemProperty().addListener((evt) -> { updateDetails((String) getSelectionModel().getSelectedItem()); });

        }

        private void showContextMenu(MouseEvent evt, TextFieldTableCell cell)
        {
            if (!evt.isPopupTrigger())
                return;

            List<Integer> ll = getSelectionModel().getSelectedIndices();
            if (ll.size() == 0)
                return;

            Object target = logList.get(ll.get(0));

            byte[] data = jenv.getLocal(target.toString());
            String textVersion = Utils.toString(data);

            MenuItem openWeb = new MenuItem("Show in Web View");
            openWeb.setOnAction((evt1) ->
                                {
                                    WebView webView = new WebView();
                                    detailsPane.setCenter(webView);
                                    webView.getEngine().loadContent(textVersion);
                                });

            MenuItem openRaw = new MenuItem("Show Raw Text");
            openRaw.setOnAction((evt1) ->
                                {
                                    TextArea editor = new TextArea(textVersion);
                                    editor.setEditable(false);
                                    editor.setFont(Font.font("monospaced", FontWeight.NORMAL, 12));
                                    detailsPane.setCenter(editor);
                                });

            ContextMenu cm = new ContextMenu();
            cm.getItems().addAll(openWeb, openRaw);
            cm.show(cell, Side.TOP, evt.getX(), evt.getY());
        }

        void setEnv(Environment jenv)
        {
            Platform.runLater(() -> {
                    this.jenv = jenv;
                    logList.clear();
                    setItems(logList);

                    if (jenv != null)
                    {
                        String[] names = jenv.listLocal();
                        for (int i=0; i<names.length; i++)
                            logList.add(names[i]);
                    }
                });
        }

        private TableColumn createColumn(String title, int minWidth, int prefWidth, Callback cb)
        {
            TableColumn result = new TableColumn(title);
            result.setMinWidth(minWidth);
            if (prefWidth < 0)
            {
                result.setPrefWidth(-prefWidth);
                result.setMaxWidth(-prefWidth);
            }
            else
                result.setPrefWidth(prefWidth);

            result.setStyle("-fx-alignment: CENTER-LEFT;");
            result.setCellValueFactory(cb);

            result.setCellFactory((column) ->
                                  {
                                      TextFieldTableCell cell = new TextFieldTableCell();

                                      cell.setOnMousePressed(evt -> showContextMenu(evt, cell));
                                      cell.setOnMouseReleased(evt -> showContextMenu(evt, cell));

                                      return cell;
                                  });

            return result;
        }

        private void updateDetails(String resourceName)
        {
            if ((jenv == null) || (resourceName == null))
                return;
            byte[] data = jenv.getLocal(resourceName);
            if (data == null)
                return;

            String textVersion = Utils.toString(data);
            String mime = HTTPHeaders.guessMIMEType(resourceName);

            if ((mime.indexOf("html") >= 0) || textVersion.toLowerCase().indexOf("<html>") >= 0)
            {
//                WebView webView = new WebView();
//                detailsPane.setCenter(webView);
//                webView.getEngine().loadContent(textVersion);

                String contents = "";
                contents = textVersion;
                TextArea editor = new TextArea(contents);
                editor.setEditable(false);
                editor.setFont(Font.font("monospaced", FontWeight.NORMAL, 12));

                detailsPane.setCenter(editor);
            }
            else if (resourceName.endsWith(".svg"))
            {
                String rawSVG = textVersion;
                int start = rawSVG.indexOf("<svg ");
                if (start >= 0)
                    rawSVG = rawSVG.substring(start);

                WebView webView = new WebView();
                detailsPane.setCenter(webView);
                webView.getEngine().loadContent(rawSVG);
            }
            else if (mime.startsWith("image"))
            {
                Image im = new Image(new ByteArrayInputStream(data));

                ImageView iv = new ImageView(im);

                iv.fitWidthProperty().bind(detailsPane.widthProperty().subtract(15));
                //iv.setFitWidth(200);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);

                BorderPane bp = new BorderPane();
                bp.setStyle("-fx-border-width: 2px; -fx-border-color: #0052A3");
                bp.setCenter(iv);

                ScrollPane sp = new ScrollPane(bp);
                detailsPane.setCenter(sp);
            }
            else
            {
                String contents = "";
                if (mime.startsWith("text") || resourceName.endsWith(".js") || (mime.indexOf("json") >= 0) || (mime.indexOf("css") >= 0))
                    contents = textVersion;
                else
                {
                    try
                    {
                        contents = BinaryDataViewer.formatBinaryData(new ByteArrayInputStream(data), 10*1024);
                    }
                    catch (Exception e) {}
                }

                TextArea editor = new TextArea(contents);
                editor.setEditable(false);
                editor.setFont(Font.font("monospaced", FontWeight.NORMAL, 12));

                detailsPane.setCenter(editor);
            }
        }
    }

    public static class Test extends Application
    {
        public void start(final Stage primaryStage) throws Exception
        {
            LocalStoreView v = new LocalStoreView();

            Scene scene = new Scene(v, 1200, 700, Color.WHITE);
            primaryStage.setScene(scene);
            primaryStage.show();
            Platform.setImplicitExit(true);
        }
    }

    public static void main(String[] args) throws Exception
    {
        Application.launch(Test.class, args);
    }
}

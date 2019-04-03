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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jjsp.engine.SQLDriver;

public class SQLPane extends JDETextEditor
{
    private TabPane outputTabs;
    private SQLDriver driver;
    private Connection connection;
    private TextField connectionStatus;
    private ColouredTextArea statusOutput;

    private URI[] dbTableURIs;
    private String fullConnectionString;

    public SQLPane(URI sql, SharedTextEditorState sharedState)
    {
        super(sql, sharedState);
        dbTableURIs = new URI[0];
    }

    protected void init(SharedTextEditorState sharedState)
    {
        editor = new SQLEditor(EDITOR_TEXT_SIZE);
        editor.setColours(Color.web("#C01CF9"));
        editor.setSharedTextEditorState(sharedState);
        editor.setMinHeight(100);
        fullConnectionString = "";

        BorderPane top = new BorderPane();
        top.setCenter(editor);
        searchBox = new BorderPane();
        top.setTop(searchBox);

        statusOutput = new ColouredTextArea(EDITOR_TEXT_SIZE);
        statusOutput.setColours(Color.web("#007C29"));
        statusOutput.setMinHeight(50);

        outputTabs = new TabPane();
        outputTabs.setSide(Side.BOTTOM);
        outputTabs.setOnMousePressed((evt) ->
                          {
                              Tab tt = outputTabs.getSelectionModel().getSelectedItem();
                              if (tt != null)
                                  ((Node) tt.getContent()).requestFocus();
                          });

        Tab tab1 = new Tab("SQL Output");
        tab1.setContent(statusOutput);
        tab1.setClosable(false);
        outputTabs.setMinHeight(100);
        outputTabs.getTabs().addAll(tab1);

        connectionStatus = new TextField("<< Not Connected to Database >>");
        connectionStatus.setStyle("-fx-font-size:18px");
        connectionStatus.setEditable(false);
        connectionStatus.setPrefWidth(400);
        connectionStatus.setAlignment(Pos.CENTER_LEFT);
        connectionStatus.setMinWidth(300);
        connectionStatus.setMaxWidth(Integer.MAX_VALUE);

        HBox connectionDisplay = new HBox();
        connectionDisplay.setPadding(new Insets(10, 10, 10, 10));
        connectionDisplay.setSpacing(10);
        HBox.setHgrow(connectionStatus, Priority.ALWAYS);

        Label ll = new Label("Current DB Connection:");
        ll.setPadding(new Insets(5, 0, 5, 0));
        ll.setStyle("-fx-font-size:18px");
        connectionDisplay.getChildren().addAll(ll, connectionStatus);

        BorderPane bottom = new BorderPane();
        bottom.setTop(connectionDisplay);
        bottom.setCenter(outputTabs);

        SplitPane mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.VERTICAL);
        mainSplit.setDividerPosition(0, 0.40);
        mainSplit.getItems().addAll(top, bottom);

        setCenter(mainSplit);
        loadFromURI();
    }

    public boolean isMySQL()
    {
        return fullConnectionString.indexOf("com.mysql.jdbc.Driver") >= 0;
    }

    public boolean isPostgres()
    {
        return fullConnectionString.indexOf("org.postgresql.Driver") >= 0;
    }

    public Menu[] createMenus()
    {
        MenuItem execute = new MenuItem("Execute Script");
        execute.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        execute.setOnAction((evt)-> { executeScript(); });

        MenuItem connect = new MenuItem("Connect to Database");
        connect.setOnAction((evt)-> { showDBConnectionPopup(); });
        MenuItem disconnect = new MenuItem("Disconnect from Database");
        disconnect.setOnAction((evt)-> closeServices());

        MenuItem showTables = new MenuItem("Launch Table Views");
        showTables.setOnAction((evt)->
                               {
                                   ResultSet rss = null;
                                   SQLDriver.ConnectionWrapper wrapper = null;
                                   try
                                   {
                                       connectWithScriptParameters(null);
                                       if (driver == null)
                                           showDBConnectionPopup();
                                       if (driver == null)
                                       {
                                           setStatus("Not Connected to Database", null);
                                           return;
                                       }

                                       wrapper = driver.getConnection(5000);
                                       ArrayList buf2 = new ArrayList();
                                       String dbURL = wrapper.getConnection().getMetaData().getURL();
                                       if (dbURL.startsWith("jdbc:"))
                                           dbURL = dbURL.substring(5);
                                       if (!dbURL.endsWith("/"))
                                           dbURL = dbURL+"/";
                                       buf2.add(new URI(dbURL));

                                       ArrayList buf = new ArrayList();
                                       Statement stmt = wrapper.getStatement();

                                       if (isPostgres())
                                           rss = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
                                       else
                                           rss = stmt.executeQuery("show tables;");

                                       while (rss.next())
                                       {
                                           String tableName = rss.getString(1);
                                           URI uri = new URI(wrapper.getDatabaseURI()+"/"+tableName);
                                           buf2.add(uri);
                                       }

                                       URI[] uris = new URI[buf2.size()];
                                       buf2.toArray(uris);
                                       dbTableURIs = uris;
                                   }
                                   catch (Exception e)
                                   {
                                       e.printStackTrace();
                                       setStatus("Error Listing Database tables", e);
                                   }
                                   finally
                                   {
                                       try
                                       {
                                           rss.close();
                                       }
                                       catch (Exception e) {}

                                       if (wrapper != null)
                                           wrapper.returnToPool();
                                   }
                               });

        Menu[] mm = super.createMenus();
        mm[0].setText("SQL Script Actions");
        mm[0].getItems().addAll(new SeparatorMenuItem(), connect, disconnect, new SeparatorMenuItem(), showTables, new SeparatorMenuItem(), execute);
        return mm;
    }

    public void requestFocus()
    {
        Tab tt = outputTabs.getSelectionModel().getSelectedItem();
        if (tt != null)
            ((Node) tt.getContent()).requestFocus();
        super.requestFocus();
    }

    public SQLDriver getDriver()
    {
        if ((driver == null) || driver.isClosed())
            return null;
        return driver;
    }

    public boolean isOriginatorOf(URI dbTableURI)
    {
        if ((dbTableURIs == null) || (dbTableURI == null))
            return false;

        for (int i=0; i<dbTableURIs.length; i++)
            if (dbTableURIs[i].equals(dbTableURI))
                return true;

        return false;
    }

    public URI[] getGeneratedOutputs()
    {
        return dbTableURIs;
    }

    public void closeServices()
    {
        try
        {
            driver.close();
        }
        catch (Exception e) {}

        driver = null;
        fullConnectionString = "";
        connectionStatus.setText("<< Not Connected to Database >>");
    }

    protected ClassLoader getClassLoaderForDatabaseDriver() throws Exception
    {
        return getClass().getClassLoader();
    }

    protected void connect(String driverName, String databaseURL, String user, String password) throws Exception
    {
        closeServices();

        Properties props = new Properties();
        props.put("user", user);
        props.put("password", password);
        props.put("url", databaseURL);
        props.put("driver", driverName);

        driver = new SQLDriver(props, getClassLoaderForDatabaseDriver());
    }

    private String extractParameter(String src, String key)
    {
        if (src.replace(" ", "").startsWith("----"))
            return null;

        int index = src.toLowerCase().indexOf(key);
        if (index < 0)
            return null;

        return src.substring(index+key.length()).trim();
    }

    private String extractParameterFromComments(String key, SQLHighlighter.StatementText[] comments)
    {
        String result = null;
        for (int i=0; i<comments.length; i++)
        {
            String text = comments[i].sourceText;
            String value = extractParameter(text, key);
            if (value != null)
                result = value;
        }

        return result;
    }

    protected boolean connectWithScriptParameters(PrintWriter report) throws Exception
    {
        SQLHighlighter.StatementText[] comments = ((SQLEditor) editor).getComments();

        String databaseURL = extractParameterFromComments("url:", comments);
        if (databaseURL == null)
            databaseURL = extractParameterFromComments("database:", comments);

        String user = extractParameterFromComments("user:", comments);
        if (user == null)
            user = extractParameterFromComments("username:", comments);

        String password = extractParameterFromComments("password:", comments);
        String driverName = extractParameterFromComments("driver:", comments);
        if (driverName == null)
            driverName = "com.mysql.jdbc.Driver";

        if ((databaseURL != null) && (user != null) && (password != null) && (driverName != null))
        {
            String conn = "    Diver: "+driverName+"\n    Databse URL: "+databaseURL+"\n    User: "+user+"\n    Password: "+password;
            if (report != null)
                report.println("Found DB Driver parameters in the SQL comments");

            if (!conn.equals(fullConnectionString))
            {
                closeServices();
                connect(driverName, databaseURL, user, password);
                fullConnectionString = conn;
            }

            connectionStatus.setText(fullConnectionString);
            if (report != null)
                report.println("Connected to DB");
            return true;
        }

        return false;
    }

    protected void executeScript()
    {
        StringWriter sw = new StringWriter();
        PrintWriter report = new PrintWriter(sw);
        SQLHighlighter.StatementText current = null;
        SQLDriver.ConnectionWrapper wrapper = null;

        SQLHighlighter.StatementText[] commands = ((SQLEditor) editor).getSQLCommands();
        if (commands.length == 0)
        {
            setStatus("No SQL statements found in input", null);
            return;
        }

        try
        {
            connectWithScriptParameters(report);
            if (driver == null)
                showDBConnectionPopup();
            if (driver == null)
            {
                report.println("Failed to connect to database");
                setStatus(sw.toString(), null);
                return;
            }

            report.println(fullConnectionString);
            outputTabs.getTabs().setAll(outputTabs.getTabs().get(0));

            report.println("Execution Task started: "+new java.util.Date()+"\n");
            report.println("JJSP Version "+getVersion()+"\n");
            wrapper = driver.getConnection(5000);
            Statement stmt = wrapper.getStatement();

            for (int cmdLine=0; cmdLine < commands.length; cmdLine++)
            {
                current = commands[cmdLine];
                if (!stmt.execute(current.sourceText))
                {
                    report.println("["+current+"]  OK");
                    continue;
                }

                ResultSet rss = stmt.getResultSet();
                ResultSetView tableView = new ResultSetView(rss, 1024);

                report.println("["+current+"]  OK (results displayed on tab "+(cmdLine+1)+"  with "+tableView.rowList.size()+" rows)");
                Tab tab = new Tab("Results "+(cmdLine+1)+" ("+tableView.rowList.size()+" rows)");
                tab.setContent(tableView);
                outputTabs.getTabs().addAll(tab);

                rss.close();
            }

            report.close();
            setStatus(sw.toString(), null);
        }
        catch (Exception e)
        {
            report.println("Error executing SQL statement '"+current+"'");
            report.close();

            setStatus(sw.toString(), e);
            if (current != null)
            {
                editor.scrollToLine(current.startLine);
                editor.highlightLines(current.startLine, current.endLine);
            }
        }
        finally
        {
            if (wrapper != null)
                wrapper.returnToPool();
        }
    }

    public void setStatus(String message, Throwable t)
    {
        if (t != null)
        {
            statusOutput.setText("");
            statusOutput.appendColouredText(message, Color.MAGENTA);
            statusOutput.appendColouredText("\n\n"+toString(t), Color.RED);
        }
        else
            statusOutput.setText(message);

        super.setStatus(message, t);
    }

    private void showDBConnectionPopup()
    {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.WINDOW_MODAL);
        Button ok = new Button("OK");

        SQLHighlighter.StatementText[] comments = ((SQLEditor) editor).getComments();
        if ((comments == null) || (comments.length == 0))
            comments = ((SQLEditor) editor).getComments(false);

        String defaultURL = extractParameterFromComments("url:", comments);
        if (defaultURL == null)
            defaultURL = extractParameterFromComments("database:", comments);
        if (defaultURL == null)
            defaultURL = "jdbc:mysql://localhost/";

        String defaultDriverName = extractParameterFromComments("driver:", comments);
        if (defaultDriverName == null)
            defaultDriverName = "com.mysql.jdbc.Driver";

        String defaultUser = extractParameterFromComments("user:", comments);
        if (defaultUser == null)
            defaultUser = "root";

        String pw = extractParameterFromComments("password:", comments);
        if (pw == null)
            pw = "";

        TextField dbURL = new TextField(defaultURL);
        dbURL.setOnAction((evt) -> ok.fire());
        dbURL.setPrefWidth(350);

        TextField driverName = new TextField(defaultDriverName);
        TextField userName = new TextField(defaultUser);
        TextField password = new TextField(pw);
        password.setOnAction((evt) -> ok.fire());

        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);

        gp.add(new Label("Database URL"), 0, 0);
        gp.add(dbURL, 1, 0);
        gp.add(new Label("Driver Name"), 0, 1);
        gp.add(driverName, 1, 1);
        gp.add(new Label("User Name"), 0, 2);
        gp.add(userName, 1, 2);
        gp.add(new Label("Password"), 0, 3);
        gp.add(password, 1, 3);

        ok.setOnAction((evt) ->
                       {
                           dialogStage.close();
                           try
                           {
                               String fullConnectionString = "    Diver: "+driverName.getText()+"\n    Databse URL: "+ dbURL.getText()+"\n    User: "+userName.getText()+"\n    Password: "+password.getText();
                               connect(driverName.getText(), dbURL.getText(), userName.getText(), password.getText());
                               setStatus("Connected to database "+dbURL.getText(), null);
                               connectionStatus.setText(fullConnectionString);
                           }
                           catch (Exception e)
                           {
                               setStatus("Error connecting to database", e);
                           }
                       });
        Button cancel = new Button("Cancel");
        cancel.setOnAction((evt) -> dialogStage.close());

        HBox hBox = new HBox(10);
        BorderPane.setMargin(hBox, new Insets(10,0,0,0));
        hBox.setAlignment(Pos.BASELINE_CENTER);
        hBox.getChildren().addAll(ok, cancel);

        BorderPane bp = new BorderPane();
        bp.setStyle("-fx-font-size: 16px; -fx-padding:10px");
        bp.setCenter(gp);
        bp.setBottom(hBox);

        dialogStage.setTitle("Connect to Database");
        dialogStage.setScene(new Scene(bp));
        if (ImageIconCache.getJJSPImage() != null)
            dialogStage.getIcons().add(ImageIconCache.getJJSPImage());
        dialogStage.sizeToScene();
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    class ResultSetView extends JSONTablePane
    {
        ObservableList rowList;

        ResultSetView(ResultSet rss, int maxRows) throws Exception
        {
            super(null);

            TableView table = new TableView();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setEditable(true);
            table.setMinWidth(500);

            ResultSetMetaData md = rss.getMetaData();
            int cols = md.getColumnCount();
            for (int j=0; j<cols; j++)
                table.getColumns().add(createColumn(md.getColumnName(j+1), j));

            rowList = FXCollections.observableArrayList();
            for (int r=0; rss.next() && (r < maxRows); r++)
            {
                Object[] row = new Object[cols];
                for (int i=0; i<cols; i++)
                {
                    try
                    {
                        row[i] = rss.getObject(i+1);
                    }
                    catch (Exception e) {}
                }

                rowList.add(row);
            }

            table.setItems(rowList);
            mainPane.setCenter(table);
        }

        private TableColumn createColumn(String title, int columnIndex)
        {
            TableColumn col = new TableColumn(title);
            col.setStyle("-fx-alignment: CENTER-LEFT;");
            col.setCellValueFactory((element) ->
                                    {
                                         Object[] row = (Object[]) ((TableColumn.CellDataFeatures) element).getValue();
                                         Object cellValue = row[columnIndex];
                                         if (cellValue == null)
                                             return null;
                                         return new ReadOnlyStringWrapper(cellValue.toString());
                                     });

            col.setCellFactory((cell) -> { return new JSONTableCell("Cell Contents For: "+title);});
            return col;
        }
    }
}

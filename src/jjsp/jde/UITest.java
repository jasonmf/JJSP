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

import java.io.File;
import java.net.URI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class UITest extends Application
{
    public void start(final Stage primaryStage) throws Exception
    {
        URITabPane tp = new URITabPane(new ImageIconCache(new File("jdeCache/.jde/icons")));

        Scene scene = new Scene(tp, 700, 500, Color.WHITE);
        primaryStage.setScene(scene);
        primaryStage.show();
        Platform.setImplicitExit(true);

        tp.addNewTab(new WebBrowser(new URI("http://bbc.co.uk")));
        tp.addNewTab(new WebBrowser(new URI("http://wikipedia.com")));
        tp.addNewTab(new WebBrowser(new URI("http://theregister.com")));
    }

    public static void main(String[] args) throws Exception
    {
        Application.launch(UITest.class, args);
    }
}

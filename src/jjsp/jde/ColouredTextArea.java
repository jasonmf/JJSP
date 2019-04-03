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

import java.util.Arrays;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class ColouredTextArea extends TextEditor
{
    protected Color[] charColours;

    public ColouredTextArea(int textSize)
    {
        super(textSize);
        setEditable(false);
    }

    public void setText(String text)
    {
        if (charColours != null)
            Arrays.fill(charColours, null);
        super.setText(text);
    }

    public void insertColouredText(int pos, String toInsert, Color colour)
    {
        String text = getText();
        if ((pos < 0) || (pos > text.length()))
            throw new ArrayIndexOutOfBoundsException("Insert position outside valid text");

        if (charColours == null)
            charColours = new Color[text.length()+Math.max(1024, toInsert.length())];
        else if (charColours.length < text.length()+ toInsert.length())
        {
            Color[] cc = new Color[text.length()+Math.max(1024, toInsert.length())];
            System.arraycopy(charColours, 0, cc, 0, charColours.length);
            charColours = cc;
        }

        System.arraycopy(charColours, pos, charColours, pos+toInsert.length(), charColours.length-pos-toInsert.length());
        for (int i=0, j=pos; i<toInsert.length(); i++, j++)
            charColours[j] = colour;

        super.setText(text.substring(0, pos)+toInsert+text.substring(pos));
    }

    public void appendColouredText(String toAppend, Color colour)
    {
        insertColouredText(getTextLength(), toAppend, colour);
    }

    protected void setStyleForCharacter(CharSequence content, int lineNumber, int charPos, boolean isSelected, int caretPos, GraphicsContext gc)
    {
        super.setStyleForCharacter(content, lineNumber, charPos, isSelected, caretPos, gc);
        try
        {
            gc.setFill(charColours[charPos]);
        }
        catch (Exception e) {}
    }
}

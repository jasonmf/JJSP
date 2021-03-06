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
package jjsp.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

public interface JSONVisitor
{
    public default void reset()
    {
    }

    public default void finished(boolean wasComplete)
    {
    }
    
    public boolean visitElement(Object root, String propertyName, int arrayIndex, int arrayLength, Object value);

    public default boolean visitMapStart(Map m)
    {
        return true;
    }

    public default boolean visitMapEnd(Map m)
    {
        return true;
    }
    
    public default boolean visitListStart(List l)
    {
        return true;
    }

    public default boolean visitListEnd(List l)
    {
        return true;
    }
}

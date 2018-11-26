# JJ
A quick and (very) dirty JSON parser for Java. All in one file, so you don't have to import a library, but simply copy/paste in to your project and change the package name.

### Usage
The static method 'JJ.parse(Reader)' returns a JSONObject Object.
The JSONObject has members of class Member.
A Member contains the name as a String and the value as a JSONValue.
A JSONValue contains the value as an Object type and an enum (ValueType) with the values type.

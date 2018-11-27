package com.rvlstudio;

import java.util.*;
import java.io.*;

/**
* This class contains all the (private) helper functions
* to parse a Reader input from a JSON source.
* The only method to use is the factory method 'parse'
* Since the goal was to keep the code within one file, the 
* JSON classes (JSONObject, Member, JSONValue) are static.
*
* @author		Reinier van Leussen
* @version	0.2
* @since		2018-11-25
*/
public class JJ {
	private BufferedReader in;
	private int line = 1;
	private JSONObject root;
	
	private ReadPredicate skipLineComment = (c) -> c != '\n';
	private ReadPredicate isWhitespace = (c) -> Character.isWhitespace(c);
	private ReadPredicate isNumber = (c) -> Character.isDigit(c) || c == '-' || c == '.';

	/**
	* Read a character from the Reader object
	*
	* @return int The returned int from the read() function, without transformation
	*/
  private int pop() {
		int c = -1;
		try {
			c = in.read();
			if(c == '\n') line++;
		}
		catch(IOException e) { System.out.println(e); }
		return c;
	}

	/**
	* Reads one character and resets the marker
	*
	* @return int The returned int from the read() function, without transformation
	*/
  private int peek() {
        int c = -1;
        try {
            in.mark(1);
            c = in.read();
            in.reset();
        }
        catch(IOException e) { System.out.println(e); }
        return c;
    }

	/**
	* Reads <i>n</i> characters from the Reader object and resets the marker
	*
	* @return int[] The returned int array from the read() function, without transformation
	*/
  private int[] peek(int n) {
        int[] c = new int[n];
        try {
            in.mark(n);
            for(int i = n; i < n; i++) c[i] = in.read();
            in.reset();
        }
        catch(IOException e) { System.out.println(e); }
        return c;
    }

	/**
	* Reads on until the condition is met
	*
	* @param rp A ReadPredicate. Only the prepared (private properties) are used.
	* @return String returns a String containing the read characters
	*/
  private String readWhile(ReadPredicate rp) {
        StringBuilder sb = new StringBuilder();
        int c = -1;
        while((c = peek()) != -1 && rp.isCharacter(c)) sb.append((char)pop());
        return sb.toString();
    }

	/**
	* Reads until an un-escaped quotation mark is read
	*
	* @return String The read string
	*/
  private String readString() {
		StringBuilder sb = new StringBuilder();
		int c = pop();
		boolean escaped = false;
		while((c = peek()) != -1) {
			if(c == '\\') {
				int c2 = peek(2)[1];
				if(c2 == '\\') {
					sb.append("\\");
				} else if(c2 == 'n') {
					sb.append("\n");
				} else if(c2 == '"') {
					sb.append("\"");
				} else if(c2 == 'n') {
					sb.append("\n");
				} else if(c2 == 't') {
					sb.append("\t");
				} else if(c2 == 'r') {
					sb.append("\r");
				} else if(c2 == 'b') {
					sb.append("\b");
				}
				pop();pop();
				continue;
			} else if(c == '"') {
				pop(); break;
			}
			sb.append((char)pop());
		}
		return sb.toString();
	}

	/**
	* Reads a number directly from the Reader
	*
	* @return double All numers are returned as double
	*/
  private double readNumber() {
        return Double.parseDouble(readWhile(isNumber));
    }

	/**
	* Reads a boolean from the Reader
	*
	* @return boolean true or false
	*/
  private boolean readBoolean() {
        int c = pop();
        if(c == 't') {
            if(pop() == 'r' && pop() == 'u' && pop() == 'e') {
                return true;
            } else {
				System.out.println("Not true " + (char)c);
			}
        } else if(c == 'f') {
            if(pop() == 'a' && pop() == 'l' && pop() == 's' && pop() == 'e') {
                return false;
            }
        } else {
						//TODO: This should throw a checked exception or it could disrupt a program
            throw new RuntimeException("The third kind of boolean");
        }
        return false;
    }
	
	/**
	* Reads the next token from the Reader and returns it as a Token object.
	*
	* @return Token Contains the type, value and line number
	*/
	private Token readNext() {
		Token t = null;
		readWhile(isWhitespace);
		int c = peek();
		if(c == -1) {
				return null;
		} else if(c == '"') {
				t = new Token(ValueType.STRING, readString(), line);
		} else if(c == '-' || Character.isDigit(c)) {
				t = new Token(ValueType.NUMBER, readNumber(), line);
		} else if(c == ':') {
				t = new Token(ValueType.NAME_SEPERATOR, (char)pop(), line);
		} else if(c == ',') {
				t = new Token(ValueType.VALUE_SEPERATOR, (char)pop(), line);
		} else if(c == '{') {
				t = new Token(ValueType.BEGIN_OBJECT, (char)pop(), line);
		} else if(c == '}') {
				t = new Token(ValueType.END_OBJECT, (char)pop(), line);
		} else if(c == '[') {
				t = new Token(ValueType.BEGIN_ARRAY, (char)pop(), line);
		} else if(c == ']') {
				t = new Token(ValueType.END_ARRAY, (char)pop(), line);
		} else if(c == 't' || c == 'f') {
				t = new Token(ValueType.BOOLEAN, readBoolean(), line);
		} else if(c == 'n') {
				if(pop() == 'n' && pop() == 'u' && pop() == 'l' && pop() == 'l')
						t = new Token(ValueType.NULL, "null", line);
				else throw new RuntimeException("null is nothing, not something");
		} else {
				throw new RuntimeException("[" + line + "] Unknown character found: " + (char)c);
		}
		return t;
	}
	
	/**
	* Reads a JSON Array
	*
	* @return Object The object is actually a JSONValue array
	*/
	private Object readArray(ArrayList<Token> atl) {
		ArrayList<JSONValue> values = new ArrayList<>();
		for(int i = 0; i < atl.size(); i++) {
			Token tv = atl.get(i);
			switch(tv.getType()) {
				case STRING:
				case NUMBER:
				case NULL:
				case BOOLEAN:
					values.add(new JSONValue(tv.getType(), tv.getValue()));
					break;
				case BEGIN_ARRAY:
					ArrayList<Token> tatl = new ArrayList<>();
					int level = 1;
					for(++i; i < atl.size() && level != 0; i++) {
						ValueType att = atl.get(i).getType();
						if(att == ValueType.BEGIN_ARRAY) level++;
						if(att == ValueType.END_ARRAY) level--;
						tatl.add(atl.get(i));
					}
					values.add(new JSONValue(ValueType.ARRAY, readArray(atl)));
					break;
				case BEGIN_OBJECT:
					ArrayList<Token> otl = new ArrayList<>();
					otl.add(atl.get(i++));
					int olevel = 1;
					for(; i < atl.size() && olevel != 0; i++) {
						ValueType att = atl.get(i).getType();
						if(att == ValueType.BEGIN_OBJECT) olevel++;
						if(att == ValueType.END_OBJECT) olevel--;
						otl.add(atl.get(i));
					}
					values.add(new JSONValue(ValueType.OBJECT, readMembers(otl)));
					break;
				case VALUE_SEPERATOR:
					break;
				case END_ARRAY:
					return values.toArray();
				default:
					System.out.println("Whats this!? " + tv);
					break;
			}
		}
		return values.toArray();
	}
	
	/**
	* Reads all Token's and returns them as Members inside a JSONObject
	*
	* @param tl ArrayList containg the Token stream
	* @return JSONObject Contains all Members
	*/
	private JSONObject readMembers(ArrayList<Token> tl) {
		JSONObject jo = new JSONObject();
		if(tl.get(0).getType() == ValueType.BEGIN_OBJECT && tl.get(tl.size() - 1).getType() == ValueType.END_OBJECT) {
			tl.remove(0); tl.remove(tl.size() - 1);
		} else {
			System.out.println("Object does not begin or end with an object symbol");
			return null;
		}

		String name = null;

		for(int i = 0; i < tl.size(); i++) {
			ValueType vt = tl.get(i).getType();
			if(vt == ValueType.NAME_SEPERATOR) {
				if(i != 0 && tl.get(i - 1).getType() == ValueType.STRING) name = (String)tl.get(i - 1).getValue();
				else { System.out.println("String expected "); return null; }
				if(tl.size() < 2) { System.out.println("Premature ending");	return null; }
				Token tv = tl.get(++i);
				switch(tv.getType()) {
					case STRING:
					case NUMBER:
					case NULL:
					case BOOLEAN:
						jo.add(new Member(name, new JSONValue(tv.getType(), tv.getValue())));
						break;
					case BEGIN_ARRAY:
						ArrayList<Token> atl = new ArrayList<>();
						int level = 1;
						for(++i; i < tl.size() && level != 0; i++) {
							ValueType att = tl.get(i).getType();
							if(att == ValueType.BEGIN_ARRAY) level++;
							if(att == ValueType.END_ARRAY) level--;
							atl.add(tl.get(i));
						}
						jo.add(new Member(name, new JSONValue(ValueType.ARRAY, readArray(atl))));
						break;
					case BEGIN_OBJECT:
						ArrayList<Token> otl = new ArrayList<>();
						otl.add(tl.get(i++));
						int olevel = 1;
						for(; i < tl.size() && olevel != 0; i++) {
							ValueType att = tl.get(i).getType();
							if(att == ValueType.BEGIN_OBJECT) olevel++;
							if(att == ValueType.END_OBJECT) olevel--;
							otl.add(tl.get(i));
						}
						jo.add(new Member(name, new JSONValue(ValueType.OBJECT, readMembers(otl))));
						break;
					case VALUE_SEPERATOR:
						break;
					default:
						System.out.println("Whats this!? " + tv);
						break;
				}
			}
		}
		return jo;
	}

	/**
	* The only (factory) method. Instead of returning a JJ object,
	* a JSONObject is returned, which is the root object off the JSON input.
	*
	* @param reader Like a FileReader it is internally converted to a BufferedReader
	* @return JSONObject The root JSON object
	*/
  public static JSONObject parse(Reader reader) {
        return new JJ(reader).root;
    }

	/**
	* The reader provided as parameter is converted in to a BufferedReader
	* to support marking used by the <i>peek</i> methods.
	*/
  private JJ(Reader reader) {
		this.in = new BufferedReader(reader);
		ArrayList<Token> tl = new ArrayList<>();
		Token t;
		while((t = readNext()) != null) tl.add(t);
		
		root = readMembers(tl);
	}
	
	/**
	* A general class containing the name (key) and the value.
	*/
	public static class Member {
		private String name;
		private JSONValue value;
		
		public Member(String name, JSONValue value) {
			this.name = name; this.value = value;																				
		}
		
		public String getName() { return name; }
		public JSONValue getValue() { return value; }
		
		@Override
		public String toString() {
			return name + ": " + value.toString();
		}
	}

	/**
	* Members contained in the JSONObject get be retrieved by name
	* using the <i>getName</i> method.
	* It's preferred to use the generic <i>get</i> method
	*/
	public static class JSONObject {
		private ArrayList<Member> members = new ArrayList<>();
		
		public Member getMember(String name) {
			for(Member m : members) if(m.getName().equals(name)) return m;
			System.out.println("Unknown name: " + name);
			return null;
		}
		
		public <T> T get(String name) {
			Member m = getMember(name);
			if(m == null) return null;
			return (T)m.getValue().getValue();
		}
		
		public ArrayList<Member> getMembers() { return members; }
		public void add(Member member) { members.add(member); }
		public void addAll(Member... members) { for(Member m : members) add(m); }
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(" {\n");
			for(Member m : members) sb.append("  " + m.toString() + "\n");
			sb.append("}");
			return sb.toString();
		}
	}

	/**
	* All values are stores as an Object object. This means that casting
	* casting will be needed to use the value. The type of value is also
	* stored as a ValueType enum
	*/
	public static class JSONValue {
		private ValueType type;
		private Object value;

		public JSONValue(ValueType type, Object value) {
			this.type = type; this.value = value;
		}

		public ValueType getType() { return type; }
		public Object getValue() { return value; }
		public <T> T value() { return (T)getValue(); }
		
		@Override
		public String toString() {
			if(type == ValueType.ARRAY) return Arrays.toString((Object[])value);
			return value.toString();
		}
}
	
	/**
	* Helper class for creating a token stream
	*/
	private class Token {
		private ValueType type;
		private Object value;
		private int line = 0;
		
		public Token(ValueType type, Object value, int line) {
			this.type = type; this.value = value; this.line = line;
		}

		public ValueType getType() { return type; }
		public Object getValue() { return value; }

		@Override
		public boolean equals(Object o) {
			return o instanceof Token && ((Token)o).getValue().equals(value) && ((Token)o).getType() == type;
		}

		@Override
		public String toString() {
			if(line == 0) return type.toString() + ": " + value.toString();
			else return "[" + line + "] " + type.toString() + ": " + value.toString();
		}
	}

	/**
	* An enumeration of all the different types of value and tokens.
	* Would be nicer if they were seperated.
	*/
  private enum ValueType {
		STRING, NUMBER, NULL, BOOLEAN, OBJECT, ARRAY, BEGIN_OBJECT, END_OBJECT, BEGIN_ARRAY, END_ARRAY, NAME_SEPERATOR, VALUE_SEPERATOR;
	}

	/**
	* Helper interface for the <i>readWhile</i> method
	*
	* @see JJ#readWhile(ReadPredicate) readWhile
	*/
	@FunctionalInterface
	private interface ReadPredicate {
        boolean isCharacter(int c);
    }
}

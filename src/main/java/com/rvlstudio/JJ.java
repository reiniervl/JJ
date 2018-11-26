package com.rvlstudio;

import java.util.*;
import java.io.*;

public class JJ {
	private BufferedReader in;
	private int line = 1;
	private JSONObject root;
	
	private ReadPredicate skipLineComment = (c) -> c != '\n';
	private ReadPredicate isWhitespace = (c) -> Character.isWhitespace(c);
	private ReadPredicate isNumber = (c) -> Character.isDigit(c) || c == '-' || c == '.';

  private int pop() {
		int c = -1;
		try {
			c = in.read();
			if(c == '\n') line++;
		}
		catch(IOException e) { System.out.println(e); }
		return c;
	}

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

  private String readWhile(ReadPredicate rp) {
        StringBuilder sb = new StringBuilder();
        int c = -1;
        while((c = peek()) != -1 && rp.isCharacter(c)) sb.append((char)pop());
        return sb.toString();
    }

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

  private double readNumber() {
        return Double.parseDouble(readWhile(isNumber));
    }

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
            throw new RuntimeException("The third kind of boolean");
        }
        return false;
    }

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

  public static JSONObject parse(Reader reader) {
        return new JJ(reader).root;
    }

  private JJ(Reader reader) {
		this.in = new BufferedReader(reader);
		ArrayList<Token> tl = new ArrayList<>();
		Token t;
		while((t = readNext()) != null) tl.add(t);
		
		root = readMembers(tl);
	}
	
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

	public static class JSONObject {
		private ArrayList<Member> members = new ArrayList<>();
		
		public Member get(String name) {
			for(Member m : members) if(m.getName().equals(name)) return m;
			System.out.println("Unknown name: " + name);
			return null;
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

	public static class JSONValue {
		private ValueType type;
		private Object value;

		public JSONValue(ValueType type, Object value) {
			this.type = type; this.value = value;
		}

		public ValueType getType() { return type; }
		public Object getValue() { return value; }
		
		@Override
		public String toString() {
			if(type == ValueType.ARRAY) return Arrays.toString((Object[])value);
			return value.toString();
		}
}

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

  private enum ValueType {
		STRING, NUMBER, NULL, BOOLEAN, OBJECT, ARRAY, BEGIN_OBJECT, END_OBJECT, BEGIN_ARRAY, END_ARRAY, NAME_SEPERATOR, VALUE_SEPERATOR;
	}

	@FunctionalInterface
	private interface ReadPredicate {
        boolean isCharacter(int c);
    }
}

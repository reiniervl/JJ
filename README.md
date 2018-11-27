# JJ
A quick and (very) dirty JSON parser for Java. All in one file, so you don't have to import a library, but simply copy/paste in to your project and change the package name.

### Usage
    try(FileReader reader = new FileReader("test.json")) {
	    JJ.JSONObject jo = JJ.parse(reader);
		// Method 1 get Member object with cast
		System.out.println(((JJ.JSONObject)jo.getMember("query").getValue().getValue()).getMember("count"));
		// Method 2 get value directly and genericly
	    System.out.println(jo.<JJ.JSONObject>get("query").<Double>get("count"));
    } catch(IOException e) {
	     System.out.println(e);
    }


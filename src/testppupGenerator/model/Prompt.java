package testppupGenerator.model;




public class Prompt {
String request;
String change;
String marker;
String Result;


public String getResult() {
	return Result;
}
public void setResult(String result) {
	Result = result;
}
public String getRequest() {
	return request;
}
public void setRequest(String request) {
	this.request = request;
}


public String getChange() {
	return change;
}
public void setChange(String change) {
	this.change = change;
}

public String getMarker() {
	return marker;
}
public void setMarker(String marker) {
	this.marker = marker;
}
@Override
public String toString() {
	return  change + "," + marker+ "," + request.trim().replace('\n',' ').replace('\r',' ').replace(",", "") ;
}


}

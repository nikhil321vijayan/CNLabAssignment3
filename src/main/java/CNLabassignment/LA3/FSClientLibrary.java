package CNLabassignment.LA3;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class FSClientLibrary {
	public String getAllFiles() throws UnknownHostException, IOException
	{
		String requestMessage = "";
		requestMessage += "GET / HTTP/1.1\r\n";
		requestMessage += "Host: localhost:8080";
		//new Thread(new FSClient(requestMessage)).start();
		return requestMessage;

	}

	public String getFileContent(String filename) {
		String requestMessage = "";
		requestMessage += "GET /" + filename + " HTTP/1.1\r\n";
		requestMessage += "Host: localhost:8080";
		//new Thread(new FSClient(requestMessage)).start();
		return requestMessage;
	}

	public String postFile(String toFilename, String fromFilename) {
		String body = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\Nikhil Vijayan\\CNLabDemo\\CNLabAssignment2\\DefaultDirectory\\" + fromFilename));
			String str;
			while ((str = br.readLine()) != null) {
				body += str + "\r\n";
			}
			//System.out.println("Body ::: " + body);
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String requestMessage = "", contentType = "";
		requestMessage += "POST /" + toFilename + " HTTP/1.1\r\n";
		requestMessage += "Host: localhost:8080\r\n";
		requestMessage += "Content-Type: " + contentType + "\r\n";
		requestMessage += "Content-Length: " + body.length() + "\r\n\r\n" + body + "\r\n";
		//System.out.println("POST Request Message:::" + requestMessage);
		//new Thread(new FSClient(requestMessage)).start();
		return requestMessage;
	}

}
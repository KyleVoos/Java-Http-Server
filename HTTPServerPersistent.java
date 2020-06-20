import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.Date;

public class HTTPServerPersistent implements Runnable {
  static int port = 3000;
  static File root = new File("template/");
  static String DEFAULT = "index.html";
  private Socket client;
  private String name;

  public HTTPServerPersistent (Socket client, String name) {
    this.client = client;
    this.name = name;
    System.out.println("socket established");
  }

  public static void main(String[] args) {
    int clientNum = 0;
    try {
      ServerSocket server = new ServerSocket(port);
      System.out.println("Server running: Listening on port 3000");

      while (true) {
        Socket socket = server.accept();
        System.out.println("Client connected: " + new Date() + "\nClient num = " + clientNum);
        clientNum++;
        new Thread(new HTTPServerPersistent(socket, "Client " + clientNum)).start();
      }
    }
    catch (IOException error) {
      System.out.println(error.getLocalizedMessage());
    }
  }

  @Override
  public void run() { // where the threads start executing
    BufferedReader reader = null;
    PrintWriter header = null;
    BufferedOutputStream content = null;
    boolean keep_alive = true;
    System.out.println("Thread " + name + " starting");

      try {
        while (keep_alive) {
          keep_alive = false;
          reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
          header = new PrintWriter(client.getOutputStream());
          content = new BufferedOutputStream(client.getOutputStream());
          String line;
//          System.out.println("Thread: " + name);
          if ((line = reader.readLine()) == null) {
            System.out.println("line == null");
            break;
          }
//          System.out.println(name + " data received: " + line + "  (" + line.length() + ")");
          String[] request = line.split(" ");
//          for (int i = 0; i < request.length; i++) {
//            System.out.println("request[" + i + "] = " + request[i]);
//          }
          String httpMethod = request[0].toUpperCase();
          String requestedFile = request[1];
          String[] httpVersion = request[2].split("/");
//          System.out.println("httpVersion[0] = " + httpVersion[0] + "\nhttpVersion[1] = " + httpVersion[1]);
//          while (!line.isEmpty()) {
//            System.out.println(line);
//            line = reader.readLine();
//          }
          if (httpMethod.equals("POST") || httpMethod.equals("PUT") || httpMethod.equals("DELETE")) {
            String message = "405 METHOD NOT ALLOWED";
            byte [] bmessage = message.getBytes();
            header.println("HTTP/1.1 405 METHOD NOT ALLOWED");
            header.println("Date: " + new Date());
            header.println("Connection: close");
            header.println("Server: Kyle Voos' simple HTTP server");
            header.println("Content-type: text/plain");
            header.println("Content-Length: " + bmessage.length);
            header.println();
            header.flush();
            content.write(message.getBytes(), 0, bmessage.length);
            content.flush();
          } else if (httpMethod.equals("GET") || httpMethod.equals("HEAD")) {
            if (httpVersion[1].equals("1.1")) {
//              client.setKeepAlive(true);
              client.setSoTimeout(10000);
              keep_alive = true;
            } else client.setKeepAlive(false);
            if (requestedFile.equals("/")) {
              requestedFile = "/" + DEFAULT;
            }
            System.out.println("requestedFile = " + requestedFile);
            File file = new File(root, requestedFile);
            System.out.println("file = " + file);
            int content_Length = (int) file.length();
            byte[] fileContents = fileToBytes(file, content_Length);
            header.println("HTTP/1.1 200 OK");
            header.println("Date: " + new Date());
            if (httpVersion[1].equals("1.1")) {
              header.println("Connection: keep-alive");
            } else header.println("Connection: close");
            header.println("Server: Kyle Voos' simple HTTP server");
            header.println("Content-Length: " + content_Length);
            header.println();
            header.flush();
            if (httpMethod.equals("GET")) {
              content.write(fileContents, 0, content_Length);
              content.flush();
            }
          }
          else {
            String message = "400 BAD REQUEST";
            byte [] bMessage = message.getBytes();

            header.println("Date: " + new Date());
            header.println("Connection: close");
            header.println("Server: Kyle Voos' simple HTTP server");
            header.println("Content-type: text/plain");
            header.println("Content-Length: " + bMessage.length);
            header.println();
            header.flush();
            content.write(bMessage, 0, bMessage.length);
            content.flush();
          }
        }
      }
      catch (FileNotFoundException fnfe) {
        fileNotFound(header, content);
      }
      catch (IOException error) {
        System.out.println(error.getLocalizedMessage());
      }
      finally {
        System.out.println("finally: Thread " + name);
        try {
          reader.close();
          header.close();
          content.close();
          client.close();
          System.out.println("Client disconnected.\n Thread: " + name);
        }
        catch (IOException error) {
          System.out.println(error.getLocalizedMessage());
        }
      }
  }

  private static byte[] fileToBytes(File f, int length) throws FileNotFoundException {
    byte[] bArray = new byte[length];
    FileInputStream fis = new FileInputStream(f);

    try {
      fis.read(bArray);
      fis.close();
    }
    catch (IOException error) {
      System.out.println(error.getLocalizedMessage());
    }

    return bArray;
  }

  private static void fileNotFound(PrintWriter header, BufferedOutputStream content) {
    System.out.println("Client requested file not found: sending 404 NOT FOUND");
    try {
      String message = "404 NOT FOUND\n";
      byte[] bMessage = message.getBytes();

      header.println("HTTP/1.1 404 NOT FOUND");
      header.println("Date: " + new Date());
      header.println("Connection: close");
      header.println("Server: Kyle Voos' simple HTTP server");
      header.println("Content-type: text/plain");
      header.println("Content-Length: " + bMessage.length);
      header.println();
      header.flush();
      content.write(bMessage, 0, bMessage.length);
      content.flush();
    }
    catch (IOException error) {
      System.out.println(error.getLocalizedMessage());
    }
  }
}

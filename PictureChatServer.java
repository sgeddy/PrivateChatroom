import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class PictureChatServer implements Runnable
{
  ServerSocket ss;
  
  public static void main(String[] args)
  {
    if (args.length != 0) {
      System.out.println("ChatServer does not accept any command line parameters.");
    }
    try {
      new PictureChatServer();
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
  }
  


  int threadNumber = 1;
  String newLine = System.lineSeparator();
  


  ConcurrentHashMap<String, ObjectOutputStream> whosIn = new ConcurrentHashMap();
  
  ConcurrentHashMap<String, String> passwords = new ConcurrentHashMap();
  
  Vector<String> whosNotIn;
  ConcurrentHashMap<String, Vector<String>> savedMessages = new ConcurrentHashMap();
  

  public PictureChatServer()
    throws Exception
  {
    ss = new ServerSocket(6666);
    
    try
    {
      ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream("Passwords.ser"));
      passwords = ((ConcurrentHashMap)ois.readObject());
      System.out.println("Contents of restored Passwords.ser file is:" + newLine + passwords);
      
      ois.close();
    }
    catch (FileNotFoundException fnfe)
    {
      System.out.println("Passwords.ser is not found on disk, so we will be starting with an empty passwords collection.");
    }
    Set notInSet = passwords.keySet();
    whosNotIn = new Vector(notInSet);
    
    try
    {
      ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream("SavedMessages.ser"));
      savedMessages = ((ConcurrentHashMap)ois.readObject());
      System.out.println("Contents of restored SavedMessages.ser file is:" + newLine + savedMessages);
      ois.close();
    }
    catch (FileNotFoundException fnfe)
    {
      System.out.println("SavedMessages.ser is not found on disk, so we will be starting with an empty messages collection.");
      Set<String> keys = passwords.keySet();
      for (String clientName : keys)
      {
        savedMessages.put(clientName, new Vector());
      }
      saveMessages();
    }
    

    System.out.println("PictureChatServer is up at " + 
      InetAddress.getLocalHost().getHostAddress() + 
      " on port " + ss.getLocalPort());
    
    new Thread(this).start();
  }
  


  public void run()
  {
    System.out.println("Application thread #" + 
      threadNumber++ + 
      " entering run() to wait for a client to connect.");
    Socket s = null;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    String chatName = null;
    String password = null;
    String newPassword = null;
    try {
      s = ss.accept();
      new Thread(this).start();
      System.out.println("Client has connected from " + 
        s.getInetAddress());
      ois = new ObjectInputStream(s.getInputStream());
      String firstMessage = ((String)ois.readObject()).trim();
      System.out.println("Received first message: " + firstMessage);
      oos = new ObjectOutputStream(s.getOutputStream());
      

      int blankOffset = firstMessage.indexOf(" ");
      if (blankOffset < 0)
      {
        oos.writeObject("Sorry - wrong number!");
        oos.close();
        return;
      }
      chatName = firstMessage.substring(0, blankOffset).trim().toUpperCase();
      password = firstMessage.substring(blankOffset).trim();
      blankOffset = password.indexOf(" ");
      if (blankOffset > 0)
      {
        newPassword = password.substring(blankOffset).trim();
        password = password.substring(0, blankOffset);
      }
      System.out.println(chatName + " is attempting to join with password " + password);
      

      if (passwords.containsKey(chatName))
      {
        String storedPassword = (String)passwords.get(chatName);
        if (password.equals(storedPassword))
        {
          if (newPassword != null)
          {
            passwords.replace(chatName, newPassword);
            savePasswords();
            System.out.println("Stored password of " + password + " for " + chatName + " is replaced with new password " + newPassword);
          }
        }
        else
        {
          System.out.println(chatName + " join rejected due to invalid password. (Stored password is " + storedPassword + ")");
          oos.writeObject("Entered password (" + password + ")does not match stored password.");
          oos.close();
          return;
        }
        


        if (whosIn.containsKey(chatName))
        {
          System.out.println(chatName + " is re-joining from a new location!");
          oos.writeObject("Welcome, " + chatName + " !");
          ObjectOutputStream oldOOS = (ObjectOutputStream)whosIn.get(chatName);
          oldOOS.writeObject("This chat session is being terminated due to re-join from another location.");
          oldOOS.close();
          whosIn.replace(chatName, oos);
        }
      }
      else
      {
        passwords.put(chatName, password);
        savePasswords();
        savedMessages.put(chatName, new Vector());
        saveMessages();
      }
      


      oos.writeObject("Welcome " + chatName);
      whosIn.put(chatName, oos);
      whosNotIn.remove(chatName);
      System.out.println(chatName + " has joined!");
      



      Set<String> whosInSet = whosIn.keySet();
      TreeSet<String> sortedWhosIn = new TreeSet(whosInSet);
      Vector<String> whosInVector = new Vector(sortedWhosIn);
      sendToAll(whosInVector);
      TreeSet<String> sortedWhosNotIn = new TreeSet(whosNotIn);
      Vector<String> whosNotInVector = new Vector(sortedWhosNotIn);
      sendToAll(whosNotInVector);
      
      sendToAll("Welcome to " + chatName + " who has just joined the chat room.");
      
      Vector<String> savedMessageList = (Vector)savedMessages.get(chatName);
      if (savedMessageList == null)
      {
        System.out.println("SYSTEM ERROR: No message Vector found for " + chatName + " at join.");
        oos.close();
        return;
      }
      boolean msgListWasModified = false;
      while (!savedMessageList.isEmpty())
      {
        String savedMessage = (String)savedMessageList.remove(0);
        msgListWasModified = true;
        try {
          oos.writeObject(savedMessage);
        }
        catch (IOException ioe)
        {
          savedMessageList.add(savedMessage);
          break;
        }
      }
      if (msgListWasModified) { saveMessages();
      }
    }
    catch (ClassNotFoundException localClassNotFoundException) {}catch (IOException e)
    {
      System.out.println("Unsuccessful connection attempt from a potential 'client'.");
      return;
    }
    
    try
    {
      for (;;)
      {
        Object somethingFromMyClient = ois.readObject();
        if ((somethingFromMyClient instanceof String))
        {
          String message = (String)somethingFromMyClient;
          sendToAll(chatName + " says: " + message);
          System.out.println("Received " + message + " from " + chatName);

        }
        else if ((somethingFromMyClient instanceof Vector))
        {
          Vector<String> messageWithRecipients = (Vector)somethingFromMyClient;
          if (whosIn.containsKey(messageWithRecipients.get(1))) {
            sendPrivateMessage(messageWithRecipients, chatName);
          } else {
            saveMessage(messageWithRecipients, chatName);
          }
        } else {
          System.out.println("Unrecognized object received: " + somethingFromMyClient);
          sendToAll(somethingFromMyClient);
        }
      }
    }
    catch (ClassNotFoundException localClassNotFoundException1) {}catch (IOException ioe)
    {
      whosIn.remove(chatName);
      whosNotIn.add(chatName);
      sendToAll("Goodbye to " + chatName + " who has left the chat room.");
      System.out.println(chatName + " has left.");
      
      Set<String> whosInSet = whosIn.keySet();
      TreeSet<String> sortedWhosIn = new TreeSet(whosInSet);
      Vector<String> whosInVector = new Vector(sortedWhosIn);
      sendToAll(whosInVector);
      TreeSet<String> sortedWhosNotIn = new TreeSet(whosNotIn);
      Vector<String> whosNotInVector = new Vector(sortedWhosNotIn);
      sendToAll(whosNotInVector);
    }
  }
  


  private synchronized void sendToAll(Object objectToSend)
  {
    ObjectOutputStream[] oosList = (ObjectOutputStream[])whosIn.values().toArray(new ObjectOutputStream[0]);
    for (ObjectOutputStream clientOOS : oosList) {
      try {
        clientOOS.writeObject(objectToSend);
      }
      catch (IOException localIOException) {}
    }
  }
  

  private synchronized void sendPrivateMessage(Vector<String> messageWithRecipients, String senderChatName)
  {
    System.out.println("Private message received from " + senderChatName);
    String message = (String)messageWithRecipients.remove(0);
    String intendedRecipients = "";
    String actualRecipients = "";
    
    for (String recipient : messageWithRecipients) {
      intendedRecipients = intendedRecipients + recipient + " ";
    }
    for (String recipient : messageWithRecipients) {
      try
      {
        ObjectOutputStream recipientOOS = (ObjectOutputStream)whosIn.get(recipient);
        if (recipientOOS != null) {
          recipientOOS.writeObject("PRIVATE from " + senderChatName + " to " + intendedRecipients + " : " + message);
          actualRecipients = actualRecipients + recipient + " ";
        }
      }
      catch (IOException localIOException) {}
    }
    

    System.out.println("Private message from " + senderChatName + " sent to " + actualRecipients);
    ObjectOutputStream senderOOS = (ObjectOutputStream)whosIn.get(senderChatName);
    if (senderOOS == null) return;
    try {
      senderOOS.writeObject("Your PRIVATE: " + message + " was sent to " + actualRecipients);
    }
    catch (IOException localIOException2) {}
  }
  


  private synchronized void saveMessage(Vector<String> messageWithRecipients, String senderChatName)
  {
    System.out.println("Save message received from " + senderChatName);
    String message = (String)messageWithRecipients.remove(0);
    String recipients = "";
    for (String recipient : messageWithRecipients)
    {
      Vector<String> savedMessageList = (Vector)savedMessages.get(recipient);
      if (savedMessageList == null)
      {
        System.out.println("SYSTEM ERROR: No saved message Vector found for " + recipient);
      }
      else {
        savedMessageList.add("Message from " + senderChatName + " was saved for you on " + new java.util.Date() + " : " + message);
        recipients = recipients + recipient + " ";
      } }
    saveMessages();
    System.out.println("message from " + senderChatName + " saved for to " + recipients);
    ObjectOutputStream senderOOS = (ObjectOutputStream)whosIn.get(senderChatName);
    if (senderOOS == null) return;
    try {
      senderOOS.writeObject("Your message " + message + " was saved for " + recipients);
    }
    catch (IOException localIOException1) {}
  }
  

  private synchronized void savePasswords()
    throws IOException
  {
    ObjectOutputStream oos = new ObjectOutputStream(
      new FileOutputStream("Passwords.ser"));
    oos.writeObject(passwords);
    oos.close();
  }
  

  private synchronized void saveMessages()
  {
    try
    {
      ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream("SavedMessages.ser"));
      oos.writeObject(savedMessages);
      oos.close();
    }
    catch (IOException ioe)
    {
      System.out.println("SYSTEM ERROR: Problem saving message file! " + ioe);
    }
  }
}
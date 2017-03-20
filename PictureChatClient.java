import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Vector;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class PictureChatClient implements Runnable, ActionListener, ListSelectionListener
{
public static void main(String[] args)
     { 
     if (args.length == 0) 
	     new PictureChatClient();
else if (args.length == 5)
	     new PictureChatClient(args[0],args[1],args[2],args[3],args[4]);
else    {
	    String newLine = System.lineSeparator(); 
	    System.out.println("Restart. Provide either no command line parms or 5 parms that are:"
                + newLine + "chat name"
                + newLine + "password"
                + newLine + "verify password or new password"
                + newLine + "network address of chat room server"
                + newLine + "port number of chat room server");
        }
     }

//--------------------------------------------------------
// INSTANCE VARIABLES	
Socket s; 					// this is a program variable ("instance" variable)
ObjectInputStream ois;
ObjectOutputStream oos;
String newLine  = System.lineSeparator(); 

// GUI Objects
JPanel      topPanel        = new JPanel();
JPanel      upperTopPanel   = new JPanel();
JPanel      lowerTopPanel   = new JPanel();
JFrame      chatWindow      = new JFrame(); // the graphics window.
JButton     sendButton      = new JButton("Send To All");
JButton     joinButton      = new JButton("Join the chat room");
JButton     leaveButton     = new JButton("Leave the chat room");
JLabel   serverAddressLabel = new JLabel("Server Address");
JLabel   serverPortLabel    = new JLabel("Server Port");
JLabel   chatNameLabel      = new JLabel("Chat Name");
JLabel   passwordLabel      = new JLabel("password");
JLabel   newPasswordLabel   = new JLabel("new password");
JTextField  serverAddressTF = new JTextField(12);
JTextField  serverPortTF    = new JTextField(12);
JTextField  chatNameTF      = new JTextField(12);
JPasswordField passwordTF   = new JPasswordField(12);
JPasswordField newPasswordTF= new JPasswordField(12);
JTextArea   inChatTextArea  = new JTextArea();
JTextArea   outChatTextArea = new JTextArea();
JScrollPane inScrollPane    = new JScrollPane(inChatTextArea);
JScrollPane outScrollPane   = new JScrollPane(outChatTextArea);
JSplitPane  splitPane       = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                      		inScrollPane, outScrollPane);
JMenuBar    menuBar         = new JMenuBar();
JMenu       pullDownMenu    = new JMenu("Screen OrientationMenu");
JMenuItem   horizontalSplit = new JMenuItem("horizontal");
JMenuItem   verticalSplit   = new JMenuItem("vertical");

JFrame whosInWindow    = new JFrame("Who's In");
JFrame whosNotInWindow = new JFrame("Who's NOT In");
JPanel  bottomPanel         = new JPanel(); 	
JButton showWhosInButton    = new JButton("show Who's In");
JButton showWhosNotInButton = new JButton("show Who's Not In");
JList<String> whosInList    = new JList<String>();
JList<String> whosNotInList = new JList<String>();
JButton sendPrivateButton   = new JButton("Send Private To");
JButton saveMessageButton   = new JButton("Save Message For");
JButton clearWhosInButton   = new JButton("CLEAR ALL SELECTIONS");
JButton clearWhosNotInButton= new JButton("CLEAR ALL SELECTIONS");

//Picture Button
JButton previewPicturesButton = new JButton("Preview Pictures To Send");
File localDirectory 			= new File(System.getProperty("user.dir"));
Vector<String> pictureFiles 	= new Vector<String>();
JList<ImageIcon> myPicturesList = new JList<ImageIcon>(); 
JScrollPane pictureScrollPane 	= new JScrollPane(myPicturesList);
JFrame myPictureListWindow		= new JFrame("Put pictures to send in " + localDirectory);
JLabel   pictureLabel	     	= new JLabel("Select a picture.");
JButton clearPicSelectButton   = new JButton("CLEAR SELECTION.");


// CONSTRUCTORS------------------------------------------------
public PictureChatClient() // no-parm CTOR is provided
    {                     // for "convenience".
	this("","","","",""); // (jar initiation will
    }                     // use this form.)

public PictureChatClient(String chatName,
		                String password,
		                String newPassword,
		                String serverAddress,
		                String serverPort) 
	{ 
	// Copy parms to join text fields on the GUI
	chatNameTF.setText(chatName);
	passwordTF.setText(password);
	newPasswordTF.setText(newPassword);
	serverAddressTF.setText(serverAddress);
	serverPortTF.setText(serverPort);

	// GUI-build  
    topPanel.setLayout(new GridLayout(2,1));     // rows/cols
    upperTopPanel.setLayout(new GridLayout(1,6));// rows/cols
    lowerTopPanel.setLayout(new GridLayout(1,6));// rows/cols
    upperTopPanel.add(chatNameLabel);     // 1
	upperTopPanel.add(passwordLabel);     // 2
    upperTopPanel.add(newPasswordLabel);  // 3
    upperTopPanel.add(serverAddressLabel);// 4
    upperTopPanel.add(serverPortLabel);   // 5
	upperTopPanel.add(leaveButton);       // 6
    lowerTopPanel.add(chatNameTF);     // 1        
    lowerTopPanel.add(passwordTF);     // 2
    lowerTopPanel.add(newPasswordTF);  // 3
    lowerTopPanel.add(serverAddressTF);// 4
    lowerTopPanel.add(serverPortTF);   // 5
	lowerTopPanel.add(joinButton);     // 6
	topPanel.add(upperTopPanel); // now put the upper and lower panels in  
	topPanel.add(lowerTopPanel); // the first and second row of topPanel.
	chatWindow.getContentPane().add(topPanel,"North");

	chatWindow.getContentPane().add(splitPane,"Center"); // add splitPane to middle of window

	bottomPanel.add(sendButton);
	bottomPanel.add(showWhosInButton);
	bottomPanel.add(showWhosNotInButton);
	bottomPanel.add(previewPicturesButton);
    chatWindow.getContentPane().add(bottomPanel,"South");	

    // Build MENU and add to application window 
    pullDownMenu.add(horizontalSplit);
    pullDownMenu.add(verticalSplit);
    menuBar.add(pullDownMenu);
    chatWindow.setJMenuBar(menuBar);

	// Sign up for event notification from "active" GUI objects
    joinButton.addActionListener(this);  // Sign up for event notification    
    leaveButton.addActionListener(this); // Sign up for event notification    
	sendButton.addActionListener(this);  // Sign up for event notification    
    horizontalSplit.addActionListener(this); 
    verticalSplit.addActionListener(this);
    showWhosInButton.addActionListener(this);
    showWhosNotInButton.addActionListener(this);
    previewPicturesButton.addActionListener(this);
    
	// Set attributes of the GUI objects
	chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	splitPane.setDividerLocation(150); 
	sendButton.setBackground(Color.yellow);
	sendButton.setForeground(Color.red);
	joinButton.setBackground(Color.green);
	leaveButton.setBackground(Color.red);
	showWhosInButton.setBackground(Color.blue);
	showWhosInButton.setForeground(Color.white);
	showWhosNotInButton.setBackground(Color.cyan);
	
	outChatTextArea.setEditable(false); // keep cursor out
	inChatTextArea.setFont (new Font("default",Font.BOLD,20));
	outChatTextArea.setFont(new Font("default",Font.BOLD,20));
    inChatTextArea.setFont (new Font("default",Font.BOLD,20));
    outChatTextArea.setFont(new Font("default",Font.BOLD,20));
    inChatTextArea.setLineWrap(true);
    outChatTextArea.setLineWrap(true);
	inChatTextArea.setWrapStyleWord(true);
	outChatTextArea.setWrapStyleWord(true);
    inChatTextArea.setText("Enter chat to be sent here.");
    outChatTextArea.setText("You can move the separator bar!");
    sendButton.setEnabled(false);
    leaveButton.setEnabled(false);
    inChatTextArea.setEditable(false);
    whosInWindow.setSize(200,300); 			// width, height   
    whosNotInWindow.setSize(200,300);   
    whosInWindow.setLocation(800,0);   
    whosNotInWindow.setLocation(800,300);   
    showWhosInButton.setEnabled(false);
    showWhosNotInButton.setEnabled(false);
    
	// Show the main chat window
	chatWindow.setSize(800,500);
	chatWindow.setVisible(true); // show the graphics window

	// Compose the whosIn windows
	whosInWindow.getContentPane().add(whosInList,"Center");
	whosNotInWindow.getContentPane().add(whosNotInList,"Center");
	whosInWindow.getContentPane().add(clearWhosInButton,"North");
	whosInWindow.getContentPane().add(sendPrivateButton,"South");
	whosNotInWindow.getContentPane().add(clearWhosNotInButton,"North");
	whosNotInWindow.getContentPane().add(saveMessageButton,"South");
	clearWhosInButton.addActionListener(this);
	clearWhosNotInButton.addActionListener(this);
	sendPrivateButton.addActionListener(this);
	saveMessageButton.addActionListener(this);
	clearWhosInButton.setBackground(Color.yellow);
	clearWhosNotInButton.setBackground(Color.yellow);
    sendPrivateButton.setBackground(Color.green);
	saveMessageButton.setBackground(Color.cyan);
    
	//Build the myPictureListWindow
	myPictureListWindow.getContentPane().add(clearPicSelectButton, "North");
	myPictureListWindow.getContentPane().add(pictureScrollPane, "Center");
	myPictureListWindow.getContentPane().add(pictureLabel, "South");
	clearPicSelectButton.addActionListener(this);
	pictureLabel.setForeground(Color.red);
	myPicturesList.setSelectionMode(0); 	// single-select
	myPictureListWindow.setSize(300,600);   
	myPictureListWindow.setLocation(1000, 0);
    previewPicturesButton.setEnabled(false);
	System.out.println("Local directory is " + localDirectory);
	
	myPicturesList.addListSelectionListener(this);
	}


public void actionPerformed(ActionEvent ae)
	{ 
	//String whosInListSelection = whosInList.getSelectedValue();
	//String whosNotInListSelection = whosNotInList.getSelectedValue();
    //if (ae.getSource() == whosInListListener) whosInList.getSelectedValue();
    //if (ae.getSource() == whosNotInListListener) whosNotInList.getSelectedValue();
    if (ae.getSource() == joinButton) join();
    
    if (ae.getSource() == leaveButton)leave();
    
    if (ae.getSource() == sendButton) send();
    
    if (ae.getSource() == horizontalSplit)
	   splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    
    if (ae.getSource() == verticalSplit)
	   splitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    
    if (ae.getSource() == showWhosInButton)    whosInWindow.setVisible(true);
    
    if (ae.getSource() == showWhosNotInButton) whosNotInWindow.setVisible(true);
    
    if (ae.getSource() == clearWhosInButton) 
    	{
    	whosInList.clearSelection();
    	}
    
    if (ae.getSource() == clearWhosNotInButton) 
    	{
    	whosNotInList.clearSelection();
    	}
    
    if (ae.getSource() == sendPrivateButton)
    { 
    	List<String> privateMessageList = whosInList.getSelectedValuesList();
    	if (privateMessageList.isEmpty()) 
    	{
    		outChatTextArea.append(newLine + "NO PRIVATE RECIPIENTS SELECTED");
    		return;
    	}
    	Vector<String> privateMessageRecipients = new Vector<String>(privateMessageList);
	    String message = inChatTextArea.getText().trim();
	    if (message.length() == 0)
	      {
	    	outChatTextArea.append(newLine + "NO PRIVATE MESSAGE ENTERED TO SEND");
	    	return;
	      }
	    privateMessageRecipients.insertElementAt(message,0);
	    try
	    {
	    	oos.writeObject(privateMessageRecipients);
	    	inChatTextArea.setText(""); // clear sent msg
	   	}
	    catch(IOException ioe){}
    }
    
 if (ae.getSource() == saveMessageButton)
    { 
	 	List<String> saveMessageList = whosNotInList.getSelectedValuesList();
	 	if (saveMessageList.isEmpty())
	    {
	 		outChatTextArea.append(newLine + "NO SAVE RECIPIENTS SELECTED");
	 		return;
  	  	}
	 	Vector<String> savedMessageRecipients = new Vector<String>(saveMessageList);
	 	String message = inChatTextArea.getText().trim();
	 	if (message.length() == 0)
	      {
	 		outChatTextArea.append(newLine + "NO SAVE MESSAGE ENTERED");
	 		return;
	      }
	 	savedMessageRecipients.insertElementAt(message,0);
	 	try
	 	{
	 		oos.writeObject(savedMessageRecipients);
	 		inChatTextArea.setText("");		//clear saved msg
	 	}
	 	catch(IOException ioe){}
    }
 
 if (ae.getSource() == previewPicturesButton) 
    {
	 	String[] listOfFiles = localDirectory.list();
    	for(String i : listOfFiles) 
    	{
    		String mimetype= new MimetypesFileTypeMap().getContentType(i);
            String type = mimetype.split("/")[0];
            if(type.equals("image")) pictureFiles.addElement(i);
    	}
    	
    	if (pictureFiles.isEmpty())
    	   {
    			System.out.println("No pictures are found in the local directory.");
    			return;
    	   }
    	Vector<ImageIcon> imageIcons = new Vector<ImageIcon>();
    	for (String pictureFileName : pictureFiles)
        {
    		ImageIcon picture = new ImageIcon(pictureFileName,pictureFileName);	
    		// filename,description   	    	 myPictures.add(picture);
    		imageIcons.add(picture);
        }
    	myPicturesList.setListData(imageIcons);
    	myPictureListWindow.setVisible(true);
    	}
 
 if (ae.getSource() == clearPicSelectButton) 
	{
	 	myPicturesList.clearSelection();
	 	pictureLabel.setText("Select a picture");
	}
    
	}

public void run()//new app thread becomes receive thread
	{ 
	receive();
	}

// JOIN-------------------------------------------------
private void join()
  {
  System.out.println("Entering join()."); 
  // Edit the join data in the GUI text fields
  String chatName      = chatNameTF.getText();
  String password      = passwordTF.getText();
  String newPassword   = newPasswordTF.getText();
  String serverAddress = serverAddressTF.getText();
  String serverPort    = serverPortTF.getText();
  if (chatName.contains(" ")
   || password.contains(" ")
   || newPassword.contains(" ")
   || serverAddress.contains(" ")
   || serverPort.contains(" "))
     {
	 outChatTextArea.setText("join data fields may not contain blanks");
	 return; // stop the join process
     }
  if ((chatName.length() == 0)
   || (password.length() == 0)
   || (serverAddress.length() == 0)
   || (serverPort.length() == 0))
	  {
	  outChatTextArea.setText("All join data fields are required except newPassword");
	  return; // stop the join process
	  }
  
  int serverPortNumber;
  try {
	  serverPortNumber = Integer.parseInt(serverPort);
	  if ((serverPortNumber < 1000) || (serverPortNumber > 9999))
         {
	     outChatTextArea.setText("serverPort must be in the range 1000-9999");
		 return; // stop the join process
	     }
      }
  catch(NumberFormatException nfe)
      {
	  outChatTextArea.setText("serverPort must be numeric");
	  return; // stop the join process
      }
  
  // Join data looks good, so let's try to connect and join!
  try {
      s = new Socket(serverAddress,serverPortNumber);
      outChatTextArea.setText("Connected to server!");
      oos = new ObjectOutputStream(s.getOutputStream());
      if (newPassword.length() == 0) // new pw not specified
         oos.writeObject(chatName + " " + password); // send JOIN 1st msg to server
       else // newPassword was specified
         oos.writeObject(chatName + " " 
             		   + password + " "
         	           + newPassword);
      ois = new ObjectInputStream(s.getInputStream());
      String serverReply = (String) ois.readObject();
      outChatTextArea.append(newLine + serverReply);
      if (serverReply.startsWith("Welcome"))
        	System.out.println("JOIN was successful");
       else // our join request was rejected by the server
        {
       	System.out.println("JOIN failure");
       	return; // this stops the join process
        }
	  }
	catch(Exception e)
	  {
	  String errorMessage = e.toString(); 	
	  System.out.println("Connection failure. " + errorMessage);
	  outChatTextArea.setText("Cannot connect to chat room server"
	       + newLine + errorMessage);
	  return; // STOP - don't start receive thread or
	  }       // flip GUI to CHAT state.
	
  chatWindow.setTitle(chatName + "'s Chat Room!"	
	 + "   Close the window to leave the chat room.");
  
  new Thread(this).start(); // begin execution in run()

  // Change GUI state from JOIN to CHAT
  inChatTextArea.setEditable(true);//allow chat message entry
  joinButton.setEnabled(false);    //shouldn't join in chat mode!
  sendButton.setEnabled(true);     //to send entered chat!
  leaveButton.setEnabled(true);    //in joined, need to be able to leave!
  chatNameTF.setEditable(false);   //In CHAT mode, the
  passwordTF.setEditable(false);   //user should NOT be
  newPasswordTF.setEditable(false);//able to change
  serverAddressTF.setEditable(false);//any of the 
  serverPortTF.setEditable(false); //join field values!
  showWhosInButton.setEnabled(true);
  showWhosNotInButton.setEnabled(true);
  previewPicturesButton.setEnabled(true);
  }


//SEND-------------------------------------------------
private void send()
  {	
    String chatMessage	= inChatTextArea.getText().trim();
    boolean picSelect 	= myPicturesList.isSelectionEmpty();
    if (chatMessage.length() == 0 && picSelect ) {
    	outChatTextArea.append(newLine + "No message entered or picture selected.");
    	return; 
    }
    try 
    {
    	if (chatMessage.length() != 0 && picSelect)
    	{
    		oos.writeObject(chatMessage);
        	inChatTextArea.setText("");
        	return;
    	}
    	if (!picSelect)
    	{
    		ImageIcon pictureFile = myPicturesList.getSelectedValue();
    		String chatName = chatNameTF.getText();
    		String pictureFileString = pictureFile.toString();
    		String description = (chatName + " " + pictureFileString + " " + chatMessage);
    		pictureFile = new ImageIcon(pictureFileString,description);	
    		oos.writeObject(pictureFile);
    		inChatTextArea.setText("");
    		myPicturesList.clearSelection();
    		pictureLabel.setText("Select a picture.");
    		pictureFile = new ImageIcon(pictureFileString,pictureFileString);
    	}
    }
    catch(IOException ioe)
        {
    	outChatTextArea.setText(ioe.toString());
        }
  }

//RECEIVE-------------------------------------------------
private void receive()
  {
  System.out.println("Entering receive()."); 	
	try {
	    while(true) // capture receive thread in a "do forever" loop
	         {
	    		Object somethingFromTheServer = ois.readObject(); // wait for server to send.
	    	 if (somethingFromTheServer instanceof String)//syntax: pointer to object on left,a TYPE on right
	    	   {
	    		 String chatMessage = (String) somethingFromTheServer;
	    		 outChatTextArea.append(newLine + chatMessage); // add new message to bottom of existing text.
	             outChatTextArea.setCaretPosition(outChatTextArea.getDocument().getLength()); 
	    	   }
	    	 else if (somethingFromTheServer instanceof Vector) // watch for a list of objects
	         {
	    		 Vector<String> clientList = (Vector<String>) somethingFromTheServer;
	    		    String myChatName = chatNameTF.getText().toUpperCase();
	    		    if (clientList.contains(myChatName))
	    		        whosInList.setListData(clientList);
	    		     else // no find
	    		        whosNotInList.setListData(clientList);
	         }
	    	 else if (somethingFromTheServer instanceof ImageIcon) 
	         {
	    		 ImageIcon imageIcon = (ImageIcon) somethingFromTheServer;
	    		 String description = imageIcon.getDescription();
	    		 Image image = imageIcon.getImage();
	    		 JFrame receivedPictureDisplayWindow = new JFrame();
	    		 receivedPictureDisplayWindow.setVisible(true);
	    		 outChatTextArea.append(newLine + description);
	    		 receivedPictureDisplayWindow.setTitle(description);	  
	    		 JPanel refreshingPicturePanel = new RefreshingPicturePanel(image);
	    		 receivedPictureDisplayWindow.getContentPane().add(refreshingPicturePanel,"Center");
	    		 receivedPictureDisplayWindow.setSize(600, 300);
	    		 receivedPictureDisplayWindow.setLocation(0, 520);
	         }
	    	 else System.out.println("Unexpected Object type received from server: "
                     + somethingFromTheServer);
             
	         }
	    }
	catch(Exception e)
	    {
		outChatTextArea.setText("Error receiving messages from the server."
		   + newLine + "Restart the client and rejoin the server to continue.");
	    }
  }//thread returns to run() and then to Thread object and is terminated.

//LEAVE -------------------------------------------------
private void leave()
  {
  System.out.println("Entering leave()."); 	
  try {	
      ois.close(); // hang up!
      }
  catch(IOException ioe)
      {
	  // Do nothing. (Socket may already be down.)
	  // Anyway, we want to handle this in receive().
      }
  // In any event, the user has requested to leave the
  // chat room, so we need to change the GUI from 
  // CHAT state to re-JOIN state (this is basically
  // the opposite of what is set in join())
  inChatTextArea.setEditable(false);//don't allow chat message entry
  joinButton.setEnabled(true);     //be able to join
  sendButton.setEnabled(false);    //no need to send!
  leaveButton.setEnabled(false);   //we are left!
  chatNameTF.setEditable(true);   //In JOIN mode, the
  passwordTF.setEditable(true);   //user should  be
  newPasswordTF.setEditable(true);//able to change
  serverAddressTF.setEditable(true);//any of the 
  serverPortTF.setEditable(true); //join field values!
  showWhosInButton.setEnabled(false);
  showWhosNotInButton.setEnabled(false);
  previewPicturesButton.setEnabled(false);
  }


public void valueChanged(ListSelectionEvent lse) 
{
	if (lse.getValueIsAdjusting()) return;		//user is still selecting!
	ImageIcon selectedPicture = myPicturesList.getSelectedValue();
	if (selectedPicture == null) return; 		// selection was removed!
	String pictureDescription = selectedPicture.getDescription();
	pictureLabel.setText(pictureDescription);
}

}
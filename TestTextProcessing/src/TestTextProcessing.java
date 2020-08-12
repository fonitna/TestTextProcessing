import javax.swing.Timer;
import javax.swing.*;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.io.*;
import java.util.*;
import java.lang.String;
import de.uni_leipzig.asv.toolbox.ChineseWhispers.algorithm.ChineseWhispers;
import de.uni_leipzig.asv.toolbox.viterbitagger.Tagger;
import de.uni_leipzig.asv.toolbox.baseforms.Zerleger2;
import de.uni_leipzig.asv.utils.Pretree;
import de.texttech.cc.*;
import te.indexer.*;
import te.utils.*;
import JLanI.kernel.DataSourceException;
import JLanI.kernel.LanIKernel;
import JLanI.kernel.Request;
import JLanI.kernel.RequestException;
import JLanI.kernel.Response;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;
import org.json.JSONArray;      // JSON library from http://www.json.org/java/
import org.json.JSONObject;


import org.apache.tika.exception.TikaException;  
import org.apache.tika.metadata.Metadata;  
import org.apache.tika.parser.AutoDetectParser;  
import org.apache.tika.parser.ParseContext;  
import org.apache.tika.parser.Parser;  
import org.apache.tika.sax.BodyContentHandler;  
import org.xml.sax.ContentHandler;  
import org.xml.sax.SAXException;

public class TestTextProcessing extends JFrame implements ActionListener{
	
	private Timer t;
	private int set_time_see_output = 10000; //50
	
	private static Container c; 
	private JTextArea listEntry;
	private JLabel lname;
	private JButton btnShowRing;
	private JPanel menuPanel;
	private JScrollPane scroller;
	
	private JFrame showR;
	private JPanel panelShowR;

	Font font1 = new Font("Courier New", Font.BOLD, 25);
	Font font2 = new Font("Courier New", Font.BOLD, 12);
	Font font3 = new Font("Courier New", Font.BOLD, 20);
	
	Cooccs cooccs = new Cooccs();
	Cooccs mycooccs;
	
	String baseDocDir = "";
	String inputDirPath = "";
	String satzDirPath = "";
	static String outputDirPath = "";
	String indexesDirPath = "";
	String centroidDirPath = "";

	
	public TestTextProcessing(String basedocdir) {
		
		super("Text Clusting Simulation");
		c = getContentPane();
		c.setLayout(new FlowLayout());
		
		showR = new JFrame("Text Clustering Simulation");
		
		/*btnShowRing = new JButton("Show Nodes");
		btnShowRing.addActionListener(this);
		btnShowRing.setFont(font3);
		
		menuPanel = new JPanel();
		menuPanel.setPreferredSize(new Dimension(1100,40));
		menuPanel.setBorder(BorderFactory.createBevelBorder(0));
		menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.PAGE_AXIS));
		menuPanel.add(btnShowRing);
		
		c.add(menuPanel);	*/
			
		JLabel lname = new JLabel("Text Clusting");
		lname.setFont(font1);
		c.add(lname);
		
		
		listEntry = new JTextArea(45,140);
		listEntry.setFont(font2);
		listEntry.setBackground(new Color(230,250,255));
		listEntry.setEditable(false);
		
		scroller = new JScrollPane(listEntry);
		c.add(scroller);
		
		//////////////////////////////////////////////////////////////////////////////////
		
		baseDocDir=basedocdir;
		inputDirPath = baseDocDir + "/input/";
		satzDirPath = baseDocDir + "/satzfiles/";
		outputDirPath = baseDocDir + "/output/";	
		centroidDirPath = baseDocDir + "/centroid/";
		  
		File outputDir = new File (outputDirPath);	 
		cleanDir(outputDir);
		 
		LanIKernel.propertyFile="config/lanikernel";
		
		System.out.println("Set paths directory complease!!!");
		
		t = new Timer(set_time_see_output, this);
		t.start();
		
	}
	
	public void stopTime() {
		t.stop();
	}
	public void startTime() {
		t.start();
	}
	
	private void cleanDir(File d){
		if(d.isDirectory()){
  			File[] files = d.listFiles();
  			for(int i=0; i<files.length; i++){
  				files[i].delete();
  			}
		}
	}
	
	public void preprocessing() {
		convertFiles(); //Convert input file formats into txt 	  
		System.out.println(">>>> extractSentenceFileCorpus");
		extractSentenceFileCorpus();  
	}
	
	public void createGraphDB() {
		File satzDir = new File (satzDirPath);
		if(satzDir.isDirectory()){
			File[] files = satzDir.listFiles();
			for(int i=0; i<files.length; i++){
				File curFile = new File(files[i].getAbsolutePath());
				if(curFile.isFile()){
					System.out.println("Filling DB with: " +curFile.getAbsolutePath());
  					System.out.println("");
  					
  					mycooccs = new Cooccs(curFile, true);
  					
  				}
  			}
  		}
	}
	
	public void convertFiles() {
		System.out.println("convert Files...");
		File inDir = new File (inputDirPath);
  	  
		if(inDir.isDirectory()){
			File[] files = inDir.listFiles();
			// create subdirectory to contain the converted txt files:
			File txtDir = new File(inDir.getAbsolutePath(), "txt");
			if(!txtDir.exists()){
				try{
					txtDir.mkdir();
				}catch(Exception e){e.printStackTrace();}
			} else { 
				cleanDir(txtDir);
			}
			// now loop through them and convert them
			for(int i=0; i<files.length; i++){	
				File curFile = new File(files[i].getAbsolutePath());
				if(curFile.isFile()){
					try{
						System.out.println("Convert " +curFile.getAbsolutePath());
						String curtext = "";
						InputStream inputStream = null;
						try {  
							Parser parser = new AutoDetectParser();  
							ContentHandler contentHandler = new BodyContentHandler(10*1024*1024);  
							Metadata metadata = new Metadata();  
						
							inputStream = new FileInputStream(curFile);
						
							parser.parse(inputStream, contentHandler, metadata, new ParseContext());  
						
							System.out.println("content: " + contentHandler.toString());  
							curtext = contentHandler.toString();
						
						} catch (IOException e) {  
							e.printStackTrace();  
						} catch (TikaException e) {  
							e.printStackTrace();  
						} catch (SAXException e) {  
							e.printStackTrace();  
						} finally {  
							if (inputStream != null) {  
								try {  
									inputStream.close();  
								} catch (IOException e) {  
									e.printStackTrace();  
								}  
							}  
						}  

						File destFile = new File(txtDir, curFile.getName()+".txt");
						
						FileWriter f = new FileWriter (destFile);
						if (curtext!=null) {
							f.write(curtext); 
						}
						f.close();
					}catch(Exception e){e.printStackTrace();}
				}
			} //for all files
		}
	}
	
	public void extractSentenceFiles() {
		File satzDir = new File (satzDirPath);
		cleanDir(satzDir);
		File inDir = new File (inputDirPath+"txt");
		if(inDir.isDirectory()){
			File[] files = inDir.listFiles();
			// now loop through them
			for(int i=0; i<files.length; i++){
				File curFile = new File(files[i].getAbsolutePath());
				if(curFile.isFile()){
					int language = getLanguage (curFile);
					String[] cmdArray = new String[5];
					if (language==0)
						cmdArray[0] = "-L de";
					if (language==1)
						cmdArray[0] = "-L en";
					cmdArray[1] = "-d"+curFile.getName(); //cmdArray[1] = "-dsatz"+(i+1);
					cmdArray[2] = "-p./"+satzDirPath;                    // /data/satzfiles";
  				
  					cmdArray[3] = "-a./resources/abbreviation/abbrev.txt";
  					cmdArray[4] = curFile.getAbsolutePath(); 
  							
  					Text2Satz.main(cmdArray);
  					
				}
  			} //for all files
  		}   
    }
	
	public void extractSentenceFileCorpus() {
		File satzDir = new File (satzDirPath);
		cleanDir(satzDir);
		File inDir = new File (inputDirPath+"txt");
		if(inDir.isDirectory()){
			File[] files = inDir.listFiles();
			String[] cmdArray = new String[4+files.length];
	
			cmdArray[1] = "-dcorpus";   //cmdArray[1] = "-dsatz";
			cmdArray[2] = "-p./"+satzDirPath;                     //data/satzfiles";  
		
			cmdArray[3] = "-a./resources/abbreviation/abbrev.txt";
		
			
			for(int i=0; i<files.length; i++){
				File curFile = new File(files[i].getAbsolutePath());
				
				if(curFile.isFile()){
					if (i==0) {	
						int language = getLanguage (curFile);						
						if (language==0)
	    					cmdArray[0] = "-L de";					
						if (language==1)
	    					cmdArray[0] = "-L en";	
					}
					
					cmdArray[i+4] = files[i].getAbsolutePath();

				}
			}
			if (files.length>0)
				Text2Satz.main(cmdArray);
			
			System.out.println(Text2Satz.getCountLineAll().toString());
		}
	}
	
	public int getLanguage(File satzFile) {
		String alltext=""; String lineorig;
		try {
			FileInputStream fin =  new FileInputStream(satzFile);
			BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
			while ((lineorig = myInput.readLine()) != null){
				alltext = alltext + lineorig;
			}    	  
			myInput.close();
			fin.close();
		} catch (Exception ex) {}
		
		// get the lanikernel-object
		LanIKernel lk=null;
		try {
			lk = LanIKernel.getInstance();
		} catch (DataSourceException e) {}

		// fill a request object
		Request req = null;
		try {
			// new request object
			req = new Request();
			// setting up the sentence/data
			req.setSentence(alltext);
			// dont collect useless information about the evaluation
			req.setReduce(true);
			// evaluate only log(datalength>=30) words for better speed
			req.setWordsToCheck(Math.max( (int) Math.sqrt(alltext.length()), 30));
		} catch (RequestException e1) {}

		// evaluate the request
		Response res = null;
		try {
			// the evaluation call itself
			res = lk.evaluate(req);
		} catch (Exception e2) {}

		// search the winner language
		HashMap tempmap = res.getResult();
		double val = 0.0;
		String key = null, winner = "REST";
		for (Iterator iter = tempmap.keySet().iterator(); iter.hasNext();) {
			key = (String) iter.next();
			if (((Double) tempmap.get(key)).doubleValue() > val) {
				winner = key;
				val = ((Double) tempmap.get(key)).doubleValue();
			}
		}

		int languagevalue = 1;
		if (winner.equalsIgnoreCase("de")) {
			languagevalue = Parameters.DE;
			System.out.println("Language is DE\n");
		} else if (winner.equalsIgnoreCase("en")) {
			languagevalue = Parameters.EN;
			System.out.println("Language is EN\n");
		} else if (winner.equalsIgnoreCase("REST")) {
			
			languagevalue = Parameters.EN;
			System.out.println("Cannot determine language, taking EN\n");
			
		}
			
		return languagevalue;
	}
		
	int findEntry(List termlist, String query) {
		int index=-1;
		for (int i=0; i<termlist.size(); i++)
			if (termlist.get(i).equals(query))
	  			index=i;
		return index;
	}
	
	//Pagerank berechnen
    List calculatePageRanks(List termlist, float [][] curgraph) {
    	List  pageranks_list = new Vector();        
    	float [] pagerank = new float[termlist.size()];
    	float d= 0.85f;  
                    
    	for (int i=0;i<termlist.size();i++)
    	{
    		pagerank[i]=(1-d);
    	}
                    
    	float [] out = new float [termlist.size()];
    	for (int i = 0; i < termlist.size(); i++)
    	{
    		out[i]=0;
    		for (int j = 0; j < termlist.size(); j++) {
    			if (curgraph[i][j]!=0)
    				out[i]= out[i] + 1; //curgraph[i][j]; //+1
    		}
    	}
    	//Pageranks calculate
    	for(int sz=0;sz<25;sz++){
    		for (int j=0;j<termlist.size();++j){
    			float prj = 0;
    			for (int i=0; i< termlist.size();i++){
    				if (curgraph[i][j]!=0){
    					prj=prj+  (  ((pagerank[i]*curgraph[i][j])/out[i])     );
    				}  
    			}
    			pagerank[j] = (1-d) + d*prj; 
    		}
    	} //for sz
    	float sum = 0;
    	float maxpr = 0;
    	int maxi =0;
    	for (int i=0; i< termlist.size();i++){
    		if (pagerank[i]>maxpr) {
    			maxpr=pagerank[i];
    			maxi=i;
    		}
    		sum = sum + pagerank[i];
    	}
    	for (int i=0; i< termlist.size();i++){
    		float value=0;
    		value= pagerank[i]/pagerank[maxi];
    		Word curWord = new Word((String)termlist.get(i), 0);
    		curWord.setSig(value);
    		pageranks_list.add(curWord);
    	}
    	Collections.sort(pageranks_list);
    	return pageranks_list;
    }
    
    public void writeLinesToFile(String filename, String[] linesToWrite, boolean appendToFile) {
    	
    	PrintWriter pw = null;
    	try {
    		if (appendToFile) {
    			//If the file already exists, start writing at the end of it.
    			pw = new PrintWriter(new FileWriter(filename, true));
    		}else {
    			pw = new PrintWriter(new FileWriter(filename));
    		}

    		for (int i = 0; i < linesToWrite.length; i++) {
    			pw.println(linesToWrite[i]);
    		}
    		pw.flush();
    	}catch (IOException e) {
    		e.printStackTrace();
    	}	
    	finally {
    		//Close the PrintWriter
    		if (pw != null)
    			pw.close();
    	}
    }
    
  //HITS berechnen
    List [] calculateHITS(List termlist, float [][] curgraph) {
    	List hits_hubs_list = new Vector();
    	List hits_auths_list = new Vector();
    	float [] hits_hubs = new float[termlist.size()];
    	float [] hits_auths = new float[termlist.size()];
    
    	for (int i=0;i<termlist.size();i++){
    		hits_auths[i]=0.15f;
    		hits_hubs[i]=0.15f;
    	}

    	//Main loop
    	for(int step=0;step<50;step++){
    		float norm=0;    
    		//Authorities  
    		for (int j=0;j<termlist.size();++j){
    			for (int i=0; i< termlist.size();i++){
    				if (curgraph[i][j]!=0){
    					hits_auths[j]=hits_auths[j]+(hits_hubs[i]*curgraph[i][j]);
    				}  
    			}
    			norm = norm + (hits_auths[j]*hits_auths[j]);
    		}
             
    		norm = ((Double)Math.sqrt(norm)).floatValue();
             
    		for (int j=0;j<termlist.size();++j){
    			hits_auths[j]=hits_auths[j]/norm;
    		}
    		norm = 0;
    		
    		//Hubs   
    		for (int j=0;j<termlist.size();++j){
    			for (int i=0; i< termlist.size();i++){
    				if (curgraph[j][i]!=0){
    					hits_hubs[j]=hits_hubs[j]+(hits_auths[i]*curgraph[j][i]);
    				}  
    			}
    			norm = norm + (hits_hubs[j]*hits_hubs[j]);
    		}
    		
    		norm = ((Double)Math.sqrt(norm)).floatValue();
             
    		for (int j=0;j<termlist.size();++j){
    			hits_hubs[j]=hits_hubs[j]/norm;
    		}
    	} //for sz
         
    	for (int i=0; i< termlist.size();i++){
    		float value = 0;
    		value = hits_auths[i];
      	   
    		Word curWord = new Word((String)termlist.get(i), 0);
    		curWord.setSig(value);
    		hits_auths_list.add(curWord);
    	}
         
    	for (int i=0; i< termlist.size();i++){
    		float value = 0;
    		value = hits_hubs[i];
    		Word curWord = new Word((String)termlist.get(i), 0);
    		curWord.setSig(value);
    		hits_hubs_list.add(curWord);
    	}
         
    	Collections.sort(hits_auths_list);
    	Collections.sort(hits_hubs_list);
         
    	List [] returnlist = new List[2];
    	returnlist[0] = hits_auths_list;
    	returnlist[1] = hits_hubs_list;
                    
    	return returnlist;
    }
    
    public void showAllDB() {
    	
    	cooccs.showDB();
    }
    public void showWordInEachCluster() {
    	cooccs.showWordInEachCluster();
    }
    
    public void shutdownDB() {
    	cooccs.shutdownDB();
    }
    
    public void actionPerformed(ActionEvent event) {
		String msg = cooccs.printListEntry();
		listEntry.setText(msg);
		/*if (event.getSource() == btnShowRing) {
			showNewFrame();
		}*/
		showR.repaint();
	}
    public void findCentroid() {
    	Vector q = new Vector();
    	q.add("klaus");
    	q.add("fröhlich");
    	q.add("möglichkeit");
    	q.add("käufer");
    	System.out.println(cooccs.findCentroid(q));
    }
    
    /*private void showNewFrame() {
		showR.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		showR.setVisible(true);
		showR.setSize(1100, 800);
		panelShowR = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.fillOval(100, 100, 50, 50);
            }
        };
        showR.add(panelShowR);

        showR.validate(); // because you added panel after setVisible was called
        showR.repaint(); // because you added panel after setVisible was called

	}*/

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestTextProcessing textprocessing = new TestTextProcessing("data"); //base directory: data or download 
	
		textprocessing.setSize(1100, 800);
		textprocessing.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		textprocessing.setVisible(true);
		
		String choice;
		char ch='q';
		Scanner scan = new Scanner(System.in);
		do {
			System.out.println("==========Menu===========");
			System.out.println("(P)reprocessing");
			System.out.println("(C)reate Database (and Clustering)");
			System.out.println("(S)how All Data in Database");
			System.out.println("(W)ord-Show of Each Cluster");
			System.out.println("(F)ind Centroid");
			System.out.println("(0) Stop Time");
			System.out.println("(1) Start Time");
			System.out.println("(E)xit");
			System.out.println("=========================");
			try {
				System.out.print("Enter Menu: ");
				choice = scan.nextLine();
				ch = choice.charAt(0);
			}catch(RuntimeException ex){
				System.out.println("Invalid input");
			}
			
			
			if(ch=='P') ch='p';
			if(ch=='C') ch='c';
			if(ch=='S') ch='S';
			if(ch=='E') ch='e';
			if(ch=='W') ch='w';
			if(ch=='F') ch='f';
			
			switch(ch) {
				case 'p':
					System.out.println("Starting to PreProcessing...");
					textprocessing.preprocessing();
					break;
				case 'c':
					textprocessing.createGraphDB();
					break;
				case 's':
					textprocessing.showAllDB();
					break;
				case 'w':
					textprocessing.showWordInEachCluster();
					break;
				case 'e':
					textprocessing.shutdownDB();
					System.out.println("\nExit Program...\n");
					break;
				case 'f':
					textprocessing.findCentroid();
					break;
				case '0':
					textprocessing.stopTime();
					break;
				case '1':
					textprocessing.startTime();
					break;
				/*case 'v':
					Vector v[] = new Vector[5];
					for(int j=0;j<5;j++) {
						v[j]=new Vector();
					}
					int i = 0;
					v[i].add("aaa");
					v[i].add("bbb");
					v[i].add("ccc");
					v[i].add("ddd");
					System.out.println("before :: "+v[i].toString());
					v[i].remove(2);
					v[i].add(0, "fff");
					System.out.println("after :: "+v[i].toString());
					break;*/
				default:
					System.out.println("!!! Don't have this menu !!!");
					break;					
			}
		}while(ch!='e');
	
	}

}

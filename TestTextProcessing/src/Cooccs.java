
import java.util.*;
import java.io.*;
import java.lang.*;

import te.indexer.*;
import te.utils.*;
//import textMine.GraphDB.Labels;
//import textMine.GraphDB.RelationshipTypes;
//import textMine.GraphDB.Labels;
//import Labels;
//import RelationshipTypes;
import JLanI.kernel.DataSourceException;
import JLanI.kernel.LanIKernel;
import JLanI.kernel.Request;
import JLanI.kernel.RequestException;
import JLanI.kernel.Response;
import de.texttech.cc.Text2Satz;
import de.uni_leipzig.asv.toolbox.baseforms.Zerleger2;
import de.uni_leipzig.asv.toolbox.viterbitagger.Tagger;
import de.uni_leipzig.asv.utils.Pretree;
import scala.Int;

import org.json.JSONArray;      // JSON library from http://www.json.org/java/
import org.json.JSONObject;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.*;
//import org.neo4j.kernel.Traversal;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.TransactionWork;


public class Cooccs{
//	private final Driver driver = GraphDatabase.driver( "bolt://132.176.3.105:7687", AuthTokens.basic( "neo4j", "0" ) );
	private int numMultiplySigma=3;
	private FileWriter writer,writer_cid,writer_mini_cluster;
	//private int time=0;
	
	
	// number of sentences in the corpus:
	private int line;
	
	private int run=0;

	private String initialquery = "";
	
	private Set expansionterms = null; 
	
	int language=0;
	
	protected List lemmata;
	protected int length;
	protected Map index = null;
	
	  //reduce file for baseform

    String redbase_en = "./resources/trees/en-nouns.tree";
    String redbase_de = "./resources/trees/de-nouns.tree";
    
    //de-nouns.tree   en-nouns.tree
    
    //reduce file for splitting

    String red = "./resources/trees/grfExt.tree";

    //forward file

    String forw = "./resources/trees/kompVVic.tree";

    //backward file

    String back = "./resources/trees/kompVHic.tree";
	
    String tmFile = "./resources/taggermodels/deTaggerModel.model"; /* deTaggerModel.model*/
    String tmFile2 = "./resources/taggermodels/english.model"; /* enTaggerModel.model*/
	
    
    
    int maximumaddedpaths = 0;  //50
	int maximumpathstoadd = 5; //number of query terms to consider for centroid calculation
	
	
	

	private static boolean check_start = false;
	
	//for Evolving Centroids
	private static int max_cluster = 50000;	
	private static int max_document = 2000;
	private static Vector distilledText[] = new Vector[max_cluster+1]; //contains the text to be parsed (only nouns probably) --keep word that read in each cluster	
	
	
	private static int count_cluster = 1;
	private static int count_doc=0;
	
	private static Vector wordsEachDoc[] = new Vector[max_document]; //contains words of each document for find centroid is?
	
//	int current_cluster = count_cluster;
	
	private static String centroids[] = new String[max_cluster+1];
	
	private static double averageDistance[] = new double[max_cluster+1];
	private static double standardDeviation[] = new double[max_cluster+1];
	private static Vector numDistance[] = new Vector[max_cluster+1]; //keep distance of each word in each cluster between word and the position of centroid
	
	// this map contains a mapping term->Map::String->Double, i.e. each term has a number of co-occurring items
	// associated with it each of which is in turn associated with a co-occurrence frequency.
	private static Map cooccs[] = new Map[max_cluster+1];
	// maps words to their frequency:
	private static Map frequencies[] = new Map[max_cluster+1];
	
	private static String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
	private static File database = new File(db_path);
	private static GraphDatabaseService graphDB;
	
	//for recheck mini cluster to find to insert into big cluster
	private static Vector c_update_minicluster = new Vector();
	int sumMiniClusterRecheck=0;
	int numMemberForRecheck=0;
	int numClusterMoveByBasic=0;
	int numClusterMoveByEmergency=0;
	int numClustercanNotMove=0;
	
	
	
	
	public Cooccs(){}
	
	
	/**
	* Constructor that will calculate co-occurrences from a file containing sentences.
	* @param satzFile The file that contains the initial corpus, one sentence per line
	*/
	public Cooccs(File satzFile, boolean usedb){
		
		
		
	/////create log file to get the data which used for create graph on excel program
		try{
			//System.out.println("test log file");
			writer = new FileWriter("cluster.csv");
			writer.append("#docs");
			writer.append("\t");
			writer.append("#cluster");
				
				
			writer.append("\n");
			writer.flush();
		
		}catch (IOException e) {}
		
		try{
			//System.out.println("test log file");
			writer_cid = new FileWriter("cluster_cid.csv");
			writer_cid.append("#docs");
			writer_cid.append("\t");
			writer_cid.append("#cluster");
				
				
			writer_cid.append("\n");
			writer_cid.flush();
		
		}catch (IOException e) {}		
		
		
		try{
			//System.out.println("test log file");
			writer_mini_cluster = new FileWriter("mini_cluster.csv");
			writer_mini_cluster.append("#docs");
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append("#sumMiniClusterRecheck");
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append("#lessMemberForRecheck");
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append("#clusterMoveByBasic");
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append("#clusterMoveByEmergency");
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append("#clusterCanNotMove");
				
				
			writer_mini_cluster.append("\n");
			writer_mini_cluster.flush();
		
		}catch (IOException e) {}
		
		graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(database);
		System.out.println("database opened/created");
		
		
		for(int i=1;i<=max_cluster;i++) {
			centroids[i] = "";
			distilledText[i]=new Vector();
			numDistance[i]=new Vector();
			averageDistance[i] = 0;
			standardDeviation[i] = 0;
			cooccs[i] = new HashMap();
			frequencies[i] = new HashMap();
		}
		
		
		copyWordFromDBtoArrayCluster();
		
		
		for(int j=0;j<max_document;j++) {
			wordsEachDoc[j]=new Vector();
		}
		language = getLanguage(satzFile);
 
		Indexer ind = new Indexer();
 		ind.setLanguage(language);
		        
		Pretree pretree = new Pretree();
		
		if (language==0) {
			pretree.load(redbase_de);
		} else {
			pretree.load(redbase_en);
		}
       
		Zerleger2 zer = new Zerleger2();
		zer.init(forw, back, red);
		
		Properties props = new Properties();

		String tmDir = new File(tmFile).getParent();
       
		try {
			if (language==0) {
				props.load(new FileInputStream(tmFile));
			} else {
				props.load(new FileInputStream(tmFile2));
			}
		} catch (Exception filenotfound) {}
		
		System.out.println("Prop: " + props.getProperty("taglist"));
        
		Tagger tagger = new Tagger(tmDir+"/"+props.getProperty("taglist"),
				tmDir+"/"+props.getProperty("lexicon"),
				tmDir+"/"+props.getProperty("transitions"),null, false);
		tagger.setExtern(true);
		tagger.setReplaceNumbers( false /*props.getProperty("ReplaceNumbers").equals("false")*/);
		tagger.setUseInternalTok(true);
		
		// read satzFile sentence-wise:
		String lineorig = null;
		try {
			FileInputStream fin2 =  new FileInputStream(satzFile);
			String alltext="";
			
			BufferedReader myInput2 = new BufferedReader(new InputStreamReader(fin2));
						
			while ((lineorig = myInput2.readLine()) != null){
		
				//System.out.println(tagger.tagSentence(lineorig));
				alltext = alltext + lineorig;
			
			}
			
			
			boolean helpstem= ind.getParameters().getStemming();
			if (helpstem)  ind.getParameters().setStemming(false);
				
			ind.prepare(alltext);
			
			List phrases = ind.getPhrases();
			
	 		for (int i=0; i<phrases.size(); i++) {
	 			System.out.println("phrase: " +phrases.get(i));
	 			
	 			boolean lower = false;
	 			
	 			if (phrases.get(i).toString().endsWith("A N")) lower = true;
	 			
	 			if (lower) {
	 				
	 				String help = phrases.get(i).toString();
	 				
	 				help = Character.toLowerCase(help.charAt(0)) + help.substring(1);

	 				phrases.set(i, help.substring(0, help.indexOf(",") ));
	 			
	 			} else {
	 				phrases.set(i, phrases.get(i).toString().substring(0, phrases.get(i).toString().indexOf(",") ));
	 			
	 			}
	 			
	 			System.out.println("phrase 2: " +phrases.get(i));
	 		}
	 		
	 		ind.getParameters().setStemming(helpstem);
	 		
	 		phrases = new Vector();  //comment out if phrases should appear in coocc graph
	 			 		
	 		
	 	    //Term pairs that are phrases are bound together
	 		String alltext2 = "";
	 		fin2 =  new FileInputStream(satzFile);
		
			myInput2 = new BufferedReader(new InputStreamReader(fin2));
						
			while ((lineorig = myInput2.readLine()) != null){
				
				StringBuffer strbuf = new StringBuffer(lineorig);
				
				for (int i=0; i<phrases.size(); i++) {
					while (strbuf.toString()/*.toLowerCase()*/.indexOf(((String)phrases.get(i))/*.toLowerCase()*/)!=-1     ) {
						
						int k = strbuf.toString()/*.toLowerCase()*/.indexOf(((String)phrases.get(i))/*.toLowerCase()*/);
						
						String replacement= ((String)phrases.get(i))/*.toLowerCase()*/.replaceAll(" ", "##phrase");
						
						strbuf.replace(k, k+((String)phrases.get(i))/*.toLowerCase()*/.length(), replacement);
					}
				}
				
				alltext2=alltext2+"\n"+strbuf;
				// System.out.println(alltext2);
			}

			System.out.println("CooccExtraction");
			
			//FileInputStream fin =  new FileInputStream(satzFile);
			//BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
			BufferedReader myInput = new BufferedReader(new StringReader(alltext2));

			
			Vector cleanwordlist =  new Vector();
			Vector words = new Vector();
			
			
			
//////////////////////for know end of line in each document
			Vector end_all_line = Text2Satz.getCountLineAll();
			count_doc = 0;
			line=0;
			
			System.out.println("# of End Lines of each document "+end_all_line.toString());
			int end_line_each_doc = (int)end_all_line.get(count_doc);
			////////////////////////////////////////////////////////////
			System.out.println("# read doc no. "+(count_doc+1));
			
						
			while ((lineorig = myInput.readLine()) != null){

				cleanwordlist =  new Vector();

				List allwords = new Vector();

				lineorig = lineorig.replaceAll("\\[[0-9]+\\]", "");
				lineorig = lineorig.replaceAll("[^a-zA-Z 0-9 ä ö ü Ä Ö Ü ß | \\- ## ]", ""); //- /*{1,2}*/

				//System.out.println(lineorig);

				if (!lineorig.equals("")) {
				
					String taggedsentence = tagger.tagSentence(lineorig);

					//System.out.println(taggedsentence);
				
					String [] splittedsentence = taggedsentence.split(" ");

					for (int i=0; i<splittedsentence.length; i++) {
				
						if ( (splittedsentence[i].indexOf("##phrase")!=-1)  ){
	              		  
							//System.out.println(splittedsentence[i]);
						
							String removefiller = ((String)splittedsentence[i]).replaceAll("##phrase", " ");
							//System.out.println(removefiller);
						
							int pos = removefiller.indexOf("|");  //start of wrong tag for the phrase
	              		  
							if (pos>0)
								if (removefiller.substring(0, pos)/*.toLowerCase()*/.length()>1)
									allwords.add(removefiller.substring(0, pos)+"|Phrase"/*.toLowerCase()*/);

						} else {  //no phrase
							if ( (splittedsentence[i].indexOf("|NN")!=-1) || (splittedsentence[i].indexOf("|NE")!=-1) || (splittedsentence[i].indexOf("|NP")!=-1) ){

              		  
								int pos = splittedsentence[i].indexOf("|");
              		  
								if (pos>0)
									if (splittedsentence[i].substring(0, pos)/*.toLowerCase()*/.length()>0)
										if (!splittedsentence[i].substring(0, pos).toLowerCase().equals("%n%")) {
              					  
											if (splittedsentence[i].indexOf("|NP")!=-1) {
                					  
												if (!isStopWord(splittedsentence[i].substring(0, pos))) {
              						
													allwords.add(splittedsentence[i].substring(0, pos)+"|NP"/*.toLowerCase()*/);
												} else {
												//	System.out.println("Stopword removed: "+splittedsentence[i]);
												}
                					  
											} else if (splittedsentence[i].indexOf("|NE")!=-1) {
												if (!isStopWord(splittedsentence[i].substring(0, pos))) {
                  						
													allwords.add(splittedsentence[i].substring(0, pos)+"|NE"/*.toLowerCase()*/);
												} else {
													//System.out.println("Stopword removed: "+splittedsentence[i]);
												}
              						
											} else if (splittedsentence[i].indexOf("|NN")!=-1){
              							
												if (!isStopWord(splittedsentence[i].substring(0, pos))) {

													allwords.add(splittedsentence[i].substring(0, pos)+"|NN"/*.toLowerCase()*/);
												} else {
													//	System.out.println("Stopword removed: "+splittedsentence[i]);
												}
											}
										}
							} //nouns 
						}
					} //for all terms in sentence

					for (int i=0; i<allwords.size(); i++) {
						String entry=(String)allwords.get(i); //curWord.getWordStr();
						int pos = entry.indexOf("|"); 
						int pos2= entry.indexOf("|Phrase");
						String entry2= entry.substring(0, pos);
						String tag = entry.substring(pos, entry.length());
				
						if ((entry2.length()>0) && (!entry2.equals(";")))
				
							if (pos2==-1) {  //no phrase
						
								if (language == 1) {
						       
									if (!tag.equals("|NP") && !tag.equals("|NE"))
										entry2 = entry2.toLowerCase();

									if (ind.getParameters().getStemming()) {

										Porter port = new Porter();
  								     
										if (!tag.equals("|NP") && !tag.equals("|NE"))
											entry2 = port.stem(entry2);
  							
									}
  							
									if (!isStopWord(entry2)) {
          						
										if ((entry2.length()>0) && (!entry2.equals(";")))
											cleanwordlist.add(entry2+""+tag);
  								
									}
  							
								} else {
									if (ind.getParameters().getStemming()) {
  						      
										if (!tag.equals("|NP") && !tag.equals("|NE"))
											entry2 = zer.grundFormReduktion(entry2);
							
									}
  							
									if (!isStopWord(entry2)) {
          					
										if ((entry2.length()>0) && (!entry2.equals(";")))
											cleanwordlist.add(entry2+""+tag);
  								
									}	
								}
							} else			
								cleanwordlist.add(entry);
					}
					//	words = adapttoStopwords(cleanwordlist);
					//	words = removeStopwords(words);   

					if(cleanwordlist.size() > 1){      //word
					
						for (int i=0; i<cleanwordlist.size(); i++) {
					
							String entry=(String)cleanwordlist.get(i); //curWord.getWordStr();
						
							//System.out.println("Term: " +entry);
						
							int pos = entry.indexOf("|"); 
							//int pos2= entry.indexOf("|Phrase");
						
							if (pos!=-1)
								entry= entry.substring(0, pos);
							
							cleanwordlist.set(i, entry);
							//distilledText.add(entry);
					
						}
					
						System.out.println("\n\n\n***Sentence no."+line+"...\nCleanwordList ::: "+ cleanwordlist.toString()+"\n");
					
						//extractCooccs(cleanwordlist);	//words
						addSentenceToCooccsDB(cleanwordlist);
						//wordsEachDoc[count_doc].add(cleanwordlist);
						
						
						
						//System.out.println("\n\n****Start of Sentence no."+(line+1)+"****\n");
						
						//line++;
						
						
					}

				}
				

				System.out.println("\n\n****End of Sentence no."+line+"****\n");
				
				line++;
				
				if(line==end_line_each_doc && count_doc<end_all_line.size()-1) {
					
					//updateClusterAgain();
					if(count_doc%10==0) {
						recheckMiniCluster();
					}
					System.out.println("\n\n----------------");
					int ccluster=0;
					for(int p=1;p<=count_cluster;p++) {
						if(distilledText[p].size()>0) {
							ccluster++;
							System.out.println("Cluster no."+p+"("+ccluster+") \n-->>Distilled Text(All word read only noun): " + distilledText[p].toString());
							System.out.println("-->>Centroid: " + centroids[p]);
							System.out.println("-->>Average Distance: " + averageDistance[p]);
							System.out.println("-->>Standard Deviation: " + standardDeviation[p]);
							System.out.println("-->>average + "+numMultiplySigma+"*SD : " + (averageDistance[p]+(numMultiplySigma*standardDeviation[p])));
							//System.out.println("-->>numDistance: " + numDistance[p].toString());
							System.out.println("-------------------------------------------");
						}
					}
					

					writeDataToLog1();
					
					count_doc++;
					
					System.out.println("# read doc no. "+(count_doc+1));
					
					end_line_each_doc = (int)end_all_line.get(count_doc);
				}
				
			} // for all lines
			//updateClusterAgain();
			if(count_doc>=10) {
				recheckMiniCluster();
			}
			
			writeDataToLog1();
//			writeDataToLog2();
			
			try{
			    writer.close();	
			    writer_cid.close();
			    writer_mini_cluster.close();
			}catch (IOException e) {}
			
			
			//updateClusterAndPositionCentroid();
			System.out.println("\n\n+++++++++++++++++++++++ Results of Clustering +++++++++++++++++++++++");
			int ncluster=0;
			for(int p=1;p<=count_cluster;p++) {
				if(distilledText[p].size()>0) {
					ncluster++;
					System.out.println("Cluster no."+p+"("+ncluster+") \n-->>Distilled Text(All word read only noun): " + distilledText[p].toString());
					System.out.println("-->>Centroid: " + centroids[p]);
					System.out.println("-->>Average Distance: " + averageDistance[p]);
					System.out.println("-->>Standard Deviation: " + standardDeviation[p]);
					System.out.println("-->>average + "+numMultiplySigma+"*SD : " + (averageDistance[p]+(numMultiplySigma*standardDeviation[p])));
					//System.out.println("-->>numDistance: " + numDistance[p].toString());
					System.out.println("-------------------------------------------");
				}
			}
			System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");
			
			
			
			
		}catch (Exception e) {e.printStackTrace();}

		
	}
	
	
	public void recheckMiniCluster() {
	
		c_update_minicluster.clear();
		
		double sum = 0,average=0,count_c=0;
		for(int p=1;p<=count_cluster;p++) {
			if(distilledText[p].size()>0) {
				sum=sum+distilledText[p].size();
				count_c++;
			}
		}
		average = sum / count_c;
						
		int num_member_recheck = (int)(Math.ceil(average / 4)); //25% of average
		
		numMemberForRecheck = num_member_recheck;

		for(int p=1;p<=count_cluster;p++) {
			if(distilledText[p].size()>0 && (((double)distilledText[p].size())<num_member_recheck)) {
				sumMiniClusterRecheck++;
				miniClusterMemberFindClusterAgain(p,num_member_recheck);
			}
		}
	}
	
	/*
	public void updateClusterAgain() {
		
		System.out.println("Starting to update cluster again...");
		//update cluster 
		boolean can_ins_to_cluster = false;
		try (Transaction txa = graphDB.beginTx()) 
		{
			for(int cc=1;cc<=count_cluster;cc++) {
				if(distilledText[cc].size()>0 && distilledText[cc].size()<3) {
					
					System.out.println("Cluster no."+cc+" has member less than 3, it will to find cluster again...");
					
					Vector<Integer> c_update = new Vector<Integer>();
					
					for(int p = distilledText[cc].size()-1;p >= 0;p--) {
						Node nfc = graphDB.findNode(Labels.SINGLE_NODE,"name", distilledText[cc].get(p));
						
						int get_cluster_id_check_again =(int)nfc.getProperty("clusterID");
						String s_c = (String)nfc.getProperty("name");
						double d_final = Double.MAX_VALUE;
						int cid_final=0;
						
						Iterable<Relationship> allRelationships = nfc.getRelationships();
						for (Relationship relationship : allRelationships) {
					    	
					    	Node n2=relationship.getOtherNode(nfc);
					    	
					    	int ckid =(int)n2.getProperty("clusterID");
					    	double d = getNodeDistance2(s_c,centroids[ckid]);
					    	can_ins_to_cluster = canInsertToThisCluster(s_c,ckid);
					    	if(d<d_final && ckid != cc && can_ins_to_cluster==true && distilledText[cc].size()<distilledText[ckid].size()) {
					    		d_final=d;
					    		cid_final = ckid;
					    	}
					    	
					    }
						if(cid_final!=0) {
							System.out.println("cid_final="+cid_final);
							nfc.setProperty("clusterID", cid_final);
							distilledText[cid_final].add(s_c);
							
							for(int s=distilledText[cc].size()-1;s>=0;s--) {
								if(distilledText[cc].get(s).toString().equals(s_c)) {
									distilledText[cc].remove(s);
									s=-1;
								}
							}
							
							//updateClusterAndPositionCentroid(cid_final);
							if (!c_update.contains(cid_final)) {
								System.out.println("move...");
								c_update.add(cid_final);
							}
					    } 
	
					}//end for p
					if(distilledText[cc].size()>0) {
	          			
						//updateClusterAndPositionCentroid(cc);
						if (!c_update.contains(cc)) {
							c_update.add(cc);
						}
	
					}else {
						//clear data
						centroids[cc]="";
						numDistance[cc].clear();
						averageDistance[cc] = 0;
						standardDeviation[cc] = 0;
					}
				
					//update cluster 
      				for(int u=0;u<c_update.size();u++) {
      					
      					updateClusterAndPositionCentroid(c_update.get(u));
      					
      				}
				
				}
			}
			txa.success();
		}
	}
	
	*/
	public void writeDataToLog1() {

		try{
			writer.append(Integer.toString((count_doc+1)));
			writer.append("\t");
			writer.append(Integer.toString(getNumCluster()));
			writer.append("\t");
				
			for(int p=1;p<=count_cluster;p++) {
				if(distilledText[p].size()>0) {
					//writer.append(Integer.toString(distilledText[p].size()));
					writer.append(Integer.toString(getWordInEachCluster(p)));
	      			writer.append("\t");
				}	
			}
				          				
			writer.append("\n");
		    writer.flush();
		    
		}catch (IOException e) {}
		
		try{
			writer_mini_cluster.append(Integer.toString((count_doc+1)));
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append(Integer.toString(sumMiniClusterRecheck));
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append(Integer.toString(numMemberForRecheck));
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append(Integer.toString(numClusterMoveByBasic));
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append(Integer.toString(numClusterMoveByEmergency));
			writer_mini_cluster.append("\t");
			writer_mini_cluster.append(Integer.toString(numClustercanNotMove));
				          				
			writer_mini_cluster.append("\n");
			writer_mini_cluster.flush();
		    
		}catch (IOException e) {}
		

		sumMiniClusterRecheck=0;
		numMemberForRecheck=0;
		numClusterMoveByBasic=0;
		numClusterMoveByEmergency=0;
		numClustercanNotMove=0;

	}	
	public void writeDataToLog2() {

		try{
				writer_cid.append(Integer.toString((count_doc+1)));
				writer_cid.append("\t");
				writer_cid.append(Integer.toString(getNumCluster()));
				writer_cid.append("\t");
					
				for(int d=0;d<=count_doc;d++) {
					if(wordsEachDoc[d].size()>0) {
						//writer.append(Integer.toString(distilledText[p].size()));
						writer_cid.append(Integer.toString(getClusterIDwithCentroidbySpreadingActivation(wordsEachDoc[d])));
						writer_cid.append("\t");
					}	
				}
					          				
				writer_cid.append("\n");
				writer_cid.flush();
			    
		}catch (IOException e) {}
		
	}
	
	public void shutdownDB() {
		graphDB.shutdown();
		System.out.println("database closed");
	}
	
/////////////////////////////////add or update data on Neo4j Desktop database//////////////////////////////
/*	public void createNode( String w , int c )
    {
        try ( Session session = driver.session() )
        {
        	String cmd = "CREATE (a:words{name:'"+w+"', occur:'"+c+"', clusterLeft:'"+w+"', clusterRight:'"+w+"'})";
        	session.run( cmd );
        }
    }
	
	public void updateOccur( String w , int c )
    {
        try ( Session session = driver.session() )
        {
        	String cmd = "MATCH (a:words) WHERE a.name = '"+w+"' Set a.occur='"+c+"'";
        	session.run( cmd );
        }
    }
	
	public void createRelation( String w1, String w2, int count, int dice, int cost)
    {
        try ( Session session = driver.session() )
        {
        	String cmd = "MATCH (a:words),(b:words) WHERE a.name = '"+w1+"' AND b.name = '"+w2+"' CREATE (a)-[c:connect{count:'"+count+"', dice:'"+dice+"', cost:'"+cost+"'}]->(b)";
        	//String cmd = "MATCH (a:words),(b:words) WHERE a.name = '"+w1+"' AND b.name = '"+w2+"' CREATE (a)-[c:connect{count:'"+count+"'}]->(b)";
        	session.run( cmd );
        }
    }
	
	public void updateRelationPropertyCount( String w1, String w2, int c )
    {
        try ( Session session = driver.session() )
        {
        	String cmd = "MATCH (a:words),(b:words) WHERE a.name = '"+w1+"' AND b.name = '"+w2+"' MERGE (a)-[c:connect]->(b) SET c.count='"+c+"'";
        	session.run( cmd );
        	cmd = "MATCH (a:words),(b:words) WHERE a.name = '"+w2+"' AND b.name = '"+w1+"' MERGE (a)-[c:connect]->(b) SET c.count='"+c+"'";
        	session.run( cmd );
        }
    }
	
	public void updateRelationPropertyDiceAndCost( String w1, String w2, double d, double c)
    {
        try ( Session session = driver.session() )
        {
        	String cmd = "MATCH (a:words),(b:words) WHERE a.name = '"+w1+"' AND b.name = '"+w2+"' MERGE (a)-[c:connect]->(b) SET c.dice='"+d+"',c.cost='"+c+"'";
        	session.run( cmd );
        }
    }
*/	
////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void addSentenceToCooccsDB(Vector words)   //adding each word to DB from each sentence
	{
		Cooccs cadd = new Cooccs();
		
		int c_cluster_before = count_cluster;
		
		//String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		//File database = new File(db_path);
		
		int count,f_flag=0,num_node_db=0,get_cluster_id=0;
		boolean can_ins_to_cluster = false;
		boolean re_centroid = false; //to check for running to find new position of centroid when node change cluster
		
		Vector<Integer> c_update = new Vector<Integer>();
		
      //creating a database
        //GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(database);
        //System.out.println("database opened/created");
      	
        try (Transaction tx = graphDB.beginTx()) 
      	{
      		System.out.println("in transaction of database");
      		List<String> tempS = new ArrayList<>();
            	    
      		for(Iterator i=words.iterator(); i.hasNext(); ){
            			            			
      			//starting to read word for inserting to db
      			String word = ((String)i.next()).toLowerCase();
      			//time++;
      			
      			tempS.add(word);
      			f_flag=0;//0=new node, 1=old node
      			num_node_db=0;//for counting #node on db
      			get_cluster_id=0;//for checking new node(0) or old node(>0)
      			
      			can_ins_to_cluster = false;
      			
      			System.out.println("\n\n>******> Reading new word -------> "+word);
      			
      			if (!wordsEachDoc[count_doc].contains(word)) {
      				wordsEachDoc[count_doc].add(word);
      			}
      			
      			//checking if the node is already present
      			ResourceIterator<Node> nodelist = graphDB.findNodes( Labels.SINGLE_NODE );
      			while( nodelist.hasNext() )
      			{
      				num_node_db++;
      				Node wordnode = nodelist.next();
      				if(wordnode.getProperty("name").equals(word))	
      				{
      					f_flag=1;
      					count=(int)wordnode.getProperty("occur");
      					count=count+1;
      					wordnode.setProperty("occur",count);
            					
//      					cadd.updateOccur( word , count );
      					System.out.println(word +" node was already present. Count updated.");
      					
      					get_cluster_id =(int)wordnode.getProperty("clusterID");
      				}
      			}
      			
      			
      			if(f_flag==0){//no of occurrences in database
      				Node wordnode = graphDB.createNode(Labels.SINGLE_NODE);
      				wordnode.setProperty("name", word);
            		wordnode.setProperty("occur", 1); 
            		wordnode.setProperty("clusterID", 0);
            		wordnode.setProperty("centroidL0", 0);
            		//wordnode.setProperty("clusterLeft", word);
            		//wordnode.setProperty("clusterRight", word);            
            		
//            		cadd.createNode( word , 1 );
            		
                	System.out.println(word +" node added."); 
                	 
      			}

      			//add relation 
      			Node n1 = graphDB.findNode(Labels.SINGLE_NODE,"name", word);
      			if(tempS.size()>1) {
      				boolean rel_found;
      				
                    //CONNECTING NODES_CREATING RELATIONSHIPS
      				//Node n1 = graphDB.findNode(Labels.SINGLE_NODE,"name", word);
      				Iterable<Relationship> allRelationships = n1.getRelationships();
      				
      				for (int p = tempS.size()-1;p >= 0;p--){
      					if (!((String) tempS.get(p)).equals(word)) {
      						rel_found=false;
      						Node n2 = graphDB.findNode(Labels.SINGLE_NODE,"name", tempS.get(p));
      						
      						String w2 = (String)n2.getProperty("name");
      						
      						for (Relationship relationship : allRelationships){
                				if(n2.equals(relationship.getOtherNode(n1))){
                    		        count=(int)relationship.getProperty("count");
                    		        count=count+1;
                    		        relationship.setProperty("count", count);
                    		        	
//                    		        cadd.updateRelationPropertyCount( word, w2, count );
                    		        	
                    		        System.out.println("Relation already existed between nodes "+word+" and "+tempS.get(p)+". Count updated.");
                    		        rel_found=true;
                    		        break;
                    		    }
                    		        	
                			}
      						//creating new relationship
                			if(!rel_found){
                				Relationship relationship = n1.createRelationshipTo(n2, RelationshipTypes.IS_CONNECTED );
                				relationship.setProperty("count", 1 );
                				relationship.setProperty("dice", 0.0);  //for calculating Dice ration
                				relationship.setProperty("cost", 0.0);   //for Dijkstra
                                  	
//                				cadd.createRelation( word, w2, 1,0,0);
                                  	
                				System.out.println("Relation inserted with nodes "+word+" and "+tempS.get(p));
                			}
      					}
      				}
      			}
      			
      			
      			//copy word not the same to new arraylist before check
      			List<String> tempS2 = new ArrayList<>();
      			for (int c = 0; c<tempS.size();c++){
      				if (!tempS2.contains(tempS.get(c))) {
      					tempS2.add(tempS.get(c));
          			}
      			}
      				
      			if(num_node_db == 0) { // this is a first node (first word) insert to db
          			//Node n = graphDB.findNode(Labels.SINGLE_NODE,"name", word);
          			n1.setProperty("clusterID", 1);
          			n1.setProperty("centroidL0", 1);
          			distilledText[1].add(word);
          				
          			//updateDiceandCosts();//update dice and cost for all relation present on db     				
          			//getEvolvingCentroid(word,1);//case 1
          			
          			centroids[1] = word;
          			check_start=true;
          			
          			updateClusterAndPositionCentroid(1);
          				
          		}else if(num_node_db > 0){
          			
          			//Vector<Integer> c_update = new Vector<Integer>();
          			
          			
          			check_start=true;
          			if(get_cluster_id == 0) {//new word
          				if(tempS2.size()>1) {
          					int get_cluster_id_check = 0;
          					double d_min=Double.MAX_VALUE,d=0;
          					
          					for (int p = tempS2.size()-1;p >= 0;p--){
          						if (!((String) tempS2.get(p)).equals(word)) {
          							Node nf1 = graphDB.findNode(Labels.SINGLE_NODE,"name", tempS2.get(p));
          							get_cluster_id_check =(int)nf1.getProperty("clusterID");
                  					
          							can_ins_to_cluster = canInsertToThisCluster(word,get_cluster_id_check);
                  					
                  					if(can_ins_to_cluster==true) {
                  						d = getNodeDistance2(word,centroids[get_cluster_id_check]);
              							
                      					if(d<d_min) {
                      						d_min = d;
                      						get_cluster_id = get_cluster_id_check;
                      					}
                  						
                  					}else {
                  						if (!c_update.contains(get_cluster_id)) {
		  									
		  									c_update.add(get_cluster_id);
                  						                  						
	                  						updateClusterAndPositionCentroid(get_cluster_id_check);
	                  						
	                  						can_ins_to_cluster = canInsertToThisCluster(word,get_cluster_id_check);
	                  						
	                  						if(can_ins_to_cluster==true) {
	                      						d = getNodeDistance2(word,centroids[get_cluster_id_check]);
	                  							
	                          					if(d<d_min) {
	                          						d_min = d;
	                          						get_cluster_id = get_cluster_id_check;
	                          					}
	                      						
	                      					}
                  						}
                  						               						
                  					}//end else	
          						}//end if not equal word
          					}//end for int p=tempS2
          					
          					if(get_cluster_id != 0) {
          						n1.setProperty("clusterID", get_cluster_id);
          						n1.setProperty("centroidL0", 0);
          						
          						if (!distilledText[get_cluster_id].contains(word)) {
          							distilledText[get_cluster_id].add(word);
          		      			}
          						
          					}else {
          						count_cluster++;
    	          				n1.setProperty("clusterID", count_cluster);
    	          				n1.setProperty("centroidL0", 1);
    	          				distilledText[count_cluster].add(word);
    	          					
    	          				//updateDiceandCosts();
    	          				//getEvolvingCentroid(word,count_cluster);
    	          				
    	          				centroids[count_cluster] = word;
    	          				updateClusterAndPositionCentroid(count_cluster);
          					}	         						         						
          				}else {         					
	          				count_cluster++;
	          				n1.setProperty("clusterID", count_cluster);
	          				n1.setProperty("centroidL0", 1);
	          				distilledText[count_cluster].add(word);
	          					
	          				//updateDiceandCosts();
	          				//getEvolvingCentroid(word,count_cluster);
	          				centroids[count_cluster] = word;
	          				updateClusterAndPositionCentroid(count_cluster);
          				}
          			}else if(get_cluster_id != 0){//old word
          				
          				
          				//n1.setProperty("clusterID", get_cluster_id);
    	      			//distilledText[get_cluster_id].add(word);
    	      			
          				//updateClusterAndPositionCentroid(get_cluster_id);
          				
          				//if (!c_update.contains(get_cluster_id)) {
              			//	c_update.add(get_cluster_id);
              			//}
          				     				          				
          				int get_cluster_id_check = 0;
          				String s_check = "";
          				for (int p = tempS2.size()-1;p >= 0;p--){
          					
          					if (!((String) tempS2.get(p)).equals(word)) {
          						Node nf1 = graphDB.findNode(Labels.SINGLE_NODE,"name", tempS2.get(p));
      							get_cluster_id_check =(int)nf1.getProperty("clusterID");
      							s_check = (String)nf1.getProperty("name");
      							
    	          				if(c_cluster_before == count_cluster) {
    	          					
    	          					double d1 = getNodeDistance2(s_check,centroids[get_cluster_id_check]);
      								double d2 = getNodeDistance2(s_check,centroids[get_cluster_id]);
      								//if(d2<d1 && distilledText[get_cluster_id_check].size()<distilledText[get_cluster_id].size()) {
      								if(d2<d1) {
      									
      									can_ins_to_cluster = canInsertToThisCluster(s_check,get_cluster_id);
      									
      		  							if(can_ins_to_cluster==true) {
      		  								nf1.setProperty("clusterID", get_cluster_id);
      		  								nf1.setProperty("centroidL0", 0);
      		  								
    	  		  							if (!distilledText[get_cluster_id].contains(s_check)) {
    	  	          							distilledText[get_cluster_id].add(s_check);
    	  	          		      			}
      		  								
      		  								
      		  								//distilledText[get_cluster_id].add(s_check);
      		  								
      		  								for(int s=distilledText[get_cluster_id_check].size()-1;s>=0;s--) {
      		  									if(distilledText[get_cluster_id_check].get(s).toString().equals(s_check)) {
      		  										distilledText[get_cluster_id_check].remove(s);
      		  										s=-1;
      		  									}
      		  								}
    	
      				                  		//updateClusterAndPositionCentroid(get_cluster_id);

      		  								//if (!c_update.contains(get_cluster_id)) {
      		  								//	c_update.add(get_cluster_id);
      		  								//}

      		  								if(distilledText[get_cluster_id_check].size()>0) {
      				                  			
      				                  			//updateClusterAndPositionCentroid(get_cluster_id_check);

      		  									//if (!c_update.contains(get_cluster_id_check)) {
      		  									//	c_update.add(get_cluster_id_check);
      		  									//}
    	  				                  		
      			                  			}else {
      			                  				//clear data
      				                  			centroids[get_cluster_id_check]="";
      				                  			numDistance[get_cluster_id_check].clear();
      				            				averageDistance[get_cluster_id_check] = 0;
      				            				standardDeviation[get_cluster_id_check] = 0;
      			                  			}
      		  							}else {
      		  								
      		  								if (!c_update.contains(get_cluster_id)) {
    		  									
    		  									c_update.add(get_cluster_id);

    	  		  								updateClusterAndPositionCentroid(get_cluster_id);
    	  		  								
    	  		  								can_ins_to_cluster = canInsertToThisCluster(s_check,get_cluster_id);
    	  									
    		  		  							if(can_ins_to_cluster==true) {
    		  		  								nf1.setProperty("clusterID", get_cluster_id);
    		  		  								nf1.setProperty("centroidL0", 0);
    		  		  								
    			  		  							if (!distilledText[get_cluster_id].contains(s_check)) {
    			  	          							distilledText[get_cluster_id].add(s_check);
    			  	          		      			}
    		  		  								
    		  		  								
    		  		  								//distilledText[get_cluster_id].add(s_check);
    		  		  								
    		  		  								for(int s=distilledText[get_cluster_id_check].size()-1;s>=0;s--) {
    		  		  									if(distilledText[get_cluster_id_check].get(s).toString().equals(s_check)) {
    		  		  										distilledText[get_cluster_id_check].remove(s);
    		  		  										s=-1;
    		  		  									}
    		  		  								}
    			
    		  				                  		//updateClusterAndPositionCentroid(get_cluster_id);
    		
    		  		  								//if (!c_update.contains(get_cluster_id)) {
    		  		  								//	c_update.add(get_cluster_id);
    		  		  								//}
    		
    		  		  								if(distilledText[get_cluster_id_check].size()>0) {
    		  				                  			
    		  				                  			//updateClusterAndPositionCentroid(get_cluster_id_check);
    		
    		  		  									//if (!c_update.contains(get_cluster_id_check)) {
    		  		  									//	c_update.add(get_cluster_id_check);
    		  		  									//}
    			  				                  		
    		  			                  			}else {
    		  			                  				//clear data
    		  				                  			centroids[get_cluster_id_check]="";
    		  				                  			numDistance[get_cluster_id_check].clear();
    		  				            				averageDistance[get_cluster_id_check] = 0;
    		  				            				standardDeviation[get_cluster_id_check] = 0;
    		  			                  			}
    		  		  							}
      		  							
      		  								}

      		  							}
      								}
    	
    	          				}else {//c_cluster_before != count_cluster
              					
    	          					if(get_cluster_id_check == count_cluster) {//has a new cluster of this sentence
    	  								//if(get_cluster_id_check == count_cluster) {
    	  									
    	  									can_ins_to_cluster = canInsertToThisCluster(s_check,get_cluster_id);
    	  									
    	  		  							if(can_ins_to_cluster==true) {
    	  		  								nf1.setProperty("clusterID", get_cluster_id);
    	  		  								nf1.setProperty("centroidL0", 0);
    	  		  								
    		  		  							if (!distilledText[get_cluster_id].contains(s_check)) {
    		  	          							distilledText[get_cluster_id].add(s_check);
    		  	          		      			}
    	  		  								
    	  		  								
    	  		  								//distilledText[get_cluster_id].add(s_check);
    	  		  								
    	  		  								for(int s=distilledText[get_cluster_id_check].size()-1;s>=0;s--) {
    	  		  									if(distilledText[get_cluster_id_check].get(s).toString().equals(s_check)) {
    	  		  										distilledText[get_cluster_id_check].remove(s);
    	  		  										s=-1;
    	  		  									}
    	  		  								}
    		
    	  				                  		//updateClusterAndPositionCentroid(get_cluster_id);
    	  				                  			
    		  		  							//if (!c_update.contains(get_cluster_id)) {
    		  		  							//	c_update.add(get_cluster_id);
    		  		  							//}

    	  		  								if(distilledText[get_cluster_id_check].size()>0) {
    	  				                  			
    	  				                  			//updateClusterAndPositionCentroid(get_cluster_id_check);
    	  				                  			
    	  		  									//if (!c_update.contains(get_cluster_id_check)) {
    	  		  									//	c_update.add(get_cluster_id_check);
    	  		  									//}
    		
    	  			                  			}else {
    	  			                  				//clear data
    	  				                  			 
    	  				                  			centroids[get_cluster_id_check]="";
    	  				                  			numDistance[get_cluster_id_check].clear();
    	  				            				averageDistance[get_cluster_id_check] = 0;
    	  				            				standardDeviation[get_cluster_id_check] = 0;
    	  			                  			}
    	  		  							}else {
    	  		  								
    	  		  							//if (!c_update.contains(get_cluster_id)) {
    	  		  	              			//	c_update.add(get_cluster_id);
    	  		  	              			//}
    	  		  								
    	  		  								if (!c_update.contains(get_cluster_id)) {
    	  		  									
    	  		  									c_update.add(get_cluster_id);
    	  		  									
    	  		  									updateClusterAndPositionCentroid(get_cluster_id);
    	  		  								
    	  		  									can_ins_to_cluster = canInsertToThisCluster(s_check,get_cluster_id);
    	  									
    		  		  								if(can_ins_to_cluster==true) {
    		  		  									nf1.setProperty("clusterID", get_cluster_id);
    		  		  									nf1.setProperty("centroidL0", 0);
    		  		  									
    			  		  								if (!distilledText[get_cluster_id].contains(s_check)) {
    				  	          							distilledText[get_cluster_id].add(s_check);
    				  	          		      			}
    		  		  									
    		  		  									
    		  		  									//distilledText[get_cluster_id].add(s_check);
    		  		  								
    		  		  									for(int s=distilledText[get_cluster_id_check].size()-1;s>=0;s--) {
    		  		  										if(distilledText[get_cluster_id_check].get(s).toString().equals(s_check)) {
    		  		  											distilledText[get_cluster_id_check].remove(s);
    		  		  											s=-1;
    		  		  										}
    		  		  									}
    			
    			  				                  		//updateClusterAndPositionCentroid(get_cluster_id);
    			  				                  			
    				  		  							//if (!c_update.contains(get_cluster_id)) {
    				  		  							//	c_update.add(get_cluster_id);
    				  		  							//}
    		
    			  		  								if(distilledText[get_cluster_id_check].size()>0) {
    			  				                  			
    			  				                  			//updateClusterAndPositionCentroid(get_cluster_id_check);
    			  				                  			
    			  		  									//if (!c_update.contains(get_cluster_id_check)) {
    			  		  									//	c_update.add(get_cluster_id_check);
    			  		  									//}
    				
    			  			                  			}else {
    			  			                  				//clear data
    			  				                  			 
    			  				                  			centroids[get_cluster_id_check]="";
    			  				                  			numDistance[get_cluster_id_check].clear();
    			  				            				averageDistance[get_cluster_id_check] = 0;
    			  				            				standardDeviation[get_cluster_id_check] = 0;
    			  			                  			}
    			  		  							}	
    	  		  									
    	  		  								}
    	  		  							}
    	  								//}
    	  							}else {
    	  								double d1 = getNodeDistance2(s_check,centroids[get_cluster_id_check]);
    	  								double d2 = getNodeDistance2(s_check,centroids[get_cluster_id]);
    	  								//if(d2<d1 && distilledText[get_cluster_id_check].size()<distilledText[get_cluster_id].size()) {
    	  								if(d2<d1) {
    	  									
    	  									can_ins_to_cluster = canInsertToThisCluster(s_check,get_cluster_id);
    	  									
    	  		  							if(can_ins_to_cluster==true) {
    	  		  								nf1.setProperty("clusterID", get_cluster_id);
    	  		  								nf1.setProperty("centroidL0", 0);
    	  		  								
    		  		  							if (!distilledText[get_cluster_id].contains(s_check)) {
    		  	          							distilledText[get_cluster_id].add(s_check);
    		  	          		      			}
    	  		  								
    	  		  								
    	  		  								//distilledText[get_cluster_id].add(s_check);
    	  		  								
    	  		  								for(int s=distilledText[get_cluster_id_check].size()-1;s>=0;s--) {
    	  		  									if(distilledText[get_cluster_id_check].get(s).toString().equals(s_check)) {
    	  		  										distilledText[get_cluster_id_check].remove(s);
    	  		  										s=-1;
    	  		  									}
    	  		  								}
    		
    	  				                  		//updateClusterAndPositionCentroid(get_cluster_id);

    	  		  								//if (!c_update.contains(get_cluster_id)) {
    	  		  								//	c_update.add(get_cluster_id);
    	  		  								//}

    	  		  								if(distilledText[get_cluster_id_check].size()>0) {
    	  				                  			
    	  				                  			//updateClusterAndPositionCentroid(get_cluster_id_check);

    	  		  									//if (!c_update.contains(get_cluster_id_check)) {
    	  		  									//	c_update.add(get_cluster_id_check);
    	  		  									//}
    		  				                  		
    	  			                  			}else {
    	  			                  				//clear data
    	  				                  			centroids[get_cluster_id_check]="";
    	  				                  			numDistance[get_cluster_id_check].clear();
    	  				            				averageDistance[get_cluster_id_check] = 0;
    	  				            				standardDeviation[get_cluster_id_check] = 0;
    	  			                  			}
    	  		  							}else {
    	  		  								
    	  		  								if (!c_update.contains(get_cluster_id)) {
    			  									
    			  									c_update.add(get_cluster_id);

    		  		  								updateClusterAndPositionCentroid(get_cluster_id);
    		  		  								
    		  		  								can_ins_to_cluster = canInsertToThisCluster(s_check,get_cluster_id);
    		  									
    			  		  							if(can_ins_to_cluster==true) {
    			  		  								nf1.setProperty("clusterID", get_cluster_id);
    			  		  								nf1.setProperty("centroidL0", 0);
    			  		  								
    				  		  							if (!distilledText[get_cluster_id].contains(s_check)) {
    				  	          							distilledText[get_cluster_id].add(s_check);
    				  	          		      			}
    			  		  								
    			  		  								
    			  		  								//distilledText[get_cluster_id].add(s_check);
    			  		  								
    			  		  								for(int s=distilledText[get_cluster_id_check].size()-1;s>=0;s--) {
    			  		  									if(distilledText[get_cluster_id_check].get(s).toString().equals(s_check)) {
    			  		  										distilledText[get_cluster_id_check].remove(s);
    			  		  										s=-1;
    			  		  									}
    			  		  								}
    				
    			  				                  		//updateClusterAndPositionCentroid(get_cluster_id);
    			
    			  		  								//if (!c_update.contains(get_cluster_id)) {
    			  		  								//	c_update.add(get_cluster_id);
    			  		  								//}
    			
    			  		  								if(distilledText[get_cluster_id_check].size()>0) {
    			  				                  			
    			  				                  			//updateClusterAndPositionCentroid(get_cluster_id_check);
    			
    			  		  									//if (!c_update.contains(get_cluster_id_check)) {
    			  		  									//	c_update.add(get_cluster_id_check);
    			  		  									//}
    				  				                  		
    			  			                  			}else {
    			  			                  				//clear data
    			  				                  			centroids[get_cluster_id_check]="";
    			  				                  			numDistance[get_cluster_id_check].clear();
    			  				            				averageDistance[get_cluster_id_check] = 0;
    			  				            				standardDeviation[get_cluster_id_check] = 0;
    			  			                  			}
    			  		  							}
    	  		  							
    	  		  								}

    	  		  							}
    	  								}
    	  								
    	  							}

    	          				}//end else c_cluster_before != count_cluster
          					}//end if not equal word
          					        				
          				}//end for p 
          				
  
          				
          				//update cluster 
          				//for(int u=0;u<c_update.size();u++) {
          					
          				//	updateClusterAndPositionCentroid(c_update.get(u));
          					
          				//}
          				
	
          			}
          		}
		
            }//for each word read     		        
            tx.success();
      	}
      	//graphDB.shutdown();
	}
	
	
	public void miniClusterMemberFindClusterAgain(int c_id,double num_member_recheck){
		boolean emergency = false;
		
		if(distilledText[c_id].size() < num_member_recheck) {
			System.out.println("miniClusterMemberFindClusterAgain...");
			System.out.println("num_member_recheck = "+num_member_recheck);
			System.out.println("member of cluster no."+c_id+" (in array) = "+distilledText[c_id].size()+"..."+distilledText[c_id].toString());
			
			Vector member_this_cluster = new Vector();
			Vector cid_different = new Vector(); //find clusterID of all node with has relation to different cluster
			
			try (Transaction txre = graphDB.beginTx()){
				ResourceIterator<Node> nodelist = graphDB.findNodes( Labels.SINGLE_NODE );
				while( nodelist.hasNext() ){
					Node wordnode = nodelist.next();
					if(wordnode.getProperty("clusterID").equals(c_id))	
					{
						String member = wordnode.getProperty("name").toString();
						member_this_cluster.add(member);
						
						Iterable<Relationship> allRelationships = wordnode.getRelationships();
						for (Relationship relationship : allRelationships) {
					    	
						    Node n2=relationship.getOtherNode(wordnode);
						    	
						    int ckid =(int)n2.getProperty("clusterID");
						    
						    if(ckid != c_id) {
						    	System.out.println("relation different cluster...");
						    	//if(distilledText[ckid].size()>num_member_recheck) {
						    		if (!cid_different.contains(ckid)) {
						    			cid_different.add(ckid);	
						    		}
						    	//}					    	
						    }					    
						}
					}
				}
							
				System.out.println("member of cluster no."+c_id+" (in db) = "+member_this_cluster.size()+"..."+member_this_cluster.toString());
				System.out.println("member of clusters that nodes have relation to differ cluster = "+cid_different.size()+" clusters");
				
				if(cid_different.size()>0) {
					boolean can_ins_to_cluster = false;
					for(int m=0;m<member_this_cluster.size();m++) {
						String node_move = member_this_cluster.get(m).toString();
						
						double d_final = Double.MAX_VALUE;
						int cid_final=0;
						
						for(int dc=0;dc<cid_different.size();dc++) {
							int cid_move = (int)cid_different.get(dc);
					    	can_ins_to_cluster = canInsertToThisCluster(node_move,cid_move);
					    	if(can_ins_to_cluster == true) {
					    		double d = getNodeDistance2(node_move,centroids[cid_move]);
					    		if(d<d_final) {
							    	d_final=d;
							    	cid_final = cid_move;
							    }		    		
					    	}else {
					    		if (!c_update_minicluster.contains(cid_move)) {
  									
					    			c_update_minicluster.add(cid_move);
					    			
						    		updateClusterAndPositionCentroid(cid_move);
						    		can_ins_to_cluster = canInsertToThisCluster(node_move,cid_move);
						    		
						    		if(can_ins_to_cluster == true) {
							    		double d = getNodeDistance2(node_move,centroids[cid_move]);
							    		if(d<d_final) {
									    	d_final=d;
									    	cid_final = cid_move;
									    }		    		
							    	}
					    		}
					    	}
						}

						if(cid_final!=0) {
							System.out.println("cid_final="+cid_final);
							System.out.println(node_move+" can move...");
							Node n_move = graphDB.findNode(Labels.SINGLE_NODE,"name", node_move);
							n_move.setProperty("clusterID", cid_final);
							n_move.setProperty("centroidL0", 0);	
								
							if (!distilledText[cid_final].contains(node_move)) {
		    					distilledText[cid_final].add(node_move);
		    		      	}
								
							for(int s=distilledText[c_id].size()-1;s>=0;s--) {
								if(distilledText[c_id].get(s).toString().equals(node_move)) {
									distilledText[c_id].remove(s);
									s=-1;
										
								}
							}
						}else {
							//for emergency case
							d_final = Double.MAX_VALUE;
							for(int dc=0;dc<cid_different.size();dc++) {
								int cid_move = (int)cid_different.get(dc);
								System.out.println("It cannot move in basic case, So to check in emergency case...!!!");
								can_ins_to_cluster = canInsertToThisClusterEmergencyCase(node_move,cid_move);
								if(can_ins_to_cluster == true) {
									double d = getNodeDistance2(node_move,centroids[cid_move]);
									if(d<d_final) {
										d_final=d;
										cid_final = cid_move;
						    		}
						    	}
							}
								
							if(cid_final!=0) {
								
								emergency = true;
								
								System.out.println("cid_final="+cid_final);
								System.out.println(node_move+" can move...");
								Node n_move = graphDB.findNode(Labels.SINGLE_NODE,"name", node_move);
								n_move.setProperty("clusterID", cid_final);
								n_move.setProperty("centroidL0", 0);	
									
								if (!distilledText[cid_final].contains(node_move)) {
			    					distilledText[cid_final].add(node_move);
			    		      	}
									
								for(int s=distilledText[c_id].size()-1;s>=0;s--) {
									if(distilledText[c_id].get(s).toString().equals(node_move)) {
										distilledText[c_id].remove(s);
										s=-1;
											
									}
								}
								
							}else {
								System.out.println(node_move+" cannot move...!!!");
							}
						}
						
					}
			
				}else {
					System.out.println("No different cluster to move.....");
				}
				
	
				if(distilledText[c_id].size()>0) {
					numClustercanNotMove++;
					System.out.println("cannot move....."+distilledText[c_id].size()+" nodes");
			
				}else {
					if(emergency==false) {
						numClusterMoveByBasic++;
					}else {
						numClusterMoveByEmergency++;
					}
					System.out.println("can move.....all nodes");
					//clear data
					centroids[c_id]="";
					numDistance[c_id].clear();
					averageDistance[c_id] = 0;
					standardDeviation[c_id] = 0;
				}
	
				txre.success();

			}
		}
	}

	public int getNumCluster() {
		int count = 0;
		for(int p=1;p<=count_cluster;p++) {
			if(distilledText[p].size()>0) {
				count++;
			}	
		}
		return count;
	}

	public boolean canInsertToThisCluster(String word,int c_id) {
		double area = 0;
		double d=0;
		boolean w_can = false;
		area = averageDistance[c_id]+(numMultiplySigma*standardDeviation[c_id]);
		
		System.out.println("!!! To check word'"+word+"' can insert into cluster no:"+c_id+" !!!");
		System.out.println("Cluster no."+c_id);
		//System.out.println("-->>Average Distance = " + averageDistance[c_id]);
		//System.out.println("-->>Standard Deviation = " + standardDeviation[c_id]);			
		System.out.println("-->>average + "+numMultiplySigma+"*SD : " + area);
		//System.out.println("-->>numDistance: " + numDistance[c_id].toString());
					

		d = getNodeDistance2(word,centroids[c_id]);
		
		System.out.println("-->>d of word '"+word+"' = " + d);
		if(d<=area) {
			w_can = true;
			System.out.println("it can");
		}else {
			w_can = false;
			System.out.println("it cannot");
		}
		return w_can;
	}
	
	public boolean canInsertToThisClusterEmergencyCase(String word,int c_id) {
		double area = 0;
		double d=0;
		boolean w_can = false;
		area = averageDistance[c_id]+(numMultiplySigma*standardDeviation[c_id]);
		
		System.out.println("!!! To check word'"+word+"' can insert into cluster no:"+c_id+" !!!");
		System.out.println("Cluster no."+c_id);
		//System.out.println("-->>Average Distance = " + averageDistance[c_id]);
		//System.out.println("-->>Standard Deviation = " + standardDeviation[c_id]);			
		System.out.println("-->>average + "+numMultiplySigma+"*SD : " + area);
		//System.out.println("-->>numDistance: " + numDistance[c_id].toString());
					

		d = getNodeDistance2(word,centroids[c_id]);
		
		System.out.println("-->>d of word '"+word+"' = " + d);
		//if(d<=area) {
		if(d != 0) {
			w_can = true;
			System.out.println("it can insert in emergency case (d > average+"+numMultiplySigma+"sigma)");
		}else {
			w_can = false;
			System.out.println("it cannot");
		}
		return w_can;
	}	

	

	public double getNodeDistance2(String term1, String term2) {
	
		double distance=Double.MAX_VALUE;
	
		try (Transaction tx = graphDB.beginTx()) {  
		
			Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", term1);
		
			if (temp!=null) {
			
				Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", term2);
			
				if (temp2!=null) {
					
					PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra( PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), "cost", 1 );

					WeightedPath p = finder.findSinglePath( temp, temp2 );
		
					if (p!=null) {
					
						distance = p.weight();
						
					}
				}
			}
			tx.success();
		}
		
		return distance;
	}


	private int getLanguage(File satzFile) {

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

	private boolean isStopWord(String word){
		boolean isstopword = false;
		
		Parameters p = new Parameters(true); //Parameters.getInstance();
		ExternalData ed = ExternalData.getInstance( language  ); //p.getLanguage());
		Set stopWords = ed.getStopWordMap();
		
		char firstcharacter = word.charAt(0);
				
		
		if (Character.isUpperCase(firstcharacter)) {
			
			if(stopWords.contains(word)) {
				
				isstopword=true;
				
			} else {
				
				int length = word.length();
				String s2 = word.substring(0,1).toLowerCase().concat(word.substring(1, length));
				
				if(stopWords.contains(s2)) {

					isstopword=true;
				}
				
			}
			
		} else {
			
			if(stopWords.contains(word)) {
				
				isstopword=true;
			}
			
		}
	
				
		return isstopword;
	}
	

	
	//this function when called, updates the Dice ratio and costs for all the relationships present in the database
	public void updateDiceandCosts()
	{
		int countA, countB, countAB;
		double dice,cost;
		System.out.println("\n###In update properties of relationship###");
		

		//String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		//File database = new File(db_path);
		
		//GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(database);
		try (Transaction tx5 = graphDB.beginTx()) 
	    {  	
			ResourceIterator<Node> nodelist = graphDB.findNodes( Labels.SINGLE_NODE );
			while(nodelist.hasNext())
			{
				Node wordnode = nodelist.next();
				//al.add((String)user.getProperty("name"));
				String node1=(String)wordnode.getProperty("name");
				countA=(int)wordnode.getProperty("occur");
				Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", node1);
				Iterable<Relationship> allRelationships = temp.getRelationships();
			    for (Relationship relationship : allRelationships) 
			    {
			    	Node n2=relationship.getOtherNode(temp);
			    	String node2=(String)n2.getProperty("name");
			        countB=(int)n2.getProperty("occur");
			        countAB=(int)relationship.getProperty("count");
			        
			        /***********  ********/
			        
			        int helpk=0;
			        
			        if (countB<=countA) { helpk=countB; } else
						helpk=countA;
					
					if (countAB>=helpk)
						countAB=helpk;
			        
					/***************************************/			

			        dice=(double)(2*countAB)/(countA+countB);
			        
			        if (dice>1) dice=1.0;
			        
			        relationship.setProperty("dice", dice);
			        cost = (double)(1/(dice+0.01));
			        relationship.setProperty("cost", cost);
			        
//			        updateRelationPropertyDiceAndCost(node1,node2, dice, cost);
			        
			    }
			}
			System.out.println("Update of Dice finished.");
			tx5.success();
	    }
		//graphDB.shutdown();
	}
	
	/*public void updateCluster() {
		//calculate an average distance
		double d=0,d1=0,d2=0,sum=0,sd=0;
		
		System.out.println("\n\n----------------Starting to update Cluster");
		
		for(int p=1;p<=count_cluster;p++) {
			
			if(distilledText[p].size()>0) {
				
				sum=0;
				sd=0;
				
				System.out.println("##After");
				
				numDistance[p].clear();
				averageDistance[p] = 0;
				standardDeviation[p] = 0;
				
				System.out.println("Cluster no."+p+" ");
				for(int c=0;c<distilledText[p].size();c++) {
					d=0;
					d = getNodeDistance2(distilledText[p].get(c).toString(),centroids[p]);
					
					sum = sum + d;
					
					Vector nextDistance = new Vector();
					nextDistance.add(distilledText[p].get(c).toString());
					nextDistance.add(d);
									
					numDistance[p].add(nextDistance);
				}

				averageDistance[p] = sum/distilledText[p].size();
				System.out.println("-->>Average Distance = " + averageDistance[p]);
				
				for(int n=0;n<numDistance[p].size();n++) {
					
					Vector v = (Vector)numDistance[p].get(n);
					
					sd = sd+ Math.pow(((Double)v.get(1))-averageDistance[p],2);
				}
				
				standardDeviation[p] = Math.sqrt(sd/numDistance[p].size());	
				
				System.out.println("-->>Standard Deviation = " + standardDeviation[p]);			
				System.out.println("-->>average + 3*SD : " + (averageDistance[p]+(3*standardDeviation[p])));
				System.out.println("-->>numDistance: " + numDistance[p].toString());
				System.out.println("");
			}
		}
		System.out.println("End of update cluster---------------\n\n");

	}*/
	
	/*public void updateCluster(int clusterID) {
		//calculate an average distance
		double d=0,d1=0,d2=0,sum=0,sd=0;
		
		System.out.println("\n\n----------------Starting to update Cluster no."+clusterID);
		int p=clusterID;
		//for(int p=1;p<=count_cluster;p++) {
			
			if(distilledText[p].size()>0) {
				
				sum=0;
				sd=0;
				
				System.out.println("##After");
				
				numDistance[p].clear();
				averageDistance[p] = 0;
				standardDeviation[p] = 0;
				
				System.out.println("Cluster no."+p+" ");
				for(int c=0;c<distilledText[p].size();c++) {
					d=0;
					d = getNodeDistance2(distilledText[p].get(c).toString(),centroids[p]);
					
					sum = sum + d;
					
					Vector nextDistance = new Vector();
					nextDistance.add(distilledText[p].get(c).toString());
					nextDistance.add(d);
									
					numDistance[p].add(nextDistance);
				}

				averageDistance[p] = sum/distilledText[p].size();
				System.out.println("-->>Average Distance = " + averageDistance[p]);
				
				for(int n=0;n<numDistance[p].size();n++) {
					
					Vector v = (Vector)numDistance[p].get(n);
					
					sd = sd+ Math.pow(((Double)v.get(1))-averageDistance[p],2);
				}
				
				standardDeviation[p] = Math.sqrt(sd/numDistance[p].size());	
				
				System.out.println("-->>Standard Deviation = " + standardDeviation[p]);			
				System.out.println("-->>average + 3*SD : " + (averageDistance[p]+(3*standardDeviation[p])));
				System.out.println("-->>numDistance: " + numDistance[p].toString());
				System.out.println("");
			}
		//}
		System.out.println("End of update cluster---------------\n\n");

	}
	*/
	
	
	public void updateClusterAndPositionCentroid(int c_id) {
		//calculate an average distance
		double d=0,sum=0,sd=0;
		String centroid_old = "";
		String centroid_new = "";
		System.out.println("\n\n----------------Starting to update Cluster no."+c_id);
		
		if(distilledText[c_id].size()>0) {
			
			//clear data
			//currentEvolvingCentroid[p].clear();
			//currentPath[p].clear(); 
			//centroidTrail[p].clear(); 
			//count_existingwords[p]=0;
			
			updateDiceandCosts();
			
			//HashSet mostfreqterms = new HashSet();
			//mostfreqterms = findMostfrequentterms(p);
			//getEvolvingCentroid(mostfreqterms,p);
			
			centroid_old = centroids[c_id];
			getCentroidbySpreadingActivation(c_id);
			
			
			sum=0;
			sd=0;
			
			System.out.println("##After");
			
			numDistance[c_id].clear();
			averageDistance[c_id] = 0;
			standardDeviation[c_id] = 0;
			
			System.out.println("Cluster no."+c_id+" ");
			

			if(distilledText[c_id].size()>2) {
				for(int c=0;c<distilledText[c_id].size();c++) {
					
					d=0;
					d = getNodeDistance2(distilledText[c_id].get(c).toString(),centroids[c_id]);
					
					sum = sum + d;
					
					Vector nextDistance = new Vector();
					nextDistance.add(distilledText[c_id].get(c).toString());
					nextDistance.add(d);
									
					numDistance[c_id].add(nextDistance);
				}

				averageDistance[c_id] = sum/distilledText[c_id].size();
				System.out.println("-->>Average Distance = " + averageDistance[c_id]);
				
				for(int n=0;n<numDistance[c_id].size();n++) {
					
					Vector v = (Vector)numDistance[c_id].get(n);
					
					sd = sd+ Math.pow(((Double)v.get(1))-averageDistance[c_id],2);
				}
				
				standardDeviation[c_id] = Math.sqrt(sd/numDistance[c_id].size());	
				
			}else{ //because when nodes = 2 , sd = 0
				
				if(distilledText[c_id].size() == 2) {
					for(int c=0;c<distilledText[c_id].size();c++) {
						
						d=0;
						d = getNodeDistance2(distilledText[c_id].get(c).toString(),centroids[c_id]);
						
						sum = sum + d;
						
						Vector nextDistance = new Vector();
						nextDistance.add(distilledText[c_id].get(c).toString());
						nextDistance.add(d);
										
						numDistance[c_id].add(nextDistance);
					}

					averageDistance[c_id] = sum/distilledText[c_id].size();
					System.out.println("-->>Average Distance = " + averageDistance[c_id]);
					
					
					standardDeviation[c_id] = (double)(sum/2);
				}
				if(distilledText[c_id].size() == 1) {
					
					d=1;
						
					sum = 1;
						
					Vector nextDistance = new Vector();
					nextDistance.add(distilledText[c_id].get(0).toString());
					nextDistance.add(d);
										
					numDistance[c_id].add(nextDistance);
					

					averageDistance[c_id] = sum/distilledText[c_id].size();
					System.out.println("-->>Average Distance = " + averageDistance[c_id]);
					
					
					standardDeviation[c_id] = (double)(sum/2);
				}
				
			}

			System.out.println("-->>Standard Deviation = " + standardDeviation[c_id]);			
			System.out.println("-->>average + "+numMultiplySigma+"*SD : " + (averageDistance[c_id]+(numMultiplySigma*standardDeviation[c_id])));
			//System.out.println("-->>numDistance: " + numDistance[c_id].toString());
			System.out.println("");
			
			System.out.println("End of update cluster and the position of centroid---------------\n\n");
			centroid_new = centroids[c_id];
			
			if(!centroid_old.equals(centroid_new)) {
				
				//Node co = graphDB.findNode(Labels.SINGLE_NODE,"name", centroid_old);
				//co.setProperty("centroidL0", 0);
				//Node cn = graphDB.findNode(Labels.SINGLE_NODE,"name", centroid_new);
				//cn.setProperty("centroidL0", 1);
				
				
				System.out.println("Starting to recheck members this cluster (because cluster center move)---------------\n\n");
				recheckMembersThisCluster(c_id);
			}
		
		}	
	}
	
	public void recheckMembersThisCluster(int c_id) {
		try (Transaction txre = graphDB.beginTx()){
			ResourceIterator<Node> nodelist = graphDB.findNodes( Labels.SINGLE_NODE );
			String member = "";
			Vector<Integer> c_update = new Vector<Integer>();
			boolean can_ins_to_cluster = false;
			while( nodelist.hasNext() )
			{
				Node wordnode = nodelist.next();
				if(wordnode.getProperty("clusterID").equals(c_id))	
				{
					can_ins_to_cluster = false;
					member=wordnode.getProperty("name").toString();
										
					can_ins_to_cluster = canInsertToThisCluster(member,c_id);
					if(can_ins_to_cluster==false) {
						
						double d_final = Double.MAX_VALUE;
						int cid_final=0;
						
						Iterable<Relationship> allRelationships = wordnode.getRelationships();
						for (Relationship relationship : allRelationships) {
					    	
					    	Node n2=relationship.getOtherNode(wordnode);
					    	
					    	int ckid =(int)n2.getProperty("clusterID");
					    	double d = getNodeDistance2(member,centroids[ckid]);
					    	can_ins_to_cluster = canInsertToThisCluster(member,ckid);
					    	if(d<d_final && ckid != c_id && can_ins_to_cluster==true) {
					    		d_final=d;
					    		cid_final = ckid;
					    	}
					    	
					    }
						if(cid_final!=0) {
							System.out.println("cid_final="+cid_final);
							System.out.println("move...");
							wordnode.setProperty("clusterID", cid_final);
							wordnode.setProperty("centroidL0", 0);
							
							if (!distilledText[cid_final].contains(member)) {
        							distilledText[cid_final].add(member);
        		      		}
							
							//distilledText[cid_final].add(member);
							
							for(int s=distilledText[c_id].size()-1;s>=0;s--) {
								if(distilledText[c_id].get(s).toString().equals(member)) {
									distilledText[c_id].remove(s);
									s=-1;
									
								}
							}
							
							//updateClusterAndPositionCentroid(cid_final);
							//if (!c_update.contains(cid_final)) {
								
							//	c_update.add(cid_final);
							//}
							
					    }else {
	
					    	count_cluster++;
					    	wordnode.setProperty("clusterID", count_cluster);
					    	wordnode.setProperty("centroidL0", 1);
	          				distilledText[count_cluster].add(member);
	          					
	          				
	          				for(int s=distilledText[c_id].size()-1;s>=0;s--) {
								if(distilledText[c_id].get(s).toString().equals(member)) {
									distilledText[c_id].remove(s);
									s=-1;
								}
							}
	          				
	          				//updateDiceandCosts();
	          				//getEvolvingCentroid(word,count_cluster);
	          				centroids[count_cluster] = member;
	          				updateClusterAndPositionCentroid(count_cluster);
	          				
					    }
						
					}
				}
				
			}//end while

			if(distilledText[c_id].size()>0) {
					
				//updateClusterAndPositionCentroid(c_id);
				//if (!c_update.contains(c_id)) {
				//	c_update.add(c_id);
				//}
		
			}else {
				//clear data
				centroids[c_id]="";
				numDistance[c_id].clear();
				averageDistance[c_id] = 0;
				standardDeviation[c_id] = 0;
			}

			//update cluster 
			//for(int u=0;u<c_update.size();u++) {
				
			//	updateClusterAndPositionCentroid(c_update.get(u));
				
			//}
			
			txre.success();
        }
	}
	
	public HashSet findMostfrequentterms(int clusterID) {
		
		List termlist = new Vector();
		float [][] cooccmatrix;
		
		int keyindex1 = -1,keyindex2 = -1;
		float curSig = 0;
		
		for(int t=0;t<distilledText[clusterID].size();t++) {
			termlist.add(distilledText[clusterID].get(t).toString());
		}
		
		cooccmatrix = new float[termlist.size()][termlist.size()];

		try (Transaction txfre = graphDB.beginTx()){
					
			for(Iterator i=termlist.iterator(); i.hasNext(); ){
				keyindex1 = -1;
				
				String word = (String)i.next();
				Node n1 = graphDB.findNode(Labels.SINGLE_NODE,"name", word);
				
				keyindex1 = findEntry(termlist, word);
				
				Iterable<Relationship> allRelationships = n1.getRelationships();
			    for (Relationship relationship : allRelationships) {
			    	
			    	Node n2=relationship.getOtherNode(n1);
			    	
			    	if(n2.equals(relationship.getOtherNode(n1))){
			    		keyindex2 = -1;
			    		keyindex2 = findEntry(termlist, (String)n2.getProperty("name"));
				    	curSig=(int)relationship.getProperty("count");
				    	
				    	if ((keyindex1!=-1) && (keyindex2!=-1)) {
				    		if (keyindex1==keyindex2) {
				    			curSig=1;
				    			cooccmatrix[keyindex1][keyindex2] = curSig;
				    			//Scaling *100 when using DICE coefficient; if LL: not necessary
				    		} else {
				    			if (curSig>0) {
				    				cooccmatrix[keyindex1][keyindex2] = curSig;
									//Scaling *100 when using DICE coefficient; if LL: not necessary
				    			} else {
				    				cooccmatrix[keyindex1][keyindex2] = (float) 0.01;
				    			}
				    		}
				    	}
			    	}
				        
			    }//end for relationships
				
			}//end for read word on termlist
		
			txfre.success();
		}
		
		List pageranks = calculatePageRanks(termlist, cooccmatrix);
		
		int maxentries = 1000000; //200;
		if (pageranks.size()<maxentries) maxentries=pageranks.size();
		
		HashSet mostfrequentterms = new HashSet();

		for (int j=0; j<maxentries; j++) {
	  
			Word curWord = (Word) pageranks.get(j);
	  
			//System.out.println("PageRank of " +curWord.getWordStr() + ": " + curWord.getSig());
	  
			//writeLinesToFile(outputDirPath+""+curFile.getName()+"_termvector.txt", new String[] {""+curWord.getWordStr() }, true);
	  
			if (mostfrequentterms.size()<25) 
				mostfrequentterms.add(curWord.getWordStr());
		}

		return mostfrequentterms;	
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
                    
		for (int i=0;i<termlist.size();i++){
			pagerank[i]=(1-d);
		}
                    
		float [] out = new float [termlist.size()];
                    
		for (int i = 0; i < termlist.size(); i++){
			out[i]=0;
			for (int j = 0; j < termlist.size(); j++) {
				if (curgraph[i][j]!=0)
					out[i]= out[i] + 1; //curgraph[i][j]; //+1
			}
		}
                    
		//Pageranks berechnen
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
	
	/*
	public void getCentroidbySpreadingActivation(int c_id)
	{
		Vector query = new Vector();
		
		//HashMap result = new HashMap();
		
		//int originalquerysize = query.size();
		String centroid = "";
		double shortestaveragepathlength = Double.MAX_VALUE;
		double timeelapsed = 0.0;	
		
		HashMap centroidcandidatesdata = new HashMap();
		
		Vector termcolors = new Vector();
		HashMap node2colors = new HashMap();
		
		//double arearadius = 10.0;
		
		Vector centroidcandidates = new Vector();
		
		//String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		//File database = new File(db_path);
		
		try (Transaction tx1 = graphDB.beginTx()){
			ResourceIterator<Node> nodelist = graphDB.findNodes( Labels.SINGLE_NODE );
			Vector helpquery = new Vector();
			while( nodelist.hasNext() )
			{
				Node wordnode = nodelist.next();
				if(wordnode.getProperty("clusterID").equals(c_id))	
				{
					helpquery.add(wordnode.getProperty("name").toString());
				}
				
			}
			if(helpquery.size()>1) {
				
				query = helpquery;
				System.out.println("Helpquery size: " +query.size() + "  "+query);;
				
				
				//Check if all query terms can be reached in the graph database from one another 
				//(remove one that cannot be reached by one or all of the other terms)
				//
				
				//int helpquerysize = query.size();
				
				HashSet helpqueryset = new HashSet();
				HashSet helpqueryset2bremoved = new HashSet();
				
				Vector helpquery2 = new Vector();
				
				for (int i=0; i<query.size(); i++) {
					
					helpqueryset.add(query.get(i).toString());
					
				}
				
				HashMap numberofreachednodes = new HashMap();
				Iterator iteratorq1 = helpqueryset.iterator(); 

				while (iteratorq1.hasNext()){
						   
					String queryterm = iteratorq1.next().toString();
						   
					Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm);
						   							   
					Iterator iteratorq2 = helpqueryset.iterator(); 

					while (iteratorq2.hasNext()){
						
						String queryterm2 = iteratorq2.next().toString(); 
								   
						if (!queryterm.equals(queryterm2)) {
							Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm2);
									   
							PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), 100, 1 );

							Path p = finder.findSinglePath( temp, temp2 );
									
							if (p!=null) {
								if (numberofreachednodes.containsKey(queryterm)) {
												
									HashSet helpset = (HashSet)numberofreachednodes.get(queryterm);
									helpset.add(queryterm2);
									numberofreachednodes.put(queryterm, helpset);
												
								} else {
												
									HashSet helpset= new HashSet();
									helpset.add(queryterm2);
									numberofreachednodes.put(queryterm, helpset);
								}
							}
						}
					}
				}
					   
				System.out.println("Number of reached nodes: " +numberofreachednodes.toString());;
					   
				String mostreachableterm = "";
				int numberofneighbours = 0;
					   
				Iterator iteratorq3 = numberofreachednodes.keySet().iterator();
				while (iteratorq3.hasNext()){
					String queryterm = iteratorq3.next().toString(); 
						   
					HashSet helphashset = (HashSet) numberofreachednodes.get(queryterm);
						   
					if (helphashset.size()>numberofneighbours) {
							   
						numberofneighbours = helphashset.size();
						mostreachableterm = queryterm;
					}
				}
					
				HashSet helphashset = (HashSet) numberofreachednodes.get(mostreachableterm);
				helphashset.add(mostreachableterm);
					   
				helpqueryset = helphashset;
					
				for (int i=0; i<query.size(); i++) {
					
					String helpterm = query.get(i).toString();
					
					if (helpqueryset.contains(helpterm)) {
						
						helpquery2.add(helpterm);
						
					}
					
				}
				
				
				query = helpquery2;
				System.out.println("Helpquery2 size: " +query.size() + "  "+query);;
				
			
				//hierher2
				if ((query.size()==query.size())  && (query.size()>1)) {

					double count=0; 
				
					long start = System.currentTimeMillis();
					
					int cenCandidatesSize = 2;
					if(query.size()<cenCandidatesSize) cenCandidatesSize = query.size();
				
					while (centroidcandidates.size()<cenCandidatesSize) {

						count++;
						System.out.println("Activation rounds to execute: " +count );

						//if (count>2) 
							//arearadius = arearadius + (arearadius / 2.0); 
				
						termcolors = new Vector();
						node2colors = new HashMap();	
						centroidcandidates = new Vector();
					
						int color = 0;
				
						for(Iterator i=query.iterator(); i.hasNext(); ){
					
							color++;
							termcolors.add(color);
										
							String curQueryTerm = i.next().toString();
					
							HashSet helpset = new HashSet();
							helpset.add(color);
					
							node2colors.put(curQueryTerm, helpset);
							//System.out.println("Query Term: " + curQueryTerm + " Color: " + color);
						}
			
						for(Iterator i=query.iterator(); i.hasNext(); ){
				
							String curQueryTerm = i.next().toString();
				
							HashSet visited = new HashSet();
							LinkedList<String> queue = new LinkedList<String>();
				
							visited.add(curQueryTerm);
							queue.add(curQueryTerm);
				
							Node firstsourcenode = graphDB.findNode(Labels.SINGLE_NODE,"name", curQueryTerm);
			
							int steps=0;
				
							while ((queue.size()!=0) && (steps<count)) {
					
								steps++;
					
								String sourcenodename = queue.poll();
								//System.out.println("Activating: "+ sourcenodename);
											
								Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", sourcenodename);
								Iterable<Relationship> allRelationships = temp.getRelationships();
								for (Relationship relationship : allRelationships) 
								{
									Node destinationnode=relationship.getOtherNode(temp);
				       
									String destinationnodename =  destinationnode.getProperty("name").toString();
				       
									if (!visited.contains(destinationnodename)) {

										
										
				    	   
										visited.add(destinationnodename);
										queue.add(destinationnodename);
				    	   
									}
								}
							} //while queue
				
							//HashSet help = (HashSet)node2colors.get(curQueryTerm);
				
							Iterator iterator = visited.iterator(); 
			      
							// check values
							while (iterator.hasNext()){
					   
								String nodename = iterator.next().toString();
							    //System.out.println("Value: "+ nodename+ " ");  
					   
								if (node2colors.containsKey(nodename)) {
						   
									HashSet helpset =  (HashSet)node2colors.get(nodename);
						   
									helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
						   
									node2colors.put(nodename, helpset);
						   
								} else {
						   
									HashSet helpset = new HashSet();
									helpset.add(termcolors.get(query.indexOf(curQueryTerm)));

									node2colors.put(nodename, helpset);
						   
								}
							}
				
							//System.out.println("Value: "+ node2colors.get(curQueryTerm).toString()); 
							//System.out.println("Value: "+ node2colors.get("Grenzwert").toString());
						} //for all query terms
			
						Iterator iterator = node2colors.keySet().iterator(); 
		      
						// check values
						while (iterator.hasNext()){
				   
							String nodename = iterator.next().toString();
				   
							if ((((HashSet)node2colors.get(nodename)).size()==termcolors.size()) && (helpquery.contains(nodename)) ) {
								centroidcandidates.add(nodename);
							}
						}
						System.out.println("Centroid candidates: " +centroidcandidates.size() + "  " + centroidcandidates.toString());
			
					} // centroidcandidates.size()<5
				
					long stop = System.currentTimeMillis();
					timeelapsed = ((double)(stop-start)/(double)1000);
						
					System.out.println("Centroid determination took "+timeelapsed+" seconds.");
				
				
					double averagepathlength = 0;
				
					for (int i=0; i<centroidcandidates.size(); i++) {
					
						String candidate = centroidcandidates.get(i).toString();
						averagepathlength=0;
					
						Node n1 = graphDB.findNode(Labels.SINGLE_NODE,"name", candidate);
						
						for (int j=0; j<query.size(); j++) {
						
							String curQueryTerm = query.get(j).toString();
						
							Node n2 = graphDB.findNode(Labels.SINGLE_NODE,"name", curQueryTerm);
						
							if ((n1!=null) && (n2!=null)) {
							
								PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra( PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), "cost", 1 );
							   
								WeightedPath p = finder.findSinglePath(n1, n2);
							
								if (p!=null) {
								
									averagepathlength+=p.weight();
								
								}
							}
						}
					
						averagepathlength = averagepathlength / query.size();
					
						centroidcandidatesdata.put(candidate, averagepathlength);
					
						if (averagepathlength<shortestaveragepathlength){
						
							shortestaveragepathlength = averagepathlength;
							centroid = candidate;
						}
					} //for centroidcandidates
				} // if querysize==
			   
				
				
				System.out.println("node2colors: " + node2colors.size() + "   " +centroid +  "   " + shortestaveragepathlength);
				
				//result.put("centroid", centroid);
				//result.put("shortestaveragepathlength", shortestaveragepathlength);
				//result.put("activatednodes", node2colors.size());
				//result.put("timeelapsed", timeelapsed);
				//result.put("centroidcandidatesdata", centroidcandidatesdata);
				
				centroids[c_id]=centroid;
				
				
			}else if(helpquery.size()==1){
				centroids[c_id]=helpquery.get(0).toString();
			}
			tx1.success();
		}
		//return result;
	}

	public int getClusterIDwithCentroidbySpreadingActivation(Vector query_find)
	{
		
		System.out.println("Starting to get Centroid is,,,");
		
		//HashMap result = new HashMap();
		int cluster_id=0;

		Vector query = new Vector();
		
		//HashMap result = new HashMap();
		
		//int originalquerysize = query.size();
		String centroid = "";
		double shortestaveragepathlength = Double.MAX_VALUE;
		double timeelapsed = 0.0;	
		
		HashMap centroidcandidatesdata = new HashMap();
		
		Vector termcolors = new Vector();
		HashMap node2colors = new HashMap();
		
		//double arearadius = 10.0;
		
		Vector centroidcandidates = new Vector();
		
		//String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		//File database = new File(db_path);
		
		try (Transaction tx1 = graphDB.beginTx()){
			
			Vector helpquery = new Vector();
			
			
			for (int i=0; i<query_find.size(); i++) {
				Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", query_find.get(i).toString());
				
				if (temp!=null) {
					helpquery.add(query_find.get(i).toString());
				}
				
			}
			
			if(helpquery.size()>1) {
				
				query = helpquery;
				System.out.println("Helpquery size: " +query.size() + "  "+query);;
				
				
				//Check if all query terms can be reached in the graph database from one another 
				//(remove one that cannot be reached by one or all of the other terms)
				//
				
				//int helpquerysize = query.size();
				
				HashSet helpqueryset = new HashSet();
				HashSet helpqueryset2bremoved = new HashSet();
				
				Vector helpquery2 = new Vector();
				
				for (int i=0; i<query.size(); i++) {
					
					helpqueryset.add(query.get(i).toString());
					
				}
				
				HashMap numberofreachednodes = new HashMap();
				Iterator iteratorq1 = helpqueryset.iterator(); 

				while (iteratorq1.hasNext()){
						   
					String queryterm = iteratorq1.next().toString();
						   
					Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm);
						   							   
					Iterator iteratorq2 = helpqueryset.iterator(); 

					while (iteratorq2.hasNext()){
						
						String queryterm2 = iteratorq2.next().toString(); 
								   
						if (!queryterm.equals(queryterm2)) {
							Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm2);
									   
							PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), 100, 1 );

							Path p = finder.findSinglePath( temp, temp2 );
									
							if (p!=null) {
								if (numberofreachednodes.containsKey(queryterm)) {
												
									HashSet helpset = (HashSet)numberofreachednodes.get(queryterm);
									helpset.add(queryterm2);
									numberofreachednodes.put(queryterm, helpset);
												
								} else {
												
									HashSet helpset= new HashSet();
									helpset.add(queryterm2);
									numberofreachednodes.put(queryterm, helpset);
								}
							}
						}
					}
				}
					   
				System.out.println("Number of reached nodes: " +numberofreachednodes.toString());;
					   
				String mostreachableterm = "";
				int numberofneighbours = 0;
					   
				Iterator iteratorq3 = numberofreachednodes.keySet().iterator();
				while (iteratorq3.hasNext()){
					String queryterm = iteratorq3.next().toString(); 
						   
					HashSet helphashset = (HashSet) numberofreachednodes.get(queryterm);
						   
					if (helphashset.size()>numberofneighbours) {
							   
						numberofneighbours = helphashset.size();
						mostreachableterm = queryterm;
					}
				}
					
				HashSet helphashset = (HashSet) numberofreachednodes.get(mostreachableterm);
				helphashset.add(mostreachableterm);
					   
				helpqueryset = helphashset;
					
				for (int i=0; i<query.size(); i++) {
					
					String helpterm = query.get(i).toString();
					
					if (helpqueryset.contains(helpterm)) {
						
						helpquery2.add(helpterm);
						
					}
					
				}
				
				
				query = helpquery2;
				System.out.println("Helpquery2 size: " +query.size() + "  "+query);;
				
			
				//hierher2
				if ((query.size()==query.size())  && (query.size()>1)) {
					
					double count=0; 
				
					long start = System.currentTimeMillis();
					
					int cenCandidatesSize = 2;
					if(query.size()<cenCandidatesSize) cenCandidatesSize = query.size();
				
					while (centroidcandidates.size()<cenCandidatesSize) {

						count++;
						System.out.println("Activation rounds to execute: " +count );

						//if (count>2) 
						//	arearadius = arearadius + (arearadius / 2.0); 
				
						termcolors = new Vector();
						node2colors = new HashMap();	
						centroidcandidates = new Vector();
					
						int color = 0;
				
						for(Iterator i=query.iterator(); i.hasNext(); ){
					
							color++;
							termcolors.add(color);
										
							String curQueryTerm = i.next().toString();
					
							HashSet helpset = new HashSet();
							helpset.add(color);
					
							node2colors.put(curQueryTerm, helpset);
							//System.out.println("Query Term: " + curQueryTerm + " Color: " + color);
						}
			
						for(Iterator i=query.iterator(); i.hasNext(); ){
				
							String curQueryTerm = i.next().toString();
				
							HashSet visited = new HashSet();
							LinkedList<String> queue = new LinkedList<String>();
				
							visited.add(curQueryTerm);
							queue.add(curQueryTerm);
				
							Node firstsourcenode = graphDB.findNode(Labels.SINGLE_NODE,"name", curQueryTerm);
			
							int steps=0;
				
							while ((queue.size()!=0) && (steps<count)) {
					
								steps++;
					
								String sourcenodename = queue.poll();
								//System.out.println("Activating: "+ sourcenodename);
											
								Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", sourcenodename);
								Iterable<Relationship> allRelationships = temp.getRelationships();
								for (Relationship relationship : allRelationships) 
								{
									Node destinationnode=relationship.getOtherNode(temp);
				       
									String destinationnodename =  destinationnode.getProperty("name").toString();
				       
									if (!visited.contains(destinationnodename)) {
										
									
				    	   
										visited.add(destinationnodename);
										queue.add(destinationnodename);
				    	   
									}
								}
							} //while queue
				
							//HashSet help = (HashSet)node2colors.get(curQueryTerm);
				
							Iterator iterator = visited.iterator(); 
			      
							// check values
							while (iterator.hasNext()){
					   
								String nodename = iterator.next().toString();
							    //System.out.println("Value: "+ nodename+ " ");  
					   
								if (node2colors.containsKey(nodename)) {
						   
									HashSet helpset =  (HashSet)node2colors.get(nodename);
						   
									helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
						   
									node2colors.put(nodename, helpset);
						   
								} else {
						   
									HashSet helpset = new HashSet();
									helpset.add(termcolors.get(query.indexOf(curQueryTerm)));

									node2colors.put(nodename, helpset);
						   
								}
							}
				
							//System.out.println("Value: "+ node2colors.get(curQueryTerm).toString()); 
							//System.out.println("Value: "+ node2colors.get("Grenzwert").toString());
						} //for all query terms
			
						Iterator iterator = node2colors.keySet().iterator(); 
		      
						// check values
						while (iterator.hasNext()){
				   
							String nodename = iterator.next().toString();
				   
							if ((((HashSet)node2colors.get(nodename)).size()==termcolors.size()) && (helpquery.contains(nodename)) ) {
								centroidcandidates.add(nodename);
							}
						}
						System.out.println("Centroid candidates: " +centroidcandidates.size() + "  " + centroidcandidates.toString());
			
					} // centroidcandidates.size()<5
				
					long stop = System.currentTimeMillis();
					timeelapsed = ((double)(stop-start)/(double)1000);
						
					System.out.println("Centroid determination took "+timeelapsed+" seconds.");
				
				
					double averagepathlength = 0;
				
					for (int i=0; i<centroidcandidates.size(); i++) {
					
						String candidate = centroidcandidates.get(i).toString();
						averagepathlength=0;
					
						Node n1 = graphDB.findNode(Labels.SINGLE_NODE,"name", candidate);
						
						for (int j=0; j<query.size(); j++) {
						
							String curQueryTerm = query.get(j).toString();
						
							Node n2 = graphDB.findNode(Labels.SINGLE_NODE,"name", curQueryTerm);
						
							if ((n1!=null) && (n2!=null)) {
							
								PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra( PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), "cost", 1 );
							   
								WeightedPath p = finder.findSinglePath(n1, n2);
							
								if (p!=null) {
								
									averagepathlength+=p.weight();
								
								}
							}
						}
					
						averagepathlength = averagepathlength / query.size();
					
						centroidcandidatesdata.put(candidate, averagepathlength);
					
						if (averagepathlength<shortestaveragepathlength){
						
							shortestaveragepathlength = averagepathlength;
							centroid = candidate;
						}
					} //for centroidcandidates
				} // if querysize==
			   
				
				
				System.out.println("node2colors: " + node2colors.size() + "   " +centroid +  "   " + shortestaveragepathlength);
				
				//result.put("centroid", centroid);
				//result.put("shortestaveragepathlength", shortestaveragepathlength);
				//result.put("activatednodes", node2colors.size());
				//result.put("timeelapsed", timeelapsed);
				//result.put("centroidcandidatesdata", centroidcandidatesdata);
				
				Node find_cluster = graphDB.findNode(Labels.SINGLE_NODE,"name", centroid);
				
				if (find_cluster!=null) {
					cluster_id = (int)find_cluster.getProperty("clusterID");
				}
				
				
			}
			tx1.success();
		}
		//return result;
			

		return cluster_id;
	}
	*/
	
	public void getCentroidbySpreadingActivation(int c_id)
    {
		Vector tempDelRelations_dn1 = new Vector();
		Vector tempDelRelations_dn2 = new Vector();
		Vector tempDelRelations_dcount = new Vector();
		Vector tempDelRelations_ddice = new Vector();
		Vector tempDelRelations_dcost = new Vector();
		
		Vector query = new Vector();
		//LOGGER.info("->getCentroidbySpreadingActivation() started...");
        //HashMap result = new HashMap();
        
        
        //int originalquerysize = query.size();
        String centroid = "";
        double shortestaveragepathlength = Double.MAX_VALUE;
        double timeelapsed = 0.0;    
        
        HashMap centroidcandidatesdata = new HashMap();
        
        
        Vector termcolors = new Vector();
        HashMap node2colors = new HashMap();
        HashMap<String, Double> nodedistances = new HashMap<String, Double>();
        
        double arearadius = 10.0;
        
        Vector centroidcandidates = new Vector();
        
        
        try (Transaction tx1 = graphDB.beginTx()){
			ResourceIterator<Node> nodelist = graphDB.findNodes( Labels.SINGLE_NODE );
			Vector helpquery = new Vector();
			while( nodelist.hasNext() )
			{
				Node wordnode = nodelist.next();
				if(wordnode.getProperty("clusterID").equals(c_id))	
				{
					wordnode.setProperty("centroidL0", 0);
					
					helpquery.add(wordnode.getProperty("name").toString());
					
					
					
					//cut relation not this cluster c_id
					Iterable<Relationship> allRelationships = wordnode.getRelationships();
				    for (Relationship relationship : allRelationships) {
				    	
				    	Node n2=relationship.getOtherNode(wordnode);
				    	
				    	if(n2.equals(relationship.getOtherNode(wordnode))){
				    		
				    		int n2_cid = (int)n2.getProperty("clusterID");
				    		if(n2_cid != c_id) {
				    			//System.out.println("!=");
						    	
				    			tempDelRelations_dn1.add(wordnode.getProperty("name").toString());
				    			tempDelRelations_dn2.add(n2.getProperty("name").toString());
				    			tempDelRelations_dcount.add(relationship.getProperty("count"));
				    			tempDelRelations_ddice.add(relationship.getProperty("dice"));
				    			tempDelRelations_dcost.add(relationship.getProperty("cost"));
				    		}
	
				    	}
					        
				    }//end for relationship
				    
				   
				    
	
				}//if(wordnode.getProperty("clusterID").equals(c_id))				
			}//end while nodelist
			
			if(!tempDelRelations_dn1.isEmpty()) {
				for(int d=0;d<tempDelRelations_dn1.size();d++) {
					String dn1 = tempDelRelations_dn1.get(d).toString();
					String dn2 = tempDelRelations_dn2.get(d).toString();
					int dcount = (int)tempDelRelations_dcount.get(d);
					double ddice = (double)tempDelRelations_ddice.get(d);
					double dcost = (double)tempDelRelations_dcost.get(d);
					//System.out.println("tempDelRelations:: dn1="+dn1+" dn2="+dn2+" dcount="+dcount+" ddice="+ddice+" dcost="+dcost);
				
					Node ndn1 = graphDB.findNode(Labels.SINGLE_NODE,"name", dn1);
					Node ndn2 = graphDB.findNode(Labels.SINGLE_NODE,"name", dn2);
					
					Iterable<Relationship> allRelationships = ndn1.getRelationships();
				    for (Relationship relationship : allRelationships) {
				    	
				    	Node n2=relationship.getOtherNode(ndn1);
				    	
				    	if(n2.equals(relationship.getOtherNode(ndn1))){
				    		if(n2.equals(ndn2)) {
				    			relationship.delete();
				    		}
				    	}
				    }
				}  
			}

			if(helpquery.size()>1) {
				
				query = helpquery;
				//System.out.println("Helpquery size: " +query.size() + "  "+query);
				System.out.println("Helpquery size: " +query.size());
				
				//LOGGER.info("Helpquery size: " +query.size() + "  "+query);;
                    
                    
				//Check if all query terms can be reached in the graph database from one another 
				//(remove one that cannot be reached by one or all of the other terms)
				//
                    
				//int helpquerysize = query.size();
                    
				HashSet helpqueryset = new HashSet();
				HashSet helpqueryset2bremoved = new HashSet();
                
                Vector helpquery2 = new Vector();
                
                for (int i=0; i<query.size(); i++) {
                    
                    helpqueryset.add(query.get(i).toString());
                    
                }    
                    
                    
                   
                HashMap numberofreachednodes = new HashMap();
                Iterator iteratorq1 = helpqueryset.iterator(); 

                while (iteratorq1.hasNext()){
                               
                	String queryterm = iteratorq1.next().toString();
                	Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm.toLowerCase());
                	//if (temp!=null)
                	//LOGGER.info("Temp1 node: " +temp.getProperty("name"));;
                           
                	Iterator iteratorq2 = helpqueryset.iterator(); 
                	while (iteratorq2.hasNext()){
                		String queryterm2 = iteratorq2.next().toString(); 
                                   
                		if (!queryterm.equals(queryterm2)) {
                			Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm2.toLowerCase());
                			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), 100, 1 );

                			Path p = finder.findSinglePath( temp, temp2 );
                                                                                    
                			if (p!=null) {
                				// LOGGER.info("Path found");;
                				if (numberofreachednodes.containsKey(queryterm)) {
                					HashSet helpset = (HashSet)numberofreachednodes.get(queryterm);
                					helpset.add(queryterm2);
                					numberofreachednodes.put(queryterm, helpset);
                				} else {
                					HashSet helpset= new HashSet();
                					helpset.add(queryterm2);
                					numberofreachednodes.put(queryterm, helpset);
                				}
                			}
                		}
                	}
                } 
                //LOGGER.info("Number of reached nodes: " +numberofreachednodes.toString());;
                
                //System.out.println("Number of reached nodes: " +numberofreachednodes.toString());
                
                           
                String mostreachableterm = "";
                int numberofneighbours = 0;
                       
                Iterator iteratorq3 = numberofreachednodes.keySet().iterator();
                while (iteratorq3.hasNext()){
                	String queryterm = iteratorq3.next().toString(); 
                           
                	HashSet helphashset = (HashSet) numberofreachednodes.get(queryterm);
                           
                	if (helphashset.size()>numberofneighbours) {
                               
                		numberofneighbours = helphashset.size();
                		mostreachableterm = queryterm;
                           
                	}
                       
                }  
                
                HashSet helphashset = new HashSet();
                
                if (!mostreachableterm.equals("")) {
                       
                	helphashset.addAll((HashSet) numberofreachednodes.get(mostreachableterm));
                           
                	helphashset.add(mostreachableterm);
                       
                }        

                helpqueryset = helphashset;
                
                for (int i=0; i<query.size(); i++) {
                    
                    String helpterm = query.get(i).toString();
                    
                    if (helpqueryset.contains(helpterm)) {
                        
                        helpquery2.add(helpterm);
                        
                    }
                    
                }           
                           
                query = helpquery2;
                //LOGGER.info("Helpquery2 size: " +query.size() + "  "+query);;
                //System.out.println("Helpquery2 size: " +query.size() + "  "+query);
                System.out.println("Helpquery2 size: " +query.size());
            
                //hierher2
                if (query.size()>1) {
                    
                    
                    double largestdistanceofqueryterms = 0;
                    double largestpathlength = 0;
                    
                    for (int i=0; i<query.size(); i++) {
                        
                        for (int j=i+1; j<query.size(); j++) {
                            
                            Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", query.get(i).toString().toLowerCase());
                            Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", query.get(j).toString().toLowerCase());
                            
                            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra( PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), "cost", 1 );

                            WeightedPath p = finder.findSinglePath( temp, temp2 );
                        
                            if (p!=null)
                            	if (p.weight()>largestdistanceofqueryterms) {
                            		largestpathlength = p.length();
                            		largestdistanceofqueryterms=p.weight();
                                        
                            		//LOGGER.info("Path found: " + largestpathlength + "  " + largestdistanceofqueryterms);
                            	}
                        }
                    }    
                    
                    //arearadius = Math.ceil(largestdistanceofqueryterms / 2.0)+Math.ceil((20*largestdistanceofqueryterms) / 100.0); //Math.ceil(largestdistanceofqueryterms / 2.0)+1;
                    
                    arearadius = Math.ceil((largestdistanceofqueryterms / 2.0) +  ((20*largestdistanceofqueryterms) / 100.0))  ; //Math.ceil(largestdistanceofqueryterms / 2.0)+1;
                                      
                    //LOGGER.info("largestdistanceofqueryterms: " +largestdistanceofqueryterms + "  " +largestpathlength + "  arearadius: "+arearadius);
                    
                    System.out.println("largestdistanceofqueryterms: " +largestdistanceofqueryterms + "  " +largestpathlength + "  arearadius: "+arearadius);
                    
                    
                    double count=0,maxcount=10; 
                    
                    long start = System.currentTimeMillis();    
                    
                    while ((centroidcandidates.size()<=query.size()) && (count<maxcount)   ) { //10   //neu count

                    	count++;
                    	//LOGGER.info("Activation rounds to execute: " +count );
                    	System.out.println("Activation rounds to execute: " +count );
                    
                    	//if(count>2)
                    	//	arearadius = arearadius + (arearadius / 2.0);//arearadius + 1; //(arearadius / 2.0); 
                    	
                    	if (count>2 && count<10) 
                    		arearadius = arearadius + (arearadius / 2.0);//arearadius + 1; //(arearadius / 2.0); 
                    	else
                    		arearadius = arearadius + (arearadius / 2.0) + ((20 * arearadius) / 100.0);
                    	
                    	
                    	termcolors = new Vector();
                    	node2colors = new HashMap();    
                    	centroidcandidates = new Vector();
                    	nodedistances = new HashMap<String, Double>();
                    
                    	int color = 0;
                    
                    	for(Iterator i=query.iterator(); i.hasNext(); ){

                    		color++;
                    		termcolors.add(color);
                                            
                    		String curQueryTerm = i.next().toString();
                    		centroidcandidates.add(curQueryTerm);
                        
                        
                    		HashSet helpset = new HashSet();
                    		helpset.add(color);
                        
                    		node2colors.put(curQueryTerm, helpset);
                    		//LOGGER.info("Query Term: " + curQueryTerm + " Color: " + color);
                    		nodedistances.put(curQueryTerm, 0.0);
                    	}
	                    
                    	for(Iterator i=query.iterator(); i.hasNext(); ) {
    	                    
                    		String curQueryTerm = i.next().toString();
                    
                    		HashSet visited = new HashSet();
                    		HashMap<String, Double> visiteddistance = new HashMap(); 
                    
                    		LinkedList<String> queue = new LinkedList<String>();
                    
                    		visited.add(curQueryTerm);
                    		queue.add(curQueryTerm);
                    		visiteddistance.put(curQueryTerm, 0.0);
                    
                    		Node firstsourcenode = graphDB.findNode(Labels.SINGLE_NODE,"name", curQueryTerm.toLowerCase());
                
                    		int steps=0;
                    
                    		while ((queue.size()!=0) && (steps<count)) {
                        
                    			steps++;
                        
                    			String sourcenodename = queue.poll();
                    			//    LOGGER.info("Activating: "+ sourcenodename);
                                                
                    			Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", sourcenodename.toLowerCase());
                    			Iterable<Relationship> allRelationships = temp.getRelationships();
                        
                    			for (Relationship relationship : allRelationships){
                           
                    				Node destinationnode=relationship.getOtherNode(temp);
                           
                    				String destinationnodename =  destinationnode.getProperty("name").toString();
                           
                    				if (!visited.contains(destinationnodename)) {
                    					double hv1 = visiteddistance.get(sourcenodename).doubleValue();
                    					double hv2 = Double.MAX_VALUE;
                               
                               
                    					//  LOGGER.info("hv1: " + sourcenodename + " " + destinationnodename + " " + hv1);
                    					//   LOGGER.info("hv2: " + temp.getProperty("occur") + "  " +destinationnode.getProperty("occur") +   " " + relationship.getProperty("count") + " " + relationship.getProperty("cost"));
                    					//  double hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					if (relationship.getProperty("cost") instanceof Integer) {
                    						//       LOGGER.info("Object is Integer");
                    						hv2 = ((Integer)relationship.getProperty("cost")).doubleValue();
                    					}
                               
                    					if (relationship.getProperty("cost") instanceof Float) {
                    						//       LOGGER.info("Object is Float");
                    						hv2 = ((Float)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                                   
                    					if (relationship.getProperty("cost") instanceof Double) {
                    						//   LOGGER.info("Object is Double");
                    						hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                    					
                    					if (visiteddistance.containsKey(sourcenodename)   &&  (hv1+hv2<=arearadius)    ) {
         	                               
                    						visited.add(destinationnodename);
                    						queue.add(destinationnodename);
                               
                    						visiteddistance.put(destinationnodename, hv1 + hv2);
                               
                    					}
                    					
                    				} else {   //testen ob kleinere distanz zu einem bereits visited knoten gefunden wurde
     	                               
                    					double hv1 = visiteddistance.get(sourcenodename).doubleValue();
                    					double hv2 = Double.MAX_VALUE;			
	                               
                    					//  LOGGER.info("hv1: " + sourcenodename + " " + destinationnodename + " " + hv1);
                    					//  LOGGER.info("hv2: " + temp.getProperty("occur") + "  " +destinationnode.getProperty("occur") +   " " + relationship.getProperty("count") + " " + relationship.getProperty("cost"));
                    					// double hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					if (relationship.getProperty("cost") instanceof Integer) {
                    						//       LOGGER.info("Object is Integer");
                    						hv2 = ((Integer)relationship.getProperty("cost")).doubleValue();
                    					}
                               
                    					if (relationship.getProperty("cost") instanceof Float) {
                    						//       LOGGER.info("Object is Float");
                    						hv2 = ((Float)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                                   
                    					if (relationship.getProperty("cost") instanceof Double) {
                    						//   LOGGER.info("Object is Double");
                    						hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                               
                    					if (visiteddistance.containsKey(sourcenodename)   &&  (hv1+hv2<=arearadius)    ) {
                                   
                    						visiteddistance.put(destinationnodename, hv1+hv2);
                                   
                    					}                                   
                    				}
                    			} //allrelationships
                    		} //while queue	
                    		
                    		//HashSet help = (HashSet)node2colors.get(curQueryTerm);
    	                    
                    		Iterator iterator = visited.iterator(); 
                      
                    		// check values
                    		while (iterator.hasNext()){
                           
                    			String nodename = iterator.next().toString();
                    			// LOGGER.info("Value: "+ nodename+ " ");  
                           
                    			if (node2colors.containsKey(nodename)) {
                               
                    				HashSet helpset =  (HashSet)node2colors.get(nodename);
                               
                    				helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
                               
                    				node2colors.put(nodename, helpset);
                               
                               
                               
                    				double curdistance = 0.0;
                               
                    				//  if (nodedistances.containsKey(nodename))
                    				curdistance =  ((Double)nodedistances.get(nodename)).doubleValue();
                               
                    				curdistance = curdistance + visiteddistance.get(nodename).doubleValue();
                       
                    				nodedistances.put(nodename, curdistance);
                    			} else {
                               
                    				HashSet helpset = new HashSet();
                    				helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
    
                    				node2colors.put(nodename, helpset);
                    				nodedistances.put(nodename, visiteddistance.get(nodename).doubleValue());
                               
                    			}
                    		}
                    
                    		// LOGGER.info("Value: "+ node2colors.get(curQueryTerm).toString()); 
                    		// LOGGER.info("Value: "+ node2colors.get("Grenzwert").toString());
                    	} //for all query terms				
	                               
                    	Iterator iterator = node2colors.keySet().iterator(); 
  	                  
                    	// check values
                    	while (iterator.hasNext()){
                       
                    		String nodename = iterator.next().toString();
                       
                    		if ((((HashSet)node2colors.get(nodename)).size()==termcolors.size())) {
                    			if (!centroidcandidates.contains(nodename) )
                    				centroidcandidates.add(nodename);
                           
                    		} else {
                    			if (nodedistances.containsKey(nodename)) {
                    				nodedistances.remove(nodename);
                    			}
                    		}
                    	}
                    	//System.out.println("Centroid candidates: " +centroidcandidates.size() + "  " + centroidcandidates.toString());
                    	System.out.println("Centroid candidates: " +centroidcandidates.size());
                    	//LOGGER.info("Centroid candidates: " +centroidcandidates.size() + "  " + centroidcandidates.toString());
                
                    
                    	//for case no data inside of nodedistances is null that effect to no centroid
                    	if(count==maxcount) {
                    		if(nodedistances.isEmpty()) {
                    			maxcount++;
                    			//System.out.println("maxcount = "+maxcount);
                    		}
                    	}
                    
                    } // centroidcandidates.size()<10				
	                               
                    long stop = System.currentTimeMillis();
                    timeelapsed = ((double)(stop-start)/(double)1000);
                            
                    //LOGGER.info("Centroid determination took "+timeelapsed+" seconds.");
                    
                    double averagepathlength = 0;
                    
                    double maxdistance = Double.MAX_VALUE;
                    
                    //System.out.println("centroidcandidates are::"+centroidcandidates.toString());
                    System.out.println("nodedistances are::"+nodedistances.toString());
                    
                    for (int i=0; i<centroidcandidates.size(); i++) {
                        
                        String candidate = centroidcandidates.get(i).toString();
                        
                        if (nodedistances.containsKey(candidate)) {
                        	if (((Double)nodedistances.get(candidate)).doubleValue()<maxdistance) {
                                   
                        		maxdistance = ((Double)nodedistances.get(candidate)).doubleValue();
                        		shortestaveragepathlength = maxdistance / query.size();
                        		centroid = candidate;
                        	}
                        }               
                    } //for centroidcandidates
                } // if querysize== 					
	                    
                System.out.println("node2colors: " + node2colors.size() + "   " +centroid +  "   " + shortestaveragepathlength);
				
				//result.put("centroid", centroid);
				//result.put("shortestaveragepathlength", shortestaveragepathlength);
				//result.put("activatednodes", node2colors.size());
				//result.put("timeelapsed", timeelapsed);
				//result.put("centroidcandidatesdata", centroidcandidatesdata);
				
				centroids[c_id]=centroid; 
				
				Node nc = graphDB.findNode(Labels.SINGLE_NODE,"name", centroid);
				nc.setProperty("centroidL0", 1);
				
				
			}else if(helpquery.size()==1){
				centroids[c_id]=helpquery.get(0).toString();
				Node nc = graphDB.findNode(Labels.SINGLE_NODE,"name", helpquery.get(0).toString());
				nc.setProperty("centroidL0", 1);				
			}
			
			
			
			if(!tempDelRelations_dn1.isEmpty()) {
				for(int d=0;d<tempDelRelations_dn1.size();d++) {
					String dn1 = tempDelRelations_dn1.get(d).toString();
					String dn2 = tempDelRelations_dn2.get(d).toString();
					int dcount = (int)tempDelRelations_dcount.get(d);
					double ddice = (double)tempDelRelations_ddice.get(d);
					double dcost = (double)tempDelRelations_dcost.get(d);
					//System.out.println("tempDelRelations:: dn1="+dn1+" dn2="+dn2+" dcount="+dcount+" ddice="+ddice+" dcost="+dcost);
				
					Node ndn1 = graphDB.findNode(Labels.SINGLE_NODE,"name", dn1);
					Node ndn2 = graphDB.findNode(Labels.SINGLE_NODE,"name", dn2);
					Relationship rd = ndn1.createRelationshipTo(ndn2, RelationshipTypes.IS_CONNECTED );
					rd.setProperty("count", dcount );
					rd.setProperty("dice", ddice);  //for calculating Dice ration
					rd.setProperty("cost", dcost);   //for Dijkstra
				}  
			}
			
			
			
			tx1.success();
		}
		//return result;
	}

	public int getClusterIDwithCentroidbySpreadingActivation(Vector query_find)
    {
		System.out.println("Starting to get Centroid is,,,");
		
		//HashMap result = new HashMap();
		int cluster_id=0;

		Vector query = new Vector();
        
        
       // int originalquerysize = query.size();
        String centroid = "";
        double shortestaveragepathlength = Double.MAX_VALUE;
        double timeelapsed = 0.0;    
        
        HashMap centroidcandidatesdata = new HashMap();
        
        
        Vector termcolors = new Vector();
        HashMap node2colors = new HashMap();
        HashMap<String, Double> nodedistances = new HashMap<String, Double>();
        
        double arearadius = 10.0;
        
        Vector centroidcandidates = new Vector();
        
        
        try (Transaction tx1 = graphDB.beginTx()){
        	
        	Vector helpquery = new Vector();
			
			
			for (int i=0; i<query_find.size(); i++) {
				Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", query_find.get(i).toString());
				
				if (temp!=null) {
					helpquery.add(query_find.get(i).toString());
				}
				
			}
			
			if(helpquery.size()>1) {
				
				query = helpquery;
				//System.out.println("Helpquery size: " +query.size() + "  "+query);
				System.out.println("Helpquery size: " +query.size());
				//LOGGER.info("Helpquery size: " +query.size() + "  "+query);;
                    
                    
				//Check if all query terms can be reached in the graph database from one another 
				//(remove one that cannot be reached by one or all of the other terms)
				//
                    
				int helpquerysize = query.size();
                    
				HashSet helpqueryset = new HashSet();
				HashSet helpqueryset2bremoved = new HashSet();
                
                Vector helpquery2 = new Vector();
                
                for (int i=0; i<query.size(); i++) {
                    
                    helpqueryset.add(query.get(i).toString());
                    
                }    
                    
                    
                   
                HashMap numberofreachednodes = new HashMap();
                Iterator iteratorq1 = helpqueryset.iterator(); 

                while (iteratorq1.hasNext()){
                               
                	String queryterm = iteratorq1.next().toString();
                	Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm.toLowerCase());
                	//if (temp!=null)
                	//LOGGER.info("Temp1 node: " +temp.getProperty("name"));;
                           
                	Iterator iteratorq2 = helpqueryset.iterator(); 
                	while (iteratorq2.hasNext()){
                		String queryterm2 = iteratorq2.next().toString(); 
                                   
                		if (!queryterm.equals(queryterm2)) {
                			Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", queryterm2.toLowerCase());
                			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), 100, 1 );

                			Path p = finder.findSinglePath( temp, temp2 );
                                                                                    
                			if (p!=null) {
                				// LOGGER.info("Path found");;
                				if (numberofreachednodes.containsKey(queryterm)) {
                					HashSet helpset = (HashSet)numberofreachednodes.get(queryterm);
                					helpset.add(queryterm2);
                					numberofreachednodes.put(queryterm, helpset);
                				} else {
                					HashSet helpset= new HashSet();
                					helpset.add(queryterm2);
                					numberofreachednodes.put(queryterm, helpset);
                				}
                			}
                		}
                	}
                } 
                //LOGGER.info("Number of reached nodes: " +numberofreachednodes.toString());;
                           
                String mostreachableterm = "";
                int numberofneighbours = 0;
                       
                Iterator iteratorq3 = numberofreachednodes.keySet().iterator();
                while (iteratorq3.hasNext()){
                	String queryterm = iteratorq3.next().toString(); 
                           
                	HashSet helphashset = (HashSet) numberofreachednodes.get(queryterm);
                           
                	if (helphashset.size()>numberofneighbours) {
                               
                		numberofneighbours = helphashset.size();
                		mostreachableterm = queryterm;
                           
                	}
                       
                }  
                
                HashSet helphashset = new HashSet();
                
                if (!mostreachableterm.equals("")) {
                       
                	helphashset.addAll((HashSet) numberofreachednodes.get(mostreachableterm));
                           
                	helphashset.add(mostreachableterm);
                       
                }        

                helpqueryset = helphashset;
                
                for (int i=0; i<query.size(); i++) {
                    
                    String helpterm = query.get(i).toString();
                    
                    if (helpqueryset.contains(helpterm)) {
                        
                        helpquery2.add(helpterm);
                        
                    }
                    
                }           
                           
                query = helpquery2;
                //LOGGER.info("Helpquery2 size: " +query.size() + "  "+query);;
                //System.out.println("Helpquery2 size: " +query.size() + "  "+query);
            
                //hierher2
                if (query.size()>1) {
                    
                    
                    double largestdistanceofqueryterms = 0;
                    double largestpathlength = 0;
                    
                    for (int i=0; i<query.size(); i++) {
                        
                        for (int j=i+1; j<query.size(); j++) {
                            
                            Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", query.get(i).toString().toLowerCase());
                            Node temp2 = graphDB.findNode(Labels.SINGLE_NODE,"name", query.get(j).toString().toLowerCase());
                            
                            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra( PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), "cost", 1 );

                            WeightedPath p = finder.findSinglePath( temp, temp2 );
                        
                            if (p!=null)
                            	if (p.weight()>largestdistanceofqueryterms) {
                            		largestpathlength = p.length();
                            		largestdistanceofqueryterms=p.weight();
                                        
                            		//LOGGER.info("Path found: " + largestpathlength + "  " + largestdistanceofqueryterms);
                            	}
                        }
                    }    
                    
                    //arearadius = Math.ceil(largestdistanceofqueryterms / 2.0)+Math.ceil((20*largestdistanceofqueryterms) / 100.0); //Math.ceil(largestdistanceofqueryterms / 2.0)+1;
                    
                    arearadius = Math.ceil((largestdistanceofqueryterms / 2.0) +  ((20*largestdistanceofqueryterms) / 100.0))  ; //Math.ceil(largestdistanceofqueryterms / 2.0)+1;
                                      
                    //LOGGER.info("largestdistanceofqueryterms: " +largestdistanceofqueryterms + "  " +largestpathlength + "  arearadius: "+arearadius);
                    
                    double count=0,maxcount=10; 
                    
                    long start = System.currentTimeMillis();    
                    
                    while ((centroidcandidates.size()<=query.size()) && (count<maxcount)   ) { //10   //neu count             

                    	count++;
                    	//LOGGER.info("Activation rounds to execute: " +count );
                    
                    	//if(count>2)
                    	//	arearadius = arearadius + (arearadius / 2.0);//arearadius + 1; //(arearadius / 2.0); 
                    	
                    	if (count>2 && count<10) 
                    		arearadius = arearadius + (arearadius / 2.0);//arearadius + 1; //(arearadius / 2.0); 
                    	else
                    		arearadius = arearadius + (arearadius / 2.0) + ((20 * arearadius) / 100.0);
                    	
                    	
                    	termcolors = new Vector();
                    	node2colors = new HashMap();    
                    	centroidcandidates = new Vector();
                    	nodedistances = new HashMap<String, Double>();
                    
                    	int color = 0;
                    
                    	for(Iterator i=query.iterator(); i.hasNext(); ){

                    		color++;
                    		termcolors.add(color);
                                            
                    		String curQueryTerm = i.next().toString();
                    		centroidcandidates.add(curQueryTerm);
                        
                        
                    		HashSet helpset = new HashSet();
                    		helpset.add(color);
                        
                    		node2colors.put(curQueryTerm, helpset);
                    		//LOGGER.info("Query Term: " + curQueryTerm + " Color: " + color);
                    		nodedistances.put(curQueryTerm, 0.0);
                    	}
	                    
                    	for(Iterator i=query.iterator(); i.hasNext(); ) {
    	                    
                    		String curQueryTerm = i.next().toString();
                    
                    		HashSet visited = new HashSet();
                    		HashMap<String, Double> visiteddistance = new HashMap(); 
                    
                    		LinkedList<String> queue = new LinkedList<String>();
                    
                    		visited.add(curQueryTerm);
                    		queue.add(curQueryTerm);
                    		visiteddistance.put(curQueryTerm, 0.0);
                    
                    		Node firstsourcenode = graphDB.findNode(Labels.SINGLE_NODE,"name", curQueryTerm.toLowerCase());
                
                    		int steps=0;
                    
                    		while ((queue.size()!=0) && (steps<count)) {
                        
                    			steps++;
                        
                    			String sourcenodename = queue.poll();
                    			//    LOGGER.info("Activating: "+ sourcenodename);
                                                
                    			Node temp = graphDB.findNode(Labels.SINGLE_NODE,"name", sourcenodename.toLowerCase());
                    			Iterable<Relationship> allRelationships = temp.getRelationships();
                        
                    			for (Relationship relationship : allRelationships){
                           
                    				Node destinationnode=relationship.getOtherNode(temp);
                           
                    				String destinationnodename =  destinationnode.getProperty("name").toString();
                           
                    				if (!visited.contains(destinationnodename)) {
                    					double hv1 = visiteddistance.get(sourcenodename).doubleValue();
                    					double hv2 = Double.MAX_VALUE;
                               
                               
                    					//  LOGGER.info("hv1: " + sourcenodename + " " + destinationnodename + " " + hv1);
                    					//   LOGGER.info("hv2: " + temp.getProperty("occur") + "  " +destinationnode.getProperty("occur") +   " " + relationship.getProperty("count") + " " + relationship.getProperty("cost"));
                    					//  double hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					if (relationship.getProperty("cost") instanceof Integer) {
                    						//       LOGGER.info("Object is Integer");
                    						hv2 = ((Integer)relationship.getProperty("cost")).doubleValue();
                    					}
                               
                    					if (relationship.getProperty("cost") instanceof Float) {
                    						//       LOGGER.info("Object is Float");
                    						hv2 = ((Float)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                                   
                    					if (relationship.getProperty("cost") instanceof Double) {
                    						//   LOGGER.info("Object is Double");
                    						hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                    					
                    					if (visiteddistance.containsKey(sourcenodename)   &&  (hv1+hv2<=arearadius)    ) {
         	                               
                    						visited.add(destinationnodename);
                    						queue.add(destinationnodename);
                               
                    						visiteddistance.put(destinationnodename, hv1 + hv2);
                               
                    					}
                    					
                    				} else {   //testen ob kleinere distanz zu einem bereits visited knoten gefunden wurde
     	                               
                    					double hv1 = visiteddistance.get(sourcenodename).doubleValue();
                    					double hv2 = Double.MAX_VALUE;			
	                               
                    					//  LOGGER.info("hv1: " + sourcenodename + " " + destinationnodename + " " + hv1);
                    					//  LOGGER.info("hv2: " + temp.getProperty("occur") + "  " +destinationnode.getProperty("occur") +   " " + relationship.getProperty("count") + " " + relationship.getProperty("cost"));
                    					// double hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					if (relationship.getProperty("cost") instanceof Integer) {
                    						//       LOGGER.info("Object is Integer");
                    						hv2 = ((Integer)relationship.getProperty("cost")).doubleValue();
                    					}
                               
                    					if (relationship.getProperty("cost") instanceof Float) {
                    						//       LOGGER.info("Object is Float");
                    						hv2 = ((Float)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                                   
                    					if (relationship.getProperty("cost") instanceof Double) {
                    						//   LOGGER.info("Object is Double");
                    						hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                               
                    					if (visiteddistance.containsKey(sourcenodename)   &&  (hv1+hv2<=arearadius)    ) {
                                   
                    						visiteddistance.put(destinationnodename, hv1+hv2);
                                   
                    					}                                   
                    				}
                    			} //allrelationships
                    		} //while queue	
                    		
                    		//HashSet help = (HashSet)node2colors.get(curQueryTerm);
    	                    
                    		Iterator iterator = visited.iterator(); 
                      
                    		// check values
                    		while (iterator.hasNext()){
                           
                    			String nodename = iterator.next().toString();
                    			// LOGGER.info("Value: "+ nodename+ " ");  
                           
                    			if (node2colors.containsKey(nodename)) {
                               
                    				HashSet helpset =  (HashSet)node2colors.get(nodename);
                               
                    				helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
                               
                    				node2colors.put(nodename, helpset);
                               
                               
                               
                    				double curdistance = 0.0;
                               
                    				//  if (nodedistances.containsKey(nodename))
                    				curdistance =  ((Double)nodedistances.get(nodename)).doubleValue();
                               
                    				curdistance = curdistance + visiteddistance.get(nodename).doubleValue();
                       
                    				nodedistances.put(nodename, curdistance);
                    			} else {
                               
                    				HashSet helpset = new HashSet();
                    				helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
    
                    				node2colors.put(nodename, helpset);
                    				nodedistances.put(nodename, visiteddistance.get(nodename).doubleValue());
                               
                    			}
                    		}
                    
                    		// LOGGER.info("Value: "+ node2colors.get(curQueryTerm).toString()); 
                    		// LOGGER.info("Value: "+ node2colors.get("Grenzwert").toString());
                    	} //for all query terms				
	                               
                    	Iterator iterator = node2colors.keySet().iterator(); 
  	                  
                    	// check values
                    	while (iterator.hasNext()){
                       
                    		String nodename = iterator.next().toString();
                       
                    		if ((((HashSet)node2colors.get(nodename)).size()==termcolors.size()) ) {
                    			if (!centroidcandidates.contains(nodename)    )
                    				centroidcandidates.add(nodename);
                           
                    		} else {
                    			if (nodedistances.containsKey(nodename)) {
                    				nodedistances.remove(nodename);
                    			}
                    		}
                    	}
                
                    	//LOGGER.info("Centroid candidates: " +centroidcandidates.size() + "  " + centroidcandidates.toString());
                
                    	//for case no data inside of nodedistances is null that effect to no centroid
                    	if(count==maxcount) {
                    		if(nodedistances.isEmpty()) {
                    			maxcount++;
                    			//System.out.println("maxcount = "+maxcount);
                    		}
                    	}	
                    	
                    } // centroidcandidates.size()<10				
	                               
                    long stop = System.currentTimeMillis();
                    timeelapsed = ((double)(stop-start)/(double)1000);
                            
                    //LOGGER.info("Centroid determination took "+timeelapsed+" seconds.");
                    
                    double averagepathlength = 0;
                    
                    double maxdistance = Double.MAX_VALUE;
                    
                    //System.out.println("centroidcandidates are::"+centroidcandidates.toString());
                    //System.out.println("nodedistances are::"+nodedistances.toString());
                    
                    for (int i=0; i<centroidcandidates.size(); i++) {
                        
                        String candidate = centroidcandidates.get(i).toString();
                        
                        if (nodedistances.containsKey(candidate)) {
                        	if (((Double)nodedistances.get(candidate)).doubleValue()<maxdistance) {
                                   
                        		maxdistance = ((Double)nodedistances.get(candidate)).doubleValue();
                        		shortestaveragepathlength = maxdistance / query.size();
                        		centroid = candidate;
                        	}
                        }               
                    } //for centroidcandidates
                } // if querysize== 					
	                    
                System.out.println("FindClusterID:: node2colors: " + node2colors.size() + "   " +centroid +  "   " + shortestaveragepathlength);
				
				//result.put("centroid", centroid);
				//result.put("shortestaveragepathlength", shortestaveragepathlength);
				//result.put("activatednodes", node2colors.size());
				//result.put("timeelapsed", timeelapsed);
				//result.put("centroidcandidatesdata", centroidcandidatesdata);
				
                Node find_cluster = graphDB.findNode(Labels.SINGLE_NODE,"name", centroid);
				
				if (find_cluster!=null) {
					cluster_id = (int)find_cluster.getProperty("clusterID");
					//System.out.println("cluster id : " + cluster_id);
					
				} 
				
			}
			tx1.success();
		}
		return cluster_id;
	}
	
	//do before add new documents (if this is not the first time to insert document)
	public void copyWordFromDBtoArrayCluster() {
		System.out.println("\n\n---Starting to copy All node on DB to array clusters---");
		//Vector result = new Vector();
		//Vector w[] = new Vector[max_cluster+1];
		
		//for(int p=1;p<=max_cluster;p++) {
		//	w[p] = new Vector();
		//}
		
		//String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		//File database = new File(db_path);
		
		//GraphDatabaseService graphDB2 = new GraphDatabaseFactory().newEmbeddedDatabase(database);
		//System.out.println("database opened for querying");
		
		int max_cid = 1;
		Vector<Integer> c_update = new Vector<Integer>();
		
		try (Transaction tx2 = graphDB.beginTx()) 
	    {  	
			ResourceIterator<Node> terms = graphDB.findNodes( Labels.SINGLE_NODE );
			while(terms.hasNext())
			{
				Node term = terms.next();
				String node1=(String)term.getProperty("name");
				int cid = (int)term.getProperty("clusterID");
				
				if (!distilledText[cid].contains(node1)) {
					
					distilledText[cid].add(node1);
					
					if(cid>max_cid) {
						max_cid = cid;
					}
	      		}
				
				if (!c_update.contains(cid)) {
      				c_update.add(cid);
      			}
				
				
				
				
				//w[cid].add("["+node1+"]");
				
			}
			tx2.success();
	    }
		//int id=1;
		//for(int p=1;p<=max_cluster;p++) {
		//	if(w[p].size()>0) {
		//		result.add("Cluster no."+id+"::"+w[p].toString()+"\n\n");
		//		id++;
		//	}
		//}
		
		//graphDB2.shutdown();
		
		count_cluster = max_cid;
		
		
		//update cluster 
		for(int u=0;u<c_update.size();u++) {
			if (distilledText[c_update.get(u)].size()>0) {
				updateClusterAndPositionCentroid(c_update.get(u));
			}
				
		}
		
		
		System.out.println("\n\n---All node on DB---");
		
		for(int p=1;p<=count_cluster;p++) {
			if(distilledText[p].size()>0) {
				System.out.println("Cluster no."+p+" \n-->>Distilled Text(All word read only noun): " + distilledText[p].toString());
				System.out.println("-->>Centroid: " + centroids[p]);
				System.out.println("-->>Average Distance: " + averageDistance[p]);
				System.out.println("-->>Standard Deviation: " + standardDeviation[p]);
				System.out.println("-->>average + "+numMultiplySigma+"*SD : " + (averageDistance[p]+(numMultiplySigma*standardDeviation[p])));
				//System.out.println("-->>numDistance: " + numDistance[p].toString());
				System.out.println("-------------------------------------------");
			}
		}
		//System.out.println(result.toString());
		//return result;
		
	}

	public void showWordInEachCluster() {

		Vector result = new Vector();
		Vector w[] = new Vector[max_cluster+1];
		Vector wcL0[] = new Vector[max_cluster+1];
		
		for(int p=1;p<=max_cluster;p++) {
			w[p] = new Vector();
			wcL0[p] = new Vector();
		}
		
		String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		File database = new File(db_path);
		
		GraphDatabaseService graphDB2 = new GraphDatabaseFactory().newEmbeddedDatabase(database);
		System.out.println("database opened for querying");
		
		try (Transaction tx2 = graphDB2.beginTx()) 
	    {  	
			ResourceIterator<Node> terms = graphDB2.findNodes( Labels.SINGLE_NODE );
			while(terms.hasNext())
			{
				Node term = terms.next();
				String node1=(String)term.getProperty("name");
				int cid = (int)term.getProperty("clusterID");
				int cclusL0 = (int)term.getProperty("centroidL0"); 
				
				w[cid].add("["+node1+"]");
				
				if(cclusL0==1) {
					wcL0[cid].add(node1);
				}
				
			}
			tx2.success();
	    }
		int id=1;
		for(int p=1;p<=max_cluster;p++) {
			if(w[p].size()>0) {
				result.add("## Cluster no."+id+" --"+wcL0[p]+"--##\n"+"("+w[p].size()+")"+w[p].toString()+"\n\n");
				id++;
			}
		}
		
		graphDB2.shutdown();
		
		System.out.println("\n\n---All node---");
		System.out.println(result.toString());
		//return result;
	
	}
	
	
	
	public void showDB() {
		Vector result = new Vector();
		
		String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		File database = new File(db_path);
		
		GraphDatabaseService graphDB2 = new GraphDatabaseFactory().newEmbeddedDatabase(database);
		System.out.println("database opened for querying");
		
		try (Transaction tx2 = graphDB2.beginTx()) 
	    {  	
			ResourceIterator<Node> terms = graphDB2.findNodes( Labels.SINGLE_NODE );
			while(terms.hasNext())
			{
				Node term = terms.next();
				String node1=(String)term.getProperty("name");
				result.add(term.getAllProperties()+"\n");
				
				result.add("\n");
				
				Node temp = graphDB2.findNode(Labels.SINGLE_NODE,"name", node1);
				Iterable<Relationship> allRelationships = temp.getRelationships();
			    for (Relationship relationship : allRelationships) {
			    	
			    	Node n2=relationship.getOtherNode(temp);
			    	
			    	if(n2.equals(relationship.getOtherNode(temp))){
			    		result.add(n2.getAllProperties());
				    	
				    	int countAB=(int)relationship.getProperty("count");
				    	double dice=(double)relationship.getProperty("dice");
				    	double cost=(double)relationship.getProperty("cost");
				    	
				    	result.add("count="+countAB+", dice="+dice+", cost="+cost+"\n");
			    	}
				        
			    }
			    result.add("\n\n");	
			}
			tx2.success();
	    }
		//System.out.println(lhm);
		
		graphDB2.shutdown();
		
		System.out.println("\n\n---All node---");
		System.out.println(result.toString());
		//return result;
	}

	public int getWordInEachCluster(int c_id) {
		int num = 0;
		try (Transaction txn = graphDB.beginTx()) 
	    {  	
			ResourceIterator<Node> terms = graphDB.findNodes( Labels.SINGLE_NODE );
			while(terms.hasNext())
			{
				Node term = terms.next();
				String node1=(String)term.getProperty("name");
				int id = (int)term.getProperty("clusterID");
				if(id == c_id) {
					num++;
				}
			}
			txn.success();
      	}
			
		return num;
	}
	
	public String printListEntry() {
		String msg="";
		/*if(check_start == true) {
			for(int p=1;p<=count_cluster;p++) {
				if(distilledText[p].size()>0) {
					msg+="Cluster no."+p+"\n";
					HashSet w = new HashSet();
					int c=0;
					for(int t=0;t<distilledText[p].size();t++) {
						
						if (!w.contains(distilledText[p].get(t).toString())) {
							w.add(distilledText[p].get(t).toString());
							msg+="["+distilledText[p].get(t).toString()+"]";
							c++;
							if(c%15==0)	msg+="\n";
						}
						
					}
					msg+="\n";
				}			
			}
		}*/
		if(check_start == true) {
			Vector dbText[] = new Vector[max_cluster+1];
			Vector dbCentroidL0[] = new Vector[max_cluster+1];
			//Vector dbOccur[] = new Vector[max_cluster+1];
			for(int i=1;i<=max_cluster;i++) {
				dbText[i]=new Vector();
				dbCentroidL0[i]=new Vector();
				//dbOccur[i]=new Vector();
			}
			
			try (Transaction tx2 = graphDB.beginTx()) 
		    {  	
				ResourceIterator<Node> terms = graphDB.findNodes( Labels.SINGLE_NODE );
				while(terms.hasNext())
				{
					Node term = terms.next();
					String node1=(String)term.getProperty("name");
					int c_id = (int)term.getProperty("clusterID");
					int centroid_l0 = (int)term.getProperty("centroidL0");
					//int c_occur = (int)term.getProperty("occur");
					dbText[c_id].add(node1);
					if(centroid_l0==1) {
						dbCentroidL0[c_id].add(node1);
					}
					//dbOccur[c_id].add(c_occur);
				}
				tx2.success();
	      	}
			int count_cluster_run=0;
			for(int p=1;p<=max_cluster;p++) {				
				if(dbText[p].size()>0) {
					count_cluster_run++;
					int c=0;
					msg+="\n### Cluster no."+p+"("+count_cluster_run+")--["+dbCentroidL0[p]+"]###\n";
					for(int t=0;t<dbText[p].size();t++) {
						//msg+="["+dbText[p].get(t).toString()+","+dbOccur[p].get(t).toString()+"]";
						msg+="["+dbText[p].get(t).toString()+"]";
						c++;
						if(c%12==0)	msg+="\n";
					}
					msg+="("+dbText[p].size()+")\n";
				}			
			}
		}
		
		return msg;
	}
	public String findCentroid(Vector query_find) {

		System.out.println("Starting to find Centroid is,,,");
		
		String db_path=System.getProperty("user.dir")+"/cooccsdatabase";
		File database = new File(db_path);
		
		GraphDatabaseService graphDB3 = new GraphDatabaseFactory().newEmbeddedDatabase(database);
		System.out.println("database opened for querying");
		
		//HashMap result = new HashMap();
		int cluster_id=0;

		Vector query = new Vector();
        
        
       // int originalquerysize = query.size();
        String centroid = "";
        double shortestaveragepathlength = Double.MAX_VALUE;
        double timeelapsed = 0.0;    
        
        HashMap centroidcandidatesdata = new HashMap();
        
        
        Vector termcolors = new Vector();
        HashMap node2colors = new HashMap();
        HashMap<String, Double> nodedistances = new HashMap<String, Double>();
        
        double arearadius = 10.0;
        
        Vector centroidcandidates = new Vector();
        
        
        try (Transaction tx1 = graphDB3.beginTx()){
        	
        	Vector helpquery = new Vector();
			
			
			for (int i=0; i<query_find.size(); i++) {
				Node temp = graphDB3.findNode(Labels.SINGLE_NODE,"name", query_find.get(i).toString());
				
				if (temp!=null) {
					helpquery.add(query_find.get(i).toString());
				}
				
			}
			
			if(helpquery.size()>1) {
				
				query = helpquery;
				//System.out.println("Helpquery size: " +query.size() + "  "+query);
				System.out.println("Helpquery size: " +query.size());
				//LOGGER.info("Helpquery size: " +query.size() + "  "+query);;
                    
                    
				//Check if all query terms can be reached in the graph database from one another 
				//(remove one that cannot be reached by one or all of the other terms)
				//
                    
				int helpquerysize = query.size();
                    
				HashSet helpqueryset = new HashSet();
				HashSet helpqueryset2bremoved = new HashSet();
                
                Vector helpquery2 = new Vector();
                
                for (int i=0; i<query.size(); i++) {
                    
                    helpqueryset.add(query.get(i).toString());
                    
                }    
                    
                    
                   
                HashMap numberofreachednodes = new HashMap();
                Iterator iteratorq1 = helpqueryset.iterator(); 

                while (iteratorq1.hasNext()){
                               
                	String queryterm = iteratorq1.next().toString();
                	Node temp = graphDB3.findNode(Labels.SINGLE_NODE,"name", queryterm.toLowerCase());
                	//if (temp!=null)
                	//LOGGER.info("Temp1 node: " +temp.getProperty("name"));;
                           
                	Iterator iteratorq2 = helpqueryset.iterator(); 
                	while (iteratorq2.hasNext()){
                		String queryterm2 = iteratorq2.next().toString(); 
                                   
                		if (!queryterm.equals(queryterm2)) {
                			Node temp2 = graphDB3.findNode(Labels.SINGLE_NODE,"name", queryterm2.toLowerCase());
                			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), 100, 1 );

                			Path p = finder.findSinglePath( temp, temp2 );
                                                                                    
                			if (p!=null) {
                				// LOGGER.info("Path found");;
                				if (numberofreachednodes.containsKey(queryterm)) {
                					HashSet helpset = (HashSet)numberofreachednodes.get(queryterm);
                					helpset.add(queryterm2);
                					numberofreachednodes.put(queryterm, helpset);
                				} else {
                					HashSet helpset= new HashSet();
                					helpset.add(queryterm2);
                					numberofreachednodes.put(queryterm, helpset);
                				}
                			}
                		}
                	}
                } 
                //LOGGER.info("Number of reached nodes: " +numberofreachednodes.toString());;
                           
                String mostreachableterm = "";
                int numberofneighbours = 0;
                       
                Iterator iteratorq3 = numberofreachednodes.keySet().iterator();
                while (iteratorq3.hasNext()){
                	String queryterm = iteratorq3.next().toString(); 
                           
                	HashSet helphashset = (HashSet) numberofreachednodes.get(queryterm);
                           
                	if (helphashset.size()>numberofneighbours) {
                               
                		numberofneighbours = helphashset.size();
                		mostreachableterm = queryterm;
                           
                	}
                       
                }  
                
                HashSet helphashset = new HashSet();
                
                if (!mostreachableterm.equals("")) {
                       
                	helphashset.addAll((HashSet) numberofreachednodes.get(mostreachableterm));
                           
                	helphashset.add(mostreachableterm);
                       
                }        

                helpqueryset = helphashset;
                
                for (int i=0; i<query.size(); i++) {
                    
                    String helpterm = query.get(i).toString();
                    
                    if (helpqueryset.contains(helpterm)) {
                        
                        helpquery2.add(helpterm);
                        
                    }
                    
                }           
                           
                query = helpquery2;
                //LOGGER.info("Helpquery2 size: " +query.size() + "  "+query);;
                //System.out.println("Helpquery2 size: " +query.size() + "  "+query);
            
                //hierher2
                if (query.size()>1) {
                    
                    
                    double largestdistanceofqueryterms = 0;
                    double largestpathlength = 0;
                    
                    for (int i=0; i<query.size(); i++) {
                        
                        for (int j=i+1; j<query.size(); j++) {
                            
                            Node temp = graphDB3.findNode(Labels.SINGLE_NODE,"name", query.get(i).toString().toLowerCase());
                            Node temp2 = graphDB3.findNode(Labels.SINGLE_NODE,"name", query.get(j).toString().toLowerCase());
                            
                            PathFinder<WeightedPath> finder = GraphAlgoFactory.dijkstra( PathExpanders.forTypeAndDirection( RelationshipTypes.IS_CONNECTED, Direction.BOTH ), "cost", 1 );

                            WeightedPath p = finder.findSinglePath( temp, temp2 );
                        
                            if (p!=null)
                            	if (p.weight()>largestdistanceofqueryterms) {
                            		largestpathlength = p.length();
                            		largestdistanceofqueryterms=p.weight();
                                        
                            		//LOGGER.info("Path found: " + largestpathlength + "  " + largestdistanceofqueryterms);
                            	}
                        }
                    }    
                    
                    //arearadius = Math.ceil(largestdistanceofqueryterms / 2.0)+Math.ceil((20*largestdistanceofqueryterms) / 100.0); //Math.ceil(largestdistanceofqueryterms / 2.0)+1;
                    
                    arearadius = Math.ceil((largestdistanceofqueryterms / 2.0) +  ((20*largestdistanceofqueryterms) / 100.0))  ; //Math.ceil(largestdistanceofqueryterms / 2.0)+1;
                                      
                    //LOGGER.info("largestdistanceofqueryterms: " +largestdistanceofqueryterms + "  " +largestpathlength + "  arearadius: "+arearadius);
                    
                    double count=0,maxcount=10; 
                    
                    long start = System.currentTimeMillis();    
                    
                    while ((centroidcandidates.size()<=query.size()) && (count<maxcount)   ) { //10   //neu count             

                    	count++;
                    	//LOGGER.info("Activation rounds to execute: " +count );
                    
                    	//if(count>2)
                    	//	arearadius = arearadius + (arearadius / 2.0);//arearadius + 1; //(arearadius / 2.0); 
                    	
                    	if (count>2 && count<10) 
                    		arearadius = arearadius + (arearadius / 2.0);//arearadius + 1; //(arearadius / 2.0); 
                    	else
                    		arearadius = arearadius + (arearadius / 2.0) + ((20 * arearadius) / 100.0);
                    	
                    	
                    	termcolors = new Vector();
                    	node2colors = new HashMap();    
                    	centroidcandidates = new Vector();
                    	nodedistances = new HashMap<String, Double>();
                    
                    	int color = 0;
                    
                    	for(Iterator i=query.iterator(); i.hasNext(); ){

                    		color++;
                    		termcolors.add(color);
                                            
                    		String curQueryTerm = i.next().toString();
                    		centroidcandidates.add(curQueryTerm);
                        
                        
                    		HashSet helpset = new HashSet();
                    		helpset.add(color);
                        
                    		node2colors.put(curQueryTerm, helpset);
                    		//LOGGER.info("Query Term: " + curQueryTerm + " Color: " + color);
                    		nodedistances.put(curQueryTerm, 0.0);
                    	}
	                    
                    	for(Iterator i=query.iterator(); i.hasNext(); ) {
    	                    
                    		String curQueryTerm = i.next().toString();
                    
                    		HashSet visited = new HashSet();
                    		HashMap<String, Double> visiteddistance = new HashMap(); 
                    
                    		LinkedList<String> queue = new LinkedList<String>();
                    
                    		visited.add(curQueryTerm);
                    		queue.add(curQueryTerm);
                    		visiteddistance.put(curQueryTerm, 0.0);
                    
                    		Node firstsourcenode = graphDB3.findNode(Labels.SINGLE_NODE,"name", curQueryTerm.toLowerCase());
                
                    		int steps=0;
                    
                    		while ((queue.size()!=0) && (steps<count)) {
                        
                    			steps++;
                        
                    			String sourcenodename = queue.poll();
                    			//    LOGGER.info("Activating: "+ sourcenodename);
                                                
                    			Node temp = graphDB3.findNode(Labels.SINGLE_NODE,"name", sourcenodename.toLowerCase());
                    			Iterable<Relationship> allRelationships = temp.getRelationships();
                        
                    			for (Relationship relationship : allRelationships){
                           
                    				Node destinationnode=relationship.getOtherNode(temp);
                           
                    				String destinationnodename =  destinationnode.getProperty("name").toString();
                           
                    				if (!visited.contains(destinationnodename)) {
                    					double hv1 = visiteddistance.get(sourcenodename).doubleValue();
                    					double hv2 = Double.MAX_VALUE;
                               
                               
                    					//  LOGGER.info("hv1: " + sourcenodename + " " + destinationnodename + " " + hv1);
                    					//   LOGGER.info("hv2: " + temp.getProperty("occur") + "  " +destinationnode.getProperty("occur") +   " " + relationship.getProperty("count") + " " + relationship.getProperty("cost"));
                    					//  double hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					if (relationship.getProperty("cost") instanceof Integer) {
                    						//       LOGGER.info("Object is Integer");
                    						hv2 = ((Integer)relationship.getProperty("cost")).doubleValue();
                    					}
                               
                    					if (relationship.getProperty("cost") instanceof Float) {
                    						//       LOGGER.info("Object is Float");
                    						hv2 = ((Float)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                                   
                    					if (relationship.getProperty("cost") instanceof Double) {
                    						//   LOGGER.info("Object is Double");
                    						hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                    					
                    					if (visiteddistance.containsKey(sourcenodename)   &&  (hv1+hv2<=arearadius)    ) {
         	                               
                    						visited.add(destinationnodename);
                    						queue.add(destinationnodename);
                               
                    						visiteddistance.put(destinationnodename, hv1 + hv2);
                               
                    					}
                    					
                    				} else {   //testen ob kleinere distanz zu einem bereits visited knoten gefunden wurde
     	                               
                    					double hv1 = visiteddistance.get(sourcenodename).doubleValue();
                    					double hv2 = Double.MAX_VALUE;			
	                               
                    					//  LOGGER.info("hv1: " + sourcenodename + " " + destinationnodename + " " + hv1);
                    					//  LOGGER.info("hv2: " + temp.getProperty("occur") + "  " +destinationnode.getProperty("occur") +   " " + relationship.getProperty("count") + " " + relationship.getProperty("cost"));
                    					// double hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					if (relationship.getProperty("cost") instanceof Integer) {
                    						//       LOGGER.info("Object is Integer");
                    						hv2 = ((Integer)relationship.getProperty("cost")).doubleValue();
                    					}
                               
                    					if (relationship.getProperty("cost") instanceof Float) {
                    						//       LOGGER.info("Object is Float");
                    						hv2 = ((Float)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                                   
                    					if (relationship.getProperty("cost") instanceof Double) {
                    						//   LOGGER.info("Object is Double");
                    						hv2 = ((Double)relationship.getProperty("cost")).doubleValue();
                               
                    					}
                               
                    					if (visiteddistance.containsKey(sourcenodename)   &&  (hv1+hv2<=arearadius)    ) {
                                   
                    						visiteddistance.put(destinationnodename, hv1+hv2);
                                   
                    					}                                   
                    				}
                    			} //allrelationships
                    		} //while queue	
                    		
                    		//HashSet help = (HashSet)node2colors.get(curQueryTerm);
    	                    
                    		Iterator iterator = visited.iterator(); 
                      
                    		// check values
                    		while (iterator.hasNext()){
                           
                    			String nodename = iterator.next().toString();
                    			// LOGGER.info("Value: "+ nodename+ " ");  
                           
                    			if (node2colors.containsKey(nodename)) {
                               
                    				HashSet helpset =  (HashSet)node2colors.get(nodename);
                               
                    				helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
                               
                    				node2colors.put(nodename, helpset);
                               
                               
                               
                    				double curdistance = 0.0;
                               
                    				//  if (nodedistances.containsKey(nodename))
                    				curdistance =  ((Double)nodedistances.get(nodename)).doubleValue();
                               
                    				curdistance = curdistance + visiteddistance.get(nodename).doubleValue();
                       
                    				nodedistances.put(nodename, curdistance);
                    			} else {
                               
                    				HashSet helpset = new HashSet();
                    				helpset.add(termcolors.get(query.indexOf(curQueryTerm)));
    
                    				node2colors.put(nodename, helpset);
                    				nodedistances.put(nodename, visiteddistance.get(nodename).doubleValue());
                               
                    			}
                    		}
                    
                    		// LOGGER.info("Value: "+ node2colors.get(curQueryTerm).toString()); 
                    		// LOGGER.info("Value: "+ node2colors.get("Grenzwert").toString());
                    	} //for all query terms				
	                               
                    	Iterator iterator = node2colors.keySet().iterator(); 
  	                  
                    	// check values
                    	while (iterator.hasNext()){
                       
                    		String nodename = iterator.next().toString();
                       
                    		if ((((HashSet)node2colors.get(nodename)).size()==termcolors.size()) ) {
                    			if (!centroidcandidates.contains(nodename)    )
                    				centroidcandidates.add(nodename);
                           
                    		} else {
                    			if (nodedistances.containsKey(nodename)) {
                    				nodedistances.remove(nodename);
                    			}
                    		}
                    	}
                
                    	//LOGGER.info("Centroid candidates: " +centroidcandidates.size() + "  " + centroidcandidates.toString());
                
                    	//for case no data inside of nodedistances is null that effect to no centroid
                    	if(count==maxcount) {
                    		if(nodedistances.isEmpty()) {
                    			maxcount++;
                    			//System.out.println("maxcount = "+maxcount);
                    		}
                    	}	
                    	
                    } // centroidcandidates.size()<10				
	                               
                    long stop = System.currentTimeMillis();
                    timeelapsed = ((double)(stop-start)/(double)1000);
                            
                    //LOGGER.info("Centroid determination took "+timeelapsed+" seconds.");
                    
                    double averagepathlength = 0;
                    
                    double maxdistance = Double.MAX_VALUE;
                    
                    //System.out.println("centroidcandidates are::"+centroidcandidates.toString());
                    //System.out.println("nodedistances are::"+nodedistances.toString());
                    
                    for (int i=0; i<centroidcandidates.size(); i++) {
                        
                        String candidate = centroidcandidates.get(i).toString();
                        
                        if (nodedistances.containsKey(candidate)) {
                        	if (((Double)nodedistances.get(candidate)).doubleValue()<maxdistance) {
                                   
                        		maxdistance = ((Double)nodedistances.get(candidate)).doubleValue();
                        		shortestaveragepathlength = maxdistance / query.size();
                        		centroid = candidate;
                        	}
                        }               
                    } //for centroidcandidates
                } // if querysize== 					
	                    
                System.out.println("FindClusterID:: node2colors: " + node2colors.size() + "   " +centroid +  "   " + shortestaveragepathlength);
				 
				
			}
			tx1.success();
		}
		return centroid;
	
	}
}
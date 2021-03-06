/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.wisc.cs.will.ILP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;

import edu.wisc.cs.will.Utils.condor.CondorFile;
import edu.wisc.cs.will.Utils.condor.CondorFileWriter;

//import convert.BlocksPlanner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import edu.wisc.cs.will.DataSetUtils.Example;
import edu.wisc.cs.will.FOPC.BindingList;
import edu.wisc.cs.will.FOPC.Clause;
import edu.wisc.cs.will.FOPC.HandleFOPCstrings;
import edu.wisc.cs.will.FOPC.Literal;
import edu.wisc.cs.will.FOPC.PredicateName;
import edu.wisc.cs.will.FOPC.Term;
import edu.wisc.cs.will.FOPC.Theory;
import edu.wisc.cs.will.FOPC.TypeSpec;
import edu.wisc.cs.will.FOPC.Unifier;
import edu.wisc.cs.will.FOPC.Variable;
import edu.wisc.cs.will.ResThmProver.DefaultHornClauseContext;
import edu.wisc.cs.will.ResThmProver.HornClauseContext;
import edu.wisc.cs.will.Utils.Utils;
import edu.wisc.cs.will.stdAIsearch.BestFirstSearch;
import edu.wisc.cs.will.stdAIsearch.SearchInterrupted;
import edu.wisc.cs.will.stdAIsearch.SearchStrategy;
import java.io.FileWriter;
/**
 *
 * @author twalker
 */
public final class ILPMain {
	
	public HandleFOPCstrings stringHandler;
	public int index = 0;
	public static final int maxTrial =5;	
	public static String[] argsPersist = null; 
	public static Theory outputTheory = null;
	public static String ShapeName = "Ell";
	private static HashMap<String,Double> OriginalConceptParams = null;

    public ILPouterLoop outerLooper;

    private LearnOneClause learnOneClause;

    public HornClauseContext context;

    public int numberOfFolds = 1;

    public String directory;

    public String prefix = null;

    public int firstFold = 0;

    public int lastFold = -1;

    public boolean checkpointEnabled = false;

    private long maxTimeInMilliseconds = 3 * 24 * 60 * 60 * 1000L; // As a default, allow a max of three days (e.g., overnight plus the weekend).  This is in milliseconds, but remember that the max time, command-line argument is in seconds!

    public boolean useRRR = false;

    public boolean flipFlopPosNeg = false;

    public boolean lowerCaseMeansVariable = false; // TODO - allow user to specify this.

    public String fileExtension = Utils.defaultFileExtension;

    boolean useOnion = true;

    public Boolean relevanceEnabled = true;

    public OnionFilter onionFilter = null;

    private static final String testBedsPrefix = "../Testbeds/"; // But DO include the backslash here.

    public Theory bestTheory = null;

    public CoverageScore bestTheoryTrainingScore = null;

    public ILPMain() {
    }

    public void setup(String... args) throws SearchInterrupted {
        args = Utils.chopCommentFromArgs(args);

        Utils.Verbosity defaultVerbosity = Utils.isDevelopmentRun() ? Utils.Verbosity.Developer : Utils.Verbosity.Medium;

        processFlagArguments(args);

        Utils.seedRandom(12345);

        if (Utils.isVerbositySet() == false) {
            Utils.setVerbosity(defaultVerbosity);
        }

        if (lastFold == -1) {
            lastFold = numberOfFolds - 1;
        }

        boolean partialRun = (firstFold != 0 && lastFold != numberOfFolds - 1);

        setupDirectoryAndPrefix(args);

        String[] newArgList = new String[4];
        newArgList[0] = directory + "/" + prefix + "_pos." + fileExtension;
        newArgList[1] = directory + "/" + prefix + "_neg." + fileExtension;
        newArgList[2] = directory + "/" + prefix + "_bk." + fileExtension;
        newArgList[3] = directory + "/" + prefix + "_facts." + fileExtension;

        if (flipFlopPosNeg) {
            String temp = newArgList[0];
            newArgList[0] = newArgList[1];
            newArgList[1] = temp;
        }

        Utils.createDribbleFile(directory + "/" + prefix + "_dribble" + (useRRR ? "_rrr" : "") + (flipFlopPosNeg ? "_flipFlopped" : "") + (partialRun ? "_fold" + firstFold + "to" + lastFold : "") + "." + fileExtension);
        //	Utils.waitHere(directory + prefix + "_dribble" + (useRRR ? "_rrr" : "") + (flipFlopPosNeg ? "_flipFlopped" : "") + (partialRun ? "_fold" + firstFold + "to" + lastFold : "" ) + "." + fileExtension);

        try {
            //	HandleFOPCstrings stringHandler = new HandleFOPCstrings(lowerCaseMeansVariable);
            if (context == null) {
                context = new DefaultHornClauseContext();
            }
            outerLooper = new ILPouterLoop(directory, prefix, newArgList, new Gleaner(), context);
        } catch (IOException e) {
            Utils.reportStackTrace(e);
            Utils.error("File not found: " + e.getMessage());
        }
        setupParameters();

        if (getLearnOneClause().createdSomeNegExamples) {
            Example.writeObjectsToFile(newArgList[1], getLearnOneClause().getNegExamples(), ".", "// Negative examples, including created ones.");
        }
        //Scanner myObj = new Scanner(System.in);
    	//myObj.nextInt(); //MD
        setupRelevance();
    }
    
    
    /*
     * Author: NR
     * Pick the best constraint predicate for teh next iteration and return 
     * add to best clause and return clause
     */
    private String getBestConstraint()
    {
    	String result=null;
    	
    	return result;
    }
    
    
    private String instantiateConcept(HashMap<String,Integer> params, Clause c)
    {
    	String head=null;
    	String body=null;
    	
    	return body;
    }
    
    /*
     * Author: MD
     * This method computes plan compression distance between the current theory
     * and the true theory
     */
    private double getPlanCompressionDistance(String thNew)
    {
    	double d = 0.0;
    	String g1 = "tower(a)^color(a,yellow)^height(a,2)^rectangle(b)^color(b,blue)^width(b,3)^length(b,4)^right(b,a)^"
    			+ "block(c)^location(w1)^block-location(c,w1)^right_behind(b,c)^block(d)"
    			+ "^location(w2)^block-location(d,w2)^bottom_end(a,d)^spatial-rel(top,0,w1,w2)";
    	String g2 = "tower(a)^color(a,red)^height(a,2)^rectangle(b)^color(b,blue)^width(b,3)^length(b,4)^right(b,a)^"
    			+ "block(c)^location(w1)^block-location(c,w1)^right_behind(b,c)^block(d)"
    			+ "^location(w2)^block-location(d,w2)^bottom_end(a,d)^spatial-rel(top,0,w1,w2)";
    	//String g2 = new String(g1);
    	try {
			//d = BlocksPlanner.compareNCD(g1, g2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return d;
    }
    
    
    /*
     * Author - MD
     * Method for resetting relevance file. Picks the structure from the
     * template file, replaces with provided definite clause, which signifies
     * the Relevance clause.
     */
    private String writeRelevanceFile(String file, String fileTemplate, String head, String body)
    {
    	//StringBuilder contentBuilder = new StringBuilder();
    	String content = null;
    	try {
    		//create copy
    		
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(file)));
			//Stream<String> stream = Files.lines( Paths.get(fileTemplate), StandardCharsets.UTF_8);
			//stream.forEach(s -> contentBuilder.append(s).append("\n"));
			//stream.close();
			content = new String(Files.readAllBytes(Paths.get(fileTemplate)));
			content = content.replaceAll("#--#", head);
			content = content.replaceAll("#~~#", body);
			bw.write(content);
			bw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return content;
    }

    public void runILP() throws SearchInterrupted, IOException {
    	long start1 = System.currentTimeMillis();
        long end1;
        int input = 0;
        int i = 0;
        ILPCrossValidationLoop cvLoop = new ILPCrossValidationLoop(outerLooper, numberOfFolds, firstFold, lastFold);
        CrossValidationResult results = null;
        Clause newClause = null;
    	outerLooper.initialize(false);
    	
    	int interesting = 0;
        Iterator<Clause> i_clause= context.getClausebase().getBackgroundKnowledge().iterator();
//        System.out.println("Hi checking first "+ context.getClausebase().getBackgroundKnowledge());
        List<Clause> ListOfClauses     = new ArrayList<Clause>();
        while(i_clause.hasNext()) {
       	 Clause ctemp=i_clause.next();
       	 if(ctemp.isDefiniteClauseRule())// && ctemp.getDefiniteClauseHead().getPredicateName().toString().contains(ShapeName)) {
       	 {
       		 if(ctemp.getDefiniteClauseHead().getPredicateName().toString().contains(ShapeName))
       			 interesting++;
       		 ListOfClauses.add(ctemp);
       	 }
        }
    	//Adding while loop for LAYERED REFINEMENT
    	//(not using Onion as it does not serve 
    	//our purpose of adding constraints)  -- MD
        double prevDist = 0.0;
    	for(int iter = 1;iter<=maxTrial;iter++) {
	                
	        //cvLoop.setFlipFlopPositiveAndNegativeExamples(flipFlopPosNeg);
	        cvLoop.setMaximumCrossValidationTimeInMillisec(maxTimeInMilliseconds);
	        cvLoop.executeCrossValidation();
	        results = cvLoop.getCrossValidationResults();
	        outputTheory = cvLoop.finalTheory;
	        if(iter>=maxTrial)
	        	break;
	        PredicateName pname =null;
	        input = 0;
	        //if(index<=interesting) {
            while(input == 0){

 	           if(index>=interesting)
 	        	   break;
            	
	        //-Nan--------------------
	        Clause c1=cvLoop.finalTheory.getSupportClauses().get(0);
	        List<Literal> cLit2     = new ArrayList<Literal>();
	        stringHandler = new HandleFOPCstrings();

	           List<Literal> cLit      = c1.getDefiniteClauseBody();
	           //System.out.println("Printing cLit:"+ cLit);
	           int counter = 1;
	           for(Literal l: cLit)
	           {
	              Collection<Variable> headVars = l.collectAllVariables();
	              BindingList bl = new BindingList();
	              for (Variable bVar : headVars)
	               {
	                       if ("_".equals(bVar.getName()))
	                        {
	                            bl.addBinding(bVar, (stringHandler.getVariableOrConstant(bVar.getTypeSpec(), "Anon"+counter).asTerm()));
	                            counter++;
	                        }
	                       else
	                       {
	                           bl.addBinding(bVar, stringHandler.getVariableOrConstant(bVar.getTypeSpec(), bVar.getName()).asTerm());
	                          }
	                 }
	                    cLit2.add(l.applyTheta(bl));
	            }
	           	
//	             for (Clause clause : context.getClausebase().getBackgroundKnowledge()) {
	             System.out.println(ListOfClauses);
//	             if(index>=ListOfClauses.size())
//	             {
//	            	 input=0;
//	            	 iter = maxTrial;
//	            	 break;
//	             }
	            	 
	             Clause clause=ListOfClauses.get(index);
	             
	             //if (i<2) 
	             {
	             i=i+1;

	             Literal clauseLit                        = clause.getDefiniteClauseBody().get(0);   // new addition
	             System.out.println(clauseLit + " :: "+index +" :: "+iter);//md
	             List<TypeSpec> newAdditionTypeSpec       = clauseLit.getPredicateName().getTypeOnlyList().get(0).getTypeSpecList();
	             pname = clauseLit.getPredicateName();
	             Collection<Variable> newAdditionVariable = clauseLit.collectAllVariables();
	               
	              BindingList bl2 = new BindingList();
	              for(Literal l2:cLit2)  //original clause
	              {
	                    List<TypeSpec> tspecOriginalClause             = l2.getPredicateName().getTypeOnlyList().get(0).getTypeSpecList();
	                    Collection<Variable> tsVariableOriginalClause  = l2.collectAllVariables();
	                   
	                    int originalclausecount = 0;
	                    for(Variable originalclause: tsVariableOriginalClause)
	                    {
	                       
	                        TypeSpec tsoc = tspecOriginalClause .get(originalclausecount);
	                        int newadditioncount = 0;
	                        for(Variable arg1: newAdditionVariable)
	                        {
	                            TypeSpec tsav = newAdditionTypeSpec.get(newadditioncount);
	                            if(tsoc.equals(tsav) && !(bl2.getTheta().containsKey(arg1)))
	                            {
	                                bl2.addBinding(arg1, originalclause);

	                            }
	                            newadditioncount++;
	                        }
	                        originalclausecount++;
	                    }
	               }
	               Literal clauseLit2                        = clause.getDefiniteClauseBody().get(0);
	               cLit2.add(clauseLit2.applyTheta(bl2));
	             }
//	           }
	           newClause = new Clause(c1.getStringHandler(), c1.posLiterals, cLit2);
	           System.out.println("Show this clause to user: "+newClause.getAntecedent().toString());
	           System.out.println(varCaseChange(newClause.getAntecedent().toString()));
	        //-End Nan-----------------
	           Scanner myObj = new Scanner(System.in);  // Create a Scanner object
	           System.out.println("Is this correct (Type 0 if No / 1 if Yes)?");
	           
	           input = myObj.nextInt();
	           index=index+1;
    		}
	        double dist = getPlanCompressionDistance(varCaseChange(newClause.getAntecedent().toString())); //MD
	        if(input>0 && Math.abs(dist-prevDist)<0.1)
	        	prevDist = dist;
	        else 
	        	input = 0;
        	//Utils.println(""+dist); //MD
	        if(iter<=maxTrial && input>0)
	        {
	        	//String c = getBestConstraint();
	        	try {
					Files.copy(new File(directory+"/"+prefix+"_bkRel."+fileExtension).toPath(), 
							new File(directory+"/"+prefix+"_bkRel_OLD"+System.currentTimeMillis()+"."+fileExtension).toPath(),
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//	        	FileInputStream fstreamtemp = null;
//				try {
//					fstreamtemp = new FileInputStream("C:\\Users\\dsd170230\\eclipse-workspace\\conceptForCwC\\Blocks\\blocks_bk.txt");
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//	            BufferedReader brtemp = new BufferedReader(new InputStreamReader(fstreamtemp));
//	            String strLinetemp;
//	            BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File("C:\\Users\\dsd170230\\eclipse-workspace\\conceptForCwC\\Blocks\\blocks_new_bk.txt")));
//	            String strLine3;
//  	            try {
//					while ((strLinetemp = brtemp.readLine()) != null)  
//					{
//						if((!strLinetemp.contains(pname.name)))
//						{
//							bw1.write(strLinetemp+"\n");
//						}
//					}
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//  	            bw1.close();
	        	String head = "Ell(s)";
	        	String body = varCaseChange(newClause.getAntecedent().toString());
	        	String rep = writeRelevanceFile(directory+"/"+prefix+"_bkRel."+fileExtension, 
	        			"./SingleExDescAdvice", head, body);

		        getLearnOneClause().initialized = false;
	        	getLearnOneClause().adviceProcessor.retractRelevanceAdvice();
	        	setupRelevance();
	        	setup(argsPersist);
	        	cvLoop = new ILPCrossValidationLoop(outerLooper, numberOfFolds, firstFold, lastFold);
	        	outerLooper.initialize(false);
	        	//getLearnOneClause().initialized = true;
	        	//cvLoop.unsetAdvice(cvLoop.getOuterLoop());
	        	//outerLooper=null;
	            //learnOneClause=null;
	            //context=null;
	            
	        	//setupRelevance();
	            //Scanner myObj = new Scanner(System.in);
	        	//myObj.nextInt();
	        	//setup(argsPersist);
	        	
	        	//cvLoop = new ILPCrossValidationLoop(outerLooper, numberOfFolds, firstFold, lastFold);;
	        	//outerLooper.initialize(false);
	        }
	        
        	//getLearnOneClause().initialized = true;
        	
    	}

        end1 = System.currentTimeMillis();
        Utils.println(results.toLongString()); //MD
        Utils.println(outputTheory.toPrettyString());//MD
        //test(cvLoop.finalTheory);
        Utils.println(index+"-----"+interesting);
        //Utils.println(cvLoop.getOuterLoop().innerLoopTask.getActiveAdvice().toString());
        Utils.println("\n% Took " + Utils.convertMillisecondsToTimeSpan(end1 - start1, 3) + ".");
        Utils.println("% Executed " + Utils.comma(getLearnOneClause().getTotalProofsProved()) + " proofs " + String.format("in %.2f seconds (%.2f proofs/sec).", getLearnOneClause().getTotalProofTimeInNanoseconds() / 1.0e9, getLearnOneClause().getAverageProofsCompletePerSecond()));
        Utils.println("% Performed " + Utils.comma(Unifier.getUnificationCount()) + " unifications while proving Horn clauses.");
    }

    
    /*
     * Testmethod no use
     * Auth: MD
     */
    private String varCaseChange(String sent)
    {
    	StringBuilder sb = new StringBuilder(sent);
    	sb.deleteCharAt(sent.length()-2); 
    	sb.deleteCharAt(0); 
    	sent = sb.toString();
    	String[] lits = sent.split("\\),");
    	String[] lits2 = new String[lits.length];
    	//System.out.println(lits[0]);
    	for(int iter = 0; iter<lits.length;iter++)
    	{
    		String lit = lits[iter];
    		String pred = lit.split("\\(")[0];
    		String[] args = lit.split("\\(")[1].split(",");
    		int i = pred.contains("SpRel")?1:0;
    		while(i<args.length) {
    			args[i] = args[i].toLowerCase();
    			if(args[i].trim().equals("a"))
    				args[i] = "s";
    			i++;
    		}
    		args[0] = pred.contains("SpRel")? "constant("+args[0]+")" : args[0];
    		if(iter>=lits.length-1)
    			lits2[iter] = pred+"("+String.join(",", args);
    		else
    			lits2[iter] = pred+"("+String.join(",", args)+")";
    	}
    	
    	return String.join(",", lits2);
    }
    
    public void writeLearnedTheory(String prologueString) {

        if (bestTheory != null) {
            if (directory != null && prefix != null) {
                File theoryFile = new File(directory, prefix + "_theory.txt");
                try {
                    String theoryAsString = prologueString + bestTheory.toPrettyString("") + "\n";

                    new CondorFileWriter(theoryFile).append(theoryAsString).close();
                } catch (IOException except) {
                    Utils.printlnErr("Could not save the learned theory to a file: " + except.toString() + ".");
                }
            }
        }
    }

    private void processFlagArguments(String[] args) throws IllegalArgumentException {
        // Allow these three to come in any order.
        for (int arg = 1; arg < args.length; arg++) {
            if (args[arg].equalsIgnoreCase("rrr") || args[arg].startsWith("-rrr")) {
                useRRR = true;
            }
            else if (args[arg].equalsIgnoreCase("true") || args[arg].startsWith("-true")) {
                useRRR = true;
            }
            else if (args[arg].equalsIgnoreCase("false") || args[arg].startsWith("-false")) {
                useRRR = false;
            }
            else if (args[arg].equalsIgnoreCase("std") || args[arg].startsWith("-std")) {
                useRRR = false;
            }
            else if (args[arg].startsWith("flip") || args[arg].startsWith("-flip")) {
                flipFlopPosNeg = true;
            }
            else if (args[arg].startsWith("-prefix=")) {
                prefix = args[arg].substring(args[arg].indexOf("=") + 1);
            }
            else if (Utils.isaInteger(args[arg])) {
                numberOfFolds = Integer.parseInt(args[arg]);
            } // A bare number is interpreted as the number of folds.
            else if (args[arg].startsWith("-folds=")) {
                numberOfFolds = Integer.parseInt(args[arg].substring(args[arg].indexOf("=") + 1));
            }
            else if (args[arg].startsWith("-fold=")) {
                firstFold = Integer.parseInt(args[arg].substring(args[arg].indexOf("=") + 1));
                lastFold = firstFold;
            }
            else if (args[arg].equals("-checkpoint")) {
                checkpointEnabled = true;
            }
            else if (args[arg].equals("-relevance")) {
                relevanceEnabled = true;
            }
            else if (args[arg].equals("-norelevance")) {
                relevanceEnabled = false;
            }
            else if (args[arg].startsWith("-maxTime=")) {
                int i = Integer.parseInt(args[arg].substring(args[arg].indexOf("=") + 1));
                if (i <= 0) {
                    maxTimeInMilliseconds = Long.MAX_VALUE;
                }
                else {
                    maxTimeInMilliseconds = i * 1000L;
                }
            }
            else if (args[arg].startsWith("useOnion") || args[arg].equalsIgnoreCase("-useOnion")) {
                useOnion = true;
            }
            else if (args[arg].startsWith("onion") || args[arg].equalsIgnoreCase("-onion")) {
                useOnion = true;
            }
            else if (args[arg].startsWith("noonion") || args[arg].startsWith("noOnion") || args[arg].equalsIgnoreCase("-noOnion")) {
                useOnion = false;
            }
            else if (args[arg].startsWith("-") == false) {
                fileExtension = args[1];
            }
            else {
                throw new IllegalArgumentException("Unknown argument specified: " + args[arg]);
            }
        }
    }

    private void setupDirectoryAndPrefix(String[] args) throws IllegalArgumentException {
        // LearnOnClause performs the inner loop of ILP.
        directory = args[0];
        File dir = new CondorFile(directory);
        if (dir.isDirectory() == false) {
            dir = new CondorFile(testBedsPrefix + directory);
        }
        if (dir.isDirectory() == false) {
            throw new IllegalArgumentException("Unable to find problem directory '" + directory + "'.");
        }
        directory = dir.getPath();
        if (prefix == null) {
            prefix = dir.getName();
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
    }

    private void setupParameters() {
        Gleaner gleaner = (Gleaner) getLearnOneClause().searchMonitor;
        outerLooper.writeGleanerFilesToDisk = true;
        //		if (args.length > 3) { getLearnOneClause().setMinPosCoverage(Double.parseDouble(args[3])); }
        //		if (args.length > 4) { getLearnOneClause().setMinPrecision(  Double.parseDouble(args[4]));   }
        // Set some additional parameters for the inner-loop runs.
        maxTimeInMilliseconds = 12 * 60 * 60 * 1000; // This is for any ONE task (but over ALL Onion layers for that task).
        getLearnOneClause().setMaxNodesToConsider(10000); // <-----------------------
        getLearnOneClause().setMaxNodesToCreate(100000);
        getLearnOneClause().maxSearchDepth = 1000;
        getLearnOneClause().verbosity = 0;
        getLearnOneClause().maxBodyLength = 4; // Changed by JWS for debugging purposes (feel free to edit).
        getLearnOneClause().maxNumberOfNewVars = 6;
        getLearnOneClause().maxDepthOfNewVars = 6;
        getLearnOneClause().maxPredOccurrences = 6;
        outerLooper.max_total_nodesExpanded = 100000; // <-----------------------
        outerLooper.max_total_nodesCreated = 10 * outerLooper.max_total_nodesExpanded;
        outerLooper.maxNumberOfClauses = 2; // <-----------------------
        outerLooper.maxNumberOfCycles = 2 * outerLooper.maxNumberOfClauses;
        getLearnOneClause().minNumberOfNegExamples = 0; //change by MD
        getLearnOneClause().setMinPosCoverage(1);
        getLearnOneClause().restartsRRR = 25;
        getLearnOneClause().stringHandler.setStarMode(TypeSpec.plusMode);
        getLearnOneClause().setMEstimatePos(0.01); // <-----------------------
        getLearnOneClause().setMEstimateNeg(0.01); // <-----------------------
        gleaner.reportingPeriod = 1000;
        outerLooper.setMinPrecisionOfAcceptableClause(0.01);// <----------------------- //MD change
        //outerLooper.initialize(false); // We want to initialize this as late assert possible.
        outerLooper.setCheckpointEnabled(checkpointEnabled);
        getLearnOneClause().setDumpGleanerEveryNexpansions(1000);
    }

    private void setupRelevance() throws SearchInterrupted {
    	
        if (isRelevanceEnabled()) {
            try {
                File file = getRelevanceFile();
                            	
                getLearnOneClause().setRelevanceFile(file);
                getLearnOneClause().setRelevanceEnabled(true);
            } catch (FileNotFoundException fileNotFoundException) {
                throw new SearchInterrupted("Relevance File '" + getRelevanceFile() + "' not found:\n  " + fileNotFoundException);
            } catch (IllegalStateException illegalStateException) {
                throw new SearchInterrupted(illegalStateException);
            }
        }
        else {
            getLearnOneClause().setRelevanceEnabled(false);
        }
    }

    public HornClauseContext getContext() {
        if (context == null) {
            if (outerLooper == null) {
                context = new DefaultHornClauseContext();
            }
            else {
                context = getLearnOneClause().getContext();
            }
        }

        return context;
    }

    public boolean isRelevanceEnabled() {
        return relevanceEnabled == null ? getRelevanceFile().exists() : relevanceEnabled;
    }

    public void setRelevanceEnabled(Boolean relevanceEnabled) {
        this.relevanceEnabled = relevanceEnabled;
    }

    public boolean isRelevanceEnableSet() {
        return relevanceEnabled != null;
    }

    public File getRelevanceFile() {
        File relevanceFile = new CondorFile(directory + "/" + prefix + "_bkRel." + fileExtension);

        return relevanceFile;
    }

    public LearnOneClause getLearnOneClause() {
        return outerLooper.innerLoopTask;
    }

    public Theory getBestTheory() {
        return bestTheory;
    }

    public static void mainJWS(String[] args) throws SearchInterrupted, IOException {
        //	Experimenter.mainJWS(args);
        ExperimenterMR.mainJWS(args);
    }

    public static void main(String[] args) throws SearchInterrupted, IOException {
        String userName = Utils.getUserName();
        //	waitHereUnlessCondorJob("user name = " + userName);
        if ("shavlik".equals(userName)) {
            mainJWS(args);
            System.exit(0);
        } // IF YOU ADD AN ENTRY, BE SURE TO USE *ELSE* OTHERWISE mainDefault will also be called.
        else if ("twalker".equals(userName)) {
            mainJWS(args);
            System.exit(0);
        }
        //	else if ("kunapg".equals( userName)) { mainGK( args);  System.exit(0); }
        //  else if () { mainYOU(args); }
        else {
            mainDefault(args);
        }
    }

    /**
     * @param args
     * @throws SearchInterrupted
     */
    public static void mainDefault(String[] args) throws SearchInterrupted {
        ILPMain main = new ILPMain();
        
        args=setConceptParams(args);
        //String[] argsMod = Arrays.copyOf(args, args.length-1);
        //args = argsMod;
        main.setup(args);
        
        argsPersist = args;
        try {
			main.runILP();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //System.out.println(main.getBestTheory());
    }
    private static String[] setConceptParams(String[] args)
    {
    	String[] argsMod = Arrays.copyOf(args, args.length-1);
    	OriginalConceptParams= new HashMap<String,Double>();
    	String paramString = args[args.length-1];
    	String[] components = paramString.split("\\),");
    	System.out.println(Arrays.asList(components));
    	
    	for(int i=1;i<components.length;i++)
    	{
    		String comp = components[i];
    		String[] parts = comp.split("\\(|,|\\)");
    		System.out.println(Arrays.asList(parts));
    		OriginalConceptParams.put(parts[0], Double.parseDouble(parts[2]));
    	}
    	//System.exit(0);
    	return argsMod;
    }
}

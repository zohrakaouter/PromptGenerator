package testppupGenerator.controlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.*;
import org.eclipse.jface.action.AbstractGroupMarker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.custom.ST;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import Utilities.ASTManager;
import Utilities.ASTModificationManager;
import Utilities.ChangeDetection;
import Utilities.ChatGPT;
import Utilities.ErrorsRetriever;
import Utilities.JavaVisitor;
import Utilities.MarkerComparator;
import Utilities.MoveResolution;
import Utilities.QuickAssistsProcessorGetterSetter;
import Utilities.Resolutions;
import Utilities.ResultSaver;
import Utilities.SaveModification;
import Utilities.Usage;
import Utilities.UsagePattern;
import Utilities.UsesManager;
import Utilities.UtilProjectParser;
import fr.lip6.meta.ComplexChangeDetection.Change;
import fr.lip6.meta.ComplexChangeDetection.AtomicChanges.DeleteClass;
import fr.lip6.meta.ComplexChangeDetection.AtomicChanges.DeleteProperty;
import fr.lip6.meta.ComplexChangeDetection.AtomicChanges.RenameClass;
import fr.lip6.meta.ComplexChangeDetection.AtomicChanges.RenameProperty;
import fr.lip6.meta.ComplexChangeDetection.AtomicChanges.SetProperty;
import fr.lip6.meta.ComplexChangeDetection.ComplexChanges.ComplexChange;
import fr.lip6.meta.ComplexChangeDetection.ComplexChanges.ExtractClass;
import fr.lip6.meta.ComplexChangeDetection.ComplexChanges.MoveProperty;
import fr.lip6.meta.ComplexChangeDetection.ComplexChanges.PullProperty;
import fr.lip6.meta.ComplexChangeDetection.ComplexChanges.PushProperty;
import testppupGenerator.model.Prompt;

public class PromptGenerator {
	public static ArrayList<Usage> sortMarkerList(CompilationUnit cu, ArrayList<IMarker> ml,
			ArrayList<Change> myChanges) {

		ArrayList<Usage> usages = new ArrayList<Usage>();
		ArrayList<Usage> nullusages = new ArrayList<Usage>();


		for (IMarker m : ml) {


			Usage usage = UsesManager.classify(myChanges, m, cu);

			//out = true;
			if(!usage.getPatterns().isEmpty()) {
				System.out.println(" full usage ");
				usages.add(usage);
			}
			else
				if ( (usage.getPatterns().isEmpty()) ) {

					Usage nullusage = new Usage();
					nullusage.setError(m);
					nullusage.setNode(ASTManager.getErrorNode(cu, m));

					nullusages.add(nullusage);

				}


		}

		for (Usage usage : nullusages) {
			System.out.println(" adding one null");
			usages.add(usage);

		}

		return usages;

	}
	public static void runQuickFixes() {

		//	ArrayList<Change> myChanges = ChangeDetection.initializeChangements();
		int cpt =1;
		//IProject project = UtilProjectParser.getSelectedProject();

		//ArrayList<ICompilationUnit> ListICompilUnit = UtilProjectParser.getCompilationUnits(project);

		//IJavaProject myp = UtilProjectParser.getJavaProject(project);
		ASTNode adeclaration = null;
		ASTNode anImport = null;
		Usage usage = null;
		IProblem iProblem = null;
		//ArrayList<Usage> usages = new ArrayList<Usage>();
		ICompilationUnit iCompilUnit =UtilProjectParser.getSelectedCU();

		//for (ICompilationUnit iCompilUnit : ListICompilUnit) {

		//System.out.println("Compilation unit : " + iCompilUnit.getElementName()+ cpt +" sur " +ListICompilUnit.size());
		cpt++;
		CompilationUnit compilUnit = ASTManager.getCompilationUnit(iCompilUnit);
		compilUnit.recordModifications();
		ArrayList<IProblem> problems = new ArrayList<>();
		Collections.addAll(problems, compilUnit.getProblems());
		System.out.println(" Initial number of problems " + problems.size());
		//ArrayList<IMarker> ml = new ArrayList<IMarker>();
		//	AST ast = compilUnit.getAST();// Get AST of the compilation unit
		// Create ASTWerite object to collect AST modifications
		//	ASTRewrite rewriter1 = ASTRewrite.create(ast);
		//	Document document;
		try {
			//ml = ErrorsRetriever.findJavaProblemMarkers(iCompilUnit);
			System.out.println(" Initial number of problems "+ problems.size());
			// Collections.sort(ml, new MarkerComparator());
			Iterator<IProblem> it = problems.iterator();
			while(it.hasNext()){
				IProblem ip= it.next();
				if(ip.isError()) {
					//document = new Document(iCompilUnit.getSource());

					JavaVisitor jVisitor = new JavaVisitor();
					//jVisitor.process(compilUnit);
					//	System.out.println("  THE GOT ERROR   " + im);
					//System.out.println(" next step");
					//Here apply only quick fixes
					//iProblem = ErrorsRetriever.getEquivalentProblem(problems, im);

					IJavaCompletionProposal ij =	hasQuickFixProblem(iCompilUnit,ip);

					//System.out.println("  ij** "+ ij.getDisplayString());
					if(ij!=null) {
						ij.apply(null);

						//	iCompilUnit.save(null, true);
						compilUnit = ASTManager.getCompilationUnit(iCompilUnit); // Refresh the compilation unit
						jVisitor.process(compilUnit);
						//TextEdit edits = rewriter1.rewriteAST(document, null);
						// Send modifications to be saved in disk
						//SaveModification.SaveModif(compilUnit, edits);
						Thread.sleep(3000);
						//ml = ErrorsRetriever.findJavaProblemMarkers(iCompilUnit);
						problems.clear();
						Collections.addAll(problems, compilUnit.getProblems());
						it = problems.iterator();
						System.out.println(" Next number of problems " + problems.size());

						//System.out.println(" Size of the ml " + ml.size());
						/*while (!ml.isEmpty()) {

					//	usages = sortMarkerList(compilUnit, ml, myChanges);

						// System.out.println(" change and node " + node+" "+change);

						//usage = usages.get(0);
						IMarker amarker = usage.getError();
						ASTNode node = usage.getNode();
						//Here apply only quick fixes
						//hasQuickFix(iCompilUnit, amarker,iProblem);
						System.out.println("  THE GOT ERROR   " + ml.get(0));
						//  IInvocationContext context = new AssistContext(iUnit , offset, length);

						// JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { problem }, proposals);        

						//ij.apply(null);
					}*/
					}
					else  System.out.println(" No proposals");
				}
				else System.out.println(" IT'S a warning");
			}

		}
		catch(Exception e) {

			System.out.println("   Exception :D" );
			e.printStackTrace();
		}


		//}
		System.out.println("  THE END ");
	}

	public static void appendStrToFile(String fileName,
			String str)
	{
		// Try block to check for exceptions
		try {

			// Open given file in append mode by creating an
			// object of BufferedWriter class
			BufferedWriter out = new BufferedWriter(
					new FileWriter(fileName, true));

			// Writing on output stream
			out.write(str);
			// Closing the connection
			out.close();
		}

		// Catch block to handle the exceptions
		catch (IOException e) {

			// Display message when exception occurs
			System.out.println("exception occurred" + e);
		}
	}
	public static String formatMarker(IMarker marker, CompilationUnit cu, ICompilationUnit iCompilationUnit)
	{
		String formattedMarker="";
		ASTNode errornode = ASTManager.getErrorNode(cu, marker);
		if(errornode instanceof SimpleName)
		{
			formattedMarker= "Error : "+((SimpleName)errornode).getIdentifier()+"CU : " +iCompilationUnit.getElementName()+ " Line : "+marker.getAttribute(IMarker.LINE_NUMBER, 0);
		}
		else if (errornode instanceof QualifiedName)
		{
			formattedMarker= "Error : "+ASTManager.getSNFromQF(errornode).getIdentifier()+"CU : " +iCompilationUnit.getElementName()+" Line : "+marker.getAttribute(IMarker.LINE_NUMBER, 0);	
		}
		else 
			System.out.println(" not treated situation in formatting marker, its type  "+ errornode.toString());

		return formattedMarker;

	}

	public static void run()
	{
		System.out.println(" ***************************************");
		System.out.println("  Running the prompt generator");
		System.out.println(" ***************************************");
		long startTime = System.nanoTime();

		ArrayList<Change> myChanges = ChangeDetection.initializeChangements();

		IProject project = UtilProjectParser.getSelectedProject();

		ArrayList<ICompilationUnit> ListICompilUnit = UtilProjectParser.getCompilationUnits(project);

		Prompt prompt;
		String code= "";
		int cpt=0;
		appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/ressfile.csv","NB,Change, Marker, Request, Result \n");

		for (ICompilationUnit iCompilUnit : ListICompilUnit) {

			System.out.println("Compilation unit : " + iCompilUnit.getElementName());
			CompilationUnit compilUnit = ASTManager.getCompilationUnit(iCompilUnit);

			ArrayList<IMarker> ml = new ArrayList<IMarker>();


			try {

				ml = ErrorsRetriever.findJavaProblemMarkers(iCompilUnit);
				double temp = 0.0;
				if (ml.size() > 0) {
					for(IMarker marker : ml)
					{

						System.out.println(" the correspondant marker  "+ formatMarker(marker, compilUnit,iCompilUnit));
						prompt=new Prompt();

						prompt=	generatePrompt(myChanges,marker,compilUnit);
						prompt.setMarker(formatMarker(marker, compilUnit,iCompilUnit));
						//	ResultSaver.run(prompt,pw);
						//	code =getCodePart(compilUnit, marker);
						if (!prompt.getRequest().isEmpty())
						{
							System.out.println("*** Asking chatgpt ...***");
							//ResultSaver.run(prompt,pw);
							// 
							//for(int i=0; i<10;i++)
							// {
							int i =0;
							String response="";
							Thread.sleep(3000);
							try {

								response=	ChatGPT.chatGPT(prompt.getRequest(), 0.2);//+code );
							}catch (Exception e) {
								// TODO: handle exception
								response= e.getMessage();
							}
							if(response.isEmpty())
							{
								prompt.setResult("Request too long ");
								response= " Empty response ";



								appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/ressfile"+00+".csv",cpt+","+prompt.toString()+ " Too long code "+","+response+"\n");
								appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/ressfile2.txt",cpt+","+prompt.toString()+ " Too long code "+","+response+"\n");
							}
							else {
								if(response.contains("```java")) {
									response= response.split("```java")[1];
									response=response.split("```")[0];
								}
								System.out.println(" **** Got Response : "+ response);
								appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/outputModisco/ressfile"+02+".csv",cpt+","+prompt.toString()+","+response.replace("\n","").replace("\r","").replace(";", "").replace(",", "")+"\n");//+//code.trim().replace('\n',' ').replace('\r',' ').replace(",", "").replace(";", "")+","+response.replace("\n","").replace("\r","").replace(";", "").replace(",", "")+"\n");
								appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/outputModisco/ressfile2"+02+".txt",cpt+","+prompt.toString()+","+response.replace("\n","").replace("\r","").replace(";", "").replace(",", "")+"\n");//+//code.trim().replace('\n',' ').replace('\r',' ').replace(",", "").replace(";", "")+","+response.replace("\n", "").replace("\r", "").replace(";", "").replace(",", "")+"\n");

								//	appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/ressfile.csv",cpt+","+prompt.toString()+code+","+response+"\n");
								//	appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/ressfile2.txt",cpt+","+prompt.toString()+code+","+response+"\n");

							}
							cpt++;
						}
						//}
						else {
							System.out.println( " Cannot generate a prompt , empty request ");
							prompt.setRequest("Cannot generate a prompt");
							appendStrToFile("/home/zkebaili/eclipse-workspace/PromptGenerator/outputModisco/ressfile"+02+".csv",cpt+","+prompt.toString()+","+"Cannot generate a prompt");//+//code.trim().replace('\n',' ').replace('\r',' ').replace(",", "").replace(";", "")+","+response.replace("\n","").replace("\r","").replace(";", "").replace(",", "")+"\n");
							
						}
					}
				}

			}catch (Exception e) {
				e.printStackTrace();

			}
		}

		System.out.println(" The end of prompt generator");
		long endTime = System.nanoTime();

		// obtenir la différence entre les deux valeurs de temps nano
		long timeElapsed = endTime - startTime;

		System.out.println("Execution time in nanoseconds: " + timeElapsed);
		System.out.println("Execution time in milliseconds: " + timeElapsed / 1000000);
		System.out.println("Execution time in minutes: " + timeElapsed / 1000000000/60);

	}

	public static String getMethodCode(CompilationUnit compilUnit,IMarker marker, String state)
	{
		String code ="";
		ASTNode errornode = ASTManager.getErrorNode(compilUnit, marker);
		code = ASTManager.findMethodDeclaration(errornode).toString();
		return code;
	}

	public static String getCodePart(CompilationUnit compilUnit,IMarker marker, String state)
	{
		String code ="";
		ASTNode errornode = ASTManager.getErrorNode(compilUnit, marker);

		//Statement st= (Statement)ASTManager.findStatement(errornode);
		if( state.equals("short")) {
			if( ASTManager.findStatement(errornode)!=null)
			{

				code= " Coevolve this java code snippet: "+((Statement) ASTManager.findStatement(errornode)).toString();
			}
			else if (ASTManager.findMethodDeclaration(errornode)!=null)
			{
				code =" Here is a java method signature : "+((MethodDeclaration)ASTManager.findMethodDeclaration(errornode)).toString().split("\\{")[0] + " Coevolve it according to the described change.";
				//	System.out.println(" The got string after SPLIT is " +code);

			}
			else if (ASTManager.findTypeDeclaration(errornode)!=null)
			{
				code= " Coevolve this java code snippet : "+ ASTManager.findTypeDeclaration(errornode).toString().split("\\{")[0];

			}
			else if(ASTManager.findImportDeclaration(errornode)!=null)
			{

				code= " Coevolve this java code snippet : "+ ASTManager.findImportDeclaration(errornode).toString();
			}

		}
		else if (state.equals("extended"))
		{
			if(ASTManager.findImportDeclaration(errornode)!=null)
			{

				code= " Coevolve this java code snippet : "+get3Imports(compilUnit, errornode);;

			}
			else 
				if( ASTManager.findStatement(errornode)!=null)
				{

					code= " Coevolve this java code snippet : "+ get3Instructions(compilUnit, errornode);
				}

		}

		return code; //.trim().replace('\n',' ').replace('\r',' ').replace(",", "").replace(";", " ");

	}
	public static String get3Imports(CompilationUnit cu,ASTNode impactedImport) {
		String imports="";
		int i=0;
		if(impactedImport instanceof QualifiedName)
		{
			ImportDeclaration id= (ImportDeclaration)ASTManager.findImportDeclaration(impactedImport);
			for (Object o :cu.imports())
			{
				if( o instanceof ImportDeclaration )
				{
					if(id.toString().equals(o.toString()))
					{

						if(i==0 && i+1 ==cu.imports().size() )
						{
							imports= cu.imports().get(0).toString();
						}
						else if (i==0 && i+1 < cu.imports().size() )
						{
							imports= cu.imports().get(0).toString()+cu.imports().get(i+1).toString();

						}
						else if( i-1 >0 && i+1 < cu.imports().size() )
						{
							imports= cu.imports().get(i-1).toString()+cu.imports().get(i).toString()+cu.imports().get(i+1).toString();
						}
						else if(i>0 && i+1 >cu.imports().size())
						{
							imports= cu.imports().get(i-1).toString()+cu.imports().get(i);	
						}

					}
					i++;
				}
			}
		}

		return imports;
	}
	public static String get3Instructions(CompilationUnit cu,ASTNode impactedInstuction) {
		String imports="";
		//int i=0;
		Statement instruct= (Statement)ASTManager.findStatement(impactedInstuction);
		if(instruct !=null && instruct instanceof Statement)
		{

			Block b = (Block) ASTManager.findBlock(instruct);
			System.out.println(" In get 3 instruct  1" + 	b.statements().size());



			int i= b.statements().indexOf(instruct);
			//if(id.toString().equals(o.toString()))
			//{
			System.out.println(" In get 3 instruct " + i);


			if(i==0 && i+1 ==b.statements().size() )
			{
				imports= b.statements().get(0).toString();
			}
			else if (i==0 && i+1 < b.statements().size() )
			{
				imports=b.statements().get(0).toString()+b.statements().get(i+1).toString();

			}
			else if( i-1 >0 && i+1 < b.statements().size() )
			{
				imports= b.statements().get(i-1).toString()+b.statements().get(i).toString()+b.statements().get(i+1).toString();
			}
			else if(i>0 && i+1 >b.statements().size())
			{
				imports= b.statements().get(i-1).toString()+b.statements().get(i);	
			}

			//	}
			//i++;
		}
		else System.out.println(" In get instruct but its not a statment ");




		return imports;
	}

	public static Prompt generatePrompt(ArrayList<Change> myChanges, IMarker error,CompilationUnit cu)
	{
		String request ="";
		Prompt prompt=new Prompt();
		String code = "";
		ASTNode errornode = ASTManager.getErrorNode(cu, error);
		for(Change change : myChanges) {

			if(errornode instanceof SimpleName) {

				if (change instanceof RenameClass)
				{

					String changename=((RenameClass)change).getName();
					String newname=((RenameClass)change).getNewname();
					if(((SimpleName)errornode).getIdentifier().equals(changename))
					{
						prompt.setChange("Rename class");
						code = getCodePart(cu, error,"short");
						//	request="The metaclass "+ changename+ " is renamed to "+ newname + "The class "+ changename+ " is generated " + code ;
						request="The class "+ changename+ " is generated from  The metaclass which "+ changename+ " is renamed to "+ newname  + code ;

					}
					else if(((SimpleName)errornode).getIdentifier().equals("get"+changename))
					{
						prompt.setChange("Rename class");
						code = getCodePart(cu, error,"short");
						//	request="The metaclass "+ changename+ " is renamed to "+ newname+
						//			"  the method get"+changename+" is generated and must be updated "+code ;
						request="  the method get"+changename+" is generated and must be updated " 
								+" The metaclass "+ changename+ " is renamed to "+ newname+ " "+code;

					}
					else if(((SimpleName)errornode).getIdentifier().equals("set"+changename))
					{
						prompt.setChange("Rename class");
						code = getCodePart(cu, error,"short");
						//request="The metaclass "+ changename+ " is renamed to "+ newname+
						//	"  the method set"+changename+" is generated and must be updated "+code;
						request="  the method set"+changename+" is generated and must be updated " 
								+" The metaclass "+ changename+ " is renamed to "+ newname+ " "+code;

					}
					else if(((SimpleName)errornode).getIdentifier().equals("create"+changename))
					{
						prompt.setChange("Rename class");
						code = getCodePart(cu, error,"short");
						//	request="The metaclass "+ changename+ " is renamed to "+ newname+
						//			"  the method create"+changename+" is generated and must be updated "+code;
						request="  the method create"+changename+" is generated and must be updated " 
								+" The metaclass "+ changename+ " is renamed to "+ newname+ " "+code;

					}
					else if(ASTManager.isLiteral(errornode))
					{
						String nodename=((SimpleName)errornode).getIdentifier();
						String literalchangename=ASTManager.makeLiteral(((RenameClass) change).getName());
						if(nodename.contains(literalchangename))
						{
							prompt.setChange("Rename class");
							code = getCodePart(cu, error,"short");
							//request="The metaclass "+ changename+ " is renamed to "+ newname+
							//	" the literal " +literalchangename+" is generated and must be updated"+code;
							request=" The literal " +literalchangename+" is generated from The metaclass "+ changename+ 
									"This metaclass called "+ changename+ " is renamed to "+ newname
									+code;

						}

					}
				}
				else if (change instanceof RenameProperty)
				{

					String changename=((RenameProperty)change).getName();
					String newname=((RenameProperty)change).getNewname();
					String classname= ((RenameProperty)change).getClassName();
					if(((SimpleName)errornode).getIdentifier().equals(changename))
					{

						prompt.setChange("Rename property");
						code = getCodePart(cu, error,"short");
						request="The attribute "+ changename+ " is renamed to "+ newname+" "+code ; // to review
					}
					if(((SimpleName)errornode).getIdentifier().equals("get"+capitalizeFirstLetter(((RenameProperty)change).getName())))
					{
						prompt.setChange("Rename property");
						code = getCodePart(cu, error,"short");
						//request=" The attribute "+ changename+ " is renamed to "+ newname+
						//	"  the method get"+capitalizeFirstLetter(changename)+" is generated and must be updated "+ code;
						request="  The method get"+capitalizeFirstLetter(changename)+" is generated from "
								+" The attribute "+ changename+ " which is renamed to "+ newname
								+ code;

					}
					if(((SimpleName)errornode).getIdentifier().equals("set"+capitalizeFirstLetter(((RenameProperty)change).getName())))
					{
						prompt.setChange("Rename property");
						code = getCodePart(cu, error,"short");
						//request="The attribute "+ changename+ " is renamed to "+ newname+
						//" the method set"+capitalizeFirstLetter(changename)+" is generated and must be updated "+code;
						request="  The method set"+capitalizeFirstLetter(changename)+" is generated from "
								+" The attribute "+ changename+ " which is renamed to "+ newname
								+ code;
					}
					if(((SimpleName)errornode).getIdentifier().contains("get"+((RenameProperty)change).getClassName()+"_"+((RenameProperty)change).getName()))
					{
						prompt.setChange("Rename property");
						code = getCodePart(cu, error,"short");
						//request="The attribute "+ changename+ " is renamed to "+ newname+
						//"  the method get"+classname+"_"+changename+" is generated and must be updated "+code;

						request="  The method get"+classname+"_"+changename+" is generated from  "+
								" The attribute "+ changename+ " which is renamed to "+ newname+" "+code;

					}

					if(ASTManager.isLiteral(errornode))
					{

						String changeLiteral =ASTManager.makeLiteral(((RenameProperty)change).getName());
						String classliteral =ASTManager.makeLiteral(classname);

						if(((SimpleName)errornode).getIdentifier().contains(changeLiteral))
						{
							prompt.setChange("Rename property");
							code = getCodePart(cu, error,"short");
							//request="The class "+ changename+ " is renamed to "+ newname+
							//"  the literal " +classliteral+"__"+changeLiteral+" is generated and must be updated "+code;
							request="  The literal " +classliteral+"__"+changeLiteral+" is generated from "+
									" The class "+ changename+ " which is renamed to "+ newname+" "+code;

						}

					}

					if(ASTManager.containOnlyCapitals(((SimpleName)errornode).getIdentifier())) {
						String errorid=((SimpleName)errornode).getIdentifier();
						errorid=errorid.toLowerCase();
						errorid=errorid.replace("_", "");
						RenameProperty rp=((RenameProperty)change);
						if(errorid.equals(rp.getClassName().toLowerCase()+rp.getName().toLowerCase()))
						{
							request=" not treated case ";
						}

					}


				}
				else if(change instanceof MoveProperty)
				{



					String propertymaj =capitalizeFirstLetter(((MoveProperty)change).getName());

					String property=((MoveProperty)change).getName();
					String sourceclassname = ((MoveProperty)change).getSourceClassName();
					String targetclassname = ((MoveProperty)change).getTargetClassName();
					String reference = ((MoveProperty)change).getThroughReference();
					String errorsn =((SimpleName)errornode).getIdentifier();
					if(((SimpleName)errornode).getIdentifier().equals("get"+propertymaj) || ((SimpleName)errornode).getIdentifier().equals("set"+propertymaj)) {

						if(((MoveProperty)change).getUpperBound()==-1)
						{
							prompt.setChange("Move property");
							code = getCodePart(cu, error,"short");
							request="The attribute "+ property+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+ reference+
									" Considering that calling get"+ capitalizeFirstLetter(reference) +" from the class "+sourceclassname +" returns a list of "+targetclassname+ " objects "+ code;

						}
						else
						{
							prompt.setChange("Move property");
							code = getCodePart(cu, error,"short");
							request="The attribute "+ property+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+ reference+
									" Considering that calling get"+ capitalizeFirstLetter(reference) +" from the class "+sourceclassname +" returns an instance of "+targetclassname+ " object "+code;


						}

					}
					else  if(ASTManager.containOnlyCapitals(((SimpleName)errornode).getIdentifier()))
					{
						if((((SimpleName)errornode).getIdentifier()).equals((((MoveProperty)change).getSourceClassName().toUpperCase()+"__"+ASTManager.makeLiteral(((MoveProperty)change).getName())))) {

							if(ASTManager.isLiteral(errornode)&& ((SimpleName)errornode).getIdentifier().equals(((MoveProperty)change).getSourceClassName().toUpperCase()+"__"+ASTManager.makeLiteral(((MoveProperty)change).getName())))
							{

								prompt.setChange("Move property");
								code = getCodePart(cu, error,"short");
								request="The attribute "+ property+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+ reference+
										" The literal "+errorsn +" is generated by combining the literal of the source class and the literal of the attribute  and must be updated "+ code;

							}
							else

							{

								System.out.println(" not treated case ");
							}
						}
					}
					else

					{

						System.out.println(" not treated case 2");
					}
				}
				else if( change instanceof PushProperty)
				{


					String property = ((PushProperty)change).getName();
					String superclass = ((PushProperty)change).getSuperClassName();
					ArrayList<String> subclasses = ((PushProperty)change).getSubClassesNames();
					String subclassesString="";
					String errorliteral =((SimpleName)errornode).getIdentifier();
					int cpt=0;
					for ( String s: subclasses)
					{
						if(cpt==subclasses.size())
							subclassesString+=" and "+s;
						else 
							subclassesString+=s+" ,";
					}

					if(((SimpleName)errornode).getIdentifier().equals((((PushProperty)change).getName())))
					{
						if(subclasses.size()>1)
						{
							prompt.setChange("Push property");
							//code = getCodePart(cu, error,"short");
							
							request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclasses : "+subclassesString+ " "+ code;

						}
						else 
						{
							prompt.setChange("Push class");
							code = getCodePart(cu, error,"short");
							request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclass : "+subclasses.get(0)+" "
							+code;
						}
					}
					if(((SimpleName)errornode).getIdentifier().equals("get"+capitalizeFirstLetter(((PushProperty)change).getName())) ||((SimpleName)errornode).getIdentifier().equals("is"+capitalizeFirstLetter(((PushProperty)change).getName())) )
					{
						prompt.setChange("Push class");
						code = getCodePart(cu, error,"short");
						if(subclasses.size()>1)

							//request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclasses : "+subclassesString
							//+" The method get"+capitalizeFirstLetter(property)+" is generated and must be updated "+code;
							request=" The method get"+capitalizeFirstLetter(property)+" is generated from the attribute "+property+

							"This attribute is pushed from the super class"+superclass+ " to the subclasses : "+subclassesString

							//+code;
							+ " Coevolve this java code snippet "+getMethodCode(cu, error, "short");

						else 
							//request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclass : "+subclasses.get(0)
						//+" The method get"+capitalizeFirstLetter(property)+" is generated and must be updated "+code;

							request=" The method get"+capitalizeFirstLetter(property)+" is generated from the attribute "+property+

							"This attribute is pushed from the super class"+superclass+ " to the subclasse : "+subclasses.get(0)

							//+code;
							+ " Coevolve this java code snippet "+getMethodCode(cu, error, "short");



					}
					else if(((SimpleName)errornode).getIdentifier().equals("set"+capitalizeFirstLetter(((PushProperty)change).getName())))
					{
						prompt.setChange("Push class");
						code = getCodePart(cu, error,"short");
						
						if(subclasses.size()>1)
							//request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclasses : "+subclassesString
							//+" The method set"+capitalizeFirstLetter(property)+" is generated and must be updated "+code;
							request=" The method set"+capitalizeFirstLetter(property)+" is generated from the attribute "+property+

							"This attribute is pushed from the super class"+superclass+ " to the subclasses : "+subclassesString

							//+code;
							+ " Coevolve this java code snippet "+getMethodCode(cu, error, "short");

							else 
								//request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclass : "+subclasses.get(0)
					//	+" The method set"+capitalizeFirstLetter(property)+" is generated and must be updated "+code;
								request=" The method set"+capitalizeFirstLetter(property)+" is generated from the attribute "+property+

								"This attribute is pushed from the super class"+superclass+ " to the subclasse : "+subclasses.get(0)

								//+code;
								+ " Coevolve this java code snippet "+getMethodCode(cu, error, "short");

					}
					if(ASTManager.isLiteral(errornode)&& ((SimpleName)errornode).getIdentifier().equals(ASTManager.makeLiteral(((PushProperty)change).getSuperClassName())+"__"+(ASTManager.makeLiteral(((PushProperty)change).getName()))))
					{
						prompt.setChange("Push class");
						code = getCodePart(cu, error,"short");
						if(subclasses.size()>1)
							request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclasses : "+subclassesString
							+" The literal " +errorliteral+" is generated by combining the literal of the superclass and the literal of the attribute and must be updated "+code;

						else request= "The attribute "+property+ " is pushed from the super class"+superclass+ " to the subclass : "+subclasses.get(0)
						+" The literal " +errorliteral+" is generated by combining the literal of the superclass and the literal of the attribute and must be updated "+code;

					}

				}
				else	if(change instanceof DeleteClass)
				{


					String changename=((DeleteClass)change).getName();

					if(((SimpleName)errornode).getIdentifier().equals(changename))
					{
						prompt.setChange("Delete Class ");
						code = getCodePart(cu, error,"short");
						request="Considering a metaclass in a metamodel called "+ changename+ " The class " +changename +" is generated from this metaclass. Here is a change: the metaclass "+ changename+" is removed with all the generated classes and properties are removed. "+code +" Give me the updated code without any explanations." ;

					}
					else if(((SimpleName)errornode).getIdentifier().equals("get"+changename))
					{
						prompt.setChange("Delete Class ");
						code = getCodePart(cu, error,"short");
						request="The metaclass "+ changename+ " is deleted "+
								"  the method get"+changename+" is generated and its usage must be deleted "+code + " If there is any instruction to delete please just comment it.";

					}
					else if(((SimpleName)errornode).getIdentifier().equals("set"+changename))
					{
						prompt.setChange("Delete Class ");
						code = getCodePart(cu, error,"short");
						request="The metaclass "+ changename+ " is deleted "+
								" the method set"+changename+" is generated and its usage must be deleted "+ code+ " If there is any instruction to delete please comment it with significant message ";
					}
					else if(((SimpleName)errornode).getIdentifier().equals("create"+changename))
					{
						prompt.setChange("Delete Class ");
						code = getCodePart(cu, error,"short");
						request="The metaclass "+ changename+ " is deleted "+
								"  the method create"+changename+" is generated and its usage must be deleted "+ code+ " If there is any instruction to delete, please comment it with significant message ";
					}
					else if(ASTManager.isLiteral(errornode))
					{
						String nodename=((SimpleName)errornode).getIdentifier();
						String literalchangename=ASTManager.makeLiteral(((DeleteClass) change).getName());
						if(nodename.contains(literalchangename))
						{
							prompt.setChange("Delete Class ");
							code = getCodePart(cu, error,"short");
							request="The metaclass "+ changename+ " is deleted "+
									" the literal " +literalchangename+" is generated its usage must be deleted " + code+ " If there is any instruction to delete, please comment it with significant message ";
						}

					}

				}
				else if(change instanceof DeleteProperty)
				{

					String changename=((DeleteProperty)change).getName();

					String classname= ((DeleteProperty)change).getClassName();
					if(((SimpleName)errornode).getIdentifier().equals(changename))
					{

						prompt.setChange("Delete property");
						code = getCodePart(cu, error,"extended");
						System.out.println(" The call of get 3 instruct " + get3Instructions(cu, errornode));
						request="The attribute "+ changename+ " is deleted from the class "+ classname +" "+ code + " If there is any instruction to delete, please comment it with significant message "; // To review


					}
					if(((SimpleName)errornode).getIdentifier().equals("get"+capitalizeFirstLetter(((DeleteProperty)change).getName())))
					{
						prompt.setChange("Delete property");
						code = getCodePart(cu, error,"extended");
						System.out.println(" The call of get 3 instruct " + get3Instructions(cu, errornode));

						request="The attribute "+ changename+ " is deleted from the class "+ classname+
								"  the method get"+capitalizeFirstLetter(changename)+" is generated and its usage must be deleted "+ code+ " If there is any instruction to delete, please comment it with significant message ";
					}
					if(((SimpleName)errornode).getIdentifier().equals("set"+capitalizeFirstLetter(((DeleteProperty)change).getName())))
					{
						prompt.setChange("Delete property");
						code = getCodePart(cu, error,"extended");
						System.out.println(" The call of get 3 instruct " + get3Instructions(cu, errornode));

						request="The attribute "+ changename+ " is deleted from the class "+ classname+
								" the method set"+capitalizeFirstLetter(changename)+" is generated and its usage must be deleted "+code + " If there is any instruction to delete, please comment it with significant message ";
					}
					if(((SimpleName)errornode).getIdentifier().contains("get"+((DeleteProperty)change).getClassName()+"_"+((DeleteProperty)change).getName()))
					{
						prompt.setChange("Delete property");
						code = getCodePart(cu, error,"extended");
						System.out.println(" The call of get 3 instruct " + get3Instructions(cu, errornode));

						request="The attribute "+ changename+ " is deleted from the class "+ classname+
								"  the method get"+classname+"_"+changename+" is generated and its usage must be deleted"+ code + " If there is any instruction to delete, please comment it with significant message ";

					}

					if(ASTManager.isLiteral(errornode))
					{
						String changeLiteral =ASTManager.makeLiteral(((DeleteProperty)change).getName());
						String classliteral =ASTManager.makeLiteral(classname);

						if(((SimpleName)errornode).getIdentifier().contains(changeLiteral))
						{
							prompt.setChange("Delete property");
							code = getCodePart(cu, error,"short");
							System.out.println(" The call of get 3 instruct " + get3Instructions(cu, errornode));

							request="The class "+ changename+ " is deleted from the class "+ classname+
									"  the literal " +classliteral+"__"+changeLiteral+" is generated and its usage must be deleted "+code + " If there is any instruction to delete, please comment it with significant message "; // to review because must be replaced by default value
						}

					}

					if(ASTManager.containOnlyCapitals(((SimpleName)errornode).getIdentifier())) {
						String errorid=((SimpleName)errornode).getIdentifier();
						errorid=errorid.toLowerCase();
						errorid=errorid.replace("_", "");
						DeleteProperty rp=((DeleteProperty)change);
						if(errorid.equals(rp.getClassName().toLowerCase()+rp.getName().toLowerCase()))
						{
							System.out.println(" not treated case "+error);
						}

					}

				}
				else if( change instanceof ExtractClass)
				{

					String sourceclassname = ((ExtractClass)change).getSourceClassName();
					String targetclassname = ((ExtractClass)change).getTargetClassName();

					String errorsn =((SimpleName)errornode).getIdentifier();

					ArrayList<ComplexChange> moves = ((ExtractClass)change).getMoves();
					for( ComplexChange cc :moves) {
						if( cc instanceof MoveProperty) {
							MoveProperty mp= (MoveProperty)cc;

							String propefter=mp.getName(); // get the extracted property
							String prop =capitalizeFirstLetter(mp.getName());
							String reference = ((MoveProperty)cc).getThroughReference();

							if(((MoveProperty)cc).getUpperBound()==-1)
							{

								if(((SimpleName)errornode).getIdentifier().equals("get"+prop) ) 
								{

									prompt.setChange("Extract property");

									code = getCodePart(cu, error,"short");
									request="The method get"+ capitalizeFirstLetter(reference)+" is generated from the reference "+reference+ " \n Considering that calling  get"+ capitalizeFirstLetter(reference) +" from the class  "+sourceclassname +" returns a list of "+targetclassname+ " objects "
											+ "\n The attribute "+ propefter+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+reference+ "  \n"+ 
											code;
								}
								else if (((SimpleName)errornode).getIdentifier().equals("set"+prop))
								{
									prompt.setChange("Extract property");
									code = getCodePart(cu, error,"short");
									request="The method set"+ capitalizeFirstLetter(reference)+" is generated from the reference "+reference+ " Considering that calling  set"+ capitalizeFirstLetter(reference) +" from the class  "+sourceclassname +" returns a list of "+targetclassname+ " objects "
											+ " \n The attribute "+ propefter+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+reference+ " \n"+ 
											code;
								}
							}
							else
							{
								if(errorsn.equals("get"+prop) ) {

									prompt.setChange("Extract property");
									code = getCodePart(cu, error,"short");
									request="The method get"+ capitalizeFirstLetter(reference)+" is generated from the reference "+reference+ " Considering that calling  get"+ capitalizeFirstLetter(reference) +" from the class  "+sourceclassname +" returns an instance of "+targetclassname+ " object "
											+ " The attribute "+ propefter+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+reference+ "  \n"+ 
											code;

								}
								else if (errorsn.equals("set"+prop))
								{
									prompt.setChange("Extract property");
									code = getCodePart(cu, error,"short");
									request="The method set"+ capitalizeFirstLetter(reference)+" is generated from the reference "+reference+ " Considering that calling  get"+ capitalizeFirstLetter(reference) +" from the class  "+sourceclassname +" returns an instance of "+targetclassname+ " object "
											+ " \n The attribute "+ propefter+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference" +reference+ "\n "+ 
											code;

								}
							}
							if(ASTManager.containOnlyCapitals(errorsn))
							{
								prompt.setChange("Extract property");
								code = getCodePart(cu, error,"short");
								if(errorsn.equals(((sourceclassname.toUpperCase()+"__"+ASTManager.makeLiteral(propefter))))) 
								{

									request="The attribute "+ propefter+ " is moved from the class "+ sourceclassname + " to the class "+ targetclassname+ " through the reference "+ reference+
											" The literal "+errorsn +" is generated by combining the literal of the source class and the literal of the attribute  and must be updated "+code;

								}

							}
						}

					}

				}
				else if( change instanceof PullProperty)
				{


					String property = ((PullProperty)change).getName();
					String superclass = ((PullProperty)change).getSuperClassName();
					ArrayList<String> subclasses = ((PullProperty)change).getSubClassesNames();
					//String subclassesString="";
					String erreur= ((SimpleName)errornode).getIdentifier();

					int cpt=0;
					for ( String s: subclasses)
					{


						if(erreur.equals(property))
						{

							prompt.setChange("Pull class");
							code = getCodePart(cu, error,"short");
							request= "The attribute "+property+ " is pulled from a subclass to the super class : "+superclass+" "+code;

						}
						if(((SimpleName)errornode).getIdentifier().equals("get"+capitalizeFirstLetter(((PullProperty)change).getName())) ||((SimpleName)errornode).getIdentifier().equals("is"+capitalizeFirstLetter(((PullProperty)change).getName())) )
						{

							prompt.setChange("Pull class");
							code = getCodePart(cu, error,"short");
							request= "The attribute "+property+ " is pulled from a subclass to the superclass : "+superclass
									+" The method get"+capitalizeFirstLetter(property)+" is generated and must be updated "+code;



						}
						else if(((SimpleName)errornode).getIdentifier().equals("set"+capitalizeFirstLetter(((PullProperty)change).getName())))
						{
							prompt.setChange("Pull class");
							code = getCodePart(cu, error,"short");
							request= "The attribute "+property+ " is pushed from a subclass to the superclass : "+superclass
									+" The method set"+capitalizeFirstLetter(property)+" is generated and must be updated "+code;

						}
						if(ASTManager.isLiteral(errornode)&& ((SimpleName)errornode).getIdentifier().equals(ASTManager.makeLiteral(s+"__"+(ASTManager.makeLiteral(((PullProperty)change).getName())))))
						{
							prompt.setChange("Pull class");
							code = getCodePart(cu, error,"short");
							request= "The attribute "+property+ " is pushed from the  subclass"+s+ " to the super class : "+superclass
									+" The literal " +error+" is generated by combining the literal of the subclass and the literal of the attribute and must be updated "+code;


						}



					}


				}
				else request= " SN but Cannot be matched with any change";
			} 
			else if( errornode instanceof QualifiedName)
			{
				if(change instanceof DeleteClass)
				{


					String changename=((DeleteClass)change).getName();

					if(ASTManager.getSNFromQF(errornode).getIdentifier().equals(changename))
					{
						prompt.setChange("Delete Class ");
						code = getCodePart(cu, error,"extended");
						request="The metaclass "+ changename+ " is deleted and the class "+ changename+ " is deleted also (assuming that when the metaclass is deleted all the generated elements are deleted )"+code + " If there is any instruction to delete, please comment it with significant message " ;
					}
				} else request= " QN but Cannot be matched with any change";
			}
			else request= "not SN not QN Cannot be matched with any change";
		}

		prompt.setRequest(request);
		prompt.setResult("No result Before testing");
		return prompt;
	}
	public static IJavaCompletionProposal hasQuickFixProblem(ICompilationUnit iCompilUnit,IProblem iProblem) {
		int offset = iProblem.getSourceStart();

		int end = iProblem.getSourceEnd();

		int length = end + 1 - offset;

		QuickAssistsProcessorGetterSetter qa = new QuickAssistsProcessorGetterSetter();

		IInvocationContext context = new AssistContext(iCompilUnit, offset, length);
		ArrayList<IJavaCompletionProposal> proposals = new ArrayList<IJavaCompletionProposal>();
		//System.out.println(" iproblem is "+ iProblem.getMessage());
		if (iProblem != null) {

			ProblemLocation problem = new ProblemLocation(iProblem);
			System.out.println(" for the problem " + iProblem.getSourceLineNumber()+ " Details "+ iProblem.getMessage());
			long start1 = System.currentTimeMillis();

			// System.out.println("the context is " + context.
			JavaCorrectionProcessor.collectCorrections(context, new IProblemLocation[] { problem }, proposals);

			long end1 = System.currentTimeMillis();
			System.out.println(" The time is IS " + (end1 - start1));
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/*	for (IJavaCompletionProposal ijp : proposals) {

				if (ijp.getDisplayString().equals("Add unimplemented methods")
						|| ijp.getDisplayString().contains("Import '")) {
					return ijp;
				}

			}*/
			for (IJavaCompletionProposal ijp : proposals) {
				System.out.println(" Proposal : "+ijp.getDisplayString());
			}


			if(proposals.size()>0) {
				System.out.println(" The found proposal "+ proposals.get(0).getDisplayString());
				return proposals.get(0);
			}

		}

		return null;
	}
	public static String capitalizeFirstLetter(String s)
	{

		String firstLetStr = s.substring(0, 1);
		// Get remaining letter using substring
		String remLetStr = s.substring(1);

		// convert the first letter of String to uppercase
		firstLetStr = firstLetStr.toUpperCase();

		// concantenate the first letter and remaining string
		return firstLetStr + remLetStr;
	}


}

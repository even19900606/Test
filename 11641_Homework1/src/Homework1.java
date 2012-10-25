/*
 * Course: Search Engine and Web Mining
 * Homework: 1
 * Name: Yiwen Chen
 * Andrew ID: yiwenche
 */
import java.io.*;
import java.util.*;

//Version4 
public class Homework1 {
	public static void main(String args[]) throws Exception{
		long start, end, time;
		
		//save the list of stopwords into memory
		HashMap<String, Integer>stopword = new HashMap<String, Integer>();
		LoadStopWords(stopword);
		start = System.currentTimeMillis();
		//Handling the queries
		MainProcess(stopword);
		//Compute the running time of the queries
		end = System.currentTimeMillis();
		time = end - start;
		System.out.println("The running time is: " + time + " milliseconds");
	}	
	//The whole process of boolean query handling
	public static void MainProcess(HashMap<String, Integer> stopword)throws Exception{
		//Variable Declaration
		ScoreList s;
		Node root;
		String parse, queryID;
		StringBuilder retrievalResult;
		BufferedReader in;
		BufferedWriter output;
		int dilimiter, index;
		File f, file;
		
//		//*Select*: Unstructured Query with #OR
//		f = new File("/CS/Semester1/Search Engine and Web Mining" +
//				"/Homework1/querySet1.txt");
//		//*Select*: Unstructured Query with #AND
//		f = new File("/CS/Semester1/Search Engine and Web Mining" +
//				"/Homework1/querySet2.txt");
		//*Select*: Structured Query
		f = new File("/CS/Semester1/Search Engine and Web Mining" +
				"/Homework1/querySet3.txt");
	
		in = new BufferedReader(new FileReader(f));
		dilimiter = -1;
		index = 0;
		queryID = null;
		retrievalResult = new StringBuilder();
		/*Select*: You can create six different files based on the query set and 
        query evaluation method (Namely: unrankedOR.txt  rankedOR.txt  unrankedAND.txt
			rankedAND.txt  unrankedStruct.txt  rankedStruct.txt) */
		file = new File("/CS/Semester1/Search Engine and Web Mining" +
				"/Homework1/rankedStruct.txt");
		output= new BufferedWriter(new FileWriter(file));
		//parse the query set downloaded from the web one line at a time
		while((parse = in.readLine()) != null){
			parse = parse.trim();
			if(parse.length() == 0)continue;
			else{
				index = 0;
				dilimiter = parse.indexOf(':');
				queryID = parse.substring(0, dilimiter);
				parse = parse.substring(dilimiter + 1);
				//Create a query tree to store the boolean query statement
				root = BuildQueryTree(parse);
				//Evaluate the query using depth-first, term-at-a-time strategy
				s = QueryEvaluation(root, stopword);
				//Sorting the retrieval results by their scores
				s.Sort();
				//Output the retrieval results to designated file
				while(index < s.Length() && index < 100){
					retrievalResult.delete(0, retrievalResult.length());
					retrievalResult.append(queryID + "  QO " + s.list.get(index).docID + "  " + (index + 1)
							+ "  " + s.list.get(index).score + "  " + "run-1\n");
					output.write(retrievalResult.toString());
					output.flush();
					index++;
				}
			}
		}
		output.close();
		in.close();
	}
	//Loading the stopwords from the disk into memory
	public static void LoadStopWords(HashMap<String, Integer>stopword)throws Exception{
		FileReader stwFile;
		BufferedReader input;
		String parse;
		stwFile = new FileReader("/CS/Semester1/Search Engine and Web Mining/Homework1/stopwords.txt");
		input = new BufferedReader(stwFile);
		while((parse = input.readLine()) != null){
			stopword.put(parse, 1);
		}
		input.close();
	}
	//The And Merge operation is the same for both unranked and ranked retrieval
	public static ScoreList AndMerge(ScoreList a, ScoreList b){
		ScoreList result = new ScoreList();
		int indexA, indexB;
		indexA = indexB = 0;
		//Sequentially traverse the score lists
		while(indexA < a.Length() && indexB < b.Length()){
			//Use the MIN function to combine the scores of the same documents from the query arguments
			if(a.list.get(indexA).docID == b.list.get(indexB).docID){
				result.AddDoc(a.list.get(indexA).docID, Math.min(a.list.get(indexA).score, 
					b.list.get(indexB).score));
				indexA++;
				indexB++;
			}else if(a.list.get(indexA).docID > b.list.get(indexB).docID){
				indexB++;
			}else{
				indexA++;
			}
		}
		return result;
	}
	//The Or Merge operation is the same for both unranked and ranked retrieval
	public static ScoreList OrMerge(ScoreList a, ScoreList b){
		ScoreList result = new ScoreList();
		int indexA, indexB;
		indexA = indexB = 0;
		//Sequentially traverse the score lists
		while(indexA < a.Length() || indexB < b.Length()){
			if(indexA < a.Length() && indexB < b.Length()){
				//Use the MAX function to combine the scores of the same document from query arguments
				if(a.list.get(indexA).docID == b.list.get(indexB).docID){
					result.AddDoc(a.list.get(indexA).docID, Math.max(a.list.get(indexA).score, 
							b.list.get(indexB).score));
					indexA++;
					indexB++;
				}else if(a.list.get(indexA).docID > b.list.get(indexB).docID){
					result.AddDoc(b.list.get(indexB).docID, b.list.get(indexB).score);
					indexB++;
				}else{
					result.AddDoc(a.list.get(indexA).docID, a.list.get(indexA).score);
					indexA++;
				}
			}else{
				if(indexA < a.Length()){
					result.AddDoc(a.list.get(indexA).docID, a.list.get(indexA).score);
					indexA++;
				}else{
					result.AddDoc(b.list.get(indexB).docID, b.list.get(indexB).score);
					indexB++;
				}
			}
		}
		return result;
	}
	//Compute the unranked score list of Node a based on its inverted list
	public static void GetUnrankedScoreList(Node a) throws Exception{
		File file;
		BufferedReader input;
		//Get the score list of a's title inverted list
		if(a.info.length() > 6 && a.info.substring(a.info.length() - 6, a.info.length()).equals(".title")){
			a.info = a.info.substring(0, a.info.length() - 6);
			file = new File("/CS/Semester1/Search Engine and Web Mining" +
					"/Homework1/TitleInvertedList/" + a.info + "Title.inv");
		}else{//Get the score list of a's body inverted list
			if(a.info.length() > 6 && a.info.substring(a.info.length() - 6, a.info.length()).equals(".body"))
				a.info = a.info.substring(0, a.info.length() - 5);
			file = new File("/CS/Semester1/Search Engine and Web Mining" +
					"/Homework1/BodyInvertedList/" + a.info + ".inv");
		}
		input = new BufferedReader(new FileReader(file));
		String line = null;
		String docID = null;
		input.readLine();
		input.readLine();
		int firstSpaceIndex = 0;
		a.sList = new ScoreList();
		/*parse the inverted list downloaded from the web each line at a time,
		  only get the document ID*/
		while((line = input.readLine()) != null){
			line = line.trim();
			if(line.length() == 0)continue;
			firstSpaceIndex = line.indexOf(' ');
			docID = line.substring(0, firstSpaceIndex);	
			a.sList.AddDoc(Integer.parseInt(docID), 1);
		}
		input.close();
	}
	//Compute the ranked score list of Node a based on its inverted list
	public static void GetRankedScoreList(Node a) throws Exception{
		File file;
		BufferedReader input;
		String line, docID, parse, tf;
		//Get the score list of a's title inverted list
		if(a.info.length() > 6 && a.info.substring(a.info.length() - 6, a.info.length()).equals(".title")){
			a.info = a.info.substring(0, a.info.length() - 6);
			file = new File("/CS/Semester1/Search Engine and Web Mining" +
					"/Homework1/TitleInvertedList/" + a.info + "Title.inv");
			input = new BufferedReader(new FileReader(file));
			line = docID  = tf = null;
			input.readLine();
			input.readLine();
			int firstSpaceIndex = 0;
			a.sList = new ScoreList();
			//extract the docID and tf from title inverted list.
			while((line = input.readLine()) != null){
				line = line.trim();
				if(line.length() == 0)continue;
				firstSpaceIndex = line.indexOf(' ');
				docID = line.substring(0, firstSpaceIndex);
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				while(line.charAt(firstSpaceIndex + 2) != ' '){
					line = line.substring(firstSpaceIndex);
					line = line.trim();
					firstSpaceIndex = line.indexOf(' ');
				}
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				tf = line.substring(0, firstSpaceIndex);
				a.sList.AddDoc(Integer.parseInt(docID), Integer.parseInt(tf));
				line = docID = tf = null;
			}
		}else{//Get the score list of a's body inverted list
			if(a.info.length() > 6 && a.info.substring(a.info.length() - 6, a.info.length()).equals(".body"))
				a.info = a.info.substring(0, a.info.length() - 5);
			file = new File("/CS/Semester1/Search Engine and Web Mining" +
					"/Homework1/BodyInvertedList/" + a.info + ".inv");
			input = new BufferedReader(new FileReader(file));
			line = docID = parse = tf = null;
			input.readLine();
			input.readLine();
			int firstSpaceIndex = 0;
			a.sList = new ScoreList();
			//tf of main body is always the third integer in each line.
			while((line = input.readLine()) != null){
				line = line.trim();
				if(line.length() == 0)continue;
				firstSpaceIndex = line.indexOf(' ');
				docID = line.substring(0, firstSpaceIndex);	
				parse = line.substring(firstSpaceIndex);
				parse = parse.trim();
				firstSpaceIndex = parse.indexOf(' ');
				parse = parse.substring(firstSpaceIndex);
				parse = parse.trim();
				firstSpaceIndex = parse.indexOf(' ');
				tf = parse.substring(0, firstSpaceIndex);
				a.sList.AddDoc(Integer.parseInt(docID), Integer.parseInt(tf));
				line = docID = parse = tf = null;
			}
		}
	}
	//Construct the query tree to store the query statement
	public static Node BuildQueryTree(String str) throws Exception{
		String parse, token;
		int index, base, space_index, space_base;
		char c;
		Stack stack;
		Node root, n, p;
		
		str = str.trim();
		parse = token = null;
		base = space_index = space_base = 0;
		c = '\0';
		stack = new Stack();
		root = n = p = null;
		//Construct query parsing tree
		for(index = 0; index < str.length(); index++){
			c = str.charAt(index);
			if(c != '(' && c != ')' && c != '#')continue;
			else{
				/*If encounter '(', then put the operator before '(' into the tree,
				 * let it be the child of the operator in top of the stack
				 * and then push it into the stack
				 */
				if(c == '('){
					parse = str.substring(base, index);
					parse = parse.trim();
					if(parse.length() == 0)continue;
					if(stack.isEmpty()){
						root = new Node(parse);
						stack.Push(root);
					}else{
						n = new Node(parse);
						p = stack.GetTop();
						if(p.HasChild()){
							p.LastChild().sibling = n;
						}else{
							p.child = n;
						}
						n.parent = p;
						if(parse.charAt(0) == '#')stack.Push(n);
					}
					base = index + 1;
				/*
				 * If encounter '#', check if some terms exists before '#'
				 * if so, then add them to the treek, let them be the children
				 * of the operator in top of the stack.
				 */
				}else if(c == '#'){
					parse = str.substring(base, index);
					parse = parse.trim();
					parse = parse.toLowerCase();
					if(parse.length() == 0)continue;
					if(stack.isEmpty()){
						//If a query has no explicit query operator, default to OR;
						root = new Node("#OR");
						p = root;
					}else{
						p = stack.GetTop();
					}
					space_base = space_index = 0;
					do{
						parse = parse.substring(space_base);
						space_base = 0;
						space_index = parse.indexOf(' ');
						if(space_index != -1){
							token = parse.substring(space_base, space_index);
						}else{
							token = parse.substring(space_base);
						}
						n = new Node(token);
						n.parent = p;
						if(p.HasChild()){
							p.LastChild().sibling = n;
						}else{
							p.child = n;
						}
						space_base = space_index + 1;
					}while(space_index != -1);
					base = index;
				/*
				 * If encounter ')', then pop the top operator in the stack
				 * add the terms before ')' to the tree, let them be the children
				 * of the poped operator.
				 */
				}else if(c == ')'){
					parse = str.substring(base, index);
					parse = parse.trim();
					parse = parse.toLowerCase();
					if(parse.length() == 0)continue;
					if(stack.isEmpty()){
						//If a query has no explicit query operator, default to OR;
						root = new Node("#OR");
						p = root;
					}else{
						p = stack.Pop();
					}
					space_base = space_index = 0;
					do{
						parse = parse.substring(space_base);
						space_base = 0;
						space_index = parse.indexOf(' ');
						if(space_index != -1){
							token = parse.substring(space_base, space_index);
						}else{
							token = parse.substring(space_base);
						}
						n = new Node(token);
						n.parent = p;
						if(p.HasChild()){
							p.LastChild().sibling = n;
						}else{
							p.child = n;
						}
						space_base = space_index + 1;
					}while(space_index != -1);
					base = index + 1;
				}
			}
		}
		//*Select*: If a query has no explicit query operator, default to OR/AND;
		if(root == null){
			root = new Node("#OR");
			p = root;
			space_base = space_index = 0;
			parse = str;
			parse = parse.trim();
			parse = parse.toLowerCase();
			do{
				parse = parse.substring(space_base);
				space_base = 0;
				space_index = parse.indexOf(' ');
				if(space_index != -1){
					token = parse.substring(space_base, space_index);
				}else{
					token = parse.substring(space_base);
				}
				n = new Node(token);
				n.parent = p;
				if(p.HasChild()){
					p.LastChild().sibling = n;
				}else{
					p.child = n;
				}
				space_base = space_index + 1;
			}while(space_index != -1);
		}
		//return the root of the query parsing tree
		return root;
	}
	//Evaluate the query in depth-first, term-at-a-time process strategy of the query parsing tree
	public static ScoreList QueryEvaluation(Node n, HashMap<String, Integer>stopwords) throws Exception{
		//Depth-first traversing of the tree
		if(n.HasChild()){
			QueryEvaluation(n.child, stopwords);
		}
		Node parent = n.parent;
		//If it's the root of the tree, then return the score list of the root node
		if(parent == null){
			return n.sList;
		}else{
			ScoreList result = new ScoreList();
			//Handling the #AND operation
			if(parent.info.equals("#AND")){
				Node term = parent.child;
				//Decide if it's a stopword
				while(IsStopWord(term.info, stopwords) && term != null)term = term.sibling;
				if(term == null){
					parent.sentinel = 1;
					parent.sList = result;
				}else{
					/*Select*: You can choose to get ranked or unranked score list of the word
					 * according the query requirement
					 */
					if(!term.HasScoreList())GetRankedScoreList(term);
					//if(!term.HasScoreList())GetUnrankedScoreList(term);
					result = term.sList;
					term.sentinel = 1;
					term = term.sibling;
					while(term != null){
						if(IsStopWord(term.info, stopwords)){
							term = term.sibling;
						}else{
							if(term.info.charAt(0) != '#'){
								/*Select*: You can choose to get ranked or unranked score list of the word
								 * according the query requirement
								 */
								if(!term.HasScoreList())GetRankedScoreList(term);
								//if(!term.HasScoreList())GetUnrankedScoreList(term);
								result = AndMerge(result, term.sList);
								term.sentinel = 1;
							}else{
								if(term.sentinel == 1){
								result = AndMerge(result, term.sList);
								}else{
									if(term.HasChild()){
										QueryEvaluation(term.child, stopwords);
									}
									result = AndMerge(result, term.sList);
								}
							}
							term = term.sibling;
						}
					}
					parent.sentinel = 1;
					parent.sList = result;
					return result;
				}
			//Handling the #OR operation
			}else if(parent.info.equals("#OR")){
				Node term = parent.child;
				//Determine if it's a stopword
				while(IsStopWord(term.info, stopwords) && term != null)term = term.sibling;
				if(term == null){
					parent.sentinel = 1;
					parent.sList = result;
				}else{
					/*Select*: You can choose to get ranked or unranked score list of the word
					 * according the query requirement
					 */
					if(!term.HasScoreList())GetRankedScoreList(term);
					//if(!term.HasScoreList())GetUnrankedScoreList(term);
					result = term.sList;		
					term.sentinel = 1;
					term = term.sibling;
					while(term != null){
						if(IsStopWord(term.info, stopwords)){
							term = term.sibling;
						}else{
							if(term.info.charAt(0) != '#'){
								/*Select*: You can choose to get ranked or unranked score list of the word
								 * according the query requirement
								 */
								if(!term.HasScoreList())GetRankedScoreList(term);
								//if(!term.HasScoreList())GetUnrankedScoreList(term);
								result = OrMerge(result, term.sList);						
								term.sentinel = 1;
							}else{
								if(term.sentinel == 1){
									result = OrMerge(result, term.sList);
								}else{
									if(term.HasChild()){
										QueryEvaluation(term.child, stopwords);
									}
									result = OrMerge(result, term.sList);
								}
							}
							term = term.sibling;
						}
					}
					parent.sentinel = 1;
					parent.sList = result;
					return result;
				}
			//Handling the #NEAR/n operation
			}else{//parent.info == "#NEAR/N"
				parent.info = parent.info.trim();
				int diff = Integer.parseInt(parent.info.substring(6, 7));
				Node term = parent.child;
				//Determine if it's a stopword
				while(IsStopWord(term.info, stopwords) && term != null)term = term.sibling;
				if(term == null){
					parent.sentinel = 1;
					parent.sList = result;
				}else{
					if(!term.HasInvList())BuildInvList(term);
					InvList resultInvList;
					resultInvList = term.iList;
					term = term.sibling;
					while(term != null){
						if(IsStopWord(term.info, stopwords)){
							term = term.sibling;
						}else{
							if(term.info.charAt(0) != '#'){
								if(!term.HasInvList())BuildInvList(term);
								resultInvList = RankedMergeInvList(resultInvList, term.iList, diff);
								term.sentinel = 1;
							}else{
								//program shouldn't run into this area unless the query's structure is wrong!
								System.out.println("Nonvalid boolean query!");
								return null;
							}
							term = term.sibling;
						}
					}
					result = ComputeScoreList(resultInvList);
				}
				parent.sList = result;
				parent.sentinel = 1;
				return result;
			}
			return result;
		}
	}
	//Build a new formated inverted list of Node a
	public static void BuildInvList(Node a) throws Exception{
		File file;
		BufferedReader input;
		String line, docID, parse, tf, pos;
		InvList invertedList;
		invertedList = new InvList();
		InvListEntry e;
		//Get the original title inverted list 
		if(a.info.length() > 6 && a.info.substring(a.info.length() - 6, a.info.length()).equals(".title")){
			a.info = a.info.substring(0, a.info.length() - 6);
			file = new File("/CS/Semester1/Search Engine and Web Mining" +
					"/Homework1/TitleInvertedList/" + a.info + "Title.inv");
			input = new BufferedReader(new FileReader(file));
			line = docID  = tf = pos = null;
			input.readLine();
			input.readLine();
			int firstSpaceIndex = 0;
			a.sList = new ScoreList();
			//extract the docID and tf from title inverted list.
			while((line = input.readLine()) != null){
				line = line.trim();
				if(line.length() == 0)continue;
				firstSpaceIndex = line.indexOf(' ');
				docID = line.substring(0, firstSpaceIndex);
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				while(line.charAt(firstSpaceIndex + 2) != ' '){
					line = line.substring(firstSpaceIndex);
					line = line.trim();
					firstSpaceIndex = line.indexOf(' ');
				}
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				line = line.substring(firstSpaceIndex);
				line = line.trim();
				firstSpaceIndex = line.indexOf(' ');
				tf = line.substring(0, firstSpaceIndex);
				
				parse = line;
				e = new InvListEntry(Integer.parseInt(docID), Integer.parseInt(tf));
				parse = parse.substring(firstSpaceIndex);
				parse = parse.trim();
				while((firstSpaceIndex = parse.indexOf(' ')) != -1){
					pos = parse.substring(0, firstSpaceIndex);
					e.AddPosition(Integer.parseInt(pos));
					parse = parse.substring(firstSpaceIndex);
					parse = parse.trim();
				}
				e.AddPosition(Integer.parseInt(parse));
				invertedList.AddEntry(e);
				line = docID = tf = pos = null;
			}
		}else{//Get the orginal body inverted list
			if(a.info.length() > 6 && a.info.substring(a.info.length() - 6, a.info.length()).equals(".body"))
				a.info = a.info.substring(0, a.info.length() - 5);
			file = new File("/CS/Semester1/Search Engine and Web Mining" +
					"/Homework1/BodyInvertedList/" + a.info + ".inv");
			input = new BufferedReader(new FileReader(file));
			line = docID = parse = tf = pos = null;
			input.readLine();
			input.readLine();
			int firstSpaceIndex = 0;
			a.sList = new ScoreList();
			//invertedList = new InvList();
			//tf of main body is always the third integer in each line.
			while((line = input.readLine()) != null){
				line = line.trim();
				if(line.length() == 0)continue;
				firstSpaceIndex = line.indexOf(' ');
				docID = line.substring(0, firstSpaceIndex);	
				parse = line.substring(firstSpaceIndex);
				parse = parse.trim();
				firstSpaceIndex = parse.indexOf(' ');
				parse = parse.substring(firstSpaceIndex);
				parse = parse.trim();
				firstSpaceIndex = parse.indexOf(' ');
				tf = parse.substring(0, firstSpaceIndex);
				
				e = new InvListEntry(Integer.parseInt(docID), Integer.parseInt(tf));
				parse = parse.substring(firstSpaceIndex);
				parse = parse.trim();
				while((firstSpaceIndex = parse.indexOf(' ')) != -1){
					pos = parse.substring(0, firstSpaceIndex);
					e.AddPosition(Integer.parseInt(pos));
					parse = parse.substring(firstSpaceIndex);
					parse = parse.trim();
				}
				e.AddPosition(Integer.parseInt(parse));
				invertedList.AddEntry(e);
				line = docID = parse = tf = pos = null;
			}
		}
		//invertedList.Output();
		a.iList = invertedList;
	}
	//Merge the inverted list of two words according to #NEAR/n operation, return the new ranked inverted list
	public static InvList RankedMergeInvList(InvList a, InvList b, int n){
		InvList result = new InvList();
		InvListEntry e;
		int indexA, indexB, posIndexA, posIndexB;
		indexA = indexB = posIndexA = posIndexB = 0;
		//Sequentially traverse the inverted lists
		while(indexB < b.Length()){
			while((indexA < a.Length()) && (a.list.get(indexA).docID < b.list.get(indexB).docID))indexA++;
			if(indexA >= a.Length())break;
			else if(a.list.get(indexA).docID == b.list.get(indexB).docID){
				e = new InvListEntry(a.list.get(indexA).docID, a.list.get(indexA).tf);
				posIndexA = posIndexB = 0;
				while(posIndexB < b.list.get(indexB).tf){
					//If satisfy the ordering and distance requirement, the add to the list
					while((posIndexA < a.list.get(indexA).tf) && 
							(b.list.get(indexB).position.get(posIndexB) -
									a.list.get(indexA).position.get(posIndexA) > n) ){
						posIndexA++;
					}
					if(posIndexA >= a.list.get(indexA).tf){
						break;
					}else if(b.list.get(indexB).position.get(posIndexB) -
							a.list.get(indexA).position.get(posIndexA) >= 0){
						e.AddPosition(b.list.get(indexB).position.get(posIndexB));
						posIndexA++;
						posIndexB++;
					}else{
						posIndexB++;
					}
				}
				if(e.position.size() != 0){
					e.tf = e.position.size();
					result.AddEntry(e);
				}
				indexA++;
				indexB++;
			}else{
				indexB++;
			}
		}
		return result;
	}
	//Merge the inverted list of two words according to #NEAR/n operation, return the new unranked inverted list
	public static InvList UnRankedMergeInvList(InvList a, InvList b, int n){
		InvList result = new InvList();
		InvListEntry e;
		int indexA, indexB, posIndexA, posIndexB;
		indexA = indexB = posIndexA = posIndexB = 0;
		//Sequentially traverse the inverted lists
		while(indexB < b.Length()){
			while((indexA < a.Length()) && (a.list.get(indexA).docID < b.list.get(indexB).docID))indexA++;
			if(indexA >= a.Length())break;
			else if(a.list.get(indexA).docID == b.list.get(indexB).docID){
				e = new InvListEntry(a.list.get(indexA).docID, a.list.get(indexA).tf);
				posIndexA = posIndexB = 0;
				//If satisfy the ordering and distance requirement, then add to the list
				while(posIndexB < b.list.get(indexB).tf){
					while((posIndexA < a.list.get(indexA).tf) && 
							(b.list.get(indexB).position.get(posIndexB) -
									a.list.get(indexA).position.get(posIndexA) > n) ){
						posIndexA++;
					}
					if(posIndexA >= a.list.get(indexA).tf){
						break;
					}else if(b.list.get(indexB).position.get(posIndexB) -
							a.list.get(indexA).position.get(posIndexA) >= 0){
						e.AddPosition(b.list.get(indexB).position.get(posIndexB));
						break;
						//posIndexA++;
						//posIndexB++;
					}else{
						posIndexB++;
					}
				}
				if(e.position.size() != 0){
					e.tf = e.position.size();
					result.AddEntry(e);
				}
				indexA++;
				indexB++;
			}else{
				indexB++;
			}
		}
		return result;
	}
	//Compute the score list of the given inverted list
 	public static ScoreList ComputeScoreList(InvList a){
		ScoreList result = new ScoreList();
		for(InvListEntry e: a.list){
			result.AddDoc(e.docID, e.tf);
		}
		return result;
	}
 	//Decide if the specified word is in the stopword list.
 	public static boolean IsStopWord(String word, HashMap<String, Integer>stopwords){
 		if(stopwords.containsKey(word))return true;
 		else		return false;
 	}
}
//Entry in the inverted list
class InvListEntry
{
	int docID;
	int tf;
	ArrayList<Integer>position;
	InvListEntry(int doc, int f){
		docID = doc;
		tf = f;
		position = new ArrayList<Integer>();
	}
	//Add a new position record to the entry
	public void AddPosition(int p){
		this.position.add(p);
	}
}
//Inverted list
class InvList
{
	ArrayList<InvListEntry>list;
	InvList(){
		list = new ArrayList<InvListEntry>();
	}
	//Add entry to the inverted list
	public void AddEntry(InvListEntry e){
		this.list.add(e);
	}
	//Delete entry from the inverted list
	public void DeleteEntry(int index){
		this.list.remove(index);
	}
	//Return the length of the inverted list
	public int Length(){
		return this.list.size();
	}
	//Print out the inverted list
	public void Output(){
		StringBuilder output = new StringBuilder();
		for(InvListEntry e: list){
			output.delete(0, output.length());
			output.append(e.docID + "   " + e.tf + "   " + e.position);
			System.out.println(output.toString());
		}
	}
}
//Node in the query parsing tree
class Node
{
	String info;
	Node child;
	Node sibling;
	Node parent;
	ScoreList sList;
	InvList iList;
	int sentinel = 0;
	Node(String s){
		info = s;
		child = null;
		sibling = null;
		parent = null;
		sList = null;
		iList = null;
		sentinel = 0;
	}
	//Determine if it has a child node
	boolean HasChild(){
		if(this.child != null)return true;
		else		return false;
	}
	//Determine if it has already built a inverted list
	boolean HasInvList(){
		if(this.iList != null)return true;
		else		return false;
	}
	//Determine if it has already build a score list
	boolean HasScoreList(){
		if(this.sList != null)return true;
		else		return false;
	}
	//Return the last adjacent child of the node
	Node LastChild(){
		if(!this.HasChild())return null;
		else{
			Node c = this.child;
			Node s = c.sibling;
			while(s != null){
				c = s;
				s = c.sibling;
			}
			return c;
		}
	}
}
//Entry in the score list
class DocScore
{
	int docID;
	int score;
	DocScore(int id, int sco){
		docID = id;
		score = sco;
	}
}
//Score list
class ScoreList
{
	ArrayList<DocScore> list;
	ScoreList(){
		list = new ArrayList<DocScore>();
	}
	//Add a new document and its corresponding score to the score list
	public void AddDoc(int docID, int score){
		DocScore doc = new DocScore(docID, score);
		list.add(doc);
	}
	//Printout the score list
	public void Printout(){
		for(DocScore d: list){
			System.out.println(d.docID + "  " + d.score);
		}
	}
	//Return the length of the score list
	public int Length(){
		return list.size();
	}
	/*Sort the score list in decreasing order of each entry's score.
	 * If two documents have the same score, then the one with small
	 * document ID should be listed in the front*/
	public void Sort(){
		DocScore[] a = new DocScore[this.list.size()];
		Arrays.sort(this.list.toArray(a), new DocScoreComparator());
		list.clear();
		int i;
		for(i = 0; i < a.length; i++)list.add(a[i]);
	}
}
//Overwriting the original Comparator in order to sort the score list.
class DocScoreComparator implements Comparator<DocScore>
{
	public int compare(DocScore s1, DocScore s2){
		if(s1.score > s2.score){
			return 0;
		}else if(s1.score == s2.score && s1.docID <= s2.docID){
			return 0;
		}else{
			return 1;
		}
	}
}
//stack used in building query parsing tree
class Stack
{
	Node[] stack;
	int top; //points to the first empty space above the top element in the stack
	Stack(){
		//Assume that at any time, there're no more than 100 elements in the stack
		stack = new Node[100]; 
		top = 0;
	}
	//Push a new node into the stack
	void Push(Node s){
		stack[top] = s;
		top++;
	}
	//Return and remove the top node of the stack
	Node Pop(){
		if(isEmpty())return null;
		else{
			top--;
			return stack[top];
		}
	}
	//Return but not remove the top node of the stack
	Node GetTop(){
		if(isEmpty())return null;
		else{
			return stack[top - 1];
		}
	}
	//Return the length of the stack
	int Length(){
		return top;
	}
	//Determine if the stack is empty
	boolean isEmpty(){
		if(top == 0)return true;
		else		return false;
	}
}
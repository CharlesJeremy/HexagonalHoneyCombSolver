import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;


/**
 * This is my implementation of a honeycomb solver.the problem seems innocnt enough at 
 * first sight but it is quite  hard to find a model to be an abstraction for a 
 * symmetrical hexagon and to set up the neighbors of each cell. 
 * The initializing of the gird has a time complexity of O(N) since I create every spot only once.
 * Solving also has time complexity of O(N) since it is recursive.
 * I had to make a trade off between space efficiency and speed and I had 
 * to be a little permissive with the space.I therefore had every thread have its own honeycomb 
 * structure.
 * I have never used java threads before therefore I had to read up on them before I implemented the project.
 * I realize that with big data, it wouldn't be advisable to spawn a new thread for every process and join them in 
 * sequence. I would have implemented a thread pool if I was more familiar with java threads.
 * It took me 7.5 hours in total to complete this project.
 * @author charlesmulemiMac
 *
 */

public class HoneyCombSolver {
	private static final int NUM_NEIGHBOURS = 6;
	
	private List<String> foundStrings;
	private List<String> candidates; 
	private int numLayers;
	private List<Spot> allSpots;
	
	public HoneyCombSolver(String puzzleFile, String toFind) {
		foundStrings = Collections.synchronizedList(new ArrayList<String>());
		
		List<String> allLetters = createList(puzzleFile);
		candidates = createList(toFind);
		
		numLayers = Integer.valueOf(allLetters.get(0));
		allLetters.remove(0);
		
		initializeSpots(allLetters);
	}
	
	/**
	 * Use multiple threads to solve each word and add it 
	 * to the array of solved strings
	 */
	public void  solve(){
		int i;
		int num = candidates.size();
		List<Worker> myWorkers = new ArrayList<Worker>(num);
		for(i = 0; i < num; i ++){
			Worker thread = new Worker(candidates.get(i));
			thread.start();
		}
		
		for(i = 0; i < num; i ++){
			try{
				myWorkers.get(i).join();
			}
			catch (Exception ignored) {}
		}
		
		
		Collections.sort(foundStrings);
		for(i = 0; i <  foundStrings.size(); i ++){
			System.out.println(foundStrings.get(i));
		}
	}
	
	/**
	 * Worker thread that determines whether a single word
	 * is in the 
	 */
	private class Worker extends Thread {
		private String toFind;
		private List<Spot> thisCopy;
		
		public Worker(String toSolve){
			toFind = toSolve;
			thisCopy = deepCopy();
		}
	
		public void run() {
			
			for(int i = 0; i < thisCopy.size(); i ++){
				Spot toAttempt = thisCopy.get(i);
				if(toAttempt.solveSpot(toFind, toAttempt)){
					foundStrings.add(toFind);
					break;
				}
			 }
		}
	}
		

	/**
	 * Initializes the honeycomb.This was very challenging
	 * @param honeyComb The list of strings to form the honeycomb
	 * The honeycomb is stored in a global array
	 */
	private void initializeSpots(List<String> honeyComb){
		int i, j;
		allSpots = Arrays.asList(new Spot[(3 * numLayers) * (numLayers - 1) + 1]);//formula for symmetric hexagon
		
		//create the spots
		for(i = 1; i <= numLayers; i ++){
			
			String thisLevel =  honeyComb.get(i - 1);
			
			//Hexagonal symmetry follows the following formula
			int upperIndex = ((3 * i) * (i - 1) + 1);
			int lowerIndex = (i == 1) ? (0) : ((3 * (i - 1)) * ((i - 1) - 1)) + 1;
			
			int len = thisLevel.length();
			int hasTrio = 0;
			Map<Integer, Integer> lastUpperNbr = new HashMap<Integer, Integer>();
			
			for(j = 0; j < len; j ++){
				Spot newSpot = findSpot(thisLevel.charAt(j), lowerIndex + j);
			
				//set upper neighbours
				if(i != numLayers){//last layer has no upper neighbors
					String upperNbr = honeyComb.get(i);
					int upperLen = upperNbr.length() - 1;
								
					//This is the most difficult part of the problem
					if(j == 0){//first character should have three upper neighbors
						makeNeighbour(newSpot, findSpot(upperNbr.charAt(upperLen), upperIndex + upperLen));
						makeNeighbour(newSpot, findSpot(upperNbr.charAt(0), upperIndex));
						makeNeighbour(newSpot, findSpot(upperNbr.charAt(1), upperIndex + 1));
						lastUpperNbr.put(j, 1);
					}
						
					//middle character should have three upper neighbours.This case is necessary so that the first layer can work
					if(len / 2 == j){ 
						int upperMid = upperNbr.length() / 2;
						makeNeighbour(newSpot, findSpot(upperNbr.charAt(upperMid), upperIndex + upperMid));
						makeNeighbour(newSpot, findSpot(upperNbr.charAt(upperMid + 1), upperIndex + upperMid + 1));
						makeNeighbour(newSpot, findSpot(upperNbr.charAt(upperMid - 1), upperIndex + upperMid - 1));	
						lastUpperNbr.put(j,  upperMid + 1);
					}
						
					if(j != 0 && len / 2 != j){
					     int startIndex = lastUpperNbr.get(j - 1);
					     makeNeighbour(newSpot, findSpot(upperNbr.charAt(startIndex), upperIndex + startIndex));
					     makeNeighbour(newSpot, findSpot(upperNbr.charAt(startIndex + 1), upperIndex + startIndex + 1));
						 if(j == hasTrio){
							 makeNeighbour(newSpot, findSpot(upperNbr.charAt(startIndex + 2), upperIndex + startIndex + 2));
						 }
						 lastUpperNbr.put(j, (j == hasTrio) ? (startIndex + 2) : (startIndex + 1));
					}
				}
				if(i > 1){
					if((j + 1) % (i - 1) == 0)hasTrio += (i - 1);
				}
			}
			
			if(i > 1){
				for(j = 1; j < len; j ++){
					makeNeighbour(allSpots.get(lowerIndex + (j - 1)), allSpots.get(lowerIndex + j));
				}
				makeNeighbour(allSpots.get(lowerIndex), allSpots.get(lowerIndex + len -1));
			}	
		}
	}
	
	/**
	 * Sets two spots to be neoghbours
	 * @param a first spot
	 * @param b second spot
	 */
	private void makeNeighbour(Spot a, Spot b){
		a.addNeighbour(b);
		b.addNeighbour(a);
	}
	
	/**
	 * deep copy so that each thread has an uncorrupted honeycomb
	 */
	private List<Spot> deepCopy(){
		int len = allSpots.size();
		List<Spot> result =  Arrays.asList(new Spot[len]);
		for(int i = 0; i < len; i ++){
			Spot oldSpot = allSpots.get(i);
			Spot newSpot = new Spot(oldSpot.getValue(), i);
			result.set(i, newSpot);
			int numNbr = oldSpot.getNumNeighbours();
			for(int j = 0; j < numNbr; j ++){
				Spot thisNbr = result.get(oldSpot.getNeighbour(j).getIndex());
				if(thisNbr != null){
					makeNeighbour(newSpot, thisNbr);
				}
			}
		}
		return result;
	}
	
	/**
	 * Ensures a spot has not been initialized already before attempting to 
	 * create it
	 * @param value character value of the new spot
	 * @param index index into the honeycomb array
	 * @return
	 */
	private Spot findSpot(char value, int index){
		Spot result = allSpots.get(index);
		if(result == null){
			result = new Spot(value, index);
			allSpots.set(index, result);//last
		}
		return result;
	}
	
	
	
	/**
	 * Creates a new List and reads words to be found from a specified file and fills
     * the List with these words.
     * @return The List containing the words from the file.
     */
    private List<String> createList (String fileName){
        List <String> newArray = new ArrayList<String>();
        BufferedReader myReader = null;
        String word = "";
        try{
            myReader = new BufferedReader(new FileReader(fileName));
            while(true){
                word = myReader.readLine();
                if (word == null)break;
                newArray.add(word.trim());
            }
            myReader.close();
        }catch(Exception e){
        	System.err.println("File reading exception: " + e.getMessage());
        }
        return newArray;
    }
		
    /**
     * Private class that represents a single spot on the honeycomb.
     * Stores its status variables which are its character, index and 
     * whether it has been visited
     */
	private class Spot{
		private char value;
		private int index;
		private boolean used;
		private List<Spot> neighbours;
		
		public Spot(char newValue, int myIndex){
			neighbours = new ArrayList<Spot>();
			used = false;
			this.value = newValue;
			this.index = myIndex;
		}
		
		public char getValue(){
			return value;
		}
		
		public boolean getUsed(){
			return used;
		}	
		
		public void setUsed(boolean flag){
			used = flag;
		}
		
		public int getIndex(){
			return index;
		}
		
		public int getNumNeighbours(){
			return neighbours.size();
		}
		
		
		public void addNeighbour(Spot newNbr){
			if(getNumNeighbours() != NUM_NEIGHBOURS){
				neighbours.add(newNbr);
			}else{
				throw new RuntimeException("Attempt to add more than" + NUM_NEIGHBOURS + "neighbours");
			}
			
		}
		
		public Spot getNeighbour(int index){
			return neighbours.get(index);
		}
		
		/**
		 * Recursively finds a solution
		 * @param toSolve The string to search 
		 * @param toCheck The spot to look in next.
		 * @return
		 */
		private boolean solveSpot(String toSolve, Spot toCheck){
			if(toSolve.isEmpty())return true;
			if(toCheck == null || toSolve.charAt(0) != toCheck.getValue() || toCheck.getUsed())return false;
			for(int i = 0; i < toCheck.getNumNeighbours(); i ++){
			    //trying
				toCheck.setUsed(true);
				
				if(solveSpot(toSolve.substring(1), toCheck.getNeighbour(i)))return true;
				
				//backtracking
				toCheck.setUsed(false);
			}
			return false;
		}
	}
	
	/**
	 * Main takes a two arguments from the console
	 * @param args
	 */
	public static void main(String[] args){
		HoneyCombSolver mySolver = new HoneyCombSolver(args[0], args[1]);
		mySolver.solve();
	}

}

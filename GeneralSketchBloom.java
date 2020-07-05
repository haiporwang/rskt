package minusHLL;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * for SIGCOMM2018 sketchBlm
 * changes based on CountMin: 
 * 1) one array
 * 2) change in initialization of w (w is the number of basic data structures in each segment, w / m)
 * 3) same encode and estimate
 * @author Youlin
 */
/** IMPORTANT NOTES
The advantages of this noise canceling work over virtual HyperLogLog is online query, we need 128 registers queries
However, for vHLL, it needs to query all the registers for noise calculation.
*/
public class GeneralSketchBloom {
	public static Random rand = new Random();

	public static int n = 0; 						// total number of packets
	public static int flows = 0; 					// total number of flows
	public static int avgAccess = 0; 				// average memory access for each packet
	public static  int M = 1024 * 1024* 8; 	// total memory space Mbits	
	public static GeneralDataStructure[][] C;
	public static Set<Integer> sizeMeasurementConfig = new HashSet<>(Arrays.asList()); // -1-regular CM; 0-enhanced CM; 1-Bitmap; 2-FM sketch; 3-HLL sketch
	public static Set<Integer> spreadMeasurementConfig = new HashSet<>(Arrays.asList(1)); // 1-Bitmap; 2-FM sketch; 3-HLL sketch
	public static Set<Integer> expConfig = new HashSet<>(Arrays.asList()); //0-ECountMin dist exp
	public static boolean isGetThroughput = false;

	/** parameters for count-min */
	public static final int d = 1; 			// the number of rows in Count Min
	public static int w = 1;				// the number of columns in Count Min
	public static int u = 1;				// the size of each elementary data structure in Count Min.
	public static int[] S = new int[d];		// random seeds for Count Min
	public static int m = 1;				// number of bit/register in each unit (used for bitmap, FM sketch and HLL sketch)


	/** parameters for counter */
	public static int mValueCounter = 1;			// only one counter in the counter data structure
	public static int counterSize = 32;				// size of each unit

	/** parameters for bitmap */
	public static  int bitArrayLength = 5000;

	/** parameters for FM sketch **/
	public static int mValueFM = 128;
	public static  int FMsketchSize = 32;

	/** parameters for HLL sketch **/
	public static int mValueHLL = 128;
	public static  int HLLSize = 5;

	public static int times = 1;
	
	/** number of runs for throughput measurement */
	public static int loops = 50;
	public static int Mbase=1024*1024;
	public static int[][] Marray= {{},{1,2,4,8}};
	public static int[][] mValueCounterarray= {{1},{1}};
	public static int[][] bitArrayLengtharray= {{50000},{5000}};
	public static int[][] mValueFMarray= {{128},{128}};
	public static int[][] mValueHLLarray= {{128},{32,64,256}};
	public static int[] SHLL;	
	public static void main(String[] args) throws FileNotFoundException {
		/** measurement for flow sizes **/
		if (isGetThroughput) {
			getThroughput();
			return;
		}
		System.out.println("Start****************************");
		
		
		for (int i : sizeMeasurementConfig) {
			for(int i1=0;i1<Marray[0].length;i1++) {
				M=Marray[0][i1]*Mbase;
				switch (i) {
				case 0:
				   for(int j=0;j<mValueCounterarray[0].length;j++) {
					   mValueCounter=mValueCounterarray[0][j];
					   initCM(i);
						encodeSize(GeneralUtil.dataStreamForFlowSize);
			        	estimateSize(GeneralUtil.dataSummaryForFlowSize);
				   }
				   break;
				case 1:
					for(int j=0;j<bitArrayLengtharray[0].length;j++) {
						bitArrayLength=bitArrayLengtharray[0][j];
						initCM(i);
						encodeSize(GeneralUtil.dataStreamForFlowSize);
			        	estimateSize(GeneralUtil.dataSummaryForFlowSize);
					}
					break;
				case 2:	
					for(int j=0;j<mValueFMarray[0].length;j++) {
						mValueFM=mValueFMarray[0][j];
						initCM(i);
						encodeSize(GeneralUtil.dataStreamForFlowSize);
			        	estimateSize(GeneralUtil.dataSummaryForFlowSize);
					}
					break;
				case 3:
					for(int j=0;j<mValueHLLarray[0].length;j++) {			
						mValueHLL=mValueHLLarray[0][j];
						initCM(i);
						encodeSize(GeneralUtil.dataStreamForFlowSize);
			        	estimateSize(GeneralUtil.dataSummaryForFlowSize);
					}
					break;
				default:break;
				}
			}
		}
		
		/** measurment for flow spreads **/
		System.out.println("--------------11----------------");

		for (int i : spreadMeasurementConfig) {
			for(int i1=0;i1<Marray[1].length;i1++) {
				M=Marray[1][i1]*Mbase;
				switch (i) {
				case 1:
					for(int j=0;j<bitArrayLengtharray[1].length;j++) {
						bitArrayLength=bitArrayLengtharray[1][j];
						initCM(i);
						encodeSpread(GeneralUtil.dataStreamForFlowSpread);
			    		estimateSpread(GeneralUtil.dataSummaryForFlowSpread);
					}
					break;
				case 2:	
					for(int j=0;j<mValueFMarray[1].length;j++) {
						mValueFM=mValueFMarray[1][j];
						initCM(i);
						encodeSpread(GeneralUtil.dataStreamForFlowSpread);
			    		estimateSpread(GeneralUtil.dataSummaryForFlowSpread);
					}
					break;
				case 3:
					for(int j=0;j<mValueHLLarray[1].length;j++) {			
						mValueHLL=mValueHLLarray[1][j];
						System.out.print("--------22----------------------");

						initCM(i);
						encodeSpread(GeneralUtil.dataStreamForFlowSpread);
			    		estimateSpread(GeneralUtil.dataSummaryForFlowSpread);
					}
					break;
				default:break;
				}
			}
			
		}
	
		
		/** measurement for flow sizes **/
		/*for (int i : sizeMeasurementConfig) {
			int time = 0;
			for(int j = 0; j < times; j++) {
				initCM(i);
				encodeSize(GeneralUtil.dataStreamForFlowSize);
	        	estimateSize(GeneralUtil.dataSummaryForFlowSize);
	        	time++;
			}
		}
      */
		/** measurement for flow spreads **/
		/*
		 * for (int i : spreadMeasurementConfig) {
			initCM(i);
			encodeSpread(GeneralUtil.dataStreamForFlowSpread);
    		estimateSpread(GeneralUtil.dataSummaryForFlowSpread);
		}
		 */
		/** experiment for specific requirement *
		for (int i : expConfig) {
			switch (i) {
	        case 0:  initCM(0);
					 encodeSize(GeneralUtil.dataStreamForFlowSize);
					 randomEstimate(10000000);
	                 break;
	        default: break;
			}
		}*/
		System.out.println("DONE!****************************");
	}

	// Generate counter base Counter Bloom for flow size measurement.
	public static Counter[][] generateCounter() {
		m = mValueCounter;
		u = counterSize * mValueCounter;
		w = (M / u);
		Counter[][] B = new Counter[1][w];
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < w; j++) {
				B[i][j] = new Counter(1, counterSize);
			}
		}
		return B;
	}

	// Generate bitmap base Bitmap Bloom for flow cardinality measurement.
	public static Bitmap[][] generateBitmap() {
		m = bitArrayLength*2;
		u = bitArrayLength*2;
		w = (M / u) / 1;
		Bitmap[][] B = new Bitmap[1][w];
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < w; j++) {
				B[i][j] = new Bitmap(bitArrayLength*2);
			}
		}
		HashSet<Integer> seeds = new HashSet<Integer>();
		int num = bitArrayLength;
		SHLL = new int[num];
		System.out.println("the num is "+num);
		while (num > 0) {
			int s = rand.nextInt();
			if (!seeds.contains(s)) {
				num--;
				SHLL[num] = s;
				seeds.add(s);
			}
		}
		return B;
	}

	// Generate FM sketch base FMsketch Bloom for flow cardinality measurement.
	public static FMsketch[][] generateFMsketch() {
		m = mValueFM*2;
		u = FMsketchSize * mValueFM*2;
		w = (M / u) / 1;
		FMsketch[][] B = new FMsketch[1][w];
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < w; j++) {
				B[i][j] = new FMsketch(mValueFM*2, FMsketchSize);
			}
		}
		HashSet<Integer> seeds = new HashSet<Integer>();
		int num = mValueFM;
		SHLL = new int[num];

		System.out.println("the num is "+num);
		while (num > 0) {
			int s = rand.nextInt();
			if (!seeds.contains(s)) {
				num--;
				SHLL[num] = s;
				seeds.add(s);
			}
		}
		return B;
	}

	// Generate HLL sketch base HLL Bloom for flow cardinality measurement.
	public static HyperLogLog[][] generateHyperLogLog() {
		m = mValueHLL*2;
		u = HLLSize * mValueHLL*2;
		w = (M / u) / 1;
		HyperLogLog[][] B = new HyperLogLog[1][w];
		for (int i = 0; i < 1; i++) {
			for (int j = 0; j < w; j++) {
				B[i][j] = new HyperLogLog(2*mValueHLL, HLLSize);
			}
		}
		HashSet<Integer> seeds = new HashSet<Integer>();

		int num =mValueHLL ;
		SHLL = new int[num];

		System.out.println("the num is "+num);
		while (num > 0) {
			int s = rand.nextInt();
			if (!seeds.contains(s)) {
				num--;
				SHLL[num] = s;
				seeds.add(s);
			}
		}
		return B;
	}

	// Init the Count Min for different elementary data structures.
	public static void initCM(int index) {
		switch (index) {
		case 0: case -1: C = generateCounter();
		break;
		case 1:  C = generateBitmap();
		break;
		case 2:  C = generateFMsketch();
		break;
		case 3:  C = generateHyperLogLog();
		break;
		default: break;
		}
		generateCMRandomSeeds();
		System.out.println("\nSketchBloom-" + C[0][0].getDataStructureName() + " Initialized!-----------");
	}
	
	// Generate random seeds for Counter Min.
	public static void generateCMRandomSeeds() {
		HashSet<Integer> seeds = new HashSet<Integer>();
		int num = d;
		while (num > 0) {
			int s = rand.nextInt();
			if (!seeds.contains(s)) {
				num--;
				S[num] = s;
				seeds.add(s);
			}
		}
		
	}

	/** Encode elements to the Count Min for flow size measurement. */
	public static void encodeSize(String filePath) throws FileNotFoundException {
		System.out.println("Encoding elements using " + C[0][0].getDataStructureName().toUpperCase() + "s for flow size measurement......" );
		Scanner sc = new Scanner(new File(filePath));
		n = 0;

		while (sc.hasNextLine()) {
			String entry = sc.nextLine();
			String[] strs = entry.split("\\s+");
			String flowid = GeneralUtil.getSizeFlowID(strs, true);
			n++;

			if (C[0][0].getDataStructureName().equals("Counter")) {
				int minVal = Integer.MAX_VALUE;
				int[] tempS = new int[1];
				
				for (int i = 0; i < d; i++) {
					tempS[0] = S[i];
					int j = (GeneralUtil.intHash(GeneralUtil.FNVHash1(flowid) ^ S[i]) % w + w) % w; 

					C[0][j].encode(flowid,tempS);
				}
			} else {
				for (int i = 0; i < d; i++) {
					int j = (GeneralUtil.intHash(GeneralUtil.FNVHash1(flowid) ^ S[i]) % w + w) % w; 
					C[0][j].encode(flowid, SHLL);
				}
			}
		}
		System.out.println("Total number of encoded pakcets: " + n);
		sc.close();
	}
    
	public static double getnoise() {
		double res=0.0;
		Random ran=new Random();
		for(int i=0;i<w;i++) {
			int estimate = Integer.MAX_VALUE;
			//int r=ran.nextInt();
			for(int j=0;j<d;j++) {				
				int p=((ran.nextInt()^S[j])%w+w)%w;
				estimate = Math.min(estimate, C[0][p].getValue());
			}
			res+=(double)estimate;
		}
		return res/(double)w;
	}
	
	public static double getnoise1() {
		double res=0.0;
		Random ran=new Random();
		for(int i=0;i<w;i++) {
			res+=(double)C[0][i].getValue();
		}
		return res/w;
	}
	
	/** Estimate flow sizes. */
	public static void estimateSize(String filePath) throws FileNotFoundException {
		System.out.println("Estimating Flow SIZEs..." ); 
		Scanner sc = new Scanner(new File(filePath));
		String resultFilePath = GeneralUtil.path + "SketchBloom\\size\\+-" + C[0][0].getDataStructureName()
				+ "_M_" +  M / 1024 / 1024 + "_d_" + d + "_u_" + u + "_m_" + m + "_T_" + times;
		PrintWriter pw = new PrintWriter(new File(resultFilePath));
		System.out.println("w :" + w);
		System.out.println("Result directory: " + resultFilePath); 
		double noise=getnoise();
		//double noise=(double)18215144*(double)d/(double)w;
		while (sc.hasNextLine()) {
			String entry = sc.nextLine();
			String[] strs = entry.split("\\s+");
			String flowid = GeneralUtil.getSizeFlowID(strs, false);
			int num = Integer.parseInt(strs[strs.length-1]);

			//if (rand.nextDouble() <= GeneralUtil.getSizeSampleRate(num)) {
				int estimate = Integer.MAX_VALUE;
				int [] value =new int[d];
				for(int i = 0; i < d; i++) {
					int [] tempS = new int[1];
					int j = (GeneralUtil.intHash(GeneralUtil.FNVHash1(flowid) ^ S[i]) % w + w) % w;
					value[i] =C[0][j].getValue(flowid, tempS);
					//if (value[i]>130000) System.out.println("value[i] is "+value[i]);
					//estimate = Math.min(estimate, C[0][j].getValue(flowid, SHLL));
				}
				Arrays.sort(value);
				estimate = (d%2)==1?(value[(d-1)/2]):(value[d/2]+value[d/2-1])/2;
				//estimate-=(int) noise;
				if(estimate<=0) estimate=1;
				pw.println(entry + "\t" + estimate);
			//}
		}
		sc.close();
		pw.close();
		// obtain estimation accuracy results
		GeneralUtil.analyzeAccuracy(resultFilePath);
	}
	
	/** Encode elements to the Count Min for flow spread measurement. */
	public static void encodeSpread(String filePath) throws FileNotFoundException {
		System.out.println("Encoding elements using " + C[0][0].getDataStructureName().toUpperCase() + "s for flow spread measurement......" );
		Scanner sc = new Scanner(new File(filePath));
		n = 0;
		
		while (sc.hasNextLine()) {
			String entry = sc.nextLine();
			String[] strs = entry.split("\\s+");
			String[] res = GeneralUtil.getSperadFlowIDAndElementID(strs, true);
			String flowid = res[0];
			String elementid = res[1];
			n++;
			for (int i = 0; i < d; i++) {
				int j = (GeneralUtil.intHash(GeneralUtil.FNVHash1(flowid) ^ S[i]) % w + w) % w;
				//if (n==1)System.out.println("----ggggg--------------------------");

				C[0][j].encode(flowid,elementid,SHLL);
			}
		}
		System.out.println("Total number of encoded pakcets: " + n); 
		sc.close();
	}

	/** Estimate flow spreads. */
	public static void estimateSpread(String filepath) throws FileNotFoundException {
		System.out.println("Estimating Flow CARDINALITY..." ); 
		Scanner sc = new Scanner(new File(filepath));
		String resultFilePath = GeneralUtil.path + "SketchBloom\\spread\\+-+rSkt2_"+ C[0][0].getDataStructureName()
				+ "_M_" +  M / 1024 / 1024 + "_d_" + d + "_u_" + u + "_m_" + m;
		PrintWriter pw = new PrintWriter(new File(resultFilePath));
		System.out.println("Result directory: " + resultFilePath); 
		double noise=0.0;//getnoise();
		while (sc.hasNextLine()) {
			String entry = sc.nextLine();
			String[] strs = entry.split("\\s+");
			String flowid = GeneralUtil.getSperadFlowIDAndElementID(strs, false)[0];
			int num = Integer.parseInt(strs[strs.length-1]);
			
			//if (rand.nextDouble() <= GeneralUtil.getSpreadSampleRate(num)) {
				int estimate = Integer.MAX_VALUE;
				int [] value = new int[d];
				for(int i = 0; i < d; i++) {
					int j = (GeneralUtil.intHash(GeneralUtil.FNVHash1(flowid) ^ S[i]) % w + w) % w;
					value[i]= C[0][j].getValue(flowid,SHLL);
				}
				Arrays.sort(value);
				estimate = d%2==1?value[(d-1)/2]:(int)((value[d/2]+value[d/2-1])/2);
				estimate-=(int) noise;
				if(estimate<=0) estimate=1;
				pw.println(entry + "\t" + estimate);
			//}
		}
		sc.close();
		pw.close();
		// obtain estimation accuracy results
		GeneralUtil.analyzeAccuracy(resultFilePath);
	}
	

	public static void getThroughput() throws FileNotFoundException {
		Scanner sc = new Scanner(new File(GeneralUtil.dataStreamForFlowThroughput));
		ArrayList<Integer> dataFlowID = new ArrayList<Integer> ();
		ArrayList<Integer> dataElemID = new ArrayList<Integer> ();
		n = 0;
		if (sizeMeasurementConfig.size() > 0) {
			while (sc.hasNextLine()) {
				String entry = sc.nextLine();				
				String[] strs = entry.split("\\s+");
				dataFlowID.add(GeneralUtil.FNVHash1(entry));
				dataElemID.add(GeneralUtil.FNVHash1(strs[1]));
				n++;
			}
			sc.close();
		} else {
			while (sc.hasNextLine()) {
				String entry = sc.nextLine();				
				String[] strs = entry.split("\\s+");
				dataFlowID.add(GeneralUtil.FNVHash1(strs[0]));
				dataElemID.add(GeneralUtil.FNVHash1(strs[1]));
				n++;
			}
			sc.close();
		}
		System.out.println("total number of packets: " + n);
		
		/** measurement for flow sizes **/
		for (int i : sizeMeasurementConfig) {
			tpForSize(i, dataFlowID, dataElemID);
		}
		
		/** measurement for flow spreads **/
		for (int i : spreadMeasurementConfig) {
			tpForSpread(i, dataFlowID, dataElemID);
		}
	}
	
	
	/** Get throughput for flow size measurement. */
	public static void tpForSize(int SketchBlm, ArrayList<Integer> dataFlowID, ArrayList<Integer> dataElemID) throws FileNotFoundException {
		int totalNum = dataFlowID.size();
		initCM(SketchBlm);
		String resultFilePath = GeneralUtil.path + "Throughput\\SketchBloom_size_" + C[0][0].getDataStructureName()
				+ "_M_" +  M / 1024 / 1024 + "_d_" + d + "_u_" + u + "_m_" + m + "_tp_" + GeneralUtil.throughputSamplingRate;
		PrintWriter pw = new PrintWriter(new File(resultFilePath));
		Double res = 0.0;
		
		if (SketchBlm == 0) { // for enhanced countmin
			double duration = 0;
			
			for (int i = 0; i < loops; i++) {
				initCM(SketchBlm);
				int[] arrIndex = new int[d];
				int[] arrVal = new int[d];
				
				long startTime = System.nanoTime();
				for (int j = 0; j < totalNum; j++) {
					//if (rand.nextDouble() <= GeneralUtil.throughputSamplingRate) {
						int minVal = Integer.MAX_VALUE;
		                for (int k = 0; k < d; k++) {
		                    int jj = (GeneralUtil.intHash(dataFlowID.get(j) ^ S[k]) % w + w) % w;
		                    arrIndex[k] = jj;
		                    arrVal[k] = C[0][jj].getValue();
		                    minVal = Math.min(minVal, arrVal[k]);
		                }
		                
		                for (int k = 0; k < d; k++) {
				            if (arrVal[k] == minVal) {
				            	C[0][arrIndex[k]].encode(dataElemID.get(j));           
				            }
		                }
					//}
				}
				long endTime = System.nanoTime();
				duration += 1.0 * (endTime - startTime) / 1000000000;
			}
			res = 1.0 * totalNum / (duration / loops);
			//System.out.println("Average execution time: " + 1.0 * duration / loops + " seconds");
			System.out.println(C[0][0].getDataStructureName() + "\t Average Throughput: " + 1.0 * totalNum / (duration / loops) + " packets/second" );
		} else {
			double duration = 0;

			
			for (int i = 0; i < loops; i++) {
				initCM(SketchBlm);
				long startTime = System.nanoTime();
				for (int j = 0; j < totalNum; j++) {
					//if (rand.nextDouble() <= GeneralUtil.throughputSamplingRate) {
		                for (int k = 0; k < d; k++) {
		                	C[0][(GeneralUtil.intHash(dataFlowID.get(j) ^ S[k]) % w + w) % w].encode();
		                }
					//}
				}
				long endTime = System.nanoTime();
				duration += 1.0 * (endTime - startTime) / 1000000000;
			}
			
			res = 1.0 * totalNum / (duration / loops);
			//System.out.println("Average execution time: " + 1.0 * duration / loops + " seconds");
			System.out.println(C[0][0].getDataStructureName() + "\t Average Throughput: " + 1.0 * totalNum / (duration / loops) + " packets/second" );
		}
		pw.println(res.intValue());
		pw.close();
	}
	
	/** Get throughput for flow spread measurement. */
	public static void tpForSpread(int SketchBlm, ArrayList<Integer> dataFlowID, ArrayList<Integer> dataElemID) throws FileNotFoundException {
		int totalNum = dataFlowID.size();
		initCM(SketchBlm);
		String resultFilePath = GeneralUtil.path + "Throughput\\SketchBloom_spread_" + C[0][0].getDataStructureName()
				+ "_M_" +  M / 1024 / 1024 + "_d_" + d + "_u_" + u + "_m_" + m + "_tp_" + GeneralUtil.throughputSamplingRate;
		PrintWriter pw = new PrintWriter(new File(resultFilePath));
		Double res = 0.0;
		
		double duration = 0;
		totalNum = 100;
		for (int i = 0; i < loops; i++) {
			initCM(SketchBlm);
			long startTime = System.nanoTime();
			for (int j = 0; j < totalNum; j++) {
				int estimate = Integer.MAX_VALUE;
				int [] value = new int[d];
				for (int k = 0; k < d; k++) {
						int lll = (GeneralUtil.intHash(GeneralUtil.FNVHash1(dataFlowID.get(j)) ^ S[k]) % w + w) % w;
						value[k]= C[0][lll].getValue(dataFlowID.get(j),SHLL);
					//int ll = (GeneralUtil.intHash(GeneralUtil.FNVHash1(dataElemID.get(j)) ^ S[k]) % w + w) % w;
					//C[0][ll].encode(dataElemID.get(j),SHLL);

					}
					//Arrays.sort(value);
					//estimate = d%2==1?value[(d-1)/2]:(int)((value[d/2]+value[d/2-1])/2);
					if(estimate<=0) estimate=1;
                
			}	
			long endTime = System.nanoTime();
			duration += 1.0 * (endTime - startTime) / 1000000000;
		}
		res = 1.0 * totalNum / (duration / loops);
		//System.out.println("Average execution time: " + 1.0 * duration / loops + " seconds");
		System.out.println(C[0][0].getDataStructureName() + "\t Average Throughput: " + 1.0 * totalNum / (duration / loops) + " packets/second" );
		pw.println(res.intValue());
		pw.close();
	}
}

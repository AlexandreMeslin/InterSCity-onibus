/**
 * 
 */
package br.com.meslin.onibus.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * @author meslin
 *
 */
public class Join2Regions {
	private static final double MAX_DISTANCE = 0.01;
	private static String regionA;
	private static String regionB;
	private static String outputfile;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 3) {
			System.err.println("Usage: java -classpath .:/media/meslin/643CA9553CA92352/Program Files/Java/ContextNet/contextnet-2.5.jar:/media/meslin/643CA9553CA92352/Program Files/Java/ContextNet/udilib.jar:/media/meslin/643CA9553CA92352/Program Files/Java/JMapViewer/JMapViewer.jar:/media/meslin/643CA9553CA92352/Program Files/Java/JSON/JSON-Parser/json-20160810.jar br.com.meslin.onibus.main.Join2Regions <region A> <region B> <outputfile>");
			return;
		}
		
		regionA = args[0];
		regionB = args[1];
		outputfile = args[2];
		System.out.println("Adding " + regionA + " to " +  regionB + " forming " + outputfile);

		Join2Regions join2Regions = new Join2Regions();
		join2Regions.doAll();
		System.out.println();
	}

	private void doAll() {
		Vector<Vertice> listA = new Vector<Vertice>();
		Vector<Vertice> listB = new Vector<Vertice>();
		Region region;

		/*
		 * Get the vertices
		 */
		// read the 1st region
		region = readRegion(regionA, 1);
		for(Coordinate p : region.getPoints()) {
/*			// if there are 2 point far away, create fake points (check starts at 2nd point)
			if(listA.size() > 1) {
				Coordinate p0 =listA.get(listA.size()-1).getPoint(); 
				if(distance(p, p0) > MAX_DISTANCE) {
					double y0, y1, x0, x1, x, y;
					x0 = p0.getLat();
					y0 = p0.getLon();
					x1 = p.getLat();
					y1 = p.getLon();
					for(double i=0; i<distance(p, p0); i += MAX_DISTANCE/2) {
						x = ((MAX_DISTANCE/2 + distance(p, p0)) * x0)/2 - MAX_DISTANCE/2/distance(p, p0)*x1;
						y = ((MAX_DISTANCE/2 + distance(p, p0)) * y0)/2 - MAX_DISTANCE/2/distance(p, p0)*y1;
						listA.add(new Vertice(new Coordinate(x, y)));
						x0 = x;
						y0 = y;
					}
				}
			}
*/			Vertice point = new Vertice(p);
			listA.add(point);
		}
		System.out.println("1st region with " + listA.size() + " vertices");

		// read the 2nd region
		region = readRegion(regionB, 2);
		for(Coordinate p : region.getPoints()) {
/*			// if there are 2 point far away, create fake points
			if(listB.size() > 1) {
				Coordinate p0 =listB.get(listB.size()-1).getPoint(); 
				if(distance(p, p0) > MAX_DISTANCE) {
					double y0, y1, x0, x1, x, y;
					x0 = p0.getLat();
					y0 = p0.getLon();
					x1 = p.getLat();
					y1 = p.getLon();
					for(double i=0; i<distance(p, p0); i += MAX_DISTANCE/2) {
						x = ((MAX_DISTANCE/2 + distance(p, p0)) * x0)/2 - MAX_DISTANCE/2/distance(p, p0)*x1;
						y = ((MAX_DISTANCE/2 + distance(p, p0)) * y0)/2 - MAX_DISTANCE/2/distance(p, p0)*y1;
						listB.add(new Vertice(new Coordinate(x, y)));
						x0 = x;
						y0 = y;
					}
				}
			}
*/			Vertice point = new Vertice(p);
			listB.add(point);
		}
		System.out.println("2nd region with " + listB.size() + " vertices");
		
		Vertice.unusedCoordinate = listA.size() + listB.size();

		/*
		 * Catalog the dups
		 */
		// look for dups between region
		for(int ib=0; ib<listB.size(); ib++) {
			for(int ia=0; ia<listA.size(); ia++) {
				if(listB.get(ib).getPoint().getLat() == listA.get(ia).getPoint().getLat() && listB.get(ib).getPoint().getLon() == listA.get(ia).getPoint().getLon()) {
					listA.get(ia).setFoundAt(ib);
					listB.get(ib).setFoundAt(ia);
					Vertice.unusedCoordinate -= 2;
				}
			}
		}

		System.out.println("Checking consistency");
		// check for consistency
		for(int ia=0; ia<listA.size(); ia++) {
			if(!listA.get(ia).isUnique()) {
				if(listB.get(listA.get(ia).getFoundAt()).isUnique()) {
					System.err.println("***** Inconsistent struct found from A to B at " + ia + " - " + listB.get(listA.get(ia).getFoundAt()));
					return;
				}
			}
		}
		
		System.out.println("Double checking consistency");
		// double check for consistency
		for(int ib=0; ib<listB.size(); ib++) {
			if(!listB.get(ib).isUnique()) {
				if(listA.get(listB.get(ib).getFoundAt()).isUnique()) {
					System.err.println("***** Inconsistent struct found from B to A at " + ib + " - " + listA.get(listB.get(ib).getFoundAt()));
					return;
				}
			}
		}
		
		// ignore single dup point
		for(int ia=0; ia<listA.size(); ia++) {
			if(ia==1186) {
				System.out.println("ListA = " + listA.get(ia-2).isUnique() + "," + listA.get(ia-1).isUnique() + "," + listA.get(ia).isUnique() + "," + listA.get((ia+1) % listA.size()).isUnique() + "," + listA.get((ia+2) % listA.size()).isUnique());
				System.out.println("Found = " + listA.get(ia-2).getFoundAt() + "," + listA.get(ia-1).getFoundAt() + "," + listA.get(ia).getFoundAt() + "," + listA.get((ia+1) % listA.size()).getFoundAt() + "," + listA.get(ia+2).getFoundAt());
				System.out.println("Point = " + listA.get(ia).getPoint().getLat() + "," + listA.get(ia).getPoint().getLon());
			}
			if(!listA.get(ia).isUnique() && listA.get((ia+1) % listA.size()).isUnique() && listA.get(ia-1<0?listA.size()-1:ia-1).isUnique()) {
				System.out.println("Setting " + ia + " as unique at listA");
				Vertice.unusedCoordinate++;
				listA.get(ia).setUnique(true);
			}
		}
		for(int ib=0; ib<listB.size(); ib++) {
			if(!listB.get(ib).isUnique() && listB.get((ib+1) % listB.size()).isUnique() && listB.get((ib-1<0)?(listB.size()-1):(ib-1)).isUnique()) {
				System.out.println("Setting " + ib + " as unique at listB");
				Vertice.unusedCoordinate++;
				listB.get(ib).setUnique(true);
			}
		}
		/*
		 * Joining regions
		 */
		region = new Region();
		int i = 0;
		int stepA = 1;
		int stepB = 0;

		while(!listA.get(i).isUnique()) {
			i += stepA;
			if(i >= listA.size()) i = 0;
			else if(i < 0) i = listA.size()-1;
		}
		while(Vertice.unusedCoordinate>1) {
			// part A
			System.out.println("i = " + i + ", Passo = " + stepA);
			if(i < 0) break;
			System.out.println("Creating region based on A (" + i + ")");
			while(listA.get(i).isUnique() && !listA.get(i).isVisited()) {
				region.add(listA.get(i).getPoint());
				listA.get(i).setVisited(true);
				i += stepA;
				if(i >= listA.size()) i = 0;
				else if(i < 0) i = listA.size()-1;
			}
			if(!listA.get(i).isVisited()) {
				region.add(listA.get(i).getPoint());
			}
			System.out.println("Paused at " + i);
			i = listA.get(i).getFoundAt();

			// part B
			if(stepB == 0 && listB.get((i+1)%listB.size()).isUnique()) {
				stepB = 1;
			}
			else {
				stepB = -1;
			}
			System.out.println("i = " + i + ", Passo = " + stepB);
			i += stepB;
			if(i == listB.size()) i = 0;
			else if(i == -1) i = listB.size()-1;
			if(i < 0) break;
			System.out.println("Creating region based on B (" + i + ")");
			while(listB.get(i).isUnique() && !listB.get(i).isVisited()) {
				region.add(listB.get(i).getPoint());
				listB.get(i).setVisited(true);
				i += stepB;
				if(i >= listB.size()) i = 0;
				else if(i < 0) i = listB.size()-1;
			}
			if(!listB.get(i).isVisited()) {
				region.add(listB.get(i).getPoint());
			}			
			System.out.println("Paused at " + i);
			i = listB.get(i).getFoundAt() + 1;
		}
		
		/*
		 * Cleaning up odd tiles
		 */
		double max1 =0, max2 =0;
		for(i=0; i<region.getPoints().size() -1; i++) {
			if(distance(region.getPoints().get(i), region.getPoints().get((i+1)%region.getPoints().size())) >= max1) {
				max2 = max1;
				max1 = distance(region.getPoints().get(i), region.getPoints().get((i+1)%region.getPoints().size()));
			}
			else if(distance(region.getPoints().get(i), region.getPoints().get((i+1)%region.getPoints().size())) >= max2) {
				max2 = distance(region.getPoints().get(i), region.getPoints().get((i+1)%region.getPoints().size()));
			}
		}
		System.out.println("Max1 = " + max1 + ", Max2 = " + max2);
		for(i=0; i<region.getPoints().size(); i++) {
			if(distance(region.getPoints().get(i), region.getPoints().get((i+1)%region.getPoints().size())) > MAX_DISTANCE && distance(region.getPoints().get(i), region.getPoints().get(i-1<0? region.getPoints().size()-1:i-1)) > MAX_DISTANCE) {
				region.getPoints().remove(i);
			}
			
		}
		/*
		 * creating output file
		 */
		try {
			FileWriter writer = new FileWriter(outputfile, false);
			PrintWriter printWriter = new PrintWriter(writer);
			for(Coordinate point: region.getPoints()) {
				printWriter.println(point.getLat() + " " + point.getLon());
			}
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	private double distance(Coordinate coordinate1, Coordinate coordinate2) {
		return Math.sqrt(Math.pow(Double.parseDouble(coordinate1.getLat()) - Double.parseDouble(coordinate2.getLat()), 2) + Math.pow(Double.parseDouble(coordinate1.getLon()) - Double.parseDouble(coordinate2.getLon()), 2));
	}

	
	/**
	 * Reads a region from a given file<br>
	 * @param filename	name of the file describing a region
	 * @param regionNumber number of the region
	 * @return a region
	 */
	private Region readRegion(String filename, int regionNumber) {
		// reads a region. A region is described by an X, Y coordinate per line
		Region region = new Region();
		region.setNumber(regionNumber);
//		System.err.println("[" + this.getClass().getName() + ".SelecionaGrupo] " + " criando região número " + region.getNumero());
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(filename));
			String line;
			while((line = br.readLine()) != null)
			{
				Coordinate coordinate = new Coordinate(
						line.substring(0, line.indexOf(" ")).trim(),
						line.substring(line.indexOf(" ")).trim()
						);
				region.add(coordinate);
			}
		}
		catch (IOException e)
		{
			System.err.println("Date = " + new Date());
			e.printStackTrace();
		}
		finally {
			if(br != null)
			{
				try {
					br.close();
				}
				catch (IOException e)
				{
					System.err.println("Date = " + new Date());
					e.printStackTrace();
				}
			}
		}
		return region;
	}
}

class Vertice {
	private Coordinate point;
	private boolean unique;
	private boolean visited;
	private int foundAt;

	public static int unusedCoordinate;

	public Vertice(Coordinate point) {
		this.point = point;
		this.unique = true;
		this.visited = false;
		this.foundAt = -1;
	}

	/**
	 * @return the point
	 */
	public Coordinate getPoint() {
		return point;
	}

	/**
	 * @param point the point to set
	 */
	public void setPoint(Coordinate point) {
		this.point = point;
	}

	/**
	 * @return the unique
	 */
	public boolean isUnique() {
		return unique;
	}

	/**
	 * @param unique the unique to set
	 */
	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	/**
	 * @return the used
	 */
	public boolean isVisited() {
		return visited;
	}

	/**
	 * @param visited the used to set
	 */
	public void setVisited(boolean visited) {
		this.visited = visited;
		unusedCoordinate--;
	}

	/**
	 * @return the foundAt
	 */
	public int getFoundAt() {
		return foundAt;
	}

	/**
	 * @param foundAt the foundAt to set
	 */
	public void setFoundAt(int foundAt) {
		this.foundAt = foundAt;
		this.unique = false;
	}
}
/**
 * Define uma região e métodos de acesso e verificação.
 * <p>
 * A região pode ser concava ou convexa
 * 
 * @author meslin
 *
 */
class Region
{
	private List<Coordinate> points;
	private int number;

	/**
	 * Constroi uma região vazia
	 */
	public Region()
	{
		super();
		points = new ArrayList<Coordinate>();
	}
	
	public void setNumber(int numero) { this.number = numero; }
	public int getNumero() { return this.number; }

	/**
	 * Adiciona um ponto à região
	 * 
	 * @param point
	 */
	public void add(Coordinate point)
	{
		this.points.add(point);
	}
	public List<Coordinate> getPoints()
	{
		return this.points;
	}
	
//	public boolean contains(Bus coordinates)
//	{
//		return contains(new Coordinate(coordinates.getLatitude(), coordinates.getLongitude()));
//	}
}
class Coordinate {
	public String lat, lon;

	public Coordinate(String lat, String lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public String getLat() {
		return lat;
	}

	public String getLon() {
		return lon;
	}
}
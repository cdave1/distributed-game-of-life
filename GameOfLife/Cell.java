// Copyright David Petrie 2008
//
// Represents a cell in the game.
import java.util.*;

class Cell
{
	public int row;
	public int col;
	public boolean hasLife;


	
	// csv in row,col,hasLife format
	public Cell(String csv)
	{
		StringTokenizer st = new StringTokenizer(csv, ",");
		if (st.countTokens() != 3) System.out.println("Badly formatted csv: " + csv);
		else {
			this.row = Integer.parseInt(st.nextToken());
			this.col = Integer.parseInt(st.nextToken());
			this.hasLife = Boolean.parseBoolean(st.nextToken());
		}				
	}



	public Cell(int row, int col, String life)
	{
		this.row = row;
		this.col = col;
		if (life.equals("x")) this.hasLife = true;
		else this.hasLife = false;
	}



	public Cell(int row, int col, boolean hasLife)
	{
		this.row = row;
		this.col = col;
		this.hasLife = hasLife;
	}


	public boolean equals(Cell c)
	{
		return ((c.row == this.row) && (c.col == this.col) && (c.hasLife == this.hasLife));
	}



	public String toString()
	{
		if (this.hasLife) return "O";
		return " ";
	}



	public String toCSV()
	{
		return "" + row + "," + col + "," + hasLife;
	}
}

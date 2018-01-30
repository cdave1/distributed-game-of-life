// Copyright David Petrie 2008
//
// Game of life utility functions.
//
// Most of these are used by two classes so it made sense to
// centralise them all.
import java.net.*;
import java.util.*;
import java.io.*;

public class GameOfLifeUtils
{
	// From a matrix of cells, this function will find the next state of those
	// cells. It needs to create a new array for this.
	public static Cell[][] NextState(Cell[][] in) 
	{
		Cell[][] newCells = new Cell[in.length][in[0].length];
		for (int row = 0; row < in.length; row++)
		{
			for (int col = 0; col < in[row].length; col++)
			{
				Cell c = in[row][col];
				int n = GameOfLifeUtils.CountLiveCells(in,row,col);
				if (c.hasLife) {
					if (n <= 1 || n >= 4) newCells[row][col] = new Cell(c.row, c.col, false);
					else newCells[row][col] = new Cell(c.row, c.col, c.hasLife);
				} else {
					if (n == 3) newCells[row][col] = new Cell(c.row, c.col, true);
					else newCells[row][col] = new Cell(c.row, c.col, false);
				}
			}
		}
		return newCells;
	}



	// counts the number of cells that have life around a central cell at row/col
	public static int CountLiveCells(Cell[][] cells, int row, int col)
	{
		int count = 0, r = 0, c = 0;
		for (Direction d: Direction.values())
		{
			r = row + d.rows;
			c = col + d.cols;
			if (!(r < 0 || r >= cells.length || c < 0 || c >= cells[0].length))
			{
				if (cells[r][c].hasLife) count++;
			}
		}
		return count;
	}



	// Prints cells to stdout
	public static void PrintCells(Cell[][] cells)
	{
		for (int row = 0; row < cells.length; row++)
		{
			for (int col = 0; col < cells[row].length; col++)
			{
				System.out.print(cells[row][col]);
			}
			System.out.println();
		}
	}



	// Prints cells to a buffer in CSV format.
	public static void PrintCells(Cell[][] cells, PrintWriter out)
	{
		for (int row = 0; row < cells.length; row++)
		{
			for (int col = 0; col < cells[row].length; col++)
				out.println(cells[row][col].toCSV());
		}
	}



	// compares two cell sets
	public static boolean CompareCells(Cell[][] one, Cell[][] two)
	{
		boolean isIdentical = true;
		for (int row = 0; row < one.length; row++)
		{
			for (int col = 0; col < one[row].length; col++) {
				if (!one[row][col].equals(two[row][col])) isIdentical = false;
			}
		}
		return isIdentical;
	}




	// Loads a cell set from file. 
	//
	// This code is fairly ugly but it's not core to the app so I'm not going to
	// spend any additional time making it prettier.	
	public static Cell[][] LoadPattern(String filename) {
		Cell[][] cells = new Cell[0][0];
		try {
			File file = new File(filename);
			int rows = GetLineCount(file);
			System.out.println("Lines in file \"" + filename + "\": " + rows);

			BufferedReader rdr = new BufferedReader(new FileReader(file));
			String line = rdr.readLine();
			cells = new Cell[rows][line.length()];
			int row = 0;
			int col = 0;
		
			while(line != null) {
				col = 0;
				while (line.length() > 0) {
					cells[row][col] = new Cell(row, col, line.substring(0,1));
					line = line.substring(1);
					col++;
				}
				row++;
				line = rdr.readLine();
			}
			return cells;
		} catch (Exception ex) { System.out.println(ex.toString()); }
		return cells;
	}



	// Gets the row of the work in the worker pattern
	public static int GetWorkerRow(int workers, int workerID)
	{	
		int cols = GetWorkerPatternCols(workers);
		if (cols == 0) return 1;
		return (int)Math.floor(workerID / cols);
	}



	// Gets the column of the work in the worker pattern
	public static int GetWorkerCol(int workers, int workerID)
	{
		int cols = GetWorkerPatternCols(workers);
		if (cols == 0) return 1;
		return workerID % cols;
	}



	// Gets the number of rows in the worker pattern
	public static int GetWorkerPatternRows(int workers)
	{
		LinkedList<Integer> f = GameOfLifeUtils.GetFactors(workers);
		if (f == null) return 1;
		return f.removeLast();
	}



	// Gets the number of columns in the worker pattern.
	public static int GetWorkerPatternCols(int workers)
	{
		LinkedList<Integer> f = GameOfLifeUtils.GetFactors(workers);
		if (f == null) return 1;
		f.removeLast();
		return f.removeLast();		
	}



	// Take the initial pattern and shoves it into the middle of a space that can
	// be evenly split up amongst workers.
	public static Cell[][] GetGameOfLifeSpace(int workers, int workerWidth, Cell[][] initialPattern)
	{
		int rows = Math.max(1, GetWorkerPatternRows(workers));
		int cols = Math.max(1, GetWorkerPatternCols(workers));

		int startRow = (int)(((double)rows/2.0)*workerWidth);
		int startCol = (int)(((double)cols/2.0)*workerWidth);

		startRow -= (initialPattern.length/2);
		startCol -= (initialPattern[0].length/2);

		startRow = Math.max(0, startRow);
		startCol = Math.max(0, startCol);

		Cell[][] gameSpace = new Cell[rows * workerWidth][cols * workerWidth];

		for (int r = 0; r < rows*workerWidth; r++)
		{
			for (int c = 0; c < cols*workerWidth; c++)
				gameSpace[r][c] = new Cell(r,c,false);
		}
		
		int endRow = Math.min((startRow + initialPattern.length), gameSpace.length);
		int endCol = Math.min((startCol + initialPattern[0].length), gameSpace[0].length);

		for (int r = startRow; r < endRow; r++)
		{
			for (int c = startCol; c < endCol; c++)
			{
				Cell cell = initialPattern[r-startRow][c-startCol];
				gameSpace[r][c] = new Cell(r, c, cell.hasLife);
			}
		}

		return gameSpace;
	}



	// Gets two factors of a number as close to sqr root as possible.
	//
	// Used to split the cells across each of the 
	public static LinkedList<Integer> GetFactors(int number) {
		if (number == 1) return null;
		java.util.LinkedList<Integer> l = new java.util.LinkedList();
		l.add(1);
		for (int i = 2; i <= number; i++) {
			while (number % i == 0) {
				l.add(i);
				if (l.size() == 3) l.add(l.removeFirst() * l.removeLast());
				number = number / i;
			}
			if (number == 1) break;
		}
		return l;
	}



	// Gets number of lines in a file.
	//
	// Adapted from a function here:
	// http://forum.java.sun.com/thread.jspa?threadID=474733&forumID=31
	public static int GetLineCount(File file) {
		int lineCount = 0;
		try {
			Reader reader = new InputStreamReader(new FileInputStream(file));
		        char[] buffer = new char[4096];
	        	for (int charsRead = reader.read(buffer); charsRead >= 0; charsRead = reader.read(buffer))
        		{
				for (int charIndex = 0; charIndex < charsRead ; charIndex++)
				{
					if (buffer[charIndex] == '\n')
					lineCount++;
				}
        		}
		        reader.close();

		} catch (Exception ex) { System.out.println(ex.toString()); }
        	return lineCount;
	}
}

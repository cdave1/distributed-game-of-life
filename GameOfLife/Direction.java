// Represents possible moves from a position.
// rows - rows offset to move in that direction
// cols - columns offset to move in that direction
public enum Direction {
	NORTH(-1,0),
	NORTHEAST(-1, 1),
	EAST(0, 1),
	SOUTHEAST(1, 1),
	SOUTH(1, 0),
	SOUTHWEST(1, -1),
	WEST(0, -1),
	NORTHWEST(-1, -1);

	public final int rows;
	public final int cols;
	public final String CSV;

	Direction (int rows, int cols){
		this.rows = rows;
		this.cols = cols;
		this.CSV = rows + ", " + cols;
	}
}

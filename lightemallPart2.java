import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//represents a game piece in the board
class GamePiece {
  // in logical coordinates, with the origin
  // at the top-left corner of the screen
  int row;
  int col;

  // whether this GamePiece is connected to the
  // adjacent left, right, top, or bottom pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;

  // whether the power station is on this piece
  boolean powerStation;
  boolean powered;

  // constructor 1
  public GamePiece(int row, int col) {
    this.row = row;
    this.col = col;
  }

  // constructor 2
  public GamePiece(int row, int col, boolean left, boolean right, boolean top, boolean bottom,
      boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }


  // Generate an image of this, the given GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this
  // - hasPowerStation: if true, draws a fancy star on this tile to represent the
  // power station
  //
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    // Start tile image off as a blue square with a wire-width square in the middle,
    // to make image "cleaner" (will look strange if tile has no wire, but that
    // can't be)
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    if (hasPowerStation) {
      image = new OverlayImage(
          new OverlayImage(new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
              new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))),
          image);
    }
    return image;
  }

  // method that rotates the gamePiece it is called on
  public void rotate() {
    boolean temp = this.left;
    this.left = this.bottom;
    this.bottom = this.right;
    this.right = this.top;
    this.top = temp;
  }

  //checks if two GamePieces are the same GamePiece
  public boolean sameGamePiece(GamePiece that) {
    return that.row == this.row 
        && that.col == this.col 
        && that.left == this.left
        && that.right == this.right 
        && that.top == this.top 
        && that.bottom == this.bottom
        && that.powerStation == this.powerStation;
  }

  public void connectTo(GamePiece to) {
    int rowDiff = this.row - to.row;
    int colDiff = this.col - to.col;

    if (rowDiff <= 1 && rowDiff >= -1 && colDiff <= 1 && colDiff >= -1) {
      if (rowDiff == 1) {
        this.top = true;
        to.bottom = true;
      } else if (rowDiff == -1) {
        this.bottom = true;
        to.top = true;
      } else if (colDiff == 1) {
        this.left = true;
        to.right = true;
      } else if (colDiff == -1) {
        this.right = true;
        to.left = true;
      }
    }
  }
}

//lightEmAll class that represents the game
class LightEmAll2 extends World {
  // a list of columns of GamePieces,
  // i.e., represents the board in column-major order
  ArrayList<ArrayList<GamePiece>> board;

  // a list of all nodes
  ArrayList<GamePiece> nodes;

  // a list of edges of the minimum spanning tree
  // GIVES A SUBSET OF EDGES
  ArrayList<Edge> mst;

  // the width and height of the board
  int width;
  int height;

  // the current location of the power station,
  // as well as its effective radius
  int powerRow;
  int powerCol;
  int radius;

  // random seed for examples to make the board
  Random rand;

  //field to keep track of the number of steps
  int steps;

  // constructor 1
  // more consistent game with seeded random
  LightEmAll2(ArrayList<ArrayList<GamePiece>> board, ArrayList<GamePiece> nodes, int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.rand = rand; // assign the random number generator
    this.nodes = nodes;
    this.board = board;

    // adding pieces to the game in order to draw them
    addGamePieces();

    // setting the board equal to the manual board created
    board = fixedBoard();

    // randomizing the nodes in the constructor so that it initializes in the
    // beginning of every game
    for (GamePiece gp : nodes) {
      // randomly rotate each GamePiece object
      for (int i = 0; i < this.rand.nextInt(4); i++) {
        gp.rotate();
      }
    }

    // to initialize the powerStation
    // SET IN THE CENTER OF THE GRID FOR PART 1
    this.powerRow = Math.round(height / 2);
    this.powerCol = Math.round(width / 2);
    this.board.get(this.powerCol).get(this.powerRow).powered = true;
    this.board.get(this.powerCol).get(this.powerRow).powerStation = true;

    // calling bfs to update the links
    bfs();

  }

  //  // constructor 2 that is completely random every time
  //  // fully random game
  //  LightEmAll2(ArrayList<ArrayList<GamePiece>> board, ArrayList<GamePiece> nodes, int width,
  //      int height) {
  //    this(board, nodes, width, height, new Random()); // call main constructor with null Random
  //  }



  LightEmAll2(int width, int height, Random rand) {
    this.width = width;
    this.height = height;
    this.rand = rand; //or should it be new Random()???
    //this.board = new ArrayList<>();
    //this.nodes = new ArrayList<>();

    //initialize board
    initializeBoard();

    //power station should be at top left
    this.powerRow = 0;
    this.powerCol = 0;

    this.nodes = Utils.flatten(this.board); // all the gamepieces in your board 

    this.MST();

    // randomizing the nodes in the constructor so that it initializes in the
    // beginning of every game
    for (GamePiece gp : nodes) {
      // randomly rotate each GamePiece object
      for (int i = 0; i < this.rand.nextInt(4); i++) {
        gp.rotate();
      }
    }

  }

  // EFFECT: initializes the board to to have no connections
  public void initializeBoard() {
    this.board = new ArrayList<>();
    for (int i = 0; i < this.width; i++) {
      ArrayList<GamePiece> col = new ArrayList<>();
      for (int j = 0; j < this.height; j++) {
        col.add(new GamePiece(j, i));
      }
      this.board.add(col);
    }
    this.board.get(0).get(0).powerStation = true;

  }

  // EFFECT: creates connections and unions between edges
  public void MST() {
    HashMap<GamePiece, GamePiece> reps = new HashMap<>();
    for (GamePiece gp : this.nodes) {
      reps.put(gp, gp);
    }
    ArrayList<Edge> treeEdges = new ArrayList<>();
    ArrayList<Edge> genList = this.generateEdges(); //calls method that generates edges

    while(treeEdges.size() < this.nodes.size() - 1) {
      Edge current = genList.remove(0);
      GamePiece from = current.from;
      GamePiece to = current.to;

      if (!this.topRep(reps.get(to), reps).sameGamePiece(this.topRep(reps.get(from), reps))) {
        treeEdges.add(current);
        this.union(to, from , reps);
        to.connectTo(from);
      }
    }
  }

  // EFFECT: sets the top representatives for each node
  public void union(GamePiece to, GamePiece from, HashMap<GamePiece,GamePiece> reps) {
    reps.put(topRep(to, reps), topRep(from, reps));
  }

  // finds the top level representative for the given GamePiece in the MST
  public GamePiece topRep(GamePiece gp, HashMap<GamePiece, GamePiece> reps) {
    if (gp.sameGamePiece(reps.get(gp))) {
      return gp;
    }
    return this.topRep(reps.get(gp), reps);
  }

  // finds all the possible edges on this board
  public ArrayList<Edge> generateEdges() {
    ArrayList<Edge> edges = new ArrayList<>();
    //represents directions to neighboring cells
    ArrayList<Posn> directions = new ArrayList<>(Arrays.asList(new Posn(-1, 0), new Posn(0, -1), new Posn(0, 1), new Posn(1, 0)));

    for (int col = 0; col < this.width; col++) {
      for (int row = 0; row < this.height; row++) {
        for (int v = 0; v < directions.size(); v++) {
          //for each vector p, calculates the coordinates x and y of the neighboring cell by adding the vector's 
          //coordinates to the current cell's coordinates (row and col)
          Posn p = directions.get(v);        
          int x = p.x + col;
          int y = p.y + row;
          if (this.validCoor(x, y)) { //checks if they are valid coordinates on the grid
            //if they are, make it a new edge with a random weight
            edges.add(new Edge(this.board.get(col).get(row), this.board.get(x).get(y), this.rand.nextInt(40)));
          }
        }
      }
    }
    //sort the edges
    Collections.sort(edges, new CompWeight());
    return edges;
  }

  public boolean validCoor(int x, int y) {
    return x >= 0 && x < this.width && y >= 0 && y < this.height;
  }


  //pt 1

  // creates the fixed manual grid (vertical lines and one horizontal line
  // straight through)
  public ArrayList<ArrayList<GamePiece>> fixedBoard() {

    for (int col = 0; col < this.width; col++) {
      ArrayList<GamePiece> column = new ArrayList<>();

      for (int row = 0; row < this.height; row++) {
        // column.add(row, new GamePiece(row, col));
        // get the current piece we are working with
        GamePiece piece = this.board.get(col).get(row);
        // all the vertical lines
        // every row should have the top piece be true, except for the top most row
        piece.top = (row > 0);
        // every row should have the bottom piece be true, except for the bot most row
        piece.bottom = (row < (height - 1));

        // the one horizontal line in the middle of the grid checks if the row we are at
        // is the middle-most row, set the horizontal line there the col cannot be 0
        // because the leftmost tile in the col cannot branch out left
        piece.left = (row == (height / 2) && col != 0);
        // the col cannot be the width - 1 because the rightmost tile in the col cannot
        // branch right
        piece.right = (row == (height / 2) && col != (width - 1));

        column.add(piece);
        nodes.add(piece);
      }
      board.add(column);
    }
    return board;
  }

  // method that adds pieces to the game in order to draw them
  public void addGamePieces() {
    for (int col = 0; col < this.width; col++) {
      ArrayList<GamePiece> column = new ArrayList<>();
      for (int row = 0; row < this.height; row++) {
        column.add(row, new GamePiece(row, col));
      }
      board.add(column);
    }
  }

  // onKeyEvent to allow the player to move the power station
  public void onKeyEvent(String key) {
    // check if the pressed key is "r"
    if (key.equals("r")) {
      // call resetBoard() to reset the board
      resetBoard();
    } else {
      if (key.equals("left")) {
        movePowerStation(-1, 0);
      }
      else if (key.equals("right")) {
        movePowerStation(1, 0);
      }
      else if (key.equals("down")) {
        movePowerStation(0, 1);
      }
      else if (key.equals("up")) {
        movePowerStation(0, -1);
      }
    }
  }

  // helper for onKeyEvent that is the action of moving the power station
  public void movePowerStation(int changeCol, int changeRow) {
    int newPowerCol = this.powerCol + changeCol;
    int newPowerRow = this.powerRow + changeRow;

    // check if the new position is within the board numbers
    if (newPowerCol >= 0 && newPowerCol < this.width && newPowerRow >= 0
        && newPowerRow < this.height) {

      // check if the new position is the power station
      GamePiece currentPiece = this.board.get(this.powerCol).get(this.powerRow);
      GamePiece newPiece = this.board.get(newPowerCol).get(newPowerRow);

      if ((changeCol == -1 && currentPiece.left && newPiece.right)
          || (changeCol == 1 && currentPiece.right && newPiece.left)
          || (changeRow == -1 && currentPiece.top && newPiece.bottom)
          || (changeRow == 1 && currentPiece.bottom && newPiece.top)) {

        // Update the power station position
        this.board.get(this.powerCol).get(this.powerRow).powerStation = false;
        this.powerCol = newPowerCol;
        this.powerRow = newPowerRow;
        this.board.get(this.powerCol).get(this.powerRow).powerStation = true;

      }
    }
  }

  // 10.1.3
  // updating pieces' links
  // starts at the power station and iterates through closest neighbors
  // to update the links and power them up
  public void bfs() {
    ArrayList<GamePiece> nodes = new ArrayList<>();
    ArrayList<GamePiece> visited = new ArrayList<>();

    GamePiece currentPowerStation = board.get(powerCol).get(powerRow);

    // add the power station
    nodes.add(currentPowerStation);

    while (!nodes.isEmpty()) {

      GamePiece curr = nodes.remove(0);

      // mark current piece as visited
      visited.add(curr);
      curr.powered = true;

      // check neighboring pieces that are connected and not visited yet

      // check the top of the current power station && if it is inbounds && if the
      // visitors list does not have it yet
      if (curr.top && curr.row > 0 && !visited.contains(board.get(curr.col).get(curr.row - 1))) {
        GamePiece topPiece = (board.get(curr.col).get(curr.row - 1));
        if (topPiece.bottom) {
          nodes.add(topPiece);
        }
      }

      // check the bot of the current power station && if it is inbounds && if the
      // visitors list does not have it yet
      if (curr.bottom && curr.row < height - 1
          && !visited.contains(board.get(curr.col).get(curr.row + 1))) {
        GamePiece botPiece = (board.get(curr.col).get(curr.row + 1));
        if (botPiece.top) {
          nodes.add(botPiece);
        }

      }

      // check the left of the current power station && if it is inbounds && if the
      // visitors list does not have it yet
      if (curr.left && curr.col > 0 && !visited.contains(board.get(curr.col - 1).get(curr.row))) {
        GamePiece leftPiece = (board.get(curr.col - 1).get(curr.row));
        if (leftPiece.right) {
          nodes.add(leftPiece);
        }
      }

      // check the right of the current power station && if it is inbounds && if the
      // visitors list does not have it yet
      if (curr.right && curr.col < width - 1
          && !visited.contains(board.get(curr.col + 1).get(curr.row))) {
        GamePiece rightPiece = (board.get(curr.col + 1).get(curr.row));
        if (rightPiece.left) {
          nodes.add(rightPiece);
        }
      }
    }
  }

  // mouse click to implement rotating the individual pieces
  public void onMouseClicked(Posn pos) {

    // convert mouse click position to grid coordinates
    int row = pos.y / 50;
    int col = pos.x / 50;

    // check if the click is within the bounds of the board
    if (row >= 0 && row < height && col >= 0 && col < width) {
      // get the game piece at the clicked position
      GamePiece clickedPiece = board.get(col).get(row);

      // rotate the clicked piece
      rotatePiece(clickedPiece);

      // "reset" the game after rotating the clicked piece, so bfs can run again
      for (GamePiece node : this.nodes) {
        node.powered = false;
      }

      // update power distribution
      bfs();

    }

    if (checkWinCondition()) {
      this.endOfWorld("CONGRATS! YOU WON");
    }

  }

  // rotate the given game piece
  public void rotatePiece(GamePiece piece) {
    // toggle the rotation status of the piece
    piece.rotate();
    //every time you rotate a piece it is a step that you take, the lower the steps the better
    steps++;
  }

  // checks if the player has won yet by checking if all the nodes are powered up
  public boolean checkWinCondition() {
    // you went through every single node, point to congrats end scene
    for (GamePiece node : nodes) {
      if (!node.powered) {
        return false;
      }
    }
    return true;
  }

  // shows the last scene (you won)
  public WorldScene lastScene(String message) {

    // create a new scene with the size of the grid
    WorldScene scene = this.makeScene();

    // draw the message at the center of the scene
    TextImage messageText = new TextImage(message, 20, FontStyle.BOLD, Color.BLACK);
    scene.placeImageXY(messageText, 125, 125);

    // return the scene
    return scene;
  }

  // creating the worldscene to make the game/grid show up
  public WorldScene makeScene() {

    WorldScene ws = new WorldScene(500, 500);

    // constants :
    int tileSize = 50;
    int wireWidth = 5;

    // empty image base to build off of
    WorldImage gridImage = new EmptyImage();

    // loops through each row
    for (int row = 0; row < height; row++) {
      WorldImage rowImage = new EmptyImage();

      // loop through each column
      for (int col = 0; col < width; col++) {
        // get the current piece in the col list
        GamePiece piece = board.get(col).get(row);

        // generates the tile image for the current piece
        // wire color calls on a helper method to calculate the wire color of the
        // current piece
        // power station field checks if the row and col of this piece is equal to the
        // powerStation row and col
        // if they are, it will be true and their will be a powerStation there,
        // otherwise no
        Color color = Color.black;
        if (piece.powered) {
          int green = 255 - 25 * (int) Math.sqrt(Math.pow(this.powerCol - row, 2) 
              + Math.pow(this.powerRow - col, 2));
          color = new Color(255, green, 0);
        }
        WorldImage tile = piece.tileImage(tileSize, wireWidth, color,
            (piece.row == powerRow && piece.col == powerCol));

        // add tile to the row image
        rowImage = new BesideImage(rowImage, tile);
      }

      // add row image to the grid image
      gridImage = new AboveImage(gridImage, rowImage);

      ws.placeImageXY(gridImage, 125, 125);

      // setting up a title, first row 
      TextImage title = new TextImage("WELCOME TO POWER LINE!", 15, FontStyle.BOLD, Color.BLACK);
      ws.placeImageXY(title, 125, 260);

      //set up who the game was made by 
      TextImage coders = new TextImage("made by : livia & gayatri", 15, FontStyle.BOLD, Color.darkGray);
      ws.placeImageXY(coders, 125, 280);

      // setting up an end game pointer
      TextImage restartGame = new TextImage("click r to restart the game", 18, FontStyle.BOLD, Color.GRAY);
      ws.placeImageXY(restartGame, 125, 300);

      //draw the steps taken so far
      TextImage stepsText = new TextImage("amount of steps: " + steps, 15, FontStyle.BOLD, Color.BLACK);
      ws.placeImageXY(stepsText, 125, 350);

      //draw the score
      //TextImage scoreText = new TextImage("Score: " + score, 20, FontStyle.BOLD, Color.BLACK);
      //ws.placeImageXY(scoreText, 400, 100);

    }

    return ws;
  }

  //EXTRA CREDIT :::::::

  //implement POWER STATION RADIUS


  //  For this extra credit feature, the power station reach must be modeled as limited by distance,
  //  up to an effective radius, where the radius is defined as (𝑑𝑖𝑎𝑚𝑒𝑡𝑒𝑟/2)+1
  //  Initially, you will compute the initial radius based on the initial (solved) board, before randomizing it. 
  //  To compute the diameter of your graph, first run breadth-first search (see below) to determine the last-found 
  //  GamePiece. Next, run breadth-first search starting from that last-found piece and count the depth to the 
  //  new last-found piece again. That depth is the diameter of the graph. As long as the board is not fully connected, 
  //  this initial radius should be used to calculate whether a connected wire is powered or not.
  //
  //  Whenever all the cells are fully connected, you should recompute the power station’s effective radius based on the 
  //  current configuration. That updated radius should be used to test if all of the cells are within the effective 
  //  radius of the power station.


  //WHISTLES :

  //  Enhancing the graphics. For example, you could implement the gradient coloring, as wires get further from the power station. 
  // small whistle, just check how far the gradient is and put that in calculate wire color helper


  //  Allowing the player to start a new puzzle without restarting the program.
  // restarting the board to randomize again and set new kruskal to play, click r to restart

  //should it change where the pipe is or should the pipe stay in the same place?
  // Method to reset the board with default connectivity
  public void resetBoard() {
    // Clear existing board
    this.board.clear();
    this.nodes.clear();

    addGamePieces();

    board = fixedBoard();

    // randomizing the nodes in the constructor so that it initializes in the
    // beginning of every game
    for (GamePiece gp : nodes) {
      // randomly rotate each GamePiece object
      for (int i = 0; i < this.rand.nextInt(4); i++) {
        gp.rotate();
      }
    }      

    bfs();
  }


  //i put this in the code :::
  //  Keeping score: how many steps does the player take before connecting all the wires? Lower scores are better... 
  //  You’d need to enhance the display to render the score so far somehow.
  // every click to connect makes a lower score?? but what if it is a wrong connection? just know that socre should be
  // 0 when everything is connected and they won, display score


  //  Or, keeping time: display how long it takes for the player to beat the game.
  // implement on tick so that every tick adds to the time displayed, when 60 sec hits add it to be one minute 
}

//edge class that represents the links between cells
class Edge {
  GamePiece from;
  GamePiece to;
  int weight;

  Edge(GamePiece from, GamePiece to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;  
  }

}

class CompWeight implements Comparator<Edge> {
  // compares the edges by weight
  public int compare(Edge e1, Edge e2) {
    return e1.weight - e2.weight;
  }
}

class Utils {
  static // Turns a 2D ArrayList into a 1D ArrayList
  <T> ArrayList<T> flatten(ArrayList<ArrayList<T>> toFlatten) {
    ArrayList<T> flat = new ArrayList<>();
    for (ArrayList<T> al : toFlatten) {
      flat.addAll(al);
    }
    return flat;
  }
}


//examples class for lightEmAll
class LightEmAllExamples2 {

  // manually drawn board
  LightEmAll2 lightem1;

  ArrayList<ArrayList<GamePiece>> emptyBoard;

  ArrayList<GamePiece> emptyNodes;

  // board drawn for pt 2 
  LightEmAll2 lightem2;
  
  LightEmAll2 lightem3;

  void initData() {

    // game1, manually drawn in constructor
    emptyBoard = new ArrayList<ArrayList<GamePiece>>();

    emptyNodes = new ArrayList<GamePiece>();

    lightem1 = new LightEmAll2(emptyBoard, emptyNodes, 5, 5, new Random(3));

    lightem2 = new LightEmAll2(5, 5, new Random());
    
    lightem3 = new LightEmAll2(3, 3, new Random());
  }

  //TESTS FROM PART 1
  // test onKeyEvent
  void testOnKeyEvent(Tester t) {
    initData();

    // test before moving it
    t.checkExpect(this.lightem1.board.get(2).get(2).powerStation, true);
    // test moving right
    lightem1.onKeyEvent("right");
    t.checkExpect(this.lightem1.board.get(2).get(2).powerStation, false);
    t.checkExpect(this.lightem1.board.get(3).get(2).powerStation, true);

    // test moving right again (power station is currently (3,2)
    lightem1.onKeyEvent("right");
    t.checkExpect(this.lightem1.board.get(3).get(2).powerStation, false);
    t.checkExpect(this.lightem1.board.get(4).get(2).powerStation, true);

    // make sure it wont go off the board and will stay within the grid
    lightem1.onKeyEvent("right");
    // farthest right it can go
    t.checkExpect(this.lightem1.board.get(4).get(2).powerStation, true);

    // test left
    lightem1.onKeyEvent("left");
    t.checkExpect(this.lightem1.board.get(4).get(2).powerStation, false);
    t.checkExpect(this.lightem1.board.get(3).get(2).powerStation, true);

    // test up if there is no wire connected for it to move to
    lightem1.onKeyEvent("up");
    // should stay the same
    t.checkExpect(this.lightem1.board.get(3).get(2).powerStation, true);
    // should not be able to move there cause there is no wire
    t.checkExpect(this.lightem1.board.get(3).get(1).powerStation, false);

    // test moving down, should be able to move down at this position
    lightem1.onKeyEvent("down");
    t.checkExpect(this.lightem1.board.get(3).get(2).powerStation, false);
    t.checkExpect(this.lightem1.board.get(3).get(3).powerStation, true);
  }

  // test movePowerStation
  void testMovePowerstation(Tester t) {
    initData();
    // before moving the power station
    t.checkExpect(lightem1.board.get(2).get(2).powerStation, true);
    // test after moving it left
    lightem1.movePowerStation(-1, 0);
    t.checkExpect(lightem1.board.get(2).get(2).powerStation, false);
    t.checkExpect(lightem1.board.get(1).get(2).powerStation, true);

    // try moving it left again, should not work because there is no wire to move
    // left to
    lightem1.movePowerStation(-1, 0);
    t.checkExpect(lightem1.board.get(1).get(2).powerStation, true);
    t.checkExpect(lightem1.board.get(0).get(2).powerStation, false);

    // move it up
    lightem1.movePowerStation(0, -1);
    t.checkExpect(lightem1.board.get(1).get(2).powerStation, false);
    t.checkExpect(lightem1.board.get(1).get(1).powerStation, true);

  }

  // test addGamePieces
  void testAddGamePieces(Tester t) {
    initData();

    // check the dimensions of the board to make sure the size is what it should be
    t.checkExpect(lightem1.board.size(), 10);

    lightem1.addGamePieces();

    // check the dimensions of the board to make sure the size is updated
    t.checkExpect(lightem1.board.size(), 15);

    lightem1.addGamePieces();

    // check the dimensions of the board to make sure the size is updated
    t.checkExpect(lightem1.board.size(), 20);
  }

  // test rotate
  void testRotate(Tester t) {
    initData();
    // before rotating
    t.checkExpect(lightem1.board.get(0).get(0),
        new GamePiece(0, 0, false, false, true, false, false, false));
    // rotate, should move it top to right
    lightem1.board.get(0).get(0).rotate();
    t.checkExpect(lightem1.board.get(0).get(0),
        new GamePiece(0, 0, false, true, false, false, false, false));

    // before rotating a 4 way wire tile
    t.checkExpect(lightem1.board.get(2).get(2),
        new GamePiece(2, 2, true, true, true, true, true, true));
    // rotate
    lightem1.board.get(2).get(2).rotate();
    // after rotating, should be the same
    t.checkExpect(lightem1.board.get(2).get(2),
        new GamePiece(2, 2, true, true, true, true, true, true));

    // rotate the same piece 4 times to get it back to the original place
    // before rotating
    t.checkExpect(lightem1.board.get(4).get(0),
        new GamePiece(0, 4, true, false, false, false, false, false));
    // rotate 4 times
    lightem1.board.get(4).get(0).rotate();
    lightem1.board.get(4).get(0).rotate();
    lightem1.board.get(4).get(0).rotate();
    lightem1.board.get(4).get(0).rotate();
    // after rotating 4 times, should be the same
    t.checkExpect(lightem1.board.get(4).get(0),
        new GamePiece(0, 4, true, false, false, false, false, false));
  }

  // test RotatePiece
  void testRotatePiece(Tester t) {
    initData();
    // before rotating it
    t.checkExpect(lightem1.board.get(0).get(0),
        new GamePiece(0, 0, false, false, true, false, false, false));
    // rotate it
    lightem1.rotatePiece(lightem1.board.get(0).get(0));
    // goes from top to right
    t.checkExpect(lightem1.board.get(0).get(0),
        new GamePiece(0, 0, false, true, false, false, false, false));

    // before rotating a 4 way wire tile
    t.checkExpect(lightem1.board.get(2).get(2),
        new GamePiece(2, 2, true, true, true, true, true, true));
    // rotate
    lightem1.rotatePiece(lightem1.board.get(2).get(2));
    // after rotating, should be the same
    t.checkExpect(lightem1.board.get(2).get(2),
        new GamePiece(2, 2, true, true, true, true, true, true));

    // rotate the same piece 4 times to get it back to the original place
    // before rotating
    t.checkExpect(lightem1.board.get(4).get(0),
        new GamePiece(0, 4, true, false, false, false, false, false));
    // rotate 4 times
    lightem1.rotatePiece(lightem1.board.get(4).get(0));
    lightem1.rotatePiece(lightem1.board.get(4).get(0));
    lightem1.rotatePiece(lightem1.board.get(4).get(0));
    lightem1.rotatePiece(lightem1.board.get(4).get(0));
    // after rotating 4 times, should be the same
    t.checkExpect(lightem1.board.get(4).get(0),
        new GamePiece(0, 4, true, false, false, false, false, false));
  }

  // test bfs method
  void testBfs(Tester t) {
    initData();

    // recall that powerstation is initialized in the center, (2, 2)

    // run bfs
    lightem1.bfs();

    // checking if pieces are correctly powered around the powerstation if they are
    // connected
    // ensure power station is powered
    t.checkExpect(lightem1.board.get(2).get(2).powered, true);

    // this tile not powered cause the wire does not connect
    t.checkExpect(lightem1.board.get(2).get(1).powered, false);
    t.checkExpect(lightem1.board.get(2).get(3).powered, true);
    t.checkExpect(lightem1.board.get(1).get(2).powered, true);
    t.checkExpect(lightem1.board.get(3).get(2).powered, true);

  }

  // test onMouseClicked method
  void testOnMouseClicked(Tester t) {
    initData();

    // initially top piece true only
    t.checkExpect(lightem1.board.get(0).get(0).top, true);

    // a mouse click on the top-left corner
    lightem1.onMouseClicked(new Posn(25, 25));

    // verify that the top-left corner piece has rotated
    t.checkExpect(lightem1.board.get(0).get(0).top, false);
    t.checkExpect(lightem1.board.get(0).get(0).right, true);

    // initially all sides true
    t.checkExpect(lightem1.board.get(2).get(2),
        new GamePiece(2, 2, true, true, true, true, true, true));

    // Additional scenario: Simulate a mouse click on an interior piece
    lightem1.onMouseClicked(new Posn(250, 250));

    // verify the center piece's parameters did not change
    t.checkExpect(lightem1.board.get(2).get(2),
        new GamePiece(2, 2, true, true, true, true, true, true));
  }

  // test checkWinCondiiton method
  void testCheckWinCondition(Tester t) {
    initData();

    // all pieces are powered
    for (ArrayList<GamePiece> column : lightem1.board) {
      for (GamePiece piece : column) {
        piece.powered = true;
      }
    }
    t.checkExpect(lightem1.checkWinCondition(), true);

    // no pieces are powered
    for (ArrayList<GamePiece> column : lightem1.board) {
      for (GamePiece piece : column) {
        piece.powered = false;
      }
    }
    t.checkExpect(lightem1.checkWinCondition(), false);

    // only one piece is powered
    lightem1.board.get(2).get(2).powered = true;
    t.checkExpect(lightem1.checkWinCondition(), false);

    // only some pieces are powered
    lightem1.board.get(3).get(3).powered = true;
    lightem1.board.get(4).get(4).powered = true;
    t.checkExpect(lightem1.checkWinCondition(), false);
  }

  // test lastScene method
  void testLastScene(Tester t) {
    initData();

    // power up all pieces to trigger a win
    for (ArrayList<GamePiece> column : lightem1.board) {
      for (GamePiece piece : column) {
        piece.powered = true;
      }
    }

    // get the expected scene
    WorldScene scene = lightem1.makeScene();
    TextImage messageText = new TextImage("Congratulations! You won!", 20, FontStyle.BOLD,
        Color.BLACK);
    scene.placeImageXY(messageText, 125, 125);

    // verify the properties of the scene
    t.checkExpect(lightem1.lastScene("Congratulations! You won!"), scene);
  }

  // test the fixedBoard method
  void testFixedBoard(Tester t) {
    initData();

    // test for a 5x5 board
    ArrayList<ArrayList<GamePiece>> board = lightem1.fixedBoard();

    // check the dimensions of the board
    t.checkExpect(board.size(), 15);
    t.checkExpect(board.get(0).size(), lightem1.height);

    // check that each game piece has valid connections
    for (int col = 0; col < lightem1.width; col++) {
      for (int row = 0; row < lightem1.height; row++) {
        GamePiece piece = board.get(col).get(row);

        // ensure that at least one side of the piece is powered
        t.checkExpect(piece.top || piece.bottom || piece.left || piece.right, true);

        // ensure that opposite sides have consistent connections
        t.checkExpect(piece.top,
            board.get(col).get((row - 1 + lightem1.height) % lightem1.height).bottom);
        t.checkExpect(piece.bottom, board.get(col).get((row + 1) % lightem1.height).top);
        t.checkExpect(piece.left,
            board.get((col - 1 + lightem1.width) % lightem1.width).get(row).right);
        t.checkExpect(piece.right, board.get((col + 1) % lightem1.width).get(row).left);
      }
    }
  }
  
  // TESTS FOR PART 2
  // test MST
//  void testMst(Tester t) {
//    initData();
//    t.checkExpect(null, null);
//  }
  
//  // test Union
//  void testUnion(Tester t) {
//    initData();
//  }

  // test topRep
  
  // test generateEdges
//  void testGenerateEdges(Tester t) {
//    initData();
//    lightem3.generateEdges();
//    t.checkExpect(lightem3.mst, null);
//  }
  
  // test validCoor
  boolean testValidCoor(Tester t) {
    initData();
    return t.checkExpect(lightem3.validCoor(0, 0), true) &&
        t.checkExpect(lightem3.validCoor(1, 0), true) &&
        t.checkExpect(lightem3.validCoor(1, 2), true) &&
        t.checkExpect(lightem3.validCoor(2, 0), true) &&
        t.checkExpect(lightem3.validCoor(3, 3), false);
  }
  
  // test resetBoard
  
  // test sameGamePiece
  boolean testSameGamePiece(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, true, true, true, true, true, true);
    GamePiece gp2 = new GamePiece(0, 0, true, true, true, true, true, true);
    GamePiece gp3 = new GamePiece(0, 0, false, true, true, true, false, true);
    GamePiece gp4 = new GamePiece(1, 1, true, true, true, true, true, true);
    return t.checkExpect(gp1.sameGamePiece(gp2), true) &&
        t.checkExpect(gp2.sameGamePiece(gp3), false) &&
        t.checkExpect(gp2.sameGamePiece(gp4), false);
  }
  
  // test connectTo
  void testConnectTo(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, true, true, true, true, true, true);
    GamePiece gp2 = new GamePiece(0, 0, true, true, true, true, true, true);
    GamePiece gp3 = new GamePiece(0, 0, false, true, true, true, false, true);
    GamePiece gp4 = new GamePiece(1, 1, true, true, true, true, true, true);
    
  }
  
  // test compare
  
  // test Utils
  
  // test makeScene
   
//  void testBigBang(Tester t) {
//    this.initData();
//
//    LightEmAll2 world = lightem2;
//
//    int worldWidth = 500;
//    int worldHeight = 500;
//    double tickRate = 0.1;
//    world.bigBang(worldWidth, worldHeight, tickRate);
//  }

}
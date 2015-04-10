package edu.augustana.csc490.basketballgamehanson;

/**
 * Created by MHanson on 4/6/2015.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ShooterView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = "ShooterView"; // for Log.w(TAG, ...)

    private ShooterThread shooterThread; // runs the main game loop
    private Activity activity; // keep a reference to the main Activity
    private boolean dialogIsDisplayed = false;



    //variables for the game loop and tracking statistics
    private boolean gameOver = true; // is the game over?
    private double timeLeft; // time remaining in seconds
    private int score; // players score
    private int shotsTaken; // shots the user has taken
    private double totalElapsedTime; // elapsed seconds

    private Line backBoard; // start and end points of backboard
    private int backboardDistance; // backboard distance from left
    private int backboardBeginning; // backboard distance from top
    private int backboardEnd; // backboard bottom's distance from top
    private int initialBackboardVelocity;  // initial backboard speed multiplier
    private float backboardVelocity; // backboard speed multiplier
    private int lineWidth; // width of backboard

    // variables for front of rim
    private Line frontRim; // start and end points of front of rim
    private int frontRimDistance; // front of rim distance from left
    private int frontRimBeginning; // front of rim distance from top
    private int frontRimEnd; // front of rim bottom's distance from top
    private int initialFrontRimVelocity; // front of rim initial speed multiplier
    private float frontRimVelocity; // front of rim speed multiplier

    // variables for point checker
    private Line pointChecker;
    private int pointCheckerDistance;
    private int pointCheckerBeginning;
    private int pointCheckerEnd;
    private int initialPointCheckerVelocity;
    private float pointCheckerVelocity;

    //variables for player and basketball
    private Point basketball; //basketball image's upper-left corner
    private int basketballVelocityX; //basketball's x velocity
    private int basketballVelocityY; // basketball's y velocity
    private boolean basketballOnScreen; // is the basketball on the screen?
    private boolean basketballHasScored; // has the basketball gone through the rim?
    private int basketballRadius; // basketball's radius
    private int basketballSpeed; // basketball's speed
    private int playerLength; // player's length
    private Point playerEnd; // the endpoint of the player



    private float x;
    private int y;
    private int screenWidth;
    private int screenHeight;

    // Paint variables used when drawing each item on the screen
    private Paint textPaint; // Paint used to draw text
    private Paint basketballPaint; //Paint used to draw the basketball
    private Paint playerPaint; // Paint used to draw the player
    private Paint backboardPaint; //Paint used to draw the backboard
    private Paint backgroundPaint; // Paint used to clear the drawing area
    private Paint frontRimPaint; // Paint used to draw the front of the rim
    private Paint pointCheckerPaint;

    // Bitmap variables used for drawing images
    private Bitmap basketballBitMap;
    private Bitmap backgroundBitMap;



    private Paint myPaint;





    public ShooterView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        activity = (Activity) context;

        getHolder().addCallback(this);

        //initialize Lines and Point representing game items
        backBoard = new Line();
        basketball = new Point();
        frontRim = new Line();
        pointChecker = new Line();



        //construct Paints for drawing text, basketball, backboard, and player
        // These are configured in method onSizeChanged
        textPaint = new Paint();
        playerPaint = new Paint();
        basketballPaint = new Paint();
        backboardPaint = new Paint();
        backgroundPaint = new Paint();
        frontRimPaint = new Paint();

        pointCheckerPaint = new Paint();

        //construct BitMaps
        basketballBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        //source: pdclipart.org
        basketballBitMap = basketballBitMap.createScaledBitmap(basketballBitMap, 40, 40, true);
        backgroundBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.basketballbackground);
        backgroundBitMap = backgroundBitMap.createScaledBitmap(backgroundBitMap, 575, 920, true);



        backboardPaint.setColor(Color.GRAY);
        playerPaint.setColor(Color.BLACK);
        basketballPaint.setColor(Color.RED);
        frontRimPaint.setColor(Color.RED);

        pointCheckerPaint.setColor(Color.RED);

    } // end ShooterView constructor



    // called when the size changes (and first time, when view is created)
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w;
        screenHeight = h;
        playerLength = w / 8; // player length 1/8 screen width

        basketballRadius = w / 36; // basketball radius 1/36 screen width
        basketballSpeed = w * (12 / 4); // basketball speed multiplier


        lineWidth = w / 24; // backboard 1/24 screen width

        // configure instance variables related to the backboard
        backboardDistance = w * 19 / 20; // backboard 9/10 screen width from left
        backboardBeginning = h / 8; // distance from top 1/8 screen height
        backboardEnd = h * 2 / 8; // distance from top 2/8 screen height
        initialBackboardVelocity = h / 4; // initial backboard speed multiplier
        backBoard.start = new Point (backboardDistance, backboardBeginning);
        backBoard.end = new Point(backboardDistance, backboardEnd);

        // configure instance variables related to the front of the rim
        frontRimDistance = w * 8/10;
        frontRimBeginning = h * 15 / 64;
        frontRimEnd = backboardEnd;
        initialFrontRimVelocity = initialBackboardVelocity;
        frontRim.start = new Point(frontRimDistance, frontRimBeginning);
        frontRim.end = new Point(frontRimDistance, frontRimEnd);



        // configure instance variables related to the point checker
        pointCheckerDistance = w * 139 / 160;
        pointCheckerBeginning = h * 15 / 64;
        pointCheckerEnd = backboardEnd;
        initialPointCheckerVelocity = initialBackboardVelocity;
        pointChecker.start = new Point(pointCheckerDistance, pointCheckerBeginning);
        pointChecker.end = new Point(pointCheckerDistance, pointCheckerEnd);



        //endpoint of the player initially points horizontally
        playerEnd = new Point(playerLength, h);

        // configure Paint objects for drawing game elements
        textPaint.setTextSize(w / 20); // text size 1/20 of screen width
        textPaint.setAntiAlias(true); // smoothes the text
        playerPaint.setStrokeWidth(lineWidth * 1.5f); // set line thickness
        backboardPaint.setStrokeWidth(lineWidth); // set line thickness
        frontRimPaint.setStrokeWidth(lineWidth / 2);
        pointCheckerPaint.setStrokeWidth(lineWidth * 3);
        backgroundPaint.setColor(Color.WHITE); // set background color


        startNewGame();
    }

    public void startNewGame()
    {


        backboardVelocity = initialBackboardVelocity;
        frontRimVelocity = initialFrontRimVelocity;
        pointCheckerVelocity = initialPointCheckerVelocity;
        timeLeft = 60;
        score = 0;
        basketballOnScreen = false;
        shotsTaken = 0;
        totalElapsedTime = 0.0;

        backBoard.start.set(backboardDistance, backboardBeginning);
        backBoard.end.set(backboardDistance, backboardEnd);
        frontRim.start.set(frontRimDistance, frontRimBeginning);
        frontRim.end.set(frontRimDistance, frontRimEnd);
        pointChecker.start.set(pointCheckerDistance, pointCheckerBeginning);
        pointChecker.end.set(pointCheckerDistance, pointCheckerEnd);


        if (gameOver)
        {
            gameOver = false;
            shooterThread = new ShooterThread(getHolder());
            shooterThread.start(); // start the main game loop going
        }
    }


    private void gameStep()
    {
        x++;
    }

    public void updatePositions(double elapsedTimeMS){
        double interval = elapsedTimeMS / 1000.0;

        if (basketballOnScreen) { // if there is currently a basketball on the screen
            //update basketball position
            basketball.x += interval * basketballVelocityX;
            basketball.y += interval *basketballVelocityY;
            //exerts gravity on ball (archs shot)
            basketballVelocityY += basketballSpeed / 75;










            // check for collision with backboard
            if (basketball.x + basketballRadius > backboardDistance - 30 &&
                    basketball.x - basketballRadius < backboardDistance - 30 &&
                    basketball.y + basketballRadius > backBoard.start.y &&
                    basketball.y - basketballRadius < backBoard.end.y) {

                basketballVelocityX *= -0.01; //reverse the basketball's direction
                //basketballHasScored = true;


                // check for collision with front of rim
            } else if (basketball.x + basketballRadius > frontRimDistance - 40 &&
                    basketball.x - basketballRadius < frontRimDistance - 40 &&
                    basketball.y + basketballRadius > frontRim.start.y &&
                    basketball.y - basketballRadius < frontRim.end.y){
                basketballVelocityX *= -0.01;

                //check for collisions with left and right walls
            } else if (basketball.x + basketballRadius > screenWidth || basketball.x - basketballRadius < 0) {
                basketballOnScreen = false; // remove basketball from screen
                if(basketballHasScored){
                    score++;
                }
                //check for collisions with top and bottom walls
            } else if (basketball.y + basketballRadius > screenHeight || basketball.y - basketballRadius < 0) {
                basketballOnScreen = false;
                if(basketballHasScored){
                    score++;
                }

                // check for collision with point checker
                // score increases if hit
            }
            if (basketball.x >= pointCheckerDistance - (lineWidth * 3) &&
                    basketball.x <= backboardDistance &&
                    basketball.y > pointChecker.start.y &&
                    basketball.y < pointChecker.end.y){
                basketballHasScored = true;

            }
        }
        // update the backboard's position
        double backboardUpdate = interval * backboardVelocity;
        backBoard.start.y += backboardUpdate;
        backBoard.end.y += backboardUpdate;

        // update the rim's position
        double frontRimUpdate = interval * frontRimVelocity;
        double pointCheckerUpdate = interval * pointCheckerVelocity;
        frontRim.start.y += frontRimUpdate;
        frontRim.end.y += frontRimUpdate;
        pointChecker.start.y += pointCheckerUpdate;
        pointChecker.end.y += pointCheckerUpdate;




        // if the backboard hit the top or bottom, reverse direction
        if (backBoard.start.y < 2 || backBoard.end.y > screenHeight / 1.2){
            backboardVelocity *= -1;
            frontRimVelocity *= -1;
            pointCheckerVelocity *= -1;
        }


        timeLeft -= interval;

        //if the timer reached zero
        if (timeLeft <= 0.0)
        {
            timeLeft = 0.0;
            gameOver = true; // the game is over
            shooterThread.setRunning(false);
            stopGame();

            showGameOverDialog(R.string.game_over); // show the game over dialog
        }




    }

    public void shootBasketball(MotionEvent event)
    {
        if (basketballOnScreen) return;

        double angle = alignShot(event);

        //move the basketball to start from bottom left
        basketball.x = basketballRadius;
        basketball.y = screenHeight * 15 / 16;

        //get the x component of the total velocity
        basketballVelocityX = (int) (basketballSpeed * Math.sin(angle));

        //get the y component of the total velocity
        basketballVelocityY = (int) (-basketballSpeed * Math.cos(angle));
        basketballOnScreen = true; // the basketball is on the screen
        basketballHasScored = false;
        ++shotsTaken; // increment shotsTaken


    }// end method shootBasketball



    public double alignShot(MotionEvent event){
        //get the location of the touch in this view
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());


        // compute the touch's distance from bottom left of the screen on the y-axis
        double centerMinusY = (screenHeight - touchPoint.y);

        double angle = 0; //initialize angle to 0

        // calculate the angle the player makes with the horizontal
        if(centerMinusY != 0) //prevent division by 0
            angle = Math.atan((double) touchPoint.x / centerMinusY);

        // if the touch is on the lower half of the screen
        if (touchPoint.y > screenHeight)
            angle += Math.PI; // adjust the angle

        // calculate the endpoint of the player
        playerEnd.x = (int) (playerLength * Math.sin(angle));
        playerEnd.y = (int) (-playerLength * Math.cos(angle) + screenHeight);

        return angle;
    } //end method alignShot

    public void drawGameElements(Canvas canvas)
    {


        if (canvas != null) {


            //draw the background
            canvas.drawBitmap(backgroundBitMap, 0,0, backgroundPaint);

            //display shots taken and time remaining
            canvas.drawText(getResources().getString(R.string.score_time_remaining_format, score, timeLeft), 30, 50, textPaint);


            // if a basketball is currently on the screen, draw it
            if(basketballOnScreen){


                canvas.drawBitmap(basketballBitMap, basketball.x, basketball.y, basketballPaint);

            }

            //draw the player
            canvas.drawLine(0, screenHeight, playerEnd.x, playerEnd.y, playerPaint);


            // draw the backboard
            canvas.drawLine(backBoard.start.x, backBoard.start.y, backBoard.end.x, backBoard.end.y, backboardPaint);

            //draw the front of the rim
            canvas.drawLine(frontRim.start.x, frontRim.start.y, frontRim.end.x, frontRim.end.y, frontRimPaint);

            //draw the point checker
            canvas.drawLine(pointChecker.start.x, pointChecker.start.y, pointChecker.end.x, pointChecker.end.y, pointCheckerPaint);
        }
    }// end method drawGameElements

    //display an AlertDialog when the game ends
    private void showGameOverDialog(final int messageId){
        // DialogFragment to display stats and start new game
        final DialogFragment gameResult =
                new DialogFragment()
                {
                    // create an AlertDialog and return it
                    @Override
                    public Dialog onCreateDialog(Bundle bundle)
                    {
                        // create dialog displaying String resource for messageId
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(getActivity());
                        builder.setTitle(getResources().getString(messageId));

                        // display number of shots taken and total time elapsed
                        builder.setMessage(getResources().getString(
                                R.string.results_format, shotsTaken, score));
                        builder.setPositiveButton(R.string.reset_game,
                                new DialogInterface.OnClickListener()
                                {
                                    // called when "Reset Game" Button is pressed
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        dialogIsDisplayed = false;
                                        startNewGame(); // set up and start a new game
                                    }
                                } // end anonymous inner class
                        ); // end call to setPositiveButton

                        return builder.create(); // return the AlertDialog
                    } // end method onCreateDialog
                }; // end DialogFragment anonymous inner class

        // in GUI thread, use FragmentManager to display the DialogFragment
        activity.runOnUiThread(
                new Runnable() {
                    public void run()
                    {
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false); // modal dialog
                        gameResult.show(activity.getFragmentManager(), "results");
                    }
                } // end Runnable
        );
    }

    // stop the game; may be called by the BasektballGameFragment onPause
    public void stopGame()
    {
        if(shooterThread != null){
            shooterThread.setRunning(false);
        }

    }

    // release resources; may be called by BasektballGameFragment onDestroy
    public void releaseResources()
    {
        // release any resources (e.g. SoundPool stuff)
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    // called when the surface is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // ensure that thread terminates properly
        boolean retry = true;
        shooterThread.setRunning(false); // terminate gameThread

        while (retry)
        {
            try
            {
                shooterThread.join(); // wait for shooterThread to finish
                retry = false;
            }
            catch (InterruptedException e)
            {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {

        shootBasketball(e);


        //}

        return true;
    }

    // Thread subclass to run the main game loop
    private class ShooterThread extends Thread
    {
        private SurfaceHolder surfaceHolder; // for manipulating canvas
        private boolean threadIsRunning = true; // running by default

        // initializes the surface holder
        public ShooterThread(SurfaceHolder holder)
        {
            surfaceHolder = holder;
            setName("ShooterThread");

        }

        // changes running state
        public void setRunning(boolean running)
        {
            threadIsRunning = running;
        }

        @Override
        public void run()
        {
            Canvas canvas = null;
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning)
            {
                try
                {
                    // get Canvas for exclusive drawing from this thread
                    canvas = surfaceHolder.lockCanvas(null);

                    // lock the surfaceHolder for drawing
                    synchronized(surfaceHolder)
                    {


                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        previousFrameTime = currentTime; // update previous time
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(10); // update game state
                        drawGameElements(canvas); // draw using the canvas

                    }

                }
                finally  // regardless if any errors happen...
                {
                    // make sure we unlock canvas so other threads can use it
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
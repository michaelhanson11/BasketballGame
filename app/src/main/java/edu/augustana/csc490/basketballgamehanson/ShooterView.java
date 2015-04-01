// CannonView.java
// Displays and controls the Cannon Game
package edu.augustana.csc490.basketballgamehanson;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ShooterView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = "ShooterView"; // for Log.w(TAG, ...)

    private ShooterThread shooterThread; // runs the main game loop
    private Activity activity; // keep a reference to the main Activity
    private boolean dialogIsDisplayed = false;


    // constants for game play
    public static final int SCORE = 0;

    //variables for the game loop and tracking statistics
    private boolean gameOver = true; // is the game over?
    private double timeLeft; // time remaining in seconds
    private int shotsTaken; // shots the user has taken
    private double totalElapsedTime; // elapsed seconds

    private Line backBoard; // start and end points of backboard
    private int backboardDistance; // target distance from left
    private int backboardBeginning; // target distance from top
    private int backboardEnd; // target bottom's distance from top
    private int initialBackboardVelocity;  // initial target speed multiplier
    private float backboardVelocity; // target speed multiplier
    private int lineWidth; // width of backboard

    //variables for player and basketball
    private Point basketball; //basketball image's upper-left corner
    private int basketballVelocityX; //basketball's x velocity
    private int basketballVelocityY; // basketball's y velocity
    private boolean basketballOnScreen; // is the basketball on the screen?
    private int basketballRadius; // basketball's radius
    private int basketballSpeed; // basketball's speed
    private int basketballFriction;
    private int playerLength; // player's length
    private Point playerEnd; // the endpoint of the player


    // constants and variables for managing sounds
    private static final int BACKBOARD_SOUND_ID = 0;
    private static final int SHOT_SOUND_ID = 1;
    private static final int MAKE_SOUND_ID = 2;
    private SoundPool soundPool; // plays sound effects
    private SparseIntArray soundMap; // maps ID's to SoundPool

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

    private Paint myPaint;


    public ShooterView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        activity = (Activity) context;

        getHolder().addCallback(this);

        //initialize Lines and Point representing game items
        backBoard = new Line();
        basketball = new Point();

        // initialize SoundPool to play the app's sound effects
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        // create Map of sounds and pre-load sounds
        //soundMap = new SparseIntArray(3); // create new hash map
        //soundMap.put(BACKBOARD_SOUND_ID, soundPool.load(context,));
        //soundMap.put(SHOT_SOUND_ID, soundPool.load(context, ));
        //soundMap.put(MAKE_SOUND_ID, soundPool.load(context, ));

        //construct Paints for drawing text, basketball, backboard, and player
        // These are configured in method onSizeChanged
        textPaint = new Paint();
        playerPaint = new Paint();
        basketballPaint = new Paint();
        backboardPaint = new Paint();
        backgroundPaint = new Paint();

        backboardPaint.setColor(Color.BLUE);
        playerPaint.setColor(Color.GREEN);
        basketballPaint.setColor(Color.RED);

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
        basketballSpeed = w * 3 / 2; // basketball speed multiplier
        basketballFriction = 0;

        lineWidth = w / 24; // backboard 1/24 screen width

        // configure instance variables related to the backboard
        backboardDistance = w * 9 / 10; // backboard 9/10 screen width from left
        backboardBeginning = h / 8; // distance from top 1/8 screen height
        backboardEnd = h * 2 / 8; // distance from top 2/8 screen height
        initialBackboardVelocity = h / 4; // initial backboard speed multipler
        backBoard.start = new Point (backboardDistance, backboardBeginning);
        backBoard.end = new Point(backboardDistance, backboardEnd);

        //endpoint of the player initially points horizontally
        playerEnd = new Point(playerLength, h);

        // configure Paint objects for drawing game elements
        textPaint.setTextSize(w / 20); // text size 1/20 of screen width
        textPaint.setAntiAlias(true); // smoothes the text
        playerPaint.setStrokeWidth(lineWidth * 1.5f); // set line thickness
        backboardPaint.setStrokeWidth(lineWidth); // set line thickness
        backgroundPaint.setColor(Color.WHITE); // set background color


        startNewGame();
    }

    public void startNewGame()
    {
        //this.x = 25;
        //this.y = 25;

        backboardVelocity = initialBackboardVelocity;
        timeLeft = 5;
        basketballOnScreen = false;
        shotsTaken = 0;
        totalElapsedTime = 0.0;

        backBoard.start.set(backboardDistance, backboardBeginning);
        backBoard.end.set(backboardDistance, backboardEnd);

        Log.w(TAG,"Starting new game, about to create thread if gameOver is true");
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
            basketball.y += interval * basketballVelocityY;
            //basketball.y += -(Math.pow(interval, 4) * basketballVelocityY);


            // check for collision with backboard
            if (basketball.x + basketballRadius > backboardDistance &&
                    basketball.x - basketballRadius < backboardDistance &&
                    basketball.y + basketballRadius > backBoard.start.y &&
                    basketball.y - basketballRadius < backBoard.end.y) {

                basketballVelocityX *= -1; //reverse the basketball's direction

                //play backboard sound
                //soundPool.play(soundMap.get(BACKBOARD_SOUND_ID), 1, 1, 1, 0, 1f);
                //check for collisions with left and right walls
            } else if (basketball.x + basketballRadius > screenWidth || basketball.x - basketballRadius < 0) {
                basketballOnScreen = false; // remove basketball from screen
                //check for collisions with top and bottom walls
            } else if (basketball.y + basketballRadius > screenHeight || basketball.y - basketballRadius < 0) {
                basketballOnScreen = false;
            }
        }
           // update the backboard's position
           double backboardUpdate = interval * backboardVelocity;
            backBoard.start.y += backboardUpdate;
            backBoard.end.y += backboardUpdate;

            // if the backboard hit the top or bottom, reverse direction
            if (backBoard.start.y < 0 || backBoard.end.y > screenHeight)
                backboardVelocity *= -1;

            timeLeft -= interval;

            //if the timer reached zero
            if (timeLeft <= 0.0)
            {
                timeLeft = 0.0;
                gameOver = true; // the game is over
                shooterThread.setRunning(false);
                stopGame();
                //Log.w(TAG, "launching game over dialog, thread running = " + shooterThread.threadIsRunning);
                showGameOverDialog(R.string.lose); // show the losing dialog
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
        ++shotsTaken; // increment shotsTaken


    }// end method shootBasketball

    public double alignShot(MotionEvent event){
        //get the location of the touch in this view
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());

        // compute the touch's distance from bottom left of the screen on the y-axis
        double centerMinusY = (screenHeight - touchPoint.y);

        double angle = 0; //initialize angle to 0

        // calculate the angle the she makes with the horizontal
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
                //canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
               //canvas.drawCircle(x, y, 20, basketballPaint);

            //clear the background
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);

            //display shots taken and time remaining
            canvas.drawText(getResources().getString(R.string.time_remaining_shots_taken_format, shotsTaken, timeLeft), 30, 50, textPaint);


            // if a basketball is currently on the screen, draw it
            if(basketballOnScreen){
                canvas.drawCircle(basketball.x, basketball.y, basketballRadius, basketballPaint);
            }

            //draw the player
            canvas.drawLine(0, screenHeight, playerEnd.x, playerEnd.y, playerPaint);

            // draw the backboard
            canvas.drawLine(backBoard.start.x, backBoard.start.y, backBoard.end.x, backBoard.end.y, backboardPaint);
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
                                R.string.results_format, shotsTaken));
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
//        if(!dialogIsDisplayed){
//            shooterThread = new ShooterThread(holder); // create thread
//            shooterThread.setRunning(true); // start game running
//            shooterThread.start(); // start the game loop thread
//        }

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
                shooterThread.join(); // wait for gameThread to finish
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
        //int action = e.getAction();
        //if (e.getAction() == MotionEvent.ACTION_DOWN)
        //{
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
            Log.e(TAG,"creating thread");
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
                        //gameStep();         // update game state
                        //drawGameElements(canvas); // draw using the canvas
                        long currentTime = System.currentTimeMillis();
                        double elapsedTimeMS = currentTime - previousFrameTime;
                        previousFrameTime = currentTime; // update previous time
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(10); // update game state
                        drawGameElements(canvas); // draw using the canvas
                        //Log.w("ShooterThread", "thread running = " + threadIsRunning);
                    }
                    //Thread.sleep(10); // if you want to slow down the action...
                //} catch (InterruptedException ex) {
                //    Log.e(TAG,ex.toString());
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
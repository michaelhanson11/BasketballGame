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

    private GameThread shooterThread; // runs the main game loop
    private Activity mainActivity; // keep a reference to the main Activity
    private boolean dialogIsDisplayed = false;


    // constants for game play
    public static final int SCORE = 0;

    //variables for the game loop and tracking statistics
    private boolean gameOver; // is the game over?
    private double timeLeft; // time remaining in seconds
    private int shotsTaken; // shots the user has taken
    private double totalElapsedTime; // elapsed seconds

    private Line backBoard; // start and end points of backboard
    private int targetDistance; // target distance from left
    private int targetBeginning; // target distance from top
    private int targetEnd; // target bottom's distance from top
    private int initialTargetVelocity;  // initial target speed multiplier
    private float targetVelocity; // target speed multiplier
    private int lineWidth; // width of backboard

    //variables for player and basketball
    private Point basketball; //basketball image's upper-left corner
    private int basketballVelocityX; //basketball's x velocity
    private int basketballVelocityY; // basketball's y velocity
    private boolean basketballOnScreen; // is the basketball on the screen?
    private int basketballRadius; // basketball's radius
    private int basketballSpeed; // basketball's speed
    private int playerLength; // player's length
    private Point playerEnd; // the endpoint of the player


    // constants and variables for managing sounds
    private static final int BACKBOARD_SOUND_ID = 0;
    private static final int SHOT_SOUND_ID = 1;
    private static final int MAKE_SOUND_ID = 2;
    private SoundPool soundPool; // plays sound effects
    private SparseIntArray soundMap; // maps ID's to SoundPool

    private int x;
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
        mainActivity = (Activity) context;

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
        backgroundPaint.setColor(Color.GREEN);
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

        lineWidth = w / 24; // backboard 1/24 screen width

        // configure instance variables related to the backboard
        targetDistance = w * 7 / 8; // backboard 7/8 screen width from left
        targetBeginning = h / 2; // distance from top 1/8 screen height
        targetEnd = h * 7 / 8; // distance from top 7/8 screen height
       // initialTargetVelocity = -h / 4; // initial backboard speed multipler
        backBoard.start = new Point (targetDistance, targetBeginning);
        backBoard.end = new Point(targetDistance, targetEnd);

        // configure Paint objects for drawing game elements
        textPaint.setTextSize(w / 20); // text size 1/20 of screen width
        textPaint.setAntiAlias(true); // smoothes the text
        playerPaint.setStrokeWidth(lineWidth * 1.5f); // set line thickness
        backboardPaint.setStrokeWidth(lineWidth); // set line thickness
        backgroundPaint.setColor(Color.WHITE); // set background color
        //not working

        startNewGame();
    }

    public void startNewGame()
    {
        this.x = 25;
        this.y = 25;

        if (gameOver)
        {
            gameOver = false;
            shooterThread = new GameThread(getHolder());
            shooterThread.start(); // start the main game loop going
        }
    }


    private void gameStep()
    {
        x++;
    }

    public void updateView(Canvas canvas)
    {
        if (canvas != null) {
            //canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), backgroundPaint);
            canvas.drawCircle(x, y, 20, basketballPaint);
        }
    }

    // stop the game; may be called by the BasektballGameFragment onPause
    public void stopGame()
    {
        if (shooterThread != null)
            shooterThread.setRunning(false);
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
        if (e.getAction() == MotionEvent.ACTION_DOWN)
        {
            this.x = (int) e.getX();
            this.y = (int) e.getY();
        }

        return true;
    }

    // Thread subclass to run the main game loop
    private class GameThread extends Thread
    {
        private SurfaceHolder surfaceHolder; // for manipulating canvas
        private boolean threadIsRunning = true; // running by default

        // initializes the surface holder
        public GameThread(SurfaceHolder holder)
        {
            surfaceHolder = holder;
            setName("GameThread");
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

            while (threadIsRunning)
            {
                try
                {
                    // get Canvas for exclusive drawing from this thread
                    canvas = surfaceHolder.lockCanvas(null);

                    // lock the surfaceHolder for drawing
                    synchronized(surfaceHolder)
                    {
                        gameStep();         // update game state
                        updateView(canvas); // draw using the canvas
                    }
                    Thread.sleep(10); // if you want to slow down the action...
                } catch (InterruptedException ex) {
                    Log.e(TAG,ex.toString());
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
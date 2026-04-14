package com.example.snake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

public class SnakeView extends SurfaceView implements Runnable {
    private static final String PREFS_NAME = "snake_prefs";
    private static final String KEY_HIGH_SCORE = "high_score";

    private Thread thread;
    private volatile boolean isPlaying;
    private final SurfaceHolder surfaceHolder;
    private final Paint paint = new Paint();
    private final Random random = new Random();

    private final int screenX, screenY;
    private final int blockSize;
    private final int numBlocksWide = 20;
    private final int numBlocksHigh;

    private int snakeLength = 1;
    private final int[] snakeX = new int[200];
    private final int[] snakeY = new int[200];

    private int foodX, foodY;
    private int score = 0;
    private int highScore = 0;
    private final SharedPreferences prefs;

    private enum Direction { UP, RIGHT, DOWN, LEFT }
    private Direction direction = Direction.RIGHT;

    private long nextFrameTime;
    private final long baseFrameDelay = 120;
    private final long minFrameDelay = 50;
    private final int speedStep = 5;
    private long frameDelay = baseFrameDelay;

    private boolean gameOverPause = false;

    public SnakeView(Context context, Point size) {
        super(context);
        screenX = size.x;
        screenY = size.y;
        blockSize = screenX / numBlocksWide;
        numBlocksHigh = screenY / blockSize;
        surfaceHolder = getHolder();

        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        highScore = prefs.getInt(KEY_HIGH_SCORE, 0);

        newGame();
    }

    private void newGame() {
        snakeLength = 1;
        snakeX[0] = numBlocksWide / 2;
        snakeY[0] = numBlocksHigh / 2;
        direction = Direction.RIGHT;
        score = 0;
        frameDelay = baseFrameDelay;
        gameOverPause = false;
        spawnFood();
        nextFrameTime = System.currentTimeMillis();
    }

    private void spawnFood() {
        boolean valid;
        do {
            valid = true;
            foodX = random.nextInt(numBlocksWide);
            foodY = random.nextInt(numBlocksHigh);

            for (int i = 0; i < snakeLength; i++) {
                if (foodX == snakeX[i] && foodY == snakeY[i]) {
                    valid = false;
                    break;
                }
            }
        } while (!valid);
    }

    private void moveSnake() {
        for (int i = snakeLength - 1; i > 0; i--) {
            snakeX[i] = snakeX[i - 1];
            snakeY[i] = snakeY[i - 1];
        }

        switch (direction) {
            case UP: snakeY[0]--; break;
            case RIGHT: snakeX[0]++; break;
            case DOWN: snakeY[0]++; break;
            case LEFT: snakeX[0]--; break;
        }

        if (snakeX[0] < 0) snakeX[0] = numBlocksWide - 1;
        if (snakeX[0] >= numBlocksWide) snakeX[0] = 0;
        if (snakeY[0] < 0) snakeY[0] = numBlocksHigh - 1;
        if (snakeY[0] >= numBlocksHigh) snakeY[0] = 0;
    }

    private void eatFood() {
        snakeLength++;
        score++;
        if (score > highScore) {
            highScore = score;
            prefs.edit().putInt(KEY_HIGH_SCORE, highScore).apply();
        }
        frameDelay = Math.max(minFrameDelay, baseFrameDelay - (score / speedStep) * 10);
        spawnFood();
    }

    private boolean isDead() {
        for (int i = snakeLength - 1; i > 0; i--) {
            if (snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) return true;
        }
        return false;
    }

    private void update() {
        if (snakeX[0] == foodX && snakeY[0] == foodY) {
            eatFood();
        }
        moveSnake();
        if (isDead()) {
            gameOverPause = true;
        }
    }

    private boolean updateRequired() {
        if (nextFrameTime <= System.currentTimeMillis()) {
            nextFrameTime = System.currentTimeMillis() + frameDelay;
            return true;
        }
        return false;
    }

    private void drawGame() {
        if (surfaceHolder.getSurface().isValid()) {
            Canvas canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.BLACK);

            paint.setColor(Color.WHITE);
            paint.setTextSize(60);
            canvas.drawText("Score: " + score, 20, 70, paint);
            canvas.drawText("High: " + highScore, 20, 130, paint);

            paint.setColor(Color.GREEN);
            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(
                        snakeX[i] * blockSize,
                        snakeY[i] * blockSize,
                        (snakeX[i] * blockSize) + blockSize,
                        (snakeY[i] * blockSize) + blockSize,
                        paint
                );
            }

            paint.setColor(Color.RED);
            canvas.drawRect(
                    foodX * blockSize,
                    foodY * blockSize,
                    (foodX * blockSize) + blockSize,
                    (foodY * blockSize) + blockSize,
                    paint
            );

            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void run() {
        while (isPlaying) {
            if (updateRequired()) {
                update();
                drawGame();

                if (gameOverPause) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException ignored) {}
                    newGame();
                }
            }
        }
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException ignored) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getX() >= screenX / 2) {
                if (direction == Direction.UP) direction = Direction.RIGHT;
                else if (direction == Direction.RIGHT) direction = Direction.DOWN;
                else if (direction == Direction.DOWN) direction = Direction.LEFT;
                else direction = Direction.UP;
            } else {
                if (direction == Direction.UP) direction = Direction.LEFT;
                else if (direction == Direction.LEFT) direction = Direction.DOWN;
                else if (direction == Direction.DOWN) direction = Direction.RIGHT;
                else direction = Direction.UP;
            }
        }
        return true;
    }
}

package com.Array;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Arrays;

public class Main implements ApplicationListener {

    private ShapeRenderer sr;
    private SpriteBatch batch;
    private BitmapFont font;

    // Sound
    private Sound randomSound;
    private float soundTimer = 0;
    private float nextSoundTime;
    private Music endSound;
    private Sound coinSound;
    private boolean endSoundPlayed = false;
    // Game timer
    private float gameTimer = 30;
    private boolean gameOver = false;

    // Firebase
    private boolean scoreRequested = false;

    private final String projectId = "supercalifragilisticexpialadoc";
    private final String apiKey = "YOUR_FIREBASE_API_KEY";
    private final String collection = "first%20game";
    private final String documentId = "scores";

    // Grid: 0 = Empty, 1 = Player, 2 = Coin
    private final int[][] grid = new int[5][5];

    private int r = 2;
    private int c = 2;
    private int coinsCollected = 0;

    @Override
    public void create() {

        sr = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        randomSound = Gdx.audio.newSound(
            Gdx.files.internal("high-decibel-alarm.mp3")
        );
        endSound = Gdx.audio.newMusic(
            Gdx.files.internal("extremely-loud-scream-alarm.mp3")
        );

        endSound.setLooping(false);
        endSound.setVolume(1f);
        coinSound = Gdx.audio.newSound(
            Gdx.files.internal("extremely-loud-blast-caution-intense-noise.mp3")
        );


        resetGame();
    }

    // --------------------------------
    // RESET GAME
    // --------------------------------

    private void resetGame() {

        // Clear the grid
        for (int[] ints : grid) {
            Arrays.fill(ints, 0);
        }

        // Reset player
        r = 2;
        c = 2;
        grid[r][c] = 1;

        // Reset score and timer
        coinsCollected = 0;
        gameTimer = 30;
        gameOver = false;

        // Reset sound
        soundTimer = 0;
        nextSoundTime = MathUtils.random(5f, 15f);

        if (randomSound != null) {
            randomSound.stop();
        }

        // Allow a new Firebase request when this round ends
        scoreRequested = false;

        // Spawn new coins
        spawnCoin();
        spawnCoin();
    }

    // --------------------------------
    // SPAWN COIN
    // --------------------------------

    private void spawnCoin() {

        // Count available empty spaces
        int emptySpaces = 0;

        for (int[] ints : grid) {
            for (int anInt : ints) {

                if (anInt == 0) {
                    emptySpaces++;
                }
            }
        }

        // Don't spawn if there are no empty spaces
        if (emptySpaces == 0) {
            return;
        }

        int randomRow;
        int randomCol;

        do {
            randomRow = MathUtils.random(0, grid.length - 1);
            randomCol = MathUtils.random(0, grid[0].length - 1);

        } while (grid[randomRow][randomCol] != 0);

        grid[randomRow][randomCol] = 2;
    }

    // --------------------------------
    // RENDER
    // --------------------------------

    @Override
    public void render() {

        float delta = Gdx.graphics.getDeltaTime();

        // --------------------------------
        // RESTART WITH SPACE
        // --------------------------------

        if (gameOver && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            resetGame();
            endSound.stop();
            endSoundPlayed = false;
        }

        // --------------------------------
        // GAME TIMER
        // --------------------------------

        if (!gameOver) {

            gameTimer -= delta;

            if (gameTimer <= 0) {
                gameTimer = 0;
                gameOver = true;

                // Request Firebase score only once
                if (!scoreRequested) {
                    scoreRequested = true;
                    getScoreFromFirebase(coinsCollected);
                }
            }
        }

        // --------------------------------
        // RANDOM SOUND
        // --------------------------------

        if (!gameOver) {

            soundTimer += delta;

            if (soundTimer >= nextSoundTime) {

                randomSound.play(100.0f);

                soundTimer = 0;
                nextSoundTime = MathUtils.random(5f, 15f);
            }
        }

        // --------------------------------
        // PLAYER MOVEMENT
        // --------------------------------

        if (!gameOver) {

            int nr = r;
            int nc = c;

            if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
                nr--;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
                nr++;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
                nc--;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
                nc++;
            }

            // Check grid boundaries
            if (nr >= 0 && nr < grid.length &&
                nc >= 0 && nc < grid[0].length) {

                // Collect coin
                if (grid[nr][nc] == 2) {

                    coinsCollected++;
                    grid[nr][nc] = 0;
                    coinSound.play(100.0f);
                    spawnCoin();

                }
                keepCoinsOnGrid();
                // Move player
                grid[r][c] = 0;

                r = nr;
                c = nc;

                grid[r][c] = 1;
            }
        }

        // --------------------------------
        // CLEAR SCREEN
        // --------------------------------

        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // --------------------------------
        // DRAW GRID
        // --------------------------------

        if (!gameOver) {

            sr.begin(ShapeRenderer.ShapeType.Filled);

            for (int i = 0; i < grid.length; i++) {

                for (int j = 0; j < grid[i].length; j++) {

                    if (grid[i][j] == 1) {
                        sr.setColor(Color.FIREBRICK);
                    } else if (grid[i][j] == 2) {
                        sr.setColor(Color.GOLD);
                    } else {
                        sr.setColor(Color.DARK_GRAY);
                    }

                    sr.rect(
                        100 + j * 80,
                        50 + (4 - i) * 80,
                        76,
                        76
                    );
                }
            }

            sr.end();
        }

        // --------------------------------
        // DRAW TEXT
        // --------------------------------

        batch.begin();

        if (!gameOver) {

            font.getData().setScale(2);
            font.setColor(Color.WHITE);

            font.draw(batch, "Time: " + (int) gameTimer, 100, 450);
            font.draw(batch, "Coins: " + coinsCollected, 350, 450);

        } else {

            // GAME OVER SCREEN
            if (!endSoundPlayed) {
                endSound.play();
                endSoundPlayed = true;
            }
            font.setColor(Color.RED);
            font.getData().setScale(3);


            font.draw(
                batch,
                "GAME OVER",
                Gdx.graphics.getWidth() / 2f - 150,
                Gdx.graphics.getHeight() / 2f + 40
            );

            font.setColor(Color.WHITE);
            font.getData().setScale(2);

            font.draw(
                batch,
                "Coins Collected: " + coinsCollected,
                Gdx.graphics.getWidth() / 2f - 150,
                Gdx.graphics.getHeight() / 2f - 20
            );

            font.getData().setScale(1.5f);

            font.draw(
                batch,
                "Press SPACE to Restart",
                Gdx.graphics.getWidth() / 2f - 130,
                Gdx.graphics.getHeight() / 2f - 80
            );
        }

        batch.end();
    }
    public void keepCoinsOnGrid() {
        int coinCount = 0;

        for (int[] ints : grid) {
            for (int anInt : ints) {
                if (anInt == 2) {
                    coinCount++;
                }
            }
        }

        while (coinCount < 2) {
            spawnCoin();
            coinCount++;
        }
    }
    // --------------------------------
    // FIREBASE GET SCORE
    // --------------------------------

    public void getScoreFromFirebase(int finalScore) {

        String url = "https://firestore.googleapis.com/v1/projects/"
            + projectId
            + "/databases/(default)/documents/"
            + collection
            + "/"
            + documentId
            + "?key="
            + apiKey;

        Net.HttpRequest request =
            new Net.HttpRequest(Net.HttpMethods.GET);

        request.setUrl(url);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {

                HttpStatus status = httpResponse.getStatus();

                if (status.getStatusCode() == 200) {

                    JsonValue base = new JsonReader().parse(
                        httpResponse.getResultAsString()
                    );

                    JsonValue fields = base.get("fields");

                    int first = Integer.parseInt(
                        fields.get("High score").getString("integerValue")
                    );

                    int second = Integer.parseInt(
                        fields.get("#2 Score").getString("integerValue")
                    );

                    int third = Integer.parseInt(
                        fields.get("#3 score").getString("integerValue")
                    );

                    // Check where the new score belongs
                    if (finalScore > first) {
                        third = second;
                        second = first;
                        first = finalScore;
                    }
                    else if (finalScore > second) {
                        third = second;
                        second = finalScore;
                    }
                    else if (finalScore > third) {
                        third = finalScore;
                    }

                    // Save the updated leaderboard
                    updateScoreInFirebase(first, second, third);

                } else {
                    Gdx.app.log("Firebase", "GET Error: "
                        + status.getStatusCode()
                        + " - " + httpResponse.getResultAsString());
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("Firebase", "Connection Error", t);
            }

            @Override
            public void cancelled() {
                Gdx.app.log("Firebase", "Request Cancelled");
            }
        });
    }

    // --------------------------------
    // FIREBASE UPDATE SCORE
    // --------------------------------

    public void updateScoreInFirebase(int first, int second, int third) {

        String url = "https://firestore.googleapis.com/v1/projects/"
            + projectId
            + "/databases/(default)/documents/"
            + collection
            + "/"
            + documentId
            + "?key="
            + apiKey
            + "&updateMask.fieldPaths=%60High%20score%60"
            + "&updateMask.fieldPaths=%60%232%20Score%60"
            + "&updateMask.fieldPaths=%60%233%20score%60";

        Net.HttpRequest request =
            new Net.HttpRequest(Net.HttpMethods.POST);

        request.setUrl(url);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("X-HTTP-Method-Override", "PATCH");

        String jsonBody =
            "{"
                + "\"fields\": {"
                + "\"High score\": { \"integerValue\": \"" + first + "\" },"
                + "\"#2 Score\": { \"integerValue\": \"" + second + "\" },"
                + "\"#3 score\": { \"integerValue\": \"" + third + "\" }"
                + "}"
                + "}";

        request.setContent(jsonBody);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {

            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {

                if (httpResponse.getStatus().getStatusCode() == 200) {
                    Gdx.app.log("Firebase", "Leaderboard updated! \n 1st score:"+first+"\n 2nd score: "+second+"\n 3rd Score: "+third);
                } else {
                    Gdx.app.log("Firebase", "Update Error: "
                        + httpResponse.getStatus().getStatusCode()
                        + " - " + httpResponse.getResultAsString());
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("Firebase", "Connection Error", t);
            }

            @Override
            public void cancelled() {
                Gdx.app.log("Firebase", "Request Cancelled");
            }
        });
    }

    // --------------------------------
    // OTHER METHODS
    // --------------------------------

    @Override
    public void resize(int w, int h) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {

        sr.dispose();
        batch.dispose();
        font.dispose();

        if (randomSound != null) {
            randomSound.dispose();
        }
    }
}

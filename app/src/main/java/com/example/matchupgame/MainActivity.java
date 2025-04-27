package com.example.matchupgame;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private boolean isSoundOn = true;
    private ImageButton btnMute;
    private int moveCount = 0;
    private TextView txtMoveCounter;
    private TextView txtTimer;

    private GridView gridView;
    private CardAdapter adapter;
    private List<MemoryCard> cards;

    private MemoryCard selectedCard1 = null;
    private MemoryCard selectedCard2 = null;
    private View viewCard1 = null;
    private View viewCard2 = null;
    private Handler handler = new Handler();

    private boolean isBusy = false;

    private int secondsElapsed = 0;
    private boolean isTimerRunning = false;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private SoundPool soundPool;
    private int soundFlip, soundMatch, soundMismatch, soundWin;

    int[] images = {
            R.drawable.sage, R.drawable.viper,
            R.drawable.reyna, R.drawable.killjoy,
            R.drawable.jet, R.drawable.neon
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = findViewById(R.id.gridView);
        txtMoveCounter = findViewById(R.id.txtMoveCounter);
        txtTimer = findViewById(R.id.txtTimer);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();

        soundFlip = soundPool.load(this, R.raw.flip, 1);
        soundMatch = soundPool.load(this, R.raw.match, 1);
        soundMismatch = soundPool.load(this, R.raw.mismatch, 1);
        soundWin = soundPool.load(this, R.raw.win, 1);

        setupGame();

        ImageButton btnReset = findViewById(R.id.btnReset);


        btnReset.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_exit, null);

            TextView title = dialogView.findViewById(R.id.dialogTitle);
            TextView message = dialogView.findViewById(R.id.dialogMessage);
            title.setText("Restart Game");
            message.setText("Are you sure you want to restart this game?\nYour current progress will be lost.");

            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            dialogView.findViewById(R.id.btnConfirmYes).setOnClickListener(btn -> {
                dialog.dismiss();
                setupGame();
            });

            dialogView.findViewById(R.id.btnConfirmCancel).setOnClickListener(btn -> dialog.dismiss());

            dialog.show();
        });


        btnMute = findViewById(R.id.btnMute);
        btnMute.setOnClickListener(v -> {
            isSoundOn = !isSoundOn;
            btnMute.setImageResource(isSoundOn ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
        });

        ImageButton btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_exit, null);

            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            dialogView.findViewById(R.id.btnConfirmYes).setOnClickListener(btn -> {
                dialog.dismiss();
                Intent intent = new Intent(this, TitleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });

            dialogView.findViewById(R.id.btnConfirmCancel).setOnClickListener(btn -> dialog.dismiss());

            dialog.show();
        });


    }

    private void setupGame() {
        stopTimer();
        secondsElapsed = 0;
        isTimerRunning = false;
        txtTimer.setText("Time: 0s");

        moveCount = 0;
        updateMoveCounter();

        cards = new ArrayList<>();
        for (int img : images) {
            cards.add(new MemoryCard(img));
            cards.add(new MemoryCard(img));
        }
        Collections.shuffle(cards);
        adapter = new CardAdapter(this, cards);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (isBusy) return;

            MemoryCard tappedCard = cards.get(position);
            if (tappedCard.isFaceUp() || tappedCard.isMatched()) return;

            tappedCard.setFaceUp(true);

            if (isSoundOn) {
                soundPool.play(soundFlip, 1, 1, 0, 0, 1);
            }

            adapter.flipCard(view, true, tappedCard.getImageId());

            if (selectedCard1 == null) {
                selectedCard1 = tappedCard;
                viewCard1 = view;

                if (!isTimerRunning) {
                    startTimer();
                }
            } else if (selectedCard2 == null) {
                selectedCard2 = tappedCard;
                viewCard2 = view;

                moveCount++;
                updateMoveCounter();

                isBusy = true;

                handler.postDelayed(() -> {
                    checkMatch();
                    isBusy = false;
                }, 800);
            }
        });

        selectedCard1 = null;
        selectedCard2 = null;
        viewCard1 = null;
        viewCard2 = null;
    }

    private void checkMatch() {
        if (selectedCard1 != null && selectedCard2 != null) {
            if (selectedCard1.getImageId() == selectedCard2.getImageId()) {
                selectedCard1.setMatched(true);
                selectedCard2.setMatched(true);
                if (isSoundOn) {
                    soundPool.play(soundMatch, 1, 1, 0, 0, 1);
                }
            } else {
                selectedCard1.setFaceUp(false);
                selectedCard2.setFaceUp(false);
                if (isSoundOn) {
                    soundPool.play(soundMismatch, 1, 1, 0, 0, 1);
                }

                if (viewCard1 != null) adapter.flipCard(viewCard1, false, 0);
                if (viewCard2 != null) adapter.flipCard(viewCard2, false, 0);
            }
        }

        selectedCard1 = null;
        selectedCard2 = null;
        viewCard1 = null;
        viewCard2 = null;

        if (allCardsMatched()) {
            isBusy = true;
            gridView.setEnabled(false);
            stopTimer();

            if (isSoundOn) {
                soundPool.play(soundWin, 1, 1, 0, 0, 1);
            }

            handler.postDelayed(this::showVictoryDialog, 300); // delayed dialog for smoothness
        }
    }

    private void showVictoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_victory, null);
        TextView txtDetails = dialogView.findViewById(R.id.txtVictoryDetails);
        txtDetails.setText("Moves: " + moveCount + "\nTime: " + secondsElapsed + "s");

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btnPlayAgain).setOnClickListener(v -> {
            dialog.dismiss();
            setupGame();
            gridView.setEnabled(true);
            isBusy = false;
        });

        dialogView.findViewById(R.id.btnExit).setOnClickListener(v -> finish());

        dialog.show();
    }


    private boolean allCardsMatched() {
        for (MemoryCard card : cards) {
            if (!card.isMatched()) {
                return false;
            }
        }
        return true;
    }

    private void updateMoveCounter() {
        txtMoveCounter.setText("Moves: " + moveCount);
    }

    private void startTimer() {
        isTimerRunning = true;
        secondsElapsed = 0;
        txtTimer.setText("Time: 0s");

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                txtTimer.setText("Time: " + secondsElapsed + "s");
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}

package com.example.matchupgame;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import java.util.List;

public class CardAdapter extends BaseAdapter {
    private Context context;
    private List<MemoryCard> cards;

    public CardAdapter(Context context, List<MemoryCard> cards) {
        this.context = context;
        this.cards = cards;
    }

    @Override
    public int getCount() {
        return cards.size();
    }

    @Override
    public Object getItem(int position) {
        return cards.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    static class ViewHolder {
        View cardFront;
        View cardBack;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View cardView = convertView;
        ViewHolder holder;

        if (cardView == null) {
            cardView = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
            holder = new ViewHolder();
            holder.cardFront = cardView.findViewById(R.id.cardFront);
            holder.cardBack = cardView.findViewById(R.id.cardBack);

            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int cardWidth = screenWidth / 3 - 32;
            int cardHeight = (int) (cardWidth * 1.4);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(cardWidth, cardHeight);
            holder.cardFront.setLayoutParams(params);
            holder.cardBack.setLayoutParams(params);

            cardView.setTag(holder);
        } else {
            holder = (ViewHolder) cardView.getTag();
        }

        MemoryCard card = cards.get(position);

        if (card.isFaceUp() || card.isMatched()) {
            if (holder.cardFront.getVisibility() != View.VISIBLE) {
                holder.cardFront.setBackgroundResource(card.getImageId());
                holder.cardFront.setVisibility(View.VISIBLE);
                holder.cardBack.setVisibility(View.GONE);
            }
        } else {
            if (holder.cardBack.getVisibility() != View.VISIBLE) {
                holder.cardBack.setVisibility(View.VISIBLE);
                holder.cardFront.setVisibility(View.GONE);
            }
        }


        return cardView;
    }

    public void flipCard(View cardView, boolean isShowingImage, int imageId) {
        View cardFront = cardView.findViewById(R.id.cardFront);
        View cardBack = cardView.findViewById(R.id.cardBack);

        cardView.animate().rotationY(90f).setDuration(150).withEndAction(() -> {
            if (isShowingImage) {
                cardFront.setBackgroundResource(imageId);
                cardFront.setVisibility(View.VISIBLE);
                cardBack.setVisibility(View.GONE);
            } else {
                cardBack.setVisibility(View.VISIBLE);
                cardFront.setVisibility(View.GONE);
            }
            cardView.setRotationY(-90f);
            cardView.animate().rotationY(0f).setDuration(150).start();
        }).start();
    }
}

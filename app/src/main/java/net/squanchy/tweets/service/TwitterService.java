package net.squanchy.tweets.service;

import java.util.List;

import net.squanchy.tweets.domain.view.HashtagEntity;
import net.squanchy.tweets.domain.view.MentionEntity;
import net.squanchy.tweets.domain.view.Tweet;
import net.squanchy.tweets.domain.view.UrlEntity;
import net.squanchy.tweets.domain.view.User;

import io.reactivex.Observable;

import static net.squanchy.support.lang.Lists.filter;
import static net.squanchy.support.lang.Lists.map;

public class TwitterService {

    private final TwitterRepository repository;

    public TwitterService(TwitterRepository repository) {
        this.repository = repository;
    }

    public Observable<List<Tweet>> refresh(String query) {
        return repository.load(query)
                .map(search -> search.tweets)
                .map(list -> filter(list, tweet -> tweet.retweetedStatus == null))
                .map(tweets -> map(tweets, this::toViewModel));
    }

    private Tweet toViewModel(com.twitter.sdk.android.core.models.Tweet tweet) {
        return Tweet.builder()
                .id(tweet.id)
                .text(displayableTextFor(tweet))
                .createdAt(tweet.createdAt)
                .user(User.create(tweet.user.name, tweet.user.screenName, tweet.user.profileImageUrl))
                .hashtags(parseHashtags(tweet.entities.hashtags))
                .mentions(parseMentions(tweet.entities.userMentions))
                .urls(parseUrls(tweet.entities.urls))
                .build();
    }

    private String displayableTextFor(com.twitter.sdk.android.core.models.Tweet tweet) {
        List<Integer> displayTextRange = tweet.displayTextRange;
        if (displayTextRange.size() != 2) {
            return tweet.text;
        }
        Integer beginIndex = displayTextRange.get(0);
        Integer endIndex = displayTextRange.get(1);
        return tweet.text.substring(beginIndex, endIndex);
    }

    private List<HashtagEntity> parseHashtags(List<com.twitter.sdk.android.core.models.HashtagEntity> entities) {
        return map(entities, entity -> HashtagEntity.create(entity.text, entity.getStart(), entity.getEnd()));
    }

    private List<MentionEntity> parseMentions(List<com.twitter.sdk.android.core.models.MentionEntity> entities) {
        return map(entities, entity -> MentionEntity.create(entity.screenName, entity.getStart(), entity.getEnd()));
    }

    private List<UrlEntity> parseUrls(List<com.twitter.sdk.android.core.models.UrlEntity> entities) {
        return map(entities, entity -> UrlEntity.create(entity.url, entity.getStart(), entity.getEnd()));
    }
}

package mflix.api.daos;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import mflix.api.models.Comment;
import mflix.api.models.Critic;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.bson.codecs.configuration.CodecRegistries.*;

@Component
public class CommentDao extends AbstractMFlixDao {

    public static String COMMENT_COLLECTION = "comments";

    private MongoCollection<Comment> commentCollection;

    private CodecRegistry pojoCodecRegistry;

    private final Logger log;

    @Autowired
    public CommentDao(
            MongoClient mongoClient, @Value("${spring.mongodb.database}") String databaseName) {
        super(mongoClient, databaseName);
        log = LoggerFactory.getLogger(this.getClass());
        this.db = this.mongoClient.getDatabase(MFLIX_DATABASE);
        this.pojoCodecRegistry =
                fromRegistries(
                        MongoClientSettings.getDefaultCodecRegistry(),
                        fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        this.commentCollection =
                db.getCollection(COMMENT_COLLECTION, Comment.class).withCodecRegistry(pojoCodecRegistry);
    }

    /**
     * Returns a Comment object that matches the provided id string.
     *
     * @param id - comment identifier
     * @return Comment object corresponding to the identifier value
     */
    public Comment getComment(String id) {
        return commentCollection.find(new Document("_id", new ObjectId(id))).first();
    }

    /**
     * Adds a new Comment to the collection. The equivalent instruction in the mongo shell would be:
     *
     * <p>db.comments.insertOne({comment})
     *
     * <p>
     *
     * @param comment - Comment object.
     * @throw IncorrectDaoOperation if the insert fails, otherwise
     * returns the resulting Comment object.
     */
    public Comment addComment(Comment comment) {
        if (comment.getId() == null)
            return null;
        commentCollection.insertOne(comment);
        // TODO> Ticket - Handling Errors: Implement a try catch block to
        // handle a potential write exception when given a wrong commentId.
        return comment;
    }

    /**
     * Updates the comment text matching commentId and user email. This method would be equivalent to
     * running the following mongo shell command:
     *
     * <p>db.comments.update({_id: commentId}, {$set: { "text": text, date: ISODate() }})
     *
     * <p>
     *
     * @param commentId - comment id string value.
     * @param text      - comment text to be updated.
     * @param email     - user email.
     * @return true if successfully updates the comment text.
     */
    public boolean updateComment(String commentId, String text, String email) {
        Bson commentFilter = Filters.eq("_id", new ObjectId(commentId));
        Comment comment = commentCollection.find(commentFilter).first();
        Document newComment = new Document("text", text).append("date", new Date());

        if (comment != null && Objects.equals(comment.getEmail(), email)) {
            return commentCollection.updateOne(commentFilter, new Document("$set", newComment)).wasAcknowledged();
        }

        // TODO> Ticket - Handling Errors: Implement a try catch block to
        // handle a potential write exception when given a wrong commentId.
        return false;
    }

    /**
     * Deletes comment that matches user email and commentId.
     *
     * @param commentId - commentId string value.
     * @param email     - user email value.
     * @return true if successful deletes the comment.
     */
    public boolean deleteComment(String commentId, String email) {
        Bson commentFilter = Filters.eq("_id", new ObjectId(commentId));
        Comment comment = commentCollection.find(commentFilter).first();

        if (comment != null && Objects.equals(comment.getEmail(), email)) {
            return commentCollection.deleteOne(commentFilter).wasAcknowledged();
        }

        // TODO> Ticket Handling Errors - Implement a try catch block to
        // handle a potential write exception when given a wrong commentId.
        return false;
    }

    /**
     * Ticket: User Report - produce a list of users that comment the most in the website. Query the
     * `comments` collection and group the users by number of comments. The list is limited to up most
     * 20 commenter.
     *
     * @return List {@link Critic} objects.
     */
    public List<Critic> mostActiveCommenters() {
        List<Critic> mostActive = new ArrayList<>();
        CriticCodec criticCodec = new CriticCodec();
        CodecRegistry codecRegistry =
                fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromCodecs(criticCodec));
        List<Bson> pipeline = new ArrayList<>();
        MongoCollection<Critic> criticCollection = db.getCollection(COMMENT_COLLECTION, Critic.class).withCodecRegistry(codecRegistry);

        Bson sortStage = Aggregates.sort(Sorts.descending("count"));
        Bson limitStage = Aggregates.limit(20);
        BsonField sum1 = Accumulators.sum("count", 1);
        Bson groupStage = Aggregates.group("$email", sum1);

        pipeline.add(groupStage);
        pipeline.add(sortStage);
        pipeline.add(limitStage);

        criticCollection.withReadConcern(ReadConcern.MAJORITY)
                .aggregate(pipeline).iterator().forEachRemaining(mostActive::add);

        return mostActive;
    }
}

package mflix.api.daos;

import mflix.api.models.Critic;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.*;
import org.bson.types.ObjectId;

public class CriticCodec implements CollectibleCodec<Critic> {

    private final Codec<Document> documentCodec;

    public CriticCodec() {
        super();
        this.documentCodec = new DocumentCodec();
    }

    public void encode(BsonWriter bsonWriter, Critic critic, EncoderContext encoderContext) {
        Document criticDoc = new Document();
//        String criticId = critic.getId();
//        int numComments = critic.getNumComments();
//
//        if (null != criticId) {
//            criticDoc.put("_id", new ObjectId(criticId));
//        }
//
//        if (0 != numComments) {
//            criticDoc.put("num_movies", numComments);
//        }

        documentCodec.encode(bsonWriter, criticDoc, encoderContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Critic decode(BsonReader bsonReader, DecoderContext decoderContext) {
        Document criticDoc = documentCodec.decode(bsonReader, decoderContext);
        Critic critic = new Critic();
        critic.setId(criticDoc.getString("_id"));
        critic.setNumComments(criticDoc.getInteger("count"));
        return critic;
    }

    @Override
    public Class<Critic> getEncoderClass() {
        return Critic.class;
    }

    @Override
    public Critic generateIdIfAbsentFromDocument(Critic critic) {
        if (!documentHasId(critic))
            critic.setId(new ObjectId().toHexString());

        return critic;
    }

    @Override
    public boolean documentHasId(Critic critic) {
        return null != critic.getId();
    }

    @Override
    public BsonString getDocumentId(Critic critic) {
        if (!documentHasId(critic)) {
            throw new IllegalStateException("This document does not have an " + "_id");
        }

        return new BsonString(critic.getId());
    }
}

package com.knoldus.usercrud.user.impl;

import akka.Done;
import akka.NotUsed;
import com.knoldus.usercrud.user.impl.UserCommand.AddNewUser;
import com.knoldus.usercurd.user.api.User;
import com.knoldus.usercurd.user.api.UserService;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Created by harmeet on 30/1/17.
 */
public class UserServiceImpl implements UserService {

    private final PersistentEntityRegistry persistentEntityRegistry;
    private final CassandraSession session;

    @Inject
    public UserServiceImpl(final PersistentEntityRegistry registry, ReadSide readSide, CassandraSession session) {
        this.persistentEntityRegistry = registry;
        this.session = session;

        persistentEntityRegistry.register(UserEntity.class);
        readSide.register(UserEventProcessor.class);
    }

    @Override
    public ServiceCall<NotUsed, Optional<User>> user(String id) {
        return request -> {
            CompletionStage<Optional<User>> userFuture = session.selectAll("SELECT * FROM users WHERE id = ?", id)
                    .thenApply(rows ->
                            rows.stream()
                                    .map(row -> new User(row.getString("id"), row.getString("name"), row.getInt("age")))
                                    .findFirst()
                    );
            return userFuture;
        };
    }

    @Override
    public ServiceCall<User, Done> newUser() {
        return user -> {
            PersistentEntityRef<UserCommand> ref = persistentEntityRegistry.refFor(UserEntity.class, user.id);
            return ref.ask(new AddNewUser(user));
        };
    }
}
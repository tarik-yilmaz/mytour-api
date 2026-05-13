package org.fhtw.mytourapi.repository;

import org.fhtw.mytourapi.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameNormalized(String usernameNormalized);

    boolean existsByUsernameNormalized(String usernameNormalized);
}

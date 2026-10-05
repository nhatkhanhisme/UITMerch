package com.uitmerch.backend.order.security;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface CheckoutActorRepository extends JpaRepository<CheckoutActor,String> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CheckoutActor a where a.actorKey=:key")
    Optional<CheckoutActor> locked(String key);
}

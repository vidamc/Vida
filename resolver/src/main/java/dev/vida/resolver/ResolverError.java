/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.Version;
import dev.vida.core.VersionRange;
import java.util.List;
import java.util.Objects;

/**
 * Структурированные ошибки резолвера. Перебор запечатан, расширение —
 * через новые записи sealed-иерархии.
 */
@ApiStatus.Stable
public sealed interface ResolverError {

    /**
     * Универс не содержит ни одного провайдера для требуемого id или ни один
     * не попадает в запрашиваемый диапазон.
     *
     * @param id         отсутствующий id
     * @param range      самый ограничительный из активных диапазонов
     * @param requesters цепочка id, которые привели к этому требованию
     */
    record Missing(String id, VersionRange range, List<String> requesters)
            implements ResolverError {
        public Missing {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(range, "range");
            requesters = List.copyOf(Objects.requireNonNull(requesters, "requesters"));
        }
    }

    /**
     * Для одного id существуют взаимно-исключающие ограничения: ни одна
     * версия из универса не попадает во все одновременно. Отличается от
     * {@link Missing} только семантикой (кандидаты есть, но их диапазоны
     * «схлопываются в пустоту»).
     */
    record VersionConflict(String id, List<Constraint> constraints)
            implements ResolverError {
        public VersionConflict {
            Objects.requireNonNull(id, "id");
            constraints = List.copyOf(Objects.requireNonNull(constraints, "constraints"));
            if (constraints.isEmpty()) {
                throw new IllegalArgumentException("constraints must not be empty");
            }
        }

        /** Одно ограничение, наложенное конкретным requester’ом. */
        public record Constraint(String requester, VersionRange range) {
            public Constraint {
                Objects.requireNonNull(requester, "requester");
                Objects.requireNonNull(range, "range");
            }
        }
    }

    /**
     * Явная несовместимость: выбранный провайдер объявил другой выбранный
     * провайдер несовместимым.
     *
     * @param declarer      id, объявивший несовместимость
     * @param declarerVer   его версия
     * @param offender      id «обидчика»
     * @param offenderVer   версия обидчика
     * @param range         диапазон, указанный в decl
     */
    record Incompatibility(String declarer, Version declarerVer,
                           String offender, Version offenderVer,
                           VersionRange range) implements ResolverError {
        public Incompatibility {
            Objects.requireNonNull(declarer, "declarer");
            Objects.requireNonNull(declarerVer, "declarerVer");
            Objects.requireNonNull(offender, "offender");
            Objects.requireNonNull(offenderVer, "offenderVer");
            Objects.requireNonNull(range, "range");
        }
    }

    /**
     * Id попал в {@link ResolverOptions#excludes()}, но какой-то требующий
     * его провайдер помечает зависимость как REQUIRED.
     */
    record Excluded(String id, String requester) implements ResolverError {
        public Excluded {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(requester, "requester");
        }
    }

    /**
     * Pin указывает на версию, которая не проходит по диапазону, объявленному
     * зависимостью, или её просто нет в универсе.
     */
    record BadPin(String id, Version pinned, String reason) implements ResolverError {
        public BadPin {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(pinned, "pinned");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /** Поиск превысил лимит {@link ResolverOptions#maxDecisions()}. */
    record DecisionLimit(int decisionsTaken) implements ResolverError {
        public DecisionLimit {
            if (decisionsTaken < 0) {
                throw new IllegalArgumentException("decisionsTaken must be >= 0");
            }
        }
    }

    /** Поиск превысил лимит {@link ResolverOptions#timeoutMillis()}. */
    record Timeout(long elapsedMillis, int decisionsTaken) implements ResolverError {
        public Timeout {
            if (elapsedMillis < 0 || decisionsTaken < 0) {
                throw new IllegalArgumentException("elapsedMillis and decisionsTaken must be >= 0");
            }
        }
    }

    /**
     * Входные данные уже содержат дубликат провайдеров с одной и той же
     * парой (id, version). Резолвер такие случаи не разруливает — это делает
     * дискавери-слой или автор универса.
     */
    record DuplicateProvider(String id, Version version) implements ResolverError {
        public DuplicateProvider {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(version, "version");
        }
    }
}

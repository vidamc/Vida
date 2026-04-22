/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import dev.vida.core.Version;
import dev.vida.core.VersionRange;
import dev.vida.resolver.DependencyKind;
import dev.vida.resolver.Dependency;
import dev.vida.resolver.Provider;
import dev.vida.resolver.Resolution;
import dev.vida.resolver.ResolverError;
import dev.vida.resolver.ResolverOptions;
import dev.vida.resolver.ResolverStrategy;
import dev.vida.resolver.Universe;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Бэктрекинговый поиск решения. Алгоритм: хронологический бэктрекинг с
 * эвристикой MRV — на каждом шаге выбирается id с минимальным числом
 * кандидатов. Мягкие требования, в отличие от жёстких, не приводят к
 * бэктреку: если их нельзя удовлетворить — id уходит в {@code
 * optionalMissing}.
 */
@ApiStatus.Internal
public final class Backtracker {

    /** Технический токен requester'а для корневых ограничений. */
    public static final String ROOT_REQUESTER = "<root>";

    private final Universe universe;
    private final ResolverOptions options;
    private final long deadlineNanos;
    private int decisions;
    private final long startNanos;

    /** Последняя зафиксированная ошибка — пригождается, когда все кандидаты отпали. */
    private ResolverError lastError;

    public Backtracker(Universe universe, ResolverOptions options) {
        this.universe = universe;
        this.options = options;
        this.startNanos = System.nanoTime();
        this.deadlineNanos = startNanos + options.timeoutMillis() * 1_000_000L;
    }

    // ===================================================================
    //                              ENTRY
    // ===================================================================

    public Result<Resolution, ResolverError> solve(Set<String> roots) {
        SearchState state = new SearchState();

        // Корневые требования — все жёсткие, ANY-range.
        for (String root : roots) {
            state.addRangeConstraint(root,
                    new Constraint(ROOT_REQUESTER, VersionRange.ANY, DependencyKind.REQUIRED, null));
            state.pending.add(root);
        }

        // Фастпасы по pin'ам: если pin ссылается на id вне универса — BadPin сразу.
        for (Map.Entry<String, Version> e : options.pins().entrySet()) {
            List<Provider> cands = universe.candidates(e.getKey());
            boolean ok = false;
            for (Provider p : cands) {
                if (p.version().equals(e.getValue())) { ok = true; break; }
            }
            if (!ok) {
                return Result.err(new ResolverError.BadPin(e.getKey(), e.getValue(),
                        "pinned version is not present in universe"));
            }
        }

        ResolverError err = recurse(state);
        if (err != null) {
            return Result.err(err);
        }
        return Result.ok(toResolution(state));
    }

    // ===================================================================
    //                              CORE
    // ===================================================================

    /**
     * @return {@code null} при успехе (состояние полностью разрешено),
     *         иначе — причина провала.
     */
    private ResolverError recurse(SearchState state) {
        ResolverError limit = checkLimits();
        if (limit != null) return limit;

        String pickedId = pickNext(state);
        if (pickedId == null) {
            return null; // success
        }

        // ------------------------------------------------ excludes
        if (options.excludes().contains(pickedId)) {
            String hardRequester = firstRequiredRequester(state.ranges(pickedId));
            if (hardRequester != null) {
                return recordError(new ResolverError.Excluded(pickedId, hardRequester));
            }
            // Все требования мягкие: пропускаем id и идём дальше.
            state.pending.remove(pickedId);
            state.optionalMissing.add(pickedId);
            return recurse(state);
        }

        // ------------------------------------------------ candidates
        List<Provider> cands = filterCandidates(pickedId, state);
        if (cands.isEmpty()) {
            return handleNoCandidates(pickedId, state);
        }
        sortByStrategy(cands, options.strategy());

        // ------------------------------------------------ try each
        ResolverError fail = null;
        for (Provider c : cands) {
            decisions++;
            ResolverError lim = checkLimits();
            if (lim != null) return lim;

            SearchState snap = state.snapshot();
            ResolverError applyFail = apply(state, pickedId, c);
            if (applyFail == null) {
                ResolverError sub = recurse(state);
                if (sub == null) return null; // success propagates up
                fail = sub;
            } else {
                fail = applyFail;
            }
            state.restoreFrom(snap);
        }
        // Цикл пуст быть не мог (cands.isEmpty() обработан выше), и раз мы здесь —
        // хоть одна итерация fail присвоила.
        return recordError(fail);
    }

    // ===================================================================
    //                         PICK / FILTER
    // ===================================================================

    /** MRV: id с наименьшим числом прошедших фильтр кандидатов. */
    private String pickNext(SearchState state) {
        if (state.pending.isEmpty()) return null;

        String best = null;
        int bestCount = Integer.MAX_VALUE;
        for (String id : state.pending) {
            if (state.byQuery.containsKey(id)) {
                // Уже разрешён (через алиас кого-то уже выбранного) — чистим pending.
                continue;
            }
            int n = countFiltered(id, state);
            if (n < bestCount) {
                best = id;
                bestCount = n;
                if (n == 0) break; // неразрешимо — хотим фейл как можно раньше
            }
        }
        if (best == null) {
            // Все pending уже покрыты — почистим и вернём null.
            state.pending.removeIf(state.byQuery::containsKey);
            return state.pending.isEmpty() ? null : state.pending.iterator().next();
        }
        return best;
    }

    private int countFiltered(String id, SearchState state) {
        int n = 0;
        for (Provider p : universe.candidates(id)) {
            if (candidateMatches(p, id, state)) n++;
        }
        return n;
    }

    private List<Provider> filterCandidates(String id, SearchState state) {
        Version pin = options.pins().get(id);
        List<Provider> out = new ArrayList<>();
        for (Provider p : universe.candidates(id)) {
            if (pin != null && !p.version().equals(pin)) continue;
            if (candidateMatches(p, id, state)) out.add(p);
        }
        return out;
    }

    /** Кандидат не должен противоречить ни одному накопленному ограничению. */
    private boolean candidateMatches(Provider p, String forQuery, SearchState state) {
        List<String> keys = p.provides().isEmpty()
                ? List.of(p.id())
                : listOf(p.id(), p.provides());

        // Проверяем на конфликт собственных id/алиасов с уже выбранными.
        Provider selectedSameId = state.selected.get(p.id());
        if (selectedSameId != null && !selectedSameId.equals(p)) return false;
        for (String key : keys) {
            Provider bound = state.byQuery.get(key);
            if (bound != null && !bound.equals(p)) return false;
        }

        // Проверяем ограничения для каждого id/алиаса.
        for (String key : keys) {
            for (Constraint c : state.ranges(key)) {
                if (!c.range().matches(p.version())) return false;
            }
            for (Constraint c : state.incompats(key)) {
                if (c.range().matches(p.version())) return false;
            }
        }
        return true;
    }

    private static List<String> listOf(String head, List<String> tail) {
        List<String> out = new ArrayList<>(tail.size() + 1);
        out.add(head);
        out.addAll(tail);
        return out;
    }

    private static void sortByStrategy(List<Provider> xs, ResolverStrategy strategy) {
        switch (strategy) {
            case NEWEST -> xs.sort(Comparator.<Provider, Version>comparing(Provider::version).reversed());
            case OLDEST -> xs.sort(Comparator.comparing(Provider::version));
            case STABLE_FIRST -> xs.sort((a, b) -> {
                boolean ap = a.version().isPreRelease();
                boolean bp = b.version().isPreRelease();
                if (ap != bp) return ap ? 1 : -1;
                return b.version().compareTo(a.version()); // внутри группы — новее раньше
            });
        }
    }

    // ===================================================================
    //                              APPLY
    // ===================================================================

    /** @return {@code null} при успехе, либо структурированная ошибка. */
    private ResolverError apply(SearchState state, String pickedId, Provider p) {
        List<String> keys = p.provides().isEmpty()
                ? List.of(p.id())
                : listOf(p.id(), p.provides());

        // Финальная проверка: вдруг после предыдущего decision’а правила изменились
        // и кандидат уже не валиден. Быстро защищает от повторной траты decisions.
        if (!candidateMatches(p, pickedId, state)) {
            return buildConflict(pickedId, state, p);
        }

        state.selected.put(p.id(), p);
        for (String k : keys) state.byQuery.put(k, p);
        state.pending.remove(pickedId);
        for (String k : keys) state.pending.remove(k);

        String requesterTag = p.id() + "@" + p.version();
        for (Dependency dep : p.dependencies()) {
            Constraint c = new Constraint(requesterTag, dep.range(), dep.kind(), p);
            String target = dep.targetId();

            if (dep.kind() == DependencyKind.INCOMPATIBLE) {
                state.addIncompatConstraint(target, c);
                Provider bound = state.byQuery.get(target);
                if (bound != null && dep.range().matches(bound.version())) {
                    return new ResolverError.Incompatibility(
                            p.id(), p.version(), bound.id(), bound.version(), dep.range());
                }
                continue;
            }

            state.addRangeConstraint(target, c);
            Provider bound = state.byQuery.get(target);
            if (bound != null) {
                if (!dep.range().matches(bound.version())) {
                    return buildConflict(target, state, /*prospect=*/null);
                }
                // Уже удовлетворено.
                continue;
            }

            // Ранее id мог попасть в optionalMissing — если теперь нашёлся жёсткий
            // requester, дадим ещё один шанс.
            if (dep.kind() == DependencyKind.REQUIRED
                    && state.optionalMissing.remove(target)) {
                state.pending.add(target);
                continue;
            }

            if (dep.kind() == DependencyKind.OPTIONAL && options.skipOptional()) {
                state.optionalMissing.add(target);
            } else {
                state.pending.add(target);
            }
        }

        return null;
    }

    // ===================================================================
    //                          NO-CANDIDATES
    // ===================================================================

    private ResolverError handleNoCandidates(String id, SearchState state) {
        List<Constraint> constraints = state.ranges(id);
        String hardRequester = firstRequiredRequester(constraints);

        if (hardRequester == null) {
            // Только мягкие — просто уходим в missing, без ошибки.
            state.pending.remove(id);
            state.optionalMissing.add(id);
            ResolverError sub = recurse(state);
            return sub; // может быть null (успех) или реальная ошибка дальше
        }

        List<Provider> raw = universe.candidates(id);
        if (raw.isEmpty()) {
            VersionRange worst = constraints.isEmpty()
                    ? VersionRange.ANY
                    : constraints.get(0).range();
            List<String> reqIds = new ArrayList<>();
            for (Constraint c : constraints) reqIds.add(c.requester());
            return new ResolverError.Missing(id, worst, reqIds);
        }

        // Если хотя бы один кандидат проходит по range-ограничениям, но
        // был выброшен incompat-ом — это более точный диагноз.
        ResolverError incErr = tryIncompatibilityError(id, raw, state);
        if (incErr != null) return incErr;

        return buildConflict(id, state, null);
    }

    /**
     * Если в сыром списке кандидатов есть такой, чья версия попадает во все
     * range-ограничения, но хотя бы один incompat-диапазон её исключает —
     * возвращаем соответствующую {@link ResolverError.Incompatibility} от
     * того декларанта, который и наложил этот incompat.
     */
    private ResolverError tryIncompatibilityError(String id, List<Provider> raw, SearchState state) {
        List<Constraint> ranges = state.ranges(id);
        List<Constraint> incompats = state.incompats(id);
        if (incompats.isEmpty()) return null;
        for (Provider p : raw) {
            boolean passesRanges = true;
            for (Constraint c : ranges) {
                if (!c.range().matches(p.version())) { passesRanges = false; break; }
            }
            if (!passesRanges) continue;
            for (Constraint inc : incompats) {
                if (inc.range().matches(p.version())) {
                    Provider declarer = inc.declarer();
                    if (declarer != null) {
                        return new ResolverError.Incompatibility(
                                declarer.id(), declarer.version(),
                                p.id(), p.version(), inc.range());
                    }
                    // Корневых incompat у нас нет, но на всякий случай fall-through.
                }
            }
        }
        return null;
    }

    /**
     * Собирает {@link ResolverError.VersionConflict} из накопленных REQUIRED
     * ограничений. Если в числе виновников есть pin — приоритет у него через
     * {@link ResolverError.BadPin}.
     */
    private ResolverError buildConflict(String id, SearchState state, Provider newProspect) {
        Version pin = options.pins().get(id);
        if (pin != null) {
            // Проверяем, есть ли хоть одно ограничение, которое pin нарушает.
            for (Constraint c : state.ranges(id)) {
                if (!c.range().matches(pin)) {
                    return new ResolverError.BadPin(id, pin,
                            "pinned version does not satisfy constraint " + c.range()
                                    + " imposed by " + c.requester());
                }
            }
        }
        List<ResolverError.VersionConflict.Constraint> out = new ArrayList<>();
        for (Constraint c : state.ranges(id)) {
            if (c.kind() == DependencyKind.REQUIRED) {
                out.add(new ResolverError.VersionConflict.Constraint(c.requester(), c.range()));
            }
        }
        // Add optional too, если hard-requester’ов не нашлось — для полноты.
        if (out.isEmpty()) {
            for (Constraint c : state.ranges(id)) {
                out.add(new ResolverError.VersionConflict.Constraint(c.requester(), c.range()));
            }
        }
        if (out.isEmpty()) {
            // Странный случай: VersionConflict без ограничений невозможен,
            // значит id оказался несовместим не по range, а только по incompat
            // (без hard-requester'а) — тогда это not-a-bug, просто пропуск id.
            out.add(new ResolverError.VersionConflict.Constraint(
                    "(incompatibility)", VersionRange.ANY));
        }
        if (newProspect != null) {
            // Кандидат отпал — не страшно, лог просто запишет последнюю попытку.
        }
        return new ResolverError.VersionConflict(id, out);
    }

    // ===================================================================
    //                            HELPERS
    // ===================================================================

    private static String firstRequiredRequester(List<Constraint> xs) {
        for (Constraint c : xs) {
            if (c.kind() == DependencyKind.REQUIRED) return c.requester();
        }
        return null;
    }

    private ResolverError checkLimits() {
        if (decisions > options.maxDecisions()) {
            return new ResolverError.DecisionLimit(decisions);
        }
        if (System.nanoTime() > deadlineNanos) {
            long elapsed = (System.nanoTime() - startNanos) / 1_000_000L;
            return new ResolverError.Timeout(elapsed, decisions);
        }
        return null;
    }

    private ResolverError recordError(ResolverError e) {
        if (e != null) lastError = e;
        return e;
    }

    /** Собирает финальный {@link Resolution}. */
    private Resolution toResolution(SearchState state) {
        // Разобьём выбранные провайдеры на required / optional-included.
        Set<String> optIncluded = new LinkedHashSet<>();
        for (Provider p : state.selected.values()) {
            List<String> keys = p.provides().isEmpty()
                    ? List.of(p.id())
                    : listOf(p.id(), p.provides());
            boolean anyHard = false;
            for (String k : keys) {
                for (Constraint c : state.ranges(k)) {
                    if (c.kind() == DependencyKind.REQUIRED) { anyHard = true; break; }
                }
                if (anyHard) break;
            }
            if (!anyHard) optIncluded.add(p.id());
        }
        long elapsed = System.nanoTime() - startNanos;
        return new Resolution(
                new LinkedHashMap<>(state.selected),
                optIncluded,
                state.optionalMissing,
                decisions,
                Duration.ofNanos(elapsed));
    }

    // Пакетно-приватный аксессор для статистики (тесты / инспекция).
    public int decisionsMade() { return decisions; }
    public ResolverError lastError() { return lastError; }
}

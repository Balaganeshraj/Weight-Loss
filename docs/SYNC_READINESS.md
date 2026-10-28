# Offline-first synchronization boundary

This release has no Internet permission, network client, connected provider, account or background upload. `JournalRepository` is the UI-facing domain interface. `LocalRepository` supplies encrypted Room storage. `SyncQueue` exposes pending mutations separately; `CloudJournalGateway` is an unimplemented provider contract. `SyncCoordinator` cannot run without explicit consent.

Room schema 2 adds a transactionally written encrypted outbox and per-record local revisions. Migration 1→2 preserves existing encrypted records without destructive migration. Save and queue insertion share a transaction. Later local corrections compact older unsent content for the same record. Deletion removes the record and queues a content-free tombstone. Stable mutation UUIDs survive retries. Whole-journal deletion clears records and outbox together. Timer state is local-only; saving a completed session atomically writes its activity mutation and removes the timer.

Only explicitly acknowledged known mutation IDs are removed. Unknown acknowledgements and contradictory acknowledgement/conflict responses fail validation. Conflicts and failed requests remain queued. There is no unbounded last-write-wins merge, nor a server-side idempotency claim.

Before connecting a real provider:

1. Choose/implement a vetted identity provider, server authorization from validated identity, owner isolation and secure token handling. Do not accept an owner ID from the journal JSON as authority.
2. Define remote revision/ETag mapping, idempotency retention, deletion acknowledgement and conflict-resolution UI. `localRevision` is a local ordering aid, not a server ETag.
3. Add an initial snapshot/import protocol for pre-existing records; the v1→v2 migration intentionally does not upload or enqueue historical records without a future consented bootstrap.
4. Implement pull cursors, conflict handling, deletion propagation, account export/deletion and restore tests. A push interface is not a complete synchronization service.
5. Implement appropriate network constraints/backoff, update privacy/Data Safety, obtain explicit sync consent and request network access only in that intentionally connected version.

Unit tests cover consent gating, acknowledgements, conflict retention and retry preservation. Instrumentation tests cover encrypted migration, transactional edits, queue compaction and tombstones. Read VALIDATION.md for execution status.

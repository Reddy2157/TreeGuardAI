# TreeGuard AI — Firestore Security Specification

## 1. Data Invariants
1. **Authentication & Ownership Isolation**: Every document across `/trees`, `/observations`, `/stress_records`, `/maintenance_actions`, `/weather_records`, and `/storm_comparisons` MUST belong to an authenticated user (`request.auth != null`) and have `userId == request.auth.uid`.
2. **Strict Schema & Bounds Validation**: Every string field enforces non-empty/maximum character length limits; every enum field enforces exact allowed values; every numeric score or coordinate enforces valid range bounds.
3. **Immutable Audit Fields**: `userId` and `createdAt` (and `treeId` on child records) are strictly immutable on update. Append-only historical logs (`observations`, `stress_records`, `weather_records`, `storm_comparisons`) disallow updates to preserve longitudinal audit integrity.
4. **Query Enforcer**: Every `allow list` rule enforces `resource.data.userId == request.auth.uid`, requiring client queries to filter with `.whereEqualTo("userId", uid)`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: `GET /trees/tree_1` with `auth = null` -> REJECT.
2. **Cross-User Read (IDOR)**: `GET /trees/bob_tree` as `alice_uid` -> REJECT.
3. **Unfiltered Collection List Scraping**: `LIST /trees` without `.where("userId", "==", uid)` -> REJECT.
4. **Identity Spoofing on Create**: `POST /trees/tree_1` by `alice_uid` with `userId: "bob_uid"` -> REJECT.
5. **Shadow / Ghost Field Injection**: `POST /trees/tree_1` including `"isAdmin": true` -> REJECT via `.keys().hasOnly(...)`.
6. **Oversized String DoS (Denial of Wallet)**: `POST /trees/tree_1` with a 10,000-char `notes` string -> REJECT via `.size() <= 2000`.
7. **Invalid Enum Injection**: `POST /trees/tree_1` with `currentPriority: "SUPER_CRITICAL"` -> REJECT.
8. **Future Timestamp Spoofing**: `POST /trees/tree_1` with `createdAt` set 1 hour in the future -> REJECT via `incoming().createdAt <= request.time`.
9. **Immutable Field Mutation on Update**: `PATCH /trees/tree_1` changing `createdAt` or `userId` -> REJECT.
10. **Value Poisoning on Update**: `PATCH /maintenance_actions/act_1` setting `estimatedCost` to a negative number `-500` -> REJECT.
11. **Historical Tampering**: `PATCH /observations/obs_1` attempting to rewrite a past AI Tree Doctor report -> REJECT (`allow update: if false`).
12. **Path ID Poisoning**: Document ID with spaces or special characters `tree$123!` -> REJECT via `isValidId()`.

# OpenTelemetry Android SDK - PR Submission Strategy
## Session Management Enhancement Contributions

**Date:** November 21, 2025  
**Status:** Ready for upstream submission  
**Legal Approval:** ✅ Approved by Legal (Maureen)

---

## 🎯 Submission Order (Corrected)

### **Phase 1: Build Trust (Week 1)**
**PR #1: `fix/session-manager-concurrency`** ← **START HERE**

### **Phase 2: Foundation (Week 2-3)**
**PR #2: `feat/session-management-infrastructure`** ← **Core utilities + Metrics**

### **Phase 3: Integration (Week 3-4)**
**PR #3: `feat/session-telemetry-integration`** ← **All instrumentation**

### **Phase 4: Evaluate**
**`session-management-updates`** - Likely not needed if PR #2 and #3 merge

---

## 📊 Branch Status

| Branch | Files | Lines | Type | Complexity |
|--------|-------|-------|------|------------|
| fix/session-manager-concurrency | 3 | 273 | Bug Fix | Low |
| feat/session-management-infrastructure | 45 | 3,896 | Feature | Medium |
| feat/session-telemetry-integration | 66 | 2,587 | Feature | High |
| session-management-updates | 61 | 2,339 | Combined | Medium |

---

## 🚀 PR #1: Session Manager Concurrency Fix

### **Branch:** `fix/session-manager-concurrency`

### **Title:**
```
Fix SessionManager thread-safety issue with atomic operations
```

### **Description:**

```markdown
## Description

Fixes thread-safety issue in SessionManager where concurrent calls to `getSessionId()` 
could create duplicate sessions during session timeout/expiration.

**Problem:**
The existing code had a documented TODO acknowledging this race condition:
```kotlin
// TODO FIXME: This is not threadsafe -- if two threads call getSessionId()
// at the same time while timed out, two new sessions are created
```

**Solution:**
- Changed `session` from mutable variable to `AtomicReference<Session>`
- Implemented compare-and-set (CAS) pattern for atomic session updates
- Only one thread successfully creates new session; others use the newly created one
- Extracted observer notification into separate method for clarity

**Testing:**
Added 3 comprehensive concurrency tests:
- Concurrent access during timeout (10 threads)
- Concurrent access with timeout handler (5 threads)  
- Session consistency under concurrent load (20 threads)

All existing tests continue to pass.

## Type of Change

- [x] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature
- [ ] Breaking change
- [x] Documentation update

## Checklist

- [x] Code follows project style guidelines (spotless applied)
- [x] Self-review completed
- [x] Added tests that prove the fix is effective
- [x] New and existing tests pass locally
- [x] Documentation updated where applicable

## Additional Context

This uses the standard atomic compare-and-set pattern commonly used for 
thread-safe operations in Java/Kotlin. The pattern is well-documented in 
"Java Concurrency in Practice" and used throughout the Android/Java ecosystem.

Related semantic conventions: https://opentelemetry.io/docs/specs/semconv/general/session/
```

### **Initial Comment After Creation:**

```markdown
Hi OpenTelemetry maintainers! 👋

I've been working on improving session management in the Android SDK and 
wanted to start by contributing this thread-safety fix.

This is the first of a series of improvements we're planning to contribute:
1. **This PR:** Thread-safety fix (low-risk, addresses existing TODO)
2. **Follow-up:** Session infrastructure (core utilities + metrics support)
3. **Follow-up:** Session ID integration across all instrumentation

I've already reached out to the team and received positive feedback. 
Looking forward to collaborating with you on these enhancements!

Happy to make any adjustments based on your review.
```

### **Why This PR First:**
- ✅ Smallest change (3 files, 273 lines)
- ✅ Fixes documented bug with TODO comment
- ✅ Low risk, high value
- ✅ Builds credibility with maintainers
- ✅ All tests passing
- ✅ Standard pattern (atomic CAS)

### **Timeline:**
- **Submit:** Week 1 (Today!)
- **Expected Response:** 2-7 days
- **Review Cycles:** 1-3 rounds
- **Merge Time:** 1-4 weeks

---

## 🏗️ PR #2: Session Management Infrastructure

### **Branch:** `feat/session-management-infrastructure`

### **Title:**
```
Add session management infrastructure with metrics support
```

### **Description:**

```markdown
## Description

Adds foundational infrastructure for session tracking across all telemetry signals.

**This PR includes:**

### Core Session Utilities
- `SessionIdentifiers` - Data class for session.id and session.previous_id
- `SessionExtensions` - Kotlin extensions for adding session IDs to spans/logs/metrics
- `SessionIdentifierFacade` - Facade pattern for consistent session access
- `SessionIdFacade` - UUID fallback for session ID generation

### Metrics Infrastructure (Novel Feature!)
- `SessionMetricExporterAdapter` - Adapter pattern for adding session IDs to metrics
- Decorator pattern for all metric data types (Sum, Gauge, Histogram, etc.)
- Factory classes for creating session-enhanced metric data
- 11 wrapper models for different metric types

**Key Innovation:** This enables session-based metrics analytics - a capability 
**not present in the iOS or JavaScript SDKs**! This allows correlating metrics 
like memory usage, network bytes, or performance counters to specific user sessions.

### Architecture Patterns Used
- **Adapter Pattern** - Wraps metric exporters to inject session attributes
- **Factory Pattern** - Creates session-enhanced metric data
- **Decorator Pattern** - Adds session attributes without modifying original data
- **Facade Pattern** - Simplifies session identifier access

**Dependencies:**
- Requires: PR #1 (session-manager-concurrency fix)
- Followed by: Session telemetry integration (separate PR)

## Type of Change

- [ ] Bug fix
- [x] New feature (non-breaking)
- [ ] Breaking change
- [x] Infrastructure improvement

## Checklist

- [x] Code follows project style guidelines (spotless applied)
- [x] Self-review completed
- [x] Comprehensive test coverage added
- [x] New and existing tests pass locally
- [x] Documentation added for new components

## Testing

- Comprehensive test coverage for all factories and adapters (~1,400 lines of tests)
- Metric data transformation tests
- Session identifier extraction tests
- Edge cases (empty IDs, null handling, etc.)

## Additional Context

**Comparison with Other Platforms:**
- **iOS Swift SDK:** Has session support for spans/logs, but NOT metrics
- **Web JavaScript SDK:** Has interface only, no automatic implementation
- **This Android implementation:** First to support session IDs on metrics!

This infrastructure establishes the foundation for comprehensive session tracking.
A follow-up PR will integrate this across all instrumentation modules.

## References

- OpenTelemetry Semantic Conventions: https://opentelemetry.io/docs/specs/semconv/general/session/
- Depends on: PR #1 (session concurrency fix)
```

### **Why This PR Second:**
- ✅ Establishes foundation before integration
- ✅ Core utilities needed by PR #3
- ✅ Metrics support is unique/novel feature
- ✅ Easier to review infrastructure in isolation
- ✅ Logical progression: foundation → usage

### **Timeline:**
- **Submit:** Week 2 (after PR #1 gets initial review)
- **Reference:** PR #1 in description
- **Note:** Don't wait for PR #1 to merge, just get initial feedback

---

## 🔌 PR #3: Session Telemetry Integration

### **Branch:** `feat/session-telemetry-integration`

### **Title:**
```
Integrate session identifiers across all telemetry instrumentation
```

### **Description:**

```markdown
## Description

Integrates session identifiers across all telemetry instrumentation modules,
using the infrastructure established in PR #2.

**This PR adds session IDs to:**

### Instrumentation Coverage
- ✅ **ANR (Application Not Responding)** - Session context for app freezes
- ✅ **Crash reporting** - Link crashes to user sessions
- ✅ **View clicks** - Track user interactions per session
- ✅ **Compose clicks** - Session context for Jetpack Compose UI
- ✅ **Network changes** - Correlate connectivity with sessions
- ✅ **WebSocket events** - Session tracking for real-time connections
- ✅ **Slow rendering / jank** - Performance issues per session
- ✅ **App startup events** - Session initialization tracking

### Integration Points
- **SessionIdSpanAppender** - Automatically adds session IDs to all spans
- **SessionIdLogRecordAppender** - Automatically adds session IDs to all log records
- **Factory Pattern** - SessionProviderFactory and SessionManagerFactory
- **Configuration DSL** - Easy session configuration in OpenTelemetryRumInitializer

### Example Usage

```kotlin
OpenTelemetryRumInitializer.initialize(context) {
    session {
        // Session expires after 15 minutes in background (default)
        backgroundInactivityTimeout = 15.minutes

        // Session expires after 4 hours regardless of activity (default)
        maxLifetime = 4.hours
    }
}
```

**Result:** All telemetry data now includes session context (`session.id` and 
`session.previous_id`) for comprehensive session-based observability.

**Dependencies:**
- Requires: PR #2 (session-management-infrastructure)
- Uses: Core utilities and patterns from PR #2

## Type of Change

- [ ] Bug fix
- [x] New feature (non-breaking)
- [ ] Breaking change
- [x] Documentation update

## Checklist

- [x] Code follows project style guidelines (spotless applied)
- [x] Self-review completed
- [x] Integration tests for all instrumentation modules
- [x] New and existing tests pass locally
- [x] Documentation updated (instrumentation/sessions/README.md)

## Testing

- Integration tests for all 8 instrumentation modules
- Processor tests for spans and logs
- Factory pattern tests
- Configuration DSL tests
- ~50 new test methods

## Additional Context

This PR completes the session management enhancement by integrating the 
infrastructure from PR #2 across all instrumentation. Together, these changes
bring the Android SDK to parity with iOS (spans/logs) and beyond (metrics).

**Instrumentation Integration Pattern:**
Each instrumentation module now receives a `SessionProvider` and uses the
session extension functions to automatically add session attributes to events.

## References

- Builds on: PR #2 (session-management-infrastructure)
- OpenTelemetry Semantic Conventions: https://opentelemetry.io/docs/specs/semconv/general/session/
- iOS Reference Implementation: https://github.com/open-telemetry/opentelemetry-swift
```

### **Why This PR Third:**
- ✅ Uses foundation from PR #2
- ✅ Touches many modules (easier to review after understanding foundation)
- ✅ Completes the session management story
- ✅ Can reference both PR #1 and PR #2
- ✅ Logical conclusion: bug fix → foundation → integration

### **Timeline:**
- **Submit:** Week 3 (after PR #2 gets initial review)
- **Reference:** Both PR #1 and PR #2 in description
- **Note:** May need to wait for PR #2 merge or coordinate timing

---

## 📋 Pre-Submission Checklist

### **For Each PR:**

- [ ] Branch pushed to your fork
- [ ] All tests passing locally (`./gradlew clean build test`)
- [ ] Spotless formatting applied (`./gradlew spotlessApply`)
- [ ] Commits signed off with DCO (`git commit --signoff`)
- [ ] PR description ready (use templates above)
- [ ] Read the CONTRIBUTING.md guide
- [ ] Ready to be responsive to feedback

### **DCO Sign-off:**

If you forgot to sign off commits:
```bash
git commit --amend --signoff --no-edit
git push --force-with-lease origin <branch-name>
```

### **Final Verification Commands:**

```bash
cd /Users/c781502/Git/external/oss-contributions-opentelemetry-android-sdk

# For each branch:
git checkout <branch-name>
git fetch origin
git rebase origin/main  # If needed
./gradlew clean build test spotlessCheck
git push origin <branch-name>
```

---

## ⏰ What to Expect

### **Response Times:**
- **Initial response:** 2-7 days (maintainers are volunteers)
- **Review cycles:** 1-3 rounds typically
- **Merge time:** 1-4 weeks for small fixes, 2-6 weeks for features

### **Common Feedback:**
- Code style preferences
- Test coverage requests
- Documentation improvements
- API design discussions
- Performance considerations

### **Best Practices:**
- ✅ Respond promptly to feedback (within 24-48 hours)
- ✅ Be open to suggestions and compromise
- ✅ Keep changes focused (why we're doing 3 separate PRs)
- ✅ Ask clarifying questions if anything is unclear
- ✅ Be patient and professional
- ✅ Show appreciation for reviewer time

---

## 💬 Communication Strategy

### **After PR #1 Submission:**
Update your team:
```
📢 Good news! First PR submitted upstream: Session Manager concurrency fix.
Will monitor for feedback and keep you posted on progress.
Link: [PR URL]
```

### **After Getting Feedback:**
Share learnings:
```
📢 Update: Received feedback on PR #1. Maintainers requested [X].
This is helpful context for our upcoming PRs #2 and #3.
```

### **After PR #1 Merge:**
Celebrate and prepare:
```
🎉 Great news! PR #1 merged into OpenTelemetry Android SDK!
Preparing PR #2 (session infrastructure) for submission next week.
```

---

## 🎓 Pro Tips for Success

### **1. Start Small** ✅
You're doing this! PR #1 is perfectly sized.

### **2. Reference Existing Code**
Point out the TODO comment - shows you understand the codebase.

### **3. Comprehensive Testing**
Your 200+ lines of tests demonstrate quality and thoroughness.

### **4. Clean Commit History**
Consider squashing commits if needed before final submission.

### **5. Be Responsive**
Quick responses = faster merges. Check GitHub notifications daily.

### **6. Show Expertise**
Your testing, documentation, and architecture choices show professionalism.

### **7. Mention Follow-ups**
Shows you're invested long-term, not just drive-by contributions.

### **8. Learn and Adapt**
Use feedback from PR #1 to improve PR #2 and #3.

---

## 🏆 Success Metrics

### **PR #1 Success:**
- ✅ Merged within 4 weeks
- ✅ Minimal revision requests (1-2 rounds)
- ✅ Positive maintainer feedback
- ✅ Builds trust for larger PRs

### **PR #2 Success:**
- ✅ Maintainers understand the foundation
- ✅ Metrics approach validated or refined
- ✅ Architecture patterns approved
- ✅ Merged within 6 weeks

### **PR #3 Success:**
- ✅ Integration approach validated
- ✅ Instrumentation changes accepted
- ✅ Complete session management in SDK
- ✅ Merged within 8 weeks

### **Overall Success:**
- ✅ All PRs merged by Q1 2025
- ✅ Feature in next OpenTelemetry Android SDK release
- ✅ CVS eliminates fork maintenance
- ✅ Community benefits from session enhancements

---

## 📊 Comparison: Before vs After

### **Before (Current State):**
- ❌ SessionManager has thread-safety bug
- ❌ No session IDs on instrumentation events
- ❌ No session IDs on metrics
- ❌ Manual integration required

### **After (All 3 PRs Merged):**
- ✅ Thread-safe session management
- ✅ Session IDs on ALL telemetry (spans, logs, events, metrics)
- ✅ Automatic integration via processors
- ✅ Configuration DSL for easy setup
- ✅ Android SDK at parity with iOS + metrics support
- ✅ First platform with session IDs on metrics!

---

## 🌍 Platform Feature Comparison

| Feature | Web (JS) | iOS (Swift) | Android (After PRs) |
|---------|----------|-------------|---------------------|
| SessionManager | ⚠️ Experimental | ✅ Production | ✅ Production |
| Automatic Span Processor | ❌ No | ✅ Yes | ✅ Yes |
| Automatic Log Processor | ❌ No | ✅ Yes | ✅ Yes |
| Session Events | ⚠️ Manual | ✅ Yes | ✅ Yes |
| **Session IDs on Metrics** | ❌ **No** | ❌ **No** | ✅ **YES!** |
| Thread Safety | ✅ Yes | ✅ Yes | ✅ Yes (fixed) |
| Persistence | Interface only | ✅ UserDefaults | ✅ Yes |

**Key Insight:** Your Android implementation will be the most comprehensive!

---

## 📞 Contact & Resources

### **Internal:**
- **Technical Lead:** [Your Name]
- **Legal Approval:** Maureen (approved ✅)
- **Team Channel:** [Your Slack/Teams channel]

### **External:**
- **Project:** https://github.com/open-telemetry/opentelemetry-android
- **Contributing:** https://github.com/open-telemetry/opentelemetry-android/blob/main/CONTRIBUTING.md
- **Semantic Conventions:** https://opentelemetry.io/docs/specs/semconv/general/session/
- **Web Session Proposal:** https://github.com/open-telemetry/opentelemetry-js-contrib/issues/2358

### **Related PRs:**
- PR #1: [URL after submission]
- PR #2: [URL after submission]
- PR #3: [URL after submission]

---

## ✅ Ready to Go!

You're prepared with:
- ✅ Legal approval from Maureen
- ✅ Comprehensive testing (>100 test methods)
- ✅ Clean, well-documented code
- ✅ Clear business value
- ✅ Strong architectural patterns
- ✅ Proper submission strategy

**Next Action:** Submit PR #1 today! 🚀

**Good luck!** This is high-quality work that will benefit the entire OpenTelemetry community.

---

**Document Version:** 1.0  
**Last Updated:** November 21, 2025  
**Status:** Ready for execution


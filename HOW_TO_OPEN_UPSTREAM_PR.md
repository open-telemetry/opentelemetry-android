# How to Open a Pull Request Upstream
## OpenTelemetry Android SDK Contributions

**Audience:** CVS Health team members contributing to OpenTelemetry  
**Repository:** https://github.com/open-telemetry/opentelemetry-android  
**Date:** November 21, 2025

---

## 📋 Prerequisites

Before you start, ensure you have:

- [ ] GitHub account
- [ ] Git configured locally
- [ ] Legal approval for contributions
- [ ] Branch ready with changes
- [ ] All tests passing
- [ ] Code formatted (spotless applied)
- [ ] DCO sign-off on commits

---

## 🔧 Part 1: Fork Setup (One-Time Only)

### **Step 1: Fork the Repository**

1. Go to: https://github.com/open-telemetry/opentelemetry-android
2. Click the **"Fork"** button in the top-right corner
3. Select your GitHub account as the destination
4. Wait for GitHub to create your fork
5. Your fork will be at: `https://github.com/YOUR-USERNAME/opentelemetry-android`

### **Step 2: Add Your Fork as a Remote**

```bash
cd /Users/c781502/Git/external/oss-contributions-opentelemetry-android-sdk

# Check current remotes
git remote -v

# Add your fork (replace YOUR-USERNAME with your GitHub username)
git remote add fork https://github.com/YOUR-USERNAME/opentelemetry-android.git

# Verify it was added
git remote -v
```

You should see:
```
origin  https://github.com/open-telemetry/opentelemetry-android.git (fetch)
origin  https://github.com/open-telemetry/opentelemetry-android.git (push)
fork    https://github.com/YOUR-USERNAME/opentelemetry-android.git (fetch)
fork    https://github.com/YOUR-USERNAME/opentelemetry-android.git (push)
```

**Note:** If `origin` points to CVS internal fork, you may need to rename:
```bash
git remote rename origin cvs-origin
git remote add origin https://github.com/open-telemetry/opentelemetry-android.git
```

---

## 📝 Part 2: Prepare Your Branch

### **Step 1: Ensure Branch is Up-to-Date**

```bash
# Switch to your feature branch
git checkout fix/session-manager-concurrency

# Fetch latest from upstream
git fetch origin

# Rebase on latest main (if needed)
git rebase origin/main

# If there are conflicts, resolve them:
# 1. Edit conflicted files
# 2. git add <resolved-files>
# 3. git rebase --continue
```

### **Step 2: Verify DCO Sign-off**

All commits must be signed off (Developer Certificate of Origin):

```bash
# Check if your commits are signed off
git log --show-signature

# Look for this line in each commit:
# Signed-off-by: Your Name <your.email@example.com>
```

**If commits are NOT signed off:**

```bash
# Sign off the most recent commit
git commit --amend --signoff --no-edit

# For multiple commits, rebase interactively
git rebase -i HEAD~3  # Replace 3 with number of commits
# In the editor, change "pick" to "edit" for each commit
# Then for each commit:
git commit --amend --signoff --no-edit
git rebase --continue
```

### **Step 3: Run Final Checks**

```bash
# Clean build
./gradlew clean

# Format code
./gradlew spotlessApply

# Run all tests
./gradlew build test spotlessCheck

# Verify success
echo $?  # Should output: 0
```

### **Step 4: Push to Your Fork**

```bash
# Push to your fork (not origin!)
git push fork fix/session-manager-concurrency

# If you amended commits, force push (with safety):
git push --force-with-lease fork fix/session-manager-concurrency
```

**Important:** Always push to `fork`, not `origin`. `origin` is the upstream repo where you don't have write access.

---

## 🚀 Part 3: Create the Pull Request

### **Step 1: Navigate to GitHub**

**Option A: Via your fork**
1. Go to: `https://github.com/YOUR-USERNAME/opentelemetry-android`
2. You should see a yellow banner: "Your recently pushed branch: fix/session-manager-concurrency"
3. Click **"Compare & pull request"**

**Option B: Via upstream repo**
1. Go to: https://github.com/open-telemetry/opentelemetry-android
2. Click **"Pull requests"** tab
3. Click **"New pull request"**
4. Click **"compare across forks"** link
5. Set the compare:
   - **base repository:** `open-telemetry/opentelemetry-android`
   - **base:** `main`
   - **head repository:** `YOUR-USERNAME/opentelemetry-android`
   - **compare:** `fix/session-manager-concurrency`

### **Step 2: Fill Out PR Form**

#### **Title:**
```
Fix SessionManager thread-safety issue with atomic operations
```

**Guidelines:**
- Clear and descriptive
- Start with verb (Fix, Add, Improve, etc.)
- Under 72 characters
- No period at the end

#### **Description:**

Paste the PR template from `PR_SUBMISSION_STRATEGY_PARALLEL.md`

**Key sections:**
- [ ] **Description** - What and why
- [ ] **Related PRs** - Links to other PRs in the series
- [ ] **Type of Change** - Check appropriate boxes
- [ ] **Checklist** - Check all boxes
- [ ] **Testing** - What tests were added
- [ ] **Additional Context** - Any extra info

#### **Reviewers:**

Leave blank initially. Maintainers will assign themselves or others.

#### **Labels:**

You typically can't add labels as an external contributor. Maintainers will add them.

#### **Projects / Milestones:**

Leave blank. Maintainers manage these.

### **Step 3: Submit**

**For Regular PR:**
1. Click **"Create pull request"** button

**For Draft PR:**
1. Click dropdown arrow on button
2. Select **"Create draft pull request"**
3. Use for PR #2 and #3 to signal they're not ready to merge yet

### **Step 4: Post Initial Comment**

Immediately after creating the PR, add this comment:

```markdown
Hi OpenTelemetry maintainers! 👋

I've submitted a comprehensive session management enhancement as **3 related PRs**:

1. **#[NUMBER]** - Thread-safety fix (foundation) ← This PR
2. **#[NUMBER]** - Session infrastructure (utilities + metrics)
3. **#[NUMBER]** - Telemetry integration (comprehensive coverage)

**Dependencies:** PR #1 → PR #2 → PR #3 (clear merge order)

I've submitted all three at once so you can:
- See the complete vision and roadmap
- Review design decisions with full context
- Potentially parallelize reviews if you have multiple reviewers

I've already discussed this with the team and received positive feedback. 
Happy to make any adjustments based on your review.

Looking forward to collaborating! 🚀
```

---

## 🔗 Part 4: Link PRs Together

After creating all PRs, update each PR description to include actual links:

### **Edit PR Descriptions:**

1. Go to each PR
2. Click **"..."** (three dots) next to the PR title
3. Select **"Edit"**
4. Update the "Related PRs" section with actual PR numbers:

**Before:**
```markdown
- PR #2: Session infrastructure (depends on this) - [Will link after creation]
```

**After:**
```markdown
- PR #2: Session infrastructure (depends on this) - #123
```

**Pro tip:** Use `#123` format - GitHub auto-links to the PR

---

## 📧 Part 5: Monitor and Respond

### **Step 1: Enable Notifications**

1. Click **"Watch"** button (top-right of each PR)
2. Or enable email notifications in GitHub settings

### **Step 2: Check Daily**

- GitHub notifications (bell icon)
- Email notifications
- Direct PR page: `https://github.com/open-telemetry/opentelemetry-android/pull/[NUMBER]`

### **Step 3: Respond to Feedback**

When maintainers leave comments:

#### **For Code Changes Requested:**

```bash
# Switch to your branch
git checkout fix/session-manager-concurrency

# Make the requested changes
# Edit files...

# Stage changes
git add .

# Commit with sign-off
git commit --signoff -m "Address review feedback: [describe changes]"

# Push to your fork
git push fork fix/session-manager-concurrency
```

GitHub automatically updates the PR with new commits.

#### **For Questions/Discussion:**

1. Reply directly in the PR comment thread
2. Be professional and respectful
3. Ask clarifying questions if needed
4. Be open to suggestions

#### **Response Time:**
- Aim to respond within 24-48 hours
- If busy, add a comment: "Thanks for the feedback! I'll address this by [date]"

---

## 🔄 Part 6: Handling Common Scenarios

### **Scenario 1: Need to Update Multiple Commits**

```bash
# Interactive rebase
git rebase -i HEAD~3  # Last 3 commits

# In the editor:
# - "pick" → "edit" to modify a commit
# - "pick" → "squash" to combine commits
# - Save and close

# Make changes if needed
# git add . && git commit --amend --signoff --no-edit
# git rebase --continue

# Force push
git push --force-with-lease fork fix/session-manager-concurrency
```

### **Scenario 2: Merge Conflicts with Main**

```bash
# Fetch latest upstream
git fetch origin

# Rebase on main
git rebase origin/main

# Resolve conflicts:
# 1. Open conflicted files
# 2. Edit to resolve
# 3. git add <file>
# 4. git rebase --continue

# Force push
git push --force-with-lease fork fix/session-manager-concurrency
```

### **Scenario 3: CI/CD Checks Failing**

1. Click on the failing check in the PR
2. View the logs to understand the failure
3. Fix the issue locally
4. Commit and push the fix
5. CI will automatically re-run

**Common failures:**
- **Spotless check:** Run `./gradlew spotlessApply`
- **Tests failing:** Run `./gradlew test` locally
- **DCO check:** Ensure all commits are signed off

### **Scenario 4: Requested to Squash Commits**

```bash
# Squash last 3 commits into one
git rebase -i HEAD~3

# In the editor:
# - First commit: "pick"
# - Other commits: change "pick" to "squash"
# - Save and close

# Edit the combined commit message
# Save and close

# Force push
git push --force-with-lease fork fix/session-manager-concurrency
```

### **Scenario 5: Need to Split a PR**

If maintainers ask to split changes:

```bash
# Create new branch from main
git checkout -b fix/smaller-feature origin/main

# Cherry-pick specific commits
git cherry-pick <commit-hash>

# Push new branch
git push fork fix/smaller-feature

# Create new PR following steps above
```

---

## ✅ Part 7: PR Approval and Merge

### **What Happens:**

1. **Initial Review** (2-7 days)
   - Maintainers review your code
   - May request changes
   - May approve immediately

2. **Revision Cycle** (varies)
   - You address feedback
   - Maintainers re-review
   - May take 1-3 cycles

3. **Approval** 
   - Maintainer approves PR
   - CI checks must pass
   - May require 2+ approvals

4. **Merge**
   - Maintainer merges PR (not you!)
   - PR closes automatically
   - Your changes are in `main`!

### **After Merge:**

```bash
# Update your local main
git checkout main
git pull origin main

# Delete your feature branch (optional)
git branch -d fix/session-manager-concurrency
git push fork --delete fix/session-manager-concurrency
```

### **Celebrate!** 🎉

- Update your team
- Add to your accomplishments
- Prepare for next PR in sequence

---

## 📊 Part 8: Checklist Summary

### **Before Opening PR:**

- [ ] Branch created from latest `main`
- [ ] All commits signed off (DCO)
- [ ] Code formatted (`./gradlew spotlessApply`)
- [ ] All tests passing (`./gradlew test`)
- [ ] Build successful (`./gradlew build`)
- [ ] Branch pushed to your fork
- [ ] PR description prepared

### **When Opening PR:**

- [ ] Title is clear and descriptive
- [ ] Description follows template
- [ ] Related PRs section filled
- [ ] Checkboxes marked appropriately
- [ ] Initial comment posted
- [ ] Links between PRs added

### **After Opening PR:**

- [ ] Notifications enabled
- [ ] Checking daily for feedback
- [ ] Responding within 24-48 hours
- [ ] CI checks are passing
- [ ] Team is updated with PR link

---

## 🎓 Tips for Success

### **DO:**
✅ Read the project's CONTRIBUTING.md first  
✅ Keep PRs focused and atomic  
✅ Write clear commit messages  
✅ Add comprehensive tests  
✅ Respond promptly to feedback  
✅ Be open to suggestions  
✅ Thank reviewers for their time  
✅ Ask questions when unclear  

### **DON'T:**
❌ Force push without `--force-with-lease`  
❌ Push to upstream `origin` (use your `fork`)  
❌ Add unrelated changes to PR  
❌ Forget DCO sign-off  
❌ Take feedback personally  
❌ Argue unnecessarily  
❌ Disappear after opening PR  
❌ Make changes directly in GitHub UI (use git)  

### **Communication:**
- **Be professional** - You represent CVS Health
- **Be patient** - Maintainers are volunteers
- **Be collaborative** - Open to alternative approaches
- **Be thorough** - Explain your reasoning
- **Be responsive** - Check notifications daily

---

## 🔧 Troubleshooting

### **Problem: Can't push to origin**

**Error:** `Permission denied` or `403 Forbidden`

**Solution:**
```bash
# You're trying to push to upstream, not your fork
# Use your fork instead:
git push fork <branch-name>
```

### **Problem: DCO check failing**

**Error:** `DCO check failed`

**Solution:**
```bash
# Sign off commits
git rebase HEAD~3 --signoff  # For last 3 commits
git push --force-with-lease fork <branch-name>
```

### **Problem: Merge conflicts**

**Error:** `CONFLICT (content): Merge conflict in file.kt`

**Solution:**
```bash
git fetch origin
git rebase origin/main
# Edit conflicted files
git add <resolved-files>
git rebase --continue
git push --force-with-lease fork <branch-name>
```

### **Problem: CI checks failing**

**Solution:**
```bash
# Check what's failing in PR
# Run the same checks locally
./gradlew spotlessCheck
./gradlew test
./gradlew build

# Fix issues, commit, and push
```

### **Problem: Wrong base branch**

If you accidentally opened PR against wrong branch:

1. Close the PR
2. Switch base branch:
   - Click "Edit" on PR
   - Change "base" dropdown
   - Or close and create new PR

---

## 📚 Useful Git Commands Reference

### **Status and Info:**
```bash
git status                    # Current branch status
git log --oneline            # Commit history
git remote -v                # List remotes
git branch -a                # List all branches
```

### **Branch Management:**
```bash
git checkout -b new-branch   # Create and switch to branch
git checkout main            # Switch to main
git branch -d old-branch     # Delete local branch
git push fork --delete old-branch  # Delete remote branch
```

### **Updating:**
```bash
git fetch origin             # Get latest from upstream
git pull origin main         # Update main branch
git rebase origin/main       # Rebase on latest main
```

### **Committing:**
```bash
git add .                    # Stage all changes
git commit --signoff -m "msg"  # Commit with DCO
git commit --amend           # Modify last commit
git rebase -i HEAD~3         # Interactive rebase
```

### **Pushing:**
```bash
git push fork branch-name                    # Normal push
git push --force-with-lease fork branch-name # Safe force push
```

---

## 📞 Getting Help

### **GitHub Issues:**
If you have questions about the project:
- https://github.com/open-telemetry/opentelemetry-android/issues

### **OpenTelemetry Slack:**
- Join: https://slack.cncf.io/
- Channel: `#otel-android`

### **Internal CVS Resources:**
- Your team lead
- This documentation
- Other team members who've contributed

### **Git/GitHub Help:**
- Git documentation: https://git-scm.com/doc
- GitHub guides: https://guides.github.com/

---

## 🎯 Quick Reference Card

**Fork URL:** `https://github.com/YOUR-USERNAME/opentelemetry-android`  
**Upstream URL:** `https://github.com/open-telemetry/opentelemetry-android`

### **Common Commands:**

```bash
# Before starting
git checkout main
git pull origin main
git checkout -b feature/my-change

# Before pushing
./gradlew spotlessApply
./gradlew test
git add .
git commit --signoff -m "Description"
git push fork feature/my-change

# After feedback
# (make changes)
git add .
git commit --signoff -m "Address review feedback"
git push fork feature/my-change

# If conflicts
git fetch origin
git rebase origin/main
# (resolve conflicts)
git rebase --continue
git push --force-with-lease fork feature/my-change
```

---

## ✅ Final Checklist

Before asking for help, verify:

- [ ] I've read this entire document
- [ ] My fork is set up correctly
- [ ] I'm pushing to my fork, not upstream
- [ ] All commits are signed off (DCO)
- [ ] Tests pass locally
- [ ] Code is formatted (spotless)
- [ ] PR description is complete
- [ ] I've responded to feedback

---

**Document Version:** 1.0  
**Last Updated:** November 21, 2025  
**Maintained By:** CVS Health Android Team  
**Next Review:** After first successful upstream PR

---

## 📖 Additional Resources

- **OpenTelemetry Android SDK:** https://github.com/open-telemetry/opentelemetry-android
- **Contributing Guide:** https://github.com/open-telemetry/opentelemetry-android/blob/main/CONTRIBUTING.md
- **Semantic Conventions:** https://opentelemetry.io/docs/specs/semconv/
- **Developer Certificate of Origin:** https://developercertificate.org/
- **Git Best Practices:** https://git-scm.com/book/en/v2

Good luck with your contributions! 🚀


# PR Diagram Snippets - Ready to Paste

## 🚀 Quick Instructions

**After exporting diagrams to images:**

1. Upload image to GitHub (drag & drop into any PR/issue comment box)
2. GitHub will give you a URL like: `https://user-images.githubusercontent.com/...`
3. Copy that URL
4. Replace `IMAGE_URL` in the snippets below
5. Paste into your PR description

---

## PR #1: Thread-Safety Fix

### Concurrency Flow - Before vs After

**Before (Race Condition):**

```markdown
![Concurrency Before Fix](IMAGE_URL_FOR_PR1_BEFORE)
```

**After (Atomic CAS):**

```markdown
![Concurrency After Fix](IMAGE_URL_FOR_PR1_AFTER)
```

### Full Section to Add:

```markdown
## 🔍 Visual Explanation

### Before: Race Condition

![Concurrency Race Condition](IMAGE_URL_FOR_PR1_BEFORE)

Multiple threads could create different session IDs when accessing `getSessionId()` concurrently during timeout.

### After: Atomic Solution

![Concurrency with AtomicReference](IMAGE_URL_FOR_PR1_AFTER)

Using `AtomicReference` with compare-and-set ensures only one thread creates a new session, while others use it.
```

---

## PR #2: Session Infrastructure

### Component Architecture

```markdown
![Component Architecture](IMAGE_URL_FOR_PR2_ARCHITECTURE)
```

### Metric Export Flow

```markdown
![Metric Export Flow](IMAGE_URL_FOR_PR2_FLOW)
```

### Full Section to Add:

```markdown
## 🏗️ Architecture Overview

### Component Relationships

![Session Infrastructure Components](IMAGE_URL_FOR_PR2_ARCHITECTURE)

The infrastructure provides three key components:
- **SessionMetricExporterAdapter**: Decorator pattern for metric enrichment
- **SessionIdentifierFacade**: Unified session access
- **SessionExtensions**: Kotlin extension functions

### Metric Export Flow

![Session Metric Export Process](IMAGE_URL_FOR_PR2_FLOW)

Session IDs are injected into all metric data points during export, ensuring consistent correlation.
```

---

## PR #3: Telemetry Integration

### Session ID Propagation

```markdown
![Session Propagation](IMAGE_URL_FOR_PR3_PROPAGATION)
```

### Before vs After Coverage

```markdown
![Coverage Comparison](IMAGE_URL_FOR_PR3_BEFORE_AFTER)
```

### Instrumentation Lifecycle

```markdown
![Lifecycle Integration](IMAGE_URL_FOR_PR3_LIFECYCLE)
```

### Full Section to Add:

```markdown
## 🔗 Integration Overview

### Session ID Propagation Across Telemetry

![Session ID Flow](IMAGE_URL_FOR_PR3_PROPAGATION)

Session IDs are automatically injected into all three observability signals (spans, logs, metrics).

### Coverage: Before vs After

![Observability Coverage](IMAGE_URL_FOR_PR3_BEFORE_AFTER)

This PR completes the session management implementation by adding metrics support.

### Instrumentation Lifecycle Integration

![Session Lifecycle](IMAGE_URL_FOR_PR3_LIFECYCLE)

Session management is fully integrated into the instrumentation lifecycle, with automatic span creation on session boundaries.
```

---

## 📋 Upload Workflow

### Method 1: GitHub UI (Easiest)

1. Go to your PR on GitHub
2. Click **"Edit"** on the PR description
3. Drag & drop an image file into the text area
4. GitHub uploads it and gives you markdown: `![image](https://user-images...)`
5. Repeat for all images
6. Arrange them using the snippets above
7. Click **"Update comment"**

### Method 2: imgur

1. Go to https://imgur.com
2. Upload all images
3. Get direct links (right-click → "Copy image address")
4. Replace `IMAGE_URL` in snippets
5. Paste into PR description

### Method 3: Commit to Repo

```bash
cd /Users/c781502/Git/external/oss-contributions-opentelemetry-android-sdk
git add docs/diagrams/exported/
git commit -m "Add exported diagrams"
git push gregorys-fork <branch-name>
```

Then reference with relative paths:
```markdown
![Diagram](docs/diagrams/exported/PR1_concurrency_before.png)
```

---

## 🎨 Styling Tips

### Add Width Constraints (Optional)

```markdown
<img src="IMAGE_URL" width="600" alt="Diagram">
```

### Center Images (Optional)

```markdown
<p align="center">
  <img src="IMAGE_URL" alt="Diagram">
</p>
```

### Add Captions

```markdown
![Concurrency Flow](IMAGE_URL)
*Figure 1: Thread-safe session creation using AtomicReference*
```

---

## ✅ Checklist Before Posting

- [ ] All diagrams exported to PNG
- [ ] Images uploaded to GitHub/imgur
- [ ] URLs copied and replaced in snippets
- [ ] Tested that images display correctly
- [ ] Added to PR description
- [ ] PR description saved

---

## 🐛 Troubleshooting

**Images not showing:**
- Check URL is accessible (open in browser)
- Use direct image links (not album links)
- Ensure images are public

**Images too large:**
- Use `width="600"` attribute
- Re-export with smaller dimensions

**Want to change diagram:**
- Edit `.mmd` file
- Re-export
- Re-upload
- Update URL in PR

---

**Pro Tip:** Upload all images to a single GitHub comment first, then copy all the URLs at once!


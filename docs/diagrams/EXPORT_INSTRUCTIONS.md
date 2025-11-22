# Mermaid Diagram Export Instructions

GitHub should render Mermaid diagrams natively, but if they're not rendering in your PR, here are three ways to export them as images:

## Option 1: Use Mermaid Live Editor (Easiest)

1. Go to: https://mermaid.live/
2. Open one of the `.mmd` files in this directory
3. Copy the contents
4. Paste into the Mermaid Live Editor
5. Click **"Actions"** → **"PNG"** or **"SVG"**
6. Download the image
7. Upload to GitHub or imgur
8. Reference in PR: `![Diagram Name](image-url)`

## Option 2: Install mermaid-cli and Use the Export Script

### Install mermaid-cli:
```bash
npm install -g @mermaid-js/mermaid-cli
```

### Run the export script:
```bash
cd /Users/c781502/Git/external/oss-contributions-opentelemetry-android-sdk/docs/diagrams
./export_diagrams.sh
```

This will generate PNG files for all diagrams in the `exported/` directory.

## Option 3: Use VS Code Extension

1. Install "Markdown Preview Mermaid Support" extension
2. Open any `.mmd` file
3. Right-click in preview → "Copy Image" or "Save Image"

---

## Diagram Files:

### PR #1: Thread-Safety Fix
- `PR1_concurrency_before.mmd` - Race condition scenario
- `PR1_concurrency_after.mmd` - Atomic CAS solution

### PR #2: Session Infrastructure
- `PR2_component_architecture.mmd` - Component relationships
- `PR2_metric_export_flow.mmd` - Metric export sequence

### PR #3: Telemetry Integration
- `PR3_session_propagation.mmd` - Session ID flow across signals
- `PR3_before_after.mmd` - Coverage comparison
- `PR3_instrumentation_lifecycle.mmd` - Lifecycle integration

---

## Why Aren't Mermaid Diagrams Rendering in GitHub?

**Possible reasons:**
1. **Syntax error** - Check the diagram in mermaid.live first
2. **Browser issue** - Try a different browser or incognito mode
3. **GitHub cache** - Edit the PR description to force refresh
4. **Need triple backticks** - Make sure format is:
   ````markdown
   ```mermaid
   graph TB
   ...
   ```
   ````

**Troubleshooting:**
- Test in mermaid.live first to verify syntax
- Ensure no extra spaces before/after triple backticks
- Try editing and re-saving the PR description
- Check if other mermaid diagrams render on GitHub

---

## Image URLs After Export

Once exported, you can:

1. **Upload to GitHub Issues:**
   - Go to any GitHub issue/PR
   - Drag-and-drop image
   - Copy the uploaded URL
   - Use in your PR description

2. **Commit to repo:**
   - Save images in `docs/diagrams/exported/`
   - Commit and push
   - Reference relative path: `![Diagram](docs/diagrams/exported/PR1_before.png)`

3. **Use imgur or similar:**
   - Upload to https://imgur.com
   - Copy direct link
   - Use in PR description


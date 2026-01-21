# SESSION_CONTEXT.md Auto-Update Automation

This document describes the automated system for maintaining SESSION_CONTEXT.md with every issue, fix, and significant project activity.

## 🎯 Goal
Ensure SESSION_CONTEXT.md is automatically updated whenever issues are investigated, fixed, or significant progress is made, eliminating manual documentation burden.

## 🔧 Implementation Details

### 1. Enhanced Claude Code Hook (PRIMARY METHOD)
**File**: `.claude/hooks/user-prompt-submit-hook.md`

**Triggers automatically when:**
- Starting new Claude Code conversations
- Keywords detected: "investigate", "debug", "error", "issue", "fix", etc.
- Error messages, stack traces, or exceptions are discussed
- Successful completion of troubleshooting
- Application restarts or PM2 activities

**What it does:**
- Reads SESSION_CONTEXT.md at session start
- Provides structured template for documenting issues
- Instructs Claude to update context after every significant task
- Maintains chronological project history

### 2. Manual Update Command
**File**: `.claude/commands/update-context.md`

**Usage**: `/update-context` in Claude Code

**Purpose**:
- Force update SESSION_CONTEXT.md mid-conversation
- Structured template for consistent documentation
- Backup method when automatic triggers don't fire

### 3. Shell Script Automation
**File**: `scripts/update-session-context.sh`

**Usage**:
```bash
./scripts/update-session-context.sh [type] [description]
./scripts/update-session-context.sh "error" "Fixed logback configuration"
```

**Features:**
- Automatic timestamp and git status logging
- PM2 status integration
- Error log analysis (last 5 minutes)
- Creates backup copies of SESSION_CONTEXT.md
- Can be integrated with external automation

### 4. Git Hook Integration (OPTIONAL)
**File**: `scripts/git-hooks/post-commit.example`

**Installation**:
```bash
# Copy to git hooks directory
cp scripts/git-hooks/post-commit.example .git/hooks/post-commit
chmod +x .git/hooks/post-commit
```

**Purpose**:
- Updates SESSION_CONTEXT.md after every git commit
- Logs commit message and changed files
- Links code changes to project context

## 📋 Automation Workflow

### When Issues Occur:
1. **Claude Code Hook** detects error keywords → Automatically reads SESSION_CONTEXT.md
2. **Investigation proceeds** → Context maintained in real-time during conversation
3. **Issue resolved** → SESSION_CONTEXT.md automatically updated with full details
4. **Code committed** → Git hook (if installed) logs commit to context
5. **Script backup** → Manual script available for external triggers

### Information Automatically Captured:
- ✅ Error messages and stack traces
- ✅ Root cause analysis and investigation steps
- ✅ Files examined and line numbers
- ✅ Solutions implemented and code changes
- ✅ Validation results and testing outcomes
- ✅ Git status and modified files
- ✅ PM2 process status and logs
- ✅ Timestamps and chronological order

## 🚀 Getting Started

### Immediate (Already Active):
- Enhanced Claude Code hook is already installed and active
- Next Claude Code conversation will automatically use the enhanced system
- SESSION_CONTEXT.md will be updated automatically during issue resolution

### Optional Enhancements:
1. **Install Git Hook** (recommended for commit tracking):
   ```bash
   cp scripts/git-hooks/post-commit.example .git/hooks/post-commit
   chmod +x .git/hooks/post-commit
   ```

2. **Test Shell Script**:
   ```bash
   ./scripts/update-session-context.sh "test" "Testing automation system"
   ```

3. **Verify Hook System**:
   - Start a new Claude Code conversation
   - Mention "debug" or "error" to trigger context loading
   - Watch SESSION_CONTEXT.md get updated automatically

## 📝 Template Structure

Every issue automatically gets documented with:

```markdown
## New Issue Investigation - [Date]

### Current Error: [Description] ✅ FIXED/🔄 IN_PROGRESS/❌ FAILED
**Error Details:**
[Error messages and symptoms]

### Analysis Results ✅ COMPLETED
**Root Cause Identified:**
- **Issue**: [Technical description]
- **Location**: [Files and line numbers]
- **Cause**: [Why it happened]
- **Impact**: [Effect on application]

### Solution Implementation ✅ COMPLETED
**Fix Applied:**
- [Changes made]

**Files Modified:**
- [List with descriptions]

### Validation Results ✅ VERIFIED
**Application Status After Fix:**
- ✅ [Verification points]
```

## 🔍 Monitoring

### Check Automation Status:
- Review `.claude/hooks/user-prompt-submit-hook.md` for current triggers
- Check SESSION_CONTEXT.md for recent auto-updates
- Verify script permissions: `ls -la scripts/update-session-context.sh`

### Backup System:
- Every script run creates timestamped backups: `SESSION_CONTEXT.md.backup.*`
- Git tracks all changes to SESSION_CONTEXT.md
- Manual recovery always possible

## ✅ Success Indicators

The automation is working correctly when:
- SESSION_CONTEXT.md gets updated during Claude Code conversations
- Every issue resolution is automatically documented
- No manual documentation is required
- Project context is maintained across sessions
- All team members have access to current project status

---

**Status**: ✅ **ACTIVE** - Automation system installed and operational
**Next Steps**: System will automatically maintain documentation going forward
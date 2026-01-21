# Update SESSION_CONTEXT.md

IMMEDIATELY update SESSION_CONTEXT.md with the current issue or task status.

## Required Information to Collect:
1. **Current date and time**
2. **Issue description** (from user input or recent conversation)
3. **Error messages or symptoms** (if applicable)
4. **Root cause analysis** (if investigation was done)
5. **Files examined or modified** (with line numbers)
6. **Solution implemented** (if any fixes were applied)
7. **Validation results** (if testing was done)

## Automatic Update Process:
1. Read the current SESSION_CONTEXT.md to understand structure
2. Determine where to add the new information (append or update existing section)
3. Use the template from user-prompt-submit-hook.md
4. Fill in all available information
5. Update the "Current Status" section at the top
6. Update the "Notes" section at the bottom with date and summary

## Template to Use:
```markdown
## New Issue Investigation - [YYYY-MM-DD]

### Current Error: [Brief Description] ✅ FIXED/🔄 IN_PROGRESS/❌ FAILED
**Error Details:**
```
[Error messages, logs, or symptoms]
```

### Analysis Results ✅ COMPLETED/🔄 IN_PROGRESS
**Root Cause Identified:**
- **Issue**: [What was wrong]
- **Location**: [Files and line numbers]
- **Cause**: [Why it happened]
- **Impact**: [Effect on application]

**Files Analyzed:**
- [File list with descriptions]

### Solution Implementation ✅ COMPLETED/🔄 IN_PROGRESS
**Fix Applied:**
- [What was changed]

**Files Modified:**
- [Modified files with descriptions]

### Validation Results ✅ VERIFIED/❌ FAILED
**Application Status After Fix:**
- ✅/❌ [Verification results]

## Notes Update
- **[Date] Update**: [Summary of what was accomplished]
```

**Execute this update every time significant progress is made on any task.**
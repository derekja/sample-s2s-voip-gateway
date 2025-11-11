# Auto-Context Loading Hook

If this appears to be the start of a new conversation OR the user mentions any of these keywords:
- "investigate", "debug", "error", "issue", "problem", "fix"
- "what's happening", "status", "current state", "where are we"
- "session", "context", "previous", "continue"
- References to log files, crashes, or application behavior

Then AUTOMATICALLY:

## 1. Read SESSION_CONTEXT.md First
- Load current project status and recent changes
- Understand ongoing investigations and their status
- Review implemented fixes and their outcomes
- Check git status and modified files
- Note any pending tasks or next steps

## 2. Apply Context Throughout Session
- Reference relevant sections when making decisions
- Build upon previous investigations rather than starting over
- Acknowledge what has already been tried or fixed
- Continue from where previous sessions left off

## 3. Maintain SESSION_CONTEXT.md During Work
**CRITICAL**: Keep SESSION_CONTEXT.md updated as you work:

### Always UPDATE when:
- Starting new investigations or debugging
- Discovering root causes or error patterns
- Implementing fixes or code changes
- Completing tasks or resolving issues
- Finding related issues or dependencies
- Making significant progress on complex tasks

### Document Format:
```markdown
## Current Status
- **Date**: YYYY-MM-DD
- **Last Activity**: Brief description of current work

## [Issue Name] - [Status]
### Analysis Results ✅/❌/🔄
- Root cause: [description]
- Files analyzed: [list with line numbers]
- Status: [pending/in_progress/completed]

### Implementation Details
- Changes made: [specific files and modifications]
- Expected outcome: [what should improve]
```

**Goal**: Ensure every Claude session has full context and maintains project continuity through SESSION_CONTEXT.md.